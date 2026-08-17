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
import io.qt.core.QBuffer;
import io.qt.core.QByteArray;
import io.qt.core.QIODevice;
import io.qt.core.QSize;
import io.qt.gui.QImage;
import io.qt.gui.QImageReader;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtSvgLibraryImageImpl implements DesktopQtImageReference {
    private static final Logger LOG = Logger.getInstance(DesktopQtSvgLibraryImageImpl.class);

    private static final String FORMAT = "svg";

    private static final int ourCacheSize = 512;

    /**
     * Rasterized icons, held against the bytes they were drawn from rather than against the image which asked for
     * them: a theme change hands every icon owner in the ide a new pixmap at once, and rasterizing each svg again
     * on every change - and again on the way back - is what makes picking a theme take a visible while. The bytes
     * of a library are what a theme swaps, so an entry cannot outlive the library it belongs to.
     */
    private static final Map<CacheKey, QPixmap> ourCache = Collections.synchronizedMap(
        new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, QPixmap> eldest) {
                return size() > ourCacheSize;
            }
        }
    );

    private record CacheKey(byte[] data, int width, int height, double ratio) {
    }

    private final byte[] myX1Data;
    private final byte @Nullable [] myX2Data;

    public DesktopQtSvgLibraryImageImpl(byte[] x1Data, byte @Nullable [] x2Data) {
        myX1Data = x1Data;
        myX2Data = x2Data;
    }

    @Override
    public QPixmap toQPixmap(int width, int height) {
        double ratio = DesktopQtImage.devicePixelRatio();

        byte[] data = ratio > 1 && myX2Data != null ? myX2Data : myX1Data;

        int physicalWidth = DesktopQtImage.toPhysical(width, ratio);
        int physicalHeight = DesktopQtImage.toPhysical(height, ratio);

        CacheKey key = new CacheKey(data, physicalWidth, physicalHeight, ratio);

        QPixmap cached = ourCache.get(key);
        // a pixmap of qt outlives the java object standing for it only as long as nothing disposed it, and one
        // handed to a widget which has since been torn down is of no use to the next asker
        if (cached != null && !cached.isDisposed()) {
            return cached;
        }

        // the svg is rasterized straight at the physical size, so it stays sharp at any fractional scale instead
        // of being rendered once at its own size and stretched afterwards
        QImage image = render(data, physicalWidth, physicalHeight);
        if (image == null) {
            return DesktopQtEmptyImageImpl.createPixmap(width, height, ratio);
        }

        QPixmap pixmap = QPixmap.fromImage(image);
        if (pixmap.isNull()) {
            return DesktopQtEmptyImageImpl.createPixmap(width, height, ratio);
        }

        pixmap.setDevicePixelRatio(ratio);

        ourCache.put(key, pixmap);

        return pixmap;
    }

    private static @Nullable QImage render(byte[] data, int width, int height) {
        try {
            QBuffer buffer = new QBuffer(new QByteArray(data));
            if (!buffer.open(QIODevice.OpenModeFlag.ReadOnly)) {
                return null;
            }

            try {
                QImageReader reader = new QImageReader(buffer, new QByteArray(FORMAT));
                if (!reader.canRead()) {
                    return null;
                }

                reader.setScaledSize(new QSize(width, height));

                QImage image = reader.read();
                return image == null || image.isNull() ? null : image;
            }
            finally {
                buffer.close();
            }
        }
        catch (Throwable e) {
            LOG.warn(e);
            return null;
        }
    }
}
