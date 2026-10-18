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
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Creates a change list.
 *
 * @author max
 */
public class NewChangelistDialog extends ChangeListDialogDescriptor {
    private final Project myProject;
    private @Nullable NewEditChangelistPanel myPanel;
    private @Nullable LocalChangeList myCreatedList;

    public NewChangelistDialog(Project project) {
        super(VcsLocalize.changesDialogNewchangelistTitle());
        myProject = project;
    }

    /**
     * @return the created change list, {@code null} when the dialog was cancelled
     */
    @RequiredUIAccess
    public CompletableFuture<@Nullable LocalChangeList> showAsync() {
        return myProject.getApplication().getInstance(DialogService.class)
            .build(myProject, this)
            .showAsync()
            .handle((value, error) -> error == null && value != null ? myCreatedList : null);
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
        panel.init(null);
        return withErrorLabel(panel.getContent());
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        NewEditChangelistPanel panel = myPanel;
        if (!(action instanceof DialogOkAction) || panel == null) {
            return true;
        }

        boolean makeActive = panel.getMakeActiveCheckBox().getValueOrError();
        VcsConfiguration.getInstance(myProject).MAKE_NEW_CHANGELIST_ACTIVE = makeActive;

        ChangeListManager manager = ChangeListManager.getInstance(myProject);
        LocalChangeList list = manager.addChangeList(panel.getChangeListName(), panel.getDescription());
        if (makeActive) {
            manager.setDefaultChangeList(list);
        }
        panel.changelistCreatedOrChanged(list);
        myCreatedList = list;
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

    @Override
    public @Nullable String getHelpId() {
        return "new_changelist_dialog";
    }
}
