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
import consulo.desktop.awt.ui.impl.image.DesktopAWTScalableImage;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.ImageKey;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author VISTALL
 * @since 04-Apr-22
 */
public class AppIconUtil {
  private static final int MIN_ICON_SIZE = 32;

  public static void updateWindowIcon(@Nonnull Window window) {
    window.setIconImage(loadWindowIcon());
  }

  @Nonnull
  public static Image loadWindowIcon() {
    boolean sandbox = ApplicationProperties.isInSandbox();
    ImageKey x16Key = sandbox ? PlatformIconGroup.icon16_sandbox() : PlatformIconGroup.icon16();
    ImageKey x32Key = sandbox ? PlatformIconGroup.icon32_sandbox() : PlatformIconGroup.icon32();
    return new DesktopAWTScalableImage(IconLibraryManager.LIGHT_LIBRARY_ID, x16Key, x32Key);
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
