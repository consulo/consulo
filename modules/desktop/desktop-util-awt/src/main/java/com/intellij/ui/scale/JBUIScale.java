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
package com.intellij.ui.scale;

import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * part of JBUI
 */
public class JBUIScale {
  private static AtomicNotNullLazyValue<Pair<String, Integer>> ourSystemFontValue = AtomicNotNullLazyValue.createValue(JBUIScale::calcSystemFontData);
  private static final float DISCRETE_SCALE_RESOLUTION = 0.25f;

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

    if (SystemInfo.isLinux) {
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
        float scale = UIUtil.isJreHiDPIEnabled() ? 1f : discreteScale(dpi / 96f); // no scaling in JRE-HiDPI mode
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
    else if (SystemInfo.isWindows) {
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

  @Nonnull
  public static Pair<String, Integer> getSystemFontData() {
    return ourSystemFontValue.getValue();
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

  public static float sysScale(Graphics2D g) {
    return JBUI.sysScale(g);
  }

  public static float sysScale(Component comp) {
    return JBUI.sysScale(comp);
  }

  public static int scale(int pixel) {
    return JBUI.scale(pixel);
  }

  public static float scale(float pixel) {
    return JBUI.scale(pixel);
  }

  /**
   * Returns the system scale factor, corresponding to the graphics configuration.
   * In the IDE-managed HiDPI mode defaults to {@link #sysScale()}
   */
  public static float sysScale(@Nullable GraphicsConfiguration gc) {
    if (UIUtil.isJreHiDPIEnabled() && gc != null) {
      if (gc.getDevice().getType() != GraphicsDevice.TYPE_PRINTER) {
        if (SystemInfo.isMac && UIUtil.isJreHiDPI_earlierVersion()) {
          return UIUtil.DetectRetinaKit.isOracleMacRetinaDevice(gc.getDevice()) ? 2f : 1f;
        }
        return (float)gc.getDefaultTransform().getScaleX();
      }
    }
    return JBUI.sysScale();
  }
}
