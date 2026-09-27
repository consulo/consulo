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
import io.qt.gui.QPixmap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

final class DesktopQtDisabledPixmapCache {
    private static final int CACHE_SIZE = 256;

    private record CacheKey(DesktopQtImage image, int width, int height, double ratio, long modificationCount, boolean dark) {
    }

    private static final Map<CacheKey, QPixmap> ourCache = Collections.synchronizedMap(
        new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, QPixmap> eldest) {
                return size() > CACHE_SIZE;
            }
        }
    );

    private DesktopQtDisabledPixmapCache() {
    }

    static QPixmap get(DesktopQtImage image) {
        return get(image, 0, 0, image::toQPixmap);
    }

    static QPixmap get(DesktopQtImage owner, int width, int height, Supplier<QPixmap> source) {
        CacheKey key = new CacheKey(owner,
            width,
            height,
            DesktopQtImage.devicePixelRatio(),
            IconLibraryManager.get().getModificationCount(),
            DesktopQtGrayedImageImpl.isDarkStyle());

        QPixmap cached = ourCache.get(key);
        if (cached != null && !cached.isDisposed()) {
            return cached;
        }

        QPixmap pixmap = source.get();
        if (pixmap.isNull()) {
            return pixmap;
        }

        QPixmap gray = DesktopQtGrayedImageImpl.toGrayPixmap(pixmap);
        ourCache.put(key, gray);
        return gray;
    }
}
