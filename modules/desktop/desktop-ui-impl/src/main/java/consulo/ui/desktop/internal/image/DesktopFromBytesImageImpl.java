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
package consulo.ui.desktop.internal.image;

import com.intellij.util.ImageLoader;
import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.ui.JBImageIcon;
import consulo.awt.impl.ToSwingIconWrapper;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.image.BufferedImage;

/**
 * @author VISTALL
 * @since 2019-02-13
 */
public class DesktopFromBytesImageImpl implements Image, ToSwingIconWrapper {
  private Icon myIcon;

  public DesktopFromBytesImageImpl(byte[] bytes, int width, int height) {
    try {
      java.awt.Image image = ImageLoader.loadFromStream(new UnsyncByteArrayInputStream(bytes));
      JBHiDPIScaledImage icon = new JBHiDPIScaledImage(image, width, height, BufferedImage.TYPE_INT_ARGB);
      myIcon = new JBImageIcon(icon);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
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
