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
public class DesktopQtTransparentImageImpl implements Image, DesktopQtImage {
    private final Image myOriginal;
    private final float myAlpha;

    public DesktopQtTransparentImageImpl(Image original, float alpha) {
        myOriginal = original;
        myAlpha = alpha;
    }

    @Override
    public int getHeight() {
        return myOriginal.getHeight();
    }

    @Override
    public int getWidth() {
        return myOriginal.getWidth();
    }

    @Override
    public QPixmap toQPixmap() {
        int width = getWidth();
        int height = getHeight();

        QPixmap target = DesktopQtEmptyImageImpl.createPixmap(width, height);

        QPixmap source = DesktopQtImage.toQPixmap(myOriginal);
        if (source.isNull()) {
            return target;
        }

        QPainter painter = new QPainter(target);
        try {
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);
            painter.setOpacity(myAlpha);

            painter.drawPixmap(new QRect(0, 0, width, height), source);
        }
        finally {
            painter.end();
        }

        return target;
    }
}
