// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import consulo.language.editor.rawHighlight.impl.HighlightInfoImpl;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.intention.IntentionAction;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.codeInspection.ex.QuickFixWrapper;
import consulo.application.internal.concurrency.JobLauncher;
import consulo.language.editor.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Attachment;
import consulo.logging.Logger;
import consulo.editor.Editor;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import consulo.application.util.function.Processor;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DoNotShowInspectionIntentionMenuContributor implements IntentionMenuContributor {
  private static final Logger LOG = Logger.getInstance(DoNotShowInspectionIntentionMenuContributor.class);

  @Override
  public void collectActions(@Nonnull Editor hostEditor, @Nonnull PsiFile hostFile, @Nonnull ShowIntentionsPass.IntentionsInfo intentions, int passIdToShowIntentionsFor, int offset) {
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
  private static void collectIntentionsFromDoNotShowLeveledInspections(@Nonnull final Project project,
                                                                       @Nonnull final PsiFile hostFile,
                                                                       @Nonnull PsiElement psiElement,
                                                                       final int offset,
                                                                       @Nonnull final ShowIntentionsPass.IntentionsInfo intentions) {
    if (!psiElement.isPhysical()) {
      VirtualFile virtualFile = hostFile.getVirtualFile();
      String text = hostFile.getText();
      LOG.error("not physical: '" + psiElement.getText() + "' @" + offset + " " + psiElement.getTextRange() +
                " elem:" + psiElement + " (" + psiElement.getClass().getName() + ")" +
                " in:" + psiElement.getContainingFile() + " host:" + hostFile + "(" + hostFile.getClass().getName() + ")",
                new Attachment(virtualFile != null ? virtualFile.getPresentableUrl() : "null", text != null ? text : "null"));
    }
    if (DumbService.isDumb(project)) {
      return;
    }

    final List<LocalInspectionToolWrapper> intentionTools = new ArrayList<>();
    final InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    final InspectionToolWrapper[] tools = profile.getInspectionTools(hostFile);
    for (InspectionToolWrapper toolWrapper : tools) {
      if (toolWrapper instanceof GlobalInspectionToolWrapper) {
        toolWrapper = ((GlobalInspectionToolWrapper)toolWrapper).getSharedLocalInspectionToolWrapper();
      }
      if (toolWrapper instanceof LocalInspectionToolWrapper && !((LocalInspectionToolWrapper)toolWrapper).isUnfair()) {
        final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
        if (profile.isToolEnabled(key, hostFile) && HighlightDisplayLevel.DO_NOT_SHOW.equals(profile.getErrorLevel(key, hostFile))) {
          intentionTools.add((LocalInspectionToolWrapper)toolWrapper);
        }
      }
    }

    if (intentionTools.isEmpty()) {
      return;
    }

    List<PsiElement> elements = PsiTreeUtil.collectParents(psiElement, PsiElement.class, true, e -> e instanceof PsiDirectory);
    PsiElement elementToTheLeft = psiElement.getContainingFile().findElementAt(offset - 1);
    if (elementToTheLeft != psiElement && elementToTheLeft != null) {
      List<PsiElement> parentsOnTheLeft = PsiTreeUtil.collectParents(elementToTheLeft, PsiElement.class, true, e -> e instanceof PsiDirectory || elements.contains(e));
      elements.addAll(parentsOnTheLeft);
    }

    final Set<String> dialectIds = InspectionEngine.calcElementDialectIds(elements);
    final LocalInspectionToolSession session = new LocalInspectionToolSession(hostFile, 0, hostFile.getTextLength());
    final Processor<LocalInspectionToolWrapper> processor = toolWrapper -> {
      final LocalInspectionTool localInspectionTool = toolWrapper.getTool();
      final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
      final String displayName = toolWrapper.getDisplayName();
      final ProblemsHolder holder = new ProblemsHolder(InspectionManager.getInstance(project), hostFile, true) {
        @Override
        public void registerProblem(@Nonnull ProblemDescriptor problemDescriptor) {
          super.registerProblem(problemDescriptor);
          if (problemDescriptor instanceof ProblemDescriptorBase) {
            final TextRange range = ((ProblemDescriptorBase)problemDescriptor).getTextRange();
            if (range != null && range.containsOffset(offset)) {
              final QuickFix[] fixes = problemDescriptor.getFixes();
              if (fixes != null) {
                for (int k = 0; k < fixes.length; k++) {
                  final IntentionAction intentionAction = QuickFixWrapper.wrap(problemDescriptor, k);
                  final HighlightInfoImpl.IntentionActionDescriptor actionDescriptor =
                          new HighlightInfoImpl.IntentionActionDescriptor(intentionAction, null, displayName, null, key, null, HighlightSeverity.INFORMATION);
                  (problemDescriptor.getHighlightType() == ProblemHighlightType.ERROR ? intentions.errorFixesToShow : intentions.intentionsToShow).add(actionDescriptor);
                }
              }
            }
          }
        }
      };
      InspectionEngine.createVisitorAndAcceptElements(localInspectionTool, holder, true, session, elements, dialectIds, InspectionEngine.getDialectIdsSpecifiedForTool(toolWrapper));
      localInspectionTool.inspectionFinished(session, holder);
      return true;
    };
    // indicator can be null when run from EDT
    ProgressIndicator progress = ObjectUtils.notNull(ProgressIndicatorProvider.getGlobalProgressIndicator(), new DaemonProgressIndicator());
    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(intentionTools, progress, processor);
  }
}
