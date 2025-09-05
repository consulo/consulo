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
package consulo.ide.impl.diff;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.util.LineRange;
import consulo.diff.util.TextDiffType;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.codeEditor.markup.LineMarkerRendererEx;
import consulo.ide.impl.idea.openapi.util.BooleanGetter;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static consulo.diff.internal.DiffImplUtil.getLineCount;

public class DiffDrawUtil {
  public static final int STRIPE_LAYER = HighlighterLayer.ERROR - 1;
  public static final int DEFAULT_LAYER = HighlighterLayer.SELECTION - 3;
  public static final int INLINE_LAYER = HighlighterLayer.SELECTION - 2;
  public static final int LINE_MARKER_LAYER = HighlighterLayer.SELECTION - 1;

  private static final double CTRL_PROXIMITY_X = 0.3;

  public static final LineSeparatorRenderer BORDER_LINE_RENDERER = new LineSeparatorRenderer() {
    @Override
    public void drawLine(Graphics g, int x1, int x2, int y) {
      Rectangle clip = g.getClipBounds();
      x2 = clip.x + clip.width;
      g.setColor(JBColor.border());
      g.drawLine(x1, y, x2, y);
    }
  };

  private DiffDrawUtil() {
  }

  @Nonnull
  public static ColorValue getDividerColor() {
    return getDividerColor(null);
  }

  @Nonnull
  public static ColorValue getDividerColor(@Nullable Editor editor) {
    return getDividerColorFromScheme(editor != null ? editor.getColorsScheme() : EditorColorsManager.getInstance().getGlobalScheme());
  }

  @Nonnull
  public static ColorValue getDividerColorFromScheme(@Nonnull EditorColorsScheme scheme) {
    ColorValue gutterBackground = scheme.getColor(EditorColors.GUTTER_BACKGROUND);
    if (gutterBackground == null) {
      gutterBackground = EditorColors.GUTTER_BACKGROUND.getDefaultColorValue();
    }
    return gutterBackground;
  }

  public static void drawConnectorLineSeparator(@Nonnull Graphics2D g, int x1, int x2, int start1, int end1, int start2, int end2) {
    drawConnectorLineSeparator(g, x1, x2, start1, end1, start2, end2, null);
  }

  public static void drawConnectorLineSeparator(@Nonnull Graphics2D g,
                                                int x1,
                                                int x2,
                                                int start1,
                                                int end1,
                                                int start2,
                                                int end2,
                                                @Nullable EditorColorsScheme scheme) {
    DiffLineSeparatorRenderer.drawConnectorLine(g, x1, x2, start1, start2, end1 - start1, scheme);
  }

  public static void drawChunkBorderLine(@Nonnull Graphics2D g,
                                         int x1,
                                         int x2,
                                         int y,
                                         @Nonnull ColorValue colorValue,
                                         boolean doubleLine,
                                         boolean dottedLine) {
    Color color = TargetAWT.to(colorValue);

    if (dottedLine && doubleLine) {
      UIUtil.drawBoldDottedLine(g, x1, x2, y - 1, null, color, false);
      UIUtil.drawBoldDottedLine(g, x1, x2, y, null, color, false);
    }
    else if (dottedLine) {
      UIUtil.drawBoldDottedLine(g, x1, x2, y - 1, null, color, false);
    }
    else if (doubleLine) {
      UIUtil.drawLine(g, x1, y, x2, y, null, color);
      UIUtil.drawLine(g, x1, y + 1, x2, y + 1, null, color);
    }
    else {
      UIUtil.drawLine(g, x1, y, x2, y, null, color);
    }
  }

  public static void drawTrapezium(@Nonnull Graphics2D g,
                                   int x1,
                                   int x2,
                                   int start1,
                                   int end1,
                                   int start2,
                                   int end2,
                                   @Nullable Color fillColor,
                                   @Nullable Color borderColor) {
    if (fillColor != null) {
      int[] xPoints = new int[]{x1, x2, x2, x1};
      int[] yPoints = new int[]{start1, start2, end2 + 1, end1 + 1};

      g.setColor(fillColor);
      g.fillPolygon(xPoints, yPoints, xPoints.length);
    }

    if (borderColor != null) {
      g.setColor(borderColor);
      g.drawLine(x1, start1, x2, start2);
      g.drawLine(x1, end1, x2, end2);
    }
  }

  public static void drawCurveTrapezium(@Nonnull Graphics2D g,
                                        int x1,
                                        int x2,
                                        int start1,
                                        int end1,
                                        int start2,
                                        int end2,
                                        @Nullable Color fillColor,
                                        @Nullable Color borderColor) {
    Shape upperCurve = makeCurve(x1, x2, start1, start2, true);
    Shape lowerCurve = makeCurve(x1, x2, end1 + 1, end2 + 1, false);
    Shape lowerCurveBorder = makeCurve(x1, x2, end1, end2, false);

    if (fillColor != null) {
      Path2D path = new Path2D.Double();
      path.append(upperCurve, true);
      path.append(lowerCurve, true);

      g.setColor(fillColor);
      g.fill(path);
    }

    if (borderColor != null) {
      g.setColor(borderColor);
      g.draw(upperCurve);
      g.draw(lowerCurveBorder);
    }
  }

  private static Shape makeCurve(int x1, int x2, int y1, int y2, boolean forward) {
    int width = x2 - x1;
    if (forward) {
      return new CubicCurve2D.Double(x1, y1, x1 + width * CTRL_PROXIMITY_X, y1, x1 + width * (1.0 - CTRL_PROXIMITY_X), y2, x1 + width, y2);
    }
    else {
      return new CubicCurve2D.Double(x1 + width, y2, x1 + width * (1.0 - CTRL_PROXIMITY_X), y2, x1 + width * CTRL_PROXIMITY_X, y1, x1, y1);
    }
  }

  //
  // Impl
  //

  public static int lineToY(@Nonnull Editor editor, int line) {
    return DiffImplUtil.lineToY(editor, line);
  }

  @Nonnull
  public static TextAttributes getTextAttributes(@Nonnull final TextDiffType type, @Nullable final Editor editor, final boolean ignored) {
    return new TextAttributes() {
      @Override
      public ColorValue getBackgroundColor() {
        return ignored ? type.getIgnoredColor(editor) : type.getColor(editor);
      }
    };
  }

  @Nonnull
  private static TextAttributes getStripeTextAttributes(@Nonnull final TextDiffType type, @Nonnull final Editor editor) {
    return new TextAttributes() {
      @Override
      public ColorValue getErrorStripeColor() {
        return type.getMarkerColor(editor);
      }
    };
  }

  @Nonnull
  private static LineSeparatorRenderer createDiffLineRenderer(@Nonnull final Editor editor,
                                                              @Nonnull final TextDiffType type,
                                                              @Nonnull SeparatorPlacement placement,
                                                              final boolean doubleLine,
                                                              final boolean resolved) {
    return new LineSeparatorRenderer() {
      @Override
      public void drawLine(Graphics g, int x1, int x2, int y) {
        // TODO: change LineSeparatorRenderer interface ?
        Rectangle clip = g.getClipBounds();
        x2 = clip.x + clip.width;
        if (placement == SeparatorPlacement.TOP) y++;
        drawChunkBorderLine((Graphics2D)g, x1, x2, y, type.getColor(editor), doubleLine, resolved);
      }
    };
  }

  @Nonnull
  private static LineMarkerRenderer createFoldingGutterLineRenderer(@Nonnull final TextDiffType type,
                                                                    @Nonnull final SeparatorPlacement placement,
                                                                    final boolean doubleLine,
                                                                    final boolean resolved) {
    return new LineMarkerRendererEx() {
      @Override
      public void paint(Editor editor, Graphics g, Rectangle r) {
        EditorGutterComponentEx gutter = ((EditorEx)editor).getGutterComponentEx();
        Graphics2D g2 = (Graphics2D)g;

        int x1 = gutter.getWhitespaceSeparatorOffset();
        int x2 = gutter.getComponent().getWidth();

        int y = r.y;
        if (placement == SeparatorPlacement.BOTTOM) y += editor.getLineHeight();

        drawChunkBorderLine(g2, x1, x2, y - 1, type.getColor(editor), doubleLine, resolved);
      }

      @Nonnull
      @Override
      public LineMarkerRenderer.Position getPosition() {
        return LineMarkerRenderer.Position.CUSTOM;
      }
    };
  }

  //
  // Highlighters
  //

  // TODO: desync of range and 'border' line markers on typing

  @Nonnull
  public static List<RangeHighlighter> createUnifiedChunkHighlighters(@Nonnull Editor editor,
                                                                      @Nonnull LineRange deleted,
                                                                      @Nonnull LineRange inserted,
                                                                      @Nullable List<DiffFragment> innerFragments) {
    boolean ignored = innerFragments != null;

    List<RangeHighlighter> list = new ArrayList<>();
    if (!inserted.isEmpty() && !deleted.isEmpty()) {
      list.addAll(createHighlighter(editor, deleted.start, deleted.end, TextDiffType.DELETED, ignored));
      list.addAll(createHighlighter(editor, inserted.start, inserted.end, TextDiffType.INSERTED, ignored));
    }
    else if (!inserted.isEmpty()) {
      list.addAll(createHighlighter(editor, inserted.start, inserted.end, TextDiffType.INSERTED, ignored));
    }
    else if (!deleted.isEmpty()) {
      list.addAll(createHighlighter(editor, deleted.start, deleted.end, TextDiffType.DELETED, ignored));
    }

    if (innerFragments != null) {
      int deletedStartOffset = editor.getDocument().getLineStartOffset(deleted.start);
      int insertedStartOffset = editor.getDocument().getLineStartOffset(inserted.start);

      for (DiffFragment fragment : innerFragments) {
        int deletedWordStart = deletedStartOffset + fragment.getStartOffset1();
        int deletedWordEnd = deletedStartOffset + fragment.getEndOffset1();
        list.addAll(createInlineHighlighter(editor, deletedWordStart, deletedWordEnd, TextDiffType.DELETED));

        int insertedWordStart = insertedStartOffset + fragment.getStartOffset2();
        int insertedWordEnd = insertedStartOffset + fragment.getEndOffset2();
        list.addAll(createInlineHighlighter(editor, insertedWordStart, insertedWordEnd, TextDiffType.INSERTED));
      }
    }

    return list;
  }

  @Nonnull
  public static List<RangeHighlighter> createHighlighter(@Nonnull Editor editor,
                                                         int startLine,
                                                         int endLine,
                                                         @Nonnull TextDiffType type,
                                                         boolean ignored) {
    return new LineHighlighterBuilder(editor, startLine, endLine, type).withIgnored(ignored).done();
  }

  @Nonnull
  public static List<RangeHighlighter> createHighlighter(@Nonnull Editor editor,
                                                         int startLine,
                                                         int endLine,
                                                         @Nonnull TextDiffType type,
                                                         boolean ignored,
                                                         boolean resolved,
                                                         boolean hideWithoutLineNumbers) {
    return new LineHighlighterBuilder(editor, startLine, endLine, type).withIgnored(ignored)
                                                                       .withResolved(resolved)
                                                                       .withHideWithoutLineNumbers(hideWithoutLineNumbers)
                                                                       .done();
  }

  @Nonnull
  public static List<RangeHighlighter> createInlineHighlighter(@Nonnull Editor editor, int start, int end, @Nonnull TextDiffType type) {
    return new InlineHighlighterBuilder(editor, start, end, type).done();
  }

  @Nonnull
  public static List<RangeHighlighter> createLineMarker(@Nonnull Editor editor, int line, @Nonnull TextDiffType type) {
    if (line == 0) return Collections.emptyList();
    return createLineMarker(editor, line - 1, type, SeparatorPlacement.BOTTOM, false, false, true);
  }

  @Nonnull
  private static List<RangeHighlighter> createLineMarker(@Nonnull Editor editor,
                                                         int line,
                                                         @Nonnull TextDiffType type,
                                                         @Nonnull SeparatorPlacement placement,
                                                         boolean doubleLine,
                                                         boolean resolved,
                                                         boolean paintFoldingOutline) {
    LineMarkerBuilder builder =
      new LineMarkerBuilder(editor, line, placement).withType(type).withResolved(resolved).withDefaultRenderer(doubleLine);
    if (paintFoldingOutline) builder.withDefaultGutterRenderer(doubleLine);
    return builder.done();
  }

  @Nonnull
  public static List<RangeHighlighter> createBorderLineMarker(@Nonnull Editor editor,
                                                              int line,
                                                              @Nonnull SeparatorPlacement placement) {
    return new LineMarkerBuilder(editor, line, placement).withRenderer(BORDER_LINE_RENDERER).done();
  }

  @Nonnull
  public static List<RangeHighlighter> createLineSeparatorHighlighter(@Nonnull Editor editor,
                                                                      int offset1,
                                                                      int offset2,
                                                                      @Nonnull BooleanGetter condition) {
    RangeHighlighter marker =
      editor.getMarkupModel().addRangeHighlighter(offset1, offset2,
                                                  LINE_MARKER_LAYER, null, HighlighterTargetArea.LINES_IN_RANGE);

    DiffLineSeparatorRenderer renderer = new DiffLineSeparatorRenderer(editor, condition);
    marker.setLineSeparatorPlacement(SeparatorPlacement.TOP);
    marker.setLineSeparatorRenderer(renderer);
    marker.setLineMarkerRenderer(renderer);

    return Collections.singletonList(marker);
  }

  private static class LineHighlighterBuilder {
    @Nonnull
    private final Editor editor;
    @Nonnull
    private final TextDiffType type;
    private final int startLine;
    private final int endLine;

    private boolean ignored = false;
    private boolean resolved = false;
    private boolean hideWithoutLineNumbers = false;

    private LineHighlighterBuilder(@Nonnull Editor editor, int startLine, int endLine, @Nonnull TextDiffType type) {
      this.editor = editor;
      this.type = type;
      this.startLine = startLine;
      this.endLine = endLine;
    }

    @Nonnull
    public LineHighlighterBuilder withIgnored(boolean ignored) {
      this.ignored = ignored;
      return this;
    }

    @Nonnull
    public LineHighlighterBuilder withResolved(boolean resolved) {
      this.resolved = resolved;
      return this;
    }

    public LineHighlighterBuilder withHideWithoutLineNumbers(boolean hideWithoutLineNumbers) {
      this.hideWithoutLineNumbers = hideWithoutLineNumbers;
      return this;
    }

    @Nonnull
    public List<RangeHighlighter> done() {
      List<RangeHighlighter> highlighters = new ArrayList<>();

      boolean isEmptyRange = startLine == endLine;
      boolean isLastLine = endLine == getLineCount(editor.getDocument());

      TextRange offsets = DiffImplUtil.getLinesRange(editor.getDocument(), startLine, endLine);
      int start = offsets.getStartOffset();
      int end = offsets.getEndOffset();

      TextAttributes attributes = isEmptyRange || resolved ? null : getTextAttributes(type, editor, ignored);
      TextAttributes stripeAttributes = isEmptyRange || resolved ? null : getStripeTextAttributes(type, editor);

      RangeHighlighter highlighter =
        editor.getMarkupModel().addRangeHighlighter(start, end,
                                                    DEFAULT_LAYER, attributes, HighlighterTargetArea.LINES_IN_RANGE);
      highlighters.add(highlighter);

      highlighter.setLineMarkerRenderer(new DiffLineMarkerRenderer(highlighter,
                                                                   type,
                                                                   ignored,
                                                                   resolved,
                                                                   hideWithoutLineNumbers,
                                                                   isEmptyRange,
                                                                   isLastLine));

      if (isEmptyRange) {
        if (startLine == 0) {
          highlighters.addAll(createLineMarker(editor, 0, type, SeparatorPlacement.TOP, true, resolved, false));
        }
        else {
          highlighters.addAll(createLineMarker(editor, startLine - 1, type, SeparatorPlacement.BOTTOM, true, resolved, false));
        }
      }
      else if (resolved) {
        highlighters.addAll(createLineMarker(editor, startLine, type, SeparatorPlacement.TOP, false, resolved, false));
        highlighters.addAll(createLineMarker(editor, endLine - 1, type, SeparatorPlacement.BOTTOM, false, resolved, false));
      }

      if (stripeAttributes != null) {
        RangeHighlighter stripeHighlighter =
          editor.getMarkupModel().addRangeHighlighter(start, end,
                                                      STRIPE_LAYER, stripeAttributes, HighlighterTargetArea.LINES_IN_RANGE);
        highlighters.add(stripeHighlighter);
      }

      return highlighters;
    }
  }

  private static class LineMarkerBuilder {
    @Nonnull
    private final Editor editor;
    @Nonnull
    private final SeparatorPlacement placement;
    private final int line;

    private boolean resolved = false;
    @Nullable
    private TextDiffType type;
    @Nullable
    private LineSeparatorRenderer renderer;
    @Nullable
    private LineMarkerRenderer gutterRenderer;

    private LineMarkerBuilder(@Nonnull Editor editor, int line, @Nonnull SeparatorPlacement placement) {
      this.editor = editor;
      this.line = line;
      this.placement = placement;
    }

    @Nonnull
    public LineMarkerBuilder withType(@Nonnull TextDiffType type) {
      this.type = type;
      return this;
    }

    @Nonnull
    public LineMarkerBuilder withResolved(boolean resolved) {
      this.resolved = resolved;
      return this;
    }

    @Nonnull
    public LineMarkerBuilder withRenderer(@Nonnull LineSeparatorRenderer renderer) {
      this.renderer = renderer;
      return this;
    }

    @Nonnull
    public LineMarkerBuilder withDefaultRenderer(boolean doubleLine) {
      assert type != null;
      this.renderer = createDiffLineRenderer(editor, type, placement, doubleLine, resolved);
      return this;
    }

    @Nonnull
    public LineMarkerBuilder withDefaultGutterRenderer(boolean doubleLine) {
      assert type != null;
      this.gutterRenderer = createFoldingGutterLineRenderer(type, placement, doubleLine, resolved);
      return this;
    }

    @Nonnull
    public List<RangeHighlighter> done() {
      // We won't use addLineHighlighter as it will fail to add marker into empty document.
      //RangeHighlighter highlighter = editor.getMarkupModel().addLineHighlighter(line, HighlighterLayer.SELECTION - 1, null);

      int offset = DocumentUtil.getFirstNonSpaceCharOffset(editor.getDocument(), line);
      RangeHighlighter highlighter =
        editor.getMarkupModel().addRangeHighlighter(offset, offset,
                                                    LINE_MARKER_LAYER, null, HighlighterTargetArea.LINES_IN_RANGE);

      highlighter.setLineSeparatorPlacement(placement);
      highlighter.setLineSeparatorRenderer(renderer);
      highlighter.setLineMarkerRenderer(gutterRenderer);

      if (type == null || resolved) return Collections.singletonList(highlighter);

      TextAttributes stripeAttributes = getStripeTextAttributes(type, editor);
      RangeHighlighter stripeHighlighter =
        editor.getMarkupModel().addRangeHighlighter(offset, offset,
                                                    STRIPE_LAYER, stripeAttributes, HighlighterTargetArea.LINES_IN_RANGE);

      return ContainerUtil.list(highlighter, stripeHighlighter);
    }
  }
}
