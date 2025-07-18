/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

public class CleanUnshelvedAction extends DumbAwareAction {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    final Project project = e.getData(Project.KEY);
    final Presentation presentation = e.getPresentation();
    if (project == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    presentation.setVisible(true);
    presentation.setEnabled(!ShelveChangesManager.getInstance(project).getRecycledShelvedChangeLists().isEmpty());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getRequiredData(Project.KEY);
    CleanUnshelvedFilterDialog dialog = new CleanUnshelvedFilterDialog(project);
    dialog.show();
    if (dialog.isOK()) {
      if (dialog.isUnshelvedWithFilterSelected()) {
        ShelveChangesManager.getInstance(project).cleanUnshelved(false, dialog.getTimeLimitInMillis());
      }
      else if (dialog.isAllUnshelvedSelected()) {
        ShelveChangesManager.getInstance(project).clearRecycled();
      }
      else {
        ShelveChangesManager.getInstance(project).cleanUnshelved(true, System.currentTimeMillis());
      }
    }
  }
}
