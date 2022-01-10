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
package com.intellij.application.options.colors;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.EventDispatcher;
import com.intellij.util.FontUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.awt.TargetAWT;
import consulo.localize.LocalizeValue;
import consulo.ui.ColorBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author cdr
 */
public class ColorAndFontDescriptionPanel implements OptionsPanelImpl.ColorDescriptionPanel {
  private EventDispatcher<Listener> myListeners = EventDispatcher.create(Listener.class);

  private JPanel myPanel;

  private ColorBox myBackgroundChooser;
  private ColorBox myForegroundChooser;
  private ColorBox myEffectsColorChooser;
  private ColorBox myErrorStripeColorChooser;

  private JBCheckBox myCbBackground;
  private JBCheckBox myCbForeground;
  private JBCheckBox myCbEffects;
  private JBCheckBox myCbErrorStripe;

  private Map<String, EffectType> myEffectsMap;

  {
    Map<String, EffectType> map = ContainerUtil.newLinkedHashMap();
    map.put(ApplicationBundle.message("combobox.effect.underscored"), EffectType.LINE_UNDERSCORE);
    map.put(ApplicationBundle.message("combobox.effect.boldunderscored"), EffectType.BOLD_LINE_UNDERSCORE);
    map.put(ApplicationBundle.message("combobox.effect.underwaved"), EffectType.WAVE_UNDERSCORE);
    map.put(ApplicationBundle.message("combobox.effect.bordered"), EffectType.BOXED);
    map.put(ApplicationBundle.message("combobox.effect.strikeout"), EffectType.STRIKEOUT);
    map.put(ApplicationBundle.message("combobox.effect.bold.dottedline"), EffectType.BOLD_DOTTED_LINE);
    myEffectsMap = Collections.unmodifiableMap(map);
  }

  private JComboBox<String> myEffectsCombo;
  private final EffectsComboModel myEffectsModel;

  private JBCheckBox myCbBold;
  private JBCheckBox myCbItalic;
  private JTextPane myInheritanceLabel;

  private JBCheckBox myInheritAttributesBox;

  public ColorAndFontDescriptionPanel() {
    myPanel = new JPanel(new VerticalLayout(5));

    myCbBold = new JBCheckBox(ApplicationBundle.message("checkbox.font.bold"));
    myCbItalic = new JBCheckBox(ApplicationBundle.message("checkbox.font.italic"));

    JPanel leftFontLine = new JPanel(new HorizontalLayout(0));
    leftFontLine.add(myCbBold);
    leftFontLine.add(myCbItalic);
    myPanel.add(new BorderLayoutPanel().addToRight(leftFontLine));

    myCbForeground = new JBCheckBox(ApplicationBundle.message("checkbox.color.foreground"));
    myForegroundChooser = ColorBox.create();
    myCbBackground = new JBCheckBox(ApplicationBundle.message("checkbox.color.background"));
    myBackgroundChooser = ColorBox.create();
    myCbErrorStripe = new JBCheckBox(ApplicationBundle.message("checkbox.color.error.stripe.mark"));
    myErrorStripeColorChooser = ColorBox.create();
    myCbEffects = new JBCheckBox(ApplicationBundle.message("checkbox.color.effects"));
    myEffectsColorChooser = ColorBox.create();

    FormBuilder fontColorOptions = FormBuilder.createFormBuilder();
    fontColorOptions.addLabeledComponent(myCbForeground, (JComponent)TargetAWT.to(myForegroundChooser));
    fontColorOptions.addLabeledComponent(myCbBackground, (JComponent)TargetAWT.to(myBackgroundChooser));
    fontColorOptions.addLabeledComponent(myCbErrorStripe, (JComponent)TargetAWT.to(myErrorStripeColorChooser));
    fontColorOptions.addLabeledComponent(myCbEffects, (JComponent)TargetAWT.to(myEffectsColorChooser));
    myPanel.add(fontColorOptions.getPanel());

    myEffectsCombo = new ComboBox<>();
    myEffectsModel = new EffectsComboModel(ContainerUtil.newArrayList(myEffectsMap.keySet()));
    myEffectsCombo.setModel(myEffectsModel);
    myEffectsCombo.setRenderer(new ColoredListCellRenderer<String>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList list, String value, int index, boolean selected, boolean hasFocus) {
        append(value != null ? value : "<invalid>");
      }
    });

    myPanel.add(new BorderLayoutPanel().addToRight(myEffectsCombo));

    myInheritAttributesBox = new JBCheckBox(ApplicationBundle.message("label.inherit.attributes"));
    myPanel.add(myInheritAttributesBox);
    myInheritanceLabel = new JTextPane();
    myPanel.add(myInheritanceLabel);

    ActionListener actionListener = e -> onSettingsChanged();
    for (JBCheckBox c : new JBCheckBox[]{myCbBackground, myCbForeground, myCbEffects, myCbErrorStripe, myCbItalic, myCbBold, myInheritAttributesBox}) {
      c.addActionListener(actionListener);
    }
    for (ColorBox c : new ColorBox[]{myBackgroundChooser, myForegroundChooser, myEffectsColorChooser, myErrorStripeColorChooser}) {
      c.addValueListener(it -> onSettingsChanged());
    }
    myEffectsCombo.addActionListener(actionListener);
    Messages.configureMessagePaneUi(myInheritanceLabel, "<html>", null);
    myInheritanceLabel.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        onHyperLinkClicked(e);
      }
    });
    myInheritanceLabel.setBorder(BorderFactory.createEmptyBorder());
  }

  protected void onHyperLinkClicked(HyperlinkEvent e) {
    myListeners.getMulticaster().onHyperLinkClicked(e);
  }

  @RequiredUIAccess
  protected void onSettingsChanged() {
    myErrorStripeColorChooser.setEnabled(myCbErrorStripe.isSelected());
    myForegroundChooser.setEnabled(myCbForeground.isSelected());
    myBackgroundChooser.setEnabled(myCbBackground.isSelected());
    myEffectsColorChooser.setEnabled(myCbEffects.isSelected());
    myEffectsCombo.setEnabled(myCbEffects.isSelected());

    myListeners.getMulticaster().onSettingsChanged();
  }

  @Nonnull
  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  @RequiredUIAccess
  public void resetDefault() {
    myCbBold.setSelected(false);
    myCbBold.setEnabled(false);
    myCbItalic.setSelected(false);
    myCbItalic.setEnabled(false);
    updateColorChooser(myCbForeground, myForegroundChooser, false, false, null);
    updateColorChooser(myCbBackground, myBackgroundChooser, false, false, null);
    updateColorChooser(myCbErrorStripe, myErrorStripeColorChooser, false, false, null);
    updateColorChooser(myCbEffects, myEffectsColorChooser, false, false, null);
    myEffectsCombo.setEnabled(false);
    myInheritanceLabel.setVisible(false);
    myInheritAttributesBox.setVisible(false);
  }

  @Override
  public void reset(@Nonnull EditorSchemeAttributeDescriptor d) {
    if (!(d instanceof ColorAndFontDescription)) {
      return;
    }

    ColorAndFontDescription description = (ColorAndFontDescription)d;

    if (description.isFontEnabled()) {
      myCbBold.setEnabled(true);
      myCbItalic.setEnabled(true);
      int fontType = description.getFontType();
      myCbBold.setSelected((fontType & Font.BOLD) != 0);
      myCbItalic.setSelected((fontType & Font.ITALIC) != 0);
    }
    else {
      myCbBold.setSelected(false);
      myCbBold.setEnabled(false);
      myCbItalic.setSelected(false);
      myCbItalic.setEnabled(false);
    }

    updateColorChooser(myCbForeground, myForegroundChooser, description.isForegroundEnabled(), description.isForegroundChecked(), description.getForegroundColor());

    updateColorChooser(myCbBackground, myBackgroundChooser, description.isBackgroundEnabled(), description.isBackgroundChecked(), description.getBackgroundColor());

    updateColorChooser(myCbErrorStripe, myErrorStripeColorChooser, description.isErrorStripeEnabled(), description.isErrorStripeChecked(), description.getErrorStripeColor());

    EffectType effectType = description.getEffectType();
    updateColorChooser(myCbEffects, myEffectsColorChooser, description.isEffectsColorEnabled(), description.isEffectsColorChecked(), description.getEffectColor());

    if (description.isEffectsColorEnabled() && description.isEffectsColorChecked()) {
      myEffectsCombo.setEnabled(true);
      myEffectsModel.setEffectName(ContainerUtil.reverseMap(myEffectsMap).get(effectType));
    }
    else {
      myEffectsCombo.setEnabled(false);
    }
    setInheritanceInfo(description);
  }

  @Override
  public void apply(@Nonnull EditorSchemeAttributeDescriptor d, EditorColorsScheme scheme) {
    if (!(d instanceof ColorAndFontDescription)) {
      return;
    }

    ColorAndFontDescription description = (ColorAndFontDescription)d;

    description.setInherited(myInheritAttributesBox.isSelected());
    if (description.isInherited()) {
      TextAttributes baseAttributes = description.getBaseAttributes();
      if (baseAttributes != null) {
        description.setFontType(baseAttributes.getFontType());
        description.setForegroundChecked(baseAttributes.getForegroundColor() != null);
        description.setForegroundColor(baseAttributes.getForegroundColor());
        description.setBackgroundChecked(baseAttributes.getBackgroundColor() != null);
        description.setBackgroundColor(baseAttributes.getBackgroundColor());
        description.setErrorStripeChecked(baseAttributes.getErrorStripeColor() != null);
        description.setErrorStripeColor(baseAttributes.getErrorStripeColor());
        description.setEffectColor(baseAttributes.getEffectColor());
        description.setEffectType(baseAttributes.getEffectType());
        description.setEffectsColorChecked(baseAttributes.getEffectColor() != null);
      }
      else {
        description.setInherited(false);
      }
      reset(description);
    }
    else {
      setInheritanceInfo(description);
      int fontType = Font.PLAIN;
      if (myCbBold.isSelected()) fontType |= Font.BOLD;
      if (myCbItalic.isSelected()) fontType |= Font.ITALIC;
      description.setFontType(fontType);
      description.setForegroundChecked(myCbForeground.isSelected());
      description.setForegroundColor(myForegroundChooser.getValueOrError());
      description.setBackgroundChecked(myCbBackground.isSelected());
      description.setBackgroundColor(myBackgroundChooser.getValueOrError());
      description.setErrorStripeChecked(myCbErrorStripe.isSelected());
      description.setErrorStripeColor(myErrorStripeColorChooser.getValueOrError());
      description.setEffectsColorChecked(myCbEffects.isSelected());
      description.setEffectColor(myEffectsColorChooser.getValueOrError());

      if (myEffectsCombo.isEnabled()) {
        String effectType = (String)myEffectsCombo.getModel().getSelectedItem();
        description.setEffectType(myEffectsMap.get(effectType));
      }
    }
    description.apply(scheme);
  }

  @Override
  public void addListener(@Nonnull Listener listener) {
    myListeners.addListener(listener);
  }

  @RequiredUIAccess
  private static void updateColorChooser(JCheckBox checkBox, ColorBox colorPanel, boolean isEnabled, boolean isChecked, @Nullable ColorValue color) {
    checkBox.setEnabled(isEnabled);
    checkBox.setSelected(isChecked);
    if (color != null) {
      colorPanel.setValue(color, false);
    }
    else {
      colorPanel.setValue(StandardColors.WHITE, false);
    }
    colorPanel.setEnabled(isChecked);
  }

  @RequiredUIAccess
  private void setInheritanceInfo(ColorAndFontDescription description) {
    Pair<ColorSettingsPage, AttributesDescriptor> baseDescriptor = description.getBaseAttributeDescriptor();
    if (baseDescriptor != null) {
      LocalizeValue attrName = baseDescriptor.second.getDisplayName();
      String attrLabel = attrName.get().replaceAll(ColorOptionsTree.NAME_SEPARATOR, FontUtil.rightArrow(UIUtil.getLabelFont()));
      ColorSettingsPage settingsPage = baseDescriptor.first;
      String style = "<div style=\"text-align:right\" vertical-align=\"top\">";
      String tooltipText;
      String labelText;
      if (settingsPage != null) {
        String pageName = settingsPage.getDisplayName();
        tooltipText = "'" + attrLabel + "' from<br>'" + pageName + "' section";
        labelText = style + "'" + attrLabel + "'<br>of <a href=\"" + attrName + "\">" + pageName;
      }
      else {
        tooltipText = attrLabel;
        labelText = style + attrLabel + "<br>&nbsp;";
      }

      myInheritanceLabel.setVisible(true);
      myInheritanceLabel.setText(labelText);
      myInheritanceLabel.setToolTipText(tooltipText);
      myInheritanceLabel.setEnabled(true);
      myInheritAttributesBox.setVisible(true);
      myInheritAttributesBox.setEnabled(true);
      myInheritAttributesBox.setSelected(description.isInherited());
      setEditEnabled(!description.isInherited(), description);
    }
    else {
      myInheritanceLabel.setVisible(false);
      myInheritAttributesBox.setSelected(false);
      myInheritAttributesBox.setVisible(false);
      setEditEnabled(true, description);
    }
  }

  @RequiredUIAccess
  private void setEditEnabled(boolean isEditEnabled, ColorAndFontDescription description) {
    myCbBackground.setEnabled(isEditEnabled && description.isBackgroundEnabled());
    myCbForeground.setEnabled(isEditEnabled && description.isForegroundEnabled());
    myCbBold.setEnabled(isEditEnabled && description.isFontEnabled());
    myCbItalic.setEnabled(isEditEnabled && description.isFontEnabled());
    myCbEffects.setEnabled(isEditEnabled && description.isEffectsColorEnabled());
    myCbErrorStripe.setEnabled(isEditEnabled && description.isErrorStripeEnabled());
    myErrorStripeColorChooser.setEditable(isEditEnabled);
    myEffectsColorChooser.setEditable(isEditEnabled);
    myForegroundChooser.setEditable(isEditEnabled);
    myBackgroundChooser.setEditable(isEditEnabled);
  }

  private static class EffectsComboModel extends CollectionComboBoxModel<String> {
    public EffectsComboModel(List<String> names) {
      super(names);
    }

    /**
     * Set the current effect name when a text attribute selection changes without notifying the listeners since otherwise it will
     * be considered as an actual change and lead to unnecessary evens including 'read-only scheme' check.
     *
     * @param effectName
     */
    public void setEffectName(@Nonnull String effectName) {
      mySelection = effectName;
    }
  }
}
