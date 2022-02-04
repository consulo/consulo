/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.actions;

import com.intellij.codeHighlighting.ColorGenerator;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.color.ColorValue;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AnnotationsSettings {
  private static final int ANCHORS_COUNT = 5;
  private static final int COLORS_BETWEEN_ANCHORS = 4;
  private static final int SHUFFLE_STEP = 4;

  static final List<EditorColorKey> ANCHOR_COLOR_KEYS = createColorKeys(ANCHORS_COUNT);

  @Nonnull
  private static List<EditorColorKey> createColorKeys(int count) {
    List<EditorColorKey> keys = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      keys.add(EditorColorKey.createColorKey("VCS_ANNOTATIONS_COLOR_" + (i + 1)));
    }
    return keys;
  }

  public static AnnotationsSettings getInstance() {
    return ServiceManager.getService(AnnotationsSettings.class);
  }

  @Nonnull
  public List<ColorValue> getAuthorsColors(@Nullable EditorColorsScheme scheme) {
    if (scheme == null) scheme = EditorColorsManager.getInstance().getGlobalScheme();
    List<ColorValue> colors = getOrderedColors(scheme);

    List<ColorValue> authorColors = new ArrayList<>();
    for (int i = 0; i < SHUFFLE_STEP; i++) {
      for (int k = 0; k <= colors.size() / SHUFFLE_STEP; k++) {
        int index = k * SHUFFLE_STEP + i;
        if (index < colors.size()) authorColors.add(colors.get(index));
      }
    }

    return authorColors;
  }

  @Nonnull
  public List<ColorValue> getOrderedColors(@Nullable EditorColorsScheme scheme) {
    if (scheme == null) scheme = EditorColorsManager.getInstance().getGlobalScheme();

    List<ColorValue> anchorColors = new ArrayList<>();
    for (EditorColorKey key : ANCHOR_COLOR_KEYS) {
      ContainerUtil.addIfNotNull(anchorColors, scheme.getColor(key));
    }

    return ColorGenerator.generateLinearColorSequence(anchorColors, COLORS_BETWEEN_ANCHORS);
  }

  @Nonnull
  List<Integer> getAnchorIndexes(@Nullable EditorColorsScheme scheme) {
    if (scheme == null) scheme = EditorColorsManager.getInstance().getGlobalScheme();

    List<Integer> result = new ArrayList<>(ANCHORS_COUNT);

    int count = 0;
    for (EditorColorKey key : ANCHOR_COLOR_KEYS) {
      if (scheme.getColor(key) != null) {
        result.add(count);
        count += COLORS_BETWEEN_ANCHORS + 1;
      }
      else {
        result.add(null);
      }
    }

    return result;
  }
}
