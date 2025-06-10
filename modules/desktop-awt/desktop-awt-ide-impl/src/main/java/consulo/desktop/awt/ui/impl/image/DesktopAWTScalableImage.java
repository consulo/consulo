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
import consulo.desktop.awt.uiOld.FakeComponent;
import consulo.ui.Size2D;
import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.BaseIconLibraryManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

/**
 * @author VISTALL
 * @since 02.06.2024
 */
public class DesktopAWTScalableImage extends AbstractMultiResolutionImage {
  private final Map<Size2D, Image> myCachedImages = new HashMap<>();

  private final List<consulo.ui.image.Image> myImages;

  private final String myForceLibraryId;

  private final consulo.ui.image.Image myBaseImage;

  public DesktopAWTScalableImage(consulo.ui.image.Image baseImage, consulo.ui.image.Image... otherImages) {
    this(null, baseImage, otherImages);
  }

  public DesktopAWTScalableImage(String forceLibraryId, consulo.ui.image.Image baseImage, consulo.ui.image.Image... otherImages) {
    myForceLibraryId = forceLibraryId;
    myBaseImage = baseImage;
    myImages = new ArrayList<>(otherImages.length + 1);
    myImages.add(baseImage);
    Collections.addAll(myImages, otherImages);
    myImages.sort((o1, o2) -> Integer.compareUnsigned(o2.getWidth() * o2.getHeight(), o1.getHeight() * o1.getWidth()));
  }

  private Image getOrScale(Size2D expectedSize) {
    Image image = myCachedImages.get(expectedSize);
    if (image != null) {
      return image;
    }

    consulo.ui.image.Image closestImage = myImages.get(0);

    for (consulo.ui.image.Image uiImage : myImages) {
      Size2D uiSize = uiImage.getSize();

      if (expectedSize.equals(uiSize)) {
        return toImage(uiImage, expectedSize);
      }

      if (uiImage.getWidth() >= expectedSize.width() && uiImage.getHeight() >= expectedSize.height()) {
        closestImage = uiImage;
      }
    }

    Image result = toImage(closestImage, expectedSize);
    myCachedImages.put(expectedSize, result);
    return result;
  }

  @SuppressWarnings("UndesirableClassUsage")
  private Image toImage(consulo.ui.image.Image image, Size2D expectedSize) {
    int width = expectedSize.width();
    int height = expectedSize.height();

    if (image instanceof ImageKey imageKey) {
      BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();

      DesktopAWTImageReference ref =
        (DesktopAWTImageReference)iconLibraryManager.resolveImage(myForceLibraryId, imageKey);

      assert ref != null;

      BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = bufferedImage.createGraphics();
      ref.draw(JBUI.ScaleContext.create(JBUI.Scale.create(1, JBUI.ScaleType.SYS_SCALE)), graphics, 0, 0, width, height);
      graphics.dispose();
      return bufferedImage;
    }
    else {
      Icon icon = TargetAWT.to(image);

      BufferedImage bufferedImage = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = bufferedImage.createGraphics();
      GraphicsUtil.setupAntialiasing(graphics);
      icon.paintIcon(FakeComponent.INSTANCE, graphics, 0, 0);
      graphics.dispose();
      return bufferedImage;
    }
  }

  @Override
  protected Image getBaseImage() {
    return getOrScale(myBaseImage.getSize());
  }

  @Override
  public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
    return getOrScale(new Size2D((int)Math.ceil(destImageWidth), (int)Math.ceil(destImageHeight)));
  }

  @Override
  public List<Image> getResolutionVariants() {
    return myImages.stream().map(it -> getOrScale(it.getSize())).toList();
  }
}
