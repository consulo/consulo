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
public abstract class DesktopInnerImageImpl<T extends DesktopInnerImageImpl<T>> extends JBUI.CachingScalableJBIcon<T> implements Image, DesktopLibraryInnerImage {
  protected final int myWidth;
  protected final int myHeight;
  protected final Supplier<ImageFilter> myFilter;

  private final NotNullLazyValue<T> myGrayedImageValue;

  protected java.awt.Image myCachedImage;

  protected DesktopInnerImageImpl(int width, int height, @Nullable Supplier<ImageFilter> imageFilterSupplier) {
    myWidth = width;
    myHeight = height;
    myFilter = imageFilterSupplier;

    myGrayedImageValue = NotNullLazyValue.createValue(() -> withFilter(() -> UIUtil.getGrayFilter(DarkThemeCalculator.isDark())));
  }

  @Nonnull
  protected abstract T withFilter(@Nullable Supplier<ImageFilter> filter);

  @Nonnull
  protected abstract java.awt.Image calcImage(@Nonnull Graphics originalGraphics);

  @Override
  public void paintIcon(Component c, Graphics originalGraphics, int x, int y) {
    if (updateScaleContext(JBUI.ScaleContext.create((Graphics2D)originalGraphics))) {
      myCachedImage = null;
    }

    java.awt.Image cachedImage = myCachedImage;
    if (cachedImage == null) {
      cachedImage = calcImage(originalGraphics);
      myCachedImage = cachedImage;
    }

    UIUtil.drawImage(originalGraphics, cachedImage, x, y, null);
  }

  @Nonnull
  @Override
  protected T copy() {
    return withFilter(myFilter);
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
    return JBUI.scale(myHeight);
  }

  @Override
  public int getWidth() {
    return JBUI.scale(myWidth);
  }
}
