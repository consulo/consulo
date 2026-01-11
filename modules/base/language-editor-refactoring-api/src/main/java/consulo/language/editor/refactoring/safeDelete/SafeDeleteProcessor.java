/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.safeDelete;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.editor.refactoring.event.RefactoringEventData;
import consulo.language.editor.refactoring.event.RefactoringEventListener;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteCustomUsageInfo;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteReferenceUsageInfo;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteUsageInfo;
import consulo.language.editor.refactoring.ui.RefactoringUIUtil;
import consulo.language.editor.refactoring.util.NonCodeSearchDescriptionLocation;
import consulo.language.editor.refactoring.util.TextOccurrencesUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.usage.*;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author dsl
 */
public class SafeDeleteProcessor extends BaseRefactoringProcessor {
    private static final Logger LOG = Logger.getInstance(SafeDeleteProcessor.class);
    private final PsiElement[] myElements;
    private boolean mySearchInCommentsAndStrings;
    private boolean mySearchNonJava;
    private boolean myPreviewNonCodeUsages = true;

    private SafeDeleteProcessor(
        Project project,
        @Nullable Runnable prepareSuccessfulCallback,
        PsiElement[] elementsToDelete,
        boolean isSearchInComments,
        boolean isSearchNonJava
    ) {
        super(project, prepareSuccessfulCallback);
        myElements = elementsToDelete;
        mySearchInCommentsAndStrings = isSearchInComments;
        mySearchNonJava = isSearchNonJava;
    }

    @Override
    @Nonnull
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        return new SafeDeleteUsageViewDescriptor(myElements);
    }

    @RequiredReadAction
    private static boolean isInside(PsiElement place, PsiElement[] ancestors) {
        return isInside(place, Arrays.asList(ancestors));
    }

    @RequiredReadAction
    private static boolean isInside(PsiElement place, Collection<? extends PsiElement> ancestors) {
        for (PsiElement element : ancestors) {
            if (isInside(place, element)) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    public static boolean isInside(PsiElement place, PsiElement ancestor) {
        if (ancestor instanceof PsiDirectoryContainer directoryContainer) {
            PsiDirectory[] directories = directoryContainer.getDirectories(place.getResolveScope());
            for (PsiDirectory directory : directories) {
                if (isInside(place, directory)) {
                    return true;
                }
            }
        }

        if (ancestor instanceof PsiFile ancestorFile) {
            for (PsiFile file : ancestorFile.getViewProvider().getAllFiles()) {
                if (PsiTreeUtil.isAncestor(file, place, false)) {
                    return true;
                }
            }
        }

        boolean isAncestor = PsiTreeUtil.isAncestor(ancestor, place, false);
        if (!isAncestor && ancestor instanceof PsiNameIdentifierOwner nameIdentifierOwner) {
            PsiElement nameIdentifier = nameIdentifierOwner.getNameIdentifier();
            if (nameIdentifier != null && !PsiTreeUtil.isAncestor(ancestor, nameIdentifier, true)) {
                isAncestor = PsiTreeUtil.isAncestor(nameIdentifier.getParent(), place, false);
            }
        }

        if (!isAncestor) {
            InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(place.getProject());
            PsiLanguageInjectionHost host = injectedLanguageManager.getInjectionHost(place);
            while (host != null) {
                if (PsiTreeUtil.isAncestor(ancestor, host, false)) {
                    isAncestor = true;
                    break;
                }
                host = injectedLanguageManager.getInjectionHost(host);
            }
        }
        return isAncestor;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    protected UsageInfo[] findUsages() {
        List<UsageInfo> usages = Collections.synchronizedList(new ArrayList<UsageInfo>());
        ExtensionPoint<SafeDeleteProcessorDelegate> safeDeleteDelegates =
            myProject.getApplication().getExtensionPoint(SafeDeleteProcessorDelegate.class);

        for (PsiElement element : myElements) {
            SimpleReference<Boolean> handled = SimpleReference.create(false);
            safeDeleteDelegates.forEachBreakable(delegate -> {
                if (!delegate.handlesElement(element)) {
                    return ExtensionPoint.Flow.CONTINUE;
                }
                NonCodeUsageSearchInfo filter = delegate.findUsages(element, myElements, usages);
                if (filter != null) {
                    for (PsiElement nonCodeUsageElement : filter.getElementsToSearch()) {
                        addNonCodeUsages(
                            nonCodeUsageElement,
                            usages,
                            filter.getInsideDeletedCondition(),
                            mySearchNonJava,
                            mySearchInCommentsAndStrings
                        );
                    }
                }
                handled.set(true);
                return ExtensionPoint.Flow.BREAK;
            });
            if (!handled.get() && element instanceof PsiNamedElement) {
                findGenericElementUsages(element, usages, myElements);
                addNonCodeUsages(
                    element,
                    usages,
                    getDefaultInsideDeletedCondition(myElements),
                    mySearchNonJava,
                    mySearchInCommentsAndStrings
                );
            }
        }
        UsageInfo[] result = usages.toArray(new UsageInfo[usages.size()]);
        return UsageViewUtil.removeDuplicatedUsages(result);
    }

    @RequiredReadAction
    public static Predicate<PsiElement> getDefaultInsideDeletedCondition(PsiElement[] elements) {
        return usage -> !(usage instanceof PsiFile) && isInside(usage, elements);
    }

    public static void findGenericElementUsages(
        PsiElement element,
        List<UsageInfo> usages,
        PsiElement[] allElementsToDelete
    ) {
        ReferencesSearch.search(element).forEach(reference -> {
            PsiElement refElement = reference.getElement();
            if (!isInside(refElement, allElementsToDelete)) {
                usages.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(refElement, element, false));
            }
            return true;
        });
    }

    @Override
    @RequiredUIAccess
    protected boolean preprocessUsages(@Nonnull SimpleReference<UsageInfo[]> refUsages) {
        UsageInfo[] usages = refUsages.get();
        List<LocalizeValue> conflicts = new ArrayList<>();

        Application app = myProject.getApplication();
        ExtensionPoint<SafeDeleteProcessorDelegate> safeDeleteDelegates = app.getExtensionPoint(SafeDeleteProcessorDelegate.class);
        for (PsiElement element : myElements) {
            Collection<LocalizeValue> foundConflicts = safeDeleteDelegates.computeSafeIfAny(
                delegate -> delegate.handlesElement(element) ? delegate.findConflicts(element, myElements) : null
            );
            if (foundConflicts != null) {
                conflicts.addAll(foundConflicts);
            }
        }

        Map<PsiElement, UsageHolder> elementsToUsageHolders = sortUsages(usages);
        Collection<UsageHolder> usageHolders = elementsToUsageHolders.values();
        for (UsageHolder usageHolder : usageHolders) {
            if (usageHolder.hasUnsafeUsagesInCode()) {
                conflicts.add(usageHolder.getDescription());
            }
        }

        if (!conflicts.isEmpty()) {
            RefactoringEventData conflictData = new RefactoringEventData();
            conflictData.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts);
            myProject.getMessageBus()
                .syncPublisher(RefactoringEventListener.class)
                .conflictsDetected("refactoring.safeDelete", conflictData);
            if (app.isUnitTestMode()) {
                if (!ConflictsInTestsException.isTestIgnore()) {
                    throw new ConflictsInTestsException(conflicts);
                }
            }
            else {
                UnsafeUsagesDialog dialog = new UnsafeUsagesDialog(conflicts, myProject);
                if (!dialog.showAndGet()) {
                    int exitCode = dialog.getExitCode();
                    prepareSuccessful(); // dialog is always dismissed;
                    if (exitCode == UnsafeUsagesDialog.VIEW_USAGES_EXIT_CODE) {
                        showUsages(usages);
                    }
                    return false;
                }
                else {
                    myPreviewNonCodeUsages = false;
                }
            }
        }

        SimpleReference<UsageInfo[]> preprocessedUsages = SimpleReference.create(usages);
        boolean hasUsages = safeDeleteDelegates.allMatchSafe(delegate -> {
            preprocessedUsages.set(delegate.preprocessUsages(myProject, preprocessedUsages.get()));
            return !preprocessedUsages.isNull();
        });
        if (!hasUsages) {
            return false;
        }
        UsageInfo[] filteredUsages = UsageViewUtil.removeDuplicatedUsages(preprocessedUsages.get());
        prepareSuccessful(); // dialog is always dismissed
        refUsages.set(filteredUsages);
        return true;
    }

    @RequiredReadAction
    private void showUsages(UsageInfo[] usages) {
        UsageViewPresentation presentation = new UsageViewPresentation();
        presentation.setTabText(RefactoringLocalize.safeDeleteTitle());
        presentation.setTargetsNodeText(RefactoringLocalize.attemptingToDeleteTargetsNodeText());
        presentation.setShowReadOnlyStatusAsRed(true);
        presentation.setShowCancelButton(true);
        presentation.setCodeUsagesString(RefactoringLocalize.referencesFoundInCode());
        presentation.setUsagesInGeneratedCodeString(RefactoringLocalize.referencesFoundInGeneratedCode());
        presentation.setNonCodeUsagesString(RefactoringLocalize.occurrencesFoundInCommentsStringsAndNonJavaFiles());
        presentation.setUsagesString(RefactoringLocalize.usageviewUsagestext());

        UsageViewManager manager = UsageViewManager.getInstance(myProject);
        UsageView usageView = showUsages(usages, presentation, manager);
        usageView.addPerformOperationAction(
            new RerunSafeDelete(myProject, myElements, usageView),
            RefactoringLocalize.retryCommand(),
            LocalizeValue.empty(),
            RefactoringLocalize.rerunSafeDelete()
        );
        usageView.addPerformOperationAction(
            () -> {
                ExtensionPoint<SafeDeleteProcessorDelegate> safeDeleteDelegates =
                    myProject.getApplication().getExtensionPoint(SafeDeleteProcessorDelegate.class);
                SimpleReference<UsageInfo[]> preprocessedUsages = SimpleReference.create(usages);
                boolean hasUsages = safeDeleteDelegates.allMatchSafe(delegate -> {
                    preprocessedUsages.set(delegate.preprocessUsages(myProject, preprocessedUsages.get()));
                    return !preprocessedUsages.isNull();
                });
                if (!hasUsages) {
                    return;
                }
                UsageInfo[] filteredUsages = UsageViewUtil.removeDuplicatedUsages(preprocessedUsages.get());
                execute(filteredUsages);
            },
            LocalizeValue.localizeTODO("Delete Anyway"),
            RefactoringLocalize.usageviewNeedRerun(),
            RefactoringLocalize.usageviewDoaction()
        );
    }

    @RequiredReadAction
    private UsageView showUsages(UsageInfo[] usages, UsageViewPresentation presentation, UsageViewManager manager) {
        ExtensionPoint<SafeDeleteProcessorDelegate> safeDeleteDelegates =
            myProject.getApplication().getExtensionPoint(SafeDeleteProcessorDelegate.class);
        UsageView view = safeDeleteDelegates.computeSafeIfAny(delegate -> {
            if (delegate instanceof SafeDeleteProcessorDelegateBase safeDeleteProcessorDelegateBase) {
                return safeDeleteProcessorDelegateBase.showUsages(usages, presentation, manager, myElements);
            }
            return null;
        });
        if (view != null) {
            return view;
        }
        UsageTarget[] targets = new UsageTarget[myElements.length];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = RefactoringInternalHelper.getInstance().createPsiElement2UsageTargetAdapter(myElements[i]);
        }

        return manager.showUsages(targets, UsageInfoToUsageConverter.convert(myElements, usages), presentation);
    }

    public PsiElement[] getElements() {
        return myElements;
    }

    private static class RerunSafeDelete implements Runnable {
        SmartPsiElementPointer[] myPointers;
        private final Project myProject;
        private final UsageView myUsageView;

        RerunSafeDelete(Project project, PsiElement[] elements, UsageView usageView) {
            myProject = project;
            myUsageView = usageView;
            myPointers = new SmartPsiElementPointer[elements.length];
            for (int i = 0; i < elements.length; i++) {
                PsiElement element = elements[i];
                myPointers[i] = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(element);
            }
        }

        @Override
        public void run() {
            myProject.getApplication().invokeLater(() -> {
                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                myUsageView.close();
                List<PsiElement> elements = new ArrayList<>();
                for (SmartPsiElementPointer pointer : myPointers) {
                    PsiElement element = pointer.getElement();
                    if (element != null) {
                        elements.add(element);
                    }
                }
                if (!elements.isEmpty()) {
                    SafeDeleteHandler.invoke(myProject, PsiUtilCore.toPsiElementArray(elements), true);
                }
            });
        }
    }

    /**
     * @param usages
     * @return Map from elements to UsageHolders
     */
    @RequiredReadAction
    private static Map<PsiElement, UsageHolder> sortUsages(@Nonnull UsageInfo[] usages) {
        Map<PsiElement, UsageHolder> result = new HashMap<>();

        for (UsageInfo usage : usages) {
            if (usage instanceof SafeDeleteUsageInfo safeDeleteUsageInfo) {
                PsiElement referencedElement = safeDeleteUsageInfo.getReferencedElement();
                if (!result.containsKey(referencedElement)) {
                    result.put(referencedElement, new UsageHolder(referencedElement, usages));
                }
            }
        }
        return result;
    }


    @Override
    protected void refreshElements(@Nonnull PsiElement[] elements) {
        LOG.assertTrue(elements.length == myElements.length);
        System.arraycopy(elements, 0, myElements, 0, elements.length);
    }

    @Override
    @RequiredReadAction
    protected boolean isPreviewUsages(@Nonnull UsageInfo[] usages) {
        return myPreviewNonCodeUsages && UsageViewUtil.reportNonRegularUsages(usages, myProject)
            || super.isPreviewUsages(filterToBeDeleted(usages));
    }

    private static UsageInfo[] filterToBeDeleted(UsageInfo[] infos) {
        List<UsageInfo> list = new ArrayList<>();
        for (UsageInfo info : infos) {
            if (!(info instanceof SafeDeleteReferenceUsageInfo safeDeleteReferenceUsageInfo)
                || safeDeleteReferenceUsageInfo.isSafeDelete()) {
                list.add(info);
            }
        }
        return list.toArray(new UsageInfo[list.size()]);
    }

    @Nullable
    @Override
    protected RefactoringEventData getBeforeData() {
        RefactoringEventData beforeData = new RefactoringEventData();
        beforeData.addElements(myElements);
        return beforeData;
    }

    @Nullable
    @Override
    protected String getRefactoringId() {
        return "refactoring.safeDelete";
    }

    @Override
    @RequiredWriteAction
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
        try {
            for (UsageInfo usage : usages) {
                if (usage instanceof SafeDeleteCustomUsageInfo safeDeleteCustomUsageInfo) {
                    safeDeleteCustomUsageInfo.performRefactoring();
                }
            }

            ExtensionPoint<SafeDeleteProcessorDelegate> safeDeleteDelegates =
                myProject.getApplication().getExtensionPoint(SafeDeleteProcessorDelegate.class);
            for (PsiElement element : myElements) {
                safeDeleteDelegates.forEach(delegate -> {
                    if (delegate.handlesElement(element)) {
                        delegate.prepareForDeletion(element);
                    }
                });

                element.delete();
            }
        }
        catch (IncorrectOperationException e) {
            RefactoringUIUtil.processIncorrectOperation(myProject, e);
        }
    }

    private LocalizeValue calcCommandName() {
        return RefactoringLocalize.safeDeleteCommand(RefactoringUIUtil.calculatePsiElementDescriptionList(myElements));
    }

    @Nonnull
    private LocalizeValue myCachedCommandName = LocalizeValue.empty();

    @Nonnull
    @Override
    protected LocalizeValue getCommandName() {
        if (myCachedCommandName.isEmpty()) {
            myCachedCommandName = calcCommandName();
        }
        return myCachedCommandName;
    }

    public static void addNonCodeUsages(
        PsiElement element,
        List<UsageInfo> usages,
        @Nullable Predicate<PsiElement> insideElements,
        boolean searchNonJava,
        boolean searchInCommentsAndStrings
    ) {
        UsageInfoFactory nonCodeUsageFactory = (usage, startOffset, endOffset) -> {
            if (insideElements != null && insideElements.test(usage)) {
                return null;
            }
            return new SafeDeleteReferenceSimpleDeleteUsageInfo(usage, element, startOffset, endOffset, true, false);
        };
        if (searchInCommentsAndStrings) {
            String stringToSearch =
                ElementDescriptionUtil.getElementDescription(element, NonCodeSearchDescriptionLocation.STRINGS_AND_COMMENTS);
            TextOccurrencesUtil.addUsagesInStringsAndComments(element, stringToSearch, usages, nonCodeUsageFactory);
        }
        if (searchNonJava) {
            String stringToSearch = ElementDescriptionUtil.getElementDescription(element, NonCodeSearchDescriptionLocation.NON_JAVA);
            TextOccurrencesUtil.addTextOccurences(
                element,
                stringToSearch,
                GlobalSearchScope.projectScope(element.getProject()),
                usages,
                nonCodeUsageFactory
            );
        }
    }

    @Override
    @RequiredReadAction
    protected boolean isToBeChanged(@Nonnull UsageInfo usageInfo) {
        if (usageInfo instanceof SafeDeleteReferenceUsageInfo safeDeleteReferenceUsageInfo) {
            return safeDeleteReferenceUsageInfo.isSafeDelete() && super.isToBeChanged(usageInfo);
        }
        return super.isToBeChanged(usageInfo);
    }

    @RequiredReadAction
    public static boolean validElement(@Nonnull PsiElement element) {
        if (element instanceof PsiFile) {
            return true;
        }
        if (!element.isPhysical()) {
            return false;
        }
        RefactoringSupportProvider provider = RefactoringSupportProvider.forLanguage(element.getLanguage());
        return provider.isSafeDeleteAvailable(element);
    }

    public static SafeDeleteProcessor createInstance(
        Project project,
        @Nullable Runnable prepareSuccessfulCallback,
        PsiElement[] elementsToDelete,
        boolean isSearchInComments,
        boolean isSearchNonJava
    ) {
        return new SafeDeleteProcessor(project, prepareSuccessfulCallback, elementsToDelete, isSearchInComments, isSearchNonJava);
    }

    public static SafeDeleteProcessor createInstance(
        @Nonnull Project project,
        @Nullable Runnable prepareSuccessfulCallBack,
        PsiElement[] elementsToDelete,
        boolean isSearchInComments,
        boolean isSearchNonJava,
        boolean askForAccessors
    ) {
        List<PsiElement> elements = new ArrayList<>(Arrays.asList(elementsToDelete));
        Set<PsiElement> elementsToDeleteSet = new HashSet<>(Arrays.asList(elementsToDelete));

        ExtensionPoint<SafeDeleteProcessorDelegate> safeDeleteDelegates =
            project.getApplication().getExtensionPoint(SafeDeleteProcessorDelegate.class);

        for (PsiElement psiElement : elementsToDelete) {
            safeDeleteDelegates.forEachBreakable(delegate -> {
                if (!delegate.handlesElement(psiElement)) {
                    return ExtensionPoint.Flow.CONTINUE;
                }
                Collection<PsiElement> addedElements =
                    delegate.getAdditionalElementsToDelete(psiElement, elementsToDeleteSet, askForAccessors);
                if (addedElements != null) {
                    elements.addAll(addedElements);
                }
                return ExtensionPoint.Flow.BREAK;
            });
        }

        return new SafeDeleteProcessor(
            project,
            prepareSuccessfulCallBack,
            PsiUtilCore.toPsiElementArray(elements),
            isSearchInComments,
            isSearchNonJava
        );
    }

    public boolean isSearchInCommentsAndStrings() {
        return mySearchInCommentsAndStrings;
    }

    public void setSearchInCommentsAndStrings(boolean searchInCommentsAndStrings) {
        mySearchInCommentsAndStrings = searchInCommentsAndStrings;
    }

    public boolean isSearchNonJava() {
        return mySearchNonJava;
    }

    public void setSearchNonJava(boolean searchNonJava) {
        mySearchNonJava = searchNonJava;
    }

    @Override
    protected boolean skipNonCodeUsages() {
        return true;
    }
}
