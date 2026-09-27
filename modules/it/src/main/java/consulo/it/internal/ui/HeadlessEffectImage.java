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
 * An image with an effect applied to it - greyed, made transparent, colorized, resized or given a text - kept
 * together with the image it was applied to.
 *
 * @author VISTALL
 */
public final class HeadlessEffectImage implements Image {
    private final Image myBase;
    private final String myEffect;
    private final int myWidth;
    private final int myHeight;

    public HeadlessEffectImage(Image base, String effect, int width, int height) {
        myBase = base;
        myEffect = effect;
        myWidth = width;
        myHeight = height;
    }

    public Image getBase() {
        return myBase;
    }

    public String getEffect() {
        return myEffect;
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
        return HeadlessImages.unwrap(o) instanceof HeadlessEffectImage that
            && myWidth == that.myWidth
            && myHeight == that.myHeight
            && myEffect.equals(that.myEffect)
            && myBase.equals(that.myBase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myBase, myEffect, myWidth, myHeight);
    }

    @Override
    public String toString() {
        return myEffect + "[" + myBase + "]";
    }
}
