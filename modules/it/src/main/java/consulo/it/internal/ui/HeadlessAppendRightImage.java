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

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Two images side by side.
 *
 * @author VISTALL
 */
public final class HeadlessAppendRightImage implements Image {
    private final Image myLeft;
    private final Image myRight;

    public HeadlessAppendRightImage(Image left, Image right) {
        myLeft = left;
        myRight = right;
    }

    public Image getLeft() {
        return myLeft;
    }

    public Image getRight() {
        return myRight;
    }

    @Override
    public int getWidth() {
        return myLeft.getWidth() + myRight.getWidth();
    }

    @Override
    public int getHeight() {
        return Math.max(myLeft.getHeight(), myRight.getHeight());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return HeadlessImages.unwrap(o) instanceof HeadlessAppendRightImage that
            && myLeft.equals(that.myLeft)
            && myRight.equals(that.myRight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myLeft, myRight);
    }

    @Override
    public String toString() {
        return "appendRight[" + myLeft + ", " + myRight + "]";
    }
}
