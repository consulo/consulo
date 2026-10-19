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
package consulo.desktop.qt.editor.impl.internal;

import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPen;

/**
 * Draws a half of a line frame: the gutter half is closed on the left, the text half on the right, and both are open
 * where they meet - as the awt editor draws it.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class DesktopQtLineFramePainter {
    private static final double RADIUS = 4;

    private DesktopQtLineFramePainter() {
    }

    public static void paint(QPainter painter, QColor color, int left, int top, int right, int bottom, boolean closedOnLeft) {
        if (right <= left || bottom <= top) {
            return;
        }

        double radius = Math.min(RADIUS, Math.min(right - left, bottom - top) / 2.0);
        double x0 = left + 0.5;
        double x1 = right - 0.5;
        double y0 = top + 0.5;
        double y1 = bottom - 0.5;

        QPainterPath path = new QPainterPath();
        if (closedOnLeft) {
            path.moveTo(x1, y0);
            path.lineTo(x0 + radius, y0);
            path.quadTo(x0, y0, x0, y0 + radius);
            path.lineTo(x0, y1 - radius);
            path.quadTo(x0, y1, x0 + radius, y1);
            path.lineTo(x1, y1);
        }
        else {
            path.moveTo(x0, y0);
            path.lineTo(x1 - radius, y0);
            path.quadTo(x1, y0, x1, y0 + radius);
            path.lineTo(x1, y1 - radius);
            path.quadTo(x1, y1, x1 - radius, y1);
            path.lineTo(x0, y1);
        }

        painter.save();
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
        painter.setBrush(Qt.BrushStyle.NoBrush);
        painter.setPen(new QPen(color, 1));
        painter.drawPath(path);
        painter.restore();
    }
}
