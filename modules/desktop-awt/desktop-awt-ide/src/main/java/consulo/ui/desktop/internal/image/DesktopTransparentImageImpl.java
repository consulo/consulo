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
package consulo.ui.desktop.internal.image;

import com.intellij.ui.RetrievableIcon;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2018-05-06
 */
public class DesktopTransparentImageImpl implements RetrievableIcon, Image {
  private final consulo.ui.image.Image myImage;
  private final float myAlpha;

  public DesktopTransparentImageImpl(Image image, float alpha) {
    myImage = image;
    myAlpha = alpha;
  }

  @Nullable
  @Override
  public Icon retrieveIcon() {
    return TargetAWT.to(myImage);
  }

  @Override
  public int getIconHeight() {
    return myImage.getHeight();
  }

  @Override
  public int getIconWidth() {
    return myImage.getWidth();
  }

  @Override
  public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
    final Graphics2D g2 = (Graphics2D)g;
    final Composite saveComposite = g2.getComposite();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myAlpha));
    Icon icon = retrieveIcon();
    if (icon != null) {
      icon.paintIcon(c, g2, x, y);
    }
    g2.setComposite(saveComposite);
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DesktopTransparentImageImpl that = (DesktopTransparentImageImpl)o;
    return Float.compare(that.myAlpha, myAlpha) == 0 && Objects.equals(myImage, that.myImage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myImage, myAlpha);
  }
}
