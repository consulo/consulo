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
package consulo.ide.impl.idea.codeInsight.hint.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.util.TextRange;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.idea.codeInsight.hint.ImplementationViewComponentImpl;
import consulo.ide.impl.idea.codeInsight.navigation.ImplementationSearcher;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.ide.impl.idea.openapi.progress.impl.BackgroundableProcessIndicator;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.PopupPositionManager;
import consulo.ide.impl.idea.ui.popup.PopupUpdateProcessor;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.ui.navigation.BackgroundUpdaterTaskBase;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.popup.GenericListComponentUpdater;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.usage.Usage;
import consulo.usage.UsageInfo;
import consulo.usage.UsageInfo2UsageAdapter;
import consulo.usage.UsageView;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;

@ActionImpl(id = IdeActions.ACTION_QUICK_IMPLEMENTATIONS)
public class ShowImplementationsAction extends AnAction implements PopupAction {
    public static final String CODEASSISTS_QUICKDEFINITION_LOOKUP_FEATURE = "codeassists.quickdefinition.lookup";
    public static final String CODEASSISTS_QUICKDEFINITION_FEATURE = "codeassists.quickdefinition";

    private static final Logger LOG = Logger.getInstance(ShowImplementationsAction.class);

    private Reference<JBPopup> myPopupRef;
    private Reference<ImplementationsUpdaterTask> myTaskRef;

    public ShowImplementationsAction() {
        super(ActionLocalize.actionQuickimplementationsText(), ActionLocalize.actionQuickimplementationsDescription());
        setEnabledInModalContext(true);
        setInjectedContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        performForContext(e.getDataContext(), true);
    }

    @TestOnly
    @RequiredUIAccess
    public void performForContext(DataContext dataContext) {
        performForContext(dataContext, true);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        Editor editor = getEditor(e.getDataContext());

        PsiFile file = e.getData(PsiFile.KEY);
        PsiElement element = ReadAction.compute(() -> getElement(project, file, editor, e.getData(PsiElement.KEY)));

        PsiFile containingFile = element != null ? element.getContainingFile() : file;
        boolean enabled = !(containingFile == null || !containingFile.getViewProvider().isPhysical());
        e.getPresentation().setEnabled(enabled);
    }

    protected static Editor getEditor(@Nonnull DataContext dataContext) {
        Editor editor = dataContext.getData(Editor.KEY);

        if (editor == null) {
            PsiFile file = dataContext.getData(PsiFile.KEY);
            if (file != null) {
                VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile != null) {
                    FileEditor fileEditor = FileEditorManager.getInstance(file.getProject()).getSelectedEditor(virtualFile);
                    if (fileEditor instanceof TextEditor textEditor) {
                        editor = textEditor.getEditor();
                    }
                }
            }
        }
        return editor;
    }

    @RequiredUIAccess
    public void performForContext(@Nonnull DataContext dataContext, boolean invokedByShortcut) {
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        PsiFile file = dataContext.getData(PsiFile.KEY);
        Editor editor = getEditor(dataContext);

        PsiElement element = dataContext.getData(PsiElement.KEY);
        boolean isInvokedFromEditor = dataContext.getData(Editor.KEY) != null;
        element = getElement(project, file, editor, element);

        if (element == null && file == null) {
            return;
        }
        PsiFile containingFile = element != null ? element.getContainingFile() : file;
        if (containingFile == null || !containingFile.getViewProvider().isPhysical()) {
            return;
        }


        PsiReference ref = null;
        if (editor != null) {
            ref = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());
            if (element == null && ref != null) {
                element = TargetElementUtil.adjustReference(ref);
            }
        }

        //check attached sources if any
        if (element instanceof PsiCompiledElement) {
            element = element.getNavigationElement();
        }

        String text = "";
        PsiElement[] impls = PsiElement.EMPTY_ARRAY;
        if (element != null) {
            impls = getSelfAndImplementations(editor, element, createImplementationsSearcher());
            text = SymbolPresentationUtil.getSymbolPresentableText(element);
        }

        if (impls.length == 0 && ref instanceof PsiPolyVariantReference polyReference) {
            PsiElement refElement = polyReference.getElement();
            TextRange rangeInElement = polyReference.getRangeInElement();
            String refElementText = refElement.getText();
            LOG.assertTrue(
                rangeInElement.getEndOffset() <= refElementText.length(),
                "Ref: " + polyReference + "; refElement: " + refElement + "; refText: " + refElementText
            );
            text = rangeInElement.substring(refElementText);
            ResolveResult[] results = polyReference.multiResolve(false);
            List<PsiElement> implsList = new ArrayList<>(results.length);

            for (ResolveResult result : results) {
                PsiElement resolvedElement = result.getElement();

                if (resolvedElement != null && resolvedElement.isPhysical()) {
                    implsList.add(resolvedElement);
                }
            }

            if (!implsList.isEmpty()) {
                impls = implsList.toArray(new PsiElement[implsList.size()]);
            }
        }

        showImplementations(impls, project, text, editor, file, element, isInvokedFromEditor, invokedByShortcut);
    }

    @RequiredReadAction
    protected static PsiElement getElement(@Nonnull Project project, PsiFile file, Editor editor, PsiElement element) {
        if (element == null && editor != null) {
            element = TargetElementUtil.findTargetElement(editor, TargetElementUtil.getAllAccepted());
            PsiElement adjustedElement = TargetElementUtil.adjustElement(editor, TargetElementUtil.getAllAccepted(), element, null);
            if (adjustedElement != null) {
                element = adjustedElement;
            }
            else if (file != null) {
                element = DocumentationManager.getInstance(project).getElementFromLookup(editor, file);
            }
        }
        return element;
    }

    @Nonnull
    ImplementationSearcher createImplementationsSearcher() {
        if (Application.get().isUnitTestMode()) {
            return new ImplementationSearcher() {
                @Override
                protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
                    return ShowImplementationsAction.filterElements(targetElements);
                }
            };
        }
        return new ImplementationSearcher.FirstImplementationsSearcher() {
            @Override
            protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
                return ShowImplementationsAction.filterElements(targetElements);
            }

            @Override
            protected boolean isSearchDeep() {
                return ShowImplementationsAction.this.isSearchDeep();
            }
        };
    }

    @RequiredUIAccess
    private void updateElementImplementations(PsiElement element, Editor editor, @Nonnull Project project, PsiFile file) {
        PsiElement[] impls = {};
        String text = "";
        if (element != null) {
            // if (element instanceof PsiPackage) return;
            PsiFile containingFile = element.getContainingFile();
            if (containingFile == null || !containingFile.getViewProvider().isPhysical()) {
                return;
            }

            impls = getSelfAndImplementations(editor, element, createImplementationsSearcher());
            text = SymbolPresentationUtil.getSymbolPresentableText(element);
        }

        showImplementations(impls, project, text, editor, file, element, false, false);
    }

    @RequiredUIAccess
    protected void showImplementations(
        @Nonnull PsiElement[] impls,
        @Nonnull final Project project,
        String text,
        final Editor editor,
        final PsiFile file,
        PsiElement element,
        boolean invokedFromEditor,
        boolean invokedByShortcut
    ) {
        if (impls.length == 0) {
            return;
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed(CODEASSISTS_QUICKDEFINITION_FEATURE);
        if (LookupManager.getInstance(project).getActiveLookup() != null) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed(CODEASSISTS_QUICKDEFINITION_LOOKUP_FEATURE);
        }

        int index = 0;
        if (invokedFromEditor && file != null && impls.length > 1) {
            VirtualFile virtualFile = file.getVirtualFile();
            PsiFile containingFile = impls[0].getContainingFile();
            if (virtualFile != null && containingFile != null && virtualFile.equals(containingFile.getVirtualFile())) {
                PsiFile secondContainingFile = impls[1].getContainingFile();
                if (secondContainingFile != containingFile) {
                    index = 1;
                }
            }
        }

        SimpleReference<UsageView> usageView = new SimpleReference<>();
        LocalizeValue title = CodeInsightLocalize.implementationViewTitle(text);
        JBPopup popup = SoftReference.dereference(myPopupRef);
        if (popup != null && popup.isVisible() && popup instanceof AbstractPopup abstractPopup) {
            ImplementationViewComponentImpl component = (ImplementationViewComponentImpl) abstractPopup.getComponent();
            popup.setCaption(title.get());
            component.update(impls, index);
            updateInBackground(editor, element, component, title, abstractPopup, usageView);
            if (invokedByShortcut) {
                abstractPopup.focusPreferredComponent();
            }
            return;
        }

        ImplementationViewComponentImpl component = new ImplementationViewComponentImpl(impls, index);
        if (component.hasElementsToShow()) {
            PopupUpdateProcessor updateProcessor = new PopupUpdateProcessor(project) {
                @Override
                @RequiredUIAccess
                public void updatePopup(Object lookupItemObject) {
                    PsiElement element = lookupItemObject instanceof PsiElement psiElement
                        ? psiElement
                        : DocumentationManager.getInstance(project).getElementFromLookup(editor, file);
                    updateElementImplementations(element, editor, project, file);
                }
            };

            popup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, component.getPreferredFocusableComponent())
                .setProject(project)
                .addListener(updateProcessor)
                .addUserData(updateProcessor)
                .setDimensionServiceKey(project, DocumentationManagerHelper.JAVADOC_LOCATION_AND_SIZE, false)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(invokedFromEditor && LookupManager.getActiveLookup(editor) == null)
                .setTitle(title)
                .setCouldPin(popup1 -> {
                    usageView.set(component.showInUsageView());
                    popup1.cancel();
                    myTaskRef = null;
                    return false;
                })
                .setCancelCallback(() -> {
                    ImplementationsUpdaterTask task = SoftReference.dereference(myTaskRef);
                    if (task != null) {
                        task.cancelTask();
                    }
                    return Boolean.TRUE;
                })
                .createPopup();

            updateInBackground(editor, element, component, title, (AbstractPopup)popup, usageView);

            PopupPositionManager.positionPopupInBestPosition(popup, editor, DataManager.getInstance().getDataContext());
            component.setHint(popup, title);

            myPopupRef = new WeakReference<>(popup);
        }
    }

    private void updateInBackground(
        Editor editor,
        @Nullable PsiElement element,
        @Nonnull ImplementationViewComponentImpl component,
        @Nonnull LocalizeValue title,
        @Nonnull AbstractPopup popup,
        @Nonnull SimpleReference<UsageView> usageView
    ) {
        ImplementationsUpdaterTask updaterTask = SoftReference.dereference(myTaskRef);
        if (updaterTask != null) {
            updaterTask.cancelTask();
        }

        if (element == null) {
            return; //already found
        }
        ImplementationsUpdaterTask task = new ImplementationsUpdaterTask(element, editor, title, isIncludeAlwaysSelf(), component);
        task.init(popup, new ImplementationViewComponentUpdater(component, isIncludeAlwaysSelf() ? 1 : 0), usageView);

        myTaskRef = new WeakReference<>(task);
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, new BackgroundableProcessIndicator(task));
    }

    protected boolean isIncludeAlwaysSelf() {
        return true;
    }

    @Nonnull
    @RequiredReadAction
    private static PsiElement[] getSelfAndImplementations(
        Editor editor,
        @Nonnull PsiElement element,
        @Nonnull ImplementationSearcher handler
    ) {
        return getSelfAndImplementations(editor, element, handler, !(element instanceof PomTargetPsiElement));
    }

    @Nonnull
    @RequiredReadAction
    static PsiElement[] getSelfAndImplementations(
        Editor editor,
        @Nonnull PsiElement element,
        @Nonnull ImplementationSearcher handler,
        boolean includeSelfAlways
    ) {
        PsiElement[] handlerImplementations = handler.searchImplementations(element, editor, includeSelfAlways, true);
        if (handlerImplementations.length > 0) {
            return handlerImplementations;
        }

        ThrowableComputable<PsiElement[], RuntimeException> action = () -> {
            PsiElement psiElement = element;
            PsiFile psiFile = psiElement.getContainingFile();
            if (psiFile == null) {
                // Magically, it's null for ant property declarations.
                psiElement = psiElement.getNavigationElement();
                psiFile = psiElement.getContainingFile();
                if (psiFile == null) {
                    return PsiElement.EMPTY_ARRAY;
                }
            }
            if (psiFile.getVirtualFile() != null && (psiElement.getTextRange() != null || psiElement instanceof PsiFile)) {
                return new PsiElement[]{psiElement};
            }
            return PsiElement.EMPTY_ARRAY;
        };
        return ReadAction.compute(action);
    }

    @Nonnull
    private static PsiElement[] filterElements(@Nonnull PsiElement[] targetElements) {
        Set<PsiElement> unique = new LinkedHashSet<>(Arrays.asList(targetElements));
        for (PsiElement elt : targetElements) {
            Application.get().runReadAction(() -> {
                PsiFile containingFile = elt.getContainingFile();
                LOG.assertTrue(containingFile != null, elt);
                PsiFile psiFile = containingFile.getOriginalFile();
                if (psiFile.getVirtualFile() == null) {
                    unique.remove(elt);
                }
            });
        }
        // special case for Python (PY-237)
        // if the definition is the tree parent of the target element, filter out the target element
        for (int i = 1; i < targetElements.length; i++) {
            PsiElement targetElement = targetElements[i];
            if (Application.get().runReadAction((Supplier<Boolean>)() -> PsiTreeUtil.isAncestor(targetElement, targetElements[0], true))) {
                unique.remove(targetElements[0]);
                break;
            }
        }
        return PsiUtilCore.toPsiElementArray(unique);
    }

    protected boolean isSearchDeep() {
        return false;
    }

    private static class ImplementationViewComponentUpdater implements GenericListComponentUpdater<PsiElement> {
        private final ImplementationViewComponentImpl myComponent;
        private final int myIncludeSelfIdx;

        ImplementationViewComponentUpdater(ImplementationViewComponentImpl component, int includeSelfIdx) {
            myComponent = component;
            myIncludeSelfIdx = includeSelfIdx;
        }

        @Override
        public void paintBusy(boolean paintBusy) {
            //todo notify busy
        }

        @Override
        @RequiredUIAccess
        public void replaceModel(@Nonnull List<? extends PsiElement> data) {
            PsiElement[] elements = myComponent.getElements();
            int startIdx = elements.length - myIncludeSelfIdx;
            List<PsiElement> result = new ArrayList<>();
            Collections.addAll(result, elements);
            result.addAll(data.subList(startIdx, data.size()));
            myComponent.update(result.toArray(PsiElement.EMPTY_ARRAY), myComponent.getIndex());
        }
    }

    private class ImplementationsUpdaterTask extends BackgroundUpdaterTaskBase<PsiElement> {
        @Nonnull
        private final LocalizeValue myCaption;
        private final Editor myEditor;
        @Nonnull
        private final PsiElement myElement;
        private final boolean myIncludeSelf;
        private PsiElement[] myElements;

        private final ImplementationViewComponentImpl myComponent;

        private ImplementationsUpdaterTask(
            @Nonnull PsiElement element,
            Editor editor,
            @Nonnull LocalizeValue caption,
            boolean includeSelf,
            ImplementationViewComponentImpl component
        ) {
            super(element.getProject(), ImplementationSearcher.SEARCHING_FOR_IMPLEMENTATIONS, null);
            myCaption = caption;
            myEditor = editor;
            myElement = element;
            myComponent = component;
            myIncludeSelf = includeSelf;
        }

        @Override
        public String getCaption(int size) {
            return myCaption.get();
        }

        @Override
        protected void paintBusy(boolean paintBusy) {
            //todo notify busy
        }

        @Nullable
        @Override
        @RequiredReadAction
        protected Usage createUsage(PsiElement element) {
            return new UsageInfo2UsageAdapter(new UsageInfo(element));
        }

        @Override
        @RequiredReadAction
        public void run(@Nonnull final ProgressIndicator indicator) {
            super.run(indicator);
            ImplementationSearcher.BackgroundableImplementationSearcher implementationSearcher =
                new ImplementationSearcher.BackgroundableImplementationSearcher() {
                    @Override
                    protected boolean isSearchDeep() {
                        return ShowImplementationsAction.this.isSearchDeep();
                    }

                    @Override
                    protected void processElement(PsiElement element) {
                        if (!updateComponent(element, null)) {
                            indicator.cancel();
                        }
                        indicator.checkCanceled();
                    }

                    @Override
                    protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
                        return ShowImplementationsAction.filterElements(targetElements);
                    }
                };
            if (!myIncludeSelf) {
                myElements = getSelfAndImplementations(myEditor, myElement, implementationSearcher, false);
            }
            else {
                myElements = getSelfAndImplementations(myEditor, myElement, implementationSearcher);
            }
        }

        @Override
        public int getCurrentSize() {
            if (myElements != null) {
                return myElements.length;
            }
            return super.getCurrentSize();
        }

        @Override
        @RequiredUIAccess
        public void onSuccess() {
            if (!cancelTask()) {
                myComponent.update(myElements, myComponent.getIndex());
            }
            super.onSuccess();
        }
    }
}
