// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.Language;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Allows to register providers from one family for multiple languages.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DeclarativeInlayHintsProviderFactory {
    /**
     * @return list of providers which may be run on a file with specific language.
     */
    List<InlayProviderInfo> getProvidersForLanguage(Language language);

    /**
     * List of languages for which theoretically this factory may create providers (or may not).
     */
    Set<Language> getSupportedLanguages();

    /**
     * Searches for provider info by id of the provider. Must provide one of the providers of {@link #getProvidersForLanguage}.
     */
    @Nullable InlayProviderInfo getProviderInfo(Language language, String providerId);
}
