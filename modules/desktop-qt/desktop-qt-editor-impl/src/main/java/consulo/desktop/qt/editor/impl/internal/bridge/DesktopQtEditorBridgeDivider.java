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
package consulo.desktop.qt.editor.impl.internal.bridge;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.internal.bridge.EditorBridgeConnector;
import consulo.codeEditor.internal.bridge.EditorBridgeFoldConnector;
import consulo.colorScheme.EditorColorsScheme;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorImpl;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorWidget;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.diff.DiffColors;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.util.ColorValueUtil;
import io.qt.core.QPoint;
import io.qt.core.QPointF;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPen;
import io.qt.widgets.QSplitter;
import io.qt.widgets.QSplitterHandle;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * The strip between two bridged editors, drawing their connectors and fold connectors.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class DesktopQtEditorBridgeDivider extends QSplitterHandle {
    private static final double CTRL_PROXIMITY_X = 0.3;

    private final DesktopQtEditorBridge myBridge;

    public DesktopQtEditorBridgeDivider(Qt.Orientation orientation, QSplitter parent, DesktopQtEditorBridge bridge) {
        super(orientation, parent);
        myBridge = bridge;
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            paint(painter);
        }
        finally {
            painter.end();
        }
    }

    private void paint(QPainter painter) {
        List<Editor> editors = myBridge.getEditors();
        int gap = splitter().indexOf(this) - 1;
        if (gap < 0 || gap + 1 >= editors.size()) {
            return;
        }
        Editor editor1 = editors.get(gap);
        Editor editor2 = editors.get(gap + 1);

        EditorColorsScheme scheme = editor1.getColorsScheme();
        ColorValue background = scheme.getColor(EditorColors.GUTTER_BACKGROUND);
        if (background == null) {
            background = EditorColors.GUTTER_BACKGROUND.getDefaultColorValue();
        }
        painter.fillRect(rect(), TargetQt.to(background));

        int width = width();

        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
        for (EditorBridgeConnector connector : myBridge.getConnectors(gap)) {
            int start1 = lineY(editor1, connector.startLine1());
            int end1 = lineY(editor1, connector.endLine1());
            int start2 = lineY(editor2, connector.startLine2());
            int end2 = lineY(editor2, connector.endLine2());
            if (Math.max(end1, end2) < 0 && Math.max(start1, start2) < 0 || Math.min(start1, start2) > height()) {
                continue;
            }
            paintPolygon(painter, width, start1, end1, start2, end2, connector.color(), connector.resolved(), myBridge.isCurved());
        }
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, false);

        for (EditorBridgeFoldConnector connector : myBridge.getFoldConnectors(gap)) {
            int start1 = lineY(editor1, connector.line1());
            int start2 = lineY(editor2, connector.line2());
            paintConnectorLine(painter, 0, width, start1, start2, editor1.getLineHeight(), scheme);
        }
    }

    private int lineY(Editor editor, int line) {
        if (!(editor instanceof DesktopQtEditorImpl qtEditor)) {
            return 0;
        }

        DesktopQtEditorWidget surface = qtEditor.getSurface();
        if (surface == null) {
            return 0;
        }

        int y;
        if (line >= editor.getDocument().getLineCount()) {
            y = qtEditor.getRowLayout().getContentHeight();
        }
        else {
            y = qtEditor.getRowLayout().visualLineToY(qtEditor.getVisualLines().logicalToVisualLine(Math.max(0, line)));
        }
        y -= surface.verticalScrollBar().value();

        QPoint global = surface.viewport().mapToGlobal(new QPoint(0, y));
        return mapFromGlobal(global).y();
    }

    private static void paintPolygon(
        QPainter painter,
        int width,
        int start1,
        int end1,
        int start2,
        int end2,
        ColorValue color,
        boolean resolved,
        boolean curved
    ) {
        int startY1 = start1 - 1;
        int endY1 = end1 - 1;
        int startY2 = start2 - 1;
        int endY2 = end2 - 1;

        if (endY1 - startY1 < 2) {
            endY1 = startY1 + 1;
        }
        if (endY2 - startY2 < 2) {
            endY2 = startY2 + 1;
        }

        QColor qColor = TargetQt.to(color);
        QColor fillColor = resolved ? null : qColor;
        QColor borderColor = resolved ? qColor : null;

        if (curved) {
            drawCurveTrapezium(painter, 0, width, startY1, endY1, startY2, endY2, fillColor, borderColor);
        }
        else {
            drawTrapezium(painter, 0, width, startY1, endY1, startY2, endY2, fillColor, borderColor);
        }
    }

    private static void drawTrapezium(
        QPainter painter,
        int x1,
        int x2,
        int start1,
        int end1,
        int start2,
        int end2,
        @Nullable QColor fillColor,
        @Nullable QColor borderColor
    ) {
        if (fillColor != null) {
            QPainterPath path = new QPainterPath();
            path.moveTo(x1, start1);
            path.lineTo(x2, start2);
            path.lineTo(x2, end2 + 1);
            path.lineTo(x1, end1 + 1);
            path.closeSubpath();
            painter.fillPath(path, new QBrush(fillColor));
        }

        if (borderColor != null) {
            painter.setPen(borderPen(borderColor));
            painter.drawLine(new QPointF(x1, start1), new QPointF(x2, start2));
            painter.drawLine(new QPointF(x1, end1), new QPointF(x2, end2));
        }
    }

    private static void drawCurveTrapezium(
        QPainter painter,
        int x1,
        int x2,
        int start1,
        int end1,
        int start2,
        int end2,
        @Nullable QColor fillColor,
        @Nullable QColor borderColor
    ) {
        double ctrl1 = x1 + (x2 - x1) * CTRL_PROXIMITY_X;
        double ctrl2 = x1 + (x2 - x1) * (1.0 - CTRL_PROXIMITY_X);

        if (fillColor != null) {
            QPainterPath path = new QPainterPath();
            path.moveTo(x1, start1);
            path.cubicTo(ctrl1, start1, ctrl2, start2, x2, start2);
            path.lineTo(x2, end2 + 1);
            path.cubicTo(ctrl2, end2 + 1, ctrl1, end1 + 1, x1, end1 + 1);
            path.closeSubpath();
            painter.fillPath(path, new QBrush(fillColor));
        }

        if (borderColor != null) {
            painter.setPen(borderPen(borderColor));
            painter.setBrush(Qt.BrushStyle.NoBrush);

            QPainterPath upper = new QPainterPath();
            upper.moveTo(x1, start1);
            upper.cubicTo(ctrl1, start1, ctrl2, start2, x2, start2);
            painter.drawPath(upper);

            QPainterPath lower = new QPainterPath();
            lower.moveTo(x2, end2);
            lower.cubicTo(ctrl2, end2, ctrl1, end1, x1, end1);
            painter.drawPath(lower);
        }
    }

    private static QPen borderPen(QColor color) {
        QPen pen = new QPen(color);
        pen.setWidthF(2.3);
        pen.setStyle(Qt.PenStyle.CustomDashLine);
        pen.setDashPattern(List.of(1.0, 1.0));
        return pen;
    }

    private static void paintConnectorLine(QPainter painter, int x1, int x2, int y1, int y2, int lineHeight, EditorColorsScheme scheme) {
        int step = Math.max(lineHeight / 3, 1);
        int height = getHeight(lineHeight);
        int dx = Math.max(lineHeight / 4, 1);
        int dy = Math.max(lineHeight / 6, 1);

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

        ColorValue background = getBackgroundColor(scheme);

        QPainterPath polygon = new QPainterPath();
        polygon.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < xPoints.length; i++) {
            polygon.lineTo(xPoints[i], yPoints[i]);
        }
        polygon.closeSubpath();
        painter.fillPath(polygon, new QBrush(TargetQt.to(background)));

        int half = xPoints.length / 2;

        if (scheme.getColor(DiffColors.DIFF_SEPARATORS_TOP_BORDER) != null) {
            for (int i = 0; i < height; i++) {
                ColorValue color = getTopBorderColor(i, lineHeight, scheme);
                if (color == null) {
                    break;
                }
                painter.setPen(TargetQt.to(color));
                drawPolyline(painter, xPoints, yPoints, 0, half, i);
            }
        }

        if (scheme.getColor(DiffColors.DIFF_SEPARATORS_BOTTOM_BORDER) != null) {
            int[] xBottomPoints = Arrays.copyOfRange(xPoints, half, xPoints.length);
            int[] yBottomPoints = Arrays.copyOfRange(yPoints, half, yPoints.length);
            for (int i = height - 1; i >= 0; i--) {
                ColorValue color = getBottomBorderColor(i, lineHeight, scheme);
                if (color == null) {
                    break;
                }
                painter.setPen(TargetQt.to(color));
                drawPolyline(painter, xBottomPoints, yBottomPoints, 0, half, -(height - 1 - i));
            }
        }
    }

    private static void drawPolyline(QPainter painter, int[] xPoints, int[] yPoints, int from, int count, int shiftY) {
        QPoint[] points = new QPoint[count];
        for (int i = 0; i < count; i++) {
            points[i] = new QPoint(xPoints[from + i], yPoints[from + i] + shiftY);
        }
        painter.drawPolyline(points);
    }

    private static int getHeight(int lineHeight) {
        return Math.max(lineHeight / 2, 1);
    }

    private static ColorValue getBackgroundColor(EditorColorsScheme scheme) {
        ColorValue color = scheme.getColor(DiffColors.DIFF_SEPARATORS_BACKGROUND);
        return color != null ? color : new RGBColor(128, 128, 128);
    }

    private static @Nullable ColorValue getTopBorderColor(int i, int lineHeight, EditorColorsScheme scheme) {
        int border = Math.max(lineHeight / 4, 1);
        double ratio = (double) i / border;
        if (ratio > 1) {
            return null;
        }

        ColorValue top = scheme.getColor(DiffColors.DIFF_SEPARATORS_TOP_BORDER);
        if (top == null) {
            return null;
        }
        return ColorValueUtil.mix(top, getBackgroundColor(scheme), ratio);
    }

    private static @Nullable ColorValue getBottomBorderColor(int i, int lineHeight, EditorColorsScheme scheme) {
        int height = getHeight(lineHeight);
        int border = Math.max(lineHeight / 12, 1);

        int index = height - i - 1;
        double ratio = (double) index / border;
        if (ratio > 1) {
            return null;
        }

        ColorValue bottom = scheme.getColor(DiffColors.DIFF_SEPARATORS_BOTTOM_BORDER);
        if (bottom == null) {
            return null;
        }
        return ColorValueUtil.mix(bottom, getBackgroundColor(scheme), ratio);
    }
}
