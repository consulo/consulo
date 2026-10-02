// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.internal.UrlPathInlayActionService;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.language.editor.inlay.DeclarativePresentationTreeBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;

public final class PsiElementUrlPathInlayHint implements UrlPathInlayHint {
    private final UrlPathContext myContext;
    private final int myOffset;
    private final SmartPsiElementPointer<PsiElement> myAttachedTo;

    @RequiredReadAction
    public PsiElementUrlPathInlayHint(PsiElement psiElement, UrlPathContext context) {
        myContext = context;
        myOffset = psiElement.getTextRange().getStartOffset();
        myAttachedTo = SmartPointerManager.createPointer(psiElement);
    }

    @Override
    public UrlPathContext getContext() {
        return myContext;
    }

    @Override
    public int getOffset() {
        return myOffset;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Style getStyle() {
        return Style.INLINE;
    }

    @Override
    public SmartPsiElementPointer<PsiElement> getAttachedTo() {
        return myAttachedTo;
    }

    @Override
    public void buildPresentation(DeclarativePresentationTreeBuilder builder) {
        UrlPathInlayActionService.getInstance().buildPresentation(builder);
    }
}
