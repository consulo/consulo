// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiNamedElement;
import consulo.localize.LocalizeValue;

import java.util.Objects;

/**
 * Data representing suggested Rename refactoring.
 */
public final class SuggestedRenameData extends SuggestedRefactoringData {
    private final PsiNamedElement myDeclaration;
    private final String myOldName;

    public SuggestedRenameData(PsiNamedElement declaration, String oldName) {
        myDeclaration = declaration;
        myOldName = oldName;
    }

    @Override
    public PsiNamedElement getDeclaration() {
        return myDeclaration;
    }

    public String getOldName() {
        return myOldName;
    }

    @RequiredReadAction
    public String getNewName() {
        return Objects.requireNonNull(myDeclaration.getName());
    }

    @Override
    public LocalizeValue getIntentionText() {
        return RefactoringLocalize.suggestedRefactoringRenameIntentionText(myOldName);
    }
}
