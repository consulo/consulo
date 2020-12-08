/*
 * Copyright 2013-2020 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.impl.EditorTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.wm.util.IdeFrameUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 07/12/2020
 */
@Singleton
public class DesktopEditorTrackerImpl extends EditorTracker {
  private static final Logger LOG = Logger.getInstance(DesktopEditorTrackerImpl.class);

  private final Map<Window, WindowAdapter> myWindowToWindowFocusListenerMap = new HashMap<>();

  @Inject
  public DesktopEditorTrackerImpl(@Nonnull Project project, @Nonnull Provider<WindowManager> windowManagerProvider) {
    super(project, windowManagerProvider);

    project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
        JFrame frame = windowManagerProvider.get().getFrame(myProject);
        if (frame == null || frame.getFocusOwner() == null) {
          return;
        }

        setActiveWindow(TargetAWT.from(frame));
      }
    });
  }

  @RequiredUIAccess
  @Override
  protected void editorCreated(@Nonnull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    if ((editor.getProject() != null && editor.getProject() != myProject) || myProject.isDisposedOrDisposeInProgress()) {
      return;
    }

    final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
    if (psiFile == null) return;

    final JComponent component = editor.getComponent();
    final JComponent contentComponent = editor.getContentComponent();

    final PropertyChangeListener propertyChangeListener = evt -> {
      if (evt.getOldValue() == null && evt.getNewValue() != null) {
        registerEditor(editor);
      }
    };
    component.addPropertyChangeListener("ancestor", propertyChangeListener);

    FocusListener focusListener = new FocusListener() {
      @Override
      public void focusGained(@Nonnull FocusEvent e) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        Window window = myEditorToWindowMap.get(editor);
        if (window == null) {
          return;
        }

        List<Editor> list = myWindowToEditorsMap.get(window);
        int index = list.indexOf(editor);
        LOG.assertTrue(index >= 0);
        if (list.isEmpty()) return;

        for (int i = index - 1; i >= 0; i--) {
          list.set(i + 1, list.get(i));
        }
        list.set(0, editor);

        setActiveWindow(window);
      }

      @Override
      public void focusLost(@Nonnull FocusEvent e) {
      }
    };
    contentComponent.addFocusListener(focusListener);

    myExecuteOnEditorRelease.put(event.getEditor(), () -> {
      component.removePropertyChangeListener("ancestor", propertyChangeListener);
      contentComponent.removeFocusListener(focusListener);
    });
  }

  @Override
  protected void unregisterEditor(Editor editor) {
    Window oldWindow = myEditorToWindowMap.remove(editor);
    if (oldWindow != null) {
      List<Editor> editorsList = myWindowToEditorsMap.get(oldWindow);
      boolean removed = editorsList.remove(editor);
      LOG.assertTrue(removed);

      if (editorsList.isEmpty()) {
        myWindowToEditorsMap.remove(oldWindow);
        final WindowAdapter listener = myWindowToWindowFocusListenerMap.remove(oldWindow);
        if (listener != null) {
          java.awt.Window frame = TargetAWT.to(oldWindow);

          frame.removeWindowFocusListener(listener);
          frame.removeWindowListener(listener);
        }
      }
    }
  }

  private Window windowByEditor(Editor editor) {
    Window window = TargetAWT.from(SwingUtilities.windowForComponent(editor.getComponent()));
    if (window != null) {
      IdeFrame ideFrame = window.getUserData(IdeFrame.KEY);
      if (IdeFrameUtil.isRootFrame(ideFrame)) {
        if (myProject != ideFrame.getProject()) {
          return null;
        }
      }
    }
    return window;
  }

  private void registerEditor(@Nonnull Editor editor) {
    unregisterEditor(editor);

    Window window = windowByEditor(editor);
    if (window == null) {
      return;
    }

    myEditorToWindowMap.put(editor, window);
    List<Editor> list = myWindowToEditorsMap.get(window);
    if (list == null) {
      list = new ArrayList<>();
      myWindowToEditorsMap.put(window, list);

      if (!IdeFrameUtil.isRootIdeFrameWindow(window)) {
        WindowAdapter listener = new WindowAdapter() {
          @Override
          public void windowGainedFocus(WindowEvent e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("windowGainedFocus:" + window);
            }

            setActiveWindow(window);
          }

          @Override
          public void windowLostFocus(WindowEvent e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("windowLostFocus:" + window);
            }

            setActiveWindow(null);
          }

          @Override
          public void windowClosed(WindowEvent event) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("windowClosed:" + window);
            }

            setActiveWindow(null);
          }
        };
        myWindowToWindowFocusListenerMap.put(window, listener);

        java.awt.Window frame = TargetAWT.to(window);
        frame.addWindowFocusListener(listener);
        frame.addWindowListener(listener);
        if (frame.isFocused()) {  // windowGainedFocus is missed; activate by force
          setActiveWindow(window);
        }
      }
    }
    list.add(editor);

    if (myActiveWindow == window) {
      setActiveWindow(window); // to fire event
    }
  }
}
