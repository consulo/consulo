/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;

import java.awt.*;

public class FontUtil {
  @Nonnull
  public static String rightArrow(@Nonnull Font font) {
    return canDisplay(font, '\u2192', "->");
  }

  @Nonnull
  public static String upArrow(@Nonnull Font font, @Nonnull String defaultValue) {
    return canDisplay(font, '\u2191', defaultValue);
  }

  @Nonnull
  public static String canDisplay(@Nonnull Font font, char value, @Nonnull String defaultValue) {
    return font.canDisplay(value) ? String.valueOf(value) : defaultValue;
  }

  @Nonnull
  public static Font minusOne(@Nonnull Font font) {
    return font.deriveFont(font.getSize() - 1f);
  }

  @Nonnull
  public static String spaceAndThinSpace() {
    return " " + thinSpace();
  }

  @Nonnull
  public static String thinSpace() {
    return canDisplay(UIUtil.getLabelFont(), '\u2009', " ");
  }
}
