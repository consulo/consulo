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
package consulo.webBrowser.firefox;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.LabeledBuilder;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.webBrowser.localize.WebBrowserLocalize;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class FirefoxSettingsConfigurable implements Configurable {
    private static final FileChooserDescriptor PROFILES_INI_CHOOSER_DESCRIPTOR = createProfilesIniChooserDescriptor();

    private final FirefoxSettings mySettings;
    private final MutableFlatDataModel<String> myProfileModel = FlatDataModel.of(List.of());

    private @Nullable VerticalLayout myLayout;
    private @Nullable ComboBox<String> myProfileBox;
    private FileChooserTextBoxBuilder.@Nullable Controller myProfilesIniBox;

    private String myLastProfilesIniPath;
    private String myDefaultProfilesIniPath = "";
    private String myDefaultProfile;

    public FirefoxSettingsConfigurable(FirefoxSettings settings) {
        mySettings = settings;
    }

    public static FileChooserDescriptor createProfilesIniChooserDescriptor() {
        return new FileChooserDescriptor(true, false, false, false, false, false) {
            @Override
            @RequiredUIAccess
            public boolean isFileSelectable(VirtualFile file) {
                return file.getName().equals(FirefoxUtil.PROFILES_INI_FILE) && super.isFileSelectable(file);
            }
        }.withShowHiddenFiles(Platform.current().os().isUnix());
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        if (myLayout == null) {
            myLayout = VerticalLayout.create();

            FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(null);
            builder.fileChooserDescriptor(PROFILES_INI_CHOOSER_DESCRIPTOR);
            builder.dialogTitle(WebBrowserLocalize.chooserTitleSelectProfilesIniFile());
            builder.uiDisposable(parentDisposable);
            myProfilesIniBox = builder.build();

            myProfileBox = ComboBox.create(myProfileModel);

            // the profile list is read from whatever ini the field points at, so it is rebuilt as the path is typed
            myProfilesIniBox.getComponent().addValueListener(event -> updateProfilesList());

            myLayout.add(LabeledBuilder.filled(
                WebBrowserLocalize.labelTextPathToProfilesIni(),
                myProfilesIniBox.getComponent()
            ));
            myLayout.add(LabeledBuilder.filled(WebBrowserLocalize.labelTextProfile(), myProfileBox));
        }

        return myLayout;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        return !Comparing.equal(mySettings.getProfile(), getConfiguredProfileName())
            || !Comparing.equal(mySettings.getProfilesIniPath(), getConfiguredProfileIniPath());
    }

    @RequiredUIAccess
    private @Nullable String getConfiguredProfileIniPath() {
        if (myProfilesIniBox == null) {
            return null;
        }
        String path = PathUtil.toSystemIndependentName(StringUtil.nullize(myProfilesIniBox.getValue()));
        return myDefaultProfilesIniPath.equals(path) ? null : path;
    }

    @RequiredUIAccess
    private @Nullable String getConfiguredProfileName() {
        String selected = myProfileBox == null ? null : myProfileBox.getValue();
        return Comparing.equal(myDefaultProfile, selected) ? null : selected;
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        mySettings.setProfile(getConfiguredProfileName());
        mySettings.setProfilesIniPath(getConfiguredProfileIniPath());
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        if (myProfilesIniBox == null || myProfileBox == null) {
            return;
        }

        File defaultFile = FirefoxUtil.getDefaultProfileIniPath();
        myDefaultProfilesIniPath = defaultFile != null ? defaultFile.getAbsolutePath() : "";

        String path = mySettings.getProfilesIniPath();
        myProfilesIniBox.setValue(path != null ? FileUtil.toSystemDependentName(path) : myDefaultProfilesIniPath);
        updateProfilesList();
        myProfileBox.setValue(ObjectUtil.notNull(mySettings.getProfile(), myDefaultProfile));
    }

    @RequiredUIAccess
    private void updateProfilesList() {
        if (myProfilesIniBox == null || myProfileBox == null) {
            return;
        }

        String profilesIniPath = myProfilesIniBox.getValue();
        if (myLastProfilesIniPath != null && myLastProfilesIniPath.equals(profilesIniPath)) {
            return;
        }

        List<FirefoxProfile> profiles = FirefoxUtil.computeProfiles(new File(profilesIniPath));
        FirefoxProfile defaultProfile = FirefoxUtil.getDefaultProfile(profiles);
        myDefaultProfile = defaultProfile != null ? defaultProfile.getName() : null;

        List<String> names = new ArrayList<>();
        for (FirefoxProfile profile : profiles) {
            names.add(profile.getName());
        }
        myProfileModel.replaceAll(names);

        if (!names.isEmpty()) {
            myProfileBox.setValue(names.get(0));
        }
        myLastProfilesIniPath = profilesIniPath;
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        myLayout = null;
        myProfileBox = null;
        myProfilesIniBox = null;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return WebBrowserLocalize.displayNameFirefoxSettings();
    }
}
