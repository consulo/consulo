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

import consulo.application.ApplicationBundle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.setting.CodeStyleSettingsCustomizable;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.valueEditor.CommaSeparatedIntegersValueEditor;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

class MarginOptionsUtil {
  public static String getDefaultRightMarginText(@Nonnull CodeStyleSettings settings) {
    return getDefaultValueText(Integer.toString(settings.getDefaultRightMargin()));
  }

  static String getDefaultVisualGuidesText(@Nonnull CodeStyleSettings settings) {
    List<Integer> softMargins = settings.getDefaultSoftMargins();
    return getDefaultValueText(
            (softMargins.size() > 0 ? CommaSeparatedIntegersValueEditor.intListToString(settings.getDefaultSoftMargins()) : ApplicationBundle.message("settings.soft.margins.empty.list")));
  }

  static String getDefaultWrapOnTypingText(@Nonnull CodeStyleSettings settings) {
    return getDefaultValueText(settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN ? "Yes" : "No");
  }

  static void customizeWrapOnTypingCombo(@Nonnull JComboBox<String> wrapOnTypingCombo, @Nonnull CodeStyleSettings settings) {
    wrapOnTypingCombo.setRenderer(new WrapOnTypingListCellRenderer(settings));
  }

  static String getDefaultValueText(@Nonnull String value) {
    return ApplicationBundle.message("settings.default.value.prefix", value);
  }

  static class WrapOnTypingListCellRenderer extends ColoredListCellRenderer<String> {
    private final CodeStyleSettings mySettings;

    public WrapOnTypingListCellRenderer(@Nonnull CodeStyleSettings settings) {
      mySettings = settings;
    }

    @Override
    protected void customizeCellRenderer(@Nonnull JList<? extends String> list, String value, int index, boolean selected, boolean hasFocus) {
      for (int i = 0; i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES.length; i++) {
        if (CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES[i] == CommonCodeStyleSettings.WrapOnTyping.DEFAULT.intValue) {
          if (CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS[i].equals(value)) {
            append(getDefaultWrapOnTypingText(mySettings));
          }
        }
      }
    }
  }
}
