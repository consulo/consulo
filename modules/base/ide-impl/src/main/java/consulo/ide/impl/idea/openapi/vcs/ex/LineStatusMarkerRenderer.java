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
package consulo.ide.impl.idea.openapi.vcs.ex;

import consulo.annotation.DeprecationInfo;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.diff.DiffColors;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.document.util.TextRange;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.paint.RectanglePainter2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.function.PairConsumer;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class LineStatusMarkerRenderer implements ActiveGutterRenderer {
    @Nonnull
    protected final Range myRange;

    public LineStatusMarkerRenderer(@Nonnull Range range) {
        myRange = range;
    }

    @Nonnull
    @Override
    public LocalizeValue getTooltipValue() {
        return LocalizeValue.empty();
    }

    @Nonnull
    public static RangeHighlighter createRangeHighlighter(
        @Nonnull Range range,
        @Nonnull TextRange textRange,
        @Nonnull MarkupModel markupModel
    ) {
        TextAttributes attributes = getTextAttributes(range);

        final RangeHighlighter highlighter = markupModel.addRangeHighlighter(
            textRange.getStartOffset(),
            textRange.getEndOffset(),
            HighlighterLayer.FIRST - 1,
            attributes,
            HighlighterTargetArea.LINES_IN_RANGE
        );

        highlighter.setThinErrorStripeMark(true);
        highlighter.setGreedyToLeft(true);
        highlighter.setGreedyToRight(true);

        highlighter.setErrorStripeTooltip(getTooltipText(range));

        return highlighter;
    }

    @Nonnull
    public static LineMarkerRenderer createRenderer(
        @Nonnull Range range,
        @Nullable Function<Editor, LineStatusMarkerPopup> popupBuilder
    ) {
        return new LineStatusMarkerRenderer(range) {
            @Override
            public boolean canDoAction(@Nonnull MouseEvent e) {
                return popupBuilder != null && isInsideMarkerArea(e);
            }

            @Override
            public void doAction(@Nonnull Editor editor, @Nonnull MouseEvent e) {
                LineStatusMarkerPopup popup = popupBuilder != null ? popupBuilder.apply(editor) : null;
                if (popup != null) {
                    popup.showHint(e);
                }
            }
        };
    }

    @Nonnull
    public static LineMarkerRenderer createRenderer(
        int line1,
        int line2,
        @Nonnull ColorValue color,
        @Nonnull LocalizeValue tooltip,
        @Nullable BiConsumer<Editor, MouseEvent> action
    ) {
        return new ActiveGutterRenderer() {
            @Override
            public void paint(Editor editor, Graphics g, Rectangle r) {
                Rectangle area = getMarkerArea(editor, r, line1, line2);
                ColorValue borderColor = getGutterBorderColor(editor);
                if (area.height != 0) {
                    paintRect((Graphics2D)g, color, borderColor, area.x, area.y, area.x + area.width, area.y + area.height);
                }
                else {
                    paintTriangle((Graphics2D)g, editor, color, borderColor, area.x, area.x + area.width, area.y);
                }
            }

            @Nonnull
            @Override
            public LocalizeValue getTooltipValue() {
                return tooltip;
            }

            @Override
            public boolean canDoAction(@Nonnull MouseEvent e) {
                return isInsideMarkerArea(e);
            }

            @Override
            public void doAction(@Nonnull Editor editor, @Nonnull MouseEvent e) {
                if (action != null) {
                    action.accept(editor, e);
                }
            }
        };
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Nonnull
    public static LineMarkerRenderer createRenderer(
        int line1,
        int line2,
        @Nonnull ColorValue color,
        @Nullable String tooltip,
        @Nullable PairConsumer<Editor, MouseEvent> action
    ) {
        return createRenderer(line1, line2, color, LocalizeValue.ofNullable(tooltip), action);
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
    private static LocalizeValue getTooltipText(@Nonnull Range range) {
        if (range.getLine1() == range.getLine2()) {
            return range.getVcsLine1() + 1 == range.getVcsLine2()
                ? VcsLocalize.tooltipTextLineBeforeDeleted(range.getLine1() + 1)
                : VcsLocalize.tooltipTextLinesBeforeDeleted(range.getLine1() + 1, range.getVcsLine2() - range.getVcsLine1());
        }
        else if (range.getLine1() + 1 == range.getLine2()) {
            return VcsLocalize.tooltipTextLineChanged(range.getLine1() + 1);
        }
        else {
            return VcsLocalize.tooltipTextLinesChanged(range.getLine1() + 1, range.getLine2());
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

        Graphics2D graphics2D = (Graphics2D)g;

        if (myRange.getInnerRanges() == null) { // Mode.DEFAULT
            if (y != endY) {
                EditorGutterComponentEx gutter = ((EditorEx)editor).getGutterComponentEx();
                int line = gutter.getHoveredFreeMarkersLine();

                if (line != -1) {
                    paintRect(graphics2D, gutterColor, borderColor, x - 1, y, endX + 2, endY);
                }
                else {
                    paintRect(graphics2D, gutterColor, borderColor, x, y, endX, endY);
                }
            }
            else {
                paintTriangle(graphics2D, editor, gutterColor, borderColor, x, endX, y);
            }
        }
        else if (y == endY) {
            paintTriangle(graphics2D, editor, gutterColor, borderColor, x, endX, y);
        }
        else { // Mode.SMART
            List<Range.InnerRange> innerRanges = myRange.getInnerRanges();
            for (Range.InnerRange innerRange : innerRanges) {
                if (innerRange.getType() == Range.DELETED) {
                    continue;
                }

                int start = DiffImplUtil.lineToY(editor, innerRange.getLine1());
                int end = DiffImplUtil.lineToY(editor, innerRange.getLine2());

                paintRect(graphics2D, getGutterColor(innerRange, editor), null, x, start, endX, end);
            }

            paintRect(graphics2D, null, borderColor, x, y, endX, endY);

            for (Range.InnerRange innerRange : innerRanges) {
                if (innerRange.getType() != Range.DELETED) {
                    continue;
                }

                int start = DiffImplUtil.lineToY(editor, innerRange.getLine1());

                paintTriangle(graphics2D, editor, getGutterColor(innerRange, editor), borderColor, x, endX, start);
            }
        }
    }

    private static void paintRect(
        @Nonnull Graphics2D g,
        @Nullable ColorValue color,
        @Nullable ColorValue borderColor,
        int x1,
        int y1,
        int x2,
        int y2
    ) {
        if (color != null) {
            g.setColor(TargetAWT.to(color));
            double width = x2 - x1;
            RectanglePainter2D.FILL.paint(g, x1, y1 + 1, width, y2 - y1 - 2, width);
        }
        else if (borderColor != null) {
            g.setColor(TargetAWT.to(borderColor));
            double width = x2 - x1;
            RectanglePainter2D.DRAW.paint(g, x1, y1 + 1, width, y2 - y1 - 2, width);
        }
    }

    @Nonnull
    public static Rectangle getMarkerArea(@Nonnull Editor editor, @Nonnull Rectangle r, int line1, int line2) {
        EditorGutterComponentEx gutter = ((EditorEx)editor).getGutterComponentEx();
        int x = r.x + JBUI.scale(1); // leave 1px for brace highlighters
        int endX = gutter.getWhitespaceSeparatorOffset();
        int y = DiffImplUtil.lineToY(editor, line1);
        int endY = DiffImplUtil.lineToY(editor, line2);
        // new ui
        if (Boolean.TRUE) {
            x = gutter.getLineMarkerFreePaintersAreaOffset() + 1; // leave 1px for brace highlighters
            x += 2; //IDEA-286352
            return new Rectangle(
                x,
                y,
                x - (int)(JBUIScale.scale(JBUI.getInt("Gutter.VcsChanges.width", 4) * getEditorScale(editor))),
                endY - y
            );
        }
        return new Rectangle(x, y, endX - x, endY - y);
    }

    public static boolean isInsideMarkerArea(@Nonnull MouseEvent e) {
        final EditorGutterComponentEx gutter = (EditorGutterComponentEx)e.getComponent();
        return gutter.isInsideMarkerArea(e);
    }

    private static void paintTriangle(
        @Nonnull Graphics2D g,
        Editor editor,
        @Nullable ColorValue color,
        @Nullable ColorValue borderColor,
        int x1,
        int x2,
        int y
    ) {
        int size = (int)JBUIScale.scale(4 * getEditorScale(editor));
        if (y < size) {
            y = size;
        }

        if (color != null) {
            g.setColor(TargetAWT.to(color));
            double width = x2 - x1;
            RectanglePainter2D.FILL.paint(g, x1, y - size + 1, width, 2 * size - 2, width);
        }
        else if (borderColor != null) {
            g.setColor(TargetAWT.to(borderColor));
            double width = x2 - x1;
            RectanglePainter2D.DRAW.paint(g, x1, y - size + 1, width, 2 * size - 2, width);
        }
    }

    private static float getEditorScale(@Nonnull Editor editor) {
        return editor instanceof RealEditor ? ((RealEditor)editor).getScale() : 1.0f;
    }

    @Nullable
    private static ColorValue getGutterColor(@Nonnull Range.InnerRange range, @Nullable Editor editor) {
        final EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case Range.INSERTED -> scheme.getColor(EditorColors.ADDED_LINES_COLOR);
            case Range.DELETED -> scheme.getColor(EditorColors.DELETED_LINES_COLOR);
            case Range.MODIFIED -> scheme.getColor(EditorColors.MODIFIED_LINES_COLOR);
            case Range.EQUAL -> scheme.getColor(EditorColors.WHITESPACES_MODIFIED_LINES_COLOR);
            default -> throw new AssertionError();
        };
    }

    @Nullable
    private static ColorValue getErrorStripeColor(@Nonnull Range range, @Nullable Editor editor) {
        final EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case Range.INSERTED -> scheme.getAttributes(DiffColors.DIFF_INSERTED).getErrorStripeColor();
            case Range.DELETED -> scheme.getAttributes(DiffColors.DIFF_DELETED).getErrorStripeColor();
            case Range.MODIFIED -> scheme.getAttributes(DiffColors.DIFF_MODIFIED).getErrorStripeColor();
            default -> throw new AssertionError();
        };
    }

    @Nullable
    private static ColorValue getGutterColor(@Nonnull Range range, @Nullable Editor editor) {
        final EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case Range.INSERTED -> scheme.getColor(EditorColors.ADDED_LINES_COLOR);
            case Range.DELETED -> scheme.getColor(EditorColors.DELETED_LINES_COLOR);
            case Range.MODIFIED -> scheme.getColor(EditorColors.MODIFIED_LINES_COLOR);
            default -> throw new AssertionError();
        };
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
    public boolean canDoAction(@Nonnull MouseEvent e) {
        return false;
    }

    @Override
    public void doAction(@Nonnull Editor editor, @Nonnull MouseEvent e) {
    }
}
