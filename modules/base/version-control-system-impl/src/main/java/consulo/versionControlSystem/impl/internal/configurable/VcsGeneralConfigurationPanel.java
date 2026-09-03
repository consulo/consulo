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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.RadioButton;
import consulo.ui.ValueGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.contentAnnotation.VcsContentAnnotationSettings;
import consulo.versionControlSystem.impl.internal.change.RemoteRevisionsCache;
import consulo.versionControlSystem.impl.internal.contentAnnotation.VcsContentAnnotationSettingsState;
import consulo.versionControlSystem.internal.ProjectLevelVcsManagerEx;
import consulo.versionControlSystem.internal.VcsShowConfirmationOptionImpl;
import consulo.versionControlSystem.internal.VcsShowOptionsSettingImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.internal.ReadonlyStatusHandlerInternal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class VcsGeneralConfigurationPanel extends SimpleConfigurableByProperties implements SearchableConfigurable {
    private final Project myProject;

    private final Map<VcsShowOptionsSettingImpl, CheckBox> myPromptOptions = new LinkedHashMap<>();
    private final List<ConfirmationGroup> myConfirmationGroups = new ArrayList<>();

    private Collection<AbstractVcs> myActiveVcses = List.of();

    private record ConfirmationGroup(VcsConfiguration.StandardConfirmation confirmation, List<RadioButton> buttons) {
    }

    public VcsGeneralConfigurationPanel(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VcsConfiguration settings = VcsConfiguration.getInstance(myProject);
        ProjectLevelVcsManagerEx vcsManager = ProjectLevelVcsManagerEx.getInstanceEx(myProject);

        myPromptOptions.clear();
        myConfirmationGroups.clear();

        VerticalLayout root = VerticalLayout.create();

        root.add(LabeledLayout.create(
            VcsLocalize.settingsWhenFilesAreCreated(),
            confirmationGroup(
                propertyBuilder,
                vcsManager,
                VcsConfiguration.StandardConfirmation.ADD,
                VcsLocalize.radioAfterCreationShowOptions(),
                VcsLocalize.radioAfterCreationAddSilently(),
                VcsLocalize.radioAfterCreationDoNotAdd()
            )
        ));

        root.add(LabeledLayout.create(
            VcsLocalize.settingsWhenFilesAreDeleted(),
            confirmationGroup(
                propertyBuilder,
                vcsManager,
                VcsConfiguration.StandardConfirmation.REMOVE,
                VcsLocalize.radioAfterDeletionShowOptions(),
                VcsLocalize.radioAfterDeletionRemoveSilently(),
                VcsLocalize.radioAfterDeletionDoNotRemove()
            )
        ));

        VerticalLayout promptsLayout = VerticalLayout.create();
        for (VcsShowOptionsSettingImpl setting : vcsManager.getAllOptions()) {
            if (!setting.getApplicableVcses().isEmpty() || myProject.isDefault()) {
                CheckBox checkBox = CheckBox.create(LocalizeValue.of(setting.getDisplayName()));
                propertyBuilder.add(checkBox, setting::getValue, setting::setValue);
                promptsLayout.add(checkBox);
                myPromptOptions.put(setting, checkBox);
            }
        }
        root.add(LabeledLayout.create(VcsLocalize.borderDisplayDialogWhenCommandsInvoked(), promptsLayout));

        VerticalLayout otherLayout = VerticalLayout.create();

        CheckBox offerToMoveChangesBox = CheckBox.create(VcsLocalize.checkboxChangelistMoveOffer());
        propertyBuilder.add(
            offerToMoveChangesBox,
            () -> settings.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT,
            value -> settings.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT = value
        );
        otherLayout.add(offerToMoveChangesBox);

        CheckBox forceNonEmptyCommentBox = CheckBox.create(VcsLocalize.checkboxForceNonEmptyMessages());
        propertyBuilder.add(
            forceNonEmptyCommentBox,
            () -> settings.FORCE_NON_EMPTY_COMMENT,
            value -> settings.FORCE_NON_EMPTY_COMMENT = value
        );
        otherLayout.add(forceNonEmptyCommentBox);

        ReadonlyStatusHandlerInternal readonlyStatusHandler =
            (ReadonlyStatusHandlerInternal)ReadonlyStatusHandler.getInstance(myProject);
        CheckBox showReadOnlyStatusDialogBox = CheckBox.create(VcsLocalize.checkboxShowClearReadOnlyStatusDialog());
        propertyBuilder.add(showReadOnlyStatusDialogBox, readonlyStatusHandler::isShowDialog, readonlyStatusHandler::setShowDialog);
        otherLayout.add(showReadOnlyStatusDialogBox);

        ComboBox<VcsShowConfirmationOption.Value> failedCommitBox = ComboBox.<VcsShowConfirmationOption.Value>builder()
            .add(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY, VcsLocalize.settingsConfirmationYes())
            .add(VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY, VcsLocalize.settingsConfirmationNo())
            .add(VcsShowConfirmationOption.Value.SHOW_CONFIRMATION, VcsLocalize.settingsConfirmationAsk())
            .build();
        propertyBuilder.add(
            failedCommitBox,
            () -> settings.MOVE_TO_FAILED_COMMIT_CHANGELIST,
            value -> settings.MOVE_TO_FAILED_COMMIT_CHANGELIST = value
        );
        otherLayout.add(LabeledBuilder.sided(VcsLocalize.createChangelistOnFailedCommit(), failedCommitBox));

        ComboBox<PatchPlacement> patchPlacementBox = ComboBox.<PatchPlacement>builder()
            .add(PatchPlacement.ASK, VcsLocalize.settingsConfirmationAsk())
            .add(PatchPlacement.SHOW, VcsLocalize.settingsConfirmationYes())
            .add(PatchPlacement.DO_NOT_SHOW, VcsLocalize.settingsConfirmationNo())
            .build();
        propertyBuilder.add(
            patchPlacementBox,
            () -> PatchPlacement.of(settings.SHOW_PATCH_IN_EXPLORER),
            value -> settings.SHOW_PATCH_IN_EXPLORER = value.myValue
        );
        LocalizeValue fileManagerName = LocalizeValue.of(Platform.current().fileManagerName());
        otherLayout.add(LabeledBuilder.sided(
            Platform.current().os().isMac()
                ? VcsLocalize.settingsRevealPatchInAfterCreation(fileManagerName)
                : VcsLocalize.settingsShowPatchInAfterCreation(fileManagerName),
            patchPlacementBox
        ));

        root.add(otherLayout);

        root.add(LabeledLayout.create(VcsLocalize.settingsGroupChanges(), createChangesLayout(propertyBuilder, settings)));

        return root;
    }

    @RequiredUIAccess
    private Component createChangesLayout(PropertyBuilder propertyBuilder, VcsConfiguration settings) {
        VerticalLayout changesLayout = VerticalLayout.create();

        CheckBox limitHistoryBox = CheckBox.create(VcsLocalize.settingsLimitHistoryBy(), settings.LIMIT_HISTORY);
        propertyBuilder.add(limitHistoryBox, () -> settings.LIMIT_HISTORY, value -> settings.LIMIT_HISTORY = value);

        IntBox historyRowsBox = IntBox.create(settings.MAXIMUM_HISTORY_ROWS).withRange(10, 1000000).withStep(10);
        propertyBuilder.add(historyRowsBox, () -> settings.MAXIMUM_HISTORY_ROWS, value -> settings.MAXIMUM_HISTORY_ROWS = value);
        changesLayout.add(VcsSettingsRows.gated(limitHistoryBox, historyRowsBox, VcsLocalize.settingsRows()));

        CheckBox showDirtyRecursivelyBox =
            CheckBox.create(VcsLocalize.settingsShowDirectoriesWithChangedDescendants(), settings.SHOW_DIRTY_RECURSIVELY);
        propertyBuilder.add(
            showDirtyRecursivelyBox,
            () -> settings.SHOW_DIRTY_RECURSIVELY,
            value -> settings.SHOW_DIRTY_RECURSIVELY = value
        );
        changesLayout.add(showDirtyRecursivelyBox);

        VcsContentAnnotationSettings annotationSettings = VcsContentAnnotationSettings.getInstance(myProject);

        CheckBox showChangedInLastBox = CheckBox.create(VcsLocalize.settingsShowChangedInLast(), annotationSettings.isShow());
        propertyBuilder.add(showChangedInLastBox, annotationSettings::isShow, annotationSettings::setShow);

        IntBox changedInLastDaysBox =
            IntBox.create(annotationSettings.getLimitDays()).withRange(1, VcsContentAnnotationSettingsState.ourMaxDays);
        propertyBuilder.add(changedInLastDaysBox, annotationSettings::getLimitDays, annotationSettings::setLimit);
        changesLayout.add(VcsSettingsRows.gated(showChangedInLastBox, changedInLastDaysBox, VcsLocalize.settingsDays()));

        if (!myProject.isDefault()) {
            CheckBox trackChangedOnServerBox = CheckBox.create(
                VcsLocalize.vcsConfigTrackChangedOnServer(),
                settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND
            );
            propertyBuilder.add(
                trackChangedOnServerBox,
                () -> settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND,
                value -> settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND = value
            );

            IntBox intervalBox = IntBox.create(settings.CHANGED_ON_SERVER_INTERVAL).withRange(5, 48 * 10 * 60).withStep(5);
            propertyBuilder.add(
                intervalBox,
                () -> settings.CHANGED_ON_SERVER_INTERVAL,
                value -> settings.CHANGED_ON_SERVER_INTERVAL = value
            );
            changesLayout.add(VcsSettingsRows.gated(trackChangedOnServerBox, intervalBox, VcsLocalize.changesMinutes()));
        }

        return changesLayout;
    }

    @RequiredUIAccess
    private Component confirmationGroup(
        PropertyBuilder propertyBuilder,
        ProjectLevelVcsManagerEx vcsManager,
        VcsConfiguration.StandardConfirmation confirmation,
        LocalizeValue showConfirmationText,
        LocalizeValue doActionText,
        LocalizeValue doNothingText
    ) {
        VcsShowConfirmationOptionImpl option = vcsManager.getConfirmation(confirmation);

        RadioButton showConfirmationBox = RadioButton.create(showConfirmationText);
        RadioButton doActionBox = RadioButton.create(doActionText);
        RadioButton doNothingBox = RadioButton.create(doNothingText);

        ValueGroup.createBool().add(showConfirmationBox).add(doActionBox).add(doNothingBox);

        List<RadioButton> buttons = List.of(showConfirmationBox, doActionBox, doNothingBox);
        propertyBuilder.add(
            () -> selectedValue(buttons),
            value -> selectValue(buttons, value),
            option::getValue,
            option::setValue
        );
        myConfirmationGroups.add(new ConfirmationGroup(confirmation, buttons));

        VerticalLayout layout = VerticalLayout.create();
        buttons.forEach(layout::add);
        return layout;
    }

    private static VcsShowConfirmationOption.Value selectedValue(List<RadioButton> buttons) {
        if (Boolean.TRUE.equals(buttons.get(0).getValue())) {
            return VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
        }
        if (Boolean.TRUE.equals(buttons.get(1).getValue())) {
            return VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY;
        }
        return VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY;
    }

    @RequiredUIAccess
    private static void selectValue(List<RadioButton> buttons, VcsShowConfirmationOption.Value value) {
        int index = switch (value) {
            case SHOW_CONFIRMATION -> 0;
            case DO_ACTION_SILENTLY -> 1;
            case DO_NOTHING_SILENTLY -> 2;
        };
        buttons.get(index).setValue(Boolean.TRUE);
    }

    @RequiredUIAccess
    @Override
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        VcsConfiguration settings = VcsConfiguration.getInstance(myProject);
        boolean oldTrackChangedOnServer = settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND;

        super.apply(component);

        if (!myProject.isDefault()) {
            RemoteRevisionsCache.getInstance(myProject).updateAutomaticRefreshAlarmState(
                oldTrackChangedOnServer != settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND
            );
        }
    }

    @RequiredUIAccess
    @Override
    protected void reset(LayoutWrapper component) {
        super.reset(component);

        updateAvailableOptions(myActiveVcses);
    }

    @RequiredUIAccess
    public void updateAvailableOptions(Collection<AbstractVcs> activeVcses) {
        myActiveVcses = activeVcses;

        for (Map.Entry<VcsShowOptionsSettingImpl, CheckBox> entry : myPromptOptions.entrySet()) {
            VcsShowOptionsSettingImpl setting = entry.getKey();
            CheckBox checkBox = entry.getValue();
            checkBox.setEnabled(setting.isApplicableTo(activeVcses) || myProject.isDefault());
            if (!myProject.isDefault()) {
                checkBox.setToolTipText(VcsLocalize.tooltipTextActionApplicableToVcses(composeText(setting.getApplicableVcses())));
            }
        }

        if (myProject.isDefault()) {
            return;
        }

        ProjectLevelVcsManagerEx vcsManager = ProjectLevelVcsManagerEx.getInstanceEx(myProject);
        for (ConfirmationGroup group : myConfirmationGroups) {
            VcsShowConfirmationOptionImpl option = vcsManager.getConfirmation(group.confirmation());
            LocalizeValue tooltip = VcsLocalize.tooltipTextActionApplicableToVcses(composeText(option.getApplicableVcses()));
            for (RadioButton button : group.buttons()) {
                button.setEnabled(option.isApplicableTo(activeVcses));
                button.setToolTipText(tooltip);
            }
        }
    }

    private static String composeText(List<AbstractVcs> applicableVcses) {
        TreeSet<String> result = new TreeSet<>();
        for (AbstractVcs abstractVcs : applicableVcses) {
            result.add(abstractVcs.getDisplayName().get());
        }
        return StringUtil.join(result, ", ");
    }

    private enum PatchPlacement {
        ASK(null),
        SHOW(Boolean.TRUE),
        DO_NOT_SHOW(Boolean.FALSE);

        private final @Nullable Boolean myValue;

        PatchPlacement(@Nullable Boolean value) {
            myValue = value;
        }

        static PatchPlacement of(@Nullable Boolean value) {
            if (value == null) {
                return ASK;
            }
            return value ? SHOW : DO_NOT_SHOW;
        }
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Confirmation");
    }

    @Override
    public String getId() {
        return "project.propVCSSupport.Confirmation";
    }
}
