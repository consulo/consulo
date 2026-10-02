// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.http;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

public class HttpHeaderReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {
    private static final Pattern HEADER_NAME_PATTERN = Pattern.compile("[^:\\r\\n]+");

    public HttpHeaderReference(PsiElement element, TextRange range) {
        super(element, range);
    }

    @Override
    public LocalizeValue buildUnresolvedMessage(String referenceText) {
        return EndpointLocalize.httpHeaderElementError();
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        String value = getValue();
        if (!HEADER_NAME_PATTERN.matcher(value).matches()) {
            return null;
        }

        return Application.get().getInstance(HttpReferenceService.class).resolveHeaderReference(getElement(), value);
    }

    @Override
    @RequiredReadAction
    public boolean isReferenceTo(PsiElement element) {
        return Application.get().getInstance(HttpReferenceService.class).isReferenceToHeaderElement(element, getValue());
    }

    public static PsiReference[] forElement(PsiElement injectionHost) {
        return forElement(injectionHost, ElementManipulators.getValueTextRange(injectionHost));
    }

    public static PsiReference[] forElement(PsiElement injectionHost, TextRange range) {
        return new PsiReference[]{new HttpHeaderReference(injectionHost, range)};
    }
}
