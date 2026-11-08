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
package consulo.desktop.awt.ui.impl.layout;

import consulo.ui.color.RGBColor;
import consulo.ui.internal.GradientColor;
import consulo.ui.internal.GradientColorBlob;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * @author VISTALL
 * @since 2026-11-07
 */
final class DesktopAWTGradientPainter {
    private DesktopAWTGradientPainter() {
    }

    static void paint(Graphics2D g, GradientColor gradient, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int size = Math.max(width, height);
            for (GradientColorBlob blob : gradient.getBlobs()) {
                float radius = Math.max(1f, blob.getRadius() * size);
                RGBColor rgb = blob.getColor().toRGB();
                float[] offsets = GradientColorBlob.FALLOFF_OFFSETS;
                Color[] colors = new Color[offsets.length];
                for (int i = 0; i < offsets.length; i++) {
                    int alpha = Math.round(rgb.getAlpha() * GradientColorBlob.falloff(offsets[i]));
                    colors[i] = new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha);
                }

                RadialGradientPaint paint = new RadialGradientPaint(
                    new Point2D.Float(blob.getCenterX() * width, blob.getCenterY() * height),
                    radius,
                    offsets,
                    colors
                );
                g2.setPaint(paint);
                g2.fillRect(0, 0, width, height);
            }
        }
        finally {
            g2.dispose();
        }
    }
}
