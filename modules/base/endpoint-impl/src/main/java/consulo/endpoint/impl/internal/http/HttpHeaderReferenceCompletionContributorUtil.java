// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.http;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.http.HttpHeaderReference;
import consulo.endpoint.http.HttpMethodReference;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReference;
import org.jspecify.annotations.Nullable;

final class HttpHeaderReferenceCompletionContributorUtil {
    private HttpHeaderReferenceCompletionContributorUtil() {
    }

    @RequiredReadAction
    static boolean hasHttpReferences(PsiElement contextElement, int offset) {
        PsiElement referenceHost = getReferenceHost(contextElement);
        if (referenceHost == null) {
            return false;
        }
        PsiReference[] references = referenceHost.getReferences();

        for (PsiReference reference : references) {
            if (!(reference instanceof HttpHeaderReference) && !(reference instanceof HttpMethodReference)) {
                continue;
            }
            if (reference.getAbsoluteRange().containsOffset(offset)) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    static @Nullable PsiElement getReferenceHost(PsiElement contextElement) {
        PsiElement current = contextElement;
        for (int i = 0; i < 3 && current != null; i++) {
            if (current instanceof PsiLanguageInjectionHost) {
                return current;
            }
            current = current instanceof PsiFile ? null : current.getParent();
        }
        return null;
    }
}
