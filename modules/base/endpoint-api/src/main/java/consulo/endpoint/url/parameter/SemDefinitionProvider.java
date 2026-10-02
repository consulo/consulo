// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.language.psi.PsiElement;

public interface SemDefinitionProvider {
    /**
     * Implements the PathVariable find-usages among non-literal elements (usually PsiParameters).
     *
     * @return {@link PsiElement}s for which the {@link PathVariableSem} that corresponds to the {@code pomTarget}
     * is defined via the {@link consulo.language.sem.SemService}
     */
    Iterable<PsiElement> findSemDefiningElements(PathVariablePomTarget pomTarget);
}
