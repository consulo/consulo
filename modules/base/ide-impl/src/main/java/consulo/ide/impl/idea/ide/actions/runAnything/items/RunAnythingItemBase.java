// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.items;

import consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingGroup;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.util.lang.ObjectUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

import static consulo.ui.ex.SimpleTextAttributes.*;

public class RunAnythingItemBase extends RunAnythingItem {
  @Nonnull
  private final String myCommand;
  @Nullable
  protected final Image myIcon;

  public RunAnythingItemBase(@Nonnull String command, @Nullable Image icon) {
    myCommand = command;
    myIcon = icon;
  }

  @Nonnull
  @Override
  public String getCommand() {
    return myCommand;
  }

  @Nullable
  public String getDescription() {
    return null;
  }

  @Nonnull
  @Override
  public Component createComponent(@Nullable String pattern, boolean isSelected, boolean hasFocus) {
    Component oldComponent = createComponent(isSelected);
    if (oldComponent != null) {
      return oldComponent;
    }

    JPanel component = new JPanel(new BorderLayout());
    Color background = UIUtil.getListBackground(isSelected, true);
    component.setBackground(background);

    SimpleColoredComponent textComponent = new SimpleColoredComponent();
    SpeedSearchUtil
            .appendColoredFragmentForMatcher(StringUtil.notNullize(getCommand()), textComponent, REGULAR_ATTRIBUTES, RunAnythingGroup.RUN_ANYTHING_MATCHER_BUILDER.apply(pattern).build(), background,
                                             isSelected);
    component.add(textComponent, BorderLayout.WEST);
    textComponent.appendTextPadding(20);
    setupIcon(textComponent, myIcon);
    addDescription(component, isSelected);

    return component;
  }

  private void addDescription(@Nonnull JPanel panel, boolean isSelected) {
    String description = getDescription();
    if (description == null) {
      return;
    }

    SimpleColoredComponent descriptionComponent = new SimpleColoredComponent();
    descriptionComponent.append(description, getDescriptionAttributes(isSelected));
    descriptionComponent.setTextAlign(SwingConstants.RIGHT);
    panel.add(descriptionComponent, BorderLayout.CENTER);
  }

  public void setupIcon(@Nonnull SimpleColoredComponent component, @Nullable Image icon) {
    component.setIcon(ObjectUtil.notNull(icon, Image.empty(16)));
    component.setIpad(JBUI.insets(0, 10, 0, 0));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RunAnythingItemBase base = (RunAnythingItemBase)o;

    if (!myCommand.equals(base.myCommand)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myCommand.hashCode();
  }

  protected static void appendDescription(@Nonnull SimpleColoredComponent component, @Nullable String description, @Nonnull Color foreground) {
    if (description != null) {
      SimpleTextAttributes smallAttributes = new SimpleTextAttributes(STYLE_SMALLER, foreground);
      component.append(StringUtil.shortenTextWithEllipsis(description, 40, 0), smallAttributes);
      component.appendTextPadding(660, SwingConstants.RIGHT);
    }
  }

  @Nonnull
  private static SimpleTextAttributes getDescriptionAttributes(boolean isSelected) {
    return new SimpleTextAttributes(STYLE_PLAIN, isSelected ? UIUtil.getListSelectionForeground(true) : UIUtil.getInactiveTextColor());
  }
}
