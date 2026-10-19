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
package consulo.desktop.awt.editor.impl.internal;

import consulo.ui.ex.awt.JBUI;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Draws a half of a line frame: the gutter half is closed on the left, the text half on the right, and both are open
 * where they meet.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class LineFramePainter {
    private LineFramePainter() {
    }

    public static void paint(Graphics2D g, Color color, int left, int top, int right, int bottom, boolean closedOnLeft) {
        if (right <= left || bottom <= top) {
            return;
        }

        float radius = Math.min(JBUI.scale(4), Math.min(right - left, bottom - top) / 2f);
        float x0 = left + 0.5f;
        float x1 = right - 0.5f;
        float y0 = top + 0.5f;
        float y1 = bottom - 0.5f;

        Path2D.Float path = new Path2D.Float();
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

        Object antialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Stroke stroke = g.getStroke();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(JBUI.scale(1f)));
        g.setColor(color);
        g.draw(path);
        g.setStroke(stroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
    }
}
