/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.ui.util;

import consulo.application.ApplicationProperties;
import consulo.desktop.awt.ui.impl.image.reference.DesktopAWTImageReference;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.BaseIconLibraryManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 04-Apr-22
 */
public class AppIconUtil {
  private static class FakeHolder {
    private static final JComponent fake = new JComponent() {
      @Override
      public String toString() {
        return "fake";
      }
    };
  }

  private static final int MIN_ICON_SIZE = 32;

  public static void updateWindowIcon(@Nonnull Window window) {
    window.setIconImages(loadWindowImages());
  }

  @Nonnull
  public static Image loadWindowIcon() {
    return new BaseMultiResolutionImage(loadWindowImages().toArray(Image[]::new));
  }

  private static List<Image> loadWindowImages() {
    boolean sandbox = ApplicationProperties.isInSandbox();

    ImageKey x16Key = sandbox ? PlatformIconGroup.icon16_sandbox() : PlatformIconGroup.icon16();
    ImageKey x32Key = sandbox ? PlatformIconGroup.icon32_sandbox() : PlatformIconGroup.icon32();

    BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();

    DesktopAWTImageReference x16Ref = (DesktopAWTImageReference)iconLibraryManager.resolveImage(IconLibraryManager.LIGHT_LIBRARY_ID,
                                                                                                x16Key.getGroupId(),
                                                                                                x16Key.getImageId());
    DesktopAWTImageReference x32Ref = (DesktopAWTImageReference)iconLibraryManager.resolveImage(IconLibraryManager.LIGHT_LIBRARY_ID,
                                                                                                x32Key.getGroupId(),
                                                                                                x32Key.getImageId());
    assert x16Ref != null;
    assert x32Ref != null;
    return List.of(toImage(x16Ref, x16Key.getWidth(), x16Key.getHeight()), toImage(x32Ref, x32Key.getWidth(), x32Key.getHeight()));
  }

  public static Image toImage(consulo.ui.image.Image image) {
    if (image instanceof ImageKey imageKey) {
      BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();

      DesktopAWTImageReference ref =
        (DesktopAWTImageReference)iconLibraryManager.resolveImage(null, imageKey.getGroupId(), imageKey.getImageId());

      assert ref != null;

      return toImage(ref, imageKey.getWidth(), imageKey.getHeight());
    }
    else {
      Icon icon = TargetAWT.to(image);

      BufferedImage bufferedImage = UIUtil.createImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

      Graphics2D graphics = bufferedImage.createGraphics();
      GraphicsUtil.setupAntialiasing(graphics);
      icon.paintIcon(FakeHolder.fake, graphics, 0, 0);
      graphics.dispose();
      return bufferedImage;
    }
  }

  private static Image toImage(DesktopAWTImageReference ref, int width, int height) {
    return toImage(ref, JBUI.ScaleContext.create(JBUI.Scale.create(1, JBUI.ScaleType.SYS_SCALE)), width, height);
  }

  @Nonnull
  private static Image toImage(DesktopAWTImageReference ref, JBUI.ScaleContext ctx, int width, int height) {
    BufferedImage x16Img = ImageUtil.createImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = x16Img.createGraphics();
    GraphicsUtil.setupAntialiasing(graphics);
    ref.draw(ctx, graphics, 0, 0, (int)width, (int)height);
    graphics.dispose();
    return x16Img;
  }

  @Nullable
  public static String findIcon(final String iconsPath) {
    final File iconsDir = new File(iconsPath);

    // 1. look for .svg icon
    for (String child : iconsDir.list()) {
      if (child.endsWith(".svg")) {
        return iconsPath + '/' + child;
      }
    }

    // 2. look for .png icon of max size
    int max = 0;
    String iconPath = null;
    for (String child : iconsDir.list()) {
      if (!child.endsWith(".png")) continue;
      final String path = iconsPath + '/' + child;
      final Icon icon = new ImageIcon(path);
      final int size = icon.getIconHeight();
      if (size >= MIN_ICON_SIZE && size > max && size == icon.getIconWidth()) {
        max = size;
        iconPath = path;
      }
    }

    return iconPath;
  }
}
