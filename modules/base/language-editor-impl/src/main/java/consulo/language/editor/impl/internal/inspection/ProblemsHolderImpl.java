/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.impl.internal.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.ExternallyDefinedPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-11-03
 */
public class ProblemsHolderImpl implements ProblemsHolder {
    private final InspectionManager myManager;
    private final PsiFile myFile;
    private final boolean myOnTheFly;
    private final List<ProblemDescriptor> myProblems = new ArrayList<>();

    public ProblemsHolderImpl(@Nonnull InspectionManager manager, @Nonnull PsiFile file, boolean onTheFly) {
        myManager = manager;
        myFile = file;
        myOnTheFly = onTheFly;
    }

    @RequiredReadAction
    @Override
    public void registerProblem(
        @Nonnull PsiElement psiElement,
        @Nonnull @Nls(capitalization = Nls.Capitalization.Sentence) String descriptionTemplate,
        @Nonnull ProblemHighlightType highlightType,
        @Nullable LocalQuickFix... fixes
    ) {
        registerProblem(myManager.createProblemDescriptor(psiElement, descriptionTemplate, myOnTheFly, fixes, highlightType));
    }

    @Override
    @RequiredReadAction
    public void registerProblem(@Nonnull ProblemDescriptor problemDescriptor) {
        PsiElement element = problemDescriptor.getPsiElement();
        if (element != null && !isInPsiFile(element)) {
            ExternallyDefinedPsiElement external = PsiTreeUtil.getParentOfType(element, ExternallyDefinedPsiElement.class, false);
            if (external != null) {
                PsiElement newTarget = external.getProblemTarget();
                if (newTarget != null) {
                    redirectProblem(problemDescriptor, newTarget);
                    return;
                }
            }
        }

        myProblems.add(problemDescriptor);
    }

    private boolean isInPsiFile(@Nonnull PsiElement element) {
        PsiFile file = element.getContainingFile();
        return myFile.getViewProvider() == file.getViewProvider();
    }

    @RequiredReadAction
    private void redirectProblem(@Nonnull final ProblemDescriptor problem, @Nonnull final PsiElement target) {
        final PsiElement original = problem.getPsiElement();
        final VirtualFile vFile = original.getContainingFile().getVirtualFile();
        assert vFile != null;
        final String path = FileUtil.toSystemIndependentName(vFile.getPath());

        String description = XmlStringUtil.stripHtml(problem.getDescriptionTemplate());

        final String template = InspectionsBundle.message(
            "inspection.redirect.template",
            description,
            path,
            original.getTextRange().getStartOffset(),
            vFile.getName()
        );


        final InspectionManager manager = InspectionManager.getInstance(original.getProject());
        final ProblemDescriptor newProblem =
            manager.createProblemDescriptor(target, template, (LocalQuickFix) null, problem.getHighlightType(), isOnTheFly());
        registerProblem(newProblem);
    }

    @RequiredReadAction
    @Override
    public void registerProblem(@Nonnull PsiReference reference, String descriptionTemplate, ProblemHighlightType highlightType) {
        List<LocalQuickFix> quickFixes = new ArrayList<>();
        PsiReferenceLocalQuickFixProvider.EP.forEachExtensionSafe(
            myFile.getProject(),
            provider -> provider.addQuickFixes(reference, quickFixes::add)
        );
        registerProblemForReference(reference, highlightType, descriptionTemplate, quickFixes.toArray(LocalQuickFix.ARRAY_FACTORY));
    }

    @RequiredReadAction
    @Override
    public void registerProblemForReference(
        @Nonnull PsiReference reference,
        @Nonnull ProblemHighlightType highlightType,
        @Nonnull String descriptionTemplate,
        @Nullable LocalQuickFix... fixes
    ) {
        ProblemDescriptor descriptor = myManager.createProblemDescriptor(
            reference.getElement(),
            reference.getRangeInElement(),
            descriptionTemplate,
            highlightType,
            myOnTheFly,
            fixes
        );
        registerProblem(descriptor);
    }

    @Override
    @RequiredReadAction
    public void registerProblem(@Nonnull PsiReference reference) {
        registerProblem(reference, ProblemsHolder.unresolvedReferenceMessage(reference).get(), ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
    }

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
    @Override
    @RequiredReadAction
    public void registerProblem(
        @Nonnull final PsiElement psiElement,
        @Nonnull final String message,
        @Nonnull ProblemHighlightType highlightType,
        @Nullable TextRange rangeInElement,
        @Nullable LocalQuickFix... fixes
    ) {

        final ProblemDescriptor descriptor =
            myManager.createProblemDescriptor(psiElement, rangeInElement, message, highlightType, myOnTheFly, fixes);
        registerProblem(descriptor);
    }

    @Override
    @RequiredReadAction
    public void registerProblem(
        @Nonnull final PsiElement psiElement,
        @Nullable TextRange rangeInElement,
        @Nonnull final String message,
        @Nullable LocalQuickFix... fixes
    ) {
        final ProblemDescriptor descriptor = myManager.createProblemDescriptor(
            psiElement,
            rangeInElement,
            message,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            myOnTheFly,
            fixes
        );
        registerProblem(descriptor);
    }

    @Override
    @Nonnull
    public List<ProblemDescriptor> getResults() {
        return myProblems;
    }

    @Override
    @Nonnull
    public ProblemDescriptor[] getResultsArray() {
        final List<ProblemDescriptor> problems = getResults();
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    @Override
    @Nonnull
    public final InspectionManager getManager() {
        return myManager;
    }

    @Override
    public boolean hasResults() {
        return !myProblems.isEmpty();
    }

    @Override
    public int getResultCount() {
        return myProblems.size();
    }

    @Override
    public boolean isOnTheFly() {
        return myOnTheFly;
    }

    @Override
    @Nonnull
    public PsiFile getFile() {
        return myFile;
    }

    @Override
    @Nonnull
    public final Project getProject() {
        return myManager.getProject();
    }
}
