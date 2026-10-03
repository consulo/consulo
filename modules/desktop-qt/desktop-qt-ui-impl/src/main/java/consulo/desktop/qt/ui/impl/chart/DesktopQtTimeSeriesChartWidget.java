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
import consulo.ui.chart.TimeSeriesKind;
import consulo.ui.impl.chart.AxisMarkers;
import consulo.ui.impl.chart.TimeSeriesChartModel;
import consulo.ui.impl.chart.TimeSeriesImpl;
import consulo.ui.impl.chart.model.Range;
import consulo.ui.impl.chart.model.SeriesData;
import consulo.ui.impl.chart.model.StreamingTimeline;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QHideEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.gui.QShowEvent;
import io.qt.gui.QWheelEvent;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopQtTimeSeriesChartWidget extends QWidget {
    private static final int PADDING = 4;
    private static final int LEGEND_ICON = 10;

    private final TimeSeriesChartModel myModel;
    private double myDragStart = Double.NaN;

    DesktopQtTimeSeriesChartWidget(@Nullable QWidget parent, DesktopQtTimeSeriesChartImpl chart) {
        super(parent);
        TimeSeriesChartModel model = chart.getModel();
        myModel = model;
        setMinimumHeight(120);
        destroyed.connect(object -> model.deactivate());
    }

    @Override
    protected void showEvent(QShowEvent event) {
        super.showEvent(event);
        myModel.activate();
    }

    @Override
    protected void hideEvent(QHideEvent event) {
        super.hideEvent(event);
        myModel.deactivate();
    }

    private QRect plotArea(QFontMetrics metrics) {
        int legendHeight = metrics.height() + PADDING * 2;
        int left = PADDING;
        for (AxisMarkers.Marker marker : AxisMarkers.compute(myModel.getValueAxisModel(), true, false)) {
            String label = marker.label();
            if (label != null) {
                left = Math.max(left, metrics.horizontalAdvance(label) + PADDING * 2);
            }
        }
        int bottom = DesktopQtChartAxes.timeAxisHeight(metrics);
        return new QRect(left, legendHeight, Math.max(1, width() - left - PADDING), Math.max(1, height() - legendHeight - bottom));
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.TextAntialiasing, true);

            QPalette palette = palette();
            QColor background = palette.color(QPalette.ColorRole.Base);
            QColor text = palette.color(QPalette.ColorRole.Text);
            QColor grid = new QColor(palette.color(QPalette.ColorRole.Mid));
            grid.setAlpha(80);

            painter.fillRect(rect(), background);

            QFontMetrics metrics = fontMetrics();
            QRect plot = plotArea(metrics);

            paintValueAxis(painter, metrics, plot, text, grid);
            paintSeries(painter, plot);
            paintSelection(painter, plot, palette.color(QPalette.ColorRole.Highlight));
            DesktopQtChartAxes.paintTimeAxis(painter, metrics, plot.x(), plot.y() + plot.height(), plot.width(),
                AxisMarkers.compute(myModel.getTimeAxisModel(), true, false), text);
            paintLegend(painter, metrics, text);
        }
        finally {
            painter.end();
        }
    }

    private void paintValueAxis(QPainter painter, QFontMetrics metrics, QRect plot, QColor text, QColor grid) {
        for (AxisMarkers.Marker marker : AxisMarkers.compute(myModel.getValueAxisModel(), true, false)) {
            String label = marker.label();
            if (label == null) {
                continue;
            }
            int y = plot.y() + plot.height() - Math.round(marker.offset() * plot.height());
            painter.setPen(grid);
            painter.drawLine(plot.x(), y, plot.x() + plot.width(), y);
            painter.setPen(text);
            painter.drawText(plot.x() - PADDING - metrics.horizontalAdvance(label), y + metrics.ascent() / 2, label);
        }
    }

    private void paintSeries(QPainter painter, QRect plot) {
        StreamingTimeline timeline = myModel.getTimeline();
        Range view = timeline.getViewRange();
        Range yRange = myModel.getYRange();
        if (view.getLength() <= 0) {
            return;
        }
        double yLength = yRange.getLength() <= 0 ? 1 : yRange.getLength();

        painter.save();
        painter.setClipRect(plot);
        for (TimeSeriesImpl series : myModel.getSeries()) {
            List<SeriesData<Long>> points = series.getRanged().getSeries();
            if (points.isEmpty()) {
                continue;
            }

            QPainterPath line = new QPainterPath();
            QPainterPath area = new QPainterPath();
            double bottom = plot.y() + plot.height();
            for (int i = 0; i < points.size(); i++) {
                SeriesData<Long> point = points.get(i);
                double x = plot.x() + (point.x - view.getMin()) / view.getLength() * plot.width();
                double y = bottom - (point.value - yRange.getMin()) / yLength * plot.height();
                if (i == 0) {
                    line.moveTo(x, y);
                    area.moveTo(x, bottom);
                }
                else {
                    line.lineTo(x, y);
                }
                area.lineTo(x, y);
                if (i == points.size() - 1) {
                    area.lineTo(x, bottom);
                    area.closeSubpath();
                }
            }

            QColor color = TargetQt.to(series.getColor());
            if (series.getKind() == TimeSeriesKind.AREA) {
                QColor fill = new QColor(color);
                fill.setAlpha(150);
                painter.fillPath(area, new QBrush(fill));
            }
            QPen pen = new QPen(color);
            pen.setWidthF(series.getKind() == TimeSeriesKind.AREA ? 1.0 : 2.0);
            painter.setPen(pen);
            painter.setBrush(new QBrush(Qt.BrushStyle.NoBrush));
            painter.drawPath(line);
        }
        painter.restore();
    }

    private void paintSelection(QPainter painter, QRect plot, QColor highlight) {
        StreamingTimeline timeline = myModel.getTimeline();
        Range selection = timeline.getSelectionRange();
        Range view = timeline.getViewRange();
        if (selection.isEmpty() || view.getLength() <= 0) {
            return;
        }
        double from = plot.x() + (selection.getMin() - view.getMin()) / view.getLength() * plot.width();
        double to = plot.x() + (selection.getMax() - view.getMin()) / view.getLength() * plot.width();
        int left = (int) Math.max(plot.x(), Math.min(from, to));
        int right = (int) Math.min(plot.x() + plot.width(), Math.max(from, to));
        if (right <= left) {
            return;
        }
        QColor fill = new QColor(highlight);
        fill.setAlpha(60);
        painter.fillRect(left, plot.y(), right - left, plot.height(), fill);
    }

    private void paintLegend(QPainter painter, QFontMetrics metrics, QColor text) {
        int x = width() - PADDING;
        int baseline = PADDING + metrics.ascent();
        List<TimeSeriesImpl> seriesList = myModel.getSeries();
        for (int i = seriesList.size() - 1; i >= 0; i--) {
            TimeSeriesImpl series = seriesList.get(i);
            String label = series.getName().get() + ": " + myModel.formatLatest(series);
            int labelWidth = metrics.horizontalAdvance(label);
            x -= labelWidth;
            painter.setPen(text);
            painter.drawText(x, baseline, label);
            x -= LEGEND_ICON + PADDING;
            painter.fillRect(x, baseline - LEGEND_ICON + 1, LEGEND_ICON, LEGEND_ICON, TargetQt.to(series.getColor()));
            x -= PADDING * 3;
        }
    }

    private double toRatio(double x) {
        QRect plot = plotArea(fontMetrics());
        return (x - plot.x()) / Math.max(1, plot.width());
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() == Qt.MouseButton.LeftButton) {
            myDragStart = toRatio(event.position().x());
            myModel.clearSelection();
            update();
        }
        super.mousePressEvent(event);
    }

    @Override
    protected void mouseMoveEvent(QMouseEvent event) {
        if (!Double.isNaN(myDragStart) && event.buttons().testFlag(Qt.MouseButton.LeftButton)) {
            myModel.select(myDragStart, toRatio(event.position().x()));
            update();
        }
        super.mouseMoveEvent(event);
    }

    @Override
    protected void mouseReleaseEvent(QMouseEvent event) {
        myDragStart = Double.NaN;
        super.mouseReleaseEvent(event);
    }

    @Override
    protected void wheelEvent(QWheelEvent event) {
        double count = -event.angleDelta().y() / 120.0;
        myModel.handleMouseWheel(count, event.modifiers().testFlag(Qt.KeyboardModifier.ControlModifier), toRatio(event.position().x()));
        event.accept();
    }
}
