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
package consulo.web.ui.impl.internal.image;

import consulo.ui.image.Image;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class WebBusyImageImpl implements Image {
    private static final ConcurrentMap<Long, WebBusyImageImpl> ourImages = new ConcurrentHashMap<>();

    public static WebBusyImageImpl of(int width, int height) {
        long key = ((long) width << 32) | (height & 0xFFFFFFFFL);
        return ourImages.computeIfAbsent(key, it -> new WebBusyImageImpl(width, height));
    }

    private final int myWidth;
    private final int myHeight;

    private WebBusyImageImpl(int width, int height) {
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
}
