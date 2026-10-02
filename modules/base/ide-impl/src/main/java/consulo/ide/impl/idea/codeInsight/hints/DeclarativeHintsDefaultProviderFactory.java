// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.Language;
import consulo.language.MetaLanguage;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.editor.inlay.DeclarativeInlayHintsProviderFactory;
import consulo.language.editor.inlay.InlayProviderInfo;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ExtensionImpl
public class DeclarativeHintsDefaultProviderFactory implements DeclarativeInlayHintsProviderFactory {
    private final Application myApplication;

    @Inject
    public DeclarativeHintsDefaultProviderFactory(Application application) {
        myApplication = application;
    }

    @Override
    public List<InlayProviderInfo> getProvidersForLanguage(Language language) {
        List<InlayProviderInfo> result = new ArrayList<>();
        myApplication.getExtensionPoint(DeclarativeInlayHintsProvider.class).forEach(provider -> {
            if (isApplicable(provider.getLanguage(), language)) {
                result.add(new InlayProviderInfo(
                    provider,
                    provider.getId(),
                    provider.getOptions(),
                    provider.isEnabledByDefault(),
                    provider.getName()
                ));
            }
        });
        return result;
    }

    @Override
    public Set<Language> getSupportedLanguages() {
        Set<Language> languages = new HashSet<>();
        myApplication.getExtensionPoint(DeclarativeInlayHintsProvider.class).forEach(provider -> languages.add(provider.getLanguage()));
        return languages;
    }

    @Override
    public @Nullable InlayProviderInfo getProviderInfo(Language language, String providerId) {
        for (InlayProviderInfo info : getProvidersForLanguage(language)) {
            if (Objects.equals(info.providerId(), providerId)) {
                return info;
            }
        }
        return null;
    }

    private static boolean isApplicable(Language providerLanguage, Language language) {
        if (language.isKindOf(providerLanguage)) {
            return true;
        }
        if (!(providerLanguage instanceof MetaLanguage metaLanguage)) {
            return false;
        }
        return metaLanguage.matchesLanguage(language);
    }
}
