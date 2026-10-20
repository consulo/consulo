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

import io.qt.core.QMargins;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QImage;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;

/**
 * The drop shadow a popup with an arrow draws around its own outline - the one of the window manager is a box and
 * knows nothing of the arrow. Two layers, a wide soft one and a tight one, the same as the other desktop frontend.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
final class DesktopQtPopupShadow {
    private static final int[] OFFSETS = {3, 1};
    private static final int[] BLURS = {10, 2};
    private static final float[] ALPHAS = {0.30f, 0.20f};

    private DesktopQtPopupShadow() {
    }

    static QMargins insets() {
        int top = 0;
        int side = 0;
        int bottom = 0;
        for (int i = 0; i < OFFSETS.length; i++) {
            top = Math.max(top, BLURS[i] - OFFSETS[i]);
            bottom = Math.max(bottom, BLURS[i] + OFFSETS[i]);
            side = Math.max(side, BLURS[i]);
        }
        return new QMargins(side, top, side, bottom);
    }

    static QImage render(QPainterPath outline, int width, int height) {
        QImage result = new QImage(Math.max(width, 1), Math.max(height, 1), QImage.Format.Format_ARGB32_Premultiplied);
        result.fill(Qt.GlobalColor.transparent);
        QPainter painter = new QPainter(result);
        try {
            for (int i = 0; i < OFFSETS.length; i++) {
                int pad = BLURS[i] * 2;
                QImage mask = new QImage(width + 2 * pad, height + 2 * pad, QImage.Format.Format_ARGB32_Premultiplied);
                mask.fill(Qt.GlobalColor.transparent);
                QPainter maskPainter = new QPainter(mask);
                try {
                    maskPainter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                    maskPainter.translate(pad, pad);
                    maskPainter.fillPath(outline, new QBrush(new QColor(0, 0, 0, Math.round(ALPHAS[i] * 255))));
                }
                finally {
                    maskPainter.end();
                }
                painter.drawImage(-pad, OFFSETS[i] - pad, DesktopQtBlur.blur(mask, BLURS[i] * 2));
            }
        }
        finally {
            painter.end();
        }
        return result;
    }
}
