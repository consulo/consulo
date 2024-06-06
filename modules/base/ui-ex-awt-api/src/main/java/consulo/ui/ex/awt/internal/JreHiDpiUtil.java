// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.internal;

import consulo.awt.hacking.GraphicsEnvironmentHacking;
import consulo.platform.Platform;
import consulo.ui.ex.awt.JBUIScale;
import consulo.util.lang.SystemProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

public final class JreHiDpiUtil {
  private static final AtomicReference<Boolean> jreHiDPI = new AtomicReference<>();
  private static volatile boolean jreHiDPI_earlierVersion;

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled and the graphics configuration represents a HiDPI device.
   * (analogue of {@link UIUtil#isRetina(Graphics2D)} on macOS)
   */
  public static boolean isJreHiDPI(@Nullable GraphicsConfiguration gc) {
    return isJreHiDPIEnabled() && JBUIScale.isHiDPI(JBUIScale.sysScale(gc));
  }

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled and the graphics represents a HiDPI device.
   * (analogue of {@link UIUtil#isRetina(Graphics2D)} on macOS)
   */
  public static boolean isJreHiDPI(@Nullable Graphics2D g) {
    return isJreHiDPIEnabled() && JBUIScale.isHiDPI(JBUIScale.sysScale(g));
  }

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled.
   * (True for macOS JDK >= 7.10 versions)
   *
   * @see ScaleType
   */
  public static boolean isJreHiDPIEnabled() {
    Boolean value = jreHiDPI.get();
    if (value == null) {
      synchronized (jreHiDPI) {
        value = jreHiDPI.get();
        if (value == null) {
          value = false;
          if (SystemProperties.getBooleanProperty("hidpi", true)) {
            jreHiDPI_earlierVersion = true;
            // fixme [vistall] always allow hidpi on other jdks
            if (Boolean.TRUE) {
              try {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                Boolean uiScaleEnabled = GraphicsEnvironmentHacking.isUIScaleEnabled(ge);
                if (uiScaleEnabled != null) {
                  value = uiScaleEnabled;
                  jreHiDPI_earlierVersion = false;
                }
              }
              catch (Throwable ignore) {
              }
            }
            if (Platform.current().os().isMac()) {
              value = true;
            }
          }
          jreHiDPI.set(value);
        }
      }
    }
    return value;
  }

  /**
   * Indicates earlier JBSDK version, not containing HiDPI changes.
   * On macOS such JBSDK supports jreHiDPI, but it's not capable to provide device scale
   * via GraphicsDevice transform matrix (the scale should be retrieved via DetectRetinaKit).
   */
  //@ApiStatus.Internal
  public static boolean isJreHiDPI_earlierVersion() {
    isJreHiDPIEnabled();
    return jreHiDPI_earlierVersion;
  }

  @TestOnly
  @Nonnull
  public static AtomicReference<Boolean> test_jreHiDPI() {
    isJreHiDPIEnabled(); // force init
    return jreHiDPI;
  }
}
