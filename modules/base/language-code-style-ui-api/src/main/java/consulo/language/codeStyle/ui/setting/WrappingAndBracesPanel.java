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
package consulo.language.codeStyle.ui.setting;

import consulo.application.ApplicationBundle;
import consulo.application.localize.ApplicationLocalize;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleConstraints;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.setting.*;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.CommaSeparatedIntegersField;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.valueEditor.CommaSeparatedIntegersValueEditor;
import consulo.util.collection.MultiMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.*;
import java.util.function.Function;

public class WrappingAndBracesPanel extends OptionTableWithPreviewPanel {

  private final MultiMap<String, String> myGroupToFields = new MultiMap<>();
  private Map<String, SettingsGroup> myFieldNameToGroup;
  private final CommaSeparatedIntegersField mySoftMarginsEditor = new CommaSeparatedIntegersField(null, 0, CodeStyleConstraints.MAX_RIGHT_MARGIN, "Optional");
  private final JComboBox<String> myWrapOnTypingCombo = new ComboBox<>(WRAP_ON_TYPING_OPTIONS);

  public WrappingAndBracesPanel(CodeStyleSettings settings) {
    super(settings);
    MarginOptionsUtil.customizeWrapOnTypingCombo(myWrapOnTypingCombo, settings);
    init();
    UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, mySoftMarginsEditor);
    UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myWrapOnTypingCombo);
  }

  @Override
  public LanguageCodeStyleSettingsProvider.SettingsType getSettingsType() {
    return LanguageCodeStyleSettingsProvider.SettingsType.WRAPPING_AND_BRACES_SETTINGS;
  }

  @Override
  protected void addOption(@Nonnull String fieldName, @Nonnull String title, @Nullable String groupName) {
    super.addOption(fieldName, title, groupName);
    if (groupName != null) {
      myGroupToFields.putValue(groupName, fieldName);
    }
  }

  @Override
  protected void addOption(@Nonnull String fieldName, @Nonnull String title, @Nullable String groupName, @Nonnull String[] options, @Nonnull int[] values) {
    super.addOption(fieldName, title, groupName, options, values);
    if (groupName == null) {
      myGroupToFields.putValue(title, fieldName);
    }
  }

  @Override
  protected void initTables() {
    for (Map.Entry<CodeStyleSettingPresentation.SettingsGroup, List<CodeStyleSettingPresentation>> entry : CodeStyleSettingPresentation.getStandardSettings(getSettingsType()).entrySet()) {
      CodeStyleSettingPresentation.SettingsGroup group = entry.getKey();
      for (CodeStyleSettingPresentation setting : entry.getValue()) {
        String fieldName = setting.getFieldName();
        String uiName = setting.getUiName();
        if (setting instanceof CodeStyleBoundedIntegerSettingPresentation) {
          CodeStyleBoundedIntegerSettingPresentation intSetting = (CodeStyleBoundedIntegerSettingPresentation)setting;
          int defaultValue = intSetting.getDefaultValue();
          addOption(fieldName, uiName, group.name, intSetting.getLowerBound(), intSetting.getUpperBound(), defaultValue, getDefaultIntValueRenderer(fieldName));
        }
        else if (setting instanceof CodeStyleSelectSettingPresentation) {
          CodeStyleSelectSettingPresentation selectSetting = (CodeStyleSelectSettingPresentation)setting;
          addOption(fieldName, uiName, group.name, selectSetting.getOptions(), selectSetting.getValues());
        }
        else if (setting instanceof CodeStyleSoftMarginsPresentation) {
          addSoftMarginsOption(fieldName, uiName, group.name);
          showOption(fieldName);
        }
        else {
          addOption(fieldName, uiName, group.name);
        }
      }
    }
  }

  private Function<Integer, String> getDefaultIntValueRenderer(@Nonnull String fieldName) {
    if ("RIGHT_MARGIN".equals(fieldName)) {
      return integer -> MarginOptionsUtil.getDefaultRightMarginText(getSettings());
    }
    else {
      return integer -> ApplicationBundle.message("integer.field.value.default");
    }
  }

  protected SettingsGroup getAssociatedSettingsGroup(String fieldName) {
    if (myFieldNameToGroup == null) {
      myFieldNameToGroup = new HashMap<>();
      Set<String> groups = myGroupToFields.keySet();
      for (String group : groups) {
        Collection<String> fields = myGroupToFields.get(group);
        SettingsGroup settingsGroup = new SettingsGroup(group, fields);
        for (String field : fields) {
          myFieldNameToGroup.put(field, settingsGroup);
        }
      }
    }
    return myFieldNameToGroup.get(fieldName);
  }

  @Nonnull
  @Override
  protected LocalizeValue getTabTitle() {
    return ApplicationLocalize.wrappingAndBraces();
  }

  protected static class SettingsGroup {
    public final String title;
    public final Collection<String> commonCodeStyleSettingFieldNames;

    public SettingsGroup(@Nonnull String title, @Nonnull Collection<String> commonCodeStyleSettingFieldNames) {
      this.title = title;
      this.commonCodeStyleSettingFieldNames = commonCodeStyleSettingFieldNames;
    }
  }


  private void addSoftMarginsOption(@Nonnull String optionName, @Nonnull String title, @Nullable String groupName) {
    Language language = getDefaultLanguage();
    if (language != null) {
      addCustomOption(new SoftMarginsOption(language, optionName, title, groupName));
    }
  }

  private static class SoftMarginsOption extends Option {

    private final Language myLanguage;

    protected SoftMarginsOption(@Nonnull Language language, @Nonnull String optionName, @Nonnull String title, @Nullable String groupName) {
      super(optionName, title, groupName, null, null);
      myLanguage = language;
    }

    @Override
    public Object getValue(CodeStyleSettings settings) {
      CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
      return langSettings.getSoftMargins();
    }

    @Override
    public void setValue(Object value, CodeStyleSettings settings) {
      settings.setSoftMargins(myLanguage, castToIntList(value));
    }

    @Override
    public boolean isEnabled() {
      return true;
    }
  }

  private static List<Integer> castToIntList(@Nullable Object value) {
    if (value instanceof List && ((List)value).size() > 0 && ((List)value).get(0) instanceof Integer) {
      //noinspection unchecked
      return (List<Integer>)value;
    }
    return Collections.emptyList();
  }

  @Nullable
  @Override
  protected JComponent getCustomValueRenderer(@Nonnull String optionName, @Nonnull Object value) {
    if (CodeStyleSoftMarginsPresentation.OPTION_NAME.equals(optionName)) {
      JLabel softMarginsLabel = new JLabel(getSoftMarginsString(castToIntList(value)));
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, softMarginsLabel);
      return softMarginsLabel;
    }
    else if ("WRAP_ON_TYPING".equals(optionName)) {
      if (value.equals(ApplicationBundle.message("wrapping.wrap.on.typing.default"))) {
        JLabel wrapLabel = new JLabel(MarginOptionsUtil.getDefaultWrapOnTypingText(getSettings()));
        UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, wrapLabel);
        return wrapLabel;
      }
    }
    return super.getCustomValueRenderer(optionName, value);
  }

  @Nonnull
  private String getSoftMarginsString(@Nonnull List<Integer> intList) {
    if (intList.size() > 0) {
      return CommaSeparatedIntegersValueEditor.intListToString(intList);
    }
    return MarginOptionsUtil.getDefaultVisualGuidesText(getSettings());
  }

  @Nullable
  @Override
  protected JComponent getCustomNodeEditor(@Nonnull MyTreeNode node) {
    String optionName = node.getKey().getOptionName();
    if (CodeStyleSoftMarginsPresentation.OPTION_NAME.equals(optionName)) {
      mySoftMarginsEditor.setValue(castToIntList(node.getValue()));
      return mySoftMarginsEditor;
    }
    else if ("WRAP_ON_TYPING".equals(optionName)) {
      Object value = node.getValue();
      if (value instanceof String) {
        for (int i = 0; i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS.length; i++) {
          if (CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS.equals(value)) {
            myWrapOnTypingCombo.setSelectedIndex(i);
            break;
          }
        }
      }
      return myWrapOnTypingCombo;
    }
    return super.getCustomNodeEditor(node);
  }

  @Nullable
  @Override
  protected Object getCustomNodeEditorValue(@Nonnull JComponent customEditor) {
    if (customEditor instanceof CommaSeparatedIntegersField) {
      return ((CommaSeparatedIntegersField)customEditor).getValue();
    }
    else if (customEditor == myWrapOnTypingCombo) {
      int i = myWrapOnTypingCombo.getSelectedIndex();
      return i >= 0 ? CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS[i] : null;
    }
    return super.getCustomNodeEditorValue(customEditor);
  }
}