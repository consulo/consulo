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
package consulo.ide.impl.idea.openapi.vcs.changes.shelf;

import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.localize.VcsLocalize;

public class DeleteAlreadyUnshelvedAction extends AnAction {
  private final String myText;

  public DeleteAlreadyUnshelvedAction() {
    myText = VcsLocalize.deleteAllAlreadyUnshelved().get();
  }

  @Override
  public void update(final AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    final Presentation presentation = e.getPresentation();
    if (project == null) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
    }
    presentation.setEnabled(true);
    presentation.setVisible(true);
    presentation.setText(myText);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(final AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }
    final int result = Messages.showYesNoDialog(
      project,
      VcsLocalize.deleteAllAlreadyUnshelvedConfirmation().get(),
      myText,
      UIUtil.getWarningIcon()
    );
    if (result == Messages.YES) {
      final ShelveChangesManager manager = ShelveChangesManager.getInstance(project);
      manager.clearRecycled();
    }
  }
}
