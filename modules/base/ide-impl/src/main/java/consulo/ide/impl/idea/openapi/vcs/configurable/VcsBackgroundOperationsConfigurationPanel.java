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
package consulo.ide.impl.idea.openapi.vcs.configurable;

import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.project.Project;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.internal.VcsShowOptionsSettingImpl;
import consulo.versionControlSystem.impl.internal.change.RemoteRevisionsCache;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CacheSettingsPanel;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class VcsBackgroundOperationsConfigurationPanel implements SearchableConfigurable {

  private JPanel myPanel;

  private final Project myProject;
  Map<VcsShowOptionsSettingImpl, JCheckBox> myPromptOptions = new LinkedHashMap<VcsShowOptionsSettingImpl, JCheckBox>();
  private JCheckBox myCbUpdateInBackground;
  private JCheckBox myCbCommitInBackground;
  private JCheckBox myCbEditInBackground;
  private JCheckBox myCbAddRemoveInBackground;
  private JCheckBox myCbCheckoutInBackground;
  private JCheckBox myPerformRevertInBackgroundCheckBox;
  private JCheckBox myTrackChangedOnServer;
  private JPanel myCachePanelHolder;
  private JSpinner myChangedOnServerInterval;
  private CacheSettingsPanel myCacheSettingsPanel;

  public VcsBackgroundOperationsConfigurationPanel(final Project project) {
    myProject = project;

    if (! myProject.isDefault()) {
      myCacheSettingsPanel.initPanel(project);
      final VcsConfiguration settings = VcsConfiguration.getInstance(myProject);
      myChangedOnServerInterval.setModel(new SpinnerNumberModel(settings.CHANGED_ON_SERVER_INTERVAL, 5, 48 * 10 * 60, 5));

      myTrackChangedOnServer.addActionListener(e -> myChangedOnServerInterval.setEnabled(myTrackChangedOnServer.isSelected()));
    }
  }

  @Override
  public void apply() throws ConfigurationException {

    VcsConfiguration settings = VcsConfiguration.getInstance(myProject);

    settings.PERFORM_COMMIT_IN_BACKGROUND = myCbCommitInBackground.isSelected();
    settings.PERFORM_UPDATE_IN_BACKGROUND = myCbUpdateInBackground.isSelected();
    settings.PERFORM_CHECKOUT_IN_BACKGROUND = myCbCheckoutInBackground.isSelected();
    settings.PERFORM_EDIT_IN_BACKGROUND = myCbEditInBackground.isSelected();
    settings.PERFORM_ADD_REMOVE_IN_BACKGROUND = myCbAddRemoveInBackground.isSelected();
    settings.PERFORM_ROLLBACK_IN_BACKGROUND = myPerformRevertInBackgroundCheckBox.isSelected();

    boolean remoteCacheStateChanged = settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND != myTrackChangedOnServer.isSelected();
    if (! myProject.isDefault()) {
      settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND = myTrackChangedOnServer.isSelected();
      settings.CHANGED_ON_SERVER_INTERVAL = ((Number) myChangedOnServerInterval.getValue()).intValue();

      myCacheSettingsPanel.apply();
    }

    for (VcsShowOptionsSettingImpl setting : myPromptOptions.keySet()) {
      setting.setValue(myPromptOptions.get(setting).isSelected());
    }
    // will check if should + was started -> inside
    RemoteRevisionsCache.getInstance(myProject).updateAutomaticRefreshAlarmState(remoteCacheStateChanged);
  }

  @Override
  public boolean isModified() {

    VcsConfiguration settings = VcsConfiguration.getInstance(myProject);
    if (settings.PERFORM_COMMIT_IN_BACKGROUND != myCbCommitInBackground.isSelected()) {
      return true;
    }

    if (settings.PERFORM_UPDATE_IN_BACKGROUND != myCbUpdateInBackground.isSelected()) {
      return true;
    }

    if (settings.PERFORM_CHECKOUT_IN_BACKGROUND != myCbCheckoutInBackground.isSelected()) {
      return true;
    }

    if (settings.PERFORM_EDIT_IN_BACKGROUND != myCbEditInBackground.isSelected()) {
      return true;
    }
    if (settings.PERFORM_ADD_REMOVE_IN_BACKGROUND != myCbAddRemoveInBackground.isSelected()) {
      return true;
    }
    if (settings.PERFORM_ROLLBACK_IN_BACKGROUND != myPerformRevertInBackgroundCheckBox.isSelected()) {
      return true;
    }

    if (! myProject.isDefault()) {
      if (settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND != myTrackChangedOnServer.isSelected()) {
        return true;
      }
      if (myCacheSettingsPanel.isModified()) return true;
      if (settings.CHANGED_ON_SERVER_INTERVAL != ((Number) myChangedOnServerInterval.getValue()).intValue()) return true;
    }
    return false;
  }

  @Override
  public void reset() {
    VcsConfiguration settings = VcsConfiguration.getInstance(myProject);
    myCbCommitInBackground.setSelected(settings.PERFORM_COMMIT_IN_BACKGROUND);
    myCbUpdateInBackground.setSelected(settings.PERFORM_UPDATE_IN_BACKGROUND);
    myCbCheckoutInBackground.setSelected(settings.PERFORM_CHECKOUT_IN_BACKGROUND);
    myCbEditInBackground.setSelected(settings.PERFORM_EDIT_IN_BACKGROUND);
    myCbAddRemoveInBackground.setSelected(settings.PERFORM_ADD_REMOVE_IN_BACKGROUND);
    myPerformRevertInBackgroundCheckBox.setSelected(settings.PERFORM_ROLLBACK_IN_BACKGROUND);
    for (VcsShowOptionsSettingImpl setting : myPromptOptions.keySet()) {
      myPromptOptions.get(setting).setSelected(setting.getValue());
    }
    
    if (! myProject.isDefault()) {
      myTrackChangedOnServer.setSelected(settings.CHECK_LOCALLY_CHANGED_CONFLICTS_IN_BACKGROUND);
      myChangedOnServerInterval.setValue(settings.CHANGED_ON_SERVER_INTERVAL);
      myChangedOnServerInterval.setEnabled(myTrackChangedOnServer.isSelected());
      myCacheSettingsPanel.reset();
    }
  }

  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Background";
  }

  @Override
  @Nonnull
  public String getId() {
    return "project.propVCSSupport.Background";
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    return getPanel();
  }

  private void createUIComponents() {
    myCachePanelHolder = new JPanel(new BorderLayout());
    myCacheSettingsPanel = new CacheSettingsPanel();
    myCachePanelHolder.add(myCacheSettingsPanel.getPanel(), BorderLayout.CENTER);
  }
}
