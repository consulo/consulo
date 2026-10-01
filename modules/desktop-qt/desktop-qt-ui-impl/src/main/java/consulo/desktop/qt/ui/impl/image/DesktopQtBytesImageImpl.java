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

import consulo.ui.ex.awt.internal.image.ImageIODecoder;
import consulo.ui.image.Image;
import io.qt.core.QBuffer;
import io.qt.core.QByteArray;
import io.qt.core.QIODevice;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QImage;
import io.qt.gui.QImageReader;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
    /**
     * The format qt reads the bytes as, null to have it look at the bytes for which they are.
     */
    private final @Nullable String myFormat;

    private final int myWidth;
    private final int myHeight;

    private DesktopQtBytesImageImpl(byte[] bytes, @Nullable String format, QImage image) {
        myBytes = bytes;
        myFormat = format;
        myWidth = image.width();
        myHeight = image.height();
    }

    /**
     * Qt reads the bytes as the format the type names, and then as whatever it finds them to be. When it has no
     * reader for them at all - a format of a plugin of the platform, or of an image format plugin of qt which is not
     * installed - they are decoded on the jvm side and handed over as png.
     */
    public static DesktopQtBytesImageImpl create(ImageType type, byte[] bytes) throws IOException {
        // a QPixmap may only be built on the gui thread, while a QImage may be read anywhere - and the size has
        // to be known right away, since the platform asks for it before anything is painted
        if (type.vector()) {
            // svg rendering relies on the Qt 'svg' image format plugin, which may be absent in a stripped deployment
            QImage image = load(bytes, SVG_FORMAT);
            if (image == null) {
                throw new IOException("Unable to read svg image of " + bytes.length + " bytes");
            }
            return new DesktopQtBytesImageImpl(bytes, SVG_FORMAT, image);
        }

        QImage image = load(bytes, type.id());
        if (image != null) {
            return new DesktopQtBytesImageImpl(bytes, type.id(), image);
        }

        image = load(bytes, null);
        if (image != null) {
            return new DesktopQtBytesImageImpl(bytes, null, image);
        }

        byte[] png = ImageIODecoder.toPng(bytes);
        if (png != null) {
            image = load(png, PNG_FORMAT);
            if (image != null) {
                return new DesktopQtBytesImageImpl(png, PNG_FORMAT, image);
            }
        }

        throw new IOException("Unable to read " + type.id() + " image of " + bytes.length + " bytes");
    }

    public static DesktopQtBytesImageImpl fromUrl(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return create(ImageType.fromFileName(url.getPath()), stream.readAllBytes());
        }
    }

    public boolean isVector() {
        return SVG_FORMAT.equals(myFormat);
    }

    /**
     * Renders only a part of a svg drawn at another size - the visible part of a zoomed one, which as a whole would
     * not even fit into memory at a high zoom.
     *
     * @param scaledWidth  width of the whole image as drawn, in physical pixels
     * @param scaledHeight height of the whole image as drawn, in physical pixels
     * @param region       part of the drawn image to render, in physical pixels
     * @return null when this is no svg or it cannot be rendered
     */
    public @Nullable QImage renderVectorRegion(int scaledWidth, int scaledHeight, QRect region) {
        if (!isVector() || scaledWidth <= 0 || scaledHeight <= 0 || region.isEmpty()) {
            return null;
        }

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

                reader.setScaledSize(new QSize(scaledWidth, scaledHeight));
                reader.setScaledClipRect(region);

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

    private static @Nullable QImage load(byte[] bytes, @Nullable String format) {
        try {
            QImage image = new QImage();
            boolean loaded = format == null ? image.loadFromData(bytes) : image.loadFromData(bytes, format);
            if (!loaded || image.isNull()) {
                return null;
            }
            return image;
        }
        catch (Throwable e) {
            return null;
        }
    }

    private @Nullable QImage load(int physicalWidth, int physicalHeight) {
        if (isVector()) {
            QImage rendered = renderSvg(physicalWidth, physicalHeight);
            if (rendered != null) {
                return rendered;
            }
        }

        QImage image = load(myBytes, myFormat);
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
