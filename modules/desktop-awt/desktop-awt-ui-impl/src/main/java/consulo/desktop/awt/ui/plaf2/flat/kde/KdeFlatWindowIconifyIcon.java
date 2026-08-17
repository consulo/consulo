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
package consulo.desktop.awt.ui.plaf2.flat.kde;

import com.formdev.flatlaf.icons.FlatWindowIconifyIcon;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * @author VISTALL
 * @since 2026-03-22
 */
public class KdeFlatWindowIconifyIcon extends FlatWindowIconifyIcon {
    @Override
    protected void paintIconAt1x(Graphics2D g, int x, int y, int width, int height, double scaleFactor) {
        double iw = (float) ((symbolHeight + 2) * scaleFactor);
        double ih = iw * 0.5f;
        double ix = x + ((width - iw) / 2f);
        double iy = y + ((height - ih) / 2f) + scaleFactor;
        float thickness = Math.max((float) scaleFactor, 1f);

        // downward chevron: M iw,0 L iw/2,ih L 0,0
        Path2D path = new Path2D.Float();
        path.moveTo(ix, iy);
        path.lineTo(ix + iw / 2f, iy + ih);
        path.lineTo(ix + iw, iy);

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        g.setStroke(oldStroke);
    }
}
