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
package consulo.desktop.qt.ui.impl.graph;

import consulo.desktop.qt.ui.impl.DesktopQtTextItemPresentation;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.Size2D;
import consulo.ui.color.ColorValue;
import consulo.ui.graph.GraphArrow;
import consulo.ui.image.Image;
import consulo.ui.impl.graph.GraphEdgeStyle;
import consulo.ui.impl.graph.GraphNodeContent;
import io.qt.core.QEvent;
import io.qt.core.QPoint;
import io.qt.core.QPointF;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QFont;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.gui.QPolygonF;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
class DesktopQtGraphCanvas extends QWidget {
    static final int MARGIN = 20;

    private static final int HEADER_PADDING_X = 10;
    private static final int HEADER_PADDING_Y = 6;
    private static final int ROW_PADDING_X = 10;
    private static final int ROW_PADDING_Y = 2;
    private static final int ICON_GAP = 4;
    private static final int ARC = 8;
    private static final int ARROW_LENGTH = 10;
    private static final int ARROW_HALF_WIDTH = 5;

    private final Runnable myRelayout;

    private List<DesktopQtGraphNode> myNodes = List.of();
    private List<DesktopQtGraphEdge> myEdges = List.of();
    private int myContentWidth;

    DesktopQtGraphCanvas(@Nullable QWidget parent, Runnable relayout) {
        super(parent);
        myRelayout = relayout;
    }

    void setGraph(List<DesktopQtGraphNode> nodes, List<DesktopQtGraphEdge> edges, int contentWidth, int contentHeight) {
        myNodes = nodes;
        myEdges = edges;
        myContentWidth = contentWidth;

        setMinimumSize(contentWidth + MARGIN * 2, contentHeight + MARGIN * 2);
        update();
    }

    Size2D measure(GraphNodeContent<DesktopQtTextItemPresentation> content) {
        QFontMetrics headerMetrics = new QFontMetrics(headerFont());
        QFontMetrics metrics = fontMetrics();

        int width = lineWidth(headerMetrics, content.getHeader()) + HEADER_PADDING_X * 2;
        int height = lineHeight(headerMetrics, content.getHeader()) + HEADER_PADDING_Y * 2;
        for (List<DesktopQtTextItemPresentation> section : content.getSections()) {
            height += 1;
            for (DesktopQtTextItemPresentation row : section) {
                width = Math.max(width, lineWidth(metrics, row) + ROW_PADDING_X * 2);
                height += lineHeight(metrics, row) + ROW_PADDING_Y * 2;
            }
        }
        return new Size2D(width, height);
    }

    private static int lineWidth(QFontMetrics metrics, DesktopQtTextItemPresentation presentation) {
        Image icon = presentation.getImage();
        int width = metrics.horizontalAdvance(presentation.toString());
        return icon == null ? width : width + icon.getWidth() + ICON_GAP;
    }

    private static int lineHeight(QFontMetrics metrics, DesktopQtTextItemPresentation presentation) {
        Image icon = presentation.getImage();
        return icon == null ? metrics.height() : Math.max(metrics.height(), icon.getHeight());
    }

    private QFont headerFont() {
        QFont font = new QFont(font());
        font.setBold(true);
        return font;
    }

    @Override
    protected void changeEvent(QEvent event) {
        super.changeEvent(event);

        QEvent.Type type = event.type();
        if (type == QEvent.Type.FontChange || type == QEvent.Type.StyleChange) {
            myRelayout.run();
        }
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.TextAntialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            int offsetX = Math.max(MARGIN, (width() - myContentWidth) / 2);
            painter.translate(offsetX, MARGIN);

            QPalette palette = palette();
            QColor defaultEdgeColor = palette.color(QPalette.ColorRole.PlaceholderText);
            QColor fill = palette.color(QPalette.ColorRole.Base);
            QColor border = palette.color(QPalette.ColorRole.Mid);
            QColor text = palette.color(QPalette.ColorRole.Text);

            for (DesktopQtGraphEdge edge : myEdges) {
                paintEdge(painter, edge, defaultEdgeColor, fill, text);
            }

            for (DesktopQtGraphNode node : myNodes) {
                paintNode(painter, node, fill, border, text);
            }
        }
        finally {
            painter.end();
        }
    }

    private void paintNode(QPainter painter, DesktopQtGraphNode node, QColor fill, QColor border, QColor text) {
        QRect bounds = node.bounds();

        painter.setPen(new QPen(border));
        painter.setBrush(new QBrush(fill));
        painter.drawRoundedRect(new QRectF(bounds.x() + 0.5, bounds.y() + 0.5, bounds.width() - 1, bounds.height() - 1), ARC, ARC);

        GraphNodeContent<DesktopQtTextItemPresentation> content = node.content();

        QFont headerFont = headerFont();
        QFontMetrics headerMetrics = new QFontMetrics(headerFont);
        DesktopQtTextItemPresentation header = content.getHeader();
        int headerHeight = lineHeight(headerMetrics, header) + HEADER_PADDING_Y * 2;
        int headerWidth = lineWidth(headerMetrics, header);
        int x = bounds.x() + (bounds.width() - headerWidth) / 2;

        painter.save();
        painter.setFont(headerFont);
        paintLine(painter, header, x, bounds.y(), headerHeight, bounds.right() - x, text);
        painter.restore();

        int y = bounds.y() + headerHeight;
        QFontMetrics metrics = fontMetrics();
        for (List<DesktopQtTextItemPresentation> section : content.getSections()) {
            painter.setPen(new QPen(border));
            painter.drawLine(bounds.x(), y, bounds.right(), y);
            y += 1;

            for (DesktopQtTextItemPresentation row : section) {
                int rowHeight = lineHeight(metrics, row) + ROW_PADDING_Y * 2;
                int rowX = bounds.x() + ROW_PADDING_X;
                paintLine(painter, row, rowX, y, rowHeight, bounds.right() - rowX, text);
                y += rowHeight;
            }
        }
    }

    private static void paintLine(QPainter painter, DesktopQtTextItemPresentation line, int x, int y, int height, int width, QColor text) {
        int textX = x;
        Image icon = line.getImage();
        if (icon != null) {
            DesktopQtImage.paint(painter, new QRect(textX, y + (height - icon.getHeight()) / 2, icon.getWidth(), icon.getHeight()), icon);
            textX += icon.getWidth() + ICON_GAP;
        }

        painter.setPen(new QPen(text));
        painter.drawText(new QRect(textX, y, Math.max(0, width - (textX - x)), height),
            Qt.AlignmentFlag.AlignLeft.value() | Qt.AlignmentFlag.AlignVCenter.value(),
            line.toString());
    }

    private void paintEdge(QPainter painter, DesktopQtGraphEdge edge, QColor defaultColor, QColor fill, QColor text) {
        QRect a = myNodes.get(edge.source()).bounds();
        QRect b = myNodes.get(edge.target()).bounds();

        int x1 = a.center().x();
        int y1 = a.center().y();
        int x2 = b.center().x();
        int y2 = b.center().y();
        if (b.top() >= a.bottom()) {
            y1 = a.bottom();
            y2 = b.top();
        }
        else if (b.bottom() <= a.top()) {
            y1 = a.top();
            y2 = b.bottom();
        }
        else if (b.left() >= a.right()) {
            x1 = a.right();
            x2 = b.left();
        }
        else {
            x1 = a.left();
            x2 = b.right();
        }

        GraphEdgeStyle style = edge.style();
        ColorValue colorValue = style.getColor();
        QColor color = colorValue == null ? defaultColor : TargetQt.to(colorValue);

        QPen pen = new QPen(color);
        pen.setStyle(switch (style.getLineStyle()) {
            case SOLID -> Qt.PenStyle.SolidLine;
            case DASHED -> Qt.PenStyle.DashLine;
            case DOTTED -> Qt.PenStyle.DotLine;
        });
        painter.setPen(pen);
        painter.drawLine(x1, y1, x2, y2);

        painter.setPen(new QPen(color));
        paintArrowHead(painter, style.getTargetArrow(), x1, y1, x2, y2, color, fill);
        paintArrowHead(painter, style.getSourceArrow(), x2, y2, x1, y1, color, fill);

        String label = style.getLabel().get();
        if (!label.isEmpty()) {
            painter.setPen(new QPen(text));
            QFontMetrics metrics = fontMetrics();
            int labelWidth = metrics.horizontalAdvance(label);
            painter.drawText(new QPoint((x1 + x2) / 2 - labelWidth / 2 + 4, (y1 + y2) / 2), label);
        }
    }

    private static void paintArrowHead(QPainter painter, GraphArrow arrow, int x1, int y1, int x2, int y2, QColor color, QColor fill) {
        if (arrow == GraphArrow.NONE) {
            return;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length == 0) {
            return;
        }

        double ux = dx / length;
        double uy = dy / length;
        double baseX = x2 - ux * ARROW_LENGTH;
        double baseY = y2 - uy * ARROW_LENGTH;
        QPointF tip = new QPointF(x2, y2);
        QPointF left = new QPointF(baseX - uy * ARROW_HALF_WIDTH, baseY + ux * ARROW_HALF_WIDTH);
        QPointF right = new QPointF(baseX + uy * ARROW_HALF_WIDTH, baseY - ux * ARROW_HALF_WIDTH);

        switch (arrow) {
            case FILLED, TRIANGLE -> {
                painter.setBrush(new QBrush(arrow == GraphArrow.FILLED ? color : fill));
                QPolygonF head = new QPolygonF();
                head.append(tip);
                head.append(left);
                head.append(right);
                painter.drawPolygon(head);
            }
            case OPEN -> {
                painter.drawLine(left, tip);
                painter.drawLine(tip, right);
            }
            case DIAMOND -> {
                painter.setBrush(new QBrush(fill));
                QPolygonF head = new QPolygonF();
                head.append(tip);
                head.append(left);
                head.append(new QPointF(x2 - ux * ARROW_LENGTH * 2, y2 - uy * ARROW_LENGTH * 2));
                head.append(right);
                painter.drawPolygon(head);
            }
            case CIRCLE -> {
                painter.setBrush(new QBrush(fill));
                double cx = x2 - ux * ARROW_HALF_WIDTH;
                double cy = y2 - uy * ARROW_HALF_WIDTH;
                painter.drawEllipse(new QPointF(cx, cy), ARROW_HALF_WIDTH, ARROW_HALF_WIDTH);
            }
            default -> {
            }
        }
    }
}
