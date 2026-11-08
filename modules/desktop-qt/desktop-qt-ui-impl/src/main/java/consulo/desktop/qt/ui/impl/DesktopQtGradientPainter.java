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
package consulo.desktop.qt.ui.impl;

import consulo.ui.color.RGBColor;
import consulo.ui.internal.GradientColor;
import consulo.ui.internal.GradientColorBlob;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QImage;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QRadialGradient;
import io.qt.widgets.QWidget;

/**
 * Renders the gradient into an image of the widget size and fills the background with it - painting on the widget
 * itself is only allowed from its own paint event.
 *
 * @author VISTALL
 * @since 2026-11-07
 */
final class DesktopQtGradientPainter {
    private DesktopQtGradientPainter() {
    }

    static void apply(QWidget widget, GradientColor gradient) {
        int width = widget.width();
        int height = widget.height();
        if (width <= 0 || height <= 0) {
            return;
        }

        QImage image = new QImage(width, height, QImage.Format.Format_ARGB32_Premultiplied);
        image.fill(TargetQt.to(gradient.getBase()));

        QPainter painter = new QPainter(image);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing);

            int size = Math.max(width, height);
            for (GradientColorBlob blob : gradient.getBlobs()) {
                double radius = Math.max(1.0, blob.getRadius() * size);
                RGBColor rgb = blob.getColor().toRGB();

                QRadialGradient radial = new QRadialGradient(blob.getCenterX() * width, blob.getCenterY() * height, radius);
                for (float offset : GradientColorBlob.FALLOFF_OFFSETS) {
                    int alpha = Math.round(rgb.getAlpha() * GradientColorBlob.falloff(offset));
                    radial.setColorAt(offset, new QColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha));
                }

                painter.fillRect(0, 0, width, height, new QBrush(radial));
            }
        }
        finally {
            painter.end();
        }

        QPalette palette = new QPalette(widget.palette());
        palette.setBrush(QPalette.ColorRole.Window, new QBrush(image));
        widget.setPalette(palette);
        widget.setAutoFillBackground(true);
    }
}
