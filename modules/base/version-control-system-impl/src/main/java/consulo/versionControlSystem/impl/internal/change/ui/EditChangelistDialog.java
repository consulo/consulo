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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Renames a change list, edits its comment and makes it active.
 *
 * @author max
 */
public class EditChangelistDialog extends ChangeListDialogDescriptor {
    private final Project myProject;
    private final LocalChangeList myList;
    private @Nullable NewEditChangelistPanel myPanel;

    public EditChangelistDialog(Project project, LocalChangeList list) {
        super(VcsLocalize.changesDialogEditchangelistTitle());
        myProject = project;
        myList = list;
    }

    @RequiredUIAccess
    public CompletableFuture<?> showAsync() {
        return myProject.getApplication().getInstance(DialogService.class).build(myProject, this).showAsync();
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        NewEditChangelistPanel panel = new NewEditChangelistPanel(myProject) {
            @Override
            @RequiredUIAccess
            protected void nameChanged(LocalizeValue errorMessage) {
                setErrorText(errorMessage);
            }
        };
        myPanel = panel;
        panel.setChangeListName(myList.getName());
        panel.setDescription(myList.getComment());
        panel.init(myList);
        panel.getMakeActiveCheckBox().setValue(myList.isDefault());
        panel.getMakeActiveCheckBox().setEnabled(!myList.isDefault());
        return withErrorLabel(panel.getContent());
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        NewEditChangelistPanel panel = myPanel;
        if (!(action instanceof DialogOkAction) || panel == null) {
            return true;
        }

        ChangeListManager manager = ChangeListManager.getInstance(myProject);
        String oldName = myList.getName();
        String newName = panel.getChangeListName();
        if (!Comparing.equal(oldName, newName) && manager.findChangeList(newName) != null) {
            setErrorText(VcsLocalize.changesDialogEditchangelistErrorAlreadyExists(newName));
            return false;
        }

        if (!oldName.equals(newName)) {
            manager.editName(oldName, newName);
        }
        String newDescription = panel.getDescription();
        if (!Comparing.equal(myList.getComment(), newDescription, true)) {
            manager.editComment(myList.getName(), newDescription);
        }
        if (!myList.isDefault() && panel.getMakeActiveCheckBox().getValueOrError()) {
            manager.setDefaultChangeList(myList);
        }
        panel.changelistCreatedOrChanged(myList);
        return true;
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        NewEditChangelistPanel panel = myPanel;
        return panel == null ? null : panel.getPreferredFocusedComponent();
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "VCS.EditChangelistDialog";
    }
}
