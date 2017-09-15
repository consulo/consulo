/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.internal;

import consulo.ui.RGBColor;
import consulo.ui.style.ColorKey;
import consulo.ui.style.Colors;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WGwtStyleImpl implements Style {
  private Map<ColorKey, RGBColor> myColors = new HashMap<>();
  private final String myName;

  public WGwtStyleImpl(String name) {
    myName = name;

    myColors.put(Colors.RED, to(Color.RED));
    myColors.put(Colors.GREEN, to(Color.GREEN));
    myColors.put(Colors.BLUE, to(Color.BLUE));

    myColors.put(ComponentColors.BORDER, to(Color.lightGray));
  }

  private static RGBColor to(Color color) {
    return new RGBColor(color.getRed(), color.getGreen(), color.getBlue());
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public RGBColor getColor(@NotNull ColorKey colorKey) {
    return myColors.get(colorKey);
  }
}
