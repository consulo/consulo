// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.SearchScope;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.UseScopeEnlarger;
import consulo.language.psi.scope.GlobalSearchScope;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public final class RenameableSemElementUseScopeEnlarger implements UseScopeEnlarger {
    @Override
    public @Nullable SearchScope getAdditionalUseScope(PsiElement element) {
        PomTargetPsiElement pomTargetPsiElement = SemElementRenamePsiElementProcessorUtil.provide(
            support -> SemElementRenamePsiElementProcessorUtil.createPomTargetFromSemElement(support, element)
        );
        PsiFile containingFile = element.getContainingFile();
        if (pomTargetPsiElement != null && containingFile != null) {
            return GlobalSearchScope.fileScope(containingFile);
        }
        return null;
    }
}
