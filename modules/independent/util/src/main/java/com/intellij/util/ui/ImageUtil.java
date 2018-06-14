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

import static com.intellij.util.ui.JBUI.ScaleType.SYS_SCALE;

/**
 * @author Konstantin Bulenkov
 */
public class ImageUtil {
  public static BufferedImage toBufferedImage(@Nonnull Image image) {
    if (image instanceof JBHiDPIScaledImage) {
      Image img = ((JBHiDPIScaledImage)image).getDelegate();
      if (img != null) {
        image = img;
      }
    }
    if (image instanceof BufferedImage) {
      return (BufferedImage)image;
    }

    @SuppressWarnings("UndesirableClassUsage")
    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
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
    return Toolkit.getDefaultToolkit().createImage(
            new FilteredImageSource(toBufferedImage(image).getSource(), filter));
  }

  /**
   * Scales the image taking into account its HiDPI awareness.
   */
  public static Image scaleImage(Image image, float scale) {
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
