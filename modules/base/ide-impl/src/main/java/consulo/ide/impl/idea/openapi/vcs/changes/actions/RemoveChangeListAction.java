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

package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.ui.ex.action.ActionsBundle;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RemoveChangeListAction extends AnAction implements DumbAware {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
    boolean visible = canRemoveChangeLists(e.getData(Project.KEY), changeLists);

    Presentation presentation = e.getPresentation();
    presentation.setEnabled(visible);
    presentation
            .setText(ActionsBundle.message("action.ChangesView.RemoveChangeList.text", changeLists != null && changeLists.length > 1 ? 1 : 0));
    if (e.getPlace().equals(ActionPlaces.CHANGES_VIEW_POPUP)) {
      presentation.setVisible(visible);
    }
    presentation.setDescription(ArrayUtil.isEmpty(e.getData(VcsDataKeys.CHANGES)) ? presentation.getText() : getDescription(changeLists));
  }

  @Nonnull
  private static String getDescription(@jakarta.annotation.Nullable ChangeList[] changeLists) {
    return ActionsBundle.message("action.ChangesView.RemoveChangeList.description",
                                 containsActiveChangelist(changeLists) ? "another changelist" : "active one");
  }

  private static boolean containsActiveChangelist(@jakarta.annotation.Nullable ChangeList[] changeLists) {
    if (changeLists == null) return false;
    return ContainerUtil.exists(changeLists, l -> l instanceof LocalChangeList && ((LocalChangeList)l).isDefault());
  }

  private static boolean canRemoveChangeLists(@jakarta.annotation.Nullable Project project, @jakarta.annotation.Nullable ChangeList[] lists) {
    if (project == null || lists == null || lists.length == 0) return false;

    int allChangeListsCount = ChangeListManager.getInstance(project).getChangeListsNumber();
    for(ChangeList changeList: lists) {
      if (!(changeList instanceof LocalChangeList)) return false;
      LocalChangeList localChangeList = (LocalChangeList) changeList;
      if (localChangeList.isReadOnly()) return false;
      if (localChangeList.isDefault() && allChangeListsCount <= lists.length) return false;
    }
    return true;
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getRequiredData(Project.KEY);
    final ChangeList[] selectedLists = e.getRequiredData(VcsDataKeys.CHANGE_LISTS);

    //noinspection unchecked
    ChangeListRemoveConfirmation.processLists(project, true, (Collection)Arrays.asList(selectedLists), new ChangeListRemoveConfirmation() {
      @Override
      public boolean askIfShouldRemoveChangeLists(@Nonnull List<? extends LocalChangeList> lists) {
        return RemoveChangeListAction.askIfShouldRemoveChangeLists(lists, project);
      }
    });
  }

  private static boolean askIfShouldRemoveChangeLists(@Nonnull List<? extends LocalChangeList> lists, Project project) {
    boolean activeChangelistSelected = lists.stream().anyMatch(LocalChangeList::isDefault);
    boolean haveNoChanges = lists.stream().allMatch(l -> l.getChanges().isEmpty());

    if (activeChangelistSelected) {
      return confirmActiveChangeListRemoval(project, lists, haveNoChanges);
    }

    String message = lists.size() == 1
      ? VcsLocalize.changesRemovechangelistWarningText(lists.get(0).getName()).get()
      : VcsLocalize.changesRemovechangelistMultipleWarningText(lists.size()).get();

    return haveNoChanges ||
      Messages.YES == Messages.showYesNoDialog(
        project,
        message,
        VcsLocalize.changesRemovechangelistWarningTitle().get(),
        Messages.getQuestionIcon()
      );
  }

  static boolean confirmActiveChangeListRemoval(@Nonnull Project project, @Nonnull List<? extends LocalChangeList> lists, boolean empty) {
    List<LocalChangeList> remainingLists = ChangeListManager.getInstance(project).getChangeListsCopy();
    remainingLists.removeAll(lists);

    // don't ask "Which changelist to make active" if there is only one option anyway
    // unless there are some changes to be moved - give user a chance to cancel deletion
    if (remainingLists.size() == 1 && empty) {
      ChangeListManager.getInstance(project).setDefaultChangeList(remainingLists.get(0));
      return true;
    }

    String[] remainingListsNames = remainingLists.stream().map(ChangeList::getName).toArray(String[]::new);
    int nameIndex = Messages.showChooseDialog(
      project,
      empty ? VcsLocalize.changesRemoveActiveEmptyPrompt().get()
        : VcsLocalize.changesRemoveActivePrompt().get(),
      VcsLocalize.changesRemoveActiveTitle().get(),
      Messages.getQuestionIcon(),
      remainingListsNames,
      remainingListsNames[0]
    );
    if (nameIndex < 0) return false;
    ChangeListManager.getInstance(project).setDefaultChangeList(remainingLists.get(nameIndex));
    return true;
  }
}