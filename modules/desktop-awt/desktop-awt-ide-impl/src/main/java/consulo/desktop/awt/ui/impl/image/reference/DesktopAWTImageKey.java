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
package consulo.desktop.awt.ui.impl.image.reference;

import consulo.desktop.awt.ui.impl.image.DesktopAWTImage;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.UIModificationTracker;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.impl.image.ImageReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-09-27
 */
public class DesktopAWTImageKey extends JBUI.RasterJBIcon implements DesktopAWTImage, ImageKey, Icon {
  private static final BaseIconLibraryManager ourLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();
  private static final UIModificationTracker ourUIModificationTracker = UIModificationTracker.getInstance();

  @Nullable
  private final String myForceIconLibraryId;
  private final String myGroupId;
  private final String myImageId;
  private final int myWidth;
  private final int myHeight;

  private transient long myModificationCount = -1;
  private transient ImageReference myImageReference;

  public DesktopAWTImageKey(@Nullable String forceIconLibraryId, String groupId, String imageId, int width, int height) {
    myForceIconLibraryId = forceIconLibraryId;
    myGroupId = groupId;
    myImageId = imageId;
    myWidth = width;
    myHeight = height;
  }

  @Override
  public int getIconHeight() {
    return (int)Math.ceil(JBUI.scale(myHeight));
  }

  @Override
  public int getIconWidth() {
    return (int)Math.ceil(JBUI.scale(myWidth));
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
  public void paintIcon(Component c, Graphics g, int x, int y) {
    JBUI.ScaleContext ctx = JBUI.ScaleContext.create(c);
    if (updateScaleContext(ctx)) {
      myModificationCount = -1;
      myImageReference = null;
    }

    ImageReference reference = resolveReference();
    if (reference == ImageReference.INVALID) {
      g.setColor(JBColor.YELLOW);
      g.fillRect(x, y, getWidth(), getHeight());
    }
    else {
      ((DesktopAWTImageReference)reference).draw(ctx, (Graphics2D) g, x, y, getWidth(), getHeight());
    }
  }

  private long currentCount() {
    return ourLibraryManager.getModificationCount() + ourUIModificationTracker.getModificationCount();
  }

  @Nonnull
  private ImageReference resolveReference() {
    long l = currentCount();
    long lastModCount = myModificationCount;
    ImageReference lastRef = myImageReference;

    if (l == lastModCount && lastRef != null) {
      return lastRef;
    }

    ImageReference ref = ourLibraryManager.resolveImage(myForceIconLibraryId, myGroupId, myImageId);
    if (ref == null) {
      ref = ImageReference.INVALID;
    }

    myModificationCount = l;
    myImageReference = ref;
    return ref;
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DesktopAWTImageKey that = (DesktopAWTImageKey)o;
    return Objects.equals(myForceIconLibraryId, that.myForceIconLibraryId) && Objects.equals(myGroupId, that.myGroupId) && Objects.equals(
      myImageId,
      that.myImageId);
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

  @Nonnull
  @Override
  public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
    return new DesktopAWTImageKey(libraryId, myGroupId, myImageId, myWidth, myHeight);
  }

  @Nonnull
  @Override
  public DesktopAWTImage copyWithNewSize(int width, int height) {
    return new DesktopAWTImageKey(myForceIconLibraryId, myGroupId, myImageId, width, height);
  }
}
