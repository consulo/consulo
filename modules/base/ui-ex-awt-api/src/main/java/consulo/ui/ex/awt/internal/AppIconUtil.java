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
package consulo.ui.ex.awt.internal;

import consulo.annotation.DeprecationInfo;
import consulo.application.ApplicationProperties;
import consulo.ui.style.StyleManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BaseMultiResolutionImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 04-Apr-22
 */
public class AppIconUtil {
  private static final int MIN_ICON_SIZE = 32;

  public static void updateWindowIcon(@Nonnull Window window) {
    updateWindowIcon(window, StyleManager.get().getCurrentStyle().isDark());
  }

  @SuppressWarnings("deprecation")
  public static void updateWindowIcon(@Nonnull Window window, boolean isDark) {
    List<Image> images = new ArrayList<>(2);

    images.add(ImageLoader.loadFromResource(getIconUrl(), AppIconUtil.class, isDark));
    images.add(ImageLoader.loadFromResource(getSmallIconUrl(), AppIconUtil.class, isDark));

    for (int i = 0; i < images.size(); i++) {
      Image image = images.get(i);
      if (image instanceof JBHiDPIScaledImage) {
        images.set(i, ((JBHiDPIScaledImage)image).getDelegate());
      }
    }

    window.setIconImages(images);
  }

  @SuppressWarnings("deprecation")
  @Nonnull
  public static Image loadWindowIcon(boolean isDark) {
    List<Image> images = new ArrayList<>(2);

    images.add(ImageLoader.loadFromResource(getIconUrl(), AppIconUtil.class, isDark));
    images.add(ImageLoader.loadFromResource(getSmallIconUrl(), AppIconUtil.class, isDark));

    for (int i = 0; i < images.size(); i++) {
      Image image = images.get(i);
      if (image instanceof JBHiDPIScaledImage) {
        images.set(i, ((JBHiDPIScaledImage)image).getDelegate());
      }
    }

    return new BaseMultiResolutionImage(images.toArray(Image[]::new));
  }


  @Deprecated
  @DeprecationInfo("Do not use this method. Use SandboxUtil.getAppIcon()")
  public static String getIconUrl() {
    return getUrl("/icon32");
  }

  @Deprecated
  @DeprecationInfo("Do not use this method. Use SandboxUtil.getAppIcon()")
  public static String getSmallIconUrl() {
    return getUrl("/icon16");
  }

  private static String getUrl(String prefix) {
    return (ApplicationProperties.isInSandbox() ? prefix + "-sandbox" : prefix) + ".png";
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
