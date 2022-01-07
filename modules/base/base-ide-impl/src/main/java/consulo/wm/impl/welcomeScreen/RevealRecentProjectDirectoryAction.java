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
package consulo.wm.impl.welcomeScreen;

import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.ide.actions.ShowFilePathAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.wm.impl.welcomeScreen.RecentProjectsWelcomeScreenActionBase;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 03/05/2021
 */
public class RevealRecentProjectDirectoryAction extends RecentProjectsWelcomeScreenActionBase {
  public RevealRecentProjectDirectoryAction() {
    getTemplatePresentation().setText(RevealFileAction.getActionName(null));
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setText(RevealFileAction.getActionName(e.getPlace()));

    List<AnAction> elements = getSelectedElements(e);
    boolean enable = elements.size() == 1 && elements.get(0) instanceof ReopenProjectAction;
    presentation.setEnabledAndVisible(enable);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    List<AnAction> elements = getSelectedElements(e);
    if (elements.size() != 1) {
      return;
    }

    AnAction action = elements.get(0);
    if (action instanceof ReopenProjectAction reopenProjectAction) {
      String projectPath = reopenProjectAction.getProjectPath();

      ShowFilePathAction.openFile(new File(projectPath));
    }
  }
}
