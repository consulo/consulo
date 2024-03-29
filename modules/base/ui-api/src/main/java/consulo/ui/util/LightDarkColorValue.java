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
package consulo.ui.util;

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.StyleManager;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11/29/2020
 */
public class LightDarkColorValue implements ColorValue {
  private final ColorValue myLightColorValue;
  private final ColorValue myDarkColorValue;

  public LightDarkColorValue(ColorValue lightColorValue, ColorValue darkColorValue) {
    myLightColorValue = lightColorValue;
    myDarkColorValue = darkColorValue;
  }

  @Nonnull
  @Override
  public RGBColor toRGB() {
    return StyleManager.get().getCurrentStyle().isDark() ? myDarkColorValue.toRGB() : myLightColorValue.toRGB();
  }
}
