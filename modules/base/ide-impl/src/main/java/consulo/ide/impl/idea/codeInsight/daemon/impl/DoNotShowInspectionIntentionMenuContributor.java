// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.concurrent.JobLauncher;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInspection.InspectionEngine;
import consulo.language.editor.intention.QuickFixWrapper;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.internal.inspection.GlobalInspectionToolWrapper;
import consulo.language.editor.internal.inspection.LocalInspectionToolWrapper;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.editor.impl.internal.inspection.ProblemsHolderImpl;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.internal.intention.IntentionsInfo;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@ExtensionImpl(id = "dontShow")
public class DoNotShowInspectionIntentionMenuContributor implements IntentionMenuContributor {
    private static final Logger LOG = Logger.getInstance(DoNotShowInspectionIntentionMenuContributor.class);

    @Override
    @RequiredReadAction
    public void collectActions(
        @Nonnull Editor hostEditor,
        @Nonnull PsiFile hostFile,
        @Nonnull IntentionsInfo intentions,
        int passIdToShowIntentionsFor,
        int offset
    ) {
        Project project = hostFile.getProject();
        final PsiElement psiElement = hostFile.findElementAt(offset);
        if (HighlightingLevelManager.getInstance(project).shouldInspect(hostFile)) {
            PsiElement intentionElement = psiElement;
            int intentionOffset = offset;
            if (psiElement instanceof PsiWhiteSpace && offset == psiElement.getTextRange().getStartOffset() && offset > 0) {
                final PsiElement prev = hostFile.findElementAt(offset - 1);
                if (prev != null && prev.isValid()) {
                    intentionElement = prev;
                    intentionOffset = offset - 1;
                }
            }
            if (intentionElement != null && intentionElement.getManager().isInProject(intentionElement)) {
                collectIntentionsFromDoNotShowLeveledInspections(project, hostFile, intentionElement, intentionOffset, intentions);
            }
        }
    }

    /**
     * Can be invoked in EDT, each inspection should be fast
     */
    @RequiredReadAction
    private static void collectIntentionsFromDoNotShowLeveledInspections(
        @Nonnull final Project project,
        @Nonnull final PsiFile hostFile,
        @Nonnull PsiElement psiElement,
        final int offset,
        @Nonnull final IntentionsInfo intentions
    ) {
        if (!psiElement.isPhysical()) {
            VirtualFile virtualFile = hostFile.getVirtualFile();
            String text = hostFile.getText();
            LOG.error(
                "not physical: '" + psiElement.getText() + "' @" + offset + " " + psiElement.getTextRange() +
                    " elem:" + psiElement + " (" + psiElement.getClass().getName() + ")" +
                    " in:" + psiElement.getContainingFile() + " host:" + hostFile + "(" + hostFile.getClass().getName() + ")",
                AttachmentFactory.get().create(
                    virtualFile != null ? virtualFile.getPresentableUrl() : "null",
                    text != null ? text : "null"
                )
            );
        }
        if (DumbService.isDumb(project)) {
            return;
        }

        List<LocalInspectionToolWrapper> intentionTools = new ArrayList<>();
        InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
        InspectionToolWrapper[] tools = profile.getInspectionTools(hostFile);
        for (InspectionToolWrapper toolWrapper : tools) {
            if (toolWrapper instanceof GlobalInspectionToolWrapper globalInspectionToolWrapper) {
                toolWrapper = globalInspectionToolWrapper.getSharedLocalInspectionToolWrapper();
            }
            if (toolWrapper instanceof LocalInspectionToolWrapper localInspectionToolWrapper && !localInspectionToolWrapper.isUnfair()) {
                HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
                if (profile.isToolEnabled(key, hostFile)
                    && HighlightDisplayLevel.DO_NOT_SHOW.equals(profile.getErrorLevel(key, hostFile))) {
                    intentionTools.add(localInspectionToolWrapper);
                }
            }
        }

        if (intentionTools.isEmpty()) {
            return;
        }

        List<PsiElement> elements = PsiTreeUtil.collectParents(psiElement, PsiElement.class, true, e -> e instanceof PsiDirectory);
        PsiElement elementToTheLeft = psiElement.getContainingFile().findElementAt(offset - 1);
        if (elementToTheLeft != psiElement && elementToTheLeft != null) {
            List<PsiElement> parentsOnTheLeft = PsiTreeUtil.collectParents(
                elementToTheLeft,
                PsiElement.class,
                true,
                e -> e instanceof PsiDirectory || elements.contains(e)
            );
            elements.addAll(parentsOnTheLeft);
        }

        final Set<String> dialectIds = InspectionEngine.calcElementDialectIds(elements);
        final LocalInspectionToolSession session = new LocalInspectionToolSession(hostFile, 0, hostFile.getTextLength());
        final Predicate<LocalInspectionToolWrapper> processor = toolWrapper -> {
            final LocalInspectionTool localInspectionTool = toolWrapper.getTool();
            final Object toolState = toolWrapper.getToolState().getState();
            final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
            final String displayName = toolWrapper.getDisplayName();
            final ProblemsHolder holder = new ProblemsHolderImpl(InspectionManager.getInstance(project), hostFile, true) {
                @Override
                @RequiredReadAction
                public void registerProblem(@Nonnull ProblemDescriptor problemDescriptor) {
                    super.registerProblem(problemDescriptor);
                    TextRange range = problemDescriptor instanceof ProblemDescriptorBase problemDescriptorBase
                        ? problemDescriptorBase.getTextRange()
                        : null;
                    QuickFix[] fixes = range != null && range.containsOffset(offset) ? problemDescriptor.getFixes() : null;
                    if (fixes != null) {
                        for (int k = 0; k < fixes.length; k++) {
                            IntentionAction intentionAction = QuickFixWrapper.wrap(problemDescriptor, k);
                            IntentionActionDescriptor actionDescriptor = new IntentionActionDescriptor(
                                intentionAction,
                                null,
                                displayName,
                                null,
                                key,
                                null,
                                HighlightSeverity.INFORMATION
                            );
                            List<IntentionActionDescriptor> intentionActionDescriptors =
                                problemDescriptor.getHighlightType() == ProblemHighlightType.ERROR
                                    ? intentions.errorFixesToShow
                                    : intentions.intentionsToShow;
                            intentionActionDescriptors.add(actionDescriptor);
                        }
                    }
                }
            };
            InspectionEngine.createVisitorAndAcceptElements(
                localInspectionTool,
                holder,
                true,
                session,
                elements,
                dialectIds,
                InspectionEngine.getDialectIdsSpecifiedForTool(toolWrapper),
                toolState
            );
            localInspectionTool.inspectionFinished(session, holder, toolState);
            return true;
        };
        // indicator can be null when run from EDT
        ProgressIndicator progress =
            ObjectUtil.notNull(ProgressIndicatorProvider.getGlobalProgressIndicator(), new DaemonProgressIndicator());
        JobLauncher.getInstance().invokeConcurrentlyUnderProgress(intentionTools, progress, processor);
    }
}
