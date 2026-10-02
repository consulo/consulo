// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.http;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.endpoint.internal.EndpointStringUtil;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.UrlConstants;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import org.jspecify.annotations.Nullable;

public class HttpMethodReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {
    public HttpMethodReference(PsiElement element, TextRange range) {
        super(element, range);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        String value = getValue();
        if (EndpointStringUtil.isBlank(value)) {
            return null;
        }

        return Application.get().getInstance(HttpReferenceService.class).resolveHttpMethod(getElement(), value, getRangeInElement());
    }

    @Override
    public LocalizeValue buildUnresolvedMessage(String referenceText) {
        return EndpointLocalize.httpMethodElementError();
    }

    @Override
    @RequiredReadAction
    public boolean isReferenceTo(PsiElement element) {
        return Application.get().getInstance(HttpReferenceService.class).isReferenceToHttpMethod(element, getValue());
    }

    @Override
    @RequiredReadAction
    public Object[] getVariants() {
        return UrlConstants.HTTP_METHODS.stream()
            .map(it -> LookupElementBuilder.create(it).withIcon(PlatformIconGroup.nodesPpweb()))
            .toArray();
    }
}
