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
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyAnAction;
import consulo.versionControlSystem.change.shelf.ShelvedChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

/**
 * @author yole
 */
@ActionImpl(id = "ShelvedChanges.Rename", shortcutFrom = @ActionRef(id = "RenameElement"))
public class RenameShelvedChangeListAction extends LegacyAnAction {
    public RenameShelvedChangeListAction() {
        super(ActionLocalize.actionShelvedchangesRenameText(), ActionLocalize.actionShelvedchangesRenameDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ShelvedChangeListImpl[] changes = e.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        ShelvedChangeListImpl[] recycledChanges = e.getData(ShelvedChangesViewManagerImpl.SHELVED_RECYCLED_CHANGELIST_KEY);
        assert changes != null || recycledChanges != null;
        ShelvedChangeListImpl changeList = (changes != null && changes.length == 1) ? changes[0] : recycledChanges[0];
        InputBoxBuilder.text()
            .asQuestion()
            .title(VcsLocalize.shelveChangesRenameTitle())
            .text(VcsLocalize.shelveChangesRenamePrompt())
            .value(changeList.DESCRIPTION)
            .validator(value -> validateName(project, changeList, value))
            .showAsync(project)
            .whenCompleteAsync((newName, error) -> {
                if (newName != null && !newName.equals(changeList.DESCRIPTION)) {
                    ShelveChangesManagerImpl.getInstance(project).renameChangeList(changeList, newName);
                }
            }, UIAccess.current());
    }

    private static @Nullable InputProblem validateName(Project project, ShelvedChangeListImpl changeList, String name) {
        if (name.isEmpty()) {
            return InputProblem.error(VcsLocalize.shelveChangesRenameEmpty());
        }
        for (ShelvedChangeList oldList : ShelveChangesManagerImpl.getInstance(project).getShelvedChangeLists()) {
            if (oldList != changeList && oldList.getDescription().equals(name)) {
                return InputProblem.error(VcsLocalize.shelveChangesRenameDuplicate());
            }
        }
        return null;
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        ShelvedChangeListImpl[] changes = e.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        ShelvedChangeListImpl[] recycledChanges = e.getData(ShelvedChangesViewManagerImpl.SHELVED_RECYCLED_CHANGELIST_KEY);
        e.getPresentation().setEnabled(
            project != null && (changes != null && changes.length == 1 || recycledChanges != null && recycledChanges.length == 1)
        );
    }
}
