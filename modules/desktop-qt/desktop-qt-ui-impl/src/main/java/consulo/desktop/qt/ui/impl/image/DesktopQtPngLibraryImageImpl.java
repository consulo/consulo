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
import io.qt.core.Qt;
import io.qt.gui.QImage;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtPngLibraryImageImpl implements DesktopQtImageReference {
    private static final Logger LOG = Logger.getInstance(DesktopQtPngLibraryImageImpl.class);

    private static final String FORMAT = "png";

    /**
     * A raster variant is only decoded once it is asked for, since an icon library holds far more entries than
     * any one run of the ide ever paints.
     */
    private static class ImageBytes {
        private static @Nullable ImageBytes of(byte @Nullable [] data) {
            return data == null ? null : new ImageBytes(data);
        }

        private volatile byte @Nullable [] myBytes;
        private volatile @Nullable QImage myImage;

        private ImageBytes(byte[] bytes) {
            myBytes = bytes;
        }

        private synchronized @Nullable QImage getOrLoad() {
            QImage image = myImage;
            if (image != null) {
                return image;
            }

            byte[] bytes = myBytes;
            if (bytes == null) {
                return null;
            }

            try {
                QImage loaded = new QImage();
                if (loaded.loadFromData(bytes, FORMAT) && !loaded.isNull()) {
                    myImage = loaded;
                    myBytes = null;
                    return loaded;
                }
            }
            catch (Throwable e) {
                LOG.warn(e);
            }

            myBytes = null;
            return null;
        }
    }

    private final ImageBytes myX1Data;
    private final @Nullable ImageBytes myX2Data;

    public DesktopQtPngLibraryImageImpl(byte[] x1Data, byte @Nullable [] x2Data) {
        myX1Data = new ImageBytes(x1Data);
        myX2Data = ImageBytes.of(x2Data);
    }

    @Override
    public QPixmap toQPixmap(int width, int height) {
        double ratio = DesktopQtImage.devicePixelRatio();

        ImageBytes target = ratio > 1 && myX2Data != null ? myX2Data : myX1Data;

        QImage image = target.getOrLoad();
        if (image == null && target != myX1Data) {
            image = myX1Data.getOrLoad();
        }

        if (image == null) {
            return DesktopQtEmptyImageImpl.createPixmap(width, height, ratio);
        }

        int physicalWidth = DesktopQtImage.toPhysical(width, ratio);
        int physicalHeight = DesktopQtImage.toPhysical(height, ratio);

        if (image.width() != physicalWidth || image.height() != physicalHeight) {
            image = image.scaled(physicalWidth,
                physicalHeight,
                Qt.AspectRatioMode.IgnoreAspectRatio,
                Qt.TransformationMode.SmoothTransformation);
        }

        QPixmap pixmap = QPixmap.fromImage(image);
        if (pixmap.isNull()) {
            return DesktopQtEmptyImageImpl.createPixmap(width, height, ratio);
        }

        pixmap.setDevicePixelRatio(ratio);
        return pixmap;
    }
}
