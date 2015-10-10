/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeHighlighting;

import com.intellij.icons.AllIcons;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.JBColor;
import com.intellij.util.IconUtil;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.HashMap;
import com.intellij.util.ui.ColorIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class HighlightDisplayLevel {
  private static final Map<HighlightSeverity, HighlightDisplayLevel> ourMap = new HashMap<HighlightSeverity, HighlightDisplayLevel>();

  public static final HighlightDisplayLevel GENERIC_SERVER_ERROR_OR_WARNING =
          new HighlightDisplayLevel(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING, createBoxIcon(CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING));

  public static final HighlightDisplayLevel ERROR =
          new HighlightDisplayLevel(HighlightSeverity.ERROR, createErrorIcon(CodeInsightColors.ERRORS_ATTRIBUTES));

  public static final HighlightDisplayLevel WARNING =
          new HighlightDisplayLevel(HighlightSeverity.WARNING, createBoxIcon(CodeInsightColors.WARNINGS_ATTRIBUTES));

  private static final Icon DO_NOT_SHOW_KEY = createBoxIcon(TextAttributesKey.createTextAttributesKey("DO_NOT_SHOW"));
  public static final HighlightDisplayLevel DO_NOT_SHOW = new HighlightDisplayLevel(HighlightSeverity.INFORMATION, DO_NOT_SHOW_KEY);
  /**
   * use #WEAK_WARNING instead
   */
  @Deprecated
  public static final HighlightDisplayLevel INFO = new HighlightDisplayLevel(HighlightSeverity.INFO, DO_NOT_SHOW.getIcon());
  public static final HighlightDisplayLevel WEAK_WARNING = new HighlightDisplayLevel(HighlightSeverity.WEAK_WARNING, DO_NOT_SHOW.getIcon());

  public static final HighlightDisplayLevel NON_SWITCHABLE_ERROR = new HighlightDisplayLevel(HighlightSeverity.ERROR);

  private Icon myIcon;
  private final HighlightSeverity mySeverity;

  @Nullable
  public static HighlightDisplayLevel find(String name) {
    for (Map.Entry<HighlightSeverity, HighlightDisplayLevel> entry : ourMap.entrySet()) {
      HighlightSeverity severity = entry.getKey();
      HighlightDisplayLevel displayLevel = entry.getValue();
      if (Comparing.strEqual(severity.getName(), name)) {
        return displayLevel;
      }
    }
    return null;
  }

  public static HighlightDisplayLevel find(HighlightSeverity severity) {
    return ourMap.get(severity);
  }

  public HighlightDisplayLevel(@NotNull HighlightSeverity severity, @NotNull Icon icon) {
    this(severity);
    myIcon = icon;
    ourMap.put(mySeverity, this);
  }

  public HighlightDisplayLevel(@NotNull HighlightSeverity severity) {
    mySeverity = severity;
  }


  @Override
  public String toString() {
    return mySeverity.toString();
  }

  @NotNull
  public String getName() {
    return mySeverity.getName();
  }

  public Icon getIcon() {
    return myIcon;
  }

  @NotNull
  public HighlightSeverity getSeverity() {
    return mySeverity;
  }

  public static void registerSeverity(@NotNull HighlightSeverity severity, final TextAttributesKey key, @Nullable Icon icon) {
    Icon severityIcon = icon != null ? icon : createBoxIcon(key);
    final HighlightDisplayLevel level = ourMap.get(severity);
    if (level == null) {
      new HighlightDisplayLevel(severity, severityIcon);
    }
    else {
      level.myIcon = severityIcon;
    }
  }

  public static int getEmptyIconDim() {
    return JBUI.scaleIconSize(12);
  }

  public static Icon createBoxIcon(@NotNull TextAttributesKey key) {
    return new SingleColorIcon(key);
  }

  @NotNull
  private static Icon createErrorIcon(@NotNull TextAttributesKey textAttributesKey) {
    return new SingleColorIcon(textAttributesKey) {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y) {
        IconUtil.colorize(AllIcons.General.InspectionsError, getColor()).paintIcon(c, g, x, y);
      }
    };
  }

  @NotNull
  public static Icon createIconByMask(final Color renderColor) {
    return new MyColorIcon(getEmptyIconDim(), renderColor);
  }

  private static class MyColorIcon extends ColorIcon implements ColoredIcon {
    public MyColorIcon(int size, @NotNull Color color) {
      super(size, color);
    }

    @Override
    public Color getColor() {
      return getIconColor();
    }
  }

  public interface ColoredIcon {
    Color getColor();
  }

  public static class SingleColorIcon extends ColorIcon implements Icon, ColoredIcon {
    private final TextAttributesKey myKey;

    public SingleColorIcon(final TextAttributesKey key) {
      super(getEmptyIconDim(), JBColor.GRAY, true);
      myKey = key;
    }

    @Override
    public Color getIconColor() {
      return getColor();
    }

    @NotNull
    @Override
    public Color getBorderColor() {
      return JBColor.LIGHT_GRAY;
    }

    @Override
    @NotNull
    public Color getColor() {
      return ObjectUtil.notNull(getColorInner(), JBColor.GRAY);
    }

    @Nullable
    public Color getColorInner() {
      final EditorColorsManager manager = EditorColorsManager.getInstance();
      if (manager != null) {
        TextAttributes attributes = manager.getGlobalScheme().getAttributes(myKey);
        Color stripe = attributes.getErrorStripeColor();
        if (stripe != null) return stripe;
        return attributes.getEffectColor();
      }
      TextAttributes defaultAttributes = myKey.getDefaultAttributes();
      if (defaultAttributes == null) defaultAttributes = TextAttributes.ERASE_MARKER;
      return defaultAttributes.getErrorStripeColor();
    }
  }
}
