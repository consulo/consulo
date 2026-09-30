/*
 * Copyright 2013-2020 consulo.io
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

import com.vaadin.flow.component.UI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.UIAccess;
import consulo.ui.ex.font.BundledFont;
import consulo.ui.ex.internal.BundledFontRegistry;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.font.Typeface;
import consulo.ui.impl.font.TypefaceImpl;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public class WebFontManagerImpl implements FontManager {
    public static final WebFontManagerImpl ourInstance = new WebFontManagerImpl();

    /**
     * What reading the installed families would cost right now.
     */
    public enum LocalFontsPermission {
        /**
         * Nothing to read and nothing to ask - an engine which has not shipped the api, or a page which is not
         * a secure context, where the call does not exist at all.
         */
        UNSUPPORTED,
        /**
         * Already agreed to, so {@link #getAvailableTypefacesAsync} will answer without prompting.
         */
        GRANTED,
        /**
         * Readable, but only by prompting - so it waits for the user to ask for it.
         */
        CAN_ASK
    }

    private static final String REGISTER_FONT = """
        const face = new FontFace($0, 'url("' + $1 + '")');
        document.fonts.add(face);
        return face.load().then(() => true);
        """;

    private static final String UNREGISTER_FONT = """
        for (const face of [...document.fonts]) {
          if (face.family === $0 || face.family === '"' + $0 + '"') {
            document.fonts.delete(face);
          }
        }
        """;

    private static final int DEFAULT_FONT_SIZE = 14;

    private static final Map<String, String> ourFontContentTypes = Map.of(
        "ttf", "font/ttf",
        "otf", "font/otf",
        "ttc", "font/collection",
        "woff", "font/woff",
        "woff2", "font/woff2"
    );

    private static final String QUERY_STATE = """
        return (async () => {
          if (!('queryLocalFonts' in window)) {
            return 'UNSUPPORTED';
          }
          try {
            const status = await navigator.permissions.query({name: 'local-fonts'});
            return status.state === 'granted' ? 'GRANTED' : 'CAN_ASK';
          }
          catch (e) {
            return 'CAN_ASK';
          }
        })();
        """;

    /**
     * Whether a family is monospaced is not something the font data says, so it is measured the way the awt
     * side measures it - a narrow glyph and a wide one advance by the same amount only in a family of one
     * width. The family is named to the canvas rather than loaded, which is what makes this cheap.
     */
    private static final String QUERY_FONTS = """
        return (async () => {
          if (!('queryLocalFonts' in window)) {
            return null;
          }
          const fonts = await window.queryLocalFonts();
          const context = document.createElement('canvas').getContext('2d');
          const seen = new Set();
          const families = [];
          let measured = 0;
          for (const font of fonts) {
            const name = font.family;
            if (seen.has(name)) {
              continue;
            }
            seen.add(name);
            context.font = '16px "' + name.replace(/["\\\\]/g, '') + '"';
            const narrow = context.measureText('l').width;
            const wide = context.measureText('W').width;
            families.push({name: name, monospaced: narrow > 0 && Math.abs(narrow - wide) < 0.01});
            // naming a family to the canvas makes the engine instantiate it, and a machine with several
            // hundred installed would hold the main thread for seconds - so the loop lets go regularly
            if (++measured % 25 === 0) {
              await new Promise(resolve => setTimeout(resolve));
            }
          }
          families.sort((one, two) => one.name.localeCompare(two.name));
          return families;
        })();
        """;

    @Override
    public boolean isRequiredPermission() {
        return true;
    }

    /**
     * Reads the installed families, prompting for them if that is what it takes - which is the difference
     * between this and {@link #getLocalFontsPermissionAsync}, which only reports what such a read would cost
     * and never puts anything in front of the user. Call this from a gesture. Whatever the browser will not
     * hand over degrades to the faces the page ships a {@code @font-face} for, which are the only ones it can
     * render regardless.
     */
    @Override
    public CompletableFuture<List<Typeface>> getAvailableTypefacesAsync(UIAccess uiAccess) {
        CompletableFuture<List<Typeface>> result = new CompletableFuture<>();

        UI ui = ((WebUIAccessImpl) uiAccess).getUI();

        // through giveAsync rather than give: give is void and drops the work on a detached ui with only a
        // warning, which would leave this future pending for the life of the session
        uiAccess.giveAsync(() -> ui.getPage()
            .executeJs(QUERY_FONTS)
            .then(
                node -> result.complete(readTypefaces(node)),
                error -> result.complete(getBundledTypefaces())
            )).whenComplete((ignored, e) -> {
            if (e != null) {
                result.complete(getBundledTypefaces());
            }
        });

        return result;
    }

    /**
     * Reports what reading the families would cost, without reading them and without prompting - so a chooser
     * can decide whether to offer the asking at all. {@link #getAvailableTypefacesAsync} is the one that does
     * the reading, and the prompting with it.
     */
    public CompletableFuture<LocalFontsPermission> getLocalFontsPermissionAsync(UIAccess uiAccess) {
        CompletableFuture<LocalFontsPermission> result = new CompletableFuture<>();

        UI ui = ((WebUIAccessImpl) uiAccess).getUI();

        uiAccess.giveAsync(() -> ui.getPage()
            .executeJs(QUERY_STATE)
            .then(
                String.class,
                state -> result.complete(readPermission(state)),
                error -> result.complete(LocalFontsPermission.UNSUPPORTED)
            )).whenComplete((ignored, e) -> {
            if (e != null) {
                result.complete(LocalFontsPermission.UNSUPPORTED);
            }
        });

        return result;
    }

    /**
     * The faces the page ships, which need no permission because it is the page that carries them.
     */
    public static List<Typeface> getBundledTypefaces() {
        Map<String, Boolean> families = new LinkedHashMap<>();
        for (List<BundledFont> fonts : Application.get().getInstance(BundledFontRegistry.class).getFonts().values()) {
            for (BundledFont font : fonts) {
                families.putIfAbsent(font.family(), font.monospaced());

                String legacyFamily = font.legacyFamily();
                if (legacyFamily != null) {
                    families.putIfAbsent(legacyFamily, font.monospaced());
                }
            }
        }

        List<Typeface> typefaces = new ArrayList<>();
        families.forEach((family, monospaced) -> typefaces.add(new TypefaceImpl(family, monospaced)));

        typefaces.sort(Comparator.comparing(Typeface::getName));
        return typefaces;
    }

    private static LocalFontsPermission readPermission(String permission) {
        try {
            return LocalFontsPermission.valueOf(permission);
        }
        catch (IllegalArgumentException e) {
            return LocalFontsPermission.UNSUPPORTED;
        }
    }

    /**
     * The installed families and the bundled ones together. The page ships a {@code @font-face} for the bundled
     * faces, so they stay renderable whatever the browser was willing to say - being told about the installed
     * ones is something gained rather than something that replaces them. Where a family is both, what is known
     * here about its pitch is kept over what was measured in the document.
     */
    private static List<Typeface> readTypefaces(JsonNode node) {
        Map<String, Typeface> byName = new TreeMap<>();

        if (node != null && node.isArray()) {
            for (JsonNode each : node) {
                String name = each.path("name").asString("");
                if (!name.isEmpty()) {
                    byName.put(name, new TypefaceImpl(name, each.path("monospaced").asBoolean(false)));
                }
            }
        }

        for (Typeface bundled : getBundledTypefaces()) {
            byName.put(bundled.getName(), bundled);
        }

        return List.copyOf(byName.values());
    }

    @Override
    public Font createFont(String fontName, int fontSize, int fontStyle) {
        return new WebFontImpl(fontName, fontSize, fontStyle);
    }

    @Override
    public CompletableFuture<Font> registerFontAsync(URL url, UIAccess uiAccess, Disposable parent) {
        CompletableFuture<Font> result = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            byte[] data;
            try (InputStream stream = url.openStream()) {
                data = stream.readAllBytes();
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
                return;
            }

            if (Disposer.isDisposed(parent)) {
                result.cancel(false);
                return;
            }

            String fileName = getFileName(url);
            String token = WebFontRegistry.registerTransientFont(data, getContentType(fileName));
            String family = "consulo-font-" + token;
            String fontName = readFontName(data, fileName);
            UI ui = ((WebUIAccessImpl) uiAccess).getUI();

            Disposer.register(parent, () -> {
                WebFontRegistry.unregisterTransientFont(token);
                uiAccess.give(() -> ui.getPage().executeJs(UNREGISTER_FONT, family));
            });

            uiAccess.giveAsync(() -> ui.getPage()
                .executeJs(REGISTER_FONT, family, WebFontRegistry.getTransientFontUrl(token))
                .then(
                    ignored -> result.complete(new WebFontImpl(family, fontName, DEFAULT_FONT_SIZE, Font.PLAIN)),
                    error -> result.completeExceptionally(new IllegalArgumentException("Cannot load font " + fileName + ": " + error))
                )).whenComplete((ignored, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                }
            });
        });
        return result;
    }

    private static String getFileName(URL url) {
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ourFontContentTypes.getOrDefault(extension, "application/octet-stream");
    }

    private static String readFontName(byte[] data, String fileName) {
        try {
            return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new ByteArrayInputStream(data)).getFontName();
        }
        catch (Exception e) {
            return fileName;
        }
    }
}
