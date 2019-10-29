// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.ui.Window;
import consulo.wm.util.IdeFrameUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

@Singleton
public final class EditorTracker implements Disposable {
  public static EditorTracker getInstance(@Nonnull Project project) {
    return project.getComponent(EditorTracker.class);
  }

  private static final Logger LOG = Logger.getInstance(EditorTracker.class);

  private final Project myProject;

  private final Map<Window, List<Editor>> myWindowToEditorsMap = new HashMap<>();
  private final Map<Window, WindowAdapter> myWindowToWindowFocusListenerMap = new HashMap<>();
  private final Map<Editor, Window> myEditorToWindowMap = new HashMap<>();
  private List<Editor> myActiveEditors = Collections.emptyList(); // accessed in EDT only

  private Window myActiveWindow;
  private final Map<Editor, Runnable> myExecuteOnEditorRelease = new HashMap<>();

  @Inject
  public EditorTracker(@Nonnull Project project, @Nonnull Provider<WindowManager> windowManagerProvider) {
    myProject = project;

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
  static final class MyAppLevelEditorFactoryListener implements EditorFactoryListener {
    @Override
    public void editorCreated(@Nonnull EditorFactoryEvent event) {
      Project project = event.getEditor().getProject();
      if (project != null) {
        getInstance(project).editorCreated(event);
      }
    }

    @Override
    public void editorReleased(@Nonnull EditorFactoryEvent event) {
      Project project = event.getEditor().getProject();
      if (project != null) {
        getInstance(project).editorReleased(event);
      }
    }
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

  private void unregisterEditor(Editor editor) {
    Window oldWindow = myEditorToWindowMap.get(editor);
    if (oldWindow != null) {
      myEditorToWindowMap.remove(editor);
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

  @Nonnull
  public List<Editor> getActiveEditors() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return myActiveEditors;
  }

  private void setActiveWindow(@Nullable Window window) {
    myActiveWindow = window;

    List<Editor> list = window == null ? null : myWindowToEditorsMap.get(window);
    if (list == null) {
      setActiveEditors(Collections.emptyList());
    }
    else {
      List<Editor> editors = new SmartList<>();
      for (Editor editor : list) {
        if (editor.getContentComponent().isShowing()) {
          editors.add(editor);
        }
      }
      setActiveEditors(editors);
    }
  }

  public void setActiveEditors(@Nonnull List<Editor> editors) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (editors.equals(myActiveEditors)) {
      return;
    }

    myActiveEditors = editors;

    if (LOG.isDebugEnabled()) {
      LOG.debug("active editors changed:");
      for (Editor editor : editors) {
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
        LOG.debug("    " + psiFile);
      }
    }

    myProject.getMessageBus().syncPublisher(EditorTrackerListener.TOPIC).activeEditorsChanged(editors);
  }

  private void editorCreated(@Nonnull EditorFactoryEvent event) {
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

  private void editorReleased(@Nonnull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    if (editor.getProject() != null && editor.getProject() != myProject) return;
    unregisterEditor(editor);
    executeOnRelease(editor);
  }

  @Override
  public void dispose() {
    executeOnRelease(null);
  }

  private void executeOnRelease(@Nullable Editor editor) {
    if (editor == null) {
      for (Runnable r : myExecuteOnEditorRelease.values()) {
        r.run();
      }
      myExecuteOnEditorRelease.clear();
    }
    else {
      final Runnable runnable = myExecuteOnEditorRelease.get(editor);
      if (runnable != null) {
        runnable.run();
        myExecuteOnEditorRelease.remove(editor);
      }
    }
  }
}
