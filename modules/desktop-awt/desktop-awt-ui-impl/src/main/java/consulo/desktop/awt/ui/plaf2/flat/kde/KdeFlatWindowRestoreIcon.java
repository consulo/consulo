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

import com.formdev.flatlaf.icons.FlatWindowRestoreIcon;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * @author VISTALL
 * @since 2026-03-22
 */
public class KdeFlatWindowRestoreIcon extends FlatWindowRestoreIcon {
    @Override
    protected void paintIconAt1x(Graphics2D g, int x, int y, int width, int height, double scaleFactor) {
        float iwh = (float) ((symbolHeight + 2) * scaleFactor);
        float ix = x + ((width - iwh) / 2f);
        float iy = y + ((height - iwh) / 2f);
        float thickness = Math.max((float) scaleFactor, 1f);

        float cx = ix + iwh / 2f;
        float cy = iy + iwh / 2f;

        // diamond: top → right → bottom → left
        Path2D path = new Path2D.Float();
        path.moveTo(cx, iy);
        path.lineTo(ix + iwh, cy);
        path.lineTo(cx, iy + iwh);
        path.lineTo(ix, cy);
        path.closePath();

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        g.setStroke(oldStroke);
    }
}
