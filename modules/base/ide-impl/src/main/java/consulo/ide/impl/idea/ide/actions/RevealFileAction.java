/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

public class RevealFileAction extends DumbAwareAction {
  public RevealFileAction() {
    getTemplatePresentation().setText(getActionName(null));
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    VirtualFile file = ShowFilePathAction.findLocalFile(e.getData(VirtualFile.KEY));
    Presentation presentation = e.getPresentation();
    presentation.setText(getActionName(e.getPlace()));
    presentation.setEnabled(file != null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    VirtualFile file = ShowFilePathAction.findLocalFile(e.getData(VirtualFile.KEY));
    if (file != null) {
      ShowFilePathAction.openFile(new File(file.getPresentableUrl()));
    }
  }

  @Nonnull
  public static String getActionName() {
    return getActionName(null);
  }

  @Nonnull
  public static String getActionName(@Nullable String place) {
    if (ActionPlaces.EDITOR_TAB_POPUP.equals(place) || ActionPlaces.EDITOR_POPUP.equals(place) || ActionPlaces.PROJECT_VIEW_POPUP.equals(place)) {
      return ShowFilePathAction.getFileManagerName();
    }
    return Platform.current().os().isMac() ? ActionsBundle.message("action.RevealIn.name.mac") : ActionsBundle.message("action.RevealIn.name.other", ShowFilePathAction.getFileManagerName());
  }
}
