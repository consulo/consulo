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
import consulo.ui.Component;
import consulo.ui.image.Image;
import consulo.ui.migration.ToSwingWrapper;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.RGBColor;
import consulo.ui.shared.Rectangle2D;
import consulo.ui.shared.Size;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 25-Sep-17
 * <p>
 * This should moved to desktop module, after split desktop and platform code
 */
public class TargetAWT {
  @Nonnull
  public static java.awt.Dimension to(@Nonnull Size size) {
    return JBUI.size(size.getWidth(), size.getHeight());
  }

  @Nonnull
  public static java.awt.Color to(@Nonnull RGBColor color) {
    return new java.awt.Color(color.getRed(), color.getGreed(), color.getBlue());
  }

  @Nonnull
  public static java.awt.Color to(@Nonnull ColorValue colorValue) {
    return to(colorValue.toRGB());
  }

  @Contract("null -> null")
  public static java.awt.Rectangle to(@Nullable Rectangle2D rectangle2D) {
    if (rectangle2D == null) {
      return null;
    }
    return new java.awt.Rectangle(rectangle2D.getCoordinate().getX(), rectangle2D.getCoordinate().getY(), rectangle2D.getSize().getWidth(), rectangle2D.getSize().getHeight());
  }

  @Nonnull
  public static java.awt.Component to(@Nonnull Component component) {
    if (component instanceof ToSwingWrapper) {
      return ((ToSwingWrapper)component).toAWT();
    }
    else if (component instanceof java.awt.Component) {
      return (java.awt.Component)component;
    }
    throw new IllegalArgumentException(component + " is not ToSwingWrapper");
  }

  @Contract("null -> null")
  public static Rectangle2D from(@Nullable java.awt.Rectangle rectangle) {
    if (rectangle == null) {
      return null;
    }
    return new Rectangle2D(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
  }

  @Contract("null -> null")
  public static RGBColor from(@Nullable java.awt.Color color) {
    if (color == null) {
      return null;
    }
    return new RGBColor(color.getRed(), color.getGreen(), color.getBlue());
  }

  @Contract("null -> null")
  public static Icon to(@Nullable Image uiImage) {
    if (uiImage == null) {
      return null;
    }

    if(uiImage instanceof Icon) {
      return (Icon)uiImage;
    }

    throw new IllegalArgumentException();
  }
}
