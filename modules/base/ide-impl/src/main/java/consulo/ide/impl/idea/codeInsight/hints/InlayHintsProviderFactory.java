// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.application.Application;
import consulo.language.Language;
import consulo.language.editor.inlay.InlayHintsProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TODO rewrite !
 */
public class InlayHintsProviderFactory {
    /**
     * @return list of potentially available providers for a particular language (not filtering enabled ones)
     */
    @Nonnull
    static List<InlayProviderInfo> findProvidersForLanguage(Language language) {
        List<InlayProviderInfo> result = new ArrayList<>();
        for (InlayHintsProvider inlayHintsProvider : Application.get().getExtensionList(InlayHintsProvider.class)) {
            if (language.isKindOf(inlayHintsProvider.getLanguage())) {
                result.add(new InlayProviderInfo(inlayHintsProvider,
                    inlayHintsProvider.getProviderId(),
                    inlayHintsProvider.getOptions(),
                    inlayHintsProvider.isEnabledByDefault(),
                    inlayHintsProvider.getProviderName()
                ));
            }
        }
        return result;
    }

    @Nullable
    static InlayProviderInfo findProviderInfo(Language language, String providerId) {
        List<InlayProviderInfo> infos = findProvidersForLanguage(language);

        for (InlayProviderInfo info : infos) {
            if (Objects.equals(info.getProviderId(), providerId)) {
                return info;
            }
        }
        return null;
    }
}
