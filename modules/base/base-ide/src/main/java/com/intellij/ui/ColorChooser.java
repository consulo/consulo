/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.function.Consumer;

/**
 * @author max
 * @author Konstantin Bulenkov
 */
public class ColorChooser {
  public static void chooseColor(Component parent,
                                 String caption,
                                 @Nullable Color preselectedColor,
                                 boolean enableOpacity,
                                 ColorPickerListener[] listeners,
                                 boolean opacityInPercent,
                                 @Nonnull Consumer<Color> colorConsumer) {
    ColorChooserService.getInstance().showDialog(parent, caption, preselectedColor, enableOpacity, listeners, opacityInPercent, colorConsumer);
  }

  public static void chooseColor(Component parent, String caption, @Nullable Color preselectedColor, boolean enableOpacity, @Nonnull Consumer<Color> colorConsumer) {
    chooseColor(parent, caption, preselectedColor, enableOpacity, ColorPickerListener.EMPTY_ARRAY, false, colorConsumer);
  }

  public static void chooseColor(Component parent, String caption, @Nullable Color preselectedColor, boolean enableOpacity, boolean opacityInPercent, @Nonnull Consumer<Color> colorConsumer) {
    chooseColor(parent, caption, preselectedColor, enableOpacity, ColorPickerListener.EMPTY_ARRAY, opacityInPercent, colorConsumer);
  }

  public static void chooseColor(Component parent, String caption, @Nullable Color preselectedColor, @Nonnull Consumer<Color> colorConsumer) {
    chooseColor(parent, caption, preselectedColor, false, colorConsumer);
  }
}
