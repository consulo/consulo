// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Provides simple text inlays (info elements rendered inside source code) for a given language.
 * The order of hints which share the same offset is not guaranteed.
 *
 * @see InlayHintsProvider for more interactive inlays.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface InlayParameterHintsProvider extends LanguageExtension {
    ExtensionPointCacheKey<InlayParameterHintsProvider, ByLanguageValue<InlayParameterHintsProvider>> KEY =
        ExtensionPointCacheKey.create("InlayParameterHintsProvider", LanguageOneToOne.build());

    @Nullable
    static InlayParameterHintsProvider forLanguage(@Nonnull Language language) {
        return Application.get().getExtensionPoint(InlayParameterHintsProvider.class).getOrBuildCache(KEY).get(language);
    }

    /**
     * Hints for params to be shown
     */
    @Nonnull
    List<InlayInfo> getParameterHints(@Nonnull PsiElement element);

    /**
     * Provides fully qualified method name (e.g. "java.util.Map.put") and list of it's parameter names.
     * Used to obtain method information when adding it to blacklist
     */
    @Nullable
    MethodInfo getMethodInfo(@Nonnull PsiElement element);

    /**
     * Default list of patterns for which hints should not be shown
     */
    @Nonnull
    Set<String> getDefaultBlackList();

    /**
     * Returns language which blacklist will be appended to the resulting one
     * E.g. to prevent possible Groovy and Kotlin extensions from showing hints for blacklisted java methods.
     */
    @Nullable
    default Language getBlackListDependencyLanguage() {
        return null;
    }

    /**
     * Customise hints presentation
     */
    @Nonnull
    default String getInlayPresentation(@Nonnull String inlayText) {
        return inlayText + ":";
    }
}
