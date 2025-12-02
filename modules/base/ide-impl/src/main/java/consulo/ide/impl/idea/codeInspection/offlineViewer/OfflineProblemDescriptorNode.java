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
package consulo.ide.impl.idea.codeInspection.offlineViewer;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.intention.QuickFixWrapper;
import consulo.ide.impl.idea.codeInspection.offline.OfflineProblemDescriptor;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.ide.impl.idea.codeInspection.ui.ProblemDescriptionNode;
import consulo.language.Language;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
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

/**
 * @author anna
 * @since 2007-01-09
 */
public class OfflineProblemDescriptorNode extends ProblemDescriptionNode {
    public OfflineProblemDescriptorNode(
        @Nonnull OfflineProblemDescriptor descriptor,
        @Nonnull LocalInspectionToolWrapper toolWrapper,
        @Nonnull InspectionToolPresentation presentation
    ) {
        super(descriptor, toolWrapper, presentation);
    }

    @RequiredReadAction
    private static PsiElement[] getElementsIntersectingRange(PsiFile file, int startOffset, int endOffset) {
        FileViewProvider viewProvider = file.getViewProvider();
        Set<PsiElement> result = new LinkedHashSet<>();
        for (Language language : viewProvider.getLanguages()) {
            PsiFile psiRoot = viewProvider.getPsi(language);
            if (HighlightingLevelManager.getInstance(file.getProject()).shouldInspect(psiRoot)) {
                result.addAll(CollectHighlightsUtil.getElementsInRange(psiRoot, startOffset, endOffset, true));
            }
        }
        return PsiUtilCore.toPsiElementArray(result);
    }

    @Nullable
    @Override
    @RequiredReadAction
    public RefEntity getElement() {
        if (userObject instanceof CommonProblemDescriptor) {
            return myElement;
        }
        if (userObject == null) {
            return null;
        }
        myElement = ((OfflineProblemDescriptor) userObject).getRefElement(myPresentation.getContext().getRefManager());
        return myElement;
    }

    @Nullable
    @Override
    @RequiredReadAction
    public CommonProblemDescriptor getDescriptor() {
        if (userObject == null) {
            return null;
        }
        if (userObject instanceof CommonProblemDescriptor commonProblemDescriptor) {
            return commonProblemDescriptor;
        }

        InspectionManager inspectionManager = InspectionManager.getInstance(myPresentation.getContext().getProject());
        OfflineProblemDescriptor offlineProblemDescriptor = (OfflineProblemDescriptor) userObject;
        RefEntity element = getElement();
        if (myToolWrapper instanceof LocalInspectionToolWrapper localInspectionToolWrapper) {
            if (element instanceof RefElement refElem) {
                PsiElement psiElement = refElem.getPsiElement();
                if (psiElement != null) {
                    PsiFile containingFile = psiElement.getContainingFile();
                    ProblemsHolderImpl holder = new ProblemsHolderImpl(inspectionManager, containingFile, false);
                    LocalInspectionTool localTool = localInspectionToolWrapper.getTool();
                    Object localToolState = localInspectionToolWrapper.getToolState().getState();
                    int startOffset = psiElement.getTextRange().getStartOffset();
                    int endOffset = psiElement.getTextRange().getEndOffset();
                    LocalInspectionToolSession session = new LocalInspectionToolSession(containingFile, startOffset, endOffset);
                    PsiElementVisitor visitor = localTool.buildVisitor(holder, false, session, localToolState);
                    localTool.inspectionStarted(session, false, localToolState);
                    PsiElement[] elementsInRange = getElementsIntersectingRange(containingFile, startOffset, endOffset);
                    for (PsiElement el : elementsInRange) {
                        el.accept(visitor);
                    }
                    localTool.inspectionFinished(session, holder, localToolState);
                    if (holder.hasResults()) {
                        List<ProblemDescriptor> list = holder.getResults();
                        int idx = offlineProblemDescriptor.getProblemIndex();
                        int curIdx = 0;
                        for (ProblemDescriptor descriptor : list) {
                            PsiNamedElement member = localTool.getProblemElement(descriptor.getPsiElement());
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
        List<String> hints = offlineProblemDescriptor.getHints();
        if (element instanceof RefElement refElem) {
            PsiElement psiElement = refElem.getPsiElement();
            if (psiElement == null) {
                return null;
            }
            ProblemDescriptor descriptor = inspectionManager.createProblemDescriptor(psiElement, offlineProblemDescriptor.getDescription(),
                (LocalQuickFix) null,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false
            );
            LocalQuickFix[] quickFixes = getFixes(descriptor, hints);
            if (quickFixes != null) {
                descriptor = inspectionManager.createProblemDescriptor(
                    psiElement,
                    offlineProblemDescriptor.getDescription(),
                    false,
                    quickFixes,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
            setUserObject(descriptor);
            return descriptor;
        }
        CommonProblemDescriptor descriptor =
            inspectionManager.createProblemDescriptor(offlineProblemDescriptor.getDescription(), (QuickFix) null);
        QuickFix[] quickFixes = getFixes(descriptor, hints);
        if (quickFixes != null) {
            descriptor = inspectionManager.createProblemDescriptor(offlineProblemDescriptor.getDescription(), quickFixes);
        }
        setUserObject(descriptor);
        return descriptor;
    }

    @Nullable
    private LocalQuickFix[] getFixes(@Nonnull CommonProblemDescriptor descriptor, List<String> hints) {
        List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>(hints == null ? 1 : hints.size());
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

    private void addFix(@Nonnull CommonProblemDescriptor descriptor, List<LocalQuickFix> fixes, String hint) {
        IntentionAction intentionAction = myPresentation.findQuickFixes(descriptor, hint);
        if (intentionAction instanceof QuickFixWrapper fixWrapper) {
            fixes.add(fixWrapper.getFix());
        }
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        return getDescriptor() != null && super.isValid();
    }

    @Override
    public FileStatus getNodeStatus() {
        return FileStatus.NOT_CHANGED;
    }
}
