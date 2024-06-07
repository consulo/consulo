/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.webBrowser.firefox;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.platform.Platform;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.webBrowser.WebBrowserBundle;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.File;
import java.util.List;

/**
 * @author nik
 */
public class FirefoxSettingsConfigurable implements Configurable {
  private static final FileChooserDescriptor PROFILES_INI_CHOOSER_DESCRIPTOR = createProfilesIniChooserDescriptor();

  private JPanel myMainPanel;
  private JComboBox myProfileCombobox;
  private TextFieldWithBrowseButton myProfilesIniPathField;
  private final FirefoxSettings mySettings;
  private String myLastProfilesIniPath;
  private String myDefaultProfilesIniPath;
  private String myDefaultProfile;

  public FirefoxSettingsConfigurable(FirefoxSettings settings) {
    mySettings = settings;
    myProfilesIniPathField.addBrowseFolderListener(WebBrowserBundle.message("chooser.title.select.profiles.ini.file"), null, null, PROFILES_INI_CHOOSER_DESCRIPTOR);
    myProfilesIniPathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        updateProfilesList();
      }
    });
  }

  public static FileChooserDescriptor createProfilesIniChooserDescriptor() {
    return new FileChooserDescriptor(true, false, false, false, false, false) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        return file.getName().equals(FirefoxUtil.PROFILES_INI_FILE) && super.isFileSelectable(file);
      }
    }.withShowHiddenFiles(Platform.current().os().isUnix());
  }

  @Override
  public JComponent createComponent() {
    return myMainPanel;
  }

  @Override
  public boolean isModified() {
    return !Comparing.equal(mySettings.getProfile(), getConfiguredProfileName()) ||
           !Comparing.equal(mySettings.getProfilesIniPath(), getConfiguredProfileIniPath());
  }

  @Nullable
  private String getConfiguredProfileIniPath() {
    String path = PathUtil.toSystemIndependentName(StringUtil.nullize(myProfilesIniPathField.getText()));
    return myDefaultProfilesIniPath.equals(path) ? null : path;
  }

  @Nullable
  private String getConfiguredProfileName() {
    final String selected = (String)myProfileCombobox.getSelectedItem();
    if (Comparing.equal(myDefaultProfile, selected)) {
      return null;
    }
    return selected;
  }

  @Override
  public void apply() throws ConfigurationException {
    mySettings.setProfile(getConfiguredProfileName());
    mySettings.setProfilesIniPath(getConfiguredProfileIniPath());
  }

  @Override
  public void reset() {
    File defaultFile = FirefoxUtil.getDefaultProfileIniPath();
    myDefaultProfilesIniPath = defaultFile != null ? defaultFile.getAbsolutePath() : "";

    String path = mySettings.getProfilesIniPath();
    myProfilesIniPathField.setText(path != null ? FileUtil.toSystemDependentName(path) : myDefaultProfilesIniPath);
    updateProfilesList();
    myProfileCombobox.setSelectedItem(ObjectUtil.notNull(mySettings.getProfile(), myDefaultProfile));
  }

  private void updateProfilesList() {
    final String profilesIniPath = myProfilesIniPathField.getText();
    if (myLastProfilesIniPath != null && myLastProfilesIniPath.equals(profilesIniPath)) {
      return;
    }

    myProfileCombobox.removeAllItems();
    final List<FirefoxProfile> profiles = FirefoxUtil.computeProfiles(new File(profilesIniPath));
    final FirefoxProfile defaultProfile = FirefoxUtil.getDefaultProfile(profiles);
    myDefaultProfile = defaultProfile != null ? defaultProfile.getName() : null;
    for (FirefoxProfile profile : profiles) {
      //noinspection unchecked
      myProfileCombobox.addItem(profile.getName());
    }
    if (!profiles.isEmpty()) {
      myProfileCombobox.setSelectedIndex(0);
    }
    myLastProfilesIniPath = profilesIniPath;
  }

  @Override
  public void disposeUIResources() {
  }

  @Override
  @Nls
  public String getDisplayName() {
    return WebBrowserBundle.message("display.name.firefox.settings");
  }
}
