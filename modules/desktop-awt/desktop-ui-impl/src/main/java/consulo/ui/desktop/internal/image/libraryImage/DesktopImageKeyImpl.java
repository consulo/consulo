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
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.desktop.util.awt.UIModificationTracker;
import consulo.ui.desktop.internal.image.DesktopBaseLazyImageImpl;
import consulo.ui.desktop.internal.image.DesktopImage;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-09-27
 */
public class DesktopImageKeyImpl extends DesktopBaseLazyImageImpl implements ImageKey, Icon, DesktopLibraryInnerImage, DesktopImage<DesktopImageKeyImpl> {
  private static final BaseIconLibraryManager ourLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();
  private static final UIModificationTracker ourUIModificationTracker = UIModificationTracker.getInstance();

  @Nullable
  private final String myForceIconLibraryId;
  private final String myGroupId;
  private final String myImageId;
  private final int myWidth;
  private final int myHeight;
  private final float myScale;

  public DesktopImageKeyImpl(@Nullable String forceIconLibraryId, String groupId, String imageId, int width, int height) {
    this(forceIconLibraryId, groupId, imageId, width, height, 1f);
  }

  public DesktopImageKeyImpl(@Nullable String forceIconLibraryId, String groupId, String imageId, int width, int height, float scale) {
    myForceIconLibraryId = forceIconLibraryId;
    myGroupId = groupId;
    myImageId = imageId;
    myWidth = width;
    myHeight = height;
    myScale = scale;
  }

  @Override
  protected long getModificationCount() {
    return ourLibraryManager.getModificationCount() + ourUIModificationTracker.getModificationCount();
  }

  @Override
  public int getIconHeight() {
    return (int)Math.ceil(JBUI.scale(myHeight) * myScale);
  }

  @Override
  public int getIconWidth() {
    return (int)Math.ceil(JBUI.scale(myWidth) * myScale);
  }

  @Nonnull
  @Override
  protected Icon calcIcon() {
    Image icon = ourLibraryManager.getIcon(myForceIconLibraryId, myGroupId, myImageId, myWidth, myHeight);
    if (icon instanceof DesktopLibraryInnerImage) {
      ((DesktopLibraryInnerImage)icon).dropCache();

      icon = ((DesktopLibraryInnerImage)icon).copyWithScale(myScale);
    }

    if (icon == null) {
      icon = ImageEffects.colorFilled(getWidth(), getHeight(), StandardColors.RED);
    }
    return TargetAWT.to(icon);
  }

  @Nonnull
  @Override
  public DesktopImageKeyImpl copyWithScale(float scale) {
    if (scale == 1f) {
      return this;
    }
    return new DesktopImageKeyImpl(myForceIconLibraryId, myGroupId, myImageId, myWidth, myHeight, scale);
  }

  @Nonnull
  @Override
  public Image makeGrayed() {
    Image icon = ourLibraryManager.getIcon(myForceIconLibraryId, myGroupId, myImageId, myWidth, myHeight);

    if (icon instanceof DesktopLibraryInnerImage) {
      icon = ((DesktopLibraryInnerImage)icon).copyWithScale(myScale);

      return ((DesktopLibraryInnerImage)icon).makeGrayed();
    }

    return ImageEffects.colorFilled(myWidth, myHeight, StandardColors.LIGHT_GRAY);
  }

  @Nonnull
  @Override
  @SuppressWarnings("UndesirableClassUsage")
  public java.awt.Image toAWTImage(@Nullable JBUI.ScaleContext ctx) {
    Image icon = ourLibraryManager.getIcon(myForceIconLibraryId, myGroupId, myImageId, myWidth, myHeight);
    if (icon instanceof DesktopLibraryInnerImage) {
      ((DesktopLibraryInnerImage)icon).dropCache();
      return ((DesktopLibraryInnerImage)icon).toAWTImage(ctx);
    }

    BufferedImage b = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = b.createGraphics();
    g.setColor(JBColor.YELLOW);
    g.fillRect(0, 0, getWidth(), getHeight());
    g.dispose();

    return ImageUtil.scaleImage(b, myScale);
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

  @Nonnull
  @Override
  public DesktopImageKeyImpl copyWithTargetIconLibrary(@Nonnull String targetIconLibrary, @Nonnull Function<Image, Image> converter) {
    return new DesktopImageKeyImpl(targetIconLibrary, myGroupId, myImageId, myWidth, myHeight);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DesktopImageKeyImpl that = (DesktopImageKeyImpl)o;
    return Objects.equals(myForceIconLibraryId, that.myForceIconLibraryId) && Objects.equals(myGroupId, that.myGroupId) && Objects.equals(myImageId, that.myImageId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myForceIconLibraryId, myGroupId, myImageId);
  }

  @Override
  public String toString() {
    return "DesktopImageKeyImpl{" +
           "myForceIconLibraryId='" +
           myForceIconLibraryId +
           '\'' +
           ", myGroupId='" +
           myGroupId +
           '\'' +
           ", myImageId='" +
           myImageId +
           '\'' +
           ", myWidth=" +
           myWidth +
           ", myHeight=" +
           myHeight +
           '}';
  }
}
