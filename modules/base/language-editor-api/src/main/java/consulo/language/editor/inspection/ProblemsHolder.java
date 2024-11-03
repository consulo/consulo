/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.language.editor.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.List;

/**
 * @author max
 */
public interface ProblemsHolder {
    @RequiredReadAction
    default void registerProblem(
        @Nonnull PsiElement psiElement,
        @Nonnull @Nls(capitalization = Nls.Capitalization.Sentence) String descriptionTemplate,
        @Nullable LocalQuickFix... fixes
    ) {
        registerProblem(psiElement, descriptionTemplate, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
    }

    @RequiredReadAction
    void registerProblem(
        @Nonnull PsiElement psiElement,
        @Nonnull @Nls(capitalization = Nls.Capitalization.Sentence) String descriptionTemplate,
        @Nonnull ProblemHighlightType highlightType,
        @Nullable LocalQuickFix... fixes
    );

    @RequiredReadAction
    void registerProblem(@Nonnull ProblemDescriptor problemDescriptor);

    @RequiredReadAction
    void registerProblem(@Nonnull PsiReference reference, String descriptionTemplate, ProblemHighlightType highlightType);

    @RequiredReadAction
    void registerProblemForReference(
        @Nonnull PsiReference reference,
        @Nonnull ProblemHighlightType highlightType,
        @Nonnull String descriptionTemplate,
        @Nullable LocalQuickFix... fixes
    );

    @RequiredReadAction
    void registerProblem(@Nonnull PsiReference reference);

    /**
     * Creates highlighter for the specified place in the file.
     *
     * @param psiElement     The highlighter will be created at the text range od this element. This psiElement must be in the current file.
     * @param message        Message for this highlighter. Will also serve as a tooltip.
     * @param highlightType  The level of highlighter.
     * @param rangeInElement The (sub)range (must be inside (0..psiElement.getTextRange().getLength()) to create highlighter in.
     *                       If you want to highlight only part of the supplied psiElement. Pass null otherwise.
     * @param fixes          (Optional) fixes to appear for this highlighter.
     */
    void registerProblem(
        @Nonnull PsiElement psiElement,
        @Nonnull String message,
        @Nonnull ProblemHighlightType highlightType,
        @Nullable TextRange rangeInElement,
        @Nullable LocalQuickFix... fixes
    );

    void registerProblem(
        @Nonnull final PsiElement psiElement,
        @Nullable TextRange rangeInElement,
        @Nonnull String message,
        @Nullable LocalQuickFix... fixes
    );

    @Nonnull
    List<ProblemDescriptor> getResults();

    @Nonnull
    default ProblemDescriptor[] getResultsArray() {
        final List<ProblemDescriptor> problems = getResults();
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    @Nonnull
    InspectionManager getManager();

    boolean hasResults();

    int getResultCount();

    boolean isOnTheFly();

    @Nonnull
    PsiFile getFile();

    @Nonnull
    Project getProject();

    @Nonnull
    @RequiredReadAction
    static LocalizeValue unresolvedReferenceMessage(@Nonnull PsiReference reference) {
        if (reference instanceof EmptyResolveMessageProvider resolveMessageProvider) {
            return resolveMessageProvider.buildUnresolvedMessage(reference.getCanonicalText());
        }
        else {
            return CodeInsightLocalize.errorCannotResolveDefaultMessage(reference.getCanonicalText());
        }
    }
}
