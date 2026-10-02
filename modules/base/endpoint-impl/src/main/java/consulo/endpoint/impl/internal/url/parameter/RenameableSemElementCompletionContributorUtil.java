// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.application.util.query.Plow;
import consulo.endpoint.url.parameter.RenameableSemElement;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.sem.SemService;
import org.jspecify.annotations.Nullable;

final class RenameableSemElementCompletionContributorUtil {
    private RenameableSemElementCompletionContributorUtil() {
    }

    static @Nullable Plow<LookupElement> getParameterNameVariants(PsiElement position) {
        SemService semService = SemService.getSemService(position.getProject());
        PsiElement element = position;
        for (int i = 0; i < 2 && element != null; i++) {
            RenameableSemElement semElement = semService.getSemElement(RenameableSemElement.RENAMEABLE_SEM_KEY, element);
            if (semElement != null) {
                return semElement.getNameVariants();
            }
            element = element instanceof PsiFile ? null : element.getParent();
        }
        return null;
    }
}
