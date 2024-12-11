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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.webBrowser.localize.WebBrowserLocalize;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
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
        $$$setupUI$$$();

        mySettings = settings;
        myProfilesIniPathField.addBrowseFolderListener(
            WebBrowserLocalize.chooserTitleSelectProfilesIniFile().get(),
            null,
            null,
            PROFILES_INI_CHOOSER_DESCRIPTOR
        );
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
            @RequiredUIAccess
            public boolean isFileSelectable(VirtualFile file) {
                return file.getName().equals(FirefoxUtil.PROFILES_INI_FILE) && super.isFileSelectable(file);
            }
        }.withShowHiddenFiles(Platform.current().os().isUnix());
    }

    @Override
    @RequiredUIAccess
    public JComponent createComponent() {
        return myMainPanel;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        return !Comparing.equal(mySettings.getProfile(), getConfiguredProfileName())
            || !Comparing.equal(mySettings.getProfilesIniPath(), getConfiguredProfileIniPath());
    }

    @Nullable
    private String getConfiguredProfileIniPath() {
        String path = PathUtil.toSystemIndependentName(StringUtil.nullize(myProfilesIniPathField.getText()));
        return myDefaultProfilesIniPath.equals(path) ? null : path;
    }

    @Nullable
    private String getConfiguredProfileName() {
        final String selected = (String) myProfileCombobox.getSelectedItem();
        if (Comparing.equal(myDefaultProfile, selected)) {
            return null;
        }
        return selected;
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
    @RequiredUIAccess
    public void disposeUIResources() {
    }

    @Override
    @Nls
    public String getDisplayName() {
        return WebBrowserLocalize.displayNameFirefoxSettings().get();
    }

    // TODO rewrite it to new UI
    private void $$$setupUI$$$() {
        myMainPanel = new JPanel();
        myMainPanel.setLayout(new GridLayoutManager(2, 1, JBUI.emptyInsets(), -1, -1));
        final Spacer spacer1 = new Spacer();
        myMainPanel.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, JBUI.emptyInsets(), -1, -1));
        myMainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, WebBrowserLocalize.labelTextPathToProfilesIni().get());
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myProfilesIniPathField = new TextFieldWithBrowseButton();
        panel1.add(myProfilesIniPathField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(350, -1), null, 2, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, JBUI.emptyInsets(), -1, -1));
        panel1.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, WebBrowserLocalize.labelTextProfile().get());
        panel3.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myProfileCombobox = new JComboBox();
        panel3.add(myProfileCombobox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label2.setLabelFor(myProfileCombobox);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myMainPanel;
    }
}
