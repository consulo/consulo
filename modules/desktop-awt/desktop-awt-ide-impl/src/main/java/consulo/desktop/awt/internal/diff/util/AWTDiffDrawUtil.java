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
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.internal.diff.util;

import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.diff.DiffColors;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.util.ColorValueUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;
import java.util.Arrays;

public class AWTDiffDrawUtil {
    private static final double CTRL_PROXIMITY_X = 0.3;

    private static final EditorColorKey BACKGROUND = DiffColors.DIFF_SEPARATORS_BACKGROUND;
    private static final EditorColorKey TOP_BORDER = DiffColors.DIFF_SEPARATORS_TOP_BORDER;
    private static final EditorColorKey BOTTOM_BORDER = DiffColors.DIFF_SEPARATORS_BOTTOM_BORDER;

    private AWTDiffDrawUtil() {
    }

    public static void drawConnectorLineSeparator(Graphics2D g, int x1, int x2, int start1, int end1, int start2, int end2) {
        drawConnectorLineSeparator(g, x1, x2, start1, end1, start2, end2, null);
    }

    public static void drawConnectorLineSeparator(
        Graphics2D g,
        int x1,
        int x2,
        int start1,
        int end1,
        int start2,
        int end2,
        @Nullable EditorColorsScheme scheme
    ) {
        drawConnectorLine(g, x1, x2, start1, start2, end1 - start1, scheme);
    }

    public static void drawChunkBorderLine(
        Graphics2D g,
        int x1,
        int x2,
        int y,
        ColorValue colorValue,
        boolean doubleLine,
        boolean dottedLine
    ) {
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

    public static void drawTrapezium(
        Graphics2D g,
        int x1,
        int x2,
        int start1,
        int end1,
        int start2,
        int end2,
        @Nullable Color fillColor,
        @Nullable Color borderColor
    ) {
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

    public static void drawCurveTrapezium(
        Graphics2D g,
        int x1,
        int x2,
        int start1,
        int end1,
        int start2,
        int end2,
        @Nullable Color fillColor,
        @Nullable Color borderColor
    ) {
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
            return new CubicCurve2D.Double(
                x1,
                y1,
                x1 + width * CTRL_PROXIMITY_X,
                y1,
                x1 + width * (1.0 - CTRL_PROXIMITY_X),
                y2,
                x1 + width,
                y2
            );
        }
        else {
            return new CubicCurve2D.Double(
                x1 + width,
                y2,
                x1 + width * (1.0 - CTRL_PROXIMITY_X),
                y2,
                x1 + width * CTRL_PROXIMITY_X,
                y1,
                x1,
                y1
            );
        }
    }

    public static void drawConnectorLine(
        Graphics2D g,
        int x1, int x2,
        int start1, int end1,
        int start2, int end2
    ) {
        drawConnectorLine(g, x1, x2, start1, start2, end1 - end2, null);
    }

    /*
     * Divider
     */
    public static void drawConnectorLine(
        Graphics2D g,
        int x1, int x2,
        int y1, int y2,
        int lineHeight,
        @Nullable EditorColorsScheme scheme
    ) {
        int step = getStepSize(lineHeight);
        int height = getHeight(lineHeight);
        int dx = getDeltaX(lineHeight);
        int dy = getDeltaY(lineHeight);

        int start1 = y1 + (lineHeight - height - step) / 2 + step / 2;
        int start2 = y2 + (lineHeight - height - step) / 2 + step / 2;
        int end1 = start1 + height - 1;
        int end2 = start2 + height - 1;

        int[] xPoints;
        int[] yPoints;

        if (Math.abs(x2 - x1) < Math.abs(y2 - y1)) {
            if (y2 < y1) {
                xPoints = new int[]{x1, x2 - dx, x2, x2, x1 + dx, x1};
                yPoints = new int[]{start1, start2 + dy, start2, end2, end1 - dy, end1};
            }
            else {
                xPoints = new int[]{x1, x1 + dx, x2, x2, x2 - dx, x1};
                yPoints = new int[]{start1, start1 + dy, start2, end2, end2 - dy, end1};
            }
        }
        else {
            xPoints = new int[]{x1, x2, x2, x1};
            yPoints = new int[]{start1, start2, end2, end1};
        }

        paintConnectorLine(g, xPoints, yPoints, lineHeight, scheme);
    }

    private static void paintConnectorLine(
        Graphics g,
        int[] xPoints, int[] yPoints,
        int lineHeight,
        @Nullable EditorColorsScheme scheme
    ) {
        // TODO: shadow looks bad with big lineHeight and slope
        int height = getHeight(lineHeight);
        if (scheme == null) {
            scheme = EditorColorsManager.getInstance().getGlobalScheme();
        }

        Graphics2D gg = ((Graphics2D) g);
        AffineTransform oldTransform = gg.getTransform();

        // background
        gg.setColor(TargetAWT.to(getBackgroundColor(scheme)));
        gg.fillPolygon(xPoints, yPoints, xPoints.length);

        if (scheme.getColor(TOP_BORDER) != null) {
            for (int i = 0; i < height; i++) {
                ColorValue color = getTopBorderColor(i, lineHeight, scheme);
                if (color == null) {
                    break;
                }

                gg.setColor(TargetAWT.to(color));
                gg.drawPolyline(xPoints, yPoints, xPoints.length / 2);
                gg.translate(0, 1);
            }
            gg.setTransform(oldTransform);
        }

        if (scheme.getColor(BOTTOM_BORDER) != null) {
            int[] xBottomPoints = Arrays.copyOfRange(xPoints, xPoints.length / 2, xPoints.length);
            int[] yBottomPoints = Arrays.copyOfRange(yPoints, xPoints.length / 2, xPoints.length);

            for (int i = height - 1; i >= 0; i--) {
                ColorValue color = getBottomBorderColor(i, lineHeight, scheme);
                if (color == null) {
                    break;
                }

                gg.setColor(TargetAWT.to(color));
                gg.drawPolyline(xBottomPoints, yBottomPoints, xPoints.length / 2);
                gg.translate(0, -1);
            }
            gg.setTransform(oldTransform);
        }
    }

    private static int getStepSize(int lineHeight) {
        return Math.max(lineHeight / 3, 1);
    }

    private static int getHeight(int lineHeight) {
        return Math.max(lineHeight / 2, 1);
    }

    private static int getDeltaX(int lineHeight) {
        return Math.max(lineHeight / 4, 1);
    }

    private static int getDeltaY(int lineHeight) {
        return Math.max(lineHeight / 6, 1);
    }

    private static ColorValue getBackgroundColor(EditorColorsScheme scheme) {
        ColorValue color = scheme.getColor(BACKGROUND);
        return color != null ? color : new RGBColor(128, 128, 128);
    }

    private static @Nullable ColorValue getTopBorderColor(int i, int lineHeight, EditorColorsScheme scheme) {
        int border = Math.max(lineHeight / 4, 1);
        double ratio = (double) i / border;
        if (ratio > 1) {
            return null;
        }

        ColorValue top = scheme.getColor(TOP_BORDER);
        if (top == null) {
            return null;
        }

        ColorValue background = getBackgroundColor(scheme);
        return ColorValueUtil.mix(top, background, ratio);
    }

    private static @Nullable ColorValue getBottomBorderColor(int i, int lineHeight, EditorColorsScheme scheme) {
        int height = getHeight(lineHeight);
        int border = Math.max(lineHeight / 12, 1);

        int index = (height - i - 1);
        double ratio = (double) index / border;
        if (ratio > 1) {
            return null;
        }

        ColorValue bottom = scheme.getColor(BOTTOM_BORDER);
        if (bottom == null) {
            return null;
        }

        ColorValue background = getBackgroundColor(scheme);
        return ColorValueUtil.mix(bottom, background, ratio);
    }
}
