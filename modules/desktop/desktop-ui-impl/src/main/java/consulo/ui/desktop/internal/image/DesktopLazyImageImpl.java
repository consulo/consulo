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

import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.laf.UIModificationTracker;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-26
 */
public class DesktopLazyImageImpl extends JBUI.RasterJBIcon implements Image {
  private static final UIModificationTracker ourTracker = UIModificationTracker.getInstance();

  private boolean myWasComputed;
  private Icon myIcon;

  private Supplier<Image> myImageSupplier;

  private volatile long myModificationCount = -1;

  public DesktopLazyImageImpl(Supplier<Image> imageSupplier) {
    myImageSupplier = imageSupplier;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (updateScaleContext(JBUI.ScaleContext.create((Graphics2D)g))) {
      myIcon = null;
    }
    final Icon icon = getOrComputeIcon();
    if (icon != null) {
      icon.paintIcon(c, g, x, y);
    }
  }

  @Override
  public int getIconWidth() {
    final Icon icon = getOrComputeIcon();
    return icon != null ? icon.getIconWidth() : 0;
  }

  @Override
  public int getIconHeight() {
    final Icon icon = getOrComputeIcon();
    return icon != null ? icon.getIconHeight() : 0;
  }

  protected final synchronized Icon getOrComputeIcon() {
    long modificationCount = ourTracker.getModificationCount();

    if (!myWasComputed || myModificationCount != modificationCount || myIcon == null) {
      myModificationCount = modificationCount;
      myWasComputed = true;
      myIcon = TargetAWT.to(myImageSupplier.get());
    }

    return myIcon;
  }

  public final void load() {
    getIconWidth();
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