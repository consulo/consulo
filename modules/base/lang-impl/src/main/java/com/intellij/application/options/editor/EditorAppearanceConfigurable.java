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

package com.intellij.application.options.editor;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.codeInsight.hints.settings.ParameterNameHintsConfigurable;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.ui.components.JBCheckBox;
import consulo.ide.settings.impl.EditorGeneralConfigurable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * To provide additional options in Editor | Appearance section register implementation of {@link com.intellij.openapi.options.UnnamedConfigurable} in the plugin.xml:
 * <p/>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;editorAppearanceConfigurable instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 *
 * @author yole
 */
public class EditorAppearanceConfigurable extends CompositeConfigurable<UnnamedConfigurable> implements Configurable {
  private static final ExtensionPointName<EditorAppearanceConfigurableEP> EP_NAME = ExtensionPointName.create("com.intellij.editorAppearanceConfigurable");
  private JPanel myRootPanel;
  private JCheckBox myCbBlinkCaret;
  private JCheckBox myCbBlockCursor;
  private JCheckBox myCbRightMargin;
  private JCheckBox myCbShowLineNumbers;
  private JCheckBox myCbShowWhitespaces;
  private JTextField myBlinkIntervalField;
  private JPanel myAddonPanel;
  private JCheckBox myCbShowMethodSeparators;
  private JCheckBox myShowVerticalIndentGuidesCheckBox;
  private JBCheckBox myShowParameterNameHints;
  private JButton myConfigureParameterHintsButton;
  private JPanel myParameterHintsSettingsPanel;

  public EditorAppearanceConfigurable() {
    myCbBlinkCaret.addActionListener(event -> myBlinkIntervalField.setEnabled(myCbBlinkCaret.isSelected()));
    initInlaysPanel();
  }

  private void initInlaysPanel() {
    boolean isInlayProvidersAvailable = InlayParameterHintsProvider.EP.hasAnyExtensions();
    myParameterHintsSettingsPanel.setVisible(isInlayProvidersAvailable);
    if (!isInlayProvidersAvailable) return;

    myConfigureParameterHintsButton.addActionListener(e -> {
      ParameterNameHintsConfigurable configurable = new ParameterNameHintsConfigurable();
      configurable.show();
    });
  }

  private void applyNameHintsSettings() {
    EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
    settings.setShowParameterNameHints(myShowParameterNameHints.isSelected());
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();

    myCbShowMethodSeparators.setSelected(DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS);
    myCbBlinkCaret.setSelected(editorSettings.isBlinkCaret());
    myBlinkIntervalField.setText(Integer.toString(editorSettings.getBlinkPeriod()));
    myBlinkIntervalField.setEnabled(editorSettings.isBlinkCaret());
    myCbRightMargin.setSelected(editorSettings.isRightMarginShown());
    myCbShowLineNumbers.setSelected(editorSettings.isLineNumbersShown());
    myCbBlockCursor.setSelected(editorSettings.isBlockCursor());
    myCbShowWhitespaces.setSelected(editorSettings.isWhitespacesShown());
    myShowVerticalIndentGuidesCheckBox.setSelected(editorSettings.isIndentGuidesShown());

    myShowParameterNameHints.setSelected(editorSettings.isShowParameterNameHints());

    super.reset();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    editorSettings.setBlinkCaret(myCbBlinkCaret.isSelected());
    try {
      editorSettings.setBlinkPeriod(Integer.parseInt(myBlinkIntervalField.getText()));
    }
    catch (NumberFormatException ignored) {
    }

    editorSettings.setBlockCursor(myCbBlockCursor.isSelected());
    editorSettings.setRightMarginShown(myCbRightMargin.isSelected());
    editorSettings.setLineNumbersShown(myCbShowLineNumbers.isSelected());
    editorSettings.setWhitespacesShown(myCbShowWhitespaces.isSelected());
    editorSettings.setIndentGuidesShown(myShowVerticalIndentGuidesCheckBox.isSelected());

    EditorGeneralConfigurable.reinitAllEditors();

    DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS = myCbShowMethodSeparators.isSelected();

    EditorGeneralConfigurable.restartDaemons();

    applyNameHintsSettings();
    super.apply();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    if (super.isModified()) return true;
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    boolean isModified = isModified(myCbBlinkCaret, editorSettings.isBlinkCaret());
    isModified |= isModified(myBlinkIntervalField, editorSettings.getBlinkPeriod());

    isModified |= isModified(myCbBlockCursor, editorSettings.isBlockCursor());

    isModified |= isModified(myCbRightMargin, editorSettings.isRightMarginShown());

    isModified |= isModified(myCbShowLineNumbers, editorSettings.isLineNumbersShown());
    isModified |= isModified(myCbShowWhitespaces, editorSettings.isWhitespacesShown());
    isModified |= isModified(myShowVerticalIndentGuidesCheckBox, editorSettings.isIndentGuidesShown());
    isModified |= isModified(myCbShowMethodSeparators, DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS);
    isModified |= myShowParameterNameHints.isSelected() != editorSettings.isShowParameterNameHints();

    return isModified;
  }

  private static boolean isModified(JToggleButton checkBox, boolean value) {
    return checkBox.isSelected() != value;
  }

  private static boolean isModified(JTextField textField, int value) {
    try {
      int fieldValue = Integer.parseInt(textField.getText().trim());
      return fieldValue != value;
    }
    catch(NumberFormatException e) {
      return false;
    }
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Gutters";
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    for (UnnamedConfigurable provider : getConfigurables()) {
      myAddonPanel.add(provider.createComponent(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                                                                            GridBagConstraints.NONE, new Insets(0,0,15,0), 0,0));
    }
    return myRootPanel;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myAddonPanel.removeAll();
    super.disposeUIResources();
  }

  @Override
  protected List<UnnamedConfigurable> createConfigurables() {
    return ConfigurableWrapper.createConfigurables(EP_NAME);
  }
}
