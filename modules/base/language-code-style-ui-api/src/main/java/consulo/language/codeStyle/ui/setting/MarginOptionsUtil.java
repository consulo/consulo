/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.setting;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.setting.CodeStyleSettingsCustomizable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.valueEditor.CommaSeparatedIntegersValueEditor;

import javax.swing.*;
import java.util.List;

class MarginOptionsUtil {
    public static LocalizeValue getDefaultRightMarginText(CodeStyleSettings settings) {
        return getDefaultValueText(LocalizeValue.of(Integer.toString(settings.getDefaultRightMargin())));
    }

    static LocalizeValue getDefaultVisualGuidesText(CodeStyleSettings settings) {
        List<Integer> softMargins = settings.getDefaultSoftMargins();
        return getDefaultValueText(
            softMargins.size() > 0
                ? LocalizeValue.of(CommaSeparatedIntegersValueEditor.intListToString(settings.getDefaultSoftMargins()))
                : CodeStyleLocalize.settingsSoftMarginsEmptyList()
        );
    }

    static LocalizeValue getDefaultWrapOnTypingText(CodeStyleSettings settings) {
        return getDefaultValueText(
            settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN
                ? CodeStyleLocalize.settingsDefaultValueYes()
                : CodeStyleLocalize.settingsDefaultValueNo()
        );
    }

    static void customizeWrapOnTypingCombo(JComboBox<LocalizeValue> wrapOnTypingCombo, CodeStyleSettings settings) {
        wrapOnTypingCombo.setRenderer(new WrapOnTypingListCellRenderer(settings));
    }

    static LocalizeValue getDefaultValueText(LocalizeValue value) {
        return CodeStyleLocalize.settingsDefaultValuePrefix(value);
    }

    static class WrapOnTypingListCellRenderer extends ColoredListCellRenderer<LocalizeValue> {
        private final CodeStyleSettings mySettings;

        public WrapOnTypingListCellRenderer(CodeStyleSettings settings) {
            mySettings = settings;
        }

        @Override
        protected void customizeCellRenderer(
            JList<? extends LocalizeValue> list,
            LocalizeValue value,
            int index,
            boolean selected,
            boolean hasFocus
        ) {
            for (int i = 0; i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES.length; i++) {
                if (CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES[i] == CommonCodeStyleSettings.WrapOnTyping.DEFAULT.intValue
                    && CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS[i].equals(value)) {
                    append(getDefaultWrapOnTypingText(mySettings));
                }
            }
        }
    }
}
