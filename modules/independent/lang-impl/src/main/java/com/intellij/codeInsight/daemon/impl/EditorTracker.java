/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.EventDispatcher;
import com.intellij.util.SmartList;
import consulo.annotation.inject.NotLazy;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

@Singleton
@NotLazy
public class EditorTracker {
  private static final Logger LOG = Logger.getInstance(EditorTracker.class);

  private final WindowManager myWindowManager;
  private final EditorFactory myEditorFactory;

  private final Map<Window, List<Editor>> myWindowToEditorsMap = new HashMap<>();
  private final Map<Window, WindowAdapter> myWindowToWindowFocusListenerMap = new HashMap<>();
  private final Map<Editor, Window> myEditorToWindowMap = new HashMap<>();
  private List<Editor> myActiveEditors = Collections.emptyList(); // accessed in EDT only

  private final EventDispatcher<EditorTrackerListener> myDispatcher = EventDispatcher.create(EditorTrackerListener.class);

  private JFrame myIdeFrame;
  private Window myActiveWindow;

  private final Project myProject;

  @Inject
  public EditorTracker(Project project,
                       WindowManager windowManager,
                       EditorFactory editorFactory) {
    myProject = project;
    myWindowManager = windowManager;
    myEditorFactory = editorFactory;

    project.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project) {
        if(myProject == project) {
          EditorTracker.this.projectOpened();
        }
      }
    });
  }

  public void projectOpened() {
    myIdeFrame = myWindowManager.getFrame(myProject);
    myProject.getMessageBus().connect(myProject).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerAdapter() {
      @Override
      public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
        if (myIdeFrame == null || myIdeFrame.getFocusOwner() == null) return;
        setActiveWindow(myIdeFrame);
      }
    });

    final MyEditorFactoryListener myEditorFactoryListener = new MyEditorFactoryListener();
    myEditorFactory.addEditorFactoryListener(myEditorFactoryListener,myProject);
    Disposer.register(myProject, new Disposable() {
      @Override
      public void dispose() {
        myEditorFactoryListener.executeOnRelease(null);
      }
    });
  }

  private void editorFocused(Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Window window = myEditorToWindowMap.get(editor);
    if (window == null) return;

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

  private void registerEditor(Editor editor) {
    unregisterEditor(editor);

    final Window window = windowByEditor(editor);
    if (window == null) return;

    myEditorToWindowMap.put(editor, window);
    List<Editor> list = myWindowToEditorsMap.get(window);
    if (list == null) {
      list = new ArrayList<>();
      myWindowToEditorsMap.put(window, list);

      if (!(window instanceof IdeFrameImpl)) {
        WindowAdapter listener =  new WindowAdapter() {
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
        window.addWindowFocusListener(listener);
        window.addWindowListener(listener);
        if (window.isFocused()) {  // windowGainedFocus is missed; activate by force
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
          oldWindow.removeWindowFocusListener(listener);
          oldWindow.removeWindowListener(listener);
        }
      }
    }
  }

  private Window windowByEditor(Editor editor) {
    Window window = SwingUtilities.windowForComponent(editor.getComponent());
    if (window instanceof IdeFrameImpl) {
      if (window != myIdeFrame) return null;
    }
    return window;
  }

  @Nonnull
  List<Editor> getActiveEditors() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return myActiveEditors;
  }

  private void setActiveWindow(Window window) {
    myActiveWindow = window;
    List<Editor> editors = editorsByWindow(myActiveWindow);
    setActiveEditors(editors);
  }

  @Nonnull
  private List<Editor> editorsByWindow(Window window) {
    List<Editor> list = myWindowToEditorsMap.get(window);
    if (list == null) return Collections.emptyList();
    List<Editor> filtered = new SmartList<>();
    for (Editor editor : list) {
      if (editor.getContentComponent().isShowing()) {
        filtered.add(editor);
      }
    }
    return filtered;
  }

  void setActiveEditors(@Nonnull List<Editor> editors) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myActiveEditors = editors;

    if (LOG.isDebugEnabled()) {
      LOG.debug("active editors changed:");
      for (Editor editor : editors) {
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
        LOG.debug("    " + psiFile);
      }
    }

    myDispatcher.getMulticaster().activeEditorsChanged(editors);
  }

  void addEditorTrackerListener(@Nonnull EditorTrackerListener listener, @Nonnull Disposable parentDisposable) {
    myDispatcher.addListener(listener,parentDisposable);
  }

  private class MyEditorFactoryListener implements EditorFactoryListener {
    private final Map<Editor, Runnable> myExecuteOnEditorRelease = new HashMap<>();

    @Override
    public void editorCreated(@Nonnull EditorFactoryEvent event) {
      final Editor editor = event.getEditor();
      if (editor.getProject() != null && editor.getProject() != myProject || myProject.isDisposed()) return;
      final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
      if (psiFile == null) return;

      final JComponent component = editor.getComponent();
      final JComponent contentComponent = editor.getContentComponent();

      final HierarchyListener hierarchyListener = new HierarchyListener() {
        @Override
        public void hierarchyChanged(@Nonnull HierarchyEvent e) {
          registerEditor(editor);
        }
      };
      component.addHierarchyListener(hierarchyListener);

      final FocusListener focusListener = new FocusListener() {
        @Override
        public void focusGained(@Nonnull FocusEvent e) {
          editorFocused(editor);
        }

        @Override
        public void focusLost(@Nonnull FocusEvent e) {
        }
      };
      contentComponent.addFocusListener(focusListener);

      myExecuteOnEditorRelease.put(event.getEditor(), () -> {
        component.removeHierarchyListener(hierarchyListener);
        contentComponent.removeFocusListener(focusListener);
      });
    }

    @Override
    public void editorReleased(@Nonnull EditorFactoryEvent event) {
      final Editor editor = event.getEditor();
      if (editor.getProject() != null && editor.getProject() != myProject) return;
      unregisterEditor(editor);
      executeOnRelease(editor);
    }

    private void executeOnRelease(Editor editor) {
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
}
