// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.suggested;

import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;

/**
 * Data representing suggested refactoring that can be performed.
 *
 * @see SuggestedRenameData
 * @see SuggestedChangeSignatureData
 */
public sealed abstract class SuggestedRefactoringData permits SuggestedRenameData, SuggestedChangeSignatureData {
    public abstract PsiElement getDeclaration();

    public abstract LocalizeValue getIntentionText();
}
