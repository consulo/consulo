// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.language.psi.PsiElement;
import consulo.language.sem.SemService;
import org.jspecify.annotations.Nullable;

public final class RenameableSemElementUtil {
    private RenameableSemElementUtil() {
    }

    public static <T extends RenameableSemElement> @Nullable T getSemElement(RenameableSemElementSupport<T> support, PsiElement element) {
        return SemService.getSemService(element.getProject()).getSemElement(support.getSemKey(), element);
    }
}
