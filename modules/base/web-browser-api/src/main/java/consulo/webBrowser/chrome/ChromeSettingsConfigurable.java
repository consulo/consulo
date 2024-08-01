/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.webBrowser.chrome;

import consulo.application.AllIcons;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.process.cmd.ParametersListUtil;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.FileChooserTextBoxBuilder;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.Indenter;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author nik
 */
public class ChromeSettingsConfigurable implements Configurable {
    private final ChromeSettings mySettings;

    private VerticalLayout myLayout;
    private TextBoxWithExpandAction myCommandLineOptionsBox;
    private CheckBox myUseCustomProfileCheckBox;
    private FileChooserTextBoxBuilder.Controller myUserDataDirBox;

    private final String myDefaultUserDirPath;

    public ChromeSettingsConfigurable(@Nonnull ChromeSettings settings) {
        mySettings = settings;
        myDefaultUserDirPath = getDefaultUserDataPath();
    }

    @RequiredUIAccess
    @Nullable
    @Override
    public Component createUIComponent(@Nonnull Disposable parentDisposable) {
        if (myLayout == null) {
            myLayout = VerticalLayout.create();

            myLayout.add(Label.create(LocalizeValue.localizeTODO("&Command line options:")));
            myCommandLineOptionsBox = TextBoxWithExpandAction.create(
                AllIcons.Actions.ShowViewer,
                "Chrome Command Line Options",
                ParametersListUtil.DEFAULT_LINE_PARSER,
                ParametersListUtil.DEFAULT_LINE_JOINER
            );

            myLayout.add(Indenter.indent(myCommandLineOptionsBox, 1));
            myUseCustomProfileCheckBox = CheckBox.create(LocalizeValue.localizeTODO("&Use custom profile directory:"));
            myLayout.add(myUseCustomProfileCheckBox);

            FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(null);
            builder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());
            builder.dialogTitle(LocalizeValue.localizeTODO("Select User Data Directory"));
            builder.dialogDescription(LocalizeValue.localizeTODO("Specifies the directory that user data (your \"profile\") is kept in"));

            myUserDataDirBox = builder.build();
            myLayout.add(Indenter.indent(myUserDataDirBox.getComponent(), 1));

            myUseCustomProfileCheckBox.addValueListener(event -> myUserDataDirBox.getComponent().setEnabled(event.getValue()));
        }

        return myLayout;
    }


    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (myUseCustomProfileCheckBox.getValue() != mySettings.isUseCustomProfile()
            || !myCommandLineOptionsBox.getValue().equals(StringUtil.notNullize(mySettings.getCommandLineOptions()))) {
            return true;
        }

        String configuredPath = getConfiguredUserDataDirPath();
        String storedPath = mySettings.getUserDataDirectoryPath();
        if (myDefaultUserDirPath.equals(configuredPath) && storedPath == null) {
            return false;
        }
        return !configuredPath.equals(storedPath);
    }

    private String getConfiguredUserDataDirPath() {
        return FileUtil.toSystemIndependentName(myUserDataDirBox.getValue());
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        mySettings.setCommandLineOptions(myCommandLineOptionsBox.getValue());
        mySettings.setUseCustomProfile(myUseCustomProfileCheckBox.getValue());
        mySettings.setUserDataDirectoryPath(getConfiguredUserDataDirPath());
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        myCommandLineOptionsBox.setValue(mySettings.getCommandLineOptions());
        myUseCustomProfileCheckBox.setValue(mySettings.isUseCustomProfile());
        String path = mySettings.getUserDataDirectoryPath();
        if (path != null) {
            myUserDataDirBox.setValue(FileUtil.toSystemDependentName(path));
        }
        else {
            myUserDataDirBox.setValue(FileUtil.toSystemDependentName(myDefaultUserDirPath));
        }
    }

    public void enableRecommendedOptions() {
        if (!myUseCustomProfileCheckBox.getValue()) {
            myUseCustomProfileCheckBox.setValue(true);
        }
    }

    private static String getDefaultUserDataPath() {
        File dir = new File(ContainerPathManager.get().getConfigPath(), "chrome-user-data");
        try {
            return FileUtil.toSystemIndependentName(dir.getCanonicalPath());
        }
        catch (IOException e) {
            return FileUtil.toSystemIndependentName(dir.getAbsolutePath());
        }
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myLayout = null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Chrome Settings";
    }
}
