/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.fileChooser.actions;

import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import consulo.ui.ex.action.Presentation;
import consulo.application.Application;
import com.intellij.openapi.fileChooser.FileSystemTree;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nullable;

public final class GotoProjectDirectory extends FileChooserAction {
  @Override
  protected void actionPerformed(final FileSystemTree fileSystemTree, final AnActionEvent e) {
    final VirtualFile projectPath = getProjectDir(e);
    if (projectPath != null) {
      fileSystemTree.select(projectPath, new Runnable() {
        @Override
        public void run() {
          fileSystemTree.expand(projectPath, null);
        }
      });
    }
  }

  @Override
  protected void update(final FileSystemTree fileSystemTree, final AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    presentation.setIcon(Application.get().getIcon());
    final VirtualFile projectPath = getProjectDir(e);
    presentation.setEnabled(projectPath != null && fileSystemTree.isUnderRoots(projectPath));
  }

  @Nullable
  private static VirtualFile getProjectDir(final AnActionEvent e) {
    final VirtualFile projectFileDir = e.getData(PlatformDataKeys.PROJECT_FILE_DIRECTORY);
    return projectFileDir != null && projectFileDir.isValid() ? projectFileDir : null;
  }
}
