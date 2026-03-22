/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.impl.internal;

import consulo.codeEditor.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.diff.DiffColors;
import consulo.diff.internal.DiffImplUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.paint.RectanglePainter2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.versionControlSystem.internal.VcsRange;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Static drawing utilities for VCS line-status gutter markers.
 * <p>
 * Equivalent to JetBrains' {@code LineStatusMarkerDrawUtil}.
 * Called by {@link LineStatusGutterMarkerRenderer} (document-level) and by
 * {@link LineStatusMarkerRenderer} (per-range popup renderer).
 */
public final class LineStatusMarkerDrawUtil {

    private LineStatusMarkerDrawUtil() {
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Paints a single VCS range marker in the gutter.
     * Handles DEFAULT mode (solid bar / deletion triangle) and SMART mode (inner ranges).
     *
     * <p>The hover state is per-range: only the marker whose line range contains the hovered
     * logical line gets the extra-width treatment (equivalent to JB's per-block isHovered logic
     * in {@code LineStatusMarkerDrawUtil.paintDefault}).
     */
    public static void paintRange(VcsRange range, Editor editor, Graphics g, Rectangle r) {
        ColorValue gutterColor = getGutterColor(range, editor);
        ColorValue borderColor = getGutterBorderColor(editor);

        EditorGutterComponentEx gutter = ((EditorEx) editor).getGutterComponentEx();
        // r.x == getLineMarkerFreePaintersAreaOffset() for RIGHT renderers; +1 px for brace highlighters.
        // r.x + r.width == right edge of the right free painters area.
        // Do NOT use getWhitespaceSeparatorOffset() — in Consulo that returns getWidth()-3
        // (the full gutter right edge), whereas the VCS marker area ends much earlier
        // (line numbers, icons and folding come after the free painters area in this layout).
        int x = r.x + JBUI.scale(1);
        int endX = r.x + r.width;
        int y = DiffImplUtil.lineToY(editor, range.getLine1());
        int endY = DiffImplUtil.lineToY(editor, range.getLine2());

        Graphics2D g2 = (Graphics2D) g;

        // Hover: expand LEFT by 3 px only if the mouse is over THIS range (not any range).
        boolean isHovered = isRangeHovered(range, gutter);
        int xPaint = isHovered ? x - JBUI.scale(3) : x;

        if (range.getInnerRanges() == null) {          // Mode.DEFAULT
            if (y != endY) {
                paintRect(g2, gutterColor, null, xPaint, y, endX, endY);
                if (borderColor != null) {
                    paintRect(g2, null, borderColor, xPaint, y, endX, endY);
                }
            }
            else {
                paintTriangle(g2, editor, gutterColor, borderColor, xPaint, endX, y);
            }
        }
        else if (y == endY) {
            paintTriangle(g2, editor, gutterColor, borderColor, xPaint, endX, y);
        }
        else {                                           // Mode.SMART – inner ranges
            List<VcsRange.InnerRange> innerRanges = range.getInnerRanges();

            for (VcsRange.InnerRange inner : innerRanges) {
                if (inner.getType() == VcsRange.DELETED) continue;
                int start = DiffImplUtil.lineToY(editor, inner.getLine1());
                int end = DiffImplUtil.lineToY(editor, inner.getLine2());
                paintRect(g2, getGutterColor(inner, editor), null, xPaint, start, endX, end);
            }

            paintRect(g2, null, borderColor, xPaint, y, endX, endY);

            for (VcsRange.InnerRange inner : innerRanges) {
                if (inner.getType() != VcsRange.DELETED) continue;
                int start = DiffImplUtil.lineToY(editor, inner.getLine1());
                paintTriangle(g2, editor, getGutterColor(inner, editor), borderColor, xPaint, endX, start);
            }
        }
    }

    /**
     * Returns true if the mouse is currently hovering over the gutter area that belongs to
     * {@code range}. Equivalent to JB's {@code isBlockUnderY} check.
     *
     * <p>For deleted ranges (line1==line2, rendered as a triangle) we extend the hover zone
     * by one extra line in each direction so the small triangle is easy to click.
     */
    private static boolean isRangeHovered(VcsRange range, EditorGutterComponentEx gutter) {
        int hoveredLine = gutter.getHoveredFreeMarkersLine();
        if (hoveredLine < 0) return false;

        int line1 = range.getLine1();
        int line2 = range.getLine2();
        if (line1 == line2) {
            // Deleted-lines triangle: allow hovering on the adjacent lines too.
            return hoveredLine >= line1 - 1 && hoveredLine <= line1;
        }
        return hoveredLine >= line1 && hoveredLine < line2;
    }

    /**
     * Returns the pixel rectangle inside the gutter where a range marker should be drawn.
     * The {@code r} rectangle is the area assigned by the gutter framework to the renderer
     * (for {@link consulo.codeEditor.markup.LineMarkerRenderer.Position#RIGHT} this starts
     * at {@link EditorGutterComponentEx#getLineMarkerFreePaintersAreaOffset()}).
     */
    public static Rectangle getMarkerArea(Editor editor, Rectangle r, int line1, int line2) {
        int x = r.x + JBUI.scale(1);  // 1 px gap for brace highlighters
        int endX = r.x + r.width;     // right edge of the right free painters area
        int y = DiffImplUtil.lineToY(editor, line1);
        int endY = DiffImplUtil.lineToY(editor, line2);
        return new Rectangle(x, y, endX - x, endY - y);
    }

    public static boolean isInsideMarkerArea(MouseEvent e) {
        EditorGutterComponentEx gutter = (EditorGutterComponentEx) e.getComponent();
        return gutter.isInsideMarkerArea(e);
    }

    // -------------------------------------------------------------------------
    // Color helpers
    // -------------------------------------------------------------------------

    public static @Nullable ColorValue getGutterColor(VcsRange range, @Nullable Editor editor) {
        EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case VcsRange.INSERTED -> scheme.getColor(EditorColors.ADDED_LINES_COLOR);
            case VcsRange.DELETED -> scheme.getColor(EditorColors.DELETED_LINES_COLOR);
            case VcsRange.MODIFIED -> scheme.getColor(EditorColors.MODIFIED_LINES_COLOR);
            default -> throw new AssertionError("Unknown range type: " + range.getType());
        };
    }

    public static @Nullable ColorValue getGutterColor(VcsRange.@Nullable InnerRange range, @Nullable Editor editor) {
        EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case VcsRange.INSERTED -> scheme.getColor(EditorColors.ADDED_LINES_COLOR);
            case VcsRange.DELETED -> scheme.getColor(EditorColors.DELETED_LINES_COLOR);
            case VcsRange.MODIFIED -> scheme.getColor(EditorColors.MODIFIED_LINES_COLOR);
            case VcsRange.EQUAL -> scheme.getColor(EditorColors.WHITESPACES_MODIFIED_LINES_COLOR);
            default -> throw new AssertionError("Unknown inner range type: " + range.getType());
        };
    }

    public static @Nullable ColorValue getErrorStripeColor(VcsRange range, @Nullable Editor editor) {
        EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case VcsRange.INSERTED -> scheme.getAttributes(DiffColors.DIFF_INSERTED).getErrorStripeColor();
            case VcsRange.DELETED -> scheme.getAttributes(DiffColors.DIFF_DELETED).getErrorStripeColor();
            case VcsRange.MODIFIED -> scheme.getAttributes(DiffColors.DIFF_MODIFIED).getErrorStripeColor();
            default -> throw new AssertionError("Unknown range type: " + range.getType());
        };
    }

    public static @Nullable ColorValue getGutterBorderColor(@Nullable Editor editor) {
        return getColorScheme(editor).getColor(EditorColors.BORDER_LINES_COLOR);
    }

    public static EditorColorsScheme getColorScheme(@Nullable Editor editor) {
        return editor != null ? editor.getColorsScheme() : EditorColorsManager.getInstance().getGlobalScheme();
    }

    // -------------------------------------------------------------------------
    // Low-level shape painters
    // -------------------------------------------------------------------------

    public static void paintRect(Graphics2D g, @Nullable ColorValue color, @Nullable ColorValue borderColor,
                                  int x1, int y1, int x2, int y2) {
        if (color != null) {
            g.setColor(TargetAWT.to(color));
            double w = x2 - x1;
            RectanglePainter2D.FILL.paint(g, x1, y1 + 1, w, y2 - y1 - 2, w);
        }
        else if (borderColor != null) {
            g.setColor(TargetAWT.to(borderColor));
            double w = x2 - x1;
            RectanglePainter2D.DRAW.paint(g, x1, y1 + 1, w, y2 - y1 - 2, w);
        }
    }

    public static void paintTriangle(Graphics2D g, Editor editor,
                                      @Nullable ColorValue color, @Nullable ColorValue borderColor,
                                      int x1, int x2, int y) {
        int size = (int) JBUIScale.scale(4 * getEditorScale(editor));
        if (y < size) y = size;

        if (color != null) {
            g.setColor(TargetAWT.to(color));
            double w = x2 - x1;
            RectanglePainter2D.FILL.paint(g, x1, y - size + 1, w, 2 * size - 2, w);
        }
        else if (borderColor != null) {
            g.setColor(TargetAWT.to(borderColor));
            double w = x2 - x1;
            RectanglePainter2D.DRAW.paint(g, x1, y - size + 1, w, 2 * size - 2, w);
        }
    }

    private static float getEditorScale(Editor editor) {
        return editor instanceof RealEditor ? ((RealEditor) editor).getScale() : 1.0f;
    }
}
