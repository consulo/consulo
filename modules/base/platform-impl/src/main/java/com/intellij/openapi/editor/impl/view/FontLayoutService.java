/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.editor.impl.view;

import consulo.awt.hacking.FontDesignMetricsHacking;
import consulo.logging.Logger;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.function.BiFunction;

/**
 * Encapsulates logic related to font metrics. Mock instance can be used in tests to make them independent on font properties on particular
 * platform.
 */
public abstract class FontLayoutService {
  private static final Logger LOG = Logger.getInstance(FontLayoutService.class);

  private static final FontLayoutService DEFAULT_INSTANCE = new DefaultFontLayoutService();
  private static FontLayoutService INSTANCE = DEFAULT_INSTANCE;

  public static FontLayoutService getInstance() {
    return INSTANCE;
  }

  @Nonnull
  public abstract GlyphVector layoutGlyphVector(@Nonnull Font font, @Nonnull FontRenderContext fontRenderContext, @Nonnull char[] chars, int start, int end, boolean isRtl);

  public abstract int charWidth(@Nonnull FontMetrics fontMetrics, char c);

  public abstract int charWidth(@Nonnull FontMetrics fontMetrics, int codePoint);

  public abstract float charWidth2D(@Nonnull FontMetrics fontMetrics, int codePoint);

  public abstract int stringWidth(@Nonnull FontMetrics fontMetrics, @Nonnull String str);

  public abstract int getHeight(@Nonnull FontMetrics fontMetrics);

  public abstract int getDescent(@Nonnull FontMetrics fontMetrics);

  @TestOnly
  public static void setInstance(@Nullable FontLayoutService fontLayoutService) {
    INSTANCE = fontLayoutService == null ? DEFAULT_INSTANCE : fontLayoutService;
  }

  private static class DefaultFontLayoutService extends FontLayoutService {
    // this flag is supported by JetBrains Runtime
    private static final int LAYOUT_NO_PAIRED_CHARS_AT_SCRIPT_SPLIT = 8;

    private final BiFunction<FontMetrics, Integer, Float> myHandleCharWidthMethod;
    private final BiFunction<FontMetrics, Character, Float> myGetLatinCharWidthMethod;

    private DefaultFontLayoutService() {
      myHandleCharWidthMethod = FontDesignMetricsHacking.handleCharWidth();
      if (myHandleCharWidthMethod == null) {
        LOG.warn("Couldn't access FontDesignMetrics.handleCharWidth method");
      }
      myGetLatinCharWidthMethod = FontDesignMetricsHacking.getLatinCharWidth();
      if (myGetLatinCharWidthMethod == null) {
        LOG.warn("Couldn't access FontDesignMetrics.getLatinCharWidth method");
      }
    }

    @Nonnull
    @Override
    public GlyphVector layoutGlyphVector(@Nonnull Font font, @Nonnull FontRenderContext fontRenderContext, @Nonnull char[] chars, int start, int end, boolean isRtl) {
      return font.layoutGlyphVector(fontRenderContext, chars, start, end, (isRtl ? Font.LAYOUT_RIGHT_TO_LEFT : Font.LAYOUT_LEFT_TO_RIGHT) | LAYOUT_NO_PAIRED_CHARS_AT_SCRIPT_SPLIT);
    }

    @Override
    public int charWidth(@Nonnull FontMetrics fontMetrics, char c) {
      return fontMetrics.charWidth(c);
    }

    @Override
    public int charWidth(@Nonnull FontMetrics fontMetrics, int codePoint) {
      return fontMetrics.charWidth(codePoint);
    }

    @Override
    public float charWidth2D(@Nonnull FontMetrics fontMetrics, int codePoint) {
      if (FontDesignMetricsHacking.isFontDesignMetrics(fontMetrics)) {
        if (codePoint < 256 && myGetLatinCharWidthMethod != null) {
          return myGetLatinCharWidthMethod.apply(fontMetrics, (char)codePoint);
        }
        if (myHandleCharWidthMethod != null) {
          return myHandleCharWidthMethod.apply(fontMetrics, codePoint);
        }
      }
      return charWidth(fontMetrics, codePoint);
    }

    @Override
    public int stringWidth(@Nonnull FontMetrics fontMetrics, @Nonnull String str) {
      return fontMetrics.stringWidth(str);
    }

    @Override
    public int getHeight(@Nonnull FontMetrics fontMetrics) {
      return fontMetrics.getHeight();
    }

    @Override
    public int getDescent(@Nonnull FontMetrics fontMetrics) {
      return fontMetrics.getDescent();
    }
  }
}
