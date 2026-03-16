// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor;

import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.ui.color.ColorValue;
import org.intellij.lang.annotations.JdkConstants;

import org.jspecify.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class LineExtensionInfo {
  
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

  public LineExtensionInfo(String text, @Nullable ColorValue color, @Nullable EffectType effectType, @Nullable ColorValue effectColor, @JdkConstants.FontStyle int fontType) {
    myText = text;
    myColor = color;
    myEffectType = effectType;
    myEffectColor = effectColor;
    myFontType = fontType;
    myBgColor = null;
  }

  public LineExtensionInfo(String text, TextAttributes attr) {
    myText = text;
    myColor = attr.getForegroundColor();
    myEffectType = attr.getEffectType();
    myEffectColor = attr.getEffectColor();
    myFontType = attr.getFontType();
    myBgColor = attr.getBackgroundColor();
  }

  
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
