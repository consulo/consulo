/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ColorGenerator {
  @Nonnull
  public static List<ColorValue> generateLinearColorSequence(@Nonnull List<ColorValue> anchorColors, int colorsBetweenAnchors) {
    assert colorsBetweenAnchors >= 0;
    if (anchorColors.isEmpty()) return List.of(StandardColors.GRAY);
    if (anchorColors.size() == 1) return List.of(anchorColors.get(0));

    int segmentCount = anchorColors.size() - 1;
    List<ColorValue> result = new ArrayList<>(anchorColors.size() + segmentCount * colorsBetweenAnchors);
    result.add(anchorColors.get(0));

    for (int i = 0; i < segmentCount; i++) {
      ColorValue color1 = anchorColors.get(i);
      ColorValue color2 = anchorColors.get(i + 1);

      List<ColorValue> linearColors = generateLinearColorSequence(color1, color2, colorsBetweenAnchors);

      // skip first element from sequence to avoid duplication from connected segments
      result.addAll(linearColors.subList(1, linearColors.size()));
    }
    return result;
  }

  @Nonnull
  public static List<ColorValue> generateLinearColorSequence(@Nonnull ColorValue color1, @Nonnull ColorValue color2, int colorsBetweenAnchors) {
    assert colorsBetweenAnchors >= 0;

    List<ColorValue> result = new ArrayList<>(colorsBetweenAnchors + 2);
    result.add(color1);

    for (int i = 1; i <= colorsBetweenAnchors; i++) {
      float ratio = (float)i / (colorsBetweenAnchors + 1);

      RGBColor rgb1 = color1.toRGB();
      RGBColor rgb2 = color2.toRGB();
      result.add(new RGBColor(
              ratio(rgb1.getRed(), rgb2.getRed(), ratio),
              ratio(rgb1.getGreen(), rgb2.getGreen(), ratio),
              ratio(rgb1.getBlue(), rgb2.getBlue(), ratio)
      ));
    }

    result.add(color2);
    return result;
  }

  private static int ratio(int val1, int val2, float ratio) {
    int value = (int)(val1 + ((val2 - val1) * ratio));
    return Math.max(Math.min(value, 255), 0);
  }
}
