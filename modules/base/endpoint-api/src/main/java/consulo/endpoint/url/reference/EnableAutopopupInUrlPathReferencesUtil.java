// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReference;
import org.jspecify.annotations.Nullable;

public final class EnableAutopopupInUrlPathReferencesUtil {
    private EnableAutopopupInUrlPathReferencesUtil() {
    }

    @RequiredReadAction
    public static boolean hasUsageUrlPathReferences(PsiElement contextElement, int offset) {
        @Nullable PsiElement referenceHost = getReferenceHost(contextElement);
        if (referenceHost == null) {
            return false;
        }
        PsiReference[] references = referenceHost.getReferences();

        for (PsiReference reference : references) {
            if (reference instanceof UrlPathReference urlPathReference && urlPathReference.getContext().isDeclaration()) {
                return false;
            }
        }

        for (PsiReference reference : references) {
            if (!(reference instanceof UrlSegmentReference)) {
                continue;
            }
            if (reference.getAbsoluteRange().containsOffset(offset)) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    private static @Nullable PsiElement getReferenceHost(PsiElement contextElement) {
        @Nullable PsiElement current = contextElement;
        for (int i = 0; i < 3 && current != null; i++) {
            if (current instanceof PsiLanguageInjectionHost) {
                return current;
            }
            current = current instanceof PsiFile ? null : current.getParent();
        }
        return null;
    }
}
