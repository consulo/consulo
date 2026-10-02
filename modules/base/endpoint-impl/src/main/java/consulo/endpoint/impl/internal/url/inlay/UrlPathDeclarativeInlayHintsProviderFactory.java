// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.endpoint.url.inlay.UrlPathInlayLanguagesProvider;
import consulo.language.Language;
import consulo.language.MetaLanguage;
import consulo.language.editor.inlay.DeclarativeInlayHintsProviderFactory;
import consulo.language.editor.inlay.InlayProviderInfo;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtensionImpl
public final class UrlPathDeclarativeInlayHintsProviderFactory implements DeclarativeInlayHintsProviderFactory {
    private static final ExtensionPointCacheKey<UrlPathInlayLanguagesProvider, Map<Language, UrlPathDeclarativeInlayHintsProvider>>
        PROVIDERS = ExtensionPointCacheKey.create("UrlPathDeclarativeInlayHintsProvider", walker -> {
            Map<Language, List<UrlPathInlayLanguagesProvider>> languagesProviders = new LinkedHashMap<>();
            walker.walk(languagesProvider -> {
                for (Language language : expand(languagesProvider.getLanguages())) {
                    List<UrlPathInlayLanguagesProvider> list = languagesProviders.computeIfAbsent(language, it -> new ArrayList<>());
                    if (!list.contains(languagesProvider)) {
                        list.add(languagesProvider);
                    }
                }
            });

            Map<Language, UrlPathDeclarativeInlayHintsProvider> providers = new LinkedHashMap<>();
            languagesProviders.forEach(
                (language, list) -> providers.put(language, new UrlPathDeclarativeInlayHintsProvider(language, list))
            );
            return providers;
        });

    private final Application myApplication;

    @Inject
    public UrlPathDeclarativeInlayHintsProviderFactory(Application application) {
        myApplication = application;
    }

    @Override
    public List<InlayProviderInfo> getProvidersForLanguage(Language language) {
        List<InlayProviderInfo> result = new ArrayList<>();
        for (UrlPathDeclarativeInlayHintsProvider provider : getUrlPathProvidersForLanguage(language)) {
            result.add(toProviderInfo(provider));
        }
        return result;
    }

    @Override
    public Set<Language> getSupportedLanguages() {
        return new LinkedHashSet<>(getProviders().keySet());
    }

    @Override
    public @Nullable InlayProviderInfo getProviderInfo(Language language, String providerId) {
        for (UrlPathDeclarativeInlayHintsProvider provider : getUrlPathProvidersForLanguage(language)) {
            if (provider.getId().equals(providerId)) {
                return toProviderInfo(provider);
            }
        }
        return null;
    }

    public List<UrlPathDeclarativeInlayHintsProvider> getUrlPathProvidersForLanguage(Language language) {
        UrlPathDeclarativeInlayHintsProvider provider = getProviders().get(language);
        return provider == null ? List.of() : List.of(provider);
    }

    private Map<Language, UrlPathDeclarativeInlayHintsProvider> getProviders() {
        return myApplication.getExtensionPoint(UrlPathInlayLanguagesProvider.class).getOrBuildCache(PROVIDERS);
    }

    private static InlayProviderInfo toProviderInfo(UrlPathDeclarativeInlayHintsProvider provider) {
        return new InlayProviderInfo(provider, provider.getId(), provider.getOptions(), provider.isEnabledByDefault(), provider.getName());
    }

    static Set<Language> expand(Collection<Language> languages) {
        Set<Language> result = new LinkedHashSet<>();
        for (Language language : languages) {
            if (language instanceof MetaLanguage metaLanguage) {
                result.addAll(metaLanguage.getMatchingLanguages());
            }
            else {
                result.add(language);
            }
        }
        return result;
    }
}
