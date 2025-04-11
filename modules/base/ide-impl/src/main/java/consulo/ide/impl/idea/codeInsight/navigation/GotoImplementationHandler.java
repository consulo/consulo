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

package consulo.ide.impl.idea.codeInsight.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ContainerProvider;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GotoImplementationHandler extends GotoTargetHandler {
    @Override
    protected String getFeatureUsedKey() {
        return "navigation.goto.implementation";
    }

    @Override
    @Nullable
    @RequiredReadAction
    public GotoData getSourceAndTargetElements(@Nonnull Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement source = TargetElementUtil.findTargetElement(editor, ImplementationSearcher.getFlags(), offset);
        if (source == null) {
            return null;
        }
        PsiReference reference = TargetElementUtil.findReference(editor, offset);
        PsiElement[] targets = new ImplementationSearcher.FirstImplementationsSearcher() {
            @Override
            protected boolean accept(PsiElement element) {
                return TargetElementUtil.acceptImplementationForReference(reference, element);
            }

            @Override
            protected boolean canShowPopupWithOneItem(PsiElement element) {
                return false;
            }
        }.searchImplementations(editor, source, offset);
        if (targets == null) {
            return null;
        }
        GotoData gotoData = new GotoData(source, targets, Collections.emptyList());
        gotoData.listUpdaterTask = new ImplementationsUpdaterTask(gotoData, editor, offset, reference) {
            @Override
            @RequiredUIAccess
            public void onFinished() {
                super.onFinished();
                PsiElement oneElement = getTheOnlyOneElement();
                if (oneElement != null && navigateToElement(oneElement)) {
                    myPopup.cancel();
                }
            }
        };
        return gotoData;
    }

    private static PsiElement getContainer(PsiElement refElement) {
        for (ContainerProvider provider : ContainerProvider.EP_NAME.getExtensionList()) {
            PsiElement container = provider.getContainer(refElement);
            if (container != null) {
                return container;
            }
        }
        return refElement.getParent();
    }

    @Override
    @Nonnull
    protected String getChooserTitle(@Nonnull PsiElement sourceElement, String name, int length, boolean finished) {
        ItemPresentation presentation = ((NavigationItem)sourceElement).getPresentation();
        String fullName;
        if (presentation == null) {
            fullName = name;
        }
        else {
            PsiElement container = getContainer(sourceElement);
            ItemPresentation containerPresentation =
                container == null || container instanceof PsiFile ? null : ((NavigationItem)container).getPresentation();
            String containerText = containerPresentation == null ? null : containerPresentation.getPresentableText();
            fullName = (containerText == null ? "" : containerText + ".") + presentation.getPresentableText();
        }
        return CodeInsightBundle.message("goto.implementation.chooserTitle", fullName, length, finished ? "" : " so far");
    }

    @Nonnull
    @Override
    protected String getFindUsagesTitle(@Nonnull PsiElement sourceElement, String name, int length) {
        return CodeInsightBundle.message("goto.implementation.findUsages.title", name, length);
    }

    @Nonnull
    @Override
    protected String getNotFoundMessage(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return CodeInsightLocalize.gotoImplementationNotfound().get();
    }

    private class ImplementationsUpdaterTask extends ListBackgroundUpdaterTask {
        private final Editor myEditor;
        private final int myOffset;
        private final GotoData myGotoData;
        private final Map<Object, PsiElementListCellRenderer> renderers = new HashMap<>();
        private final PsiReference myReference;

        ImplementationsUpdaterTask(@Nonnull GotoData gotoData, @Nonnull Editor editor, int offset, PsiReference reference) {
            super(gotoData.source.getProject(), ImplementationSearcher.SEARCHING_FOR_IMPLEMENTATIONS);
            myEditor = editor;
            myOffset = offset;
            myGotoData = gotoData;
            myReference = reference;
        }

        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
            super.run(indicator);
            for (PsiElement element : myGotoData.targets) {
                if (!updateComponent(element, createComparator(renderers, myGotoData))) {
                    return;
                }
            }
            new ImplementationSearcher.BackgroundableImplementationSearcher() {
                @Override
                protected void processElement(PsiElement element) {
                    indicator.checkCanceled();
                    if (!TargetElementUtil.acceptImplementationForReference(myReference, element)) {
                        return;
                    }
                    if (myGotoData.addTarget(element)) {
                        if (!updateComponent(element, createComparator(renderers, myGotoData))) {
                            indicator.cancel();
                        }
                    }
                }
            }.searchImplementations(myEditor, myGotoData.source, myOffset);
        }

        @Override
        @RequiredReadAction
        public String getCaption(int size) {
            return getChooserTitle(myGotoData.source, ((PsiNamedElement)myGotoData.source).getName(), size, isFinished());
        }
    }
}
