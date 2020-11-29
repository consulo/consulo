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
package com.intellij.openapi.editor.colors;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.FontSize;
import consulo.ui.color.ColorValue;
import consulo.util.pointers.Named;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Map;

public interface EditorColorsScheme extends Cloneable, TextAttributesScheme, Named {
  String DEFAULT_SCHEME_NAME = "Default";

  void setName(String name);

  void setAttributes(TextAttributesKey key, TextAttributes attributes);

  @Nonnull
  ColorValue getDefaultBackground();

  @Nonnull
  ColorValue getDefaultForeground();

  @Nullable
  ColorValue getColor(EditorColorKey key);

  void setColor(EditorColorKey key, ColorValue color);

  void fillColors(Map<EditorColorKey, ColorValue> colors);

  /**
   * The IDE has allowed to configure only a single font family for a while. However, that doesn't handle a situation when
   * that font family is unable to display particular char - fallback font family was chosen randomly from the whole collection
   * of all registered fonts.
   * <p>
   * Now it's possible to specify more than one font, i.e. directly indicated 'fallback fonts sequence' (see {@link FontPreferences}).
   * However, old single font-based API is still here in order to preserve backward compatibility ({@link #getEditorFontName()} and
   * {@link #getEditorFontSize()}). I.e. those methods are just re-written in order to use {@link FontPreferences} object exposed
   * by this method.
   *
   * @return font preferences to use
   */
  @Nonnull
  FontPreferences getFontPreferences();

  void setFontPreferences(@Nonnull FontPreferences preferences);

  String getEditorFontName();

  void setEditorFontName(String fontName);

  int getEditorFontSize();

  /**
   * @return editor font size with scaling
   */
  int getEditorFontSize(boolean scale);

  void setEditorFontSize(int fontSize);

  FontSize getQuickDocFontSize();

  void setQuickDocFontSize(@Nonnull FontSize fontSize);

  Font getFont(EditorFontType key);

  void setFont(EditorFontType key, Font font);

  float getLineSpacing();

  void setLineSpacing(float lineSpacing);

  EditorColorsScheme clone();

  /**
   * @return console font preferences to use
   * @see #getFontPreferences()
   */
  @Nonnull
  FontPreferences getConsoleFontPreferences();

  void setConsoleFontPreferences(@Nonnull FontPreferences preferences);

  String getConsoleFontName();

  void setConsoleFontName(String fontName);

  /**
   * @return console font size with scaling
   */
  int getConsoleFontSize();

  int getConsoleFontSize(boolean scale);

  void setConsoleFontSize(int fontSize);

  float getConsoleLineSpacing();

  void setConsoleLineSpacing(float lineSpacing);

  void readExternal(Element parentNode);
}
