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
package consulo.desktop.qt.versionSystemControl;

import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.image.DesktopQtCellImage;
import consulo.versionControlSystem.log.graph.paint.VcsGraphDot;
import consulo.versionControlSystem.log.graph.paint.VcsGraphMouseListener;
import consulo.versionControlSystem.log.graph.paint.VcsGraphRow;
import consulo.versionControlSystem.log.graph.paint.VcsGraphSegment;
import io.qt.core.QLineF;
import io.qt.core.QPointF;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QPainter;
import io.qt.gui.QPen;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
final class DesktopQtVcsGraphRowImage implements DesktopQtCellImage {
    private final VcsGraphRow myRow;

    DesktopQtVcsGraphRowImage(VcsGraphRow row) {
        myRow = row;
    }

    @Override
    public void paintInCell(QPainter painter, QRect cell) {
        int rowHeight = cell.height();
        if (rowHeight <= 0) {
            return;
        }

        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
        painter.translate(cell.left(), cell.top());

        for (VcsGraphSegment segment : myRow.resolveSegments(rowHeight)) {
            QPen pen = new QPen(TargetQt.to(segment.color()));
            pen.setWidthF(segment.width());
            pen.setCapStyle(Qt.PenCapStyle.RoundCap);
            pen.setJoinStyle(Qt.PenJoinStyle.BevelJoin);
            float[] dash = segment.dash();
            if (dash != null) {
                List<Double> pattern = new ArrayList<>();
                for (float value : dash) {
                    pattern.add((double) value / segment.width());
                }
                pen.setDashPattern(pattern);
                pen.setDashOffset(dash[0] / 2 / segment.width());
            }
            painter.setPen(pen);
            painter.drawLine(new QLineF(segment.x1(), segment.y1(), segment.x2(), segment.y2()));
        }

        painter.setPen(Qt.PenStyle.NoPen);
        for (VcsGraphDot dot : myRow.resolveDots(rowHeight)) {
            painter.setBrush(new QBrush(TargetQt.to(dot.color())));
            painter.drawEllipse(new QPointF(dot.x(), dot.y()), dot.radius(), dot.radius());
        }
    }

    @Override
    public void mouseMoved(int x, int y, int height) {
        VcsGraphMouseListener listener = myRow.getMouseListener();
        if (listener != null) {
            listener.mouseMoved(x, y, height);
        }
    }

    @Override
    public void mouseClicked(int x, int y, int height) {
        VcsGraphMouseListener listener = myRow.getMouseListener();
        if (listener != null) {
            listener.mouseClicked(x, y, height);
        }
    }

    @Override
    public void mouseExited() {
        VcsGraphMouseListener listener = myRow.getMouseListener();
        if (listener != null) {
            listener.mouseExited();
        }
    }

    @Override
    public int getWidth() {
        return myRow.getWidth();
    }

    @Override
    public int getHeight() {
        return 1;
    }
}
