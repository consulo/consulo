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
import io.qt.gui.QGuiApplication;
import io.qt.gui.QIcon;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;
import io.qt.gui.QScreen;
import io.qt.gui.QTransform;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public interface DesktopQtImage {
    static QIcon toQIcon(@Nullable Image image) {
        return image instanceof DesktopQtImage qtImage ? qtImage.toQIcon() : new QIcon();
    }

    static QPixmap toQPixmap(Image image) {
        if (image instanceof DesktopQtImage qtImage) {
            return qtImage.toQPixmap();
        }
        return DesktopQtEmptyImageImpl.createPixmap(image.getWidth(), image.getHeight());
    }

    /**
     * Every pixmap of this family is built at physical size and tagged with this ratio, so qt blits it one to one
     * rather than stretching a logical sized pixmap over the screen it is shown on.
     */
    static double devicePixelRatio() {
        double ratio = 0;

        try {
            QScreen screen = QGuiApplication.primaryScreen();
            if (screen != null) {
                ratio = screen.devicePixelRatio();
            }
        }
        catch (Throwable ignored) {
        }

        return ratio > 0 ? ratio : 1;
    }

    static int toPhysical(int logicalSize, double devicePixelRatio) {
        return Math.max(1, (int) Math.round(Math.max(logicalSize, 0) * devicePixelRatio));
    }

    static boolean isLive(@Nullable Image image) {
        return image instanceof DesktopQtImage qtImage && qtImage.isLive();
    }

    static long paintFrame(Image image, QPainter painter, QRect rect, QIcon.Mode mode) {
        if (image instanceof DesktopQtImage qtImage) {
            return qtImage.paintFrame(painter, rect, mode);
        }
        return 0;
    }

    static long nextFrame(long first, long second) {
        if (first <= 0) {
            return Math.max(second, 0);
        }
        if (second <= 0) {
            return first;
        }
        return Math.min(first, second);
    }

    static void paint(QPainter painter, QRect rect, Image image) {
        paint(painter, rect, QIcon.Mode.Normal, image, null);
    }

    static void paint(QPainter painter, QRect rect, QIcon.Mode mode, Image image, @Nullable DesktopQtAnimationHost host) {
        if (!(image instanceof DesktopQtImage qtImage) || !painter.isActive()) {
            return;
        }

        QRect target = fit(rect, image.getWidth(), image.getHeight());
        if (target.isEmpty()) {
            return;
        }

        long delay;
        painter.save();
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            delay = qtImage.paintFrame(painter, target, mode);
        }
        finally {
            painter.restore();
        }

        if (delay > 0) {
            requestNextFrame(painter, target, delay, host);
        }
    }

    private static QRect fit(QRect rect, int width, int height) {
        if (width <= 0 || height <= 0 || rect.width() <= 0 || rect.height() <= 0) {
            return new QRect();
        }

        double scale = Math.min(1, Math.min(rect.width() / (double) width, rect.height() / (double) height));

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        return new QRect(rect.x() + (rect.width() - targetWidth) / 2,
            rect.y() + (rect.height() - targetHeight) / 2,
            targetWidth,
            targetHeight);
    }

    private static void requestNextFrame(QPainter painter, QRect target, long delay, @Nullable DesktopQtAnimationHost host) {
        if (painter.device() instanceof QWidget widget) {
            QTransform transform = painter.worldTransform();

            QRect area = transform.isRotating() ? widget.rect() : transform.mapRect(target).adjusted(-1, -1, 1, 1);

            DesktopQtAnimationTicker.request(widget, area, delay);
        }
        else if (host != null) {
            DesktopQtAnimationTicker.request(host, delay);
        }
    }

    QPixmap toQPixmap();

    default boolean isLive() {
        return false;
    }

    default long paintFrame(QPainter painter, QRect rect, QIcon.Mode mode) {
        if (rect.isEmpty()) {
            return 0;
        }

        QPixmap pixmap = mode == QIcon.Mode.Disabled ? DesktopQtDisabledPixmapCache.get(this) : toQPixmap();
        if (!pixmap.isNull()) {
            painter.drawPixmap(rect, pixmap);
        }
        return 0;
    }

    default QIcon toQIcon() {
        if (isLive() && this instanceof Image image) {
            return new QIcon(new DesktopQtLiveIconEngine(image, null, null));
        }
        return new QIcon(toQPixmap());
    }
}
