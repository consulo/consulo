// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EditorTracker implements Disposable {
  @Nonnull
  public static EditorTracker getInstance(@Nonnull Project project) {
    return project.getInstance(EditorTracker.class);
  }

  private static final Logger LOG = Logger.getInstance(EditorTracker.class);

  protected final Project myProject;

  protected final Map<Window, List<Editor>> myWindowToEditorsMap = new HashMap<>();
  protected final Map<Editor, Window> myEditorToWindowMap = new HashMap<>();
  protected List<Editor> myActiveEditors = List.of(); // accessed in EDT only

  protected Window myActiveWindow;
  protected final Map<Editor, Runnable> myExecuteOnEditorRelease = new HashMap<>();

  @Inject
  public EditorTracker(@Nonnull Project project, @Nonnull Provider<WindowManager> windowManagerProvider) {
    myProject = project;
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

  @Nonnull
  @RequiredUIAccess
  public List<Editor> getActiveEditors() {
    myProject.getApplication().assertIsDispatchThread();
    return myActiveEditors;
  }

  @RequiredUIAccess
  protected void setActiveWindow(@Nullable Window window) {
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

  @RequiredUIAccess
  public void setActiveEditors(@Nonnull List<Editor> editors) {
    myProject.getApplication().assertIsDispatchThread();
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

  @RequiredUIAccess
  protected abstract void editorCreated(@Nonnull EditorFactoryEvent event);

  protected abstract void unregisterEditor(Editor editor);

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
