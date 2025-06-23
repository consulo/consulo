/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.codeEditor.impl.softwrap;

import consulo.codeEditor.Editor;
import consulo.codeEditor.SoftWrapDrawingType;
import consulo.codeEditor.impl.ArrowPainter;
import consulo.codeEditor.impl.ColorProvider;
import consulo.codeEditor.impl.util.EditorImplUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.function.Supplier;

/**
 * {@link SoftWrapPainter} implementation that draws arrows in soft wrap location.
 * <p/>
 * Primary idea is to use dedicated unicode symbols as soft wrap drawings and this class is introduced only as a part
 * of defensive programming - there is unlikely case that local client environment doesn't have a font that is able to
 * represent target unicode symbol. We draw an arrow manually then (platform-independent approach).
 *
 * @author Denis Zhdanov
 * @since 2010-07-02
 */
public class ArrowSoftWrapPainter implements SoftWrapPainter {

  private final HeightProvider myHeightProvider = new HeightProvider();
  private final Editor myEditor;
  private final ArrowPainter myArrowPainter;
  private int myMinWidth = -1;

  public ArrowSoftWrapPainter(Editor editor) {
    myEditor = editor;
    myArrowPainter = new ArrowPainter(ColorProvider.byColor(myEditor.getColorsScheme().getDefaultForeground()), new WidthProvider(), myHeightProvider);
  }

  @Override
  public int paint(@Nonnull Graphics g, @Nonnull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    myHeightProvider.myHeight = lineHeight / 2;

    int start;
    int end;
    int result;
    switch (drawingType) {
      case BEFORE_SOFT_WRAP_LINE_FEED:
        start = x;
        end = myEditor.getScrollingModel().getVisibleArea().width;
        result = end - start;
        break;
      case AFTER_SOFT_WRAP:
        start = 0;
        end = x;
        result = 0;
        break;
      default: throw new IllegalStateException("Soft wrap arrow painting is not set up for drawing type " + drawingType);
    }
    myArrowPainter.paint(g, y + lineHeight - g.getFontMetrics().getDescent(), start, end);
    return result;
  }

  @Override
  public int getDrawingHorizontalOffset(@Nonnull Graphics g, @Nonnull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    switch (drawingType) {
      case BEFORE_SOFT_WRAP_LINE_FEED: return myEditor.getScrollingModel().getVisibleArea().width - x;
      case AFTER_SOFT_WRAP: return 0;
      default: throw new IllegalStateException("Soft wrap arrow painting is not set up for drawing type " + drawingType);
    }
  }

  @Override
  public int getMinDrawingWidth(@Nonnull SoftWrapDrawingType drawingType) {
    if (myMinWidth < 0) {
      // We need to reserve a minimal space required for representing arrow before soft wrap-introduced line feed.
      myMinWidth = EditorImplUtil.charWidth('a', Font.PLAIN, myEditor);
    }
    return myMinWidth;
  }

  @Override
  public boolean canUse() {
    return true;
  }

  @Override
  public void reinit() {
    myMinWidth = -1;
  }

  private static class HeightProvider implements Supplier<Integer> {

    public int myHeight;

    @Override
    public Integer get() {
      return myHeight;
    }
  }

  private class WidthProvider implements Supplier<Integer> {
    @Override
    public Integer get() {
      return EditorImplUtil.getSpaceWidth(Font.PLAIN, myEditor);
    }
  }
}
