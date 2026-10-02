// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor;
import consulo.language.psi.PsiElement;

import java.util.Map;

@ExtensionImpl
public final class SemElementRenamePsiElementProcessor extends RenamePsiElementProcessor {
    @Override
    public boolean canProcessElement(PsiElement element) {
        return SemElementRenamePsiElementProcessorUtil.supportedElement(element);
    }

    @Override
    public void prepareRenaming(PsiElement element, String newName, Map<PsiElement, String> allRenames) {
        for (PsiElement companion : SemElementRenamePsiElementProcessorUtil.getCompanions(element)) {
            allRenames.put(companion, newName);
        }
    }
}
