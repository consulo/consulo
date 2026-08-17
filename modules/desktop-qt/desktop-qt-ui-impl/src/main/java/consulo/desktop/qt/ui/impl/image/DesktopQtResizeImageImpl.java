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
import consulo.ui.image.ImageKey;
import io.qt.core.QRect;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtResizeImageImpl implements Image, DesktopQtImage {
    private final Image myOriginal;
    private final int myWidth;
    private final int myHeight;

    public DesktopQtResizeImageImpl(Image original, int width, int height) {
        myOriginal = original;
        myWidth = width;
        myHeight = height;
    }

    @Override
    public int getHeight() {
        return myHeight;
    }

    @Override
    public int getWidth() {
        return myWidth;
    }

    @Override
    public QPixmap toQPixmap() {
        // an icon named by an id can be rasterized straight at the asked for size, which beats stretching the
        // pixmap the original size produced
        if (myOriginal instanceof ImageKey imageKey) {
            return new DesktopQtImageKeyImpl(imageKey.getGroupId(), imageKey.getImageId(), myWidth, myHeight).toQPixmap();
        }

        QPixmap target = DesktopQtEmptyImageImpl.createPixmap(myWidth, myHeight);

        QPixmap source = DesktopQtImage.toQPixmap(myOriginal);
        if (source.isNull()) {
            return target;
        }

        QPainter painter = new QPainter(target);
        try {
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            painter.drawPixmap(new QRect(0, 0, myWidth, myHeight), source);
        }
        finally {
            painter.end();
        }

        return target;
    }
}
