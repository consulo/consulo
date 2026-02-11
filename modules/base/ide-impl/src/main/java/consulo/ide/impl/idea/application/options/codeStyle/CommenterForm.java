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
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * Reusable commenter settings form.
 */
public class CommenterForm implements CodeStyleSettingsCustomizable {
    private JPanel myCommenterPanel;
    private CheckBox myLineCommentAtFirstColumnCb;
    private CheckBox myLineCommentAddSpaceCb;
    private CheckBox myBlockCommentAtFirstJBCheckBox;

    private final Language myLanguage;

    @RequiredUIAccess
    public CommenterForm(Language language) {
        this(language, CodeStyleLocalize.titleNamingCommentCode().get());
    }

    @RequiredUIAccess
    public CommenterForm(Language language, @Nullable String title) {
        init();
        myLanguage = language;
        if (title != null) {
            myCommenterPanel.setBorder(IdeBorderFactory.createTitledBorder(title));
        }
        myLineCommentAtFirstColumnCb.addClickListener(e -> {
            if (myLineCommentAtFirstColumnCb.getValue()) {
                myLineCommentAddSpaceCb.setValue(false);
            }
            myLineCommentAddSpaceCb.setEnabled(!myLineCommentAtFirstColumnCb.getValue());
        });
        customizeSettings();
    }

    @RequiredUIAccess
    public void reset(@Nonnull CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
        myLineCommentAtFirstColumnCb.setValue(langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
        myBlockCommentAtFirstJBCheckBox.setValue(langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN);
        myLineCommentAddSpaceCb.setValue(langSettings.LINE_COMMENT_ADD_SPACE && !langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
        myLineCommentAddSpaceCb.setEnabled(!langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
    }

    public void apply(@Nonnull CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
        langSettings.LINE_COMMENT_AT_FIRST_COLUMN = myLineCommentAtFirstColumnCb.getValue();
        langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = myBlockCommentAtFirstJBCheckBox.getValue();
        langSettings.LINE_COMMENT_ADD_SPACE = myLineCommentAddSpaceCb.getValue();
    }

    public boolean isModified(@Nonnull CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
        return myLineCommentAtFirstColumnCb.getValue() != langSettings.LINE_COMMENT_AT_FIRST_COLUMN
            || myBlockCommentAtFirstJBCheckBox.getValue() != langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN
            || myLineCommentAddSpaceCb.getValue() != langSettings.LINE_COMMENT_ADD_SPACE;
    }

    public JPanel getCommenterPanel() {
        return myCommenterPanel;
    }

    @Override
    @RequiredUIAccess
    public void showAllStandardOptions() {
        setAllOptionsVisible(true);
    }

    @Override
    @RequiredUIAccess
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

    @RequiredUIAccess
    private void setAllOptionsVisible(boolean isVisible) {
        myLineCommentAtFirstColumnCb.setVisible(isVisible);
        myLineCommentAddSpaceCb.setVisible(isVisible);
        myBlockCommentAtFirstJBCheckBox.setVisible(isVisible);
    }

    @RequiredUIAccess
    private void customizeSettings() {
        setAllOptionsVisible(false);
        LanguageCodeStyleSettingsProvider settingsProvider = LanguageCodeStyleSettingsProvider.forLanguage(myLanguage);
        if (settingsProvider != null) {
            settingsProvider.customizeSettings(this, LanguageCodeStyleSettingsProvider.SettingsType.COMMENTER_SETTINGS);
        }
        myCommenterPanel.setVisible(myLineCommentAtFirstColumnCb.isVisible() || myLineCommentAddSpaceCb.isVisible() || myBlockCommentAtFirstJBCheckBox.isVisible());
    }

    @RequiredUIAccess
    private void init() {
        myCommenterPanel = new JPanel();
        myCommenterPanel.setLayout(new GridLayoutManager(4, 1, JBUI.emptyInsets(), -1, -1));
        myCommenterPanel.setBorder(BorderFactory.createTitledBorder(CodeStyleLocalize.titleNamingCommentCode().get()));
        myLineCommentAtFirstColumnCb = CheckBox.create(CodeStyleLocalize.checkboxLineCommentAtFirstColumn());
        myCommenterPanel.add(
            TargetAWT.to(myLineCommentAtFirstColumnCb),
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
        myLineCommentAddSpaceCb = CheckBox.create(CodeStyleLocalize.checkboxLineCommentAddSpace());
        myCommenterPanel.add(
            TargetAWT.to(myLineCommentAddSpaceCb),
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
        myBlockCommentAtFirstJBCheckBox = CheckBox.create(CodeStyleLocalize.checkboxBlockCommentAtFirstColumn());
        myCommenterPanel.add(
            TargetAWT.to(myBlockCommentAtFirstJBCheckBox),
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
}
