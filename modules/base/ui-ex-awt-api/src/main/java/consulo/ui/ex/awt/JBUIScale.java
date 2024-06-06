/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.ex.awt;

import consulo.application.util.SystemInfo;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.ex.awt.internal.DetectRetinaKit;
import consulo.ui.ex.awt.internal.JreHiDpiUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * part of JBUI
 */
public class JBUIScale {
  private static Supplier<Pair<String, Integer>> ourSystemFontValue = LazyValue.atomicNotNull(JBUIScale::calcSystemFontData);
  private static final float DISCRETE_SCALE_RESOLUTION = 0.25f;
  private static final PropertyChangeSupport PCS = new PropertyChangeSupport(new JBUIScale());
  public static final String USER_SCALE_FACTOR_PROPERTY = "JBUIScale.userScaleFactor";

  @Nonnull
  private static Pair<String, Integer> calcSystemFontData() {
    // with JB Linux JDK the label font comes properly scaled based on Xft.dpi settings.
    Font font = UIManager.getFont("Label.font");
    if (SystemInfo.isMacOSElCapitan) {
      // text family should be used for relatively small sizes (<20pt), don't change to Display
      // see more about SF https://medium.com/@mach/the-secret-of-san-francisco-fonts-4b5295d9a745#.2ndr50z2v
      font = new Font(".SF NS Text", font.getStyle(), font.getSize());
    }

    Logger log = getLogger();
    boolean isScaleVerbose = Boolean.getBoolean("ide.ui.scale.verbose");
    if (isScaleVerbose) {
      log.info(String.format("Label font: %s, %d", font.getFontName(), font.getSize()));
    }

    if (Platform.current().os().isLinux()) {
      Object value = Toolkit.getDefaultToolkit().getDesktopProperty("gnome.Xft/DPI");
      if (isScaleVerbose) {
        log.info(String.format("gnome.Xft/DPI: %s", value));
      }
      if (value instanceof Integer) { // defined by JB JDK when the resource is available in the system
        // If the property is defined, then:
        // 1) it provides correct system scale
        // 2) the label font size is scaled
        int dpi = ((Integer)value).intValue() / 1024;
        if (dpi < 50) dpi = 50;
        float scale = JreHiDpiUtil.isJreHiDPIEnabled() ? 1f : discreteScale(dpi / 96f); // no scaling in JRE-HiDPI mode
        UIUtil.DEF_SYSTEM_FONT_SIZE = font.getSize() / scale; // derive actual system base font size
        if (isScaleVerbose) {
          log.info(String.format("DEF_SYSTEM_FONT_SIZE: %.2f", UIUtil.DEF_SYSTEM_FONT_SIZE));
        }
      }
      else if (!SystemInfo.isJetBrainsJvm) {
        // With Oracle JDK: derive scale from X server DPI, do not change DEF_SYSTEM_FONT_SIZE
        float size = UIUtil.DEF_SYSTEM_FONT_SIZE * getScreenScale();
        font = font.deriveFont(size);
        if (isScaleVerbose) {
          log.info(String.format("(Not-JB JRE) reset font size: %.2f", size));
        }
      }
    }
    else if (Platform.current().os().isWindows()) {
      //noinspection HardCodedStringLiteral
      @SuppressWarnings("SpellCheckingInspection") Font winFont = (Font)Toolkit.getDefaultToolkit().getDesktopProperty("win.messagebox.font");
      if (winFont != null) {
        font = winFont; // comes scaled
        if (isScaleVerbose) {
          log.info(String.format("Windows sys font: %s, %d", winFont.getFontName(), winFont.getSize()));
        }
      }
    }
    Pair<String, Integer> result = Pair.create(font.getName(), font.getSize());
    if (isScaleVerbose) {
      log.info(String.format("ourSystemFontData: %s, %d", result.first, result.second));
    }
    return result;
  }

  /**
   * The system scale factor, corresponding to the default monitor device.
   */
  private static final Supplier<Float> SYSTEM_SCALE_FACTOR = LazyValue.notNull(() -> {
    float scale = getSystemScaleFactor();
    log().info("System scale factor: " + scale + " (" + (JreHiDpiUtil.isJreHiDPIEnabled() ? "JRE" : "IDE") + "-managed HiDPI)");
    return scale;
  });

  private static float getSystemScaleFactor() {
    if (!SystemProperties.getBooleanProperty("hidpi", true)) {
      return 1f;
    }
    if (JreHiDpiUtil.isJreHiDPIEnabled()) {
      GraphicsDevice gd = null;
      try {
        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      }
      catch (HeadlessException ignore) {
      }
      if (gd != null && gd.getDefaultConfiguration() != null) {
        return sysScale(gd.getDefaultConfiguration());
      }
      return 1f;
    }
    Pair<String, Integer> fdata = JBUIScale.getSystemFontData();

    int size = fdata.getSecond();
    return getFontScale(size);
  }

  /**
   * For internal usage.
   */
  public static final Supplier<Float> DEBUG_USER_SCALE_FACTOR = LazyValue.nullable(() -> {
    Float factor = getDebugUserScaleFactor();
    if (factor != null) setUserScaleFactor(ObjectUtil.notNull(factor));
    return factor;
  });

  private static Float getDebugUserScaleFactor() {
    String prop = System.getProperty("ide.ui.scale");
    if (prop != null) {
      try {
        return Float.parseFloat(prop);
      }
      catch (NumberFormatException e) {
        log().error("ide.ui.scale system property is not a float value: " + prop);
      }
    }
    else if (Boolean.valueOf("ide.ui.scale.override")) {
      return 1f;
    }
    return null;
  }

  private static class UserScaleFactor {
    private static Float ourValue;

    static float get() {
      if (ourValue == null) {
        ourValue = getUserScaleFactor();
      }

      return ourValue;
    }

    static void set(float value) {
      ourValue = value;
    }
  }
  /**
   * The user scale factor, see {@link ScaleType#USR_SCALE}.
   */
  private static float getUserScaleFactor() {
    Float factor = DEBUG_USER_SCALE_FACTOR.get();
    if (factor != null) {
      return factor;
    }
    return computeUserScaleFactor(JreHiDpiUtil.isJreHiDPIEnabled() ? 1f : SYSTEM_SCALE_FACTOR.get());
  }

  private static float computeUserScaleFactor(float scale) {
    if (!SystemProperties.getBooleanProperty("hidpi", true)) {
      return 1f;
    }

    scale = discreteScale(scale);

    // Downgrading user scale below 1.0 may be uncomfortable (tiny icons),
    // whereas some users prefer font size slightly below normal which is ok.
    if (scale < 1 && sysScale() >= 1) {
      scale = 1;
    }

    // Ignore the correction when UIUtil.DEF_SYSTEM_FONT_SIZE is overridden, see UIUtil.initSystemFontData.
    if (Platform.current().os().isLinux() && scale == 1.25f && UIUtil.DEF_SYSTEM_FONT_SIZE == 12) {
      // Default UI font size for Unity and Gnome is 15. Scaling factor 1.25f works badly on Linux
      scale = 1f;
    }
    return scale;
  }

  @Nonnull
  public static Pair<String, Integer> getSystemFontData() {
    return ourSystemFontValue.get();
  }

  private static float getScreenScale() {
    int dpi = 96;
    try {
      dpi = Toolkit.getDefaultToolkit().getScreenResolution();
    }
    catch (HeadlessException ignored) {
    }
    return discreteScale(dpi / 96f);
  }

  public static float discreteScale(float scale) {
    return Math.round(scale / DISCRETE_SCALE_RESOLUTION) * DISCRETE_SCALE_RESOLUTION;
  }

  @Nonnull
  public static Logger getLogger() {
    return Logger.getInstance(JBUIScale.class);
  }

  public static float sysScale(Component comp) {
    if (comp != null) {
      return sysScale(comp.getGraphicsConfiguration());
    }
    return sysScale();
  }

  public static int scale(int pixels) {
    return Math.round(UserScaleFactor.get() * pixels);
  }

  public static float scale(float pixels) {
    return pixels * UserScaleFactor.get();
  }

  /**
   * Sets the user scale factor.
   * The method is used by the IDE, it's not recommended to call the method directly from the client code.
   * For debugging purposes, the following JVM system property can be used:
   * ide.ui.scale=[float]
   * or the IDE registry keys (for backward compatibility):
   * ide.ui.scale.override=[boolean]
   * ide.ui.scale=[float]
   *
   * @return the result
   */
  public static float setUserScaleFactor(float scale) {
    Float debugUserScale = DEBUG_USER_SCALE_FACTOR.get();
    if (debugUserScale != null) {
      float debugScale = ObjectUtil.notNull(debugUserScale);
      if (scale == debugScale) {
        setUserScaleFactorProperty(debugScale); // set the debug value as is, or otherwise ignore
      }
      return debugScale;
    }

    if (!SystemProperties.getBooleanProperty("hidpi", true)) {
      setUserScaleFactorProperty(1f);
      return 1f;
    }

    scale = discreteScale(scale);

    // Downgrading user scale below 1.0 may be uncomfortable (tiny icons),
    // whereas some users prefer font size slightly below normal which is ok.
    if (scale < 1 && sysScale() >= 1) scale = 1;

    // Ignore the correction when UIUtil.DEF_SYSTEM_FONT_SIZE is overridden, see UIUtil.initSystemFontData.
    if (Platform.current().os().isLinux() && scale == 1.25f && UIUtil.DEF_SYSTEM_FONT_SIZE == 12) {
      //Default UI font size for Unity and Gnome is 15. Scaling factor 1.25f works badly on Linux
      scale = 1f;
    }
    setUserScaleFactorProperty(scale);
    return scale;
  }

  private static void setUserScaleFactorProperty(float scale) {
    if (UserScaleFactor.get() == scale) return;
    Float oldValue = UserScaleFactor.get();
    UserScaleFactor.set(scale);
    PCS.firePropertyChange(USER_SCALE_FACTOR_PROPERTY, oldValue, scale);
    log().info("User scale factor: " + UserScaleFactor.get());
  }

  /**
   * Returns the system scale factor, corresponding to the default monitor device.
   */
  public static float sysScale() {
    return SYSTEM_SCALE_FACTOR.get();
  }

  /**
   * Returns whether the provided scale assumes HiDPI-awareness.
   */
  public static boolean isHiDPI(double scale) {
    // Scale below 1.0 is impractical, it's rather accepted for debug purpose.
    // Treat it as "hidpi" to correctly manage images which have different user and real size
    // (for scale below 1.0 the real size will be smaller).
    return scale != 1f;
  }

  /**
   * Returns whether the {@link ScaleType#USR_SCALE} scale factor assumes HiDPI-awareness.
   * An equivalent of {@code isHiDPI(scale(1f))}
   */
  public static boolean isUsrHiDPI() {
    return isHiDPI(scale(1f));
  }

  /**
   * Returns the system scale factor, corresponding to the graphics.
   * For BufferedImage's graphics, the scale is taken from the graphics itself.
   * In the IDE-managed HiDPI mode defaults to {@link #sysScale()}
   */
  public static float sysScale(@Nullable Graphics2D g) {
    if (JreHiDpiUtil.isJreHiDPIEnabled() && g != null) {
      GraphicsConfiguration gc = g.getDeviceConfiguration();
      if (gc == null || gc.getDevice().getType() == GraphicsDevice.TYPE_IMAGE_BUFFER || gc.getDevice().getType() == GraphicsDevice.TYPE_PRINTER) {
        // in this case gc doesn't provide a valid scale
        return Math.abs((float)g.getTransform().getScaleX());
      }
      return sysScale(gc);
    }
    return sysScale();
  }

  @Nonnull
  public static Font scale(@Nonnull Font font) {
    return font.deriveFont((float)scaleFontSize(font.getSize()));
  }

  public static int scaleFontSize(float fontSize) {
    if (UserScaleFactor.get() == 1.25f) return (int)(fontSize * 1.34f);
    if (UserScaleFactor.get() == 1.75f) return (int)(fontSize * 1.67f);
    return (int)scale(fontSize);
  }

  /**
   * Returns the system scale factor, corresponding to the graphics configuration.
   * In the IDE-managed HiDPI mode defaults to {@link #sysScale()}
   */
  public static float sysScale(@Nullable GraphicsConfiguration gc) {
    if (JreHiDpiUtil.isJreHiDPIEnabled() && gc != null) {
      if (gc.getDevice().getType() != GraphicsDevice.TYPE_PRINTER) {
        if (Platform.current().os().isMac() && JreHiDpiUtil.isJreHiDPI_earlierVersion()) {
          return DetectRetinaKit.isOracleMacRetinaDevice(gc.getDevice()) ? 2f : 1f;
        }
        return (float)gc.getDefaultTransform().getScaleX();
      }
    }
    return sysScale();
  }

  /**
   * Adds property change listener. Supported properties:
   * {@link #USER_SCALE_FACTOR_PROPERTY}
   */
  public static void addPropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener) {
    PCS.addPropertyChangeListener(propertyName, listener);
  }

  /**
   * Removes property change listener
   */
  public static void removePropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener) {
    PCS.removePropertyChangeListener(propertyName, listener);
  }

  /**
   * @return the scale factor of {@code fontSize} relative to the standard font size (currently 12pt)
   */
  public static float getFontScale(float fontSize) {
    return fontSize / UIUtil.DEF_SYSTEM_FONT_SIZE;
  }

  private static Logger log() {
    return Logger.getInstance(JBUIScale.class);
  }
}
