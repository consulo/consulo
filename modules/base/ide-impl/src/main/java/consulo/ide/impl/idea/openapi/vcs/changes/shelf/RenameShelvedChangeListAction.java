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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author yole
 */
public class RenameShelvedChangeListAction extends AnAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getRequiredData(Project.KEY);
    final ShelvedChangeList[] changes = e.getData(ShelvedChangesViewManager.SHELVED_CHANGELIST_KEY);
    final ShelvedChangeList[] recycledChanges = e.getData(ShelvedChangesViewManager.SHELVED_RECYCLED_CHANGELIST_KEY);
    assert (changes != null) || (recycledChanges != null);
    final ShelvedChangeList changeList = (changes != null && changes.length == 1) ? changes [0] : recycledChanges[0];
    String newName = Messages.showInputDialog(
      project,
      VcsLocalize.shelveChangesRenamePrompt().get(),
      VcsLocalize.shelveChangesRenameTitle().get(),
      UIUtil.getQuestionIcon(),
      changeList.DESCRIPTION,
      new InputValidator() {
        @Override
        @RequiredUIAccess
        public boolean checkInput(final String inputString) {
          if (inputString.length() == 0) {
            return false;
          }
          final List<ShelvedChangeList> list = ShelveChangesManager.getInstance(project).getShelvedChangeLists();
          for (ShelvedChangeList oldList : list) {
            if (oldList != changeList && oldList.DESCRIPTION.equals(inputString)) {
              return false;
            }
          }
          return true;
        }

        @Override
        @RequiredUIAccess
        public boolean canClose(final String inputString) {
          return checkInput(inputString);
        }
      }
    );
    if (newName != null && !newName.equals(changeList.DESCRIPTION)) {
      ShelveChangesManager.getInstance(project).renameChangeList(changeList, newName);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    final ShelvedChangeList[] changes = e.getData(ShelvedChangesViewManager.SHELVED_CHANGELIST_KEY);
    final ShelvedChangeList[] recycledChanges = e.getData(ShelvedChangesViewManager.SHELVED_RECYCLED_CHANGELIST_KEY);
    e.getPresentation().setEnabled(
      project != null && ((changes != null && changes.length == 1) || (recycledChanges != null && recycledChanges.length == 1))
    );
  }
}
