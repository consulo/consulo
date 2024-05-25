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
import consulo.desktop.awt.ui.impl.image.DesktopImage;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.ImageKey;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BaseMultiResolutionImage;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 04-Apr-22
 */
public class AppIconUtil {
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

    ImageKey x16 = sandbox ? PlatformIconGroup.icon16_sandbox() : PlatformIconGroup.icon16();
    ImageKey x32 = sandbox ? PlatformIconGroup.icon32_sandbox() : PlatformIconGroup.icon32();

    // force light theme - we don't need call #getActiveLibrary while loading
    x16 = (ImageKey)((DesktopImage)x16).copyWithTargetIconLibrary(IconLibraryManager.LIGHT_LIBRARY_ID, image -> image);
    x32 = (ImageKey)((DesktopImage)x32).copyWithTargetIconLibrary(IconLibraryManager.LIGHT_LIBRARY_ID, image -> image);

    // reset scale to default
    JBUI.ScaleContext ctx = JBUI.ScaleContext.create(JBUI.Scale.create(1, JBUI.ScaleType.SYS_SCALE));

    return ContainerUtil.map(List.of(TargetAWT.toImage(x16, ctx), TargetAWT.toImage(x32, ctx)), ImageUtil::toBufferedImage);
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
