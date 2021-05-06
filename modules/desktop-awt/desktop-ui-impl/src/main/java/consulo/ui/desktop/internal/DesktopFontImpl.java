/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.util.ui.JBUI;
import consulo.ui.font.Font;
import consulo.util.lang.BitUtil;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public final class DesktopFontImpl implements Font {
  private final java.awt.Font myFont;
  private final int myFontStyle;

  public DesktopFontImpl(@Nonnull String fontName, int fontSize, int fontStyle) {
    myFontStyle = fontStyle;

    int style = 0;
    style = BitUtil.set(style, java.awt.Font.PLAIN, BitUtil.isSet(fontStyle, Font.STYLE_PLAIN));
    style = BitUtil.set(style, java.awt.Font.BOLD, BitUtil.isSet(fontStyle, Font.STYLE_BOLD));
    style = BitUtil.set(style, java.awt.Font.ITALIC, BitUtil.isSet(fontStyle, Font.STYLE_ITALIC));

    myFont = new java.awt.Font(fontName, style, JBUI.scaleFontSize(fontSize));
  }

  public DesktopFontImpl(java.awt.Font font) {
    myFont = font;

    int result = 0;
    result = BitUtil.set(result, Font.STYLE_PLAIN, BitUtil.isSet(font.getStyle(), java.awt.Font.PLAIN));
    result = BitUtil.set(result, Font.STYLE_BOLD, BitUtil.isSet(font.getStyle(), java.awt.Font.BOLD));
    result = BitUtil.set(result, Font.STYLE_ITALIC, BitUtil.isSet(font.getStyle(), java.awt.Font.ITALIC));

    myFontStyle = result;
  }

  @Nonnull
  public java.awt.Font getFont() {
    return myFont;
  }

  @Nonnull
  @Override
  public String getName() {
    return myFont.getFontName();
  }

  @Nonnull
  @Override
  public String getFontName() {
    return myFont.getFontName();
  }

  @Nonnull
  @Override
  public String getFamily() {
    return myFont.getFamily();
  }

  @Override
  public int getFontStyle() {
    return myFontStyle;
  }

  @Override
  public int getFontSize() {
    return myFont.getSize();
  }

  @Nonnull
  @Override
  public Font buildNewFont(int newSize) {
    java.awt.Font newFont = myFont.deriveFont((float)newSize);
    return new DesktopFontImpl(newFont);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DesktopFontImpl that = (DesktopFontImpl)o;
    return Objects.equals(myFont, that.myFont);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myFont);
  }

  @Override
  public String toString() {
    return myFont.toString();
  }
}
