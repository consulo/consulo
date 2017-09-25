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
package consulo.awt;

import com.intellij.util.ui.JBUI;
import consulo.ui.RGBColor;
import consulo.ui.Rectangle2D;
import consulo.ui.Size;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author VISTALL
 * @since 25-Sep-17
 * <p>
 * This should moved to desktop module, after split desktop and platform code
 */
public class ToAWT {
  @NotNull
  public static Dimension from(@NotNull Size size) {
    return JBUI.size(size.getWidth(), size.getHeight());
  }

  @NotNull
  public static Color from(@NotNull RGBColor color) {
    return new Color(color.getRed(), color.getGreed(), color.getBlue());
  }

  @NotNull
  public static Rectangle from(@NotNull Rectangle2D rectangle2D) {
    return new Rectangle(rectangle2D.getCoordinate().getX(), rectangle2D.getCoordinate().getY(), rectangle2D.getSize().getWidth(), rectangle2D.getSize().getHeight());
  }
}
