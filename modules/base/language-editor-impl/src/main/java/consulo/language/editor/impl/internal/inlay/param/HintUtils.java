/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.impl.internal.inlay.param;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.editor.internal.InlayHintsSettings;
import consulo.language.editor.internal.ParameterNameHintsSettings;
import consulo.language.extension.LanguageExtension;
import jakarta.annotation.Nonnull;

import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2025-06-14
 */
public class HintUtils {
    public static int getSize(Editor editor) {
        return Math.max(1, editor.getColorsScheme().getEditorFontSize() - 1);
    }

    public static boolean isParameterHintsEnabledForLanguage(Language language) {
        if (!InlayHintsSettings.getInstance().hintsShouldBeShown(language)) {
            return false;
        }

        return ParameterNameHintsSettings.getInstance().isEnabledForLanguage(getLanguageForSettingKey(language));
    }

    public static List<Language> getBaseLanguagesWithProviders() {
        return Application.get()
            .getExtensionList(InlayParameterHintsProvider.class)
            .stream()
            .map(LanguageExtension::getLanguage)
            .sorted(Comparator.comparing(Language::getDisplayName))
            .toList();
    }

    @Nonnull
    public static Language getLanguageForSettingKey(Language language) {
        List<Language> supportedLanguages = getBaseLanguagesWithProviders();

        Language languageForSettings = language;

        while (languageForSettings != null && !supportedLanguages.contains(languageForSettings)) {
            languageForSettings = languageForSettings.getBaseLanguage();
        }

        return languageForSettings != null ? languageForSettings : language;
    }
}
