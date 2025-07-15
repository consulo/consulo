/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.fileChooser.actions;

import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileChooserKeys;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileSystemTreeImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.localize.UILocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

public class NewFileAction extends FileChooserAction {
  @Override
  protected void update(@Nonnull FileSystemTree fileSystemTree, @Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    final FileType fileType = e.getData(FileChooserKeys.NEW_FILE_TYPE);
    if (fileType != null) {
      presentation.setVisible(true);
      VirtualFile selectedFile = fileSystemTree.getNewFileParent();
      presentation.setEnabled(selectedFile != null && selectedFile.isDirectory());
      presentation.setIcon(PlatformIconGroup.actionsAddfile());
    }
    else {
      presentation.setVisible(false);
    }
  }

  @Override
  @RequiredUIAccess
  protected void actionPerformed(@Nonnull FileSystemTree fileSystemTree, @Nonnull AnActionEvent e) {
    FileType fileType = e.getRequiredData(FileChooserKeys.NEW_FILE_TYPE);
    final String initialContent = e.getData(FileChooserKeys.NEW_FILE_TEMPLATE_TEXT);
    if (initialContent != null) {
      createNewFile(fileSystemTree, fileType, initialContent);
    }
  }

  @RequiredUIAccess
  private static void createNewFile(FileSystemTree fileSystemTree, final FileType fileType, final String initialContent) {
    final VirtualFile file = fileSystemTree.getNewFileParent();
    if (file == null || !file.isDirectory()) return;

    String newFileName;
    while (true) {
      newFileName = Messages.showInputDialog(
          UILocalize.createNewFileEnterNewFileNamePromptText().get(),
          UILocalize.newFileDialogTitle().get(),
          UIUtil.getQuestionIcon()
      );
      if (newFileName == null) {
        return;
      }
      if ("".equals(newFileName.trim())) {
        Messages.showMessageDialog(
            UILocalize.createNewFileFileNameCannotBeEmptyErrorMessage().get(),
            UILocalize.errorDialogTitle().get(),
            UIUtil.getErrorIcon()
        );
        continue;
      }
      Exception failReason = ((FileSystemTreeImpl)fileSystemTree).createNewFile(file, newFileName, fileType, initialContent);
      if (failReason != null) {
        Messages.showMessageDialog(
            UILocalize.createNewFileCouldNotCreateFileErrorMessage(newFileName).get(),
            UILocalize.errorDialogTitle().get(),
            UIUtil.getErrorIcon()
        );
        continue;
      }
      return;
    }
  }
}
