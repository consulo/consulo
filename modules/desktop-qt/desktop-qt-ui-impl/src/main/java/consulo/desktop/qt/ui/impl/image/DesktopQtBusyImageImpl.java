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

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import io.qt.core.QRect;
import io.qt.gui.QColor;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QIcon;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPixmap;
import io.qt.widgets.QWidget;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DesktopQtBusyImageImpl implements Image, DesktopQtImage {
    private record Size(int width, int height) {
    }

    private static final ConcurrentMap<Size, DesktopQtBusyImageImpl> ourImages = new ConcurrentHashMap<>();

    public static DesktopQtBusyImageImpl of(int width, int height) {
        return ourImages.computeIfAbsent(new Size(Math.max(width, 0), Math.max(height, 0)),
            size -> new DesktopQtBusyImageImpl(size.width(), size.height()));
    }

    private final int myWidth;
    private final int myHeight;

    private DesktopQtBusyImageImpl(int width, int height) {
        myWidth = width;
        myHeight = height;
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
    public boolean isLive() {
        return true;
    }

    @Override
    public long paintFrame(QPainter painter, QRect rect, QIcon.Mode mode) {
        if (rect.isEmpty() || myWidth <= 0 || myHeight <= 0) {
            return 0;
        }

        DesktopQtSpinnerPainter.paintBusy(painter, rect, color(painter, mode));

        return DesktopQtSpinnerPainter.millisToNextStep();
    }

    private static QColor color(QPainter painter, QIcon.Mode mode) {
        QPalette palette = painter.device() instanceof QWidget widget ? widget.palette() : QGuiApplication.palette();

        return switch (mode) {
            case Disabled -> palette.color(QPalette.ColorGroup.Disabled, QPalette.ColorRole.WindowText);
            case Selected -> palette.color(QPalette.ColorGroup.Active, QPalette.ColorRole.HighlightedText);
            default -> palette.color(QPalette.ColorGroup.Active, QPalette.ColorRole.WindowText);
        };
    }

    @Override
    public QPixmap toQPixmap() {
        int side = Math.min(myWidth, myHeight);
        if (side <= 0) {
            return DesktopQtEmptyImageImpl.createPixmap(myWidth, myHeight);
        }

        ImageKey passive = PlatformIconGroup.processStep_passive();

        QPixmap still = new DesktopQtImageKeyImpl(passive.getGroupId(), passive.getImageId(), side, side).toQPixmap();
        if (myWidth == myHeight) {
            return still;
        }

        QPixmap target = DesktopQtEmptyImageImpl.createPixmap(myWidth, myHeight);
        if (still.isNull()) {
            return target;
        }

        QPainter painter = new QPainter(target);
        try {
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            painter.drawPixmap(new QRect((myWidth - side) / 2, (myHeight - side) / 2, side, side), still);
        }
        finally {
            painter.end();
        }

        return target;
    }
}
