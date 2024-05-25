/*
 * Copyright 2013-2018 consulo.io
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
package consulo.desktop.awt.ui.impl.image;

import consulo.desktop.awt.facade.ToSwingIconWrapper;
import consulo.desktop.awt.ui.impl.image.libraryImage.DesktopLibraryInnerImage;
import consulo.ide.impl.idea.util.ui.JBImageIcon;
import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.RetinaImage;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RGBImageFilter;

/**
 * @author VISTALL
 * @since 6/22/18
 * <p>
 * If image is from library - use method from library class.
 * <p>
 * In other ways, we will return not cached image
 */
public class DesktopDisabledImageImpl implements ToSwingIconWrapper, Image {
  public static Image of(@Nonnull Image original) {
    if (original instanceof DesktopLibraryInnerImage) {
      return ((DesktopLibraryInnerImage)original).makeGrayed();
    }
    return new DesktopDisabledImageImpl(original);
  }

  private final Icon myIcon;

  private DesktopDisabledImageImpl(Image original) {
    myIcon = getDisabledIcon(TargetAWT.to(original));
  }

  /**
   * Gets (creates if necessary) disabled icon based on the passed one.
   *
   * @return <code>ImageIcon</code> constructed from disabled image of passed icon.
   */
  @Nullable
  public Icon getDisabledIcon(@Nullable Icon icon) {
    if (icon instanceof DesktopLazyImageImpl) icon = ((DesktopLazyImageImpl)icon).getOrComputeIcon();
    if (icon == null) return null;

    return filterIcon(icon, UIUtil.getGrayFilter(StyleManager.get().getCurrentStyle().isDark()), null);
  }

  /**
   * Creates new icon with the filter applied.
   */
  @SuppressWarnings("UndesirableClassUsage")
  public static Icon filterIcon(@Nonnull Icon icon, RGBImageFilter filter, @Nullable Component ancestor) {
    if (icon instanceof DesktopLazyImageImpl) icon = ((DesktopLazyImageImpl)icon).getOrComputeIcon();

    final float scale;
    if (icon instanceof JBUI.ScaleContextAware) {
      scale = (float)((JBUI.ScaleContextAware)icon).getScale(JBUI.ScaleType.SYS_SCALE);
    }
    else {
      scale = UIUtil.isJreHiDPI() ? JBUI.sysScale(ancestor) : 1f;
    }
    BufferedImage image =
      new BufferedImage((int)(scale * icon.getIconWidth()), (int)(scale * icon.getIconHeight()), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = image.createGraphics();

    graphics.setColor(UIUtil.TRANSPARENT_COLOR);
    graphics.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
    graphics.scale(scale, scale);
    icon.paintIcon(LabelHolder.ourFakeComponent, graphics, 0, 0);

    graphics.dispose();

    java.awt.Image img = ImageUtil.filter(image, filter);
    if (UIUtil.isJreHiDPI()) img = RetinaImage.createFrom(img, scale, null);

    icon = new JBImageIcon(img);
    return icon;
  }


  private static class LabelHolder {
    /**
     * To get disabled icon with paint it into the image. Some icons require
     * not null component to paint.
     */
    private static final JComponent ourFakeComponent = new JLabel();
  }

  @Nonnull
  @Override
  public Icon toSwingIcon() {
    return myIcon;
  }

  @Override
  public int getHeight() {
    return myIcon.getIconHeight();
  }

  @Override
  public int getWidth() {
    return myIcon.getIconWidth();
  }
}
