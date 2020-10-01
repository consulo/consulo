/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.util;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ImageLoader;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import consulo.annotation.DeprecationInfo;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.ui.image.ImageEffects;
import consulo.ui.migration.SwingImageRef;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

@Deprecated
public final class IconLoader {
  private static final Logger LOG = Logger.getInstance(IconLoader.class);

  public static final SwingImageRef dummyIcon = new SwingImageRef() {
    @Override
    public int getHeight() {
      return consulo.ui.image.Image.DEFAULT_ICON_SIZE;
    }

    @Override
    public int getWidth() {
      return consulo.ui.image.Image.DEFAULT_ICON_SIZE;
    }

    @Override
    public int getIconWidth() {
      return consulo.ui.image.Image.DEFAULT_ICON_SIZE;
    }

    @Override
    public int getIconHeight() {
      return consulo.ui.image.Image.DEFAULT_ICON_SIZE;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(JBColor.GREEN);
      g.fillRect(x, y, getWidth(), getHeight());

    }
  };
  public static boolean STRICT = false;

  private IconLoader() {
  }

  @Deprecated
  public static Icon getIcon(@Nonnull final java.awt.Image image) {
    return new JBImageIcon(image);
  }

  public static void resetDark() {
  }

  @Nonnull
  public static SwingImageRef getIcon(@NonNls @Nonnull final String path) {
    Class callerClass = ReflectionUtil.getGrandCallerClass();

    assert callerClass != null : path;
    return getIcon(path, callerClass);
  }

  @Nullable
  private static SwingImageRef getReflectiveIcon(@Nonnull String path, ClassLoader classLoader) {
    return dummyIcon;
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link IconLoader#getIcon(String)}
   */
  @Nullable
  public static SwingImageRef findIcon(@NonNls @Nonnull String path) {
    return dummyIcon;
  }

  @Nonnull
  public static SwingImageRef getIcon(@Nonnull String path, @Nonnull final Class aClass) {
    return dummyIcon;
  }

  public static void activate() {
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link IconLoader#getIcon(String, Class)}
   */
  @Nullable
  public static SwingImageRef findIcon(@Nonnull final String path, @Nonnull final Class aClass) {
    return findIcon(path, aClass, false);
  }

  @Nullable
  public static SwingImageRef findIcon(@Nonnull String path, @Nonnull final Class aClass, boolean computeNow) {
    return findIcon(path, aClass, computeNow, STRICT);
  }

  @Nullable
  public static SwingImageRef findIcon(@Nonnull String path, @Nonnull Class aClass, boolean computeNow, boolean strict) {
    return dummyIcon;
  }

  @Nonnull
  private static Pair<String, Class> patchPath(@Nonnull String path) {
    return Pair.create(path, null);
  }

  private static boolean isReflectivePath(@Nonnull String path) {
    List<String> paths = StringUtil.split(path, ".");
    return paths.size() > 1 && paths.get(0).endsWith("Icons");
  }

  @Nullable
  public static SwingImageRef findIcon(URL url) {
    return findIcon(url, true);
  }


  @Nullable
  public static Icon findIcon(@NonNls @Nonnull String path, boolean strict) {
    Class callerClass = ReflectionUtil.getGrandCallerClass();
    if (callerClass == null) return null;
    return findIcon(path, callerClass, false, strict);
  }

  @Nullable
  public static SwingImageRef findIcon(URL url, boolean useCache) {
    return dummyIcon;
  }

  @Nullable
  public static SwingImageRef findIcon(@Nonnull String path, @Nonnull ClassLoader classLoader) {
    return dummyIcon;
  }

  public static boolean isGoodSize(@Nonnull final Icon icon) {
    return ImageLoader.isGoodSize(icon);
  }

  /**
   * Gets (creates if necessary) disabled icon based on the passed one.
   *
   * @return <code>ImageIcon</code> constructed from disabled image of passed icon.
   */
  @Nullable
  @Deprecated
  @DeprecationInfo("Use ImageEffects#grayed()")
  public static Icon getDisabledIcon(@Nullable Icon icon) {
    if(icon == null) {
      return null;
    }
    return TargetAWT.to(ImageEffects.grayed(TargetAWT.from(icon)));
  }

  /**
   * Gets a snapshot of the icon, immune to changes made by these calls:
   * {@link {@link IconLoader#resetDark()}
   *
   * @param icon the source icon
   * @return the icon snapshot
   */
  @Nonnull
  public static Icon getIconSnapshot(@Nonnull Icon icon) {
    return icon;
  }

  @Nullable
  public static Image toImage(@Nonnull Icon icon, @Nullable JBUI.ScaleContext ctx) {
    return new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
  }
}
