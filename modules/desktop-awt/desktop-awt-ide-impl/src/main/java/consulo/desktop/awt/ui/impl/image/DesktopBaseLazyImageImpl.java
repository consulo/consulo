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

import consulo.ui.ex.awt.JBUI;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-09-26
 */
public abstract class DesktopBaseLazyImageImpl extends JBUI.RasterJBIcon implements Image {
  private Icon myIcon;

  private volatile long myModificationCount = -1;

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (updateScaleContext(JBUI.ScaleContext.create((Graphics2D)g))) {
      myIcon = null;
    }
    Icon icon = getOrComputeIcon();
    icon.paintIcon(c, g, x, y);
  }

  @Override
  public int getIconWidth() {
    Icon icon = getOrComputeIcon();
    return icon.getIconWidth();
  }

  @Override
  public int getIconHeight() {
    Icon icon = getOrComputeIcon();
    return icon.getIconHeight();
  }

  public final synchronized Icon getOrComputeIcon() {
    long modificationCount = getModificationCount();

    if (myModificationCount != modificationCount) {
      myModificationCount = modificationCount;
      myIcon = null;
    }

    Icon icon = myIcon;
    if(icon == null) {
      icon = calcIcon();
      myIcon = icon;
    }

    return icon;
  }

  protected abstract long getModificationCount();
  
  @Nonnull
  protected abstract Icon calcIcon();

  public final void load() {
    getIconWidth();
  }

  @Override
  public final int getHeight() {
    return getIconHeight();
  }

  @Override
  public final int getWidth() {
    return getIconWidth();
  }
}