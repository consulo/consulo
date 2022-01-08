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
package com.intellij.util.ui;

import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ImageLoader;
import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.RetinaImage;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;

import static com.intellij.util.ui.JBUI.ScaleType.SYS_SCALE;

/**
 * @author Konstantin Bulenkov
 */
public class ImageUtil {
  /**
   * Creates a HiDPI-aware BufferedImage in device scale.
   *
   * @param width  the width in user coordinate space
   * @param height the height in user coordinate space
   * @param type   the type of the image
   * @return a HiDPI-aware BufferedImage in device scale
   * @throws IllegalArgumentException if {@code width} or {@code height} is not greater than 0
   */
  @Nonnull
  public static BufferedImage createImage(int width, int height, int type) {
    if (UIUtil.isJreHiDPIEnabled()) {
      return RetinaImage.create(width, height, type);
    }
    //noinspection UndesirableClassUsage
    return new BufferedImage(width, height, type);
  }

  /**
   * Creates a HiDPI-aware BufferedImage in the graphics config scale.
   *
   * @param gc     the graphics config
   * @param width  the width in user coordinate space
   * @param height the height in user coordinate space
   * @param type   the type of the image
   * @return a HiDPI-aware BufferedImage in the graphics scale
   * @throws IllegalArgumentException if {@code width} or {@code height} is not greater than 0
   */
  @Nonnull
  public static BufferedImage createImage(GraphicsConfiguration gc, int width, int height, int type) {
    if (UIUtil.isJreHiDPI(gc)) {
      return RetinaImage.create(gc, width, height, type);
    }
    //noinspection UndesirableClassUsage
    return new BufferedImage(width, height, type);
  }

  /**
   * Creates a HiDPI-aware BufferedImage in the graphics device scale.
   *
   * @param g      the graphics of the target device
   * @param width  the width in user coordinate space
   * @param height the height in user coordinate space
   * @param type   the type of the image
   * @return a HiDPI-aware BufferedImage in the graphics scale
   * @throws IllegalArgumentException if {@code width} or {@code height} is not greater than 0
   */
  @Nonnull
  public static BufferedImage createImage(Graphics g, int width, int height, int type) {
    return createImage(g, width, height, type, PaintUtil.RoundingMode.FLOOR);
  }

  /**
   * @throws IllegalArgumentException if {@code width} or {@code height} is not greater than 0
   * @see #createImage(GraphicsConfiguration, double, double, int, PaintUtil.RoundingMode)
   */
  @Nonnull
  public static BufferedImage createImage(Graphics g, double width, double height, int type, @Nonnull PaintUtil.RoundingMode rm) {
    if (g instanceof Graphics2D) {
      Graphics2D g2d = (Graphics2D)g;
      if (UIUtil.isJreHiDPI(g2d)) {
        return RetinaImage.create(g2d, width, height, type, rm);
      }
      //noinspection UndesirableClassUsage
      return new BufferedImage(rm.round(width), rm.round(height), type);
    }
    return createImage(rm.round(width), rm.round(height), type);
  }

  public static BufferedImage toBufferedImage(@Nonnull Image image) {
    return toBufferedImage(image, false);
  }

  public static BufferedImage toBufferedImage(@Nonnull Image image, boolean inUserSize) {
    if (image instanceof JBHiDPIScaledImage) {
      JBHiDPIScaledImage jbImage = (JBHiDPIScaledImage)image;
      Image img = jbImage.getDelegate();
      if (img != null) {
        if (inUserSize) {
          double scale = jbImage.getScale();
          img = scaleImage(img, 1 / scale);
        }
        image = img;
      }
    }
    if (image instanceof BufferedImage) {
      return (BufferedImage)image;
    }

    final int width = image.getWidth(null);
    final int height = image.getHeight(null);
    if (width <= 0 || height <= 0) {
      // avoiding NPE
      return new BufferedImage(Math.max(width, 1), Math.max(height, 1), BufferedImage.TYPE_INT_ARGB) {
        @Override
        public int getWidth() {
          return Math.max(width, 0);
        }

        @Override
        public int getHeight() {
          return Math.max(height, 0);
        }

        @Override
        public int getWidth(ImageObserver observer) {
          return Math.max(width, 0);
        }

        @Override
        public int getHeight(ImageObserver observer) {
          return Math.max(height, 0);
        }
      };
    }

    @SuppressWarnings("UndesirableClassUsage") BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = bufferedImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return bufferedImage;
  }

  public static int getRealWidth(@Nonnull Image image) {
    if (image instanceof JBHiDPIScaledImage) {
      Image img = ((JBHiDPIScaledImage)image).getDelegate();
      if (img != null) image = img;
    }
    return image.getWidth(null);
  }

  public static int getRealHeight(@Nonnull Image image) {
    if (image instanceof JBHiDPIScaledImage) {
      Image img = ((JBHiDPIScaledImage)image).getDelegate();
      if (img != null) image = img;
    }
    return image.getHeight(null);
  }

  public static int getUserWidth(@Nonnull Image image) {
    if (image instanceof JBHiDPIScaledImage) {
      return ((JBHiDPIScaledImage)image).getUserWidth(null);
    }
    return image.getWidth(null);
  }

  public static int getUserHeight(@Nonnull Image image) {
    if (image instanceof JBHiDPIScaledImage) {
      return ((JBHiDPIScaledImage)image).getUserHeight(null);
    }
    return image.getHeight(null);
  }

  public static Image filter(Image image, ImageFilter filter) {
    if (image == null || filter == null) return image;
    return Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(toBufferedImage(image).getSource(), filter));
  }

  /**
   * Scales the image taking into account its HiDPI awareness.
   */
  public static Image scaleImage(Image image, double scale) {
    return ImageLoader.scaleImage(image, scale);
  }

  /**
   * Wraps the {@code image} with {@link JBHiDPIScaledImage} according to {@code ctx} when applicable.
   */
  @Contract("null, _ -> null; !null, _ -> !null")
  public static Image ensureHiDPI(@Nullable Image image, @Nonnull JBUI.ScaleContext ctx) {
    if (image == null) return null;
    if (UIUtil.isJreHiDPI(ctx)) {
      return RetinaImage.createFrom(image, ctx.getScale(SYS_SCALE), null);
    }
    return image;
  }
}
