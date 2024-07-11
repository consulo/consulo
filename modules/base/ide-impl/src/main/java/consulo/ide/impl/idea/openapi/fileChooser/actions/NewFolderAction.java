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

import consulo.annotation.DeprecationInfo;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileSystemTreeImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

public class NewFolderAction extends FileChooserAction {
  public NewFolderAction() {
  }

  public NewFolderAction(final LocalizeValue text, final LocalizeValue description, final Image icon) {
    super(text, description, icon);
  }

  @Deprecated
  @DeprecationInfo("Use constructor with LocalizeValue")
  public NewFolderAction(final String text, final String description, final Image icon) {
    super(text, description, icon);
  }

  @Override
  protected void update(FileSystemTree fileSystemTree, AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    VirtualFile parent = fileSystemTree.getNewFileParent();
    presentation.setEnabled(parent != null && parent.isDirectory());
    setEnabledInModalContext(true);
  }

  @Override
  @RequiredUIAccess
  protected void actionPerformed(FileSystemTree fileSystemTree, AnActionEvent e) {
    createNewFolder(fileSystemTree);
  }

  @RequiredUIAccess
  private static void createNewFolder(FileSystemTree fileSystemTree) {
    final VirtualFile file = fileSystemTree.getNewFileParent();
    if (file == null || !file.isDirectory()) return;

    String newFolderName;
    while (true) {
      newFolderName = Messages.showInputDialog(
        UILocalize.createNewFolderEnterNewFolderNamePromptText().get(),
        UILocalize.newFolderDialogTitle().get(),
        UIUtil.getQuestionIcon()
      );
      if (newFolderName == null) {
        return;
      }
      if ("".equals(newFolderName.trim())) {
        Messages.showMessageDialog(
          UILocalize.createNewFolderFolderNameCannotBeEmptyErrorMessage().get(),
          UILocalize.errorDialogTitle().get(),
          UIUtil.getErrorIcon()
        );
        continue;
      }
      Exception failReason = ((FileSystemTreeImpl)fileSystemTree).createNewFolder(file, newFolderName);
      if (failReason != null) {
        Messages.showMessageDialog(
          UILocalize.createNewFolderCouldNotCreateFolderErrorMessage(newFolderName).get(),
          UILocalize.errorDialogTitle().get(),
          UIUtil.getErrorIcon()
        );
        continue;
      }
      return;
    }
  }
}
