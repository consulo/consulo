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
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

public class DeleteAlreadyUnshelvedAction extends AnAction {
  public DeleteAlreadyUnshelvedAction() {
    super(VcsLocalize.deleteAllAlreadyUnshelved());
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY));
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getRequiredData(Project.KEY);
    final int result = Messages.showYesNoDialog(
      project,
      VcsLocalize.deleteAllAlreadyUnshelvedConfirmation().get(),
      VcsLocalize.deleteAllAlreadyUnshelved().get(),
      UIUtil.getWarningIcon()
    );
    if (result == Messages.YES) {
      final ShelveChangesManager manager = ShelveChangesManager.getInstance(project);
      manager.clearRecycled();
    }
  }
}
