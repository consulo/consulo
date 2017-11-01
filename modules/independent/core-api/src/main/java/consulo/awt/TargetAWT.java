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
import consulo.ui.shared.RGBColor;
import consulo.ui.shared.Rectangle2D;
import consulo.ui.shared.Size;
import consulo.ui.migration.ToSwingWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author VISTALL
 * @since 25-Sep-17
 * <p>
 * This should moved to desktop module, after split desktop and platform code
 */
public class TargetAWT {
  @NotNull
  public static Dimension to(@NotNull Size size) {
    return JBUI.size(size.getWidth(), size.getHeight());
  }

  @NotNull
  public static Color to(@NotNull RGBColor color) {
    return new Color(color.getRed(), color.getGreed(), color.getBlue());
  }

  @Contract("null -> null")
  public static Rectangle to(@Nullable Rectangle2D rectangle2D) {
    if (rectangle2D == null) {
      return null;
    }
    return new Rectangle(rectangle2D.getCoordinate().getX(), rectangle2D.getCoordinate().getY(), rectangle2D.getSize().getWidth(), rectangle2D.getSize().getHeight());
  }

  @NotNull
  public static Component to(@NotNull consulo.ui.Component component) {
    if (component instanceof ToSwingWrapper) {
      return ((ToSwingWrapper)component).toAWT();
    }
    else if(component instanceof Component) {
      return (Component)component;
    }
    throw new IllegalArgumentException(component + " is not ToSwingWrapper");
  }

  @Contract("null -> null")
  public static Rectangle2D from(@Nullable Rectangle rectangle) {
    if (rectangle == null) {
      return null;
    }
    return new Rectangle2D(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
  }
}
