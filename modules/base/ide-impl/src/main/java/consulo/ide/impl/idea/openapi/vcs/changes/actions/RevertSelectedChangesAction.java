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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;

public class RevertSelectedChangesAction extends RevertCommittedStuffAbstractAction {

  @Override
  public void update(AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    presentation.setIcon(AllIcons.Actions.Rollback);
    presentation.setText(VcsBundle.message("action.revert.selected.changes.text"));
    super.update(e);
    presentation.setEnabled(allSelectedChangeListsAreRevertable(e));
  }

  private static boolean allSelectedChangeListsAreRevertable(AnActionEvent e) {
    ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
    if (changeLists == null) {
      return true;
    }
    for (ChangeList list : changeLists) {
      if (list instanceof CommittedChangeList) {
        if (!((CommittedChangeList)list).isModifiable()) {
          return false;
        }
      }
    }
    return true;
  }

  public RevertSelectedChangesAction() {
    super(e -> e.getData(VcsDataKeys.SELECTED_CHANGES_IN_DETAILS), e -> {
      // to ensure directory flags for SVN are initialized
      e.getData(VcsDataKeys.CHANGES_WITH_MOVED_CHILDREN);
      return e.getData(VcsDataKeys.SELECTED_CHANGES_IN_DETAILS);
    });
  }
}
