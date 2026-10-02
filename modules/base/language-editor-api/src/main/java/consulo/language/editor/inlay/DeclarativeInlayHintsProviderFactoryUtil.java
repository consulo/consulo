// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.language.Language;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DeclarativeInlayHintsProviderFactoryUtil {
    private DeclarativeInlayHintsProviderFactoryUtil() {
    }

    /**
     * @return list of potentially available providers for a particular language (not filtering enabled ones)
     */
    public static List<InlayProviderInfo> getProvidersForLanguage(Language language) {
        List<InlayProviderInfo> result = new ArrayList<>();
        getFactories().forEach(factory -> result.addAll(factory.getProvidersForLanguage(language)));
        return result;
    }

    public static @Nullable InlayProviderInfo getProviderInfo(Language language, String providerId) {
        return getFactories().computeSafeIfAny(factory -> factory.getProviderInfo(language, providerId));
    }

    public static Set<Language> getSupportedLanguages() {
        Set<Language> result = new HashSet<>();
        getFactories().forEach(factory -> result.addAll(factory.getSupportedLanguages()));
        return result;
    }

    private static ExtensionPoint<DeclarativeInlayHintsProviderFactory> getFactories() {
        return Application.get().getExtensionPoint(DeclarativeInlayHintsProviderFactory.class);
    }
}
