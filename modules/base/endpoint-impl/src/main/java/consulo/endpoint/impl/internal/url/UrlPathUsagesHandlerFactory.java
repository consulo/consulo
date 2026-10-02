// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.url.reference.UrlSegmentReferenceTarget;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.language.psi.PsiElement;

@ExtensionImpl
public final class UrlPathUsagesHandlerFactory extends FindUsagesHandlerFactory {
    @Override
    public boolean canFindUsages(PsiElement element) {
        return element instanceof UrlSegmentReferenceTarget;
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
        return new FindUsagesHandler(element) {
            @Override
            public PsiElement[] getPrimaryElements() {
                return new PsiElement[]{getPsiElement()};
            }
        };
    }
}
