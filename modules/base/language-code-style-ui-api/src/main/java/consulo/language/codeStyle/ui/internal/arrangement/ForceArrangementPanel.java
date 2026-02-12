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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.EnumComboBoxModel;
import consulo.ui.ex.awt.OptionGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public class ForceArrangementPanel {
    @Nonnull
    private final JComboBox<SelectedMode> myForceRearrangeComboBox;
    @Nonnull
    private final JPanel myPanel;

    public ForceArrangementPanel() {
        myForceRearrangeComboBox = new JComboBox<>();
        myForceRearrangeComboBox.setModel(new EnumComboBoxModel<>(SelectedMode.class));
        myForceRearrangeComboBox.setMaximumSize(myForceRearrangeComboBox.getPreferredSize());
        myPanel = createPanel();
    }

    public int getRearrangeMode() {
        return getSelectedMode().rearrangeMode;
    }

    public void setSelectedMode(@Nonnull SelectedMode mode) {
        myForceRearrangeComboBox.setSelectedItem(mode);
    }

    public void setSelectedMode(int mode) {
        SelectedMode toSetUp = SelectedMode.getByMode(mode);
        assert (toSetUp != null);
        setSelectedMode(toSetUp);
    }

    @Nonnull
    public JPanel getPanel() {
        return myPanel;
    }

    @Nonnull
    private JPanel createPanel() {
        OptionGroup group = new OptionGroup(CodeStyleLocalize.arrangementSettingsAdditionalTitle().get());
        JPanel textWithComboPanel = new JPanel();
        textWithComboPanel.setLayout(new BoxLayout(textWithComboPanel, BoxLayout.LINE_AXIS));
        textWithComboPanel.add(new JLabel(CodeStyleLocalize.arrangementSettingsAdditionalForceComboboxName().get()));
        textWithComboPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        textWithComboPanel.add(myForceRearrangeComboBox);
        group.add(textWithComboPanel);
        return group.createPanel();
    }

    @Nonnull
    private SelectedMode getSelectedMode() {
        return (SelectedMode) myForceRearrangeComboBox.getSelectedItem();
    }

    private enum SelectedMode {
        FROM_DIALOG(
            CodeStyleLocalize.arrangementSettingsAdditionalForceRearrangeAccordingToDialog(),
            CommonCodeStyleSettings.REARRANGE_ACCORDIND_TO_DIALOG
        ),
        ALWAYS(
            CodeStyleLocalize.arrangementSettingsAdditionalForceRearrangeAlways(),
            CommonCodeStyleSettings.REARRANGE_ALWAYS
        ),
        NEVER(
            CodeStyleLocalize.arrangementSettingsAdditionalForceRearrangeNever(),
            CommonCodeStyleSettings.REARRANGE_NEVER
        );

        public final int rearrangeMode;
        @Nonnull
        private final LocalizeValue myName;

        SelectedMode(@Nonnull LocalizeValue name, int mode) {
            myName = name;
            rearrangeMode = mode;
        }

        @Nullable
        private static SelectedMode getByMode(int mode) {
            for (SelectedMode currentMode : values()) {
                if (currentMode.rearrangeMode == mode) {
                    return currentMode;
                }
            }
            return null;
        }

        @Nonnull
        @Override
        public String toString() {
            return myName.get();
        }
    }
}
