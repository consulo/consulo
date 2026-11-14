// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * A language-specific completion contributor that should provide a reasonable number of symbols declared in a given file, e.g.,
 * only top-level declarations.
 * Such contributors can also help to complete words in plain text editors like VCS commit messages.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PlainTextSymbolCompletionContributor extends LanguageExtension {
    ExtensionPointCacheKey<PlainTextSymbolCompletionContributor, ByLanguageValue<PlainTextSymbolCompletionContributor>> KEY =
        ExtensionPointCacheKey.create("PlainTextSymbolCompletionContributor", LanguageOneToOne.build());

    static @Nullable PlainTextSymbolCompletionContributor forLanguage(Language language) {
        return Application.get().getExtensionPoint(PlainTextSymbolCompletionContributor.class).getOrBuildCache(KEY).get(language);
    }

    /**
     * Collects lookup elements from a given file.
     *
     * @param file            file for extracting lookup elements.
     * @param invocationCount number of times the completion was invoked (see {@link CompletionParameters#getInvocationCount()}).
     * @param prefix          prefix string that can be used to filter candidates.
     *                        It's not required to return only matches starting with prefix, but it can improve performance.
     * @return collection of suggestions used for completion.
     */
    @RequiredReadAction
    Collection<LookupElement> getLookupElements(PsiFile file, int invocationCount, String prefix);
}
