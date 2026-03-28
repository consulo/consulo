// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.codeVision;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.SyntaxTraverser;

/**
 * Abstract base for languages with curly-bracket block syntax.
 * <p>
 * Refines {@link VcsCodeVisionLanguageContext#computeEffectiveRange} by trimming
 * trailing right braces and whitespace so the code-author lens appears before
 * the closing brace rather than at the very end of the element.
 */
@UsedInPlugin
public abstract class VcsCodeVisionCurlyBracketLanguageContext implements VcsCodeVisionLanguageContext {

    /**
     * @return true iff {@code element} is a right-brace token in this language
     */
    protected abstract boolean isRBrace(PsiElement element);

    @Override
    @RequiredReadAction
    public TextRange computeEffectiveRange(PsiElement element) {
        int startOffset = VcsCodeVisionLanguageContext.super.computeEffectiveRange(element).getStartOffset();
        return TextRange.create(startOffset, computeEffectiveEndOffset(element));
    }

    @RequiredReadAction
    private int computeEffectiveEndOffset(PsiElement element) {
        PsiElement lastChild = element.getLastChild();
        if (lastChild == null) return element.getTextRange().getEndOffset();
        PsiElement end = SyntaxTraverser.psiApiReversed().children(lastChild)
            .find(child -> !(child instanceof PsiWhiteSpace) && !isRBrace(child));
        if (end == null) end = element;
        return end.getTextRange().getEndOffset();
    }
}
