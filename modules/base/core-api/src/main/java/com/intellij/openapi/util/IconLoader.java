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
import com.intellij.util.ImageLoader;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import consulo.annotations.DeprecationInfo;
import consulo.annotations.ReviewAfterMigrationToJRE;
import consulo.container.StartupError;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.ui.UIInternal;
import consulo.ui.migration.IconLoaderFacade;
import consulo.ui.migration.SwingImageRef;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

@SuppressWarnings("deprecation")
public final class IconLoader {
  private static final Logger LOG = Logger.getInstance(IconLoader.class);

  private static IconLoaderFacade ourIconLoaderFacade = findImplementation(IconLoaderFacade.class);

  @Nonnull
  @ReviewAfterMigrationToJRE(value = 9, description = "Use consulo.container.plugin.util.PlatformServiceLocator#findImplementation after migration")
  private static <T> T findImplementation(@Nonnull Class<T> interfaceClass) {
    for (T value : ServiceLoader.load(interfaceClass, UIInternal.class.getClassLoader())) {
      return value;
    }

    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      if (PluginIds.isPlatformImplementationPlugin(descriptor.getPluginId())) {
        ServiceLoader<T> loader = ServiceLoader.load(interfaceClass, descriptor.getPluginClassLoader());

        Iterator<T> iterator = loader.iterator();
        if (iterator.hasNext()) {
          return iterator.next();
        }
      }
    }

    throw new StartupError("Can't find platform implementation: " + interfaceClass);
  }

  public static boolean STRICT = false;

  private IconLoader() {
  }

  @Deprecated
  public static Icon getIcon(@Nonnull final java.awt.Image image) {
    return new JBImageIcon(image);
  }

  public static void resetDark() {
    ourIconLoaderFacade.resetDark();
  }

  @Nonnull
  public static SwingImageRef getIcon(@NonNls @Nonnull final String path) {
    Class callerClass = ReflectionUtil.getGrandCallerClass();

    assert callerClass != null : path;
    return getIcon(path, callerClass);
  }

  @Nullable
  private static SwingImageRef getReflectiveIcon(@Nonnull String path, ClassLoader classLoader) {
    try {
      @NonNls String pckg = path.startsWith("AllIcons.") ? "com.intellij.icons." : "icons.";
      Class cur = Class.forName(pckg + path.substring(0, path.lastIndexOf('.')).replace('.', '$'), true, classLoader);
      Field field = cur.getField(path.substring(path.lastIndexOf('.') + 1));

      return (SwingImageRef)field.get(null);
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link IconLoader#getIcon(String)}
   */
  @Nullable
  public static SwingImageRef findIcon(@NonNls @Nonnull String path) {
    Class callerClass = ReflectionUtil.getGrandCallerClass();
    if (callerClass == null) return null;
    return findIcon(path, callerClass);
  }

  @Nonnull
  public static SwingImageRef getIcon(@Nonnull String path, @Nonnull final Class aClass) {
    final SwingImageRef icon = findIcon(path, aClass);
    if (icon == null) {
      LOG.error("Icon cannot be found in '" + path + "', aClass='" + aClass + "'");
    }
    return icon;
  }

  public static void activate() {
    ourIconLoaderFacade.activate();
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
    String originalPath = path;
    Pair<String, Class> patchedPath = patchPath(path);
    path = patchedPath.first;
    if (patchedPath.second != null) {
      aClass = patchedPath.second;
    }
    if (isReflectivePath(path)) return getReflectiveIcon(path, aClass.getClassLoader());

    URL myURL = aClass.getResource(path);
    if (myURL == null && path.endsWith(".png")) {
      path = path.replace(".png", ".svg");
      myURL = aClass.getResource(path);
    }

    if (myURL == null) {
      if (strict) throw new RuntimeException("Can't find icon in '" + path + "' near " + aClass);
      return null;
    }
    final SwingImageRef icon = findIcon(myURL);

    ourIconLoaderFacade.set(icon, originalPath, aClass.getClassLoader());
    return icon;
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
    return ourIconLoaderFacade.findIcon(url, useCache);
  }

  @Nullable
  public static SwingImageRef findIcon(@Nonnull String path, @Nonnull ClassLoader classLoader) {
    String originalPath = path;
    Pair<String, Class> patchedPath = patchPath(path);
    path = patchedPath.first;
    if (patchedPath.second != null) {
      classLoader = patchedPath.second.getClassLoader();
    }
    if (isReflectivePath(path)) return getReflectiveIcon(path, classLoader);
    if (!StringUtil.startsWithChar(path, '/')) return null;

    final URL url = classLoader.getResource(path.substring(1));
    final SwingImageRef icon = findIcon(url);

    ourIconLoaderFacade.set(icon, originalPath, classLoader);
    return icon;
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
    return ourIconLoaderFacade.getDisabledIcon(icon);
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
    return ourIconLoaderFacade.getIconSnapshot(icon);
  }

  @Nullable
  public static Image toImage(@Nonnull Icon icon, @Nullable JBUI.ScaleContext ctx) {
    return ourIconLoaderFacade.toImage(icon, ctx);
  }
}
