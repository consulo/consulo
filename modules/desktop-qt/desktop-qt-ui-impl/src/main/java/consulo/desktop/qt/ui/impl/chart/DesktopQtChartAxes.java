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
import io.qt.gui.QColor;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QPainter;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class DesktopQtChartAxes {
    static final int MAJOR_TICK = 5;
    static final int MINOR_TICK = 2;

    private DesktopQtChartAxes() {
    }

    static int timeAxisHeight(QFontMetrics metrics) {
        return MAJOR_TICK + metrics.height() + 2;
    }

    static void paintTimeAxis(QPainter painter,
                              QFontMetrics metrics,
                              int x,
                              int y,
                              int width,
                              List<AxisMarkers.Marker> markers,
                              QColor color) {
        painter.setPen(color);
        for (AxisMarkers.Marker marker : markers) {
            int markerX = x + Math.round(marker.offset() * width);
            painter.drawLine(markerX, y, markerX, y + (marker.isMajor() ? MAJOR_TICK : MINOR_TICK));
            String label = marker.label();
            if (label != null) {
                painter.drawText(markerX + 2, y + MAJOR_TICK + metrics.ascent(), label);
            }
        }
    }
}
