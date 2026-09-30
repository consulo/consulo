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

import consulo.application.Application;
import consulo.container.plugin.PluginId;
import consulo.ui.ex.font.BundledFont;
import consulo.ui.ex.internal.BundledFontRegistry;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes the faces of {@link BundledFontRegistry} resolvable by family name in the browser. The list itself is
 * shared with the other frontends - only the way it is handed to the toolkit differs, and for a browser that is
 * a stylesheet.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebFontRegistry {
    public static final String FONT_PATH = "/fonts/";

    private static final String TRANSIENT_PATH = "transient/";

    private static final int NORMAL_WEIGHT = 400;

    public record TransientFont(byte[] data, String contentType) {
    }

    private static final Map<String, TransientFont> ourTransientFonts = new ConcurrentHashMap<>();

    private WebFontRegistry() {
    }

    public static String buildFontFaceCss() {
        StringBuilder css = new StringBuilder();

        for (Map.Entry<PluginId, List<BundledFont>> entry : getRegistry().getFonts().entrySet()) {
            for (BundledFont font : entry.getValue()) {
                String url = FONT_PATH + entry.getKey().getIdString() + "/" + font.fileName();

                appendFontFace(css, font, url, font.family(), font.weight());

                String legacyFamily = font.legacyFamily();
                if (legacyFamily != null) {
                    // the jdk hands this face out as the plain one of a family of its own, so a scheme naming it
                    // has to reach the same file without asking for a weight
                    appendFontFace(css, font, url, legacyFamily, NORMAL_WEIGHT);
                }
            }
        }

        return css.toString();
    }

    public static @Nullable BundledFont findFont(String path) {
        int separator = path.lastIndexOf('/');
        if (separator <= 0) {
            return null;
        }

        PluginId pluginId = PluginId.findId(path.substring(0, separator));
        if (pluginId == null) {
            return null;
        }

        List<BundledFont> fonts = getRegistry().getFonts().get(pluginId);
        if (fonts == null) {
            return null;
        }

        String fileName = path.substring(separator + 1);
        for (BundledFont font : fonts) {
            if (font.fileName().equals(fileName)) {
                return font;
            }
        }
        return null;
    }

    public static String registerTransientFont(byte[] data, String contentType) {
        String token = UUID.randomUUID().toString();
        ourTransientFonts.put(token, new TransientFont(data, contentType));
        return token;
    }

    public static void unregisterTransientFont(String token) {
        ourTransientFonts.remove(token);
    }

    public static String getTransientFontUrl(String token) {
        return FONT_PATH + TRANSIENT_PATH + token;
    }

    public static @Nullable TransientFont findTransientFont(String path) {
        if (!path.startsWith(TRANSIENT_PATH)) {
            return null;
        }
        return ourTransientFonts.get(path.substring(TRANSIENT_PATH.length()));
    }

    private static BundledFontRegistry getRegistry() {
        return Application.get().getInstance(BundledFontRegistry.class);
    }

    private static void appendFontFace(StringBuilder css, BundledFont font, String url, String family, int weight) {
        css.append("@font-face{")
            .append("font-family:\"").append(family).append("\";")
            .append("font-weight:").append(weight).append(';')
            .append("font-style:").append(font.italic() ? "italic" : "normal").append(';')
            // the editor measures its line metrics once and caches them, a face swapped in after that would
            // leave the whole view laid out against the metrics of the fallback
            .append("font-display:block;")
            .append("src:url(\"").append(url).append("\") format(\"truetype\");")
            .append('}');
    }
}
