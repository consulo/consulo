// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.query.Plow;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.refactoring.rename.NameSuggestionProvider;
import consulo.language.editor.refactoring.rename.SuggestedNameInfo;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@ExtensionImpl
public final class RenameableSemElementNameSuggestionProvider implements NameSuggestionProvider {
    @Override
    public @Nullable SuggestedNameInfo getSuggestedNames(PsiElement element,
                                                         @Nullable PsiElement nameSuggestionContext,
                                                         Set<String> result) {
        Plow<LookupElement> nameVariants = RenameableSemElementCompletionContributorUtil.getParameterNameVariants(element);
        if (nameVariants != null) {
            nameVariants.map(LookupElement::getLookupString).collectTo(result);
        }
        return null;
    }
}
