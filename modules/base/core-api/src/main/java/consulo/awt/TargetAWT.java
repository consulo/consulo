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
import consulo.awt.internal.SwingComponentWrapper;
import consulo.awt.internal.SwingIconWrapper;
import consulo.awt.internal.SwingWindowWrapper;
import consulo.ui.Component;
import consulo.ui.KeyCode;
import consulo.ui.Window;
import consulo.ui.image.Image;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.RGBColor;
import consulo.ui.shared.Rectangle2D;
import consulo.ui.shared.Size;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyEvent;

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
    int alpha = (int)color.getAlpha() * 255;
    return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }

  @Contract("null -> null")
  public static java.awt.Color to(@Nullable ColorValue colorValue) {
    return colorValue == null ? null : to(colorValue.toRGB());
  }

  @Contract("null -> null")
  public static java.awt.Rectangle to(@Nullable Rectangle2D rectangle2D) {
    if (rectangle2D == null) {
      return null;
    }
    return new java.awt.Rectangle(rectangle2D.getCoordinate().getX(), rectangle2D.getCoordinate().getY(), rectangle2D.getSize().getWidth(), rectangle2D.getSize().getHeight());
  }

  @Contract("null -> null")
  public static java.awt.Component to(@Nullable Component component) {
    if (component == null) {
      return null;
    }

    if (component instanceof SwingComponentWrapper) {
      return ((SwingComponentWrapper)component).toAWTComponent();
    }
    throw new IllegalArgumentException(component + " is not SwingComponentWrapper");
  }

  @Contract("null -> null")
  public static java.awt.Window to(@Nullable Window component) {
    if (component == null) {
      return null;
    }

    if (component instanceof SwingWindowWrapper) {
      return ((SwingWindowWrapper)component).toAWTWindow();
    }
    throw new IllegalArgumentException(component + " is not SwingWindowWrapper");
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
    float[] components = color.getRGBComponents(null);
    return new RGBColor(color.getRed(), color.getGreen(), color.getBlue(), components[3]);
  }

  @Contract("null -> null")
  public static Icon to(@Nullable Image image) {
    if (image == null) {
      return null;
    }

    if (image instanceof SwingIconWrapper) {
      return ((SwingIconWrapper)image).toSwingIcon();
    }
    else if (image instanceof Icon) {
      return (Icon)image;
    }

    throw new IllegalArgumentException(image + "' is not supported");
  }

  @Contract("null -> null")
  public static Image from(@Nullable Icon icon) {
    if (icon == null) {
      return null;
    }

    if (icon instanceof Image) {
      return (Image)icon;
    }

    throw new IllegalArgumentException(icon + "' is not supported");
  }

  public static int to(@Nonnull KeyCode code) {
    if (code.ordinal() >= KeyCode.A.ordinal() && code.ordinal() <= KeyCode.Z.ordinal()) {
      int diff = code.ordinal() - KeyCode.A.ordinal();
      return KeyEvent.VK_A + diff;
    }
    throw new IllegalArgumentException(code + "' is not supported");
  }
}
