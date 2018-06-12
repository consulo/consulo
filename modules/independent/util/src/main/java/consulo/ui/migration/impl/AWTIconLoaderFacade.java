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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.reference.SoftReference;
import com.intellij.ui.RetrievableIcon;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.ImageLoader;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.RetinaImage;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.WeakHashMap;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.containers.hash.LinkedHashMap;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ui.migration.IconLoaderFacade;
import consulo.ui.migration.SwingImageRef;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 2018-05-07
 */
@SuppressWarnings("deprecation")
public class AWTIconLoaderFacade implements IconLoaderFacade {
  private static final Logger LOG = Logger.getInstance(AWTIconLoaderFacade.class);

  private final ConcurrentMap<URL, CachedImageIcon> myIconsCache = ContainerUtil.newConcurrentMap(100, 0.9f, 2);
  /**
   * This cache contains mapping between icons and disabled icons.
   */
  private final Map<Icon, Icon> myIcon2DisabledIcon = new WeakHashMap<Icon, Icon>(200);

  private static boolean USE_DARK_ICONS = UIUtil.isUnderDarcula();

  private static final ImageIcon EMPTY_ICON = new ImageIcon(UIUtil.createImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)) {
    @NonNls
    public String toString() {
      return "Empty icon " + super.toString();
    }
  };

  private static boolean ourIsActivated = false;

  @Override
  public void setUseDarkIcons(boolean useDarkIcons) {
    USE_DARK_ICONS = useDarkIcons;
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
  private static ImageIcon checkIcon(final Image image, @Nonnull URL url) {
    if (image == null || image.getHeight(null) < 1) { // image wasn't loaded or broken
      return null;
    }

    final Icon icon = IconLoader.getIcon(image);
    if (icon != null && !isGoodSize(icon)) {
      LOG.error("Invalid icon: " + url); // # 22481
      return EMPTY_ICON;
    }
    assert icon instanceof ImageIcon;
    return (ImageIcon)icon;
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
    if (icon instanceof LazyIcon) icon = ((LazyIcon)icon).getOrComputeIcon();
    if (icon == null) return null;

    Icon disabledIcon = myIcon2DisabledIcon.get(icon);
    if (disabledIcon == null) {
      if (!isGoodSize(icon)) {
        LOG.error(icon); // # 22481
        return EMPTY_ICON;
      }
      if (icon instanceof CachedImageIcon) {
        disabledIcon = ((CachedImageIcon)icon).asDisabledIcon();
      }
      else {
        final float scale = UIUtil.isJreHiDPI() ? JBUI.sysScale() : 1f;  // [tav] todo: no screen available
        @SuppressWarnings("UndesirableClassUsage") BufferedImage image = new BufferedImage((int)(scale * icon.getIconWidth()), (int)(scale * icon.getIconHeight()), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();

        graphics.setColor(UIUtil.TRANSPARENT_COLOR);
        graphics.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
        graphics.scale(scale, scale);
        icon.paintIcon(LabelHolder.ourFakeComponent, graphics, 0, 0);

        graphics.dispose();

        Image img = ImageUtil.filter(image, UIUtil.getGrayFilter());
        if (UIUtil.isJreHiDPI()) img = RetinaImage.createFrom(img, scale, null);

        disabledIcon = new JBImageIcon(img);
      }
      myIcon2DisabledIcon.put(icon, disabledIcon);
    }
    return disabledIcon;
  }

  @Override
  public Icon getTransparentIcon(@Nonnull final Icon icon, final float alpha) {
    return new RetrievableIcon() {
      @Nullable
      @Override
      public Icon retrieveIcon() {
        return icon;
      }

      @Override
      public int getIconHeight() {
        return icon.getIconHeight();
      }

      @Override
      public int getIconWidth() {
        return icon.getIconWidth();
      }

      @Override
      public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        final Graphics2D g2 = (Graphics2D)g;
        final Composite saveComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        icon.paintIcon(c, g2, x, y);
        g2.setComposite(saveComposite);
      }
    };
  }

  @Override
  @Nonnull
  public Icon getIconSnapshot(@Nonnull Icon icon) {
    if (icon instanceof CachedImageIcon) {
      return ((CachedImageIcon)icon).getRealIcon();
    }
    return icon;
  }

  @Override
  public Icon createLazyIcon(final Computable<Icon> iconComputable) {
    return new LazyIcon() {
      @Override
      protected Icon compute() {
        return iconComputable.compute();
      }
    };
  }

  private static final class CachedImageIcon extends JBUI.AuxJBIcon implements ScalableIcon, SwingImageRef, consulo.ui.image.Image {
    private static final ImageFilter[] ourEmptyFilters = new ImageFilter[0];

    private volatile Object myRealIcon;
    private String myOriginalPath;
    private ClassLoader myClassLoader;
    @Nonnull
    private URL myUrl;
    private volatile boolean dark;

    private ImageFilter[] myFilters = ourEmptyFilters;
    private final MyScaledIconsCache myScaledIconsCache = new MyScaledIconsCache();

    private CachedImageIcon(@Nonnull CachedImageIcon icon) {
      myRealIcon = null; // to be computed
      myOriginalPath = icon.myOriginalPath;
      myClassLoader = icon.myClassLoader;
      myUrl = icon.myUrl;
      dark = icon.dark;
      myFilters = icon.myFilters;
    }

    public CachedImageIcon(@Nonnull URL url) {
      myUrl = url;
      dark = USE_DARK_ICONS;
    }

    @Nonnull
    private synchronized ImageIcon getRealIcon() {
      return getRealIcon(null);
    }

    @Nonnull
    private synchronized ImageIcon getRealIcon(@Nullable Graphics g) {
      if (!isValid() || needUpdateJBUIScale((Graphics2D)g)) {
        if (isLoaderDisabled()) return EMPTY_ICON;
        myRealIcon = null;
        dark = USE_DARK_ICONS;
        updateJBUIScale((Graphics2D)g);
        if (!isValid()) myScaledIconsCache.clear();
      }
      Object realIcon = myRealIcon;
      if (realIcon instanceof Icon) return (ImageIcon)realIcon;

      ImageIcon icon;
      if (realIcon instanceof Reference) {
        icon = ((Reference<ImageIcon>)realIcon).get();
        if (icon != null) return icon;
      }

      icon = myScaledIconsCache.getOrLoadIcon(getJBUIScale(JBUI.ScaleType.PIX));

      if (icon != null) {
        if (icon.getIconWidth() < 50 && icon.getIconHeight() < 50) {
          realIcon = icon;
        }
        else {
          realIcon = new SoftReference<ImageIcon>(icon);
        }
        myRealIcon = realIcon;
      }

      return icon == null ? EMPTY_ICON : icon;
    }

    private boolean isValid() {
      return myRealIcon != null && dark == USE_DARK_ICONS;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      getRealIcon(g).paintIcon(c, g, x, y);
    }

    @Override
    public int getHeight() {
      return getRealIcon().getIconHeight();
    }

    @Override
    public int getWidth() {
      return getRealIcon().getIconWidth();
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
    public Icon scale(float scale) {
      if (scale == 1f) return this;

      getRealIcon(); // force state update & cache reset

      Icon icon = myScaledIconsCache.getOrScaleIcon(getJBUIScale(JBUI.ScaleType.PIX), scale, UIUtil.isUnderDarkTheme());
      if (icon != null) {
        return icon;
      }
      return this;
    }

    private Icon asDisabledIcon() {
      CachedImageIcon icon = new CachedImageIcon(this);
      icon.myFilters = new ImageFilter[]{UIUtil.getGrayFilter()};
      return icon;
    }

    private class MyScaledIconsCache {
      // Map {false -> image}, {true -> image@2x}
      private Map<Boolean, SoftReference<Image>> origImagesCache = Collections.synchronizedMap(new HashMap<Boolean, SoftReference<Image>>(2));

      private static final int SCALED_ICONS_CACHE_LIMIT = 5;

      // Map {pixel scale -> icon}
      private Map<Float, SoftReference<ImageIcon>> scaledIconsCache = Collections.synchronizedMap(new LinkedHashMap<Float, SoftReference<ImageIcon>>(SCALED_ICONS_CACHE_LIMIT) {
        @Override
        public boolean removeEldestEntry(Map.Entry<Float, SoftReference<ImageIcon>> entry) {
          return size() > SCALED_ICONS_CACHE_LIMIT;
        }
      });

      private Image getOrLoadOrigImage(float pixScale) {
        return getOrLoadOrigImage(pixScale, true);
      }

      /**
       * Retrieves the orig image based on the pixScale.
       */
      private Image getOrLoadOrigImage(float pixScale, boolean allowFloatScaling) {
        boolean needRetinaImage = (pixScale > 1.0f);
        Image image = SoftReference.dereference(origImagesCache.get(needRetinaImage));
        if (image != null) return image;

        image = ImageLoader.loadFromUrl(myUrl, allowFloatScaling, myFilters, pixScale);
        if (image == null) return null;
        origImagesCache.put(needRetinaImage, new SoftReference<Image>(image));
        return image;
      }

      /**
       * Retrieves the orig icon based on the pixScale, then scale it by the instanceScale.
       */
      public ImageIcon getOrScaleIcon(float pixScale, float instanceScale, boolean allowFloatScaling) {
        float effectiveScale = pixScale * instanceScale;
        ImageIcon icon = SoftReference.dereference(scaledIconsCache.get(effectiveScale));
        if (icon != null) {
          return icon;
        }

        Image image = getOrLoadOrigImage(pixScale, allowFloatScaling);
        if (image == null) return null;

        image = ImageUtil.scaleImage(image, instanceScale);
        icon = checkIcon(image, myUrl);
        scaledIconsCache.put(effectiveScale, new SoftReference<ImageIcon>(icon));
        return icon;
      }

      /**
       * Retrieves the orig icon based on the pixScale.
       */
      public ImageIcon getOrLoadIcon(float pixScale) {
        return getOrScaleIcon(pixScale, 1f, true);
      }

      public void clear() {
        scaledIconsCache.clear();
        origImagesCache.clear();
      }
    }
  }

  public abstract static class LazyIcon extends JBUI.AuxJBIcon {
    private boolean myWasComputed;
    private Icon myIcon;
    private boolean isDarkVariant = USE_DARK_ICONS;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      final Icon icon = getOrComputeIcon((Graphics2D)g);
      if (icon != null) {
        icon.paintIcon(c, g, x, y);
      }
    }

    @Override
    public int getIconWidth() {
      final Icon icon = getOrComputeIcon();
      return icon != null ? icon.getIconWidth() : 0;
    }

    @Override
    public int getIconHeight() {
      final Icon icon = getOrComputeIcon();
      return icon != null ? icon.getIconHeight() : 0;
    }

    protected final synchronized Icon getOrComputeIcon() {
      return getOrComputeIcon(null);
    }

    protected final synchronized Icon getOrComputeIcon(@Nullable Graphics2D g) {
      if (!myWasComputed || isDarkVariant != USE_DARK_ICONS || needUpdateJBUIScale(g)) {
        isDarkVariant = USE_DARK_ICONS;
        updateJBUIScale(g);
        myWasComputed = true;
        myIcon = compute();
      }

      return myIcon;
    }

    public final void load() {
      getIconWidth();
    }

    protected abstract Icon compute();

    public Icon inOriginalScale() {
      Icon icon = getOrComputeIcon();
      if (icon != null) {
        if (icon instanceof CachedImageIcon) {
          Image img = ((CachedImageIcon)icon).myScaledIconsCache.getOrLoadOrigImage(1f);
          if (img != null) {
            icon = new ImageIcon(img);
          }
        }
      }
      return icon;
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
