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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.change.shelf.ShelvedChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author yole
 */
@ActionImpl(id = "ShelvedChanges.Rename", shortcutFrom = @ActionRef(id = "RenameElement"))
public class RenameShelvedChangeListAction extends AnAction {
    public RenameShelvedChangeListAction() {
        super(ActionLocalize.actionShelvedchangesRenameText(), ActionLocalize.actionShelvedchangesRenameDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getRequiredData(Project.KEY);
        ShelvedChangeListImpl[] changes = e.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        ShelvedChangeListImpl[] recycledChanges = e.getData(ShelvedChangesViewManagerImpl.SHELVED_RECYCLED_CHANGELIST_KEY);
        assert changes != null || recycledChanges != null;
        final ShelvedChangeListImpl changeList = (changes != null && changes.length == 1) ? changes[0] : recycledChanges[0];
        String newName = Messages.showInputDialog(
            project,
            VcsLocalize.shelveChangesRenamePrompt().get(),
            VcsLocalize.shelveChangesRenameTitle().get(),
            UIUtil.getQuestionIcon(),
            changeList.DESCRIPTION,
            new InputValidator() {
                @Override
                @RequiredUIAccess
                public boolean checkInput(String inputString) {
                    if (inputString.length() == 0) {
                        return false;
                    }
                    List<ShelvedChangeList> list = ShelveChangesManagerImpl.getInstance(project).getShelvedChangeLists();
                    for (ShelvedChangeList oldList : list) {
                        if (oldList != changeList && oldList.getDescription().equals(inputString)) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                @RequiredUIAccess
                public boolean canClose(String inputString) {
                    return checkInput(inputString);
                }
            }
        );
        if (newName != null && !newName.equals(changeList.DESCRIPTION)) {
            ShelveChangesManagerImpl.getInstance(project).renameChangeList(changeList, newName);
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        ShelvedChangeListImpl[] changes = e.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        ShelvedChangeListImpl[] recycledChanges = e.getData(ShelvedChangesViewManagerImpl.SHELVED_RECYCLED_CHANGELIST_KEY);
        e.getPresentation().setEnabled(
            project != null && (changes != null && changes.length == 1 || recycledChanges != null && recycledChanges.length == 1)
        );
    }
}
