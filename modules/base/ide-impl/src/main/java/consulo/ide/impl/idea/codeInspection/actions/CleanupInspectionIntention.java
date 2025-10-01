/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.SequentialModalProgressTask;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInspection.InspectionEngine;
import consulo.ide.impl.idea.codeInspection.ex.PerformFixesModalTask;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptorBase;
import consulo.language.editor.inspection.QuickFix;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.intention.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author anna
 * @since 2006-02-21
 */
public class CleanupInspectionIntention implements IntentionAction, HighPriorityAction, SyntheticIntentionAction {
    private final static Logger LOG = Logger.getInstance(CleanupInspectionIntention.class);

    private final InspectionToolWrapper myToolWrapper;
    private final Class myQuickfixClass;
    private final String myText;

    public CleanupInspectionIntention(@Nonnull InspectionToolWrapper toolWrapper, @Nonnull Class quickFixClass, String text) {
        myToolWrapper = toolWrapper;
        myQuickfixClass = quickFixClass;
        myText = text;
    }

    @Override
    @Nonnull
    public String getText() {
        return InspectionLocalize.fixAllInspectionProblemsInFile(myToolWrapper.getDisplayName()).get();
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        List<ProblemDescriptor> descriptions = ProgressManager.getInstance().runProcess(
            () -> {
                InspectionManager inspectionManager = InspectionManager.getInstance(project);
                return InspectionEngine.runInspectionOnFile(file, myToolWrapper, inspectionManager.createNewGlobalContext(false));
            },
            new EmptyProgressIndicator()
        );

        if (!descriptions.isEmpty() && !FileModificationService.getInstance().preparePsiElementForWrite(file)) {
            return;
        }

        AbstractPerformFixesTask fixesTask = applyFixes(project, "Apply Fixes", descriptions, myQuickfixClass);

        if (!fixesTask.isApplicableFixFound()) {
            HintManager.getInstance().showErrorHint(
                editor,
                "Unfortunately '" + myText + "' is currently not available for batch mode\n" +
                    " User interaction is required for each problem found"
            );
        }
    }

    @RequiredUIAccess
    public static AbstractPerformFixesTask applyFixes(
        @Nonnull Project project,
        @Nonnull String presentationText,
        @Nonnull List<ProblemDescriptor> descriptions,
        @Nullable Class quickfixClass
    ) {
        sortDescriptions(descriptions);
        return applyFixesNoSort(project, presentationText, descriptions, quickfixClass);
    }

    @RequiredUIAccess
    public static AbstractPerformFixesTask applyFixesNoSort(
        @Nonnull Project project,
        @Nonnull String presentationText,
        @Nonnull List<ProblemDescriptor> descriptions,
        @Nullable Class quickfixClass
    ) {
        SequentialModalProgressTask progressTask = new SequentialModalProgressTask(project, presentationText, true);
        boolean isBatch = quickfixClass != null && BatchQuickFix.class.isAssignableFrom(quickfixClass);
        AbstractPerformFixesTask fixesTask = isBatch
            ? new PerformBatchFixesTask(project, descriptions.toArray(ProblemDescriptor.EMPTY_ARRAY), progressTask, quickfixClass)
            : new PerformFixesTask(project, descriptions.toArray(ProblemDescriptor.EMPTY_ARRAY), progressTask, quickfixClass);
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(LocalizeValue.ofNullable(presentationText))
            .inGlobalUndoAction()
            .run(() -> {
                progressTask.setMinIterationTime(200);
                progressTask.setTask(fixesTask);
                ProgressManager.getInstance().run(progressTask);
            });
        return fixesTask;
    }

    public static void sortDescriptions(@Nonnull List<ProblemDescriptor> descriptions) {
        Collections.sort(descriptions, (o1, o2) -> {
            ProblemDescriptorBase d1 = (ProblemDescriptorBase)o1;
            ProblemDescriptorBase d2 = (ProblemDescriptorBase)o2;
            int elementsDiff = PsiUtilCore.compareElementsByPosition(d1.getPsiElement(), d2.getPsiElement());
            if (elementsDiff == 0) {
                return Comparing.compare(d1.getDescriptionTemplate(), d2.getDescriptionTemplate());
            }
            return -elementsDiff;
        });
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return myQuickfixClass != EmptyIntentionAction.class && editor != null
            && !(myToolWrapper instanceof LocalInspectionToolWrapper localInspectionToolWrapper && localInspectionToolWrapper.isUnfair());
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    private static abstract class AbstractPerformFixesTask extends PerformFixesModalTask {
        private boolean myApplicableFixFound = false;
        protected final Class myQuickfixClass;

        public AbstractPerformFixesTask(
            @Nonnull Project project,
            @Nonnull CommonProblemDescriptor[] descriptors,
            @Nonnull SequentialModalProgressTask task,
            @Nullable Class quickfixClass
        ) {
            super(project, descriptors, task);
            myQuickfixClass = quickfixClass;
        }

        protected abstract void collectFix(QuickFix fix, ProblemDescriptor descriptor, Project project);

        @Override
        protected final void applyFix(Project project, CommonProblemDescriptor descriptor) {
            QuickFix[] fixes = descriptor.getFixes();
            if (fixes != null && fixes.length > 0) {
                for (QuickFix fix : fixes) {
                    if (fix != null && (myQuickfixClass == null || fix.getClass().isAssignableFrom(myQuickfixClass))) {
                        ProblemDescriptor problemDescriptor = (ProblemDescriptor)descriptor;
                        PsiElement element = problemDescriptor.getPsiElement();
                        if (element != null && element.isValid()) {
                            collectFix(fix, problemDescriptor, project);
                            myApplicableFixFound = true;
                        }
                        break;
                    }
                }
            }
        }

        public final boolean isApplicableFixFound() {
            return myApplicableFixFound;
        }
    }

    private static class PerformBatchFixesTask extends AbstractPerformFixesTask {
        private final List<ProblemDescriptor> myBatchModeDescriptors = new ArrayList<>();
        private boolean myApplied = false;

        public PerformBatchFixesTask(
            @Nonnull Project project,
            @Nonnull CommonProblemDescriptor[] descriptors,
            @Nonnull SequentialModalProgressTask task,
            @Nonnull Class quickfixClass
        ) {
            super(project, descriptors, task, quickfixClass);
        }

        @Override
        protected void collectFix(QuickFix fix, ProblemDescriptor descriptor, Project project) {
            myBatchModeDescriptors.add(descriptor);
        }

        @Override
        public boolean isDone() {
            if (super.isDone()) {
                if (!myApplied && !myBatchModeDescriptors.isEmpty()) {
                    ProblemDescriptor representative = myBatchModeDescriptors.get(0);
                    LOG.assertTrue(representative.getFixes() != null);
                    for (QuickFix fix : representative.getFixes()) {
                        if (fix != null && fix.getClass().isAssignableFrom(myQuickfixClass)) {
                            ((BatchQuickFix)fix).applyFix(
                                myProject,
                                myBatchModeDescriptors.toArray(new ProblemDescriptor[myBatchModeDescriptors.size()]),
                                new ArrayList<>(),
                                null
                            );
                            break;
                        }
                    }
                    myApplied = true;
                }
                return true;
            }
            else {
                return false;
            }
        }
    }

    private static class PerformFixesTask extends AbstractPerformFixesTask {
        public PerformFixesTask(
            @Nonnull Project project,
            @Nonnull CommonProblemDescriptor[] descriptors,
            @Nonnull SequentialModalProgressTask task,
            @Nullable Class quickFixClass
        ) {
            super(project, descriptors, task, quickFixClass);
        }

        @Override
        protected void collectFix(QuickFix fix, ProblemDescriptor descriptor, Project project) {
            fix.applyFix(project, descriptor);
        }
    }
}
