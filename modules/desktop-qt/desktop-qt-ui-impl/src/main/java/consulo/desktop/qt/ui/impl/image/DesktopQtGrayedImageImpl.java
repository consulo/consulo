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
import consulo.ui.style.StyleManager;
import io.qt.gui.QImage;
import io.qt.gui.QPixmap;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtGrayedImageImpl implements Image, DesktopQtImage {
    /**
     * The percentages the awt frontend hands to its gray filter - how far a pixel is pulled back towards
     * white once its luminance is spent, which a dark theme needs much less of.
     */
    private static final int LIGHT_PERCENT = 65;
    private static final int DARK_PERCENT = 30;

    private final Image myOriginal;

    public DesktopQtGrayedImageImpl(Image original) {
        myOriginal = original;
    }

    public Image getOriginal() {
        return myOriginal;
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
        QPixmap source = DesktopQtImage.toQPixmap(myOriginal);
        if (source.isNull()) {
            return DesktopQtEmptyImageImpl.createPixmap(getWidth(), getHeight());
        }

        double ratio = source.devicePixelRatio();

        int percent = StyleManager.get().getCurrentStyle().isDark() ? DARK_PERCENT : LIGHT_PERCENT;

        float rest = (100 - percent) / 100f;
        float scale = rest / 3f;
        float offset = 1 - rest;

        // mirrors the awt gray filter: the luminance of a pixel is spent down to a third and then pulled back
        // towards white by the percentage asked for, which leaves the shape of an icon and none of its colour
        QImage image = source.toImage().convertedTo(QImage.Format.Format_ARGB32);

        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                int argb = image.pixel(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                double luminance = 0.30 * ((argb >> 16) & 0xFF) + 0.59 * ((argb >> 8) & 0xFF) + 0.11 * (argb & 0xFF);
                int gray = Math.clamp(Math.round(offset * 255 + scale * luminance), 0, 255);

                image.setPixel(x, y, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }

        QPixmap pixmap = QPixmap.fromImage(image);
        pixmap.setDevicePixelRatio(ratio);
        return pixmap;
    }
}
