/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal.image;

import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-10-17
 * <p>
 * Light version of {@link DesktopHeavyLayeredImageImpl} without calculating sizes inside constructor, and without support shift icons
 */
public class DesktopLayeredImageImpl extends JBUI.RasterJBIcon implements Image, DesktopImage<DesktopLayeredImageImpl> {
  private static final int WIDTH = 0;
  private static final int HEIGHT = 1;

  private final Image[] myImages;
  private int[] myCachedSize;

  public DesktopLayeredImageImpl(@Nonnull Image[] images) {
    myImages = images;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    JBUI.ScaleContext ctx = JBUI.ScaleContext.create((Graphics2D)g);

    if (updateScaleContext(ctx)) {
      myCachedSize = null;
    }

    for (Image image : myImages) {
      TargetAWT.to(image).paintIcon(c, g, x, y);
    }
  }

  private void updateSize() {
    int width = 0;
    int height = 0;

    for (Image image : myImages) {
      int h = image.getHeight();
      int w = image.getWidth();

      if (h > height) {
        height = h;
      }

      if (w > width) {
        width = w;
      }
    }

    myCachedSize = new int[]{width, height};
  }

  @Override
  public int getWidth() {
    if (myCachedSize == null) {
      updateSize();
    }
    return myCachedSize[WIDTH];
  }

  @Override
  public int getHeight() {
    if (myCachedSize == null) {
      updateSize();
    }
    return myCachedSize[HEIGHT];
  }

  @Override
  public int getIconWidth() {
    return getWidth();
  }

  @Override
  public int getIconHeight() {
    return getHeight();
  }

  @Nonnull
  @Override
  public DesktopLayeredImageImpl copyWithTargetIconLibrary(@Nonnull String iconLibraryId, @Nonnull Function<Image, Image> converter) {
    Image[] converted = new Image[myImages.length];
    for (int i = 0; i < myImages.length; i++) {
      converted[i] = converter.apply(myImages[i]);
    }
    return new DesktopLayeredImageImpl(converted);
  }

  @Nonnull
  @Override
  public DesktopLayeredImageImpl copyWithScale(float scale) {
    Image[] converted = new Image[myImages.length];
    for (int i = 0; i < myImages.length; i++) {
      converted[i] = ImageEffects.resize(myImages[i], scale);
    }
    return new DesktopLayeredImageImpl(converted);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DesktopLayeredImageImpl image = (DesktopLayeredImageImpl)o;
    return Arrays.equals(myImages, image.myImages);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(myImages);
  }
}
