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
package com.intellij.openapi.wm;

import consulo.ui.TaskBar;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;

public class AppIconScheme {
  private static ColorValue TESTS_OK_COLOR = new RGBColor(46, 191, 38);
  private static ColorValue BUILD_OK_COLOR = new RGBColor(51, 153, 255);
  private static ColorValue INDEXING_OK_COLOR = new RGBColor(255, 170, 0);
  private static ColorValue ERROR_COLOR = StandardColors.RED.getStaticValue();

  public interface Progress extends TaskBar.ProgressScheme {
    static final Progress TESTS = new Progress() {
      @Nonnull
      @Override
      public ColorValue getOkColor() {
        return TESTS_OK_COLOR;
      }

      @Nonnull
      @Override
      public ColorValue getErrorColor() {
        return ERROR_COLOR;
      }
    };

    static final Progress BUILD = new Progress() {
      @Nonnull
      @Override
      public ColorValue getOkColor() {
        return BUILD_OK_COLOR;
      }

      @Nonnull
      @Override
      public ColorValue getErrorColor() {
        return ERROR_COLOR;
      }
    };

    static final Progress INDEXING = new Progress() {
      @Nonnull
      @Override
      public ColorValue getOkColor() {
        return INDEXING_OK_COLOR;
      }

      @Nonnull
      @Override
      public ColorValue getErrorColor() {
        return ERROR_COLOR;
      }
    };

    @Nonnull
    @Override
    ColorValue getOkColor();

    @Nonnull
    @Override
    ColorValue getErrorColor();
  }
}
