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
package consulo.ide.impl.idea.application.options.codeStyle;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.setting.CodeStyleSettingsCustomizable;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * Reusable commenter settings form.
 */
public class CommenterForm implements CodeStyleSettingsCustomizable {
    private JPanel myCommenterPanel;
    private JBCheckBox myLineCommentAtFirstColumnCb;
    private JBCheckBox myLineCommentAddSpaceCb;
    private JBCheckBox myBlockCommentAtFirstJBCheckBox;

    private final Language myLanguage;

    public CommenterForm(Language language) {
        this(language, CodeStyleLocalize.titleNamingCommentCode().get());
    }

    public CommenterForm(Language language, @Nullable String title) {
        myLanguage = language;
        if (title != null) {
            myCommenterPanel.setBorder(IdeBorderFactory.createTitledBorder(title));
        }
        myLineCommentAtFirstColumnCb.addActionListener(e -> {
            if (myLineCommentAtFirstColumnCb.isSelected()) {
                myLineCommentAddSpaceCb.setSelected(false);
            }
            myLineCommentAddSpaceCb.setEnabled(!myLineCommentAtFirstColumnCb.isSelected());
        });
        customizeSettings();
    }

    public void reset(@Nonnull CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
        myLineCommentAtFirstColumnCb.setSelected(langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
        myBlockCommentAtFirstJBCheckBox.setSelected(langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN);
        myLineCommentAddSpaceCb.setSelected(langSettings.LINE_COMMENT_ADD_SPACE && !langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
        myLineCommentAddSpaceCb.setEnabled(!langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
    }

    public void apply(@Nonnull CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
        langSettings.LINE_COMMENT_AT_FIRST_COLUMN = myLineCommentAtFirstColumnCb.isSelected();
        langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = myBlockCommentAtFirstJBCheckBox.isSelected();
        langSettings.LINE_COMMENT_ADD_SPACE = myLineCommentAddSpaceCb.isSelected();
    }

    public boolean isModified(@Nonnull CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
        return myLineCommentAtFirstColumnCb.isSelected() != langSettings.LINE_COMMENT_AT_FIRST_COLUMN
            || myBlockCommentAtFirstJBCheckBox.isSelected() != langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN
            || myLineCommentAddSpaceCb.isSelected() != langSettings.LINE_COMMENT_ADD_SPACE;
    }

    public JPanel getCommenterPanel() {
        return myCommenterPanel;
    }

    @Override
    public void showAllStandardOptions() {
        setAllOptionsVisible(true);
    }

    @Override
    public void showStandardOptions(String... optionNames) {
        for (String optionName : optionNames) {
            if (CommenterOption.LINE_COMMENT_ADD_SPACE.name().equals(optionName)) {
                myLineCommentAddSpaceCb.setVisible(true);
            }
            else if (CommenterOption.LINE_COMMENT_AT_FIRST_COLUMN.name().equals(optionName)) {
                myLineCommentAtFirstColumnCb.setVisible(true);
            }
            else if (CommenterOption.BLOCK_COMMENT_AT_FIRST_COLUMN.name().equals(optionName)) {
                myBlockCommentAtFirstJBCheckBox.setVisible(true);
            }
        }
    }

    private void setAllOptionsVisible(boolean isVisible) {
        myLineCommentAtFirstColumnCb.setVisible(isVisible);
        myLineCommentAddSpaceCb.setVisible(isVisible);
        myBlockCommentAtFirstJBCheckBox.setVisible(isVisible);
    }

    private void customizeSettings() {
        setAllOptionsVisible(false);
        LanguageCodeStyleSettingsProvider settingsProvider = LanguageCodeStyleSettingsProvider.forLanguage(myLanguage);
        if (settingsProvider != null) {
            settingsProvider.customizeSettings(this, LanguageCodeStyleSettingsProvider.SettingsType.COMMENTER_SETTINGS);
        }
        myCommenterPanel.setVisible(myLineCommentAtFirstColumnCb.isVisible() || myLineCommentAddSpaceCb.isVisible() || myBlockCommentAtFirstJBCheckBox.isVisible());
    }

    {
// GUI initializer generated by Consulo GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by Consulo GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        myCommenterPanel = new JPanel();
        myCommenterPanel.setLayout(new GridLayoutManager(4, 1, JBUI.emptyInsets(), -1, -1));
        myCommenterPanel.setBorder(BorderFactory.createTitledBorder(CodeStyleLocalize.titleNamingCommentCode().get()));
        myLineCommentAtFirstColumnCb = new JBCheckBox();
        this.$$$loadButtonText$$$(myLineCommentAtFirstColumnCb, CodeStyleLocalize.checkboxLineCommentAtFirstColumn());
        myCommenterPanel.add(
            myLineCommentAtFirstColumnCb,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        Spacer spacer1 = new Spacer();
        myCommenterPanel.add(
            spacer1,
            new GridConstraints(
                3,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myLineCommentAddSpaceCb = new JBCheckBox();
        this.$$$loadButtonText$$$(myLineCommentAddSpaceCb, CodeStyleLocalize.checkboxLineCommentAddSpace());
        myCommenterPanel.add(
            myLineCommentAddSpaceCb,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        myBlockCommentAtFirstJBCheckBox = new JBCheckBox();
        this.$$$loadButtonText$$$(myBlockCommentAtFirstJBCheckBox, CodeStyleLocalize.checkboxBlockCommentAtFirstColumn());
        myCommenterPanel.add(
            myBlockCommentAtFirstJBCheckBox,
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
    }

    private void $$$loadButtonText$$$(AbstractButton component, @Nonnull LocalizeValue text0) {
        StringBuilder result = new StringBuilder();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        String text = text0.get();
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
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    public JComponent $$$getRootComponent$$$() {
        return myCommenterPanel;
    }
}
