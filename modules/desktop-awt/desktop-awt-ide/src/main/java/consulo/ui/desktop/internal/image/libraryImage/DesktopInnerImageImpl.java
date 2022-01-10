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
package consulo.ui.desktop.internal.image.libraryImage;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.awt.util.DarkThemeCalculator;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.ImageFilter;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-10-01
 */
public abstract class DesktopInnerImageImpl<T extends DesktopInnerImageImpl<T>> extends JBUI.RasterJBIcon implements Image, DesktopLibraryInnerImage {
  protected final int myWidth;
  protected final int myHeight;
  protected final Supplier<ImageFilter> myFilter;

  private final NotNullLazyValue<T> myGrayedImageValue;

  protected java.awt.Image myCachedImage;

  // additional scale. in most cases it's 1
  protected float myScale;

  protected DesktopInnerImageImpl(int width, int height, float scale, @Nullable Supplier<ImageFilter> imageFilterSupplier) {
    myWidth = width;
    myHeight = height;
    myScale = scale;
    myFilter = imageFilterSupplier;

    myGrayedImageValue = NotNullLazyValue.createValue(() -> withFilter(() -> UIUtil.getGrayFilter(DarkThemeCalculator.isDark())));
  }

  @Nonnull
  @Override
  public Image copyWithScale(float scale) {
    if (scale == 1f) {
      return this;
    }
    return withScale(scale);
  }

  @Nonnull
  protected abstract T withFilter(@Nullable Supplier<ImageFilter> filter);

  @Nonnull
  protected abstract T withScale(float scale);

  @Nonnull
  protected abstract java.awt.Image calcImage(@Nullable JBUI.ScaleContext ctx);

  @Nonnull
  @Override
  public java.awt.Image toAWTImage(@Nullable JBUI.ScaleContext ctx) {
    return calcImage(ctx);
  }

  @Override
  public void paintIcon(Component c, Graphics originalGraphics, int x, int y) {
    JBUI.ScaleContext ctx = JBUI.ScaleContext.create((Graphics2D)originalGraphics);

    if (updateScaleContext(ctx)) {
      myCachedImage = null;
    }

    java.awt.Image cachedImage = myCachedImage;
    if (cachedImage == null) {
      cachedImage = calcImage(ctx);
      myCachedImage = cachedImage;
    }

    UIUtil.drawImage(originalGraphics, cachedImage, x, y, null);
  }

  @Nonnull
  @Override
  public Image makeGrayed() {
    return myGrayedImageValue.getValue();
  }

  @Override
  public void dropCache() {
    myCachedImage = null;

    if (myGrayedImageValue.isComputed()) {
      myGrayedImageValue.get().dropCache();
    }
  }

  @Override
  public int getIconWidth() {
    return getWidth();
  }

  @Override
  public int getIconHeight() {
    return getHeight();
  }

  @Override
  public int getHeight() {
    return (int)Math.ceil(JBUI.scale(myHeight) * myScale);
  }

  @Override
  public int getWidth() {
    return (int)Math.ceil(JBUI.scale(myWidth) * myScale);
  }
}
