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

import com.intellij.ui.JBColor;
import consulo.awt.TargetAWT;
import consulo.desktop.util.awt.UIModificationTracker;
import consulo.ui.desktop.internal.image.DesktopBaseLazyImageImpl;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.migration.SwingImageRef;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author VISTALL
 * @since 2020-09-27
 */
public class DesktopImageKeyImpl extends DesktopBaseLazyImageImpl implements ImageKey, SwingImageRef, DesktopLibraryInnerImage {
  private static final BaseIconLibraryManager ourLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();
  private static final UIModificationTracker ourUIModificationTracker = UIModificationTracker.getInstance();

  private final String myGroupId;
  private final String myImageId;
  private final int myWidth;
  private final int myHeight;

  public DesktopImageKeyImpl(String groupId, String imageId, int width, int height) {
    myGroupId = groupId;
    myImageId = imageId;
    myWidth = width;
    myHeight = height;
  }

  @Override
  protected long getModificationCount() {
    return ourLibraryManager.getModificationCount() + ourUIModificationTracker.getModificationCount();
  }

  @Nonnull
  @Override
  protected Icon calcIcon() {
    Image icon = ourLibraryManager.getIcon(myGroupId, myImageId, myWidth, myHeight);
    if (icon instanceof DesktopLibraryInnerImage) {
      ((DesktopLibraryInnerImage)icon).dropCache();
    }

    if (icon == null) {
      icon = ImageEffects.colorFilled(myWidth, myHeight, StandardColors.RED);
    }
    return TargetAWT.to(icon);
  }

  @Nonnull
  @Override
  public Image makeGrayed() {
    Image icon = ourLibraryManager.getIcon(myGroupId, myImageId, myWidth, myHeight);

    if (icon instanceof DesktopLibraryInnerImage) {
      return ((DesktopLibraryInnerImage)icon).makeGrayed();
    }

    return ImageEffects.colorFilled(myWidth, myHeight, StandardColors.LIGHT_GRAY);
  }

  @Nonnull
  @Override
  @SuppressWarnings("UndesirableClassUsage")
  public java.awt.Image toAWTImage() {
    Image icon = ourLibraryManager.getIcon(myGroupId, myImageId, myWidth, myHeight);
    if (icon instanceof DesktopLibraryInnerImage) {
      return ((DesktopLibraryInnerImage)icon).toAWTImage();
    }

    BufferedImage b = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = b.createGraphics();
    g.setColor(JBColor.YELLOW);
    g.fillRect(0, 0, getWidth(), getHeight());
    g.dispose();

    return b;
  }

  @Nonnull
  @Override
  public String getGroupId() {
    return myGroupId;
  }

  @Nonnull
  @Override
  public String getImageId() {
    return myImageId;
  }

  @Override
  public void dropCache() {
    throw new UnsupportedOperationException();
  }
}
