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
package consulo.desktop.awt.versionSystemControl;

import consulo.desktop.awt.ui.impl.image.DesktopAWTCellImage;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.versionControlSystem.log.graph.paint.VcsGraphDot;
import consulo.versionControlSystem.log.graph.paint.VcsGraphMouseListener;
import consulo.versionControlSystem.log.graph.paint.VcsGraphRow;
import consulo.versionControlSystem.log.graph.paint.VcsGraphSegment;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
final class DesktopAWTVcsGraphRowImage implements DesktopAWTCellImage, Icon {
    private final VcsGraphRow myRow;

    DesktopAWTVcsGraphRowImage(VcsGraphRow row) {
        myRow = row;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        int rowHeight = c.getHeight();
        if (rowHeight <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, 0);

            for (VcsGraphSegment segment : myRow.resolveSegments(rowHeight)) {
                g2.setColor(TargetAWT.to(segment.color()));
                float[] dash = segment.dash();
                g2.setStroke(dash == null
                    ? new BasicStroke(segment.width(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL)
                    : new BasicStroke(segment.width(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, dash, dash[0] / 2));
                g2.draw(new Line2D.Float(segment.x1(), segment.y1(), segment.x2(), segment.y2()));
            }

            for (VcsGraphDot dot : myRow.resolveDots(rowHeight)) {
                g2.setColor(TargetAWT.to(dot.color()));
                float radius = dot.radius();
                g2.fill(new Ellipse2D.Float(dot.x() - radius, dot.y() - radius, 2 * radius, 2 * radius));
            }
        }
        finally {
            g2.dispose();
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
    public int getIconWidth() {
        return myRow.getWidth();
    }

    @Override
    public int getIconHeight() {
        return 1;
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
