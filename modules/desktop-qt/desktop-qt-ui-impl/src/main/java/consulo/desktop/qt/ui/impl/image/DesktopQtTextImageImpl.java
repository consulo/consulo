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
package consulo.desktop.qt.ui.impl.image;

import consulo.ui.image.Image;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QFont;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPen;
import io.qt.gui.QPixmap;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTextImageImpl implements Image, DesktopQtImage {
    private static final int TEXT_FONT_SIZE = 6;

    private final Image myBaseImage;
    private final String myText;

    public DesktopQtTextImageImpl(Image baseImage, String text) {
        myBaseImage = baseImage;
        myText = text;
    }

    public Image getBaseImage() {
        return myBaseImage;
    }

    public String getText() {
        return myText;
    }

    @Override
    public int getHeight() {
        return myBaseImage.getHeight();
    }

    @Override
    public int getWidth() {
        return myBaseImage.getWidth();
    }

    @Override
    public QPixmap toQPixmap() {
        int width = getWidth();
        int height = getHeight();

        QPixmap target = DesktopQtEmptyImageImpl.createPixmap(width, height);

        QPainter painter = new QPainter(target);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.TextAntialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            QPixmap base = DesktopQtImage.toQPixmap(myBaseImage);
            if (!base.isNull()) {
                painter.drawPixmap(new QRect(0, 0, width, height), base);
            }

            QFont font = new QFont();
            font.setPixelSize(TEXT_FONT_SIZE);

            QFontMetrics metrics = new QFontMetrics(font);

            QPainterPath path = new QPainterPath();
            path.addText(width - metrics.horizontalAdvance(myText), height - metrics.descent(), font, myText);

            // the awt effect draws the badge over a halo of the background colour, so the stroke goes down first
            QPen pen = new QPen(new QColor(Qt.GlobalColor.white));
            pen.setWidthF(1);
            pen.setJoinStyle(Qt.PenJoinStyle.RoundJoin);

            painter.setPen(pen);
            painter.setBrush(new QBrush(new QColor(Qt.GlobalColor.black)));

            painter.drawPath(path);
        }
        finally {
            painter.end();
        }

        return target;
    }
}
