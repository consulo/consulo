/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class InspectionManagerBase extends InspectionManager {
    private static final Logger LOG = Logger.getInstance(InspectionManagerBase.class);

    private final Project myProject;
    protected String myCurrentProfileName;

    public InspectionManagerBase(Project project) {
        myProject = project;
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public List<ProblemDescriptor> runLocalToolLocaly(@Nonnull LocalInspectionTool tool, @Nonnull PsiFile file, @Nonnull Object state) {
        final ProblemsHolder holder = new ProblemsHolderImpl(this, file, false);
        LocalInspectionToolSession session = new LocalInspectionToolSession(file, 0, file.getTextLength());
        final PsiElementVisitor customVisitor = tool.buildVisitor(holder, false, session, state);
        LOG.assertTrue(!(customVisitor instanceof PsiRecursiveVisitor),
            "The visitor returned from LocalInspectionTool.buildVisitor() must not be recursive: " + customVisitor);

        tool.inspectionStarted(session, false, state);

        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.accept(customVisitor);
                super.visitElement(element);
            }
        });

        tool.inspectionFinished(session, holder, state);

        return holder.getResults();
    }

    @Nonnull
    @Override
    public ProblemsHolder createProblemsHolder(@Nonnull PsiFile file, boolean onTheFly) {
        return new ProblemsHolderImpl(this, file, onTheFly);
    }

    @Override
    @Nonnull
    public Project getProject() {
        return myProject;
    }

    @Override
    @Nonnull
    public CommonProblemDescriptor createProblemDescriptor(@Nonnull String descriptionTemplate, QuickFix... fixes) {
        return new CommonProblemDescriptorBase(fixes, descriptionTemplate);
    }

    @Override
    public ModuleProblemDescriptor createProblemDescriptor(
        @Nonnull String descriptionTemplate,
        @Nonnull Module module,
        QuickFix<?>... fixes
    ) {
        return new ModuleProblemDescriptorImpl(module, descriptionTemplate, fixes);
    }

    @Override
    @Nonnull
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix fix,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly
    ) {
        LocalQuickFix[] quickFixes = fix != null ? new LocalQuickFix[]{fix} : null;
        return createProblemDescriptor(psiElement, descriptionTemplate, onTheFly, quickFixes, highlightType);
    }

    @Override
    @Nonnull
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        boolean onTheFly,
        LocalQuickFix[] fixes,
        @Nonnull ProblemHighlightType highlightType
    ) {
        return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, onTheFly, false);
    }

    @Override
    @Nonnull
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix[] fixes,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly,
        boolean isAfterEndOfLine
    ) {
        return new ProblemDescriptorBase(
            psiElement,
            psiElement,
            descriptionTemplate,
            fixes,
            highlightType,
            isAfterEndOfLine,
            null,
            true,
            onTheFly
        );
    }

    @Override
    @Nonnull
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement,
        @Nonnull String descriptionTemplate,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly,
        LocalQuickFix... fixes
    ) {
        return new ProblemDescriptorBase(startElement, endElement, descriptionTemplate, fixes, highlightType, false, null, true, onTheFly);
    }

    @Nonnull
    @Override
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull final PsiElement psiElement,
        final TextRange rangeInElement,
        @Nonnull final String descriptionTemplate,
        @Nonnull final ProblemHighlightType highlightType,
        boolean onTheFly,
        final LocalQuickFix... fixes
    ) {
        return new ProblemDescriptorBase(
            psiElement,
            psiElement,
            descriptionTemplate,
            fixes,
            highlightType,
            false,
            rangeInElement,
            true,
            onTheFly
        );
    }

    @Nonnull
    @Override
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        boolean showTooltip,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly,
        LocalQuickFix... fixes
    ) {
        return new ProblemDescriptorBase(
            psiElement,
            psiElement,
            descriptionTemplate,
            fixes,
            highlightType,
            false,
            null,
            showTooltip,
            onTheFly
        );
    }

    @Override
    @Deprecated
    @Nonnull
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix fix,
        @Nonnull ProblemHighlightType highlightType
    ) {
        LocalQuickFix[] quickFixes = fix != null ? new LocalQuickFix[]{fix} : null;
        return createProblemDescriptor(psiElement, descriptionTemplate, false, quickFixes, highlightType);
    }

    @Override
    @Deprecated
    @Nonnull
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix[] fixes,
        @Nonnull ProblemHighlightType highlightType
    ) {
        return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, false, false);
    }

    @Override
    @Deprecated
    @Nonnull
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix[] fixes,
        @Nonnull ProblemHighlightType highlightType,
        boolean isAfterEndOfLine
    ) {
        return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, true, isAfterEndOfLine);
    }

    @Override
    @Deprecated
    @Nonnull
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement,
        @Nonnull String descriptionTemplate,
        @Nonnull ProblemHighlightType highlightType,
        LocalQuickFix... fixes
    ) {
        return createProblemDescriptor(startElement, endElement, descriptionTemplate, highlightType, true, fixes);
    }

    @Nonnull
    @Override
    @Deprecated
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull final PsiElement psiElement,
        final TextRange rangeInElement,
        @Nonnull final String descriptionTemplate,
        @Nonnull final ProblemHighlightType highlightType,
        final LocalQuickFix... fixes
    ) {
        return createProblemDescriptor(psiElement, rangeInElement, descriptionTemplate, highlightType, true, fixes);
    }

    @Nonnull
    @Deprecated
    @Override
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        boolean showTooltip,
        @Nonnull ProblemHighlightType highlightType,
        LocalQuickFix... fixes
    ) {
        return createProblemDescriptor(psiElement, descriptionTemplate, showTooltip, highlightType, true, fixes);
    }

    @Override
    public String getCurrentProfile() {
        if (myCurrentProfileName == null) {
            final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(getProject());
            myCurrentProfileName = profileManager.getProjectProfile();
            if (myCurrentProfileName == null) {
                myCurrentProfileName = InspectionProfileManager.getInstance().getRootProfile().getName();
            }
        }
        return myCurrentProfileName;
    }
}
