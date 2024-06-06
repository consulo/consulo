/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.colors.impl;

import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributes;
import consulo.component.persist.scheme.ExternalInfo;
import consulo.component.persist.scheme.ExternalizableScheme;
import consulo.util.lang.Comparing;
import consulo.ui.color.ColorValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * @author Yura Cangea
 */
public class EditorColorsSchemeImpl extends AbstractColorsScheme implements ExternalizableScheme {
  private final ExternalInfo myExternalInfo = new ExternalInfo();

  public EditorColorsSchemeImpl(EditorColorsScheme parentScheme, @Nonnull EditorColorsManager editorColorsManager) {
    super(parentScheme, editorColorsManager);
  }

  @Override
  public void setAttributes(TextAttributesKey key, TextAttributes attributes) {
    if (attributes != getAttributes(key)) {
      myAttributesMap.put(key, attributes);
    }
  }

  @Override
  public void setColor(EditorColorKey key, ColorValue color) {
    if (!Comparing.equal(color, getColor(key))) {
      myColorsMap.put(key, color);
    }
  }

  @Override
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
    return myParentScheme.getAttributes(key);
  }

  public boolean containsKey(TextAttributesKey key) {
    return myAttributesMap.containsKey(key);
  }

  @Nullable
  @Override
  public ColorValue getColor(EditorColorKey key) {
    if (myColorsMap.containsKey(key)) {
      return myColorsMap.get(key);
    }
    else {
      return myParentScheme.getColor(key);
    }
  }

  @Override
  public void fillColors(Map<EditorColorKey, ColorValue> colors) {
    if (myParentScheme != null) {
      myParentScheme.fillColors(colors);
    }

    colors.putAll(myColorsMap);
  }

  @Override
  public void fillAttributes(@Nonnull Map<TextAttributesKey, TextAttributes> map) {
    if (myParentScheme != null) {
      myParentScheme.fillAttributes(map);
    }

    map.putAll(myAttributesMap);
  }

  @Override
  public EditorColorsScheme clone() {
    EditorColorsSchemeImpl newScheme = new EditorColorsSchemeImpl(myParentScheme, myEditorColorsManager);
    copyTo(newScheme);
    newScheme.setName(getName());
    return newScheme;
  }

  @Override
  @Nonnull
  public ExternalInfo getExternalInfo() {
    return myExternalInfo;
  }
}
