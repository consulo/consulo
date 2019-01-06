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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;

public class AssociateFileType extends AnAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);

    FileTypeChooser.associateFileTypeAsync(file.getName());
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
    Project project = e.getData(CommonDataKeys.PROJECT);
    boolean haveSmthToDo;
    if (project == null || file == null || file.isDirectory()) {
      haveSmthToDo = false;
    }
    else {
      // the action should also be available for files which have been auto-detected as text or as a particular language (IDEA-79574)
      haveSmthToDo = FileTypeManager.getInstance().getFileTypeByFileName(file.getName()) == UnknownFileType.INSTANCE;
    }
    presentation.setVisible(haveSmthToDo || ActionPlaces.MAIN_MENU.equals(e.getPlace()));
    presentation.setEnabled(haveSmthToDo);
  }

}
