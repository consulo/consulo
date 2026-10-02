// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.annotation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;

import java.util.List;

/**
 * Implemented by a plugin to add annotations to references inside {@link consulo.language.psi.PsiLanguageInjectionHost},
 * {@link consulo.language.psi.HintedReferenceHost} and {@link consulo.language.psi.ContributedReferenceHost} elements.
 * <p>
 * Typical usage is highlighting URL references inside String literals so that they are discoverable by users,
 * e.g. <code>URI.create("/api/example")</code> where {@code /api/example} is underlined by such annotator to show references
 * in unusual place.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ContributedReferencesAnnotator extends LanguageExtension {
    ExtensionPointCacheKey<ContributedReferencesAnnotator, ByLanguageValue<List<ContributedReferencesAnnotator>>> KEY =
        ExtensionPointCacheKey.create("ContributedReferencesAnnotator", LanguageOneToMany.build(true));

    static List<ContributedReferencesAnnotator> allForLanguageOrAny(Language language) {
        return Application.get().getExtensionPoint(ContributedReferencesAnnotator.class).getOrBuildCache(KEY).requiredGet(language);
    }

    /**
     * Annotates the specified PSI element of one of types: {@link consulo.language.psi.PsiLanguageInjectionHost},
     * {@link consulo.language.psi.HintedReferenceHost} or {@link consulo.language.psi.ContributedReferenceHost}.
     *
     * @param element    to annotate.
     * @param references references of the element.
     * @param holder     the container which receives annotations created by the plugin.
     */
    void annotate(PsiElement element, List<PsiReference> references, AnnotationHolder holder);
}
