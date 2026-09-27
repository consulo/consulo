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
import io.qt.gui.QIcon;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtAppendImageImpl implements Image, DesktopQtImage {
    private final Image myLeft;
    private final Image myRight;

    public DesktopQtAppendImageImpl(Image left, Image right) {
        myLeft = left;
        myRight = right;
    }

    public Image getLeft() {
        return myLeft;
    }

    public Image getRight() {
        return myRight;
    }

    @Override
    public int getHeight() {
        return Math.max(myLeft.getHeight(), myRight.getHeight());
    }

    @Override
    public int getWidth() {
        return myLeft.getWidth() + myRight.getWidth();
    }

    @Override
    public boolean isLive() {
        return DesktopQtImage.isLive(myLeft) || DesktopQtImage.isLive(myRight);
    }

    @Override
    public long paintFrame(QPainter painter, QRect rect, QIcon.Mode mode) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0 || rect.isEmpty()) {
            return 0;
        }

        double scaleX = rect.width() / (double) width;
        double scaleY = rect.height() / (double) height;

        long left = paintPart(painter, partRect(rect, myLeft, 0, height, scaleX, scaleY), mode, myLeft);
        long right = paintPart(painter, partRect(rect, myRight, myLeft.getWidth(), height, scaleX, scaleY), mode, myRight);

        return DesktopQtImage.nextFrame(left, right);
    }

    private static long paintPart(QPainter painter, QRect part, QIcon.Mode mode, Image image) {
        return part.isEmpty() ? 0 : DesktopQtImage.paintFrame(image, painter, part, mode);
    }

    @Override
    public QPixmap toQPixmap() {
        int width = getWidth();
        int height = getHeight();

        QPixmap target = DesktopQtEmptyImageImpl.createPixmap(width, height);

        QRect bounds = new QRect(0, 0, width, height);

        QPainter painter = new QPainter(target);
        try {
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            draw(painter, partRect(bounds, myLeft, 0, height, 1, 1), myLeft);
            draw(painter, partRect(bounds, myRight, myLeft.getWidth(), height, 1, 1), myRight);
        }
        finally {
            painter.end();
        }

        return target;
    }

    private static QRect partRect(QRect bounds, Image image, int x, int height, double scaleX, double scaleY) {
        int top = (height - image.getHeight()) / 2;

        int left = bounds.x() + (int) Math.round(x * scaleX);
        int right = bounds.x() + (int) Math.round((x + image.getWidth()) * scaleX);
        int upper = bounds.y() + (int) Math.round(top * scaleY);
        int lower = bounds.y() + (int) Math.round((top + image.getHeight()) * scaleY);

        return new QRect(left, upper, right - left, lower - upper);
    }

    private static void draw(QPainter painter, QRect part, Image image) {
        QPixmap pixmap = DesktopQtImage.toQPixmap(image);
        if (pixmap.isNull()) {
            return;
        }

        painter.drawPixmap(part, pixmap);
    }
}
