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
package consulo.execution.debug.impl.internal.setting;

import consulo.application.util.registry.Registry;
import consulo.execution.debug.XDebuggerBundle;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.IdeaTitledBorder;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public class DataViewsConfigurableUi {
  private JCheckBox enableAutoExpressionsCheckBox;
  private JFormattedTextField valueTooltipDelayTextField;
  private JPanel panel;
  private JCheckBox sortAlphabeticallyCheckBox;
  private JPanel myEditorSettingsPanel;
  private JCheckBox myShowValuesInlineCheckBox;
  private JCheckBox myShowValueTooltipCheckBox;
  private JCheckBox myShowValueTooltipOnCheckBox;
  private JBLabel myTooltipLabel;

  public DataViewsConfigurableUi() {
    UIUtil.configureNumericFormattedTextField(valueTooltipDelayTextField);
  }

  private int getValueTooltipDelay() {
    Object value = valueTooltipDelayTextField.getValue();
    return value instanceof Number ? ((Number)value).intValue() :
           StringUtil.parseInt((String)value, XDebuggerDataViewSettings.DEFAULT_VALUE_TOOLTIP_DELAY);
  }

  @Nonnull
  public JComponent getComponent() {
    return panel;
  }

  public boolean isModified(@Nonnull XDebuggerDataViewSettings settings) {
    return getValueTooltipDelay() != settings.getValueLookupDelay() ||
           sortAlphabeticallyCheckBox.isSelected() != settings.isSortValues() ||
           enableAutoExpressionsCheckBox.isSelected() != settings.isAutoExpressions() ||
           myShowValuesInlineCheckBox.isSelected() != settings.isShowValuesInline() ||
           myShowValueTooltipCheckBox.isSelected() != settings.isValueTooltipAutoShow() ||
           myShowValueTooltipOnCheckBox.isSelected() != settings.isValueTooltipAutoShowOnSelection();
  }

  public void reset(@Nonnull XDebuggerDataViewSettings settings) {
    valueTooltipDelayTextField.setValue(settings.getValueLookupDelay());
    sortAlphabeticallyCheckBox.setSelected(settings.isSortValues());
    enableAutoExpressionsCheckBox.setSelected(settings.isAutoExpressions());
    myShowValuesInlineCheckBox.setSelected(settings.isShowValuesInline());
    myShowValueTooltipCheckBox.setSelected(settings.isValueTooltipAutoShow());
    myShowValueTooltipOnCheckBox.setSelected(settings.isValueTooltipAutoShowOnSelection());
    myTooltipLabel.setText(XDebuggerBundle.message("settings.tooltip.label", Registry.stringValue("ide.forcedShowTooltip")));
  }

  public void apply(@Nonnull XDebuggerDataViewSettings settings) {
    settings.setValueLookupDelay(getValueTooltipDelay());
    settings.setSortValues(sortAlphabeticallyCheckBox.isSelected());
    settings.setAutoExpressions(enableAutoExpressionsCheckBox.isSelected());
    settings.setShowValuesInline(myShowValuesInlineCheckBox.isSelected());
    settings.setValueTooltipAutoShow((myShowValueTooltipCheckBox.isSelected()));
    settings.setValueTooltipAutoShowOnSelection(myShowValueTooltipOnCheckBox.isSelected());
  }

  private void createUIComponents() {
    myEditorSettingsPanel = new JPanel();
    IdeaTitledBorder titledBorder = IdeBorderFactory.createTitledBorder("Editor", false);
    myEditorSettingsPanel.setBorder(titledBorder);
    titledBorder.acceptMinimumSize(myEditorSettingsPanel);
  }
}