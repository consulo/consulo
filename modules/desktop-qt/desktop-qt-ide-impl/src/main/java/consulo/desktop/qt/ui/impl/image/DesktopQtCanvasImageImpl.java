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

import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtCanvasImageImpl implements Image, DesktopQtImage {
    private static final Logger LOG = Logger.getInstance(DesktopQtCanvasImageImpl.class);

    private final int myWidth;
    private final int myHeight;
    private final Consumer<Canvas2D> myConsumer;

    public DesktopQtCanvasImageImpl(int width, int height, Consumer<Canvas2D> consumer) {
        myWidth = width;
        myHeight = height;
        myConsumer = consumer;
    }

    @Override
    public int getWidth() {
        return myWidth;
    }

    @Override
    public int getHeight() {
        return myHeight;
    }

    @Override
    public QPixmap toQPixmap() {
        QPixmap pixmap = DesktopQtEmptyImageImpl.createPixmap(myWidth, myHeight);

        // the pixmap carries a device pixel ratio, so the canvas keeps handing over logical coordinates
        QPainter painter = new QPainter(pixmap);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.TextAntialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            myConsumer.accept(new DesktopQtCanvas2DImpl(painter));
        }
        catch (Throwable e) {
            LOG.warn(e);
        }
        finally {
            painter.end();
        }

        return pixmap;
    }
}
