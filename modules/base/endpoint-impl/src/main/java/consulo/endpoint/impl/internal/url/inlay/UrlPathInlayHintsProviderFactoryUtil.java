// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.application.Application;
import consulo.endpoint.url.inlay.UrlPathInlayLanguagesProvider;
import consulo.language.Language;
import org.jspecify.annotations.Nullable;

public final class UrlPathInlayHintsProviderFactoryUtil {
    private UrlPathInlayHintsProviderFactoryUtil() {
    }

    public static @Nullable UrlPathInlayLanguagesProvider getLanguagesProviderByLanguage(Language language) {
        return Application.get()
            .getExtensionPoint(UrlPathInlayLanguagesProvider.class)
            .findFirstSafe(it -> UrlPathDeclarativeInlayHintsProviderFactory.expand(it.getLanguages()).contains(language));
    }
}
