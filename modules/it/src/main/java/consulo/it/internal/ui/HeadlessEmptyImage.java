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
package consulo.it.internal.ui;

import consulo.ui.image.EmptyImage;
import org.jspecify.annotations.Nullable;

/**
 * Dummy-but-creatable headless {@link EmptyImage}.
 *
 * @author VISTALL
 */
public class HeadlessEmptyImage implements EmptyImage {
    private final int myWidth;
    private final int myHeight;

    public HeadlessEmptyImage(int width, int height) {
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

    @Override
    public boolean equals(@Nullable Object o) {
        return HeadlessImages.unwrap(o) instanceof HeadlessEmptyImage that && myWidth == that.myWidth && myHeight == that.myHeight;
    }

    @Override
    public int hashCode() {
        return 31 * myWidth + myHeight;
    }

    @Override
    public String toString() {
        return "empty(" + myWidth + "x" + myHeight + ")";
    }
}
