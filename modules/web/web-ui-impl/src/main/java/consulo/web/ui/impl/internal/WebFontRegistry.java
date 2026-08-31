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
package consulo.web.ui.impl.internal;

import consulo.ui.impl.font.BundledFontRegistry;
import consulo.ui.impl.font.BundledFontRegistry.BundledFont;

/**
 * Makes the faces of {@link BundledFontRegistry} resolvable by family name in the browser. The list itself is
 * shared with the other frontends - only the way it is handed to the toolkit differs, and for a browser that is
 * a stylesheet.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebFontRegistry {
    private static final int NORMAL_WEIGHT = 400;

    private WebFontRegistry() {
    }

    public static String buildFontFaceCss() {
        StringBuilder css = new StringBuilder();

        for (BundledFont font : BundledFontRegistry.getBundledFonts()) {
            appendFontFace(css, font, font.family(), font.weight());

            String awtFamily = font.awtFamily();
            if (awtFamily != null) {
                // the jdk hands this face out as the plain one of a family of its own, so a scheme naming it
                // has to reach the same file without asking for a weight
                appendFontFace(css, font, awtFamily, NORMAL_WEIGHT);
            }
        }

        return css.toString();
    }

    private static void appendFontFace(StringBuilder css, BundledFont font, String family, int weight) {
        css.append("@font-face{")
            .append("font-family:\"").append(family).append("\";")
            .append("font-weight:").append(weight).append(';')
            .append("font-style:").append(font.italic() ? "italic" : "normal").append(';')
            // the editor measures its line metrics once and caches them, a face swapped in after that would
            // leave the whole view laid out against the metrics of the fallback
            .append("font-display:block;")
            .append("src:url(\"").append(BundledFontRegistry.FONT_PATH).append(font.fileName()).append("\") format(\"truetype\");")
            .append('}');
    }
}
