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

import io.qt.gui.QGuiApplication;
import io.qt.gui.QIcon;
import io.qt.gui.QPixmap;
import io.qt.gui.QScreen;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public interface DesktopQtImage {
    static QIcon toQIcon(consulo.ui.image.Image image) {
        return ((DesktopQtImage) image).toQIcon();
    }

    static QPixmap toQPixmap(consulo.ui.image.Image image) {
        return ((DesktopQtImage) image).toQPixmap();
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

    QPixmap toQPixmap();

    default QIcon toQIcon() {
        return new QIcon(toQPixmap());
    }
}
