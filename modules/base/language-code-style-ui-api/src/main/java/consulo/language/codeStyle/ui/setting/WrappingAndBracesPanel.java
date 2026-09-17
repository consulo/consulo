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

import consulo.application.localize.ApplicationLocalize;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleConstraints;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.setting.*;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CommaSeparatedIntegersField;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.valueEditor.CommaSeparatedIntegersValueEditor;
import consulo.util.collection.MultiMap;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.function.Function;

public class WrappingAndBracesPanel extends OptionTableWithPreviewPanel {
    private final MultiMap<LocalizeValue, String> myGroupToFields = new MultiMap<>();
    private Map<String, SettingsGroup> myFieldNameToGroup;
    private final CommaSeparatedIntegersField mySoftMarginsEditor =
        new CommaSeparatedIntegersField(null, 0, CodeStyleConstraints.MAX_RIGHT_MARGIN, "Optional");
    private final JComboBox<LocalizeValue> myWrapOnTypingCombo = new JComboBox<>(WRAP_ON_TYPING_OPTIONS);

    @RequiredUIAccess
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
    protected void addOption(String fieldName, LocalizeValue title, LocalizeValue groupName) {
        super.addOption(fieldName, title, groupName);
        if (groupName.isNotEmpty()) {
            myGroupToFields.putValue(groupName, fieldName);
        }
    }

    @Override
    protected void addOption(String fieldName, LocalizeValue title, LocalizeValue groupName, LocalizeValue[] options, int[] values) {
        super.addOption(fieldName, title, groupName, options, values);
        if (groupName.isEmpty()) {
            myGroupToFields.putValue(title, fieldName);
        }
    }

    @Override
    protected void initTables() {
        for (Map.Entry<CodeStyleSettingPresentation.SettingsGroup, List<CodeStyleSettingPresentation>> entry
            : CodeStyleSettingPresentation.getStandardSettings(getSettingsType()).entrySet()) {
            CodeStyleSettingPresentation.SettingsGroup group = entry.getKey();
            for (CodeStyleSettingPresentation setting : entry.getValue()) {
                String fieldName = setting.getFieldName();
                LocalizeValue uiName = setting.getUiName();
                if (setting instanceof CodeStyleBoundedIntegerSettingPresentation intSetting) {
                    int defaultValue = intSetting.getDefaultValue();
                    addOption(
                        fieldName,
                        uiName,
                        group.name,
                        intSetting.getLowerBound(),
                        intSetting.getUpperBound(),
                        defaultValue,
                        getDefaultIntValueRenderer(fieldName)
                    );
                }
                else if (setting instanceof CodeStyleSelectSettingPresentation selectSetting) {
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

    private Function<Integer, LocalizeValue> getDefaultIntValueRenderer(String fieldName) {
        if ("RIGHT_MARGIN".equals(fieldName)) {
            return integer -> MarginOptionsUtil.getDefaultRightMarginText(getSettings());
        }
        else {
            return integer -> ApplicationLocalize.integerFieldValueDefault();
        }
    }

    protected SettingsGroup getAssociatedSettingsGroup(String fieldName) {
        if (myFieldNameToGroup == null) {
            myFieldNameToGroup = new HashMap<>();
            for (LocalizeValue group : myGroupToFields.keySet()) {
                Collection<String> fields = myGroupToFields.get(group);
                SettingsGroup settingsGroup = new SettingsGroup(group, fields);
                for (String field : fields) {
                    myFieldNameToGroup.put(field, settingsGroup);
                }
            }
        }
        return myFieldNameToGroup.get(fieldName);
    }

    @Override
    protected LocalizeValue getTabTitle() {
        return CodeStyleLocalize.wrappingAndBraces();
    }

    protected static class SettingsGroup {
        public final LocalizeValue myTitle;
        public final Collection<String> myCommonCodeStyleSettingFieldNames;

        public SettingsGroup(LocalizeValue title, Collection<String> commonCodeStyleSettingFieldNames) {
            myTitle = title;
            myCommonCodeStyleSettingFieldNames = commonCodeStyleSettingFieldNames;
        }
    }

    private void addSoftMarginsOption(String optionName, LocalizeValue title, LocalizeValue groupName) {
        Language language = getDefaultLanguage();
        if (language != null) {
            addCustomOption(new SoftMarginsOption(language, optionName, title, groupName));
        }
    }

    private static class SoftMarginsOption extends Option {
        private final Language myLanguage;

        protected SoftMarginsOption(Language language, String optionName, LocalizeValue title, LocalizeValue groupName) {
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
        if (value instanceof List list && !list.isEmpty() && list.get(0) instanceof Integer) {
            //noinspection unchecked
            return (List<Integer>) value;
        }
        return Collections.emptyList();
    }

    @Override
    protected @Nullable JComponent getCustomValueRenderer(String optionName, Object value) {
        if (CodeStyleSoftMarginsPresentation.OPTION_NAME.equals(optionName)) {
            JLabel softMarginsLabel = new JLabel(getSoftMarginsString(castToIntList(value)).get());
            UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, softMarginsLabel);
            return softMarginsLabel;
        }
        else if ("WRAP_ON_TYPING".equals(optionName)) {
            if (value.equals(CodeStyleLocalize.wrappingWrapOnTypingDefault())) {
                JLabel wrapLabel = new JLabel(MarginOptionsUtil.getDefaultWrapOnTypingText(getSettings()).get());
                UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, wrapLabel);
                return wrapLabel;
            }
        }
        return super.getCustomValueRenderer(optionName, value);
    }

    private LocalizeValue getSoftMarginsString(List<Integer> intList) {
        if (intList.size() > 0) {
            return LocalizeValue.of(CommaSeparatedIntegersValueEditor.intListToString(intList));
        }
        return MarginOptionsUtil.getDefaultVisualGuidesText(getSettings());
    }

    @Override
    protected @Nullable JComponent getCustomNodeEditor(MyTreeNode node) {
        String optionName = node.getKey().getOptionName();
        if (CodeStyleSoftMarginsPresentation.OPTION_NAME.equals(optionName)) {
            mySoftMarginsEditor.setValue(castToIntList(node.getValue()));
            return mySoftMarginsEditor;
        }
        else if ("WRAP_ON_TYPING".equals(optionName)) {
            if (node.getValue() instanceof LocalizeValue locValue) {
                for (int i = 0; i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS.length; i++) {
                    if (CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS[i].equals(locValue)) {
                        myWrapOnTypingCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            return myWrapOnTypingCombo;
        }
        return super.getCustomNodeEditor(node);
    }

    @Override
    protected @Nullable Object getCustomNodeEditorValue(JComponent customEditor) {
        if (customEditor instanceof CommaSeparatedIntegersField commaSeparatedIntegersField) {
            return commaSeparatedIntegersField.getValue();
        }
        else if (customEditor == myWrapOnTypingCombo) {
            int i = myWrapOnTypingCombo.getSelectedIndex();
            return i >= 0 ? CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS[i] : null;
        }
        return super.getCustomNodeEditorValue(customEditor);
    }
}