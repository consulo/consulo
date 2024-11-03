/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 09-Jan-2007
 */
package consulo.ide.impl.idea.codeInspection.offlineViewer;

import consulo.ide.impl.idea.codeInspection.ex.QuickFixWrapper;
import consulo.ide.impl.idea.codeInspection.offline.OfflineProblemDescriptor;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.ide.impl.idea.codeInspection.ui.ProblemDescriptionNode;
import consulo.language.Language;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.impl.internal.inspection.ProblemsHolderImpl;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.util.CollectHighlightsUtil;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.*;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OfflineProblemDescriptorNode extends ProblemDescriptionNode {
  public OfflineProblemDescriptorNode(@Nonnull OfflineProblemDescriptor descriptor,
                                      @Nonnull LocalInspectionToolWrapper toolWrapper,
                                      @Nonnull InspectionToolPresentation presentation) {
    super(descriptor, toolWrapper, presentation);
  }

  private static PsiElement[] getElementsIntersectingRange(PsiFile file, final int startOffset, final int endOffset) {
    final FileViewProvider viewProvider = file.getViewProvider();
    final Set<PsiElement> result = new LinkedHashSet<PsiElement>();
    for (Language language : viewProvider.getLanguages()) {
      final PsiFile psiRoot = viewProvider.getPsi(language);
      if (HighlightingLevelManager.getInstance(file.getProject()).shouldInspect(psiRoot)) {
        result.addAll(CollectHighlightsUtil.getElementsInRange(psiRoot, startOffset, endOffset, true));
      }
    }
    return PsiUtilCore.toPsiElementArray(result);
  }

  @Override
  @Nullable
  public RefEntity getElement() {
    if (userObject instanceof CommonProblemDescriptor) {
      return myElement;
    }
    if (userObject == null) {
      return null;
    }
    myElement = ((OfflineProblemDescriptor)userObject).getRefElement(myPresentation.getContext().getRefManager());
    return myElement;
  }

  @Override
  @Nullable
  public CommonProblemDescriptor getDescriptor() {
    if (userObject == null) return null;
    if (userObject instanceof CommonProblemDescriptor) {
      return (CommonProblemDescriptor)userObject;
    }

    final InspectionManager inspectionManager = InspectionManager.getInstance(myPresentation.getContext().getProject());
    final OfflineProblemDescriptor offlineProblemDescriptor = (OfflineProblemDescriptor)userObject;
    final RefEntity element = getElement();
    if (myToolWrapper instanceof LocalInspectionToolWrapper) {
      if (element instanceof RefElement) {
        final PsiElement psiElement = ((RefElement)element).getPsiElement();
        if (psiElement != null) {
          PsiFile containingFile = psiElement.getContainingFile();
          final ProblemsHolderImpl holder = new ProblemsHolderImpl(inspectionManager, containingFile, false);
          final LocalInspectionTool localTool = ((LocalInspectionToolWrapper)myToolWrapper).getTool();
          Object localToolState = myToolWrapper.getToolState().getState();
          final int startOffset = psiElement.getTextRange().getStartOffset();
          final int endOffset = psiElement.getTextRange().getEndOffset();
          LocalInspectionToolSession session = new LocalInspectionToolSession(containingFile, startOffset, endOffset);
          final PsiElementVisitor visitor = localTool.buildVisitor(holder, false, session, localToolState);
          localTool.inspectionStarted(session, false, localToolState);
          final PsiElement[] elementsInRange = getElementsIntersectingRange(containingFile,
                                                                            startOffset,
                                                                            endOffset);
          for (PsiElement el : elementsInRange) {
            el.accept(visitor);
          }
          localTool.inspectionFinished(session, holder, localToolState);
          if (holder.hasResults()) {
            final List<ProblemDescriptor> list = holder.getResults();
            final int idx = offlineProblemDescriptor.getProblemIndex();
            int curIdx = 0;
            for (ProblemDescriptor descriptor : list) {
              final PsiNamedElement member = localTool.getProblemElement(descriptor.getPsiElement());
              if (psiElement instanceof PsiFile || member != null && member.equals(psiElement)) {
                if (curIdx == idx) {
                  setUserObject(descriptor);
                  return descriptor;
                }
                curIdx++;
              }
            }
          }
        }
      }
      setUserObject(null);
      return null;
    }
    final List<String> hints = offlineProblemDescriptor.getHints();
    if (element instanceof RefElement) {
      final PsiElement psiElement = ((RefElement)element).getPsiElement();
      if (psiElement == null) return null;
      ProblemDescriptor descriptor = inspectionManager.createProblemDescriptor(psiElement, offlineProblemDescriptor.getDescription(),
                                                                               (LocalQuickFix)null,
                                                                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false);
      final LocalQuickFix[] quickFixes = getFixes(descriptor, hints);
      if (quickFixes != null) {
        descriptor = inspectionManager.createProblemDescriptor(psiElement, offlineProblemDescriptor.getDescription(), false, quickFixes,
                                                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      }
      setUserObject(descriptor);
      return descriptor;
    }
    CommonProblemDescriptor descriptor =
            inspectionManager.createProblemDescriptor(offlineProblemDescriptor.getDescription(), (QuickFix)null);
    final QuickFix[] quickFixes = getFixes(descriptor, hints);
    if (quickFixes != null) {
      descriptor = inspectionManager.createProblemDescriptor(offlineProblemDescriptor.getDescription(), quickFixes);
    }
    setUserObject(descriptor);
    return descriptor;
  }

  @Nullable
  private LocalQuickFix[] getFixes(@Nonnull CommonProblemDescriptor descriptor, List<String> hints) {
    final List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>(hints == null ? 1 : hints.size());
    if (hints == null) {
      addFix(descriptor, fixes, null);
    }
    else {
      for (String hint : hints) {
        addFix(descriptor, fixes, hint);
      }
    }
    return fixes.isEmpty() ? null : fixes.toArray(new LocalQuickFix[fixes.size()]);
  }

  private void addFix(@Nonnull CommonProblemDescriptor descriptor, final List<LocalQuickFix> fixes, String hint) {
    final IntentionAction intentionAction = myPresentation.findQuickFixes(descriptor, hint);
    if (intentionAction instanceof QuickFixWrapper) {
      fixes.add(((QuickFixWrapper)intentionAction).getFix());
    }
  }

  @Override
  public boolean isValid() {
    return getDescriptor() != null && super.isValid();
  }

  @Override
  public FileStatus getNodeStatus() {
    return FileStatus.NOT_CHANGED;
  }
}
