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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2019-11-21
 */
public class FontDesignMetricsHacking {
  private static final Logger LOG = Logger.getInstance(FontDesignMetricsHacking.class);

  public static FontMetrics getMetrics(@Nonnull Font font, @Nonnull FontRenderContext fontRenderContext) {
    try {
      return FontDesignMetrics.getMetrics(font, fontRenderContext);
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean isFontDesignMetrics(FontMetrics fontMetrics) {
    return fontMetrics instanceof FontDesignMetrics;
  }

  @Nullable
  public static BiFunction<FontMetrics, Integer, Float> handleCharWidth() {
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

  @Nullable
  public static BiFunction<FontMetrics, Character, Float> getLatinCharWidth() {
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
