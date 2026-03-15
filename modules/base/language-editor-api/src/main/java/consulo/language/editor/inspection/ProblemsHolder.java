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

import consulo.annotation.DeprecationInfo;
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
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author max
 */
public interface ProblemsHolder {
    
    ProblemBuilder newProblem(LocalizeValue descriptionTemplate);

    @Deprecated
    @DeprecationInfo("Use #newProblem()...create()")
    @RequiredReadAction
    default void registerProblem(
        PsiElement psiElement,
        String descriptionTemplate,
        @Nullable LocalQuickFix... fixes
    ) {
        newProblem(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblem()...create()")
    @RequiredReadAction
    default void registerProblem(
        PsiElement psiElement,
        String descriptionTemplate,
        ProblemHighlightType highlightType,
        @Nullable LocalQuickFix... fixes
    ) {
        newProblem(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .withFixes(fixes)
            .highlightType(highlightType)
            .create();
    }

    @RequiredReadAction
    void registerProblem(ProblemDescriptor problemDescriptor);

    @RequiredReadAction
    void registerProblem(PsiReference reference, String descriptionTemplate, ProblemHighlightType highlightType);

    @Deprecated
    @DeprecationInfo("Use #newProblem()...create()")
    @RequiredReadAction
    default void registerProblemForReference(
        PsiReference reference,
        ProblemHighlightType highlightType,
        String descriptionTemplate,
        @Nullable LocalQuickFix... fixes
    ) {
        newProblem(LocalizeValue.of(descriptionTemplate))
            .rangeByRef(reference)
            .highlightType(highlightType)
            .withFixes(fixes)
            .create();
    }

    @RequiredReadAction
    void registerProblem(PsiReference reference);

    /**
     * Creates highlighter for the specified place in the file.
     *
     * @param psiElement          The highlighter will be created at the text range od this element.
     *                            This psiElement must be in the current file.
     * @param descriptionTemplate Message for this highlighter. Will also serve as a tooltip.
     * @param highlightType       The level of highlighter.
     * @param rangeInElement      The (sub)range (must be inside (0..psiElement.getTextRange().getLength()) to create highlighter in.
     *                            If you want to highlight only part of the supplied psiElement. Pass null otherwise.
     * @param fixes               (Optional) fixes to appear for this highlighter.
     */
    @Deprecated
    @DeprecationInfo("Use #newProblem()...create()")
    @RequiredReadAction
    default void registerProblem(
        PsiElement psiElement,
        String descriptionTemplate,
        ProblemHighlightType highlightType,
        @Nullable TextRange rangeInElement,
        @Nullable LocalQuickFix... fixes
    ) {
        newProblem(LocalizeValue.of(descriptionTemplate))
            .range(psiElement, rangeInElement)
            .withFixes(fixes)
            .highlightType(highlightType)
            .create();
    }

    @Deprecated
    @RequiredReadAction
    default void registerProblem(
        PsiElement psiElement,
        @Nullable TextRange rangeInElement,
        String descriptionTemplate,
        @Nullable LocalQuickFix... fixes
    ) {
        newProblem(LocalizeValue.of(descriptionTemplate))
            .range(psiElement, rangeInElement)
            .withFixes(fixes)
            .create();
    }

    
    List<ProblemDescriptor> getResults();

    
    default ProblemDescriptor[] getResultsArray() {
        List<ProblemDescriptor> problems = getResults();
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    
    InspectionManager getManager();

    boolean hasResults();

    int getResultCount();

    boolean isOnTheFly();

    
    PsiFile getFile();

    
    Project getProject();

    
    @RequiredReadAction
    static LocalizeValue unresolvedReferenceMessage(PsiReference reference) {
        if (reference instanceof EmptyResolveMessageProvider resolveMessageProvider) {
            return resolveMessageProvider.buildUnresolvedMessage(reference.getCanonicalText());
        }
        else {
            return CodeInsightLocalize.errorCannotResolveDefaultMessage(reference.getCanonicalText());
        }
    }
}
