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
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.ExternallyDefinedPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

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

    private class MyProblemBuilder extends AbstractProblemBuilder {
        private boolean created = false;

        public MyProblemBuilder(LocalizeValue descriptionTemplate) {
            super(descriptionTemplate);
            onTheFly(ProblemsHolderImpl.this.myOnTheFly);
        }

        @Override
        @RequiredReadAction
        public void create() {
            if (created) {
                throw new IllegalStateException("Must not call .create() twice");
            }
            registerProblem(new ProblemDescriptorBase(
                myStartElement,
                myEndElement,
                myDescriptionTemplate.get(),
                myLocalQuickFixes,
                myHighlightType,
                myIsAfterEndOfLine,
                myRangeInElement,
                myShowTooltip,
                myOnTheFly
            ));
            created = true;
        }
    }

    @Nonnull
    public ProblemsHolderImpl(@Nonnull InspectionManager manager, @Nonnull PsiFile file, boolean onTheFly) {
        myManager = manager;
        myFile = file;
        myOnTheFly = onTheFly;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public ProblemBuilder newProblem(LocalizeValue descriptionTemplate) {
        return new MyProblemBuilder(descriptionTemplate);
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
        PsiElement original = problem.getPsiElement();
        VirtualFile vFile = original.getContainingFile().getVirtualFile();
        assert vFile != null;
        String path = FileUtil.toSystemIndependentName(vFile.getPath());
        String description = XmlStringUtil.stripHtml(problem.getDescriptionTemplate());

        LocalizeValue descriptionTemplate =
            InspectionLocalize.inspectionRedirectTemplate(description, path, original.getTextRange().getStartOffset(), vFile.getName());

        newProblem(descriptionTemplate)
            .range(target)
            .highlightType(problem.getHighlightType())
            .create();
    }

    @Override
    @RequiredReadAction
    public void registerProblem(@Nonnull PsiReference reference, String descriptionTemplate, ProblemHighlightType highlightType) {
        newProblem(LocalizeValue.of(descriptionTemplate))
            .range(reference)
            .highlightType(highlightType)
            .withFixes(collectQuickFixes(reference))
            .create();
    }

    @Override
    @RequiredReadAction
    public void registerProblem(@Nonnull PsiReference reference) {
        newProblem(LocalizeValue.of(ProblemsHolder.unresolvedReferenceMessage(reference).get()))
            .range(reference)
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .withFixes(collectQuickFixes(reference))
            .create();
    }

    private LocalQuickFix[] collectQuickFixes(@Nonnull PsiReference reference) {
        List<LocalQuickFix> quickFixes = new ArrayList<>();
        PsiReferenceLocalQuickFixProvider.EP.forEachExtensionSafe(
            myFile.getProject(),
            provider -> provider.addQuickFixes(reference, quickFixes::add)
        );
        return quickFixes.toArray(LocalQuickFix.ARRAY_FACTORY);
    }

    @Nonnull
    @Override
    public List<ProblemDescriptor> getResults() {
        return myProblems;
    }

    @Nonnull
    @Override
    public ProblemDescriptor[] getResultsArray() {
        final List<ProblemDescriptor> problems = getResults();
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    @Nonnull
    @Override
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

    @Nonnull
    @Override
    public PsiFile getFile() {
        return myFile;
    }

    @Nonnull
    @Override
    public final Project getProject() {
        return myManager.getProject();
    }
}
