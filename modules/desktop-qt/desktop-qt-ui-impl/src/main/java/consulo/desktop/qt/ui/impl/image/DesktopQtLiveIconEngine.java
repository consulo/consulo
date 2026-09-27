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
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.gui.QIcon;
import io.qt.gui.QIconEngine;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

public class DesktopQtLiveIconEngine extends QIconEngine {
    private static final Logger LOG = Logger.getInstance(DesktopQtLiveIconEngine.class);

    public static QIcon toQIcon(@Nullable Image image, DesktopQtAnimationHost host) {
        return toQIcon(image, null, host);
    }

    public static QIcon toQIcon(@Nullable Image image, @Nullable Image hoverImage, DesktopQtAnimationHost host) {
        if (!(image instanceof DesktopQtImage qtImage)) {
            return new QIcon();
        }

        @Nullable DesktopQtImage qtHoverImage = hoverImage != image && hoverImage instanceof DesktopQtImage hover ? hover : null;

        if (qtImage.isLive() || qtHoverImage != null && qtHoverImage.isLive()) {
            return new QIcon(new DesktopQtLiveIconEngine(image, qtHoverImage == null ? null : hoverImage, host));
        }

        if (qtHoverImage == null) {
            return qtImage.toQIcon();
        }

        QIcon icon = new QIcon(qtImage.toQPixmap());
        icon.addPixmap(qtHoverImage.toQPixmap(), QIcon.Mode.Active);
        return icon;
    }

    private final Image myImage;
    private final @Nullable Image myHoverImage;
    private final @Nullable WeakReference<DesktopQtAnimationHost> myHost;

    public DesktopQtLiveIconEngine(Image image, @Nullable Image hoverImage, @Nullable DesktopQtAnimationHost host) {
        this(host == null ? null : new WeakReference<>(host), image, hoverImage);
    }

    private DesktopQtLiveIconEngine(@Nullable WeakReference<DesktopQtAnimationHost> host, Image image, @Nullable Image hoverImage) {
        myImage = image;
        myHoverImage = hoverImage;
        myHost = host;
    }

    private Image imageFor(QIcon.Mode mode) {
        Image hoverImage = myHoverImage;
        return mode == QIcon.Mode.Active && hoverImage != null ? hoverImage : myImage;
    }

    private @Nullable DesktopQtAnimationHost host() {
        WeakReference<DesktopQtAnimationHost> host = myHost;
        return host == null ? null : host.get();
    }

    @Override
    public void paint(QPainter painter, QRect rect, QIcon.Mode mode, QIcon.State state) {
        try {
            DesktopQtImage.paint(painter, rect, mode, imageFor(mode), host());
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    @Override
    public QSize actualSize(QSize size, QIcon.Mode mode, QIcon.State state) {
        Image image = imageFor(mode);
        return new QSize(Math.max(0, Math.min(size.width(), image.getWidth())),
            Math.max(0, Math.min(size.height(), image.getHeight())));
    }

    @Override
    public QPixmap pixmap(QSize size, QIcon.Mode mode, QIcon.State state) {
        return scaledPixmap(size, mode, state, 1);
    }

    @Override
    public QPixmap scaledPixmap(QSize size, QIcon.Mode mode, QIcon.State state, double scale) {
        QSize actualSize = actualSize(size, mode, state);

        double ratio = scale > 0 ? scale : 1;

        QPixmap pixmap = DesktopQtEmptyImageImpl.createPixmap(actualSize.width(), actualSize.height(), ratio);
        if (actualSize.isEmpty()) {
            return pixmap;
        }

        QPainter painter = new QPainter(pixmap);
        try {
            DesktopQtImage.paint(painter, new QRect(0, 0, actualSize.width(), actualSize.height()), mode, imageFor(mode), host());
        }
        catch (Throwable e) {
            LOG.error(e);
        }
        finally {
            painter.end();
        }

        return pixmap;
    }

    @Override
    public QIconEngine clone() {
        return new DesktopQtLiveIconEngine(myHost, myImage, myHoverImage);
    }
}
