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
package consulo.versionControlSystem.impl.internal.change.conflict;

import consulo.ui.RadioGroup;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.impl.internal.change.ChangeListManagerImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ChangelistConflictConfigurable extends SimpleConfigurableByProperties
    implements SearchableConfigurable, Configurable.NoScroll {

    private final Project myProject;
    private final ChangelistConflictTracker myConflictTracker;

    private @Nullable MutableFlatDataModel<String> myIgnoredFilesModel;
    private @Nullable Button myClearButton;

    private boolean myIgnoredFilesCleared;

    public ChangelistConflictConfigurable(ChangeListManagerImpl manager) {
        myProject = manager.getProject();
        myConflictTracker = manager.getConflictTracker();
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        ChangelistConflictTracker.Options options = myConflictTracker.getOptions();
        VcsConfiguration settings = VcsConfiguration.getInstance(myProject);

        VerticalLayout root = VerticalLayout.create();

        CheckBox enableBox =
            CheckBox.create(VcsLocalize.settingsEnableChangelistConflictTracking(), options.TRACKING_ENABLED);
        propertyBuilder.add(enableBox, () -> options.TRACKING_ENABLED, value -> options.TRACKING_ENABLED = value);
        root.add(enableBox);

        VerticalLayout optionsLayout = VerticalLayout.create();

        CheckBox showDialogBox = CheckBox.create(VcsLocalize.settingsShowConflictResolvingDialog());
        propertyBuilder.add(showDialogBox, () -> options.SHOW_DIALOG, value -> options.SHOW_DIALOG = value);
        optionsLayout.add(showDialogBox);

        CheckBox highlightConflictsBox = CheckBox.create(VcsLocalize.settingsHighlightFilesWithConflicts());
        propertyBuilder.add(highlightConflictsBox, () -> options.HIGHLIGHT_CONFLICTS, value -> options.HIGHLIGHT_CONFLICTS = value);
        optionsLayout.add(highlightConflictsBox);

        CheckBox highlightNonActiveBox = CheckBox.create(VcsLocalize.settingsHighlightFilesFromNonActiveChangelists());
        propertyBuilder.add(
            highlightNonActiveBox,
            () -> options.HIGHLIGHT_NON_ACTIVE_CHANGELIST,
            value -> options.HIGHLIGHT_NON_ACTIVE_CHANGELIST = value
        );
        optionsLayout.add(highlightNonActiveBox);

        MutableFlatDataModel<String> ignoredFilesModel = FlatDataModel.of(List.of());
        myIgnoredFilesModel = ignoredFilesModel;
        ListBox<String> ignoredFilesBox = ListBox.create(ignoredFilesModel);

        Button clearButton = Button.create(VcsLocalize.settingsClearIgnoredConflicts());
        clearButton.addClickListener(event -> {
            ignoredFilesModel.removeAll();
            myIgnoredFilesCleared = true;
            clearButton.setEnabled(false);
        });
        myClearButton = clearButton;

        optionsLayout.add(LabeledLayout.create(
            VcsLocalize.settingsFilesWithIgnoredConflicts(),
            DockLayout.create().center(ignoredFilesBox).bottom(clearButton)
        ));

        root.add(optionsLayout);
        enableBox.addValueListener(event -> optionsLayout.setEnabledRecursive(event.getValue()));
        optionsLayout.setEnabledRecursive(options.TRACKING_ENABLED);

        VerticalLayout emptyChangelistLayout = VerticalLayout.create();
        RadioGroup<VcsShowConfirmationOption.Value> group = RadioGroup.create();

        emptyChangelistLayout.add(group.newButton(VcsLocalize.settingsShowOptionsBeforeRemoving(), VcsShowConfirmationOption.Value.SHOW_CONFIRMATION));
        emptyChangelistLayout.add(group.newButton(VcsLocalize.settingsRemoveSilently(), VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY));
        emptyChangelistLayout.add(group.newButton(VcsLocalize.settingsDoNotRemove(), VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY));

        propertyBuilder.add(
            group,
            () -> settings.REMOVE_EMPTY_INACTIVE_CHANGELISTS,
            value -> settings.REMOVE_EMPTY_INACTIVE_CHANGELISTS = value
        );
        root.add(LabeledLayout.create(VcsLocalize.settingsWhenEmptyChangelistBecomesInactive(), emptyChangelistLayout));

        return root;
    }

    @RequiredUIAccess
    @Override
    protected boolean isModified(LayoutWrapper component) {
        if (super.isModified(component)) {
            return true;
        }
        return myIgnoredFilesModel != null
            && myIgnoredFilesModel.getSize() != myConflictTracker.getIgnoredConflicts().size();
    }

    @RequiredUIAccess
    @Override
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        super.apply(component);

        if (myIgnoredFilesCleared) {
            for (ChangelistConflictTracker.Conflict conflict : myConflictTracker.getConflicts().values()) {
                conflict.ignored = false;
            }
            myIgnoredFilesCleared = false;
        }
        myConflictTracker.optionsChanged();
    }

    @RequiredUIAccess
    @Override
    protected void reset(LayoutWrapper component) {
        super.reset(component);

        Collection<String> conflicts = myConflictTracker.getIgnoredConflicts();
        if (myIgnoredFilesModel != null) {
            myIgnoredFilesModel.replaceAll(conflicts);
        }
        if (myClearButton != null) {
            myClearButton.setEnabled(!conflicts.isEmpty());
        }
        myIgnoredFilesCleared = false;
    }

    @RequiredUIAccess
    @Override
    protected void disposeUIResources(LayoutWrapper component) {
        super.disposeUIResources(component);

        myIgnoredFilesModel = null;
        myClearButton = null;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return VcsLocalize.settingsChangelistConflictsConfigurableName();
    }

    @Override
    public String getId() {
        return "project.propVCSSupport.ChangelistConflict";
    }
}
