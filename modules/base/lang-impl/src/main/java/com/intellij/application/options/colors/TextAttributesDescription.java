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

/**
 * @author cdr
 */
package com.intellij.application.options.colors;

import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;

public abstract class TextAttributesDescription extends ColorAndFontDescription {
  private final TextAttributes myAttributes;

  public TextAttributesDescription(LocalizeValue name, LocalizeValue group, TextAttributes attributes, TextAttributesKey type, EditorColorsScheme scheme, Image icon, final String toolTip) {
    super(name, group, type == null ? null : type.getExternalName(), scheme, icon, toolTip);
    myAttributes = attributes;
    initCheckedStatus();
  }

  @Override
  public int getFontType() {
    return myAttributes.getFontType();
  }

  @Override
  public void setFontType(int type) {
    myAttributes.setFontType(type);
  }

  @Override
  public ColorValue getExternalEffectColor() {
    return myAttributes.getEffectColor();
  }

  @Override
  public EffectType getExternalEffectType() {
    return myAttributes.getEffectType();
  }

  @Override
  public void setExternalEffectColor(ColorValue color) {
    myAttributes.setEffectColor(color);
  }

  @Override
  public void setExternalEffectType(EffectType type) {
    myAttributes.setEffectType(type);
  }

  @Override
  public ColorValue getExternalForeground() {
    return myAttributes.getForegroundColor();
  }

  @Override
  public void setExternalForeground(ColorValue col) {
    myAttributes.setForegroundColor(col);
  }

  @Override
  public ColorValue getExternalBackground() {
    return myAttributes.getBackgroundColor();
  }

  @Override
  public ColorValue getExternalErrorStripe() {
    return myAttributes.getErrorStripeColor();
  }

  @Override
  public void setExternalBackground(ColorValue col) {
    myAttributes.setBackgroundColor(col);
  }

  @Override
  public void setExternalErrorStripe(ColorValue col) {
    myAttributes.setErrorStripeColor(col);
  }

  protected TextAttributes getTextAttributes() {
    return myAttributes;
  }
}
