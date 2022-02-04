// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor;

import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import consulo.ui.color.ColorValue;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class LineExtensionInfo {
  @Nonnull
  private final String myText;
  @Nullable
  private final ColorValue myColor;
  @Nullable
  private final EffectType myEffectType;
  @Nullable
  private final ColorValue myEffectColor;
  @Nullable
  private final ColorValue myBgColor;
  @JdkConstants.FontStyle
  private final int myFontType;

  public LineExtensionInfo(@Nonnull String text, @Nullable ColorValue color, @Nullable EffectType effectType, @Nullable ColorValue effectColor, @JdkConstants.FontStyle int fontType) {
    myText = text;
    myColor = color;
    myEffectType = effectType;
    myEffectColor = effectColor;
    myFontType = fontType;
    myBgColor = null;
  }

  public LineExtensionInfo(@Nonnull String text, @Nonnull TextAttributes attr) {
    myText = text;
    myColor = attr.getForegroundColor();
    myEffectType = attr.getEffectType();
    myEffectColor = attr.getEffectColor();
    myFontType = attr.getFontType();
    myBgColor = attr.getBackgroundColor();
  }

  @Nonnull
  public String getText() {
    return myText;
  }

  @Nullable
  public ColorValue getColor() {
    return myColor;
  }

  @Nullable
  public ColorValue getBgColor() {
    return myBgColor;
  }

  @Nullable
  public EffectType getEffectType() {
    return myEffectType;
  }

  @Nullable
  public ColorValue getEffectColor() {
    return myEffectColor;
  }

  @JdkConstants.FontStyle
  public int getFontType() {
    return myFontType;
  }
}
