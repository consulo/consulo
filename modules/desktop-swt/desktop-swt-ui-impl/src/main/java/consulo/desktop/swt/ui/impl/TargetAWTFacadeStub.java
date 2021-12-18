/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.swt.ui.impl;

import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWTFacade;
import consulo.ui.Rectangle2D;
import consulo.ui.Size;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-02-21
 */
public class TargetAWTFacadeStub implements TargetAWTFacade {
  private static class IconWrapper implements Icon {
    private final Image myImage;

    private IconWrapper(Image image) {
      myImage = image;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {

    }

    @Override
    public int getIconWidth() {
      return myImage.getWidth();
    }

    @Override
    public int getIconHeight() {
      return myImage.getHeight();
    }
  }

  @Nonnull
  @Override
  public Dimension to(@Nonnull Size size) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Color to(@Nonnull RGBColor color) {
    return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
  }

  @Override
  public Color to(@Nullable ColorValue colorValue) {
    return colorValue == null ? null : to(colorValue.toRGB());
  }

  @Override
  public Rectangle to(@Nullable Rectangle2D rectangle2D) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Component to(@Nullable consulo.ui.Component component) {
    return null;
  }

  @Override
  public consulo.ui.Component from(@Nullable Component component) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Window to(@Nullable consulo.ui.Window component) {
    return null;
  }

  @Override
  public consulo.ui.Window from(@Nullable Window component) {
    return null;
  }

  @Override
  public Rectangle2D from(@Nullable Rectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RGBColor from(@Nullable Color color) {
    if (color == null) {
      return null;
    }
    return new RGBColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255f);
  }

  @Override
  public Icon to(@Nullable Image image) {
    if (image == null) {
      return null;
    }

    if (image instanceof Icon) {
      return (Icon)image;
    }

    return new IconWrapper(image);
  }

  @Override
  public Image from(@Nullable Icon icon) {
    if (icon == null) {
      return null;
    }

    if (icon instanceof IconWrapper) {
      return ((IconWrapper)icon).myImage;
    }

    if (icon instanceof Image) {
      return (Image)icon;
    }

    return null;
  }

  @Nonnull
  @Override
  public Font to(@Nonnull consulo.ui.font.Font font) {
    throw new UnsupportedOperationException();
  }

  @Override
  public java.awt.Image toImage(@Nonnull ImageKey key, JBUI.ScaleContext ctx) {
    throw new UnsupportedOperationException();
  }
}
