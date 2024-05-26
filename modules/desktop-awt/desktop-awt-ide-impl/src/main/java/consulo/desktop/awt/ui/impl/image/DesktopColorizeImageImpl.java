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
package consulo.desktop.awt.ui.impl.image;

import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author VISTALL
 * @since 2020-08-09
 */
public class DesktopColorizeImageImpl extends JBUI.CachingScalableJBIcon<DesktopColorizeImageImpl> implements Image {
  private final Icon myBaseImage;
  private final ColorValue myColorValue;

  public DesktopColorizeImageImpl(Icon baseImage, ColorValue colorValue) {
    myBaseImage = baseImage;
    myColorValue = colorValue;
  }

  @Nonnull
  @Override
  protected DesktopColorizeImageImpl copy() {
    return new DesktopColorizeImageImpl(myBaseImage, myColorValue);
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    UIUtil.drawImage(g, colorize(myBaseImage, TargetAWT.to(myColorValue), false), x, y, null);
  }

  @Nonnull
  private BufferedImage colorize(@Nonnull final Icon source, @Nonnull Color color, boolean keepGray) {
    float[] base = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

    final BufferedImage image = UIUtil.createImage(source.getIconWidth(), source.getIconHeight(), Transparency.TRANSLUCENT);
    final Graphics2D g = image.createGraphics();
    source.paintIcon(null, g, 0, 0);
    g.dispose();

    final BufferedImage img = UIUtil.createImage(source.getIconWidth(), source.getIconHeight(), Transparency.TRANSLUCENT);
    int[] rgba = new int[4];
    float[] hsb = new float[3];
    for (int y = 0; y < image.getRaster().getHeight(); y++) {
      for (int x = 0; x < image.getRaster().getWidth(); x++) {
        image.getRaster().getPixel(x, y, rgba);
        if (rgba[3] != 0) {
          Color.RGBtoHSB(rgba[0], rgba[1], rgba[2], hsb);
          int rgb = Color.HSBtoRGB(base[0], base[1] * (keepGray ? hsb[1] : 1f), base[2] * hsb[2]);
          img.getRaster().setPixel(x, y, new int[]{rgb >> 16 & 0xff, rgb >> 8 & 0xff, rgb & 0xff, rgba[3]});
        }
      }
    }

    return img;
  }

  @Override
  public int getIconWidth() {
    return myBaseImage.getIconWidth();
  }

  @Override
  public int getIconHeight() {
    return myBaseImage.getIconHeight();
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }
}
