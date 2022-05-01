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
package consulo.ide.impl.idea.ide.actions;

import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.file.FileTypeManager;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;

public class AssociateFileType extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
    FileTypeChooser.associateFileType(file.getName());
  }

  @Override
  public void update(AnActionEvent e) {
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
