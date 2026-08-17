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
    public QPixmap toQPixmap() {
        int width = getWidth();
        int height = getHeight();

        QPixmap target = DesktopQtEmptyImageImpl.createPixmap(width, height);

        QPainter painter = new QPainter(target);
        try {
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            draw(painter, myLeft, 0, height);
            draw(painter, myRight, myLeft.getWidth(), height);
        }
        finally {
            painter.end();
        }

        return target;
    }

    private static void draw(QPainter painter, Image image, int x, int height) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        QPixmap pixmap = DesktopQtImage.toQPixmap(image);
        if (pixmap.isNull()) {
            return;
        }

        painter.drawPixmap(new QRect(x, (height - imageHeight) / 2, imageWidth, imageHeight), pixmap);
    }
}
