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
package consulo.awt.impl;

import consulo.desktop.util.awt.UIModificationTracker;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 11/28/2020
 */
class AWTColorValue implements ColorValue {
  private static final UIModificationTracker ourUIModificationTracker = UIModificationTracker.getInstance();

  private final Color myColor;

  private RGBColor myLastRGBColor;

  private long myModificationCount = -1;

  AWTColorValue(Color color) {
    myColor = color;
  }

  @Nonnull
  @Override
  public RGBColor toRGB() {
    long oldMod = myModificationCount;

    long modificationCount = ourUIModificationTracker.getModificationCount();

    RGBColor lastRGBColor = myLastRGBColor;

    if(oldMod == modificationCount) {
      if(lastRGBColor == null) {
        return myLastRGBColor = convert();
      }
      else {
        return lastRGBColor;
      }
    }
    else {
      myModificationCount = modificationCount;
      return myLastRGBColor = convert();
    }
  }

  private RGBColor convert() {
    return new RGBColor(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), myColor.getAlpha());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AWTColorValue that = (AWTColorValue)o;
    return Objects.equals(myColor, that.myColor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myColor);
  }
}
