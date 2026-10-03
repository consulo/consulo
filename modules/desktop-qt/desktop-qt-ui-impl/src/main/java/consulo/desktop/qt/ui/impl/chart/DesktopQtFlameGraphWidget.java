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
package consulo.desktop.qt.ui.impl.chart;

import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.ui.chart.FlameGraphModel;
import consulo.ui.chart.FlameGraphOrientation;
import consulo.ui.color.ColorValue;
import consulo.ui.impl.chart.ChartFormatters;
import consulo.ui.impl.chart.ChartPalette;
import consulo.ui.impl.chart.FlameGraphNode;
import io.qt.core.QEvent;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QHelpEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QToolTip;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopQtFlameGraphWidget<E> extends QWidget {
    private static final int TEXT_PADDING = 3;
    private static final int GAP = 1;

    private final DesktopQtFlameGraphImpl<E> myGraph;

    DesktopQtFlameGraphWidget(DesktopQtFlameGraphImpl<E> graph) {
        myGraph = graph;
        setMouseTracking(true);
    }

    void contentChanged() {
        setMinimumHeight((myGraph.getMaxDepth() + 1) * rowHeight());
        updateGeometry();
        update();
    }

    int rowHeight() {
        return fontMetrics().height() + 6;
    }

    private long focusStart() {
        FlameGraphNode<E> focused = myGraph.getFocusedNode();
        return focused == null ? myGraph.getRoot().getStart() : focused.getStart();
    }

    private long focusLength() {
        FlameGraphNode<E> focused = myGraph.getFocusedNode();
        FlameGraphNode<E> node = focused == null ? myGraph.getRoot() : focused;
        return Math.max(1, node.getDuration());
    }

    private int rowTop(int depth) {
        int rowHeight = rowHeight();
        return myGraph.getOrientation() == FlameGraphOrientation.FLAME ? height() - (depth + 1) * rowHeight : depth * rowHeight;
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            painter.setRenderHint(QPainter.RenderHint.TextAntialiasing, true);
            QPalette palette = palette();
            painter.fillRect(rect(), palette.color(QPalette.ColorRole.Base));
            paintNode(painter, fontMetrics(), myGraph.getRoot(), focusStart(), focusLength());
        }
        finally {
            painter.end();
        }
    }

    private void paintNode(QPainter painter, QFontMetrics metrics, FlameGraphNode<E> node, long focusStart, long focusLength) {
        double x1 = Math.max(0, (node.getStart() - focusStart) / (double) focusLength * width());
        double x2 = Math.min(width(), (node.getEnd() - focusStart) / (double) focusLength * width());
        if (x2 - x1 < 1) {
            return;
        }

        FlameGraphModel<E> model = myGraph.getModel();
        E value = node.getValue();
        String name = model.getName(value);
        ColorValue modelColor = model.getColor(value);
        QColor fill = new QColor(TargetQt.to(modelColor != null ? modelColor : ChartPalette.flame(name)));
        if (!myGraph.isHighlighted(value)) {
            fill.setAlpha(90);
        }

        int top = rowTop(node.getDepth());
        int left = (int) Math.round(x1);
        int width = Math.max(1, (int) Math.round(x2) - left - GAP);
        int height = rowHeight() - GAP;
        painter.fillRect(left, top, width, height, fill);

        if (value.equals(myGraph.getSelected())) {
            painter.setPen(new QColor(Qt.GlobalColor.black));
            painter.drawRect(left, top, width - 1, height - 1);
        }

        String text = metrics.elidedText(name, Qt.TextElideMode.ElideRight, width - TEXT_PADDING * 2);
        if (!text.isEmpty() && width > TEXT_PADDING * 2 + metrics.horizontalAdvance("..")) {
            painter.setPen(new QColor(Qt.GlobalColor.black));
            painter.drawText(left + TEXT_PADDING, top + (height + metrics.ascent() - metrics.descent()) / 2, text);
        }

        for (FlameGraphNode<E> child : node.getChildren()) {
            paintNode(painter, metrics, child, focusStart, focusLength);
        }
    }

    private @Nullable FlameGraphNode<E> nodeAt(QPoint point) {
        int rowHeight = rowHeight();
        int depth = myGraph.getOrientation() == FlameGraphOrientation.FLAME ? (height() - point.y()) / rowHeight : point.y() / rowHeight;
        if (depth < 0) {
            return null;
        }
        long focusStart = focusStart();
        long focusLength = focusLength();
        double position = focusStart + point.x() / (double) Math.max(1, width()) * focusLength;
        return find(myGraph.getRoot(), depth, position);
    }

    private @Nullable FlameGraphNode<E> find(FlameGraphNode<E> node, int depth, double position) {
        if (position < node.getStart() || position >= node.getEnd()) {
            return null;
        }
        if (node.getDepth() == depth) {
            return node;
        }
        for (FlameGraphNode<E> child : node.getChildren()) {
            FlameGraphNode<E> found = find(child, depth, position);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() == Qt.MouseButton.LeftButton) {
            myGraph.clicked(nodeAt(event.position().toPoint()), this, event, false);
        }
        super.mousePressEvent(event);
    }

    @Override
    protected void mouseDoubleClickEvent(QMouseEvent event) {
        if (event.button() == Qt.MouseButton.LeftButton) {
            myGraph.clicked(nodeAt(event.position().toPoint()), this, event, true);
        }
        super.mouseDoubleClickEvent(event);
    }

    @Override
    public boolean event(QEvent event) {
        if (event.type() == QEvent.Type.ToolTip && event instanceof QHelpEvent help) {
            FlameGraphNode<E> node = nodeAt(help.pos());
            if (node == null) {
                QToolTip.hideText();
                event.ignore();
            }
            else {
                QToolTip.showText(help.globalPos(), tooltip(node), this);
            }
            return true;
        }
        return super.event(event);
    }

    private String tooltip(FlameGraphNode<E> node) {
        FlameGraphModel<E> model = myGraph.getModel();
        long total = myGraph.getRoot().getDuration();
        long weight = node.getDuration();
        double scale = ChartFormatters.storageScale(model.getWeightUnit());
        String value = ChartFormatters.forUnit(model.getWeightUnit()).getFormattedString(total * scale, weight * scale, true);
        double percent = total == 0 ? 0 : weight * 100.0 / total;
        return String.format("%s%n%s (%.2f%%)", model.getName(node.getValue()), value, percent);
    }
}
