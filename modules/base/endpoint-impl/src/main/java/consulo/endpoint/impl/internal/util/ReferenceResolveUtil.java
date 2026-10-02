// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ResolveResult;
import consulo.localize.LocalizeValue;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public final class ReferenceResolveUtil {
    private ReferenceResolveUtil() {
    }

    public static <T> LazyValue<T> lazySynchronousResolve(LocalizeValue statusMessage, Supplier<T> provider) {
        return LazyValue.notNull(() -> {
            if (Application.get().isDispatchThread()) {
                ThrowableSupplier<T, RuntimeException> readAction = () -> ReadAction.<T, RuntimeException>compute(provider::get);
                return ProgressManager.getInstance().<T, RuntimeException>runProcessWithProgressSynchronously(
                    readAction,
                    statusMessage,
                    true,
                    null
                );
            }
            else {
                return provider.get();
            }
        });
    }

    @RequiredReadAction
    public static @Nullable UrlPathReference getLastUrlPathReference(PsiElement psiElement) {
        List<UrlPathReference> references = new ArrayList<>();
        for (PsiReference reference : psiElement.getReferences()) {
            if (reference instanceof UrlPathReference urlPathReference) {
                references.add(urlPathReference);
            }
        }
        references.sort(Comparator.comparingInt(it -> it.getRangeInElement().getEndOffset()));
        for (UrlPathReference reference : references) {
            if (reference.isAtEnd()) {
                return reference;
            }
        }
        return null;
    }

    @RequiredReadAction
    public static @Nullable PsiElement getFakePomTargetForLastUrlPathReference(PsiElement psiElement) {
        @Nullable UrlPathReference reference = getLastUrlPathReference(psiElement);
        if (reference == null) {
            return null;
        }
        ResolveResult[] results = reference.multiResolve(false);
        if (results.length == 0) {
            return null;
        }
        return results[0].getElement();
    }
}
