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

import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QImage;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;
import io.qt.widgets.QGraphicsBlurEffect;
import io.qt.widgets.QGraphicsPixmapItem;
import io.qt.widgets.QGraphicsScene;

/**
 * A gaussian blur of an image - the drop shadows and the glassy backdrops are made of it.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
final class DesktopQtBlur {
    private DesktopQtBlur() {
    }

    static QImage blur(QImage image, double radius) {
        QGraphicsScene scene = new QGraphicsScene();
        try {
            QGraphicsPixmapItem item = scene.addPixmap(QPixmap.fromImage(image));
            QGraphicsBlurEffect effect = new QGraphicsBlurEffect();
            effect.setBlurHints(QGraphicsBlurEffect.BlurHint.QualityHint);
            effect.setBlurRadius(radius);
            item.setGraphicsEffect(effect);

            QImage blurred = new QImage(image.size(), QImage.Format.Format_ARGB32_Premultiplied);
            blurred.fill(Qt.GlobalColor.transparent);
            QPainter scenePainter = new QPainter(blurred);
            try {
                QRectF bounds = new QRectF(0, 0, blurred.width(), blurred.height());
                scene.render(scenePainter, bounds, bounds);
            }
            finally {
                scenePainter.end();
            }
            return blurred;
        }
        finally {
            scene.dispose();
        }
    }
}
