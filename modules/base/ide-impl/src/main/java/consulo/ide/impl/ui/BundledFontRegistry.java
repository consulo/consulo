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
package consulo.ide.impl.ui;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The faces bundled in consulo-ide-impl. Every frontend has to hand these to its own toolkit before a colour
 * scheme naming one of them can resolve - the browser through a stylesheet, qt through its font database, awt
 * through the graphics environment - so the list itself lives here rather than once per frontend.
 * <p>
 * Listed by hand rather than by scanning the directory, since a scan would keep the classpath resource handles
 * open.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class BundledFontRegistry {
    /**
     * @param fileName  name of the file under {@link #FONT_PATH}, both on the classpath and in the url
     * @param family    family carrying the whole weight range of the typeface
     * @param weight    css weight of this face inside {@code family}
     * @param italic    whether the face is the italic one
     * @param awtFamily the family name the jdk reports for this face, when it differs from {@code family} -
     *                  the jdk reads the ttf name table, where every weight outside regular and bold is a
     *                  family of its own, and that is the name a color scheme stores. null when the two agree
     */
    public record BundledFont(String fileName, String family, int weight, boolean italic, @Nullable String awtFamily) {
    }

    /**
     * Serves as the classpath directory of the files and as the url prefix {@code WebFontServlet} answers on.
     */
    public static final String FONT_PATH = "/fonts/";

    private static final List<BundledFont> ourFonts = List.of(
        new BundledFont("JetBrainsMono-Thin.ttf", "JetBrains Mono", 100, false, "JetBrains Mono Thin"),
        new BundledFont("JetBrainsMono-ThinItalic.ttf", "JetBrains Mono", 100, true, "JetBrains Mono Thin"),
        new BundledFont("JetBrainsMono-ExtraLight.ttf", "JetBrains Mono", 200, false, "JetBrains Mono ExtraLight"),
        new BundledFont("JetBrainsMono-ExtraLightItalic.ttf", "JetBrains Mono", 200, true, "JetBrains Mono ExtraLight"),
        new BundledFont("JetBrainsMono-Light.ttf", "JetBrains Mono", 300, false, "JetBrains Mono Light"),
        new BundledFont("JetBrainsMono-LightItalic.ttf", "JetBrains Mono", 300, true, "JetBrains Mono Light"),
        new BundledFont("JetBrainsMono-Regular.ttf", "JetBrains Mono", 400, false, null),
        new BundledFont("JetBrainsMono-Italic.ttf", "JetBrains Mono", 400, true, null),
        new BundledFont("JetBrainsMono-Medium.ttf", "JetBrains Mono", 500, false, "JetBrains Mono Medium"),
        new BundledFont("JetBrainsMono-MediumItalic.ttf", "JetBrains Mono", 500, true, "JetBrains Mono Medium"),
        new BundledFont("JetBrainsMono-SemiBold.ttf", "JetBrains Mono", 600, false, "JetBrains Mono SemiBold"),
        new BundledFont("JetBrainsMono-SemiBoldItalic.ttf", "JetBrains Mono", 600, true, "JetBrains Mono SemiBold"),
        new BundledFont("JetBrainsMono-Bold.ttf", "JetBrains Mono", 700, false, null),
        new BundledFont("JetBrainsMono-BoldItalic.ttf", "JetBrains Mono", 700, true, null),
        new BundledFont("JetBrainsMono-ExtraBold.ttf", "JetBrains Mono", 800, false, "JetBrains Mono ExtraBold"),
        new BundledFont("JetBrainsMono-ExtraBoldItalic.ttf", "JetBrains Mono", 800, true, "JetBrains Mono ExtraBold"),

        new BundledFont("FiraCode-Light.ttf", "Fira Code", 300, false, "Fira Code Light"),
        new BundledFont("FiraCode-Regular.ttf", "Fira Code", 400, false, null),
        // retina sits between regular and medium, which is what the typeface calls 450
        new BundledFont("FiraCode-Retina.ttf", "Fira Code", 450, false, "Fira Code Retina"),
        new BundledFont("FiraCode-Medium.ttf", "Fira Code", 500, false, "Fira Code Medium"),
        new BundledFont("FiraCode-SemiBold.ttf", "Fira Code", 600, false, "Fira Code SemiBold"),
        new BundledFont("FiraCode-Bold.ttf", "Fira Code", 700, false, null),

        new BundledFont("SourceCodePro-ExtraLight.ttf", "Source Code Pro", 200, false, "Source Code Pro ExtraLight"),
        new BundledFont("SourceCodePro-ExtraLightIt.ttf", "Source Code Pro", 200, true, "Source Code Pro ExtraLight"),
        new BundledFont("SourceCodePro-Light.ttf", "Source Code Pro", 300, false, "Source Code Pro Light"),
        new BundledFont("SourceCodePro-LightIt.ttf", "Source Code Pro", 300, true, "Source Code Pro Light"),
        new BundledFont("SourceCodePro-Regular.ttf", "Source Code Pro", 400, false, null),
        new BundledFont("SourceCodePro-It.ttf", "Source Code Pro", 400, true, null),
        new BundledFont("SourceCodePro-Medium.ttf", "Source Code Pro", 500, false, "Source Code Pro Medium"),
        new BundledFont("SourceCodePro-MediumIt.ttf", "Source Code Pro", 500, true, "Source Code Pro Medium"),
        // the name table of this typeface spells the weight with a lowercase b, unlike every other one here
        new BundledFont("SourceCodePro-Semibold.ttf", "Source Code Pro", 600, false, "Source Code Pro Semibold"),
        new BundledFont("SourceCodePro-SemiboldIt.ttf", "Source Code Pro", 600, true, "Source Code Pro Semibold"),
        new BundledFont("SourceCodePro-Bold.ttf", "Source Code Pro", 700, false, null),
        new BundledFont("SourceCodePro-BoldIt.ttf", "Source Code Pro", 700, true, null),
        new BundledFont("SourceCodePro-Black.ttf", "Source Code Pro", 900, false, "Source Code Pro Black"),
        new BundledFont("SourceCodePro-BlackIt.ttf", "Source Code Pro", 900, true, "Source Code Pro Black"),

        new BundledFont("Inconsolata.ttf", "Inconsolata", 400, false, null),

        new BundledFont("Inter-Thin.ttf", "Inter", 100, false, "Inter Thin"),
        new BundledFont("Inter-ThinItalic.ttf", "Inter", 100, true, "Inter Thin"),
        new BundledFont("Inter-Light.ttf", "Inter", 300, false, "Inter Light"),
        new BundledFont("Inter-LightItalic.ttf", "Inter", 300, true, "Inter Light"),
        new BundledFont("Inter-Regular.ttf", "Inter", 400, false, null),
        new BundledFont("Inter-Italic.ttf", "Inter", 400, true, null),
        new BundledFont("Inter-Medium.ttf", "Inter", 500, false, "Inter Medium"),
        new BundledFont("Inter-MediumItalic.ttf", "Inter", 500, true, "Inter Medium"),
        new BundledFont("Inter-SemiBold.ttf", "Inter", 600, false, "Inter SemiBold"),
        new BundledFont("Inter-SemiBoldItalic.ttf", "Inter", 600, true, "Inter SemiBold"),
        new BundledFont("Inter-Bold.ttf", "Inter", 700, false, null),
        new BundledFont("Inter-BoldItalic.ttf", "Inter", 700, true, null),
        new BundledFont("Inter-Black.ttf", "Inter", 900, false, "Inter Black"),
        new BundledFont("Inter-BlackItalic.ttf", "Inter", 900, true, "Inter Black"),

        new BundledFont("Roboto-Thin.ttf", "Roboto", 100, false, "Roboto Thin"),
        new BundledFont("Roboto-Light.ttf", "Roboto", 300, false, "Roboto Light")
    );

    private BundledFontRegistry() {
    }

    public static List<BundledFont> getBundledFonts() {
        return ourFonts;
    }

    /**
     * Every name a color scheme or a ui setting may store, in the same shape the awt font manager reports -
     * the typeface families plus the standalone family of each weight outside regular and bold.
     */
    public static Set<String> getFamilyNames() {
        Set<String> names = new LinkedHashSet<>();

        for (BundledFont font : ourFonts) {
            names.add(font.family());

            String awtFamily = font.awtFamily();
            if (awtFamily != null) {
                names.add(awtFamily);
            }
        }

        return names;
    }

    /**
     * @return the classpath path of a listed face, or null for anything else - an unchecked name would turn
     * the font url into a reader of arbitrary classpath resources
     */
    public static @Nullable String findResourcePath(String fileName) {
        for (BundledFont font : ourFonts) {
            if (font.fileName().equals(fileName)) {
                return FONT_PATH + font.fileName();
            }
        }

        return null;
    }
}
