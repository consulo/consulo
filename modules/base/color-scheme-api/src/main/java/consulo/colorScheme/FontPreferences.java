/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.colorScheme;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;

public interface FontPreferences {
    @Nonnull
    String DEFAULT_FONT_NAME = "Jetbrains Mono";
    int DEFAULT_FONT_SIZE = 13;

    float DEFAULT_LINE_SPACING = 1.2f;

    @Nonnull
    List<String> getEffectiveFontFamilies();

    @Nonnull
    List<String> getRealFontFamilies();

    @Nonnull
    String getFontFamily();

    int getSize(@Nonnull String fontFamily);

    void copyTo(@Nonnull FontPreferences preferences);

    boolean useLigatures();

    boolean hasSize(@Nonnull String fontName);

    float getLineSpacing();

    void setLineSpacing(float lineSpacing);

    /**
     * There is a possible case that particular font family is not available at particular environment (e.g. Monaco under *nix).
     * However, java environment tries to mask that via 'Dialog' fonts, i.e. when we try to create font like
     * {@code new Font("Monaco", style, size)}, it creates a font object which has font family "Monaco" but is a "Dialog" font.
     * <p/>
     * That's why we have a special check for such a situation.
     *
     * @param fontName       font family name to check
     * @param fontSize       target font size
     * @param fallbackScheme colors scheme to use for fallback fonts retrieval (if necessary);
     * @return fallback font family to use if font family with the given name is not registered at current environment;
     * <code>null</code> if font family with the given name is registered at the current environment
     */
    @Nullable
    static String getFallbackName(@Nonnull String fontName, int fontSize, @Nullable EditorColorsScheme fallbackScheme) {
        Font plainFont = new Font(fontName, Font.PLAIN, fontSize);
        if (plainFont.getFamily().equals("Dialog") && !("Dialog".equals(fontName) || fontName.startsWith("Dialog."))) {
            return fallbackScheme == null ? DEFAULT_FONT_NAME : fallbackScheme.getEditorFontName();
        }
        return null;
    }
}
