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
package com.intellij.testFramework;

import com.intellij.openapi.editor.impl.view.FontLayoutService;
import javax.annotation.Nonnull;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.util.Arrays;

public class MockFontLayoutService extends FontLayoutService {
  private final int myCharWidth;
  private final int myLineHeight;
  private final int myDescent;

  public MockFontLayoutService(int charWidth, int lineHeight, int descent) {
    myCharWidth = charWidth;
    myLineHeight = lineHeight;
    myDescent = descent;
  }

  @Nonnull
  @Override
  public GlyphVector layoutGlyphVector(@Nonnull Font font, @Nonnull FontRenderContext fontRenderContext, @Nonnull char[] chars, int start, int end, boolean isRtl) {
    return new MockGlyphVector(Arrays.copyOfRange(chars, start, end), isRtl);
  }

  @Override
  public int charWidth(@Nonnull FontMetrics fontMetrics, char c) {
    return myCharWidth;
  }

  @Override
  public int charWidth(@Nonnull FontMetrics fontMetrics, int codePoint) {
    return myCharWidth;
  }

  @Override
  public float charWidth2D(@Nonnull FontMetrics fontMetrics, int codePoint) {
    return myCharWidth;
  }

  @Override
  public int stringWidth(@Nonnull FontMetrics fontMetrics, @Nonnull String str) {
    return myCharWidth * str.codePointCount(0, str.length());
  }

  @Override
  public int getHeight(@Nonnull FontMetrics fontMetrics) {
    return myLineHeight;
  }

  @Override
  public int getDescent(@Nonnull FontMetrics fontMetrics) {
    return myDescent;
  }

  private class MockGlyphVector extends AbstractMockGlyphVector {
    private final char[] myChars;
    private final boolean myIsRtl;

    private MockGlyphVector(char[] chars, boolean isRtl) {
      myChars = chars;
      myIsRtl = isRtl;
    }

    @Override
    public int getNumGlyphs() {
      return myChars.length;
    }

    @Override
    public int getGlyphCharIndex(int glyphIndex) {
      return myIsRtl ? myChars.length - 1 - glyphIndex : glyphIndex;
    }

    @Override
    public Point2D getGlyphPosition(int glyphIndex) {
      return new Point(glyphIndex * myCharWidth, 0);
    }

    @Override
    public Shape getGlyphLogicalBounds(int glyphIndex) {
      return new Rectangle(glyphIndex * myCharWidth, -myDescent, myCharWidth, myLineHeight);
    }

    @Override
    public int getGlyphCode(int glyphIndex) {
      return myChars[glyphIndex];
    }
  }
}
