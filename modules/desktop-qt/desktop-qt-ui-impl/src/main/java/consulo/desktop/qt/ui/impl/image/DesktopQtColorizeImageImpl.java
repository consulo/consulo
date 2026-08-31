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

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.image.Image;
import io.qt.gui.QColor;
import io.qt.gui.QImage;
import io.qt.gui.QPixmap;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtColorizeImageImpl implements Image, DesktopQtImage {
    private final Image myOriginal;
    private final ColorValue myColorValue;

    public DesktopQtColorizeImageImpl(Image original, ColorValue colorValue) {
        myOriginal = original;
        myColorValue = colorValue;
    }

    public Image getOriginal() {
        return myOriginal;
    }

    public ColorValue getColorValue() {
        return myColorValue;
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

        RGBColor rgb = myColorValue.toRGB();

        QColor base = new QColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue()).toHsv();

        // an achromatic target has no hue at all, and qt answers -1 for it - which cannot be handed back
        float baseHue = Math.max(base.hueF(), 0f);
        float baseSaturation = base.saturationF();
        float baseValue = base.valueF();

        // mirrors the awt effect: hue and saturation come from the target color, the brightness of the source
        // is kept as a factor, transparent pixels stay transparent
        QImage image = source.toImage().convertedTo(QImage.Format.Format_ARGB32);

        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                int argb = image.pixel(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                QColor pixel = new QColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF).toHsv();

                QColor colored = QColor.fromHsvF(baseHue, baseSaturation, Math.clamp(baseValue * pixel.valueF(), 0f, 1f));

                image.setPixel(x, y, (alpha << 24) | (colored.rgb() & 0xFFFFFF));
            }
        }

        QPixmap pixmap = QPixmap.fromImage(image);
        pixmap.setDevicePixelRatio(ratio);
        return pixmap;
    }
}
