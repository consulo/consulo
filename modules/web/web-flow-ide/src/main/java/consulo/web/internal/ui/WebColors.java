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
package consulo.web.internal.ui;

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import org.jspecify.annotations.Nullable;

/**
 * A colour of the platform as css. Everything the browser is told about a colour goes through here - a
 * {@link ColorValue} is not necessarily rgb, an hsb one among them, and only {@link ColorValue#toRGB()} knows
 * how to resolve whichever it is.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public final class WebColors {
    private WebColors() {
    }

    public static @Nullable String toCssColor(@Nullable ColorValue colorValue) {
        if (colorValue == null) {
            return null;
        }

        RGBColor color = colorValue.toRGB();

        // an alpha of its own is kept - a highlight of the editor is translucent over the text it covers
        int alpha = color.getAlpha();
        if (alpha != 255) {
            return String.format("rgba(%d,%d,%d,%.3f)", color.getRed(), color.getGreen(), color.getBlue(), alpha / 255f);
        }

        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
