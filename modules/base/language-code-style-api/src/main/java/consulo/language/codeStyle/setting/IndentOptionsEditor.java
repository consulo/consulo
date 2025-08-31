/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.codeStyle.setting;

import consulo.language.codeStyle.CodeStyleBundle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.ui.ex.awt.IntegerField;
import consulo.ui.ex.awt.OptionGroup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

import static consulo.language.codeStyle.CodeStyleConstraints.*;
import static consulo.language.codeStyle.CodeStyleDefaults.DEFAULT_INDENT_SIZE;
import static consulo.language.codeStyle.CodeStyleDefaults.DEFAULT_TAB_SIZE;
import static consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS;

@SuppressWarnings({"Duplicates", "deprecation", "DeprecatedIsStillUsed"})
public class IndentOptionsEditor extends OptionGroup implements CodeStyleSettingsCustomizable {
  private static final String INDENT_LABEL = CodeStyleBundle.message("editbox.indent.indent");
  private static final String TAB_SIZE_LABEL = CodeStyleBundle.message("editbox.indent.tab.size");

  @Deprecated
  protected JTextField myIndentField;

  @Deprecated
  protected JCheckBox myCbUseTab;

  @Deprecated
  protected JTextField myTabSizeField;

  @Deprecated
  protected JLabel myTabSizeLabel;

  @Deprecated
  protected JLabel myIndentLabel;

  @Nullable
  private final LanguageCodeStyleSettingsProvider myProvider;

  public IndentOptionsEditor() {
    this(null);
  }

  /**
   * @param provider The provider which will be used to customize the indent options editor. If {@code null} is passed, no customization
   *                 will be carried out and thus all the available options will be shown.
   */
  public IndentOptionsEditor(@Nullable LanguageCodeStyleSettingsProvider provider) {
    myProvider = provider;
  }

  @Override
  public JPanel createPanel() {
    addComponents();
    if (myProvider != null) {
      myProvider.customizeSettings(this, INDENT_SETTINGS);
    }
    return super.createPanel();
  }

  protected void addComponents() {
    addTabOptions();
    addTabSizeField();
    addIndentField();
  }

  protected void addIndentField() {
    myIndentField = createIndentTextField(INDENT_LABEL, MIN_INDENT_SIZE, MAX_INDENT_SIZE, DEFAULT_INDENT_SIZE);
    myIndentLabel = new JLabel(INDENT_LABEL);
    add(myIndentLabel, myIndentField);
  }

  protected void addTabSizeField() {
    myTabSizeField = createIndentTextField(TAB_SIZE_LABEL, MIN_TAB_SIZE, MAX_TAB_SIZE, DEFAULT_TAB_SIZE);
    myTabSizeLabel = new JLabel(TAB_SIZE_LABEL);
    add(myTabSizeLabel, myTabSizeField);
  }

  /**
   * @deprecated Use {@link #createIndentTextField(String, int, int, int)}
   */
  @Deprecated
  public JTextField createIndentTextField() {
    return createIndentTextField(null, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
  }

  public IntegerField createIndentTextField(@Nullable String valueName, int minSize, int maxSize, int defaultValue) {
    IntegerField field = new IntegerField(valueName, minSize, maxSize);
    field.setDefaultValue(defaultValue);
    field.setColumns(4);
    if (defaultValue < 0) field.setCanBeEmpty(true);
    field.setMinimumSize(field.getPreferredSize());
    return field;
  }

  protected void addTabOptions() {
    myCbUseTab = new JCheckBox(CodeStyleBundle.message("checkbox.indent.use.tab.character"));
    add(myCbUseTab);
  }

  @Override
  public void showAllStandardOptions() {
    setVisible(true);
  }

  @Override
  public void showStandardOptions(String... optionNames) {
    setVisible(false);
    for (String optionName : optionNames) {
      if (IndentOption.INDENT_SIZE.toString().equals(optionName)) {
        myIndentLabel.setVisible(true);
        myIndentField.setVisible(true);
      }
      else if (IndentOption.TAB_SIZE.toString().equals(optionName)) {
        myTabSizeField.setVisible(true);
        myTabSizeLabel.setVisible(true);
      }
      else if (IndentOption.USE_TAB_CHARACTER.toString().equals(optionName)) {
        myCbUseTab.setVisible(true);
      }
    }
  }


  protected static boolean isFieldModified(JCheckBox checkBox, boolean value) {
    return checkBox.isSelected() != value;
  }

  protected static boolean isFieldModified(JTextField textField, int value) {
    if (textField instanceof IntegerField) return ((IntegerField)textField).getValue() != value;
    try {
      int fieldValue = Integer.parseInt(textField.getText().trim());
      return fieldValue != value;
    }
    catch (NumberFormatException e) {
      return false;
    }
  }

  public boolean isModified(CodeStyleSettings settings, CommonCodeStyleSettings.IndentOptions options) {
    boolean isModified;
    isModified = isFieldModified(myTabSizeField, options.TAB_SIZE);
    isModified |= isFieldModified(myCbUseTab, options.USE_TAB_CHARACTER);
    isModified |= isFieldModified(myIndentField, options.INDENT_SIZE);

    return isModified;
  }

  protected int getUIIndent() {
    assert myIndentField instanceof IntegerField;
    return ((IntegerField)myIndentField).getValue();
  }

  protected int getUITabSize() {
    assert myTabSizeField instanceof IntegerField;
    return ((IntegerField)myTabSizeField).getValue();
  }

  public void apply(CodeStyleSettings settings, CommonCodeStyleSettings.IndentOptions options) {
    options.INDENT_SIZE = getUIIndent();
    options.TAB_SIZE = getUITabSize();
    options.USE_TAB_CHARACTER = myCbUseTab.isSelected();
  }

  public void reset(@Nonnull CodeStyleSettings settings, @Nonnull CommonCodeStyleSettings.IndentOptions options) {
    ((IntegerField)myTabSizeField).setValue(options.TAB_SIZE);
    myCbUseTab.setSelected(options.USE_TAB_CHARACTER);

    ((IntegerField)myIndentField).setValue(options.INDENT_SIZE);
  }

  /**
   * @deprecated Create {@link IntegerField} and use {@link IntegerField#getValue()} instead.
   */
  @Deprecated
  protected int getFieldValue(JTextField field, int minValue, int defValue) {
    if (field instanceof IntegerField) {
      return ((IntegerField)field).getValue();
    }
    else {
      try {
        return Math.max(Integer.parseInt(field.getText()), minValue);
      }
      catch (NumberFormatException e) {
        return defValue;
      }
    }
  }

  public void setEnabled(boolean enabled) {
    myIndentField.setEnabled(enabled);
    myIndentLabel.setEnabled(enabled);
    myTabSizeField.setEnabled(enabled);
    myTabSizeLabel.setEnabled(enabled);
    myCbUseTab.setEnabled(enabled);
  }

  protected void setVisible(boolean visible) {
    myIndentField.setVisible(visible);
    myIndentLabel.setVisible(visible);
    myTabSizeField.setVisible(visible);
    myTabSizeLabel.setVisible(visible);
    myCbUseTab.setVisible(visible);
  }

}
