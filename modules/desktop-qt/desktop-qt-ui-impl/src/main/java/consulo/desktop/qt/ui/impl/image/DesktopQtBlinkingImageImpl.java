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

import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import io.qt.core.QRect;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QIcon;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;
import io.qt.gui.QStyleHints;
import io.qt.widgets.QApplication;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOption;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public final class DesktopQtBlinkingImageImpl implements Image, DesktopQtImage {
    private record OffFrame(int width, int height, double ratio, long modificationCount, QPixmap pixmap) {
    }

    private final Image myOriginal;

    private volatile @Nullable OffFrame myOffFrame;

    public DesktopQtBlinkingImageImpl(Image original) {
        myOriginal = original;
    }

    public Image getOriginal() {
        return myOriginal;
    }

    @Override
    public int getWidth() {
        return myOriginal.getWidth();
    }

    @Override
    public int getHeight() {
        return myOriginal.getHeight();
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public long paintFrame(QPainter painter, QRect rect, QIcon.Mode mode) {
        if (mode == QIcon.Mode.Disabled) {
            return DesktopQtImage.paintFrame(myOriginal, painter, rect, mode);
        }

        int halfPeriod = halfPeriod();
        if (halfPeriod <= 0) {
            return DesktopQtImage.paintFrame(myOriginal, painter, rect, mode);
        }

        long period = 2L * halfPeriod;
        long phase = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) % period;

        if (phase < halfPeriod) {
            return DesktopQtImage.nextFrame(halfPeriod - phase, DesktopQtImage.paintFrame(myOriginal, painter, rect, mode));
        }

        long toOn = period - phase;

        if (DesktopQtImage.isLive(myOriginal)) {
            return DesktopQtImage.nextFrame(toOn, DesktopQtImage.paintFrame(myOriginal, painter, rect, QIcon.Mode.Disabled));
        }

        if (!rect.isEmpty()) {
            QPixmap off = offFrame(rect.width(), rect.height());
            if (!off.isNull()) {
                painter.drawPixmap(rect, off);
            }
        }
        return toOn;
    }

    private static int halfPeriod() {
        QStyleHints styleHints = QGuiApplication.styleHints();
        return styleHints == null ? 0 : styleHints.cursorFlashTime();
    }

    private QPixmap offFrame(int width, int height) {
        double ratio = DesktopQtImage.devicePixelRatio();
        long modificationCount = IconLibraryManager.get().getModificationCount();

        OffFrame cached = myOffFrame;
        if (cached != null
            && cached.width() == width
            && cached.height() == height
            && cached.ratio() == ratio
            && cached.modificationCount() == modificationCount
            && !cached.pixmap().isDisposed()) {
            return cached.pixmap();
        }

        QPixmap source = DesktopQtEmptyImageImpl.createPixmap(width, height, ratio);

        QPainter painter = new QPainter(source);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            DesktopQtImage.paintFrame(myOriginal, painter, new QRect(0, 0, width, height), QIcon.Mode.Normal);
        }
        finally {
            painter.end();
        }

        QPixmap pixmap = toDisabled(source);

        myOffFrame = new OffFrame(width, height, ratio, modificationCount, pixmap);

        return pixmap;
    }

    private static QPixmap toDisabled(QPixmap source) {
        QStyle style = QApplication.style();
        if (style == null) {
            return DesktopQtGrayedImageImpl.toGrayPixmap(source);
        }

        QStyleOption option = new QStyleOption();
        option.setPalette(QGuiApplication.palette());

        QPixmap pixmap = style.generatedIconPixmap(QIcon.Mode.Disabled, source, option);
        return pixmap == null || pixmap.isNull() ? DesktopQtGrayedImageImpl.toGrayPixmap(source) : pixmap;
    }

    @Override
    public QPixmap toQPixmap() {
        return DesktopQtImage.toQPixmap(myOriginal);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof DesktopQtBlinkingImageImpl other && myOriginal.equals(other.myOriginal);
    }

    @Override
    public int hashCode() {
        return myOriginal.hashCode() * 31 + 1;
    }
}
