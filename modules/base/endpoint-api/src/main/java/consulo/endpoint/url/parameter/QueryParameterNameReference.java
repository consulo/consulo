// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.endpoint.url.reference.UrlSegmentReference;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.util.PartiallyKnownStringUtil;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CancellationException;

public final class QueryParameterNameReference extends PsiReferenceBase<PsiElement> implements UrlSegmentReference {
    private final UrlPathContext myContext;
    private final boolean myForceFindUsagesOnNavigate;
    private final LazyValue<Optional<QueryParameterNameTarget>> myQueryParameterPomTarget;

    public QueryParameterNameReference(UrlPathContext context, PsiLanguageInjectionHost host) {
        this(context, host, ElementManipulators.getValueTextRange(host));
    }

    public QueryParameterNameReference(UrlPathContext context, PsiLanguageInjectionHost host, TextRange rangeInElement) {
        this(context, host, rangeInElement, false);
    }

    public QueryParameterNameReference(UrlPathContext context,
                                       PsiLanguageInjectionHost host,
                                       TextRange rangeInElement,
                                       boolean forceFindUsagesOnNavigate) {
        super(host, rangeInElement, false);
        myContext = context;
        myForceFindUsagesOnNavigate = forceFindUsagesOnNavigate;
        myQueryParameterPomTarget = LazyValue.atomicNotNull(() -> Optional.ofNullable(createQueryParameterPomTarget()));
    }

    public UrlPathContext getContext() {
        return myContext;
    }

    @RequiredReadAction
    private @Nullable QueryParameterNameTarget createQueryParameterPomTarget() {
        if (!myContext.getResolveRequests().iterator().hasNext()) {
            return null;
        }

        return Application.get().getInstance(HttpReferenceService.class)
            .createQueryParameterNameTarget(myContext, getValue(), getElement().getProject());
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        @Nullable QueryParameterNameTarget target = myQueryParameterPomTarget.get().orElse(null);
        return target != null ? target.toElement(myForceFindUsagesOnNavigate) : null;
    }

    @Override
    public String getValue() {
        try {
            return super.getValue();
        }
        catch (RuntimeException e) {
            if (e instanceof ControlFlowException || e instanceof CancellationException) {
                throw e;
            }
            throw new RuntimeExceptionWithAttachments(e, PartiallyKnownStringUtil.mkAttachments(getElement()));
        }
    }

    @Override
    @RequiredReadAction
    public Object[] getVariants() {
        return Application.get().getInstance(HttpReferenceService.class).getQueryParameterNameVariants(myQueryParameterPomTarget.get().orElse(null));
    }
}
