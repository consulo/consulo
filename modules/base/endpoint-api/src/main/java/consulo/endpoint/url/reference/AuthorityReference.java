// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.ResolveResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public final class AuthorityReference extends PsiReferenceBase.Poly<PsiElement> implements UrlSegmentReference {
    private final @Nullable String myGivenValue;
    private final @Nullable Consumer<UrlSegmentReference> myCustomNavigate;

    public AuthorityReference(@Nullable String givenValue, PsiLanguageInjectionHost host, TextRange range) {
        this(givenValue, host, range, null);
    }

    public AuthorityReference(
        @Nullable String givenValue,
        PsiLanguageInjectionHost host,
        TextRange range,
        @Nullable Consumer<UrlSegmentReference> customNavigate
    ) {
        super(host, range, false);
        myGivenValue = givenValue;
        myCustomNavigate = customNavigate;
    }

    public @Nullable String getGivenValue() {
        return myGivenValue;
    }

    public @Nullable Consumer<UrlSegmentReference> getCustomNavigate() {
        return myCustomNavigate;
    }

    @Override
    @RequiredReadAction
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(resolve());
    }

    @Override
    @RequiredReadAction
    public PsiElement resolve() {
        return HttpReferenceService.getInstance()
            .resolveAuthorityReference(this, getElement(), myGivenValue != null ? myGivenValue : getValue(), myCustomNavigate);
    }

    @Override
    public String toString() {
        String refValue = myGivenValue != null ? myGivenValue : getValue();
        return "AuthorityReference(" + refValue + ", " + getRangeInElement() + ")";
    }
}
