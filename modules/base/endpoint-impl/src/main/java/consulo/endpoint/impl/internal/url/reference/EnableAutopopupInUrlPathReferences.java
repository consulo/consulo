// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.url.reference.EnableAutopopupInUrlPathReferencesUtil;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionConfidence;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ThreeState;

@ExtensionImpl(id = "enableAutopopupInUrlReferences", order = "before javaSkipAutopopupInStrings")
public final class EnableAutopopupInUrlPathReferences extends CompletionConfidence {
    @Override
    @RequiredReadAction
    public ThreeState shouldSkipAutopopup(PsiElement contextElement, PsiFile psiFile, int offset) {
        if (EnableAutopopupInUrlPathReferencesUtil.hasUsageUrlPathReferences(contextElement, offset)) {
            return ThreeState.NO;
        }

        return ThreeState.UNSURE;
    }

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }
}
