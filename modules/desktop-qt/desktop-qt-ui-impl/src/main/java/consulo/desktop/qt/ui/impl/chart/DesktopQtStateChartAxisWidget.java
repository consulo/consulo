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

import consulo.ui.impl.chart.AxisMarkers;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopQtStateChartAxisWidget extends QWidget {
    private final DesktopQtStateChartImpl<?> myChart;
    private final QScrollArea myScrollArea;

    DesktopQtStateChartAxisWidget(QWidget parent, DesktopQtStateChartImpl<?> chart, QScrollArea scrollArea) {
        super(parent);
        myChart = chart;
        myScrollArea = scrollArea;
        setFixedHeight(DesktopQtChartAxes.timeAxisHeight(fontMetrics()));
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            QPalette palette = palette();
            painter.fillRect(rect(), palette.color(QPalette.ColorRole.Base));
            QFontMetrics metrics = fontMetrics();
            int x = DesktopQtStateChartImpl.NAME_WIDTH;
            int scrollBar = myScrollArea.verticalScrollBar().isVisible() ? myScrollArea.verticalScrollBar().width() : 0;
            int width = Math.max(1, width() - x - scrollBar);
            DesktopQtChartAxes.paintTimeAxis(painter, metrics, x, 0, width, AxisMarkers.compute(myChart.getTimeAxisModel(), true, false),
                palette.color(QPalette.ColorRole.Text));
        }
        finally {
            painter.end();
        }
    }
}
