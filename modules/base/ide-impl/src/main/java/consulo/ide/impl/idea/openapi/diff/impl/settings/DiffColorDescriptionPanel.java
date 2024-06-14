// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.openapi.diff.impl.settings;

import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.diff.localize.DiffLocalize;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontDescription;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontOptions;
import consulo.ide.impl.idea.application.options.colors.EditorSchemeAttributeDescriptor;
import consulo.ide.impl.idea.application.options.colors.OptionsPanelImpl;
import consulo.ide.impl.idea.diff.util.TextDiffTypeFactory;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ui.CheckBox;
import consulo.ui.ColorBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.StandardColors;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

class DiffColorDescriptionPanel implements OptionsPanelImpl.ColorDescriptionPanel {
  private final EventDispatcher<Listener> myDispatcher = EventDispatcher.create(Listener.class);

  private final VerticalLayout myPanel;

  private ColorBox myBackgroundColorPanel;
  private ColorBox myIgnoredColorPanel;
  private ColorBox myStripeMarkColorPanel;
  private CheckBox myInheritIgnoredCheckBox;

  @Nonnull
  private final ColorAndFontOptions myOptions;

  @RequiredUIAccess
  DiffColorDescriptionPanel(@Nonnull ColorAndFontOptions options) {
    myOptions = options;

    myPanel = VerticalLayout.create();

    FormBuilder builder = FormBuilder.create();
    builder.addLabeled(
      Label.create(DiffLocalize.mergeColorOptionsBackgroundColorLabel()),
      myBackgroundColorPanel = ColorBox.create()
    );
    builder.addLabeled(
      Label.create(DiffLocalize.mergeColorOptionsIgnoredColorLabel()),
      myIgnoredColorPanel = ColorBox.create()
    );
    builder.addLabeled(
      Label.create(DiffLocalize.mergeColorOptionsStripeMarkColorLabel()),
      myStripeMarkColorPanel = ColorBox.create()
    );

    myPanel.add(builder.build());

    myInheritIgnoredCheckBox = CheckBox.create(DiffLocalize.optionInheritIgnoredColor());
    myPanel.add(myInheritIgnoredCheckBox);

    myBackgroundColorPanel.addValueListener(event -> onSettingsChanged());
    myIgnoredColorPanel.addValueListener(event -> onSettingsChanged());
    myStripeMarkColorPanel.addValueListener(event -> onSettingsChanged());

    myInheritIgnoredCheckBox.addValueListener(e -> {
      myIgnoredColorPanel.setEnabled(!myInheritIgnoredCheckBox.getValueOrError());

      if (myInheritIgnoredCheckBox.getValueOrError()) {
        myIgnoredColorPanel.setValue(null);
      }
      else {
        ColorValue background = ObjectUtil.notNull(myBackgroundColorPanel.getValue(), StandardColors.WHITE);
        ColorValue gutterBackground = myOptions.getSelectedScheme().getDefaultBackground();
        myIgnoredColorPanel.setValue(TextDiffTypeFactory.getMiddleColor(background, gutterBackground));
      }

      onSettingsChanged();
    });
  }

  @Nonnull
  @Override
  public JComponent getPanel() {
    return (JComponent)TargetAWT.to(myPanel);
  }

  private void onSettingsChanged() {
    myDispatcher.getMulticaster().onSettingsChanged();
  }

  @Override
  @RequiredUIAccess
  public void resetDefault() {
    myBackgroundColorPanel.setEnabled(false);
    myIgnoredColorPanel.setEnabled(false);
    myStripeMarkColorPanel.setEnabled(false);
    myInheritIgnoredCheckBox.setEnabled(false);
    myInheritIgnoredCheckBox.setValue(false);
  }

  @Override
  @RequiredUIAccess
  public void reset(@Nonnull EditorSchemeAttributeDescriptor attrDescription) {
    if (!(attrDescription instanceof ColorAndFontDescription)) return;
    ColorAndFontDescription description = (ColorAndFontDescription)attrDescription;

    ColorValue backgroundColor = getBackgroundColor(description);
    ColorValue ignoredColor = getIgnoredColor(description);
    ColorValue stripeMarkColor = getStripeMarkColor(description);
    boolean inheritIgnored = ignoredColor == null;

    myBackgroundColorPanel.setEnabled(true);
    myIgnoredColorPanel.setEnabled(!inheritIgnored);
    myStripeMarkColorPanel.setEnabled(true);
    myInheritIgnoredCheckBox.setEnabled(true);

    myBackgroundColorPanel.setValue(backgroundColor, false);
    myIgnoredColorPanel.setValue(ignoredColor, false);
    myStripeMarkColorPanel.setValue(stripeMarkColor, false);
    myInheritIgnoredCheckBox.setValue(inheritIgnored);
  }

  @Override
  public void apply(@Nonnull EditorSchemeAttributeDescriptor attrDescription, EditorColorsScheme scheme) {
    if (!(attrDescription instanceof ColorAndFontDescription)) return;
    ColorAndFontDescription description = (ColorAndFontDescription)attrDescription;

    description.setBackgroundChecked(true);
    description.setForegroundChecked(true);
    description.setErrorStripeChecked(true);

    setBackgroundColor(description);
    setIgnoredColor(description);
    setStripeMarkColor(description);

    description.apply(scheme);
  }

  @Override
  public void addListener(@Nonnull Listener listener) {
    myDispatcher.addListener(listener);
  }

  @Nullable
  private static ColorValue getBackgroundColor(@Nonnull TextAttributes attributes) {
    return attributes.getBackgroundColor();
  }

  @Nullable
  private static ColorValue getIgnoredColor(@Nonnull TextAttributes attributes) {
    return attributes.getForegroundColor();
  }

  @Nullable
  private static ColorValue getStripeMarkColor(@Nonnull TextAttributes attributes) {
    return attributes.getErrorStripeColor();
  }

  private void setBackgroundColor(@Nonnull TextAttributes attributes) {
    attributes.setBackgroundColor(myBackgroundColorPanel.getValue());
  }

  private void setIgnoredColor(@Nonnull TextAttributes attributes) {
    attributes.setForegroundColor(myInheritIgnoredCheckBox.getValueOrError() ? null : myIgnoredColorPanel.getValue());
  }

  private void setStripeMarkColor(@Nonnull TextAttributes attributes) {
    attributes.setErrorStripeColor(myStripeMarkColorPanel.getValue());
  }
}
