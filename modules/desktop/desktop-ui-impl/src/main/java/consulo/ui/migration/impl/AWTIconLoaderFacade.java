/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.migration.impl;

import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.reference.SoftReference;
import com.intellij.ui.RetrievableIcon;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.ImageLoader;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.RetinaImage;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.LinkedHashMap;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.logging.Logger;
import consulo.ui.desktop.internal.image.DesktopLazyImageImpl;
import consulo.desktop.util.awt.UIModificationTracker;
import consulo.ui.migration.IconLoaderFacade;
import consulo.ui.migration.SwingImageRef;
import consulo.ui.style.StyleManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static com.intellij.util.ui.JBUI.ScaleType.*;

/**
 * @author VISTALL
 * @since 2018-05-07
 */
public class AWTIconLoaderFacade implements IconLoaderFacade {
  private static final Logger LOG = Logger.getInstance(AWTIconLoaderFacade.class);

  private final ConcurrentMap<URL, CachedImageIcon> myIconsCache = ContainerUtil.newConcurrentMap(100, 0.9f, 2);
  /**
   * This cache contains mapping between icons and disabled icons.
   */
  private final Map<Icon, Icon> myIcon2DisabledIcon = ContainerUtil.createWeakMap(200);

  private static final ImageIcon EMPTY_ICON = new ImageIcon(UIUtil.createImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)) {
    @NonNls
    public String toString() {
      return "Empty icon " + super.toString();
    }
  };

  private static final UIModificationTracker ourUIModificationTracker = UIModificationTracker.getInstance();

  private static boolean ourIsActivated = false;

  @Override
  public void resetDark() {
    clearCache();
  }

  private void clearCache() {
    myIconsCache.clear();
    myIcon2DisabledIcon.clear();
  }

  @Nonnull
  public SwingImageRef getIcon(@NonNls @Nonnull final String path) {
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
  public SwingImageRef findIcon(@NonNls @Nonnull String path) {
    Class callerClass = ReflectionUtil.getGrandCallerClass();
    if (callerClass == null) return null;
    return findIcon(path, callerClass);
  }

  @Nonnull
  public SwingImageRef getIcon(@Nonnull String path, @Nonnull final Class aClass) {
    final SwingImageRef icon = findIcon(path, aClass);
    if (icon == null) {
      LOG.error("Icon cannot be found in '" + path + "', aClass='" + aClass + "'");
    }
    return icon;
  }

  @Override
  public void activate() {
    ourIsActivated = true;
  }

  private static boolean isLoaderDisabled() {
    return !ourIsActivated;
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link IconLoader#getIcon(String, Class)}
   */
  @Nullable
  public SwingImageRef findIcon(@Nonnull final String path, @Nonnull final Class aClass) {
    return findIcon(path, aClass, false);
  }

  @Nullable
  public SwingImageRef findIcon(@Nonnull String path, @Nonnull final Class aClass, boolean computeNow) {
    return findIcon(path, aClass, computeNow, IconLoader.STRICT);
  }

  @Nullable
  public SwingImageRef findIcon(@Nonnull String path, @Nonnull Class aClass, boolean computeNow, boolean strict) {
    String originalPath = path;
    Pair<String, Class> patchedPath = patchPath(path);
    path = patchedPath.first;
    if (patchedPath.second != null) {
      aClass = patchedPath.second;
    }
    if (isReflectivePath(path)) return getReflectiveIcon(path, aClass.getClassLoader());

    URL myURL = aClass.getResource(path);
    if (myURL == null) {
      if (strict) throw new RuntimeException("Can't find icon in '" + path + "' near " + aClass);
      return null;
    }
    final SwingImageRef icon = findIcon(myURL);
    if (icon instanceof CachedImageIcon) {
      ((CachedImageIcon)icon).myOriginalPath = originalPath;
      ((CachedImageIcon)icon).myClassLoader = aClass.getClassLoader();
    }
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
  public SwingImageRef findIcon(URL url) {
    return findIcon(url, true);
  }

  @Override
  @Nullable
  public SwingImageRef findIcon(URL url, boolean useCache) {
    if (url == null) {
      return null;
    }
    CachedImageIcon icon = myIconsCache.get(url);
    if (icon == null) {
      icon = new CachedImageIcon(url);
      if (useCache) {
        icon = ConcurrencyUtil.cacheOrGet(myIconsCache, url, icon);
      }
    }
    return icon;
  }

  @Override
  public void set(Icon icon, String originalPath, ClassLoader classLoader) {
    if (icon instanceof CachedImageIcon) {
      ((CachedImageIcon)icon).myOriginalPath = originalPath;
      ((CachedImageIcon)icon).myClassLoader = classLoader;
    }
  }

  @Nullable
  public SwingImageRef findIcon(@Nonnull String path, @Nonnull ClassLoader classLoader) {
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
    if (icon instanceof CachedImageIcon) {
      ((CachedImageIcon)icon).myOriginalPath = originalPath;
      ((CachedImageIcon)icon).myClassLoader = classLoader;
    }
    return icon;
  }

  @Nullable
  private static Icon checkIcon(final Image image, @Nonnull URL url) {
    if (image == null || image.getHeight(null) < 1) { // image wasn't loaded or broken
      return null;
    }

    final Icon icon = IconLoader.getIcon(image);
    if (icon != null && !isGoodSize(icon)) {
      LOG.error("Invalid icon: " + url); // # 22481
      return EMPTY_ICON;
    }
    assert icon instanceof ImageIcon;
    return icon;
  }

  public static boolean isGoodSize(@Nonnull final Icon icon) {
    return icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
  }

  /**
   * Gets (creates if necessary) disabled icon based on the passed one.
   *
   * @return <code>ImageIcon</code> constructed from disabled image of passed icon.
   */
  @Override
  @Nullable
  public Icon getDisabledIcon(@Nullable Icon icon) {
    if (icon instanceof DesktopLazyImageImpl) icon = ((DesktopLazyImageImpl)icon).getOrComputeIcon();
    if (icon == null) return null;

    Icon disabledIcon = myIcon2DisabledIcon.get(icon);
    if (disabledIcon == null) {
      disabledIcon = filterIcon(icon, UIUtil.getGrayFilter(StyleManager.get().getCurrentStyle().isDark()), null);
      myIcon2DisabledIcon.put(icon, disabledIcon);
    }
    return disabledIcon;
  }

  /**
   * Creates new icon with the filter applied.
   */
  @Nullable
  public static Icon filterIcon(@Nonnull Icon icon, RGBImageFilter filter, @Nullable Component ancestor) {
    if (icon instanceof DesktopLazyImageImpl) icon = ((DesktopLazyImageImpl)icon).getOrComputeIcon();
    if (icon == null) return null;

    if (!isGoodSize(icon)) {
      LOG.error(icon); // # 22481
      return EMPTY_ICON;
    }
    if (icon instanceof CachedImageIcon) {
      icon = ((CachedImageIcon)icon).asDisabledIcon();
    }
    else {
      final float scale;
      if (icon instanceof JBUI.ScaleContextAware) {
        scale = (float)((JBUI.ScaleContextAware)icon).getScale(SYS_SCALE);
      }
      else {
        scale = UIUtil.isJreHiDPI() ? JBUI.sysScale(ancestor) : 1f;
      }
      @SuppressWarnings("UndesirableClassUsage") BufferedImage image = new BufferedImage((int)(scale * icon.getIconWidth()), (int)(scale * icon.getIconHeight()), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D graphics = image.createGraphics();

      graphics.setColor(UIUtil.TRANSPARENT_COLOR);
      graphics.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
      graphics.scale(scale, scale);
      icon.paintIcon(LabelHolder.ourFakeComponent, graphics, 0, 0);

      graphics.dispose();

      Image img = ImageUtil.filter(image, filter);
      if (UIUtil.isJreHiDPI()) img = RetinaImage.createFrom(img, scale, null);

      icon = new JBImageIcon(img);
    }
    return icon;
  }

  @Override
  public Image toImage(Icon icon, @Nullable JBUI.ScaleContext ctx) {
    if (icon instanceof RetrievableIcon) {
      icon = ((RetrievableIcon)icon).retrieveIcon();
    }
    if (icon instanceof CachedImageIcon) {
      icon = ((CachedImageIcon)icon).getRealIcon(ctx);
    }
    if (icon instanceof ImageIcon) {
      return ((ImageIcon)icon).getImage();
    }
    else {
      BufferedImage image;
      if (GraphicsEnvironment.isHeadless()) { // for testing purpose
        image = UIUtil.createImage(ctx, icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB, PaintUtil.RoundingMode.FLOOR);
      }
      else {
        // [tav] todo: match the screen with the provided ctx
        image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
                .createCompatibleImage(icon.getIconWidth(), icon.getIconHeight(), Transparency.TRANSLUCENT);
      }
      Graphics2D g = image.createGraphics();
      try {
        icon.paintIcon(null, g, 0, 0);
      }
      finally {
        g.dispose();
      }
      return image;
    }
  }

  @Override
  @Nonnull
  public Icon getIconSnapshot(@Nonnull Icon icon) {
    if (icon instanceof CachedImageIcon) {
      return ((CachedImageIcon)icon).getRealIcon();
    }
    return icon;
  }

  public static final class CachedImageIcon extends JBUI.RasterJBIcon implements ScalableIcon, SwingImageRef, consulo.ui.image.Image {
    private static final Supplier<ImageFilter>[] EMPTY_FILTER_ARRAY = new Supplier[0];

    private volatile Object myRealIcon;
    private String myOriginalPath;
    private ClassLoader myClassLoader;
    @Nonnull
    private URL myUrl;
    private final boolean svg;
    private final boolean useCacheOnLoad;

    private volatile long myModificationCount;

    private Supplier<ImageFilter>[] myFilters = EMPTY_FILTER_ARRAY;
    private final MyScaledIconsCache myScaledIconsCache = new MyScaledIconsCache();

    {
      // For instance, ShadowPainter updates the context from outside.
      getScaleContext().addUpdateListener(new JBUI.BaseScaleContext.UpdateListener() {
        @Override
        public void contextUpdated() {
          myRealIcon = null;
        }
      });
    }

    private CachedImageIcon(@Nonnull CachedImageIcon icon) {
      myRealIcon = null; // to be computed
      myOriginalPath = icon.myOriginalPath;
      myClassLoader = icon.myClassLoader;
      myUrl = icon.myUrl;
      myModificationCount = icon.myModificationCount;
      svg = myOriginalPath != null && myOriginalPath.toLowerCase().endsWith("svg");
      useCacheOnLoad = icon.useCacheOnLoad;
    }

    public CachedImageIcon(@Nonnull URL url) {
      this(url, true);
    }

    public CachedImageIcon(@Nonnull URL url, boolean useCacheOnLoad) {
      myUrl = url;
      myModificationCount = ourUIModificationTracker.getModificationCount();
      svg = url.toString().endsWith("svg");
      this.useCacheOnLoad = useCacheOnLoad;
    }

    @Nonnull
    private synchronized Icon getRealIcon() {
      return getRealIcon(null);
    }

    @Nullable
    @TestOnly
    public ImageIcon doGetRealIcon() {
      Object icon = myRealIcon;
      if (icon instanceof Reference) {
        icon = ((Reference<ImageIcon>)icon).get();
      }
      return icon instanceof ImageIcon ? (ImageIcon)icon : null;
    }

    @Nonnull
    private synchronized Icon getRealIcon(@Nullable JBUI.ScaleContext ctx) {
      if (!isValid()) {
        if (isLoaderDisabled()) return EMPTY_ICON;
        myRealIcon = null;
        myModificationCount = ourUIModificationTracker.getModificationCount();
        myScaledIconsCache.clear();
      }

      if (!updateScaleContext(ctx) && myRealIcon != null) {
        // try returning the current icon as the context is up-to-date
        Object icon = myRealIcon;
        if (icon instanceof Reference) icon = ((Reference<ImageIcon>)icon).get();
        if (icon instanceof ImageIcon) return (ImageIcon)icon;
      }

      Icon icon = myScaledIconsCache.getOrScaleIcon(1f);
      if (icon != null) {
        myRealIcon = icon.getIconWidth() < 50 && icon.getIconHeight() < 50 ? icon : new SoftReference<>(icon);
        return icon;
      }
      return EMPTY_ICON;
    }

    private boolean isValid() {
      return myModificationCount == ourUIModificationTracker.getModificationCount();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2d = g instanceof Graphics2D ? (Graphics2D)g : null;
      getRealIcon(JBUI.ScaleContext.create(c, g2d)).paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
      return getRealIcon().getIconWidth();
    }

    @Override
    public int getIconHeight() {
      return getRealIcon().getIconHeight();
    }

    @Override
    public String toString() {
      return myUrl.toString();
    }

    @Override
    public float getScale() {
      return 1f;
    }

    @Override
    public boolean equals(Object obj) {
      if(obj instanceof CachedImageIcon) {
        CachedImageIcon other = (CachedImageIcon)obj;
        return myUrl.equals(other.myUrl) && Arrays.equals(myFilters, other.myFilters);
      }
      return super.equals(obj);
    }

    @Nonnull
    @Override
    public Icon scale(float scale) {
      if (scale == 1f) return this;

      getRealIcon(); // force state update & cache reset

      Icon icon = myScaledIconsCache.getOrScaleIcon(scale);
      if (icon != null) {
        return icon;
      }
      return this;
    }

    private Icon asDisabledIcon() {
      CachedImageIcon icon = new CachedImageIcon(this);
      icon.myFilters = new Supplier[]{new Supplier() {
        @Override
        public Object get() {
          return UIUtil.getGrayFilter(StyleManager.get().getCurrentStyle().isDark());
        }
      }};
      return icon;
    }

    private Image loadFromUrl(@Nonnull JBUI.ScaleContext ctx) {
      return ImageLoader.loadFromUrl(myUrl, true, useCacheOnLoad, myFilters, ctx);
    }

    @Override
    public int getHeight() {
      return getIconHeight();
    }

    @Override
    public int getWidth() {
      return getIconWidth();
    }

    private class MyScaledIconsCache {
      private static final int SCALED_ICONS_CACHE_LIMIT = 5;

      private final Map<Couple<Double>, SoftReference<Icon>> scaledIconsCache = Collections.synchronizedMap(new LinkedHashMap<Couple<Double>, SoftReference<Icon>>(SCALED_ICONS_CACHE_LIMIT) {
        @Override
        public boolean removeEldestEntry(Map.Entry<Couple<Double>, SoftReference<Icon>> entry) {
          return size() > SCALED_ICONS_CACHE_LIMIT;
        }
      });

      private Couple<Double> key(@Nonnull JBUI.ScaleContext ctx) {
        return new Couple<>(ctx.getScale(USR_SCALE) * ctx.getScale(OBJ_SCALE), ctx.getScale(SYS_SCALE));
      }

      /**
       * Retrieves the orig icon scaled by the provided scale.
       */
      Icon getOrScaleIcon(final float scale) {
        final JBUI.ScaleContext ctx = scale == 1 ? getScaleContext() : (JBUI.ScaleContext)getScaleContext().copy(); // not modifying this scale context
        if (scale != 1) ctx.update(OBJ_SCALE.of(scale));

        Icon icon = SoftReference.dereference(scaledIconsCache.get(key(ctx)));
        if (icon != null) {
          return icon;
        }

        if (svg) {
          SVGIconImpl svgIcon = new SVGIconImpl();
          svgIcon.setImageFilters(myFilters);
          icon = svgIcon;
          try {
            URI uri = myUrl.toURI();
            if (StyleManager.get().getCurrentStyle().isDark()) {
              String path = uri.toString();
              path = path.replace(".svg", "_dark.svg");
              uri = new URI(path);
            }
            try {
              InputStream stream = uri.toURL().openStream();
              stream.close();

              svgIcon.setSvgURI(uri);
            }
            catch (Exception e) {
              svgIcon.setSvgURI(myUrl.toURI());
            }
          }
          catch (URISyntaxException e) {
            throw new Error(e);
          }
        }
        else {
          Image image = loadFromUrl(ctx);

          icon = checkIcon(image, myUrl);
        }

        if (icon != null && icon.getIconWidth() * icon.getIconHeight() * 4 < ImageLoader.CACHED_IMAGE_MAX_SIZE) {
          scaledIconsCache.put(key(ctx), new SoftReference<>(icon));
        }
        return icon;
      }

      public void clear() {
        scaledIconsCache.clear();
      }
    }
  }

  private static class LabelHolder {
    /**
     * To get disabled icon with paint it into the image. Some icons require
     * not null component to paint.
     */
    private static final JComponent ourFakeComponent = new JLabel();
  }
}
