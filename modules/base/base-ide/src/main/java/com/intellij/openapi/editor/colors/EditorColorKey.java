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
package com.intellij.openapi.editor.colors;

import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.StandardColors;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 11/28/2020
 */
public final class EditorColorKey implements ColorValue, Comparable<EditorColorKey> {
  public static final Key<Function<EditorColorKey, ColorValue>> FUNCTION_KEY = Key.create("COLOR_KEY_FUNCTION");

  private static final Logger LOG = Logger.getInstance(EditorColorKey.class);
  private static final Map<String, EditorColorKey> ourRegistry = new HashMap<>();

  private final String myExternalName;
  private ColorValue myDefaultColorValue;

  private EditorColorKey(String externalName) {
    myExternalName = externalName;
    if (ourRegistry.containsKey(myExternalName)) {
      LOG.error("Key " + myExternalName + " already registered.");
    }
    else {
      ourRegistry.put(myExternalName, this);
    }
  }

  public static EditorColorKey find(@Nonnull String externalName) {
    EditorColorKey key = ourRegistry.get(externalName);
    return key != null ? key : new EditorColorKey(externalName);
  }

  @Override
  @Nonnull
  public String toString() {
    return myExternalName;
  }

  @Nonnull
  public String getExternalName() {
    return myExternalName;
  }

  @Override
  public int compareTo(EditorColorKey key) {
    return myExternalName.compareTo(key.myExternalName);
  }

  @Nullable
  public ColorValue getDefaultColorValue() {
    return myDefaultColorValue;
  }

  public static EditorColorKey createColorKey(String externalName) {
    return find(externalName);
  }

  public static EditorColorKey createColorKey(String externalName, ColorValue defaultColor) {
    EditorColorKey key = ourRegistry.get(externalName);
    if (key == null) {
      key = find(externalName);
    }

    if (key.getDefaultColorValue() == null) {
      key.myDefaultColorValue = defaultColor;
    }
    return key;
  }

  @Nonnull
  @Override
  public RGBColor toRGB() {
    EditorColorsScheme currentScheme = EditorColorsManager.getInstance().getCurrentScheme();

    ColorValue color = currentScheme.getColor(this);
    if (color != null) {
      return color.toRGB();
    }

    if (myDefaultColorValue != null) {
      return myDefaultColorValue.toRGB();
    }

    return StandardColors.BLACK.toRGB();
  }
}
