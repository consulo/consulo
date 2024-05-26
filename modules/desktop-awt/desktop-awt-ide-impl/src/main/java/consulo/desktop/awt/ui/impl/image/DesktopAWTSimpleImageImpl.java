/*
 * Copyright 2013-2024 consulo.io
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

import consulo.desktop.awt.ui.impl.image.reference.DesktopAWTImageReference;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * @author VISTALL
 * @since 26.05.2024
 */
public class DesktopAWTSimpleImageImpl extends JBUI.RasterJBIcon implements Image, DesktopAWTImage {
  private final DesktopAWTImageReference myImageReference;

  private int myWidth;
  private int myHeight;

  public DesktopAWTSimpleImageImpl(DesktopAWTImageReference imageReference, int width, int height) {
    myImageReference = imageReference;
    myWidth = width;
    myHeight = height;
  }

  @Override
  public int getHeight() {
    return myHeight;
  }

  @Override
  public int getWidth() {
    return myWidth;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    myImageReference.draw(JBUI.ScaleContext.create(c), (Graphics2D)g, x, y, myWidth, myHeight);
  }

  @Override
  public int getIconWidth() {
    return myWidth;
  }

  @Override
  public int getIconHeight() {
    return myHeight;
  }

  @Nonnull
  @Override
  public DesktopAWTImage copyWithNewSize(int width, int height) {
    return new DesktopAWTSimpleImageImpl(myImageReference, width, height);
  }

  @Nonnull
  @Override
  public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
    return this;
  }
}
