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
package consulo.ide.impl.idea.openapi.vcs.configurable;

import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.ide.impl.idea.ide.actions.ShowFilePathAction;
import consulo.versionControlSystem.internal.VcsShowConfirmationOptionImpl;
import consulo.versionControlSystem.internal.VcsShowOptionsSettingImpl;
import consulo.versionControlSystem.internal.ProjectLevelVcsManagerEx;
import consulo.ide.impl.idea.openapi.vcs.readOnlyHandler.ReadonlyStatusHandlerImpl;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class VcsGeneralConfigurationPanel implements SearchableConfigurable {

  private JCheckBox myForceNonEmptyComment;
  private JCheckBox myShowReadOnlyStatusDialog;

  private JRadioButton myShowDialogOnAddingFile;
  private JRadioButton myPerformActionOnAddingFile;
  private JRadioButton myDoNothingOnAddingFile;

  private JRadioButton myShowDialogOnRemovingFile;
  private JRadioButton myPerformActionOnRemovingFile;
  private JRadioButton myDoNothingOnRemovingFile;

  private JPanel myPanel;

  private final JRadioButton[] myOnFileAddingGroup;
  private final JRadioButton[] myOnFileRemovingGroup;

  private final Project myProject;
  private JPanel myPromptsPanel;

  Map<VcsShowOptionsSettingImpl, JCheckBox> myPromptOptions = new LinkedHashMap<>();
  private JPanel myRemoveConfirmationPanel;
  private JPanel myAddConfirmationPanel;
  private JCheckBox myCbOfferToMoveChanges;
  private JComboBox myFailedCommitChangelistCombo;
  private JComboBox myOnPatchCreation;
  private JCheckBox myClearInitialCommitMessage;
  private ButtonGroup myEmptyChangelistRemovingGroup;

  public VcsGeneralConfigurationPanel(final Project project) {

    myProject = project;

    myOnFileAddingGroup = new JRadioButton[]{
      myShowDialogOnAddingFile,
      myPerformActionOnAddingFile,
      myDoNothingOnAddingFile
    };

    myOnFileRemovingGroup = new JRadioButton[]{
      myShowDialogOnRemovingFile,
      myPerformActionOnRemovingFile,
      myDoNothingOnRemovingFile
    };

    myPromptsPanel.setLayout(new GridLayout(3, 0));

    List<VcsShowOptionsSettingImpl> options = ProjectLevelVcsManagerEx.getInstanceEx(project).getAllOptions();

    for (VcsShowOptionsSettingImpl setting : options) {
      if (!setting.getApplicableVcses().isEmpty() || project.isDefault()) {
        final JCheckBox checkBox = new JCheckBox(setting.getDisplayName());
        myPromptsPanel.add(checkBox);
        myPromptOptions.put(setting, checkBox);
      }
    }

    myPromptsPanel.setSize(myPromptsPanel.getPreferredSize());                           // todo check text!
    myOnPatchCreation.setName(
      (Platform.current().os().isMac() ? "Reveal patch in" : "Show patch in ") +
        ShowFilePathAction.getFileManagerName() + " after creation:"
    );
  }

  public void apply() throws ConfigurationException {

    VcsConfiguration settings = VcsConfiguration.getInstance(myProject);

    settings.FORCE_NON_EMPTY_COMMENT = myForceNonEmptyComment.isSelected();
    settings.CLEAR_INITIAL_COMMIT_MESSAGE = myClearInitialCommitMessage.isSelected();
    settings.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT = myCbOfferToMoveChanges.isSelected();
    settings.REMOVE_EMPTY_INACTIVE_CHANGELISTS = getSelected(myEmptyChangelistRemovingGroup);
    settings.MOVE_TO_FAILED_COMMIT_CHANGELIST = getFailedCommitConfirm();

    for (VcsShowOptionsSettingImpl setting : myPromptOptions.keySet()) {
      setting.setValue(myPromptOptions.get(setting).isSelected());
    }

    getAddConfirmation().setValue(getSelected(myOnFileAddingGroup));
    getRemoveConfirmation().setValue(getSelected(myOnFileRemovingGroup));
    applyPatchOption(settings);

    getReadOnlyStatusHandler().getState().SHOW_DIALOG = myShowReadOnlyStatusDialog.isSelected();
  }

  private void applyPatchOption(VcsConfiguration settings) {
    settings.SHOW_PATCH_IN_EXPLORER = getShowPatchValue();
  }
  
  @Nullable
  private Boolean getShowPatchValue() {
    final int index = myOnPatchCreation.getSelectedIndex();
    if (index == 0) {
      return null;
    } else {
      return index == 1;
    }
  }

  private VcsShowConfirmationOption.Value getFailedCommitConfirm() {
    switch(myFailedCommitChangelistCombo.getSelectedIndex()) {
      case 0: return VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY;
      case 1: return VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY;
      default: return VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
    }
  }

  private VcsShowConfirmationOption getAddConfirmation() {
    return ProjectLevelVcsManagerEx.getInstanceEx(myProject)
      .getConfirmation(VcsConfiguration.StandardConfirmation.ADD);
  }

  private VcsShowConfirmationOption getRemoveConfirmation() {
    return ProjectLevelVcsManagerEx.getInstanceEx(myProject)
      .getConfirmation(VcsConfiguration.StandardConfirmation.REMOVE);
  }


  private static VcsShowConfirmationOption.Value getSelected(JRadioButton[] group) {
    if (group[0].isSelected()) return VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
    if (group[1].isSelected()) return VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY;
    return VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY;
  }

  private static VcsShowConfirmationOption.Value getSelected(ButtonGroup group) {
    switch (UIUtil.getSelectedButton(group)) {
      case 0:
        return VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
      case 1:
        return VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY;
    }
    return VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY;
  }

  private ReadonlyStatusHandlerImpl getReadOnlyStatusHandler() {
    return ((ReadonlyStatusHandlerImpl)ReadonlyStatusHandler.getInstance(myProject));
  }

  public boolean isModified() {

    VcsConfiguration settings = VcsConfiguration.getInstance(myProject);
    if (settings.FORCE_NON_EMPTY_COMMENT != myForceNonEmptyComment.isSelected()){
      return true;
    }
    if (settings.CLEAR_INITIAL_COMMIT_MESSAGE != myClearInitialCommitMessage.isSelected()){
      return true;
    }
    if (settings.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT != myCbOfferToMoveChanges.isSelected()){
      return true;
    }
    if (settings.REMOVE_EMPTY_INACTIVE_CHANGELISTS != getSelected(myEmptyChangelistRemovingGroup)){
      return true;
    }

    if (!Comparing.equal(getFailedCommitConfirm(), settings.MOVE_TO_FAILED_COMMIT_CHANGELIST)) {
      return true;
    }

    if (getReadOnlyStatusHandler().getState().SHOW_DIALOG != myShowReadOnlyStatusDialog.isSelected()) {
      return true;
    }

    for (VcsShowOptionsSettingImpl setting : myPromptOptions.keySet()) {
      if (setting.getValue() != myPromptOptions.get(setting).isSelected()) return true;
    }

    if (getSelected(myOnFileAddingGroup) != getAddConfirmation().getValue()) return true;
    if (getSelected(myOnFileRemovingGroup) != getRemoveConfirmation().getValue()) return true;
    if (! Comparing.equal(settings.SHOW_PATCH_IN_EXPLORER, getShowPatchValue())) return true;

    return false;
  }

  public void reset() {
    VcsConfiguration settings = VcsConfiguration.getInstance(myProject);
    myForceNonEmptyComment.setSelected(settings.FORCE_NON_EMPTY_COMMENT);
    myClearInitialCommitMessage.setSelected(settings.CLEAR_INITIAL_COMMIT_MESSAGE);
    myCbOfferToMoveChanges.setSelected(settings.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT);
    int id = settings.REMOVE_EMPTY_INACTIVE_CHANGELISTS.getId();
    UIUtil.setSelectedButton(myEmptyChangelistRemovingGroup, id == 0 ? 0 : id == 1 ? 2 : 1);
    myShowReadOnlyStatusDialog.setSelected(getReadOnlyStatusHandler().getState().SHOW_DIALOG);
    if (settings.MOVE_TO_FAILED_COMMIT_CHANGELIST == VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY) {
      myFailedCommitChangelistCombo.setSelectedIndex(0);
    }
    else if (settings.MOVE_TO_FAILED_COMMIT_CHANGELIST == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
      myFailedCommitChangelistCombo.setSelectedIndex(1);
    }
    else {
      myFailedCommitChangelistCombo.setSelectedIndex(2);
    }

    for (VcsShowOptionsSettingImpl setting : myPromptOptions.keySet()) {
      myPromptOptions.get(setting).setSelected(setting.getValue());
    }

    selectInGroup(myOnFileAddingGroup, getAddConfirmation());
    selectInGroup(myOnFileRemovingGroup, getRemoveConfirmation());
    if (settings.SHOW_PATCH_IN_EXPLORER == null) {
      myOnPatchCreation.setSelectedIndex(0);
    } else if (Boolean.TRUE.equals(settings.SHOW_PATCH_IN_EXPLORER)) {
      myOnPatchCreation.setSelectedIndex(1);
    } else {
      myOnPatchCreation.setSelectedIndex(2);
    }
  }

  private static void selectInGroup(final JRadioButton[] group, final VcsShowConfirmationOption confirmation) {
    final VcsShowConfirmationOption.Value value = confirmation.getValue();
    final int index;
    //noinspection EnumSwitchStatementWhichMissesCases
    switch(value) {
      case SHOW_CONFIRMATION: index = 0; break;
      case DO_ACTION_SILENTLY: index = 1; break;
      default: index = 2;
    }
    group[index].setSelected(true);
  }


  public JComponent getPanel() {
    return myPanel;
  }

  public void updateAvailableOptions(final Collection<AbstractVcs> activeVcses) {
    for (VcsShowOptionsSettingImpl setting : myPromptOptions.keySet()) {
      final JCheckBox checkBox = myPromptOptions.get(setting);
      checkBox.setEnabled(setting.isApplicableTo(activeVcses) || myProject.isDefault());
      if (!myProject.isDefault()) {
        checkBox.setToolTipText(VcsLocalize.tooltipTextActionApplicableToVcses(composeText(setting.getApplicableVcses())).get());
      }
    }

    if (!myProject.isDefault()) {
      final ProjectLevelVcsManagerEx vcsManager = ProjectLevelVcsManagerEx.getInstanceEx(myProject);
      final VcsShowConfirmationOptionImpl addConfirmation = vcsManager.getConfirmation(VcsConfiguration.StandardConfirmation.ADD);
      UIUtil.setEnabled(myAddConfirmationPanel, addConfirmation.isApplicableTo(activeVcses), true);
      myAddConfirmationPanel.setToolTipText(
        VcsLocalize.tooltipTextActionApplicableToVcses(composeText(addConfirmation.getApplicableVcses())).get()
      );

      final VcsShowConfirmationOptionImpl removeConfirmation = vcsManager.getConfirmation(VcsConfiguration.StandardConfirmation.REMOVE);
      UIUtil.setEnabled(myRemoveConfirmationPanel, removeConfirmation.isApplicableTo(activeVcses), true);
      myRemoveConfirmationPanel.setToolTipText(
        VcsLocalize.tooltipTextActionApplicableToVcses(composeText(removeConfirmation.getApplicableVcses())).get()
      );
    }
  }

  private static String composeText(final List<AbstractVcs> applicableVcses) {
    final TreeSet<String> result = new TreeSet<>();
    for (AbstractVcs abstractVcs : applicableVcses) {
      result.add(abstractVcs.getDisplayName());
    }
    return StringUtil.join(result, ", ");
  }

  @Nls
  public String getDisplayName() {
    return "Confirmation";
  }

  public JComponent createComponent() {
    return getPanel();
  }

  public void disposeUIResources() {
  }

  @Nonnull
  public String getId() {
    return "project.propVCSSupport.Confirmation";
  }

  public Runnable enableSearch(String option) {
    return null;
  }
}
