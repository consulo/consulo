/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.ex;

import com.intellij.openapi.diff.DiffColors;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.util.Function;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.intellij.diff.util.DiffDrawUtil.lineToY;

public abstract class LineStatusMarkerRenderer implements ActiveGutterRenderer {
  @Nonnull
  protected final Range myRange;

  public LineStatusMarkerRenderer(@Nonnull Range range) {
    myRange = range;
  }

  @Nonnull
  public static RangeHighlighter createRangeHighlighter(@Nonnull Range range,
                                                        @Nonnull TextRange textRange,
                                                        @Nonnull MarkupModel markupModel) {
    TextAttributes attributes = getTextAttributes(range);

    final RangeHighlighter highlighter = markupModel.addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(),
                                                                         HighlighterLayer.FIRST - 1, attributes,
                                                                         HighlighterTargetArea.LINES_IN_RANGE);

    highlighter.setThinErrorStripeMark(true);
    highlighter.setGreedyToLeft(true);
    highlighter.setGreedyToRight(true);

    highlighter.setErrorStripeTooltip(getTooltipText(range));

    return highlighter;
  }

  @Nonnull
  public static LineMarkerRenderer createRenderer(@Nonnull Range range,
                                                  @Nullable Function<Editor, LineStatusMarkerPopup> popupBuilder) {
    return new LineStatusMarkerRenderer(range) {
      @Override
      public boolean canDoAction(MouseEvent e) {
        return popupBuilder != null && isInsideMarkerArea(e);
      }

      @Override
      public void doAction(Editor editor, MouseEvent e) {
        LineStatusMarkerPopup popup = popupBuilder != null ? popupBuilder.fun(editor) : null;
        if (popup != null) popup.showHint(e);
      }
    };
  }

  @Nonnull
  public static LineMarkerRenderer createRenderer(int line1, int line2, @Nonnull ColorValue color, @Nullable String tooltip,
                                                  @Nullable PairConsumer<Editor, MouseEvent> action) {
    return new ActiveGutterRenderer() {
      @Override
      public void paint(Editor editor, Graphics g, Rectangle r) {
        Rectangle area = getMarkerArea(editor, r, line1, line2);
        ColorValue borderColor = getGutterBorderColor(editor);
        if (area.height != 0) {
          paintRect(g, color, borderColor, area.x, area.y, area.x + area.width, area.y + area.height);
        }
        else {
          paintTriangle(g, color, borderColor, area.x, area.x + area.width, area.y);
        }
      }

      @Nullable
      @Override
      public String getTooltipText() {
        return tooltip;
      }

      @Override
      public boolean canDoAction(MouseEvent e) {
        return isInsideMarkerArea(e);
      }

      @Override
      public void doAction(Editor editor, MouseEvent e) {
        if (action != null) action.consume(editor, e);
      }
    };
  }

  @Nonnull
  private static TextAttributes getTextAttributes(@Nonnull final Range range) {
    return new TextAttributes() {
      @Override
      public ColorValue getErrorStripeColor() {
        return LineStatusMarkerRenderer.getErrorStripeColor(range, null);
      }
    };
  }

  @Nonnull
  private static String getTooltipText(@Nonnull Range range) {
    if (range.getLine1() == range.getLine2()) {
      if (range.getVcsLine1() + 1 == range.getVcsLine2()) {
        return VcsBundle.message("tooltip.text.line.before.deleted", range.getLine1() + 1);
      }
      else {
        return VcsBundle.message("tooltip.text.lines.before.deleted", range.getLine1() + 1, range.getVcsLine2() - range.getVcsLine1());
      }
    }
    else if (range.getLine1() + 1 == range.getLine2()) {
      return VcsBundle.message("tooltip.text.line.changed", range.getLine1() + 1);
    }
    else {
      return VcsBundle.message("tooltip.text.lines.changed", range.getLine1() + 1, range.getLine2());
    }
  }

  //
  // Gutter painting
  //

  @Override
  public void paint(Editor editor, Graphics g, Rectangle r) {
    ColorValue gutterColor = getGutterColor(myRange, editor);
    ColorValue borderColor = getGutterBorderColor(editor);

    Rectangle area = getMarkerArea(editor, r, myRange.getLine1(), myRange.getLine2());
    final int x = area.x;
    final int endX = area.x + area.width;
    final int y = area.y;
    final int endY = area.y + area.height;

    if (myRange.getInnerRanges() == null) { // Mode.DEFAULT
      if (y != endY) {
        paintRect(g, gutterColor, borderColor, x, y, endX, endY);
      }
      else {
        paintTriangle(g, gutterColor, borderColor, x, endX, y);
      }
    }
    else { // Mode.SMART
      if (y == endY) {
        paintTriangle(g, gutterColor, borderColor, x, endX, y);
      }
      else {
        List<Range.InnerRange> innerRanges = myRange.getInnerRanges();
        for (Range.InnerRange innerRange : innerRanges) {
          if (innerRange.getType() == Range.DELETED) continue;

          int start = lineToY(editor, innerRange.getLine1());
          int end = lineToY(editor, innerRange.getLine2());

          paintRect(g, getGutterColor(innerRange, editor), null, x, start, endX, end);
        }

        paintRect(g, null, borderColor, x, y, endX, endY);

        for (Range.InnerRange innerRange : innerRanges) {
          if (innerRange.getType() != Range.DELETED) continue;

          int start = lineToY(editor, innerRange.getLine1());

          paintTriangle(g, getGutterColor(innerRange, editor), borderColor, x, endX, start);
        }
      }
    }
  }

  private static void paintRect(@Nonnull Graphics g, @Nullable ColorValue color, @Nullable ColorValue borderColor, int x1, int y1, int x2, int y2) {
    if (color != null) {
      g.setColor(TargetAWT.to(color));
      g.fillRect(x1, y1, x2 - x1, y2 - y1);
    }
    if (borderColor != null) {
      g.setColor(TargetAWT.to(borderColor));
      UIUtil.drawLine(g, x1, y1, x2 - JBUI.scale(1), y1);
      UIUtil.drawLine(g, x1, y1, x1, y2 - JBUI.scale(1));
      UIUtil.drawLine(g, x1, y2 - JBUI.scale(1), x2 - JBUI.scale(1), y2 - JBUI.scale(1));
    }
  }

  @Nonnull
  public static Rectangle getMarkerArea(@Nonnull Editor editor, @Nonnull Rectangle r, int line1, int line2) {
    EditorGutterComponentEx gutter = ((EditorEx)editor).getGutterComponentEx();
    int x = r.x + JBUI.scale(1); // leave 1px for brace highlighters
    int endX = gutter.getWhitespaceSeparatorOffset();
    int y = lineToY(editor, line1);
    int endY = lineToY(editor, line2);
    return new Rectangle(x, y, endX - x, endY - y);
  }

  public static boolean isInsideMarkerArea(@Nonnull MouseEvent e) {
    final EditorGutterComponentEx gutter = (EditorGutterComponentEx)e.getComponent();
    return e.getX() > gutter.getLineMarkerFreePaintersAreaOffset();
  }

  private static void paintTriangle(@Nonnull Graphics g, @Nullable ColorValue color, @Nullable ColorValue borderColor, int x1, int x2, int y) {
    int size = JBUI.scale(4);

    final int[] xPoints = new int[]{x1, x1, x2};
    final int[] yPoints = new int[]{y - size, y + size, y};

    if (color != null) {
      g.setColor(TargetAWT.to(color));
      g.fillPolygon(xPoints, yPoints, xPoints.length);
    }
    if (borderColor != null) {
      g.setColor(TargetAWT.to(borderColor));
      g.drawPolygon(xPoints, yPoints, xPoints.length);
    }
  }

  @Nullable
  private static ColorValue getGutterColor(@Nonnull Range.InnerRange range, @Nullable Editor editor) {
    final EditorColorsScheme scheme = getColorScheme(editor);
    switch (range.getType()) {
      case Range.INSERTED:
        return scheme.getColor(EditorColors.ADDED_LINES_COLOR);
      case Range.DELETED:
        return scheme.getColor(EditorColors.DELETED_LINES_COLOR);
      case Range.MODIFIED:
        return scheme.getColor(EditorColors.MODIFIED_LINES_COLOR);
      case Range.EQUAL:
        return scheme.getColor(EditorColors.WHITESPACES_MODIFIED_LINES_COLOR);
      default:
        assert false;
        return null;
    }
  }

  @Nullable
  private static ColorValue getErrorStripeColor(@Nonnull Range range, @Nullable Editor editor) {
    final EditorColorsScheme scheme = getColorScheme(editor);
    switch (range.getType()) {
      case Range.INSERTED:
        return scheme.getAttributes(DiffColors.DIFF_INSERTED).getErrorStripeColor();
      case Range.DELETED:
        return scheme.getAttributes(DiffColors.DIFF_DELETED).getErrorStripeColor();
      case Range.MODIFIED:
        return scheme.getAttributes(DiffColors.DIFF_MODIFIED).getErrorStripeColor();
      default:
        assert false;
        return null;
    }
  }

  @Nullable
  private static ColorValue getGutterColor(@Nonnull Range range, @Nullable Editor editor) {
    final EditorColorsScheme scheme = getColorScheme(editor);
    switch (range.getType()) {
      case Range.INSERTED:
        return scheme.getColor(EditorColors.ADDED_LINES_COLOR);
      case Range.DELETED:
        return scheme.getColor(EditorColors.DELETED_LINES_COLOR);
      case Range.MODIFIED:
        return scheme.getColor(EditorColors.MODIFIED_LINES_COLOR);
      default:
        assert false;
        return null;
    }
  }

  @Nullable
  private static ColorValue getGutterBorderColor(@Nullable Editor editor) {
    return getColorScheme(editor).getColor(EditorColors.BORDER_LINES_COLOR);
  }

  @Nonnull
  private static EditorColorsScheme getColorScheme(@Nullable Editor editor) {
    return editor != null ? editor.getColorsScheme() : EditorColorsManager.getInstance().getGlobalScheme();
  }

  //
  // Popup
  //

  @Override
  public boolean canDoAction(MouseEvent e) {
    return false;
  }

  @Override
  public void doAction(Editor editor, MouseEvent e) {
  }
}
