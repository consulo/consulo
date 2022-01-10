/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * from kotlin
 */
public class OpenInRightSplitAction extends DumbAwareAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = getEventProject(e);
    if (project == null) {
      return;
    }

    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (file == null) {
      return;
    }

    Navigatable element = ObjectUtil.tryCast(e.getData(CommonDataKeys.PSI_ELEMENT), Navigatable.class);

    EditorWindow editorWindow = openInRightSplit(project, file, element, true);
    if (element == null && editorWindow != null) {
      VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

      if (files != null && files.length > 1) {
        for (VirtualFile virtualFile : files) {
          FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
          fileEditorManager.openFileWithProviders(virtualFile, true, editorWindow);
        }
      }
    }
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = getEventProject(e);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    FileEditor fileEditor = e.getData(PlatformDataKeys.FILE_EDITOR);

    String place = e.getPlace();
    if(project == null || fileEditor != null || editor != null || ActionPlaces.EDITOR_TAB_POPUP.equals(place) || ActionPlaces.EDITOR_POPUP.equals(place)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    VirtualFile contextFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    e.getPresentation().setEnabledAndVisible(contextFile != null && !contextFile.isDirectory());
  }

  @Nullable
  @RequiredUIAccess
  public static EditorWindow openInRightSplit(Project project, VirtualFile file, @Nullable Navigatable element, boolean requestFocus) {
    FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);

    EditorsSplitters splitters = fileEditorManager.getSplitters();

    FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(project, file);
    if (providers.length == 0) {
      if (element != null) {
        element.navigate(requestFocus);
      }
      return null;
    }

    EditorWindow editorWindow = splitters.openInRightSplit(file, requestFocus);
    if (editorWindow == null) {
      if (element != null) {
        element.navigate(requestFocus);
      }
      return null;
    }

    if (element != null && !(element instanceof PsiFile)) {
      Application.get().invokeLater(() -> {
        element.navigate(requestFocus);
      }, project.getDisposed());
    }

    return editorWindow;
  }
}
