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

import consulo.ui.image.EmptyImage;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QPixmap;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEmptyImageImpl implements EmptyImage, DesktopQtImage {
    private final int myWidth;
    private final int myHeight;

    public DesktopQtEmptyImageImpl(int width, int height) {
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
        return createPixmap(getWidth(), getHeight());
    }

    public static QPixmap createPixmap(int width, int height) {
        return createPixmap(width, height, DesktopQtImage.devicePixelRatio());
    }

    public static QPixmap createPixmap(int width, int height, double devicePixelRatio) {
        QPixmap pixmap = new QPixmap(DesktopQtImage.toPhysical(width, devicePixelRatio),
            DesktopQtImage.toPhysical(height, devicePixelRatio));
        pixmap.setDevicePixelRatio(devicePixelRatio);
        pixmap.fill(new QColor(Qt.GlobalColor.transparent));
        return pixmap;
    }
}
