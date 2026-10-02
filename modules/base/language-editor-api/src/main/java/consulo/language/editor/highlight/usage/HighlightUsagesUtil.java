// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.highlight.usage;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.IdentifierUtil;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.pom.PsiDeclaredTarget;
import consulo.language.psi.ExternallyAnnotated;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

public final class HighlightUsagesUtil {
    private HighlightUsagesUtil() {
    }

    /**
     * @return range (in the host file) to be highlighted by {@code IdentifierHighlighterPass} for this element
     */
    @RequiredReadAction
    public static @Nullable TextRange getNameIdentifierRange(PsiFile psiFile, PsiElement element) {
        InjectedLanguageManager injectedManager = InjectedLanguageManager.getInstance(psiFile.getProject());
        Pair<PsiElement, TextRange> pair = getNameIdentifierRangeInCurrentRoot(psiFile, element);
        if (pair == null) {
            return null;
        }
        return injectedManager.injectedToHost(pair.getFirst(), pair.getSecond());
    }

    /**
     * @return range (in the current containing file) to be highlighted by {@code IdentifierHighlighterPass} for this element,
     * and the context element for this range
     */
    @RequiredReadAction
    public static @Nullable Pair<PsiElement, TextRange> getNameIdentifierRangeInCurrentRoot(PsiFile psiFile, PsiElement element) {
        if (element instanceof PomTargetPsiElement pomTargetPsiElement) {
            PomTarget target = pomTargetPsiElement.getTarget();
            if (target instanceof PsiDeclaredTarget declaredTarget) {
                TextRange range = declaredTarget.getNameIdentifierRange();
                if (range != null) {
                    if (range.getStartOffset() < 0 || range.getLength() <= 0) {
                        return null;
                    }
                    PsiElement navElement = declaredTarget.getNavigationElement();
                    if (PsiUtilBase.isUnderPsiRoot(psiFile, navElement)) {
                        return Pair.create(navElement, range.shiftRight(navElement.getTextRange().getStartOffset()));
                    }
                }
            }
        }

        if (!PsiUtilBase.isUnderPsiRoot(psiFile, element)) {
            return null;
        }

        PsiElement identifier = IdentifierUtil.getNameIdentifier(element);
        if (identifier != null && PsiUtilBase.isUnderPsiRoot(psiFile, identifier)) {
            TextRange range = identifier instanceof ExternallyAnnotated externallyAnnotated
                ? externallyAnnotated.getAnnotationRegion()
                : identifier.getTextRange();
            return range == null ? null : Pair.create(identifier, range);
        }
        return null;
    }
}
