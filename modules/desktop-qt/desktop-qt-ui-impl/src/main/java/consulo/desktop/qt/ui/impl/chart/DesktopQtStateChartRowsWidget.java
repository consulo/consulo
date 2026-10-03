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
import consulo.ui.impl.chart.StatePresentation;
import consulo.ui.impl.chart.StateRowImpl;
import consulo.ui.impl.chart.model.Range;
import consulo.ui.impl.chart.model.SeriesData;
import io.qt.core.QEvent;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QHelpEvent;
import io.qt.gui.QHideEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QShowEvent;
import io.qt.widgets.QToolTip;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopQtStateChartRowsWidget<S> extends QWidget {
    static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 3;
    private static final int PADDING = 4;

    private final DesktopQtStateChartImpl<S> myChart;

    DesktopQtStateChartRowsWidget(DesktopQtStateChartImpl<S> chart) {
        myChart = chart;
        setMouseTracking(true);
        destroyed.connect(object -> chart.getSubscription().deactivate());
    }

    void rowsChanged() {
        setMinimumHeight(myChart.getRowList().size() * ROW_HEIGHT);
        updateGeometry();
        update();
    }

    @Override
    protected void showEvent(QShowEvent event) {
        super.showEvent(event);
        myChart.getSubscription().activate();
    }

    @Override
    protected void hideEvent(QHideEvent event) {
        super.hideEvent(event);
        myChart.getSubscription().deactivate();
    }

    private int chartX() {
        return DesktopQtStateChartImpl.NAME_WIDTH;
    }

    private int chartWidth() {
        return Math.max(1, width() - chartX());
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            QPalette palette = palette();
            painter.fillRect(rect(), palette.color(QPalette.ColorRole.Base));
            QColor text = palette.color(QPalette.ColorRole.Text);
            QColor unknown = palette.color(QPalette.ColorRole.Mid);
            QFontMetrics metrics = fontMetrics();

            Range view = myChart.getAxisImpl().getTimeline().getViewRange();
            double dataMax = myChart.getAxisImpl().getTimeline().getDataRange().getMax();

            List<StateRowImpl<S>> rows = myChart.getRowList();
            for (int i = 0; i < rows.size(); i++) {
                StateRowImpl<S> row = rows.get(i);
                int top = i * ROW_HEIGHT;

                String name = metrics.elidedText(row.getName().get(), Qt.TextElideMode.ElideRight, chartX() - PADDING * 2);
                painter.setPen(text);
                painter.drawText(PADDING, top + (ROW_HEIGHT + metrics.ascent() - metrics.descent()) / 2, name);

                if (view.getLength() <= 0) {
                    continue;
                }
                List<SeriesData<S>> data = row.getData().getData();
                for (int index = firstVisible(data, view.getMin()); index < data.size(); index++) {
                    SeriesData<S> item = data.get(index);
                    if (item.x > view.getMax()) {
                        break;
                    }
                    double end = index + 1 < data.size() ? data.get(index + 1).x : dataMax;
                    int x1 = toX(Math.max(item.x, view.getMin()), view);
                    int x2 = toX(Math.min(end, view.getMax()), view);
                    if (x2 <= x1) {
                        continue;
                    }
                    StatePresentation presentation = myChart.getPresentation(item.value);
                    QColor color = presentation == null ? unknown : TargetQt.to(presentation.color());
                    painter.fillRect(x1, top + ROW_GAP, x2 - x1, ROW_HEIGHT - ROW_GAP * 2, color);
                }
            }
        }
        finally {
            painter.end();
        }
    }

    private int toX(double micros, Range view) {
        return chartX() + (int) Math.round((micros - view.getMin()) / view.getLength() * chartWidth());
    }

    private static <S> int firstVisible(List<SeriesData<S>> data, double min) {
        int low = 0;
        int high = data.size() - 1;
        int result = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (data.get(mid).x <= min) {
                result = mid;
                low = mid + 1;
            }
            else {
                high = mid - 1;
            }
        }
        return result;
    }

    private @Nullable String tooltipAt(QPoint point) {
        int rowIndex = point.y() / ROW_HEIGHT;
        List<StateRowImpl<S>> rows = myChart.getRowList();
        if (rowIndex < 0 || rowIndex >= rows.size() || point.x() < chartX()) {
            return null;
        }
        Range view = myChart.getAxisImpl().getTimeline().getViewRange();
        double micros = view.getMin() + (point.x() - chartX()) / (double) chartWidth() * view.getLength();
        List<SeriesData<S>> data = rows.get(rowIndex).getData().getData();
        if (data.isEmpty() || data.get(0).x > micros) {
            return null;
        }
        S state = data.get(firstVisible(data, micros)).value;
        StatePresentation presentation = myChart.getPresentation(state);
        return presentation == null ? String.valueOf(state) : presentation.label().get();
    }

    @Override
    public boolean event(QEvent event) {
        if (event.type() == QEvent.Type.ToolTip && event instanceof QHelpEvent help) {
            String tooltip = tooltipAt(help.pos());
            if (tooltip == null) {
                QToolTip.hideText();
                event.ignore();
            }
            else {
                QToolTip.showText(help.globalPos(), tooltip, this);
            }
            return true;
        }
        return super.event(event);
    }
}
