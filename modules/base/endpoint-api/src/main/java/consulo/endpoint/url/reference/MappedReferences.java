// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;

public interface MappedReferences {
    MappedReferences EMPTY = new MappedReferences() {
        @Override
        public PsiReference[] forPsiElement(PsiElement host) {
            return PsiReference.EMPTY_ARRAY;
        }

        @Override
        public MappedReferences withRootContextProvider(UrlPathContext rooContextProvider) {
            return this;
        }
    };

    PsiReference[] forPsiElement(PsiElement host);

    MappedReferences withRootContextProvider(UrlPathContext rooContextProvider);
}
