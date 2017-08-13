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

/**
 * @author Yura Cangea
 */
package com.intellij.openapi.editor.colors.impl;

import com.intellij.openapi.editor.colors.*;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class DefaultColorsScheme extends AbstractColorsScheme implements ReadOnlyColorsScheme {
  private String myName;

  public DefaultColorsScheme(@NotNull EditorColorsManager editorColorsManager) {
    super(null, editorColorsManager);
  }

  @Override
  @Nullable
  public TextAttributes getAttributes(TextAttributesKey key) {
    if (key != null) {
      TextAttributesKey fallbackKey = key.getFallbackAttributeKey();
      TextAttributes attributes = myAttributesMap.get(key);
      if (fallbackKey == null) {
        if (attributes != null) return attributes;
      }
      else {
        if (attributes != null && !attributes.isFallbackEnabled()) return attributes;
        attributes = getFallbackAttributes(fallbackKey);
        if (attributes != null) return attributes;
      }
    }
    return myParentScheme == null ? null : myParentScheme.getAttributes(key);
  }

  @Nullable
  @Override
  public Color getColor(ColorKey key) {
    if (key == null) return null;
    Color color = myColorsMap.get(key);
    if (color != null) {
      return color;
    }
    if (myParentScheme != null) {
      color = myParentScheme.getColor(key);
      if (color != null) {
        return color;
      }
    }
    return key.getDefaultColor();
  }

  @Override
  public void readExternal(Element parentNode) {
    super.readExternal(parentNode);
    myName = parentNode.getAttributeValue(NAME_ATTR);
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void setAttributes(TextAttributesKey key, TextAttributes attributes) {
  }

  @Override
  public void setColor(ColorKey key, Color color) {
  }

  @Override
  public void setFont(EditorFontType key, Font font) {
  }

  @Override
  public EditorColorsScheme clone() {
    EditorColorsSchemeImpl newScheme = new EditorColorsSchemeImpl(this, myEditorColorsManager);
    copyTo(newScheme);
    newScheme.setName(myName);
    return newScheme;
  }
}
