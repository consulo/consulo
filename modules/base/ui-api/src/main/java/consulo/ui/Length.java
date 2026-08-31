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
package consulo.ui;

import consulo.ui.internal.CompositeLength;
import consulo.ui.internal.FontLength;
import consulo.ui.internal.PixelLength;

import java.util.List;

/**
 * A one dimensional length, in a unit the frontend resolves.
 * <p/>
 * A pixel is the same length whatever draws it, while a font counts one line of the default text, which every platform
 * sizes on its own - so a row asking for two fonts stays two lines tall after the font changes, where a row asking for
 * a pixel height would start clipping. Lengths add up, so two lines of text over a padding of eight pixels is
 * {@code ofFont(2).plusPixel(8)}.
 * <p/>
 * The kinds are not visible outside this package - a frontend takes a length apart with a {@link Visitor}, so a new
 * unit is an addition every frontend is made to answer, and where a kind is implemented stays free to change.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public sealed interface Length permits PixelLength, FontLength, CompositeLength {
    interface Visitor<R> {
        R visitPixel(int pixels);

        R visitFont(float fonts);

        R visitComposite(List<Length> parts);
    }

    static Length ofPixel(int pixels) {
        return new PixelLength(pixels);
    }

    static Length ofFont(float fonts) {
        return new FontLength(fonts);
    }

    <R> R accept(Visitor<R> visitor);

    default Length plus(Length other) {
        return new CompositeLength(List.of(this, other));
    }

    default Length plusPixel(int pixels) {
        return plus(ofPixel(pixels));
    }

    default Length plusFont(float fonts) {
        return plus(ofFont(fonts));
    }

    /**
     * @param fontHeight the height of one line of the default text, in pixels
     */
    default int toPixels(int fontHeight) {
        return accept(new Visitor<>() {
            @Override
            public Integer visitPixel(int pixels) {
                return pixels;
            }

            @Override
            public Integer visitFont(float fonts) {
                return Math.round(fonts * fontHeight);
            }

            @Override
            public Integer visitComposite(List<Length> parts) {
                int sum = 0;
                for (Length part : parts) {
                    sum += part.toPixels(fontHeight);
                }
                return sum;
            }
        });
    }
}
