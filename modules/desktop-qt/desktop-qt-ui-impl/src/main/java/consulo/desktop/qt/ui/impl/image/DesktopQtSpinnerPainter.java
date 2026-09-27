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
package consulo.desktop.qt.ui.impl.image;

import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QPaintDevice;
import io.qt.gui.QPainter;
import io.qt.gui.QPen;

import java.util.concurrent.TimeUnit;

public final class DesktopQtSpinnerPainter {
    public static final int FRAME_DELAY = 40;

    private static final int STEP = 12;
    private static final int ARC = 100;
    private static final int FULL_CIRCLE = 360;
    private static final int TRACK_ALPHA = 51;
    private static final int THICKNESS_DIVIDER = 8;

    private static final int DEGREE = 16;
    private static final int TOP_ANGLE = 90 * DEGREE;

    private static final long PERIOD = (long) FULL_CIRCLE / STEP * FRAME_DELAY;

    private DesktopQtSpinnerPainter() {
    }

    public static long millisToNextStep() {
        return FRAME_DELAY - currentMillis() % FRAME_DELAY;
    }

    public static void paintBusy(QPainter painter, QRect rect, QColor color) {
        int angle = (int) (currentMillis() % PERIOD / FRAME_DELAY) * STEP;

        paint(painter, rect, color, TOP_ANGLE - angle * DEGREE, -ARC * DEGREE);
    }

    public static void paintProgress(QPainter painter, QRect rect, QColor color, double fraction) {
        paint(painter, rect, color, TOP_ANGLE, -(int) (Math.clamp(fraction, 0, 1) * FULL_CIRCLE * DEGREE));
    }

    private static long currentMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    private static void paint(QPainter painter, QRect rect, QColor color, int startAngle, int spanAngle) {
        int side = Math.min(rect.width(), rect.height());
        if (side <= 0) {
            return;
        }

        QPaintDevice device = painter.device();
        double ratio = device == null ? 1 : device.devicePixelRatio();

        double thickness = Math.max(1 / Math.max(ratio, 1), side / (double) THICKNESS_DIVIDER);
        double inset = thickness / 2;

        QRectF ring = new QRectF(rect.x() + (rect.width() - side) / 2.0 + inset,
            rect.y() + (rect.height() - side) / 2.0 + inset,
            side - thickness,
            side - thickness);

        painter.save();
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);

            QPen pen = new QPen(new QColor(color.red(), color.green(), color.blue(), TRACK_ALPHA));
            pen.setWidthF(thickness);
            pen.setCapStyle(Qt.PenCapStyle.RoundCap);

            painter.setPen(pen);
            painter.drawArc(ring, 0, FULL_CIRCLE * DEGREE);

            pen.setColor(color);
            painter.setPen(pen);
            painter.drawArc(ring, startAngle, spanAngle);
        }
        finally {
            painter.restore();
        }
    }
}
