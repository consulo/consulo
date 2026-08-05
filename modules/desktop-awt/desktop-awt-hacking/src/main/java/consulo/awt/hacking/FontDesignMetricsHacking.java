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
package consulo.awt.hacking;

import consulo.logging.Logger;
import sun.font.FontDesignMetrics;

import org.jspecify.annotations.Nullable;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2019-11-21
 */
public class FontDesignMetricsHacking {
  private static final Logger LOG = Logger.getInstance(FontDesignMetricsHacking.class);

  private static boolean ourUnavailableReported;

  public static FontMetrics getMetrics(Font font, FontRenderContext fontRenderContext) {
    try {
      return FontDesignMetrics.getMetrics(font, fontRenderContext);
    }
    catch (Throwable e) {
      reportUnavailable(e);

      return fallbackMetrics(font, fontRenderContext);
    }
  }

  /**
   * Metrics for a runtime which did not open {@code sun.font}. The web frontend is one - it measures on a server
   * nobody looks at, the browser lays the text out itself - so a launcher without the flag should measure a little
   * differently rather than fail, which is what an editor asking for the width of an inlay used to get.
   */
  private static FontMetrics fallbackMetrics(Font font, FontRenderContext fontRenderContext) {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, fontRenderContext.getAntiAliasingHint());
      graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fontRenderContext.getFractionalMetricsHint());
      graphics.setTransform(fontRenderContext.getTransform());

      return graphics.getFontMetrics(font);
    }
    finally {
      graphics.dispose();
    }
  }

  public static boolean isFontDesignMetrics(FontMetrics fontMetrics) {
    try {
      return fontMetrics instanceof FontDesignMetrics;
    }
    catch (Throwable e) {
      reportUnavailable(e);

      return false;
    }
  }

  private static synchronized void reportUnavailable(Throwable e) {
    if (!ourUnavailableReported) {
      ourUnavailableReported = true;

      LOG.warn("Couldn't access FontDesignMetrics, falling back to the metrics of an offscreen image", e);
    }
  }

  public static @Nullable BiFunction<FontMetrics, Integer, Float> handleCharWidth() {
    try {
      Method method = FontDesignMetrics.class.getDeclaredMethod("handleCharWidth", int.class);
      method.setAccessible(true);

      return (fontMetrics, codePoint) -> {
        try {
          return (Float)method.invoke(fontMetrics, codePoint);
        }
        catch (Throwable e) {
          LOG.warn(e);
          return 0f;
        }
      };
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
    return null;
  }

  public static @Nullable BiFunction<FontMetrics, Character, Float> getLatinCharWidth() {
    try {
      Method method = FontDesignMetrics.class.getDeclaredMethod("getLatinCharWidth", char.class);
      method.setAccessible(true);

      return (fontMetrics, codePoint) -> {
        try {
          return (Float)method.invoke(fontMetrics, codePoint);
        }
        catch (Throwable e) {
          LOG.warn(e);
          return 0f;
        }
      };
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
    return null;
  }
}
