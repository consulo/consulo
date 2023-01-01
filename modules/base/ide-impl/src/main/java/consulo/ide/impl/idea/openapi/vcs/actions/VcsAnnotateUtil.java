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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.idea.openapi.vcs.impl.BackgroundableActionLock;
import consulo.ide.impl.idea.openapi.vcs.impl.VcsBackgroundableActions;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.List;

class VcsAnnotateUtil {
  @Nonnull
  public static List<Editor> getEditors(@Nonnull Project project, @Nonnull VirtualFile file) {
    FileEditor[] editors = FileEditorManager.getInstance(project).getEditors(file);
    return ContainerUtil.mapNotNull(editors, fileEditor -> fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() : null);
  }

  @Nonnull
  public static BackgroundableActionLock getBackgroundableLock(@Nonnull Project project, @Nonnull VirtualFile file) {
    return BackgroundableActionLock.getLock(project, VcsBackgroundableActions.ANNOTATE, file.getPath());
  }
}
