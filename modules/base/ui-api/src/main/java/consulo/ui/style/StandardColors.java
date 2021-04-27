/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.style;

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public enum StandardColors implements StyleColorValue {
  WHITE(new RGBColor(255, 255, 255)),
  BLACK(new RGBColor(0, 0, 0)),

  CYAN(new RGBColor(0, 255, 255)),
  MAGENTA(new RGBColor(255, 0, 255)),
  YELLOW(new RGBColor(255, 255, 0)),
  LIGHT_YELLOW(new RGBColor(255, 255, 204)),
  
  ORANGE(new RGBColor(255, 200, 0)),

  RED(new RGBColor(255, 0, 0)),
  LIGHT_RED(new RGBColor(255, 204, 204)),
  GREEN(new RGBColor(0, 255, 0)),
  BLUE(new RGBColor(0, 0, 255)),

  GRAY(new RGBColor(128, 128, 128)),
  LIGHT_GRAY(new RGBColor(192, 192, 192)),
  DARK_GRAY(new RGBColor(64, 64, 64));

  private final ColorValue myStaticValue;

  StandardColors(ColorValue staticValue) {
    myStaticValue = staticValue;
  }

  @Nonnull
  public ColorValue getStaticValue() {
    return myStaticValue;
  }
}
