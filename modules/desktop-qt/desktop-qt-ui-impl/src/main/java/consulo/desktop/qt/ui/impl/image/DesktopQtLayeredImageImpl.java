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
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtLayeredImageImpl implements Image, DesktopQtImage {
    private final Image[] myImages;

    public DesktopQtLayeredImageImpl(Image... images) {
        myImages = images;
    }

    @Override
    public int getHeight() {
        return myImages[0].getHeight();
    }

    @Override
    public int getWidth() {
        return myImages[0].getWidth();
    }

    @Override
    public boolean isLive() {
        for (Image image : myImages) {
            if (DesktopQtImage.isLive(image)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long paintFrame(QPainter painter, QRect rect, QIcon.Mode mode) {
        long next = 0;
        for (Image image : myImages) {
            next = DesktopQtImage.nextFrame(next, DesktopQtImage.paintFrame(image, painter, rect, mode));
        }
        return next;
    }

    @Override
    public QPixmap toQPixmap() {
        int width = getWidth();
        int height = getHeight();

        QPixmap target = DesktopQtEmptyImageImpl.createPixmap(width, height);

        // the target carries a device pixel ratio, so the painter already works in logical units here
        QPainter painter = new QPainter(target);
        try {
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            for (Image image : myImages) {
                QPixmap pixmap = DesktopQtImage.toQPixmap(image);
                if (pixmap.isNull()) {
                    continue;
                }

                painter.drawPixmap(new QRect(0, 0, width, height), pixmap);
            }
        }
        finally {
            painter.end();
        }

        return target;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof DesktopQtLayeredImageImpl other && Arrays.equals(myImages, other.myImages);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(myImages);
    }
}
