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
package consulo.ui.bundled.fonts.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.ui.ex.font.BundledFont;
import consulo.ui.ex.font.BundledFontProvider;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
@ExtensionImpl
public class DefaultBundledFontProvider implements BundledFontProvider {
    private static final String JETBRAINS_MONO = "JetBrains Mono";
    private static final String FIRA_CODE = "Fira Code";
    private static final String SOURCE_CODE_PRO = "Source Code Pro";
    private static final String INCONSOLATA = "Inconsolata";
    private static final String INTER = "Inter";
    private static final String ROBOTO = "Roboto";

    @Override
    public void registerFonts(Consumer<BundledFont> registrar) {
        registrar.accept(font("JetBrainsMono-Thin.ttf", JETBRAINS_MONO, 100, false, "JetBrains Mono Thin", true));
        registrar.accept(font("JetBrainsMono-ThinItalic.ttf", JETBRAINS_MONO, 100, true, "JetBrains Mono Thin", true));
        registrar.accept(font("JetBrainsMono-ExtraLight.ttf", JETBRAINS_MONO, 200, false, "JetBrains Mono ExtraLight", true));
        registrar.accept(font("JetBrainsMono-ExtraLightItalic.ttf", JETBRAINS_MONO, 200, true, "JetBrains Mono ExtraLight", true));
        registrar.accept(font("JetBrainsMono-Light.ttf", JETBRAINS_MONO, 300, false, "JetBrains Mono Light", true));
        registrar.accept(font("JetBrainsMono-LightItalic.ttf", JETBRAINS_MONO, 300, true, "JetBrains Mono Light", true));
        registrar.accept(font("JetBrainsMono-Regular.ttf", JETBRAINS_MONO, 400, false, null, true));
        registrar.accept(font("JetBrainsMono-Italic.ttf", JETBRAINS_MONO, 400, true, null, true));
        registrar.accept(font("JetBrainsMono-Medium.ttf", JETBRAINS_MONO, 500, false, "JetBrains Mono Medium", true));
        registrar.accept(font("JetBrainsMono-MediumItalic.ttf", JETBRAINS_MONO, 500, true, "JetBrains Mono Medium", true));
        registrar.accept(font("JetBrainsMono-SemiBold.ttf", JETBRAINS_MONO, 600, false, "JetBrains Mono SemiBold", true));
        registrar.accept(font("JetBrainsMono-SemiBoldItalic.ttf", JETBRAINS_MONO, 600, true, "JetBrains Mono SemiBold", true));
        registrar.accept(font("JetBrainsMono-Bold.ttf", JETBRAINS_MONO, 700, false, null, true));
        registrar.accept(font("JetBrainsMono-BoldItalic.ttf", JETBRAINS_MONO, 700, true, null, true));
        registrar.accept(font("JetBrainsMono-ExtraBold.ttf", JETBRAINS_MONO, 800, false, "JetBrains Mono ExtraBold", true));
        registrar.accept(font("JetBrainsMono-ExtraBoldItalic.ttf", JETBRAINS_MONO, 800, true, "JetBrains Mono ExtraBold", true));

        registrar.accept(font("FiraCode-Light.ttf", FIRA_CODE, 300, false, "Fira Code Light", true));
        registrar.accept(font("FiraCode-Regular.ttf", FIRA_CODE, 400, false, null, true));
        registrar.accept(font("FiraCode-Retina.ttf", FIRA_CODE, 450, false, "Fira Code Retina", true));
        registrar.accept(font("FiraCode-Medium.ttf", FIRA_CODE, 500, false, "Fira Code Medium", true));
        registrar.accept(font("FiraCode-SemiBold.ttf", FIRA_CODE, 600, false, "Fira Code SemiBold", true));
        registrar.accept(font("FiraCode-Bold.ttf", FIRA_CODE, 700, false, null, true));

        registrar.accept(font("SourceCodePro-ExtraLight.ttf", SOURCE_CODE_PRO, 200, false, "Source Code Pro ExtraLight", true));
        registrar.accept(font("SourceCodePro-ExtraLightIt.ttf", SOURCE_CODE_PRO, 200, true, "Source Code Pro ExtraLight", true));
        registrar.accept(font("SourceCodePro-Light.ttf", SOURCE_CODE_PRO, 300, false, "Source Code Pro Light", true));
        registrar.accept(font("SourceCodePro-LightIt.ttf", SOURCE_CODE_PRO, 300, true, "Source Code Pro Light", true));
        registrar.accept(font("SourceCodePro-Regular.ttf", SOURCE_CODE_PRO, 400, false, null, true));
        registrar.accept(font("SourceCodePro-It.ttf", SOURCE_CODE_PRO, 400, true, null, true));
        registrar.accept(font("SourceCodePro-Medium.ttf", SOURCE_CODE_PRO, 500, false, "Source Code Pro Medium", true));
        registrar.accept(font("SourceCodePro-MediumIt.ttf", SOURCE_CODE_PRO, 500, true, "Source Code Pro Medium", true));
        registrar.accept(font("SourceCodePro-Semibold.ttf", SOURCE_CODE_PRO, 600, false, "Source Code Pro Semibold", true));
        registrar.accept(font("SourceCodePro-SemiboldIt.ttf", SOURCE_CODE_PRO, 600, true, "Source Code Pro Semibold", true));
        registrar.accept(font("SourceCodePro-Bold.ttf", SOURCE_CODE_PRO, 700, false, null, true));
        registrar.accept(font("SourceCodePro-BoldIt.ttf", SOURCE_CODE_PRO, 700, true, null, true));
        registrar.accept(font("SourceCodePro-Black.ttf", SOURCE_CODE_PRO, 900, false, "Source Code Pro Black", true));
        registrar.accept(font("SourceCodePro-BlackIt.ttf", SOURCE_CODE_PRO, 900, true, "Source Code Pro Black", true));

        registrar.accept(font("Inconsolata.ttf", INCONSOLATA, 400, false, null, true));

        registrar.accept(font("Inter-Thin.ttf", INTER, 100, false, "Inter Thin", false));
        registrar.accept(font("Inter-ThinItalic.ttf", INTER, 100, true, "Inter Thin", false));
        registrar.accept(font("Inter-Light.ttf", INTER, 300, false, "Inter Light", false));
        registrar.accept(font("Inter-LightItalic.ttf", INTER, 300, true, "Inter Light", false));
        registrar.accept(font("Inter-Regular.ttf", INTER, 400, false, null, false));
        registrar.accept(font("Inter-Italic.ttf", INTER, 400, true, null, false));
        registrar.accept(font("Inter-Medium.ttf", INTER, 500, false, "Inter Medium", false));
        registrar.accept(font("Inter-MediumItalic.ttf", INTER, 500, true, "Inter Medium", false));
        registrar.accept(font("Inter-SemiBold.ttf", INTER, 600, false, "Inter SemiBold", false));
        registrar.accept(font("Inter-SemiBoldItalic.ttf", INTER, 600, true, "Inter SemiBold", false));
        registrar.accept(font("Inter-Bold.ttf", INTER, 700, false, null, false));
        registrar.accept(font("Inter-BoldItalic.ttf", INTER, 700, true, null, false));
        registrar.accept(font("Inter-Black.ttf", INTER, 900, false, "Inter Black", false));
        registrar.accept(font("Inter-BlackItalic.ttf", INTER, 900, true, "Inter Black", false));

        registrar.accept(font("Roboto-Thin.ttf", ROBOTO, 100, false, "Roboto Thin", false));
        registrar.accept(font("Roboto-Light.ttf", ROBOTO, 300, false, "Roboto Light", false));
    }

    private static BundledFont font(String fileName, String family, int weight, boolean italic, @Nullable String legacyFamily, boolean monospaced) {
        URL url = Objects.requireNonNull(DefaultBundledFontProvider.class.getResource(fileName), fileName);
        return new BundledFont(url, family, weight, italic, legacyFamily, monospaced);
    }
}
