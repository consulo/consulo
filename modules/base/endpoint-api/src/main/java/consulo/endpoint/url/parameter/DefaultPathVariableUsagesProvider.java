// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;

import java.util.List;

public class DefaultPathVariableUsagesProvider implements PathVariableUsagesProvider {
    @Override
    public Iterable<LookupElement> getCompletionVariantsForDeclaration(PsiElement context) {
        return List.of();
    }

    @Override
    public Iterable<PsiElement> findSemDefiningElements(PathVariablePomTarget pomTarget) {
        return List.of();
    }
}
