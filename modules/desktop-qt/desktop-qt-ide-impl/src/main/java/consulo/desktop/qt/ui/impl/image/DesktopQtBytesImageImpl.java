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
import io.qt.core.QBuffer;
import io.qt.core.QByteArray;
import io.qt.core.QIODevice;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QImage;
import io.qt.gui.QImageReader;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

/**
 * An image handed over as bytes rather than named by an id - the icon of a plugin read out of its own jar is the
 * one the platform asks for by this route, and it resolves through no icon library at all.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtBytesImageImpl implements Image, DesktopQtImage {
    private static final String SVG_FORMAT = "svg";
    private static final String PNG_FORMAT = "png";

    private final byte[] myBytes;
    private final String myFormat;

    private final int myWidth;
    private final int myHeight;

    public DesktopQtBytesImageImpl(ImageType type, byte[] bytes) throws IOException {
        myBytes = bytes;
        myFormat = type == ImageType.SVG ? SVG_FORMAT : PNG_FORMAT;

        // a QPixmap may only be built on the gui thread, while a QImage may be read anywhere - and the size has
        // to be known right away, since the platform asks for it before anything is painted
        QImage image = load();
        if (image == null) {
            throw new IOException("Unable to read " + myFormat + " image of " + bytes.length + " bytes");
        }

        myWidth = image.width();
        myHeight = image.height();
    }

    public static DesktopQtBytesImageImpl fromUrl(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            ImageType type = url.toString().toLowerCase(Locale.ROOT).endsWith(".svg") ? ImageType.SVG : ImageType.PNG;

            return new DesktopQtBytesImageImpl(type, stream.readAllBytes());
        }
    }

    private @Nullable QImage load() {
        try {
            QImage image = new QImage();
            // svg rendering relies on the Qt 'svg' image format plugin, which may be absent in a stripped deployment
            if (!image.loadFromData(myBytes, myFormat) || image.isNull()) {
                return null;
            }
            return image;
        }
        catch (Throwable e) {
            return null;
        }
    }

    private @Nullable QImage load(int physicalWidth, int physicalHeight) {
        if (SVG_FORMAT.equals(myFormat)) {
            QImage rendered = renderSvg(physicalWidth, physicalHeight);
            if (rendered != null) {
                return rendered;
            }
        }

        QImage image = load();
        if (image == null) {
            return null;
        }

        if (image.width() == physicalWidth && image.height() == physicalHeight) {
            return image;
        }

        return image.scaled(physicalWidth,
            physicalHeight,
            Qt.AspectRatioMode.IgnoreAspectRatio,
            Qt.TransformationMode.SmoothTransformation);
    }

    private @Nullable QImage renderSvg(int physicalWidth, int physicalHeight) {
        try {
            QBuffer buffer = new QBuffer(new QByteArray(myBytes));
            if (!buffer.open(QIODevice.OpenModeFlag.ReadOnly)) {
                return null;
            }

            try {
                QImageReader reader = new QImageReader(buffer, new QByteArray(SVG_FORMAT));
                if (!reader.canRead()) {
                    return null;
                }

                reader.setScaledSize(new QSize(physicalWidth, physicalHeight));

                QImage image = reader.read();
                return image == null || image.isNull() ? null : image;
            }
            finally {
                buffer.close();
            }
        }
        catch (Throwable e) {
            return null;
        }
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
        double ratio = DesktopQtImage.devicePixelRatio();

        QImage image = load(DesktopQtImage.toPhysical(myWidth, ratio), DesktopQtImage.toPhysical(myHeight, ratio));
        if (image == null) {
            return DesktopQtEmptyImageImpl.createPixmap(myWidth, myHeight, ratio);
        }

        QPixmap pixmap = QPixmap.fromImage(image);
        if (pixmap.isNull()) {
            return DesktopQtEmptyImageImpl.createPixmap(myWidth, myHeight, ratio);
        }

        pixmap.setDevicePixelRatio(ratio);
        return pixmap;
    }
}
