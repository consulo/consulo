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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.fileEditor.*;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * from kotlin
 */
public class OpenInRightSplitAction extends DumbAwareAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }

    VirtualFile file = e.getData(VirtualFile.KEY);
    if (file == null) {
      return;
    }

    Navigatable element = ObjectUtil.tryCast(e.getData(PsiElement.KEY), Navigatable.class);

    FileEditorWindow editorWindow = openInRightSplit(project, file, element, true);
    if (element == null && editorWindow != null) {
      VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);

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
    Project project = e.getData(Project.KEY);
    Editor editor = e.getData(Editor.KEY);
    FileEditor fileEditor = e.getData(FileEditor.KEY);

    String place = e.getPlace();
    if(project == null || fileEditor != null || editor != null || ActionPlaces.EDITOR_TAB_POPUP.equals(place) || ActionPlaces.EDITOR_POPUP.equals(place)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    VirtualFile contextFile = e.getData(VirtualFile.KEY);
    e.getPresentation().setEnabledAndVisible(contextFile != null && !contextFile.isDirectory());
  }

  @Nullable
  @RequiredUIAccess
  public static FileEditorWindow openInRightSplit(Project project, VirtualFile file, @Nullable Navigatable element, boolean requestFocus) {
    FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);

    FileEditorsSplitters splitters = fileEditorManager.getSplitters();

    FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(project, file);
    if (providers.length == 0) {
      if (element != null) {
        element.navigate(requestFocus);
      }
      return null;
    }

    FileEditorWindow editorWindow = splitters.openInRightSplit(file, requestFocus);
    if (editorWindow == null) {
      if (element != null) {
        element.navigate(requestFocus);
      }
      return null;
    }

    if (element != null && !(element instanceof PsiFile)) {
      Application.get().invokeLater(() -> element.navigate(requestFocus), project.getDisposed());
    }

    return editorWindow;
  }
}
