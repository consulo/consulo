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
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.LocalChangeList;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Asks for the change list to put changes into - an existing one, or a new one named in place.
 *
 * @author max
 */
public class ChangeListChooser extends ChangeListDialogDescriptor {
    private final Project myProject;
    private final Collection<? extends ChangeList> myChangeLists;
    private final @Nullable ChangeList myDefaultSelection;
    private final @Nullable String mySuggestedName;
    private @Nullable ChangeListChooserPanel myPanel;
    private @Nullable LocalChangeList mySelectedList;

    public ChangeListChooser(Project project,
                             Collection<? extends ChangeList> changelists,
                             @Nullable ChangeList defaultSelection,
                             LocalizeValue title,
                             @Nullable String suggestedName) {
        super(title);
        myProject = project;
        myChangeLists = changelists;
        myDefaultSelection = defaultSelection;
        mySuggestedName = suggestedName;
    }

    /**
     * @return the chosen change list, created when it was named new; {@code null} when the dialog was cancelled
     */
    @RequiredUIAccess
    public CompletableFuture<@Nullable LocalChangeList> showAsync() {
        return myProject.getApplication().getInstance(DialogService.class)
            .build(myProject, this)
            .showAsync()
            .handle((value, error) -> error == null && value != null ? mySelectedList : null);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        ChangeListChooserPanel panel = new ChangeListChooserPanel(myProject, this::setErrorText);
        myPanel = panel;
        panel.init();
        panel.setChangeLists(myChangeLists);
        panel.setDefaultSelection(myDefaultSelection);
        if (mySuggestedName != null) {
            panel.setSuggestedName(mySuggestedName);
        }
        return withErrorLabel(panel.getComponent());
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        ChangeListChooserPanel panel = myPanel;
        return panel == null ? null : panel.getPreferredFocusedComponent();
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "VCS.ChangelistChooser";
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        ChangeListChooserPanel panel = myPanel;
        if (action instanceof DialogOkAction && panel != null) {
            mySelectedList = panel.getSelectedList(myProject);
            return mySelectedList != null;
        }
        return true;
    }

    public @Nullable LocalChangeList getSelectedList() {
        return mySelectedList;
    }
}
