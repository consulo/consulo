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
package consulo.ui.ex.internal;

import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

/**
 * The product name drawn from the same 5x5 glyph font the splash animates, as a plain image, so a
 * frontend which cannot host a painting component still gets the logo.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class LogoImage {
    private static final int PADDING = 1, LETTER_SPACING = 1;
    private static final int N_LETTERS = Names.ourName.length();

    public static final int WIDTH = PADDING + (Glyph.WIDTH + LETTER_SPACING) * N_LETTERS - LETTER_SPACING + PADDING;
    public static final int HEIGHT = PADDING + Glyph.HEIGHT + PADDING;

    public static Image create(int pixelSize, ColorValue foreground) {
        boolean[][] pixels = new boolean[WIDTH][HEIGHT];

        for (int i = 0; i < N_LETTERS; i++) {
            int dx = PADDING + (Glyph.WIDTH + LETTER_SPACING) * i;

            Alphabet.VALID_CHARACTERS.get(Names.ourName.charAt(i)).draw(dx, PADDING, (x, y, foregroundPixel) -> {
                if (x < WIDTH && y < HEIGHT) {
                    pixels[x][y] = foregroundPixel;
                }
            });
        }

        return ImageEffects.canvas(WIDTH * pixelSize, HEIGHT * pixelSize, ctx -> {
            ctx.setFillStyle(foreground);

            // every lit pixel goes into one path, filled once - a fill per pixel would be a separate shape in
            // the svg the web frontend builds out of these calls
            ctx.beginPath();

            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (pixels[x][y]) {
                        ctx.rect(x * pixelSize, y * pixelSize, pixelSize, pixelSize);
                    }
                }
            }

            ctx.fill();
        });
    }
}
