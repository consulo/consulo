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
package consulo.ide.impl.idea.openapi.util;

import consulo.util.lang.StringUtil;
import consulo.ui.ex.JBColor;
import consulo.ide.impl.idea.util.ExceptionUtil;
import consulo.ui.ex.awt.internal.ImageLoader;
import consulo.ide.impl.idea.util.ReflectionUtil;
import consulo.ide.impl.idea.util.ui.JBImageIcon;
import consulo.ui.ex.awt.JBUI;
import consulo.annotation.DeprecationInfo;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.Pair;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

@Deprecated
public final class NotWorkingIconLoader {
  //private static final Logger LOG = Logger.getInstance(IconLoader.class);

  public static class DummyIcon implements consulo.ui.image.Image, Icon {
    private final String myCreationTrace = ExceptionUtil.getThrowableText(new Exception());

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
  }

  public static boolean STRICT = false;

  private NotWorkingIconLoader() {
  }

  @Deprecated
  public static Icon getIcon(@Nonnull final java.awt.Image image) {
    return new JBImageIcon(image);
  }

  public static void resetDark() {
  }

  @Nonnull
  public static Icon getIcon(@NonNls @Nonnull final String path) {
    return new DummyIcon();
  }

  @Nullable
  private static Icon getReflectiveIcon(@Nonnull String path, ClassLoader classLoader) {
    return new DummyIcon();
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link NotWorkingIconLoader#getIcon(String)}
   */
  @Nullable
  public static Icon findIcon(@NonNls @Nonnull String path) {
    return new DummyIcon();
  }

  @Nonnull
  public static Icon getIcon(@Nonnull String path, @Nonnull final Class aClass) {
    return new DummyIcon();
  }

  public static void activate() {
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link NotWorkingIconLoader#getIcon(String, Class)}
   */
  @Nullable
  public static Icon findIcon(@Nonnull final String path, @Nonnull final Class aClass) {
    return findIcon(path, aClass, false);
  }

  @Nullable
  public static Icon findIcon(@Nonnull String path, @Nonnull final Class aClass, boolean computeNow) {
    return findIcon(path, aClass, computeNow, STRICT);
  }

  @Nullable
  public static Icon findIcon(@Nonnull String path, @Nonnull Class aClass, boolean computeNow, boolean strict) {
    return new DummyIcon();
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
  public static Icon findIcon(URL url) {
    return findIcon(url, true);
  }


  @Nullable
  public static Icon findIcon(@NonNls @Nonnull String path, boolean strict) {
    Class callerClass = ReflectionUtil.getGrandCallerClass();
    if (callerClass == null) return null;
    return findIcon(path, callerClass, false, strict);
  }

  @Nullable
  public static Icon findIcon(URL url, boolean useCache) {
    return new DummyIcon();
  }

  @Nullable
  public static Icon findIcon(@Nonnull String path, @Nonnull ClassLoader classLoader) {
    return new DummyIcon();
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
    if (icon == null) {
      return null;
    }
    return TargetAWT.to(ImageEffects.grayed(TargetAWT.from(icon)));
  }

  @Nullable
  public static Image toImage(@Nonnull Icon icon, @Nullable JBUI.ScaleContext ctx) {
    return new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
  }
}
