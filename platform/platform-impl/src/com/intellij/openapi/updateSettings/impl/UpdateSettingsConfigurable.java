/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.util.text.DateFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author pti
 */
@Deprecated
public class UpdateSettingsConfigurable implements SearchableConfigurable {
  private UpdatesSettingsPanel myUpdatesSettingsPanel;
  private boolean myCheckNowEnabled = true;

  @Override
  public JComponent createComponent() {
    myUpdatesSettingsPanel = new UpdatesSettingsPanel();
    return myUpdatesSettingsPanel.myPanel;
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("updates.settings.title");
  }

  @Override
  public String getHelpTopic() {
    return "preferences.updates";
  }

  public void setCheckNowEnabled(boolean enabled) {
    myCheckNowEnabled = enabled;
  }

  @Override
  public void apply() throws ConfigurationException {
    UpdateSettings settings = UpdateSettings.getInstance();
    settings.setCheckNeeded(myUpdatesSettingsPanel.myCbCheckForUpdates.isSelected());

    settings.setUpdateChannelType(myUpdatesSettingsPanel.getSelectedChannelType().getCode());
  }

  @Override
  public void reset() {
    UpdateSettings settings = UpdateSettings.getInstance();
    myUpdatesSettingsPanel.myCbCheckForUpdates.setSelected(settings.isCheckNeeded());
    myUpdatesSettingsPanel.updateLastCheckedLabel();
    myUpdatesSettingsPanel.setSelectedChannelType(ChannelStatus.fromCode(settings.getUpdateChannelType()));
  }

  @Override
  public boolean isModified() {
    if (myUpdatesSettingsPanel == null) return false;
    UpdateSettings settings = UpdateSettings.getInstance();
    if (settings.isCheckNeeded() != myUpdatesSettingsPanel.myCbCheckForUpdates.isSelected()) return true;
    final JComboBox channelsBox = myUpdatesSettingsPanel.myUpdateChannelsBox;
    return (channelsBox.getSelectedItem() != null && !channelsBox.getSelectedItem().equals(ChannelStatus.fromCode(settings.getUpdateChannelType())));
  }

  @Override
  public void disposeUIResources() {
    myUpdatesSettingsPanel = null;
  }

  private class UpdatesSettingsPanel {

    private JPanel myPanel;
    private JButton myBtnCheckNow;
    private JCheckBox myCbCheckForUpdates;
    private JLabel myBuildNumber;
    private JLabel myVersionNumber;
    private JLabel myLastCheckedDate;

    private JComboBox myUpdateChannelsBox;

    public UpdatesSettingsPanel() {

      final ApplicationInfo appInfo = ApplicationInfo.getInstance();
      final String majorVersion = appInfo.getMajorVersion();
      String versionNumber = "";
      if (majorVersion != null && majorVersion.trim().length() > 0) {
        final String minorVersion = appInfo.getMinorVersion();
        if (minorVersion != null && minorVersion.trim().length() > 0) {
          versionNumber = majorVersion + "." + minorVersion;
        }
        else {
          versionNumber = majorVersion + ".0";
        }
      }
      myVersionNumber.setText(appInfo.getVersionName() + " " + versionNumber);
      myBuildNumber.setText(appInfo.getBuild().asString());

      myBtnCheckNow.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(myBtnCheckNow));
          UpdateSettings settings = new UpdateSettings();
          settings.loadState(UpdateSettings.getInstance().getState());
          settings.setUpdateChannelType(getSelectedChannelType().getCode());
          CheckForUpdateAction.actionPerformed(project, settings);  //todo load configured hosts on the fly
          updateLastCheckedLabel();
        }
      });
      myBtnCheckNow.setEnabled(myCheckNowEnabled);

      LabelTextReplacingUtil.replaceText(myPanel);

      final UpdateSettings settings = UpdateSettings.getInstance();
      myUpdateChannelsBox.setModel(new CollectionComboBoxModel(ChannelStatus.all(), ChannelStatus.fromCode(settings.getUpdateChannelType())));
    }

    private void updateLastCheckedLabel() {
      final long lastChecked = UpdateSettings.getInstance().getLastTimeChecked();
      myLastCheckedDate
        .setText(lastChecked == 0 ? IdeBundle.message("updates.last.check.never") : DateFormatUtil.formatPrettyDateTime(lastChecked));
    }

    public ChannelStatus getSelectedChannelType() {
      return (ChannelStatus) myUpdateChannelsBox.getSelectedItem();
    }

    public void setSelectedChannelType(ChannelStatus channelType) {
      myUpdateChannelsBox.setSelectedItem(channelType != null ? channelType : ChannelStatus.RELEASE);
    }
  }

  @Override
  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  @Nullable
  public Runnable enableSearch(String option) {
    return null;
  }
}
