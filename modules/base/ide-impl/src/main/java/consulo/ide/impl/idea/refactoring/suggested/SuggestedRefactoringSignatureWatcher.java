// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport;
import consulo.language.psi.PsiElement;

public interface SuggestedRefactoringSignatureWatcher {
    void editingStarted(PsiElement declaration, SuggestedRefactoringSupport refactoringSupport);

    void nextSignature(PsiElement anchor, SuggestedRefactoringSupport refactoringSupport);

    void inconsistentState();

    void reset();
}
