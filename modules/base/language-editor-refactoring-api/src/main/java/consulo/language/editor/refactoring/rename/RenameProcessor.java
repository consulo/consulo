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
package consulo.language.editor.refactoring.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.component.extension.ExtensionPoint;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.event.RefactoringEventData;
import consulo.language.editor.refactoring.event.RefactoringEventListener;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.ConflictsDialogDescriptor;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.RelatedUsageInfo;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.MessageBoxes;
import consulo.usage.*;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RenameProcessor extends BaseRefactoringProcessor {
    private static final Logger LOG = Logger.getInstance(RenameProcessor.class);

    protected final Map<PsiElement, String> myAllRenames = new LinkedHashMap<>();

    
    private PsiElement myPrimaryElement;
    private String myNewName = null;

    private boolean mySearchInComments;
    private boolean mySearchTextOccurrences;
    protected boolean myForceShowPreview;

    
    private LocalizeValue myCommandName;

    private NonCodeUsageInfo[] myNonCodeUsages = new NonCodeUsageInfo[0];
    private final List<AutomaticRenamerFactory> myRenamerFactories = new ArrayList<>();
    private final List<AutomaticRenamer> myRenamers = new ArrayList<>();
    private final List<UnresolvableCollisionUsageInfo> mySkippedUsages = new ArrayList<>();

    @RequiredReadAction
    public RenameProcessor(
        Project project,
        PsiElement element,
        String newName,
        boolean isSearchInComments,
        boolean isSearchTextOccurrences
    ) {
        super(project);
        myPrimaryElement = element;

        assertNonCompileElement(element);
        //assertValidName(element, newName);

        mySearchInComments = isSearchInComments;
        mySearchTextOccurrences = isSearchTextOccurrences;

        setNewName(newName);
    }

    public Set<PsiElement> getElements() {
        return Collections.unmodifiableSet(myAllRenames.keySet());
    }

    public String getNewName(PsiElement element) {
        return myAllRenames.get(element);
    }

    public void addRenamerFactory(AutomaticRenamerFactory factory) {
        if (!myRenamerFactories.contains(factory)) {
            myRenamerFactories.add(factory);
        }
    }

    public void removeRenamerFactory(AutomaticRenamerFactory factory) {
        myRenamerFactories.remove(factory);
    }

    @Override
    @RequiredUIAccess
    public void doRun() {
        if (!myPrimaryElement.isValid()) {
            return;
        }
        CompletableFuture<?> prepared = prepareRenamingAsync(myPrimaryElement, myNewName, myAllRenames);
        if (prepared.isDone()) {
            joinRethrowing(prepared);
            super.doRun();
            return;
        }

        prepared.whenCompleteAsync((result, error) -> {
            if (error == null && myPrimaryElement.isValid()) {
                super.doRun();
            }
        }, UIAccess.current());
    }

    public void prepareRenaming(
        PsiElement element,
        String newName,
        Map<PsiElement, String> allRenames
    ) {
        List<RenamePsiElementProcessor> processors = RenamePsiElementProcessor.allForElement(element);
        myForceShowPreview = false;
        for (RenamePsiElementProcessor processor : processors) {
            if (processor.canProcessElement(element)) {
                processor.prepareRenaming(element, newName, allRenames);
                myForceShowPreview |= processor.forcesShowPreview();
            }
        }
    }

    /**
     * Asynchronous form of {@link #prepareRenaming(PsiElement, String, Map)}: the processors which ask the user are
     * answered one after another.
     */
    @RequiredUIAccess
    public CompletableFuture<?> prepareRenamingAsync(
        PsiElement element,
        String newName,
        Map<PsiElement, String> allRenames
    ) {
        myForceShowPreview = false;
        return prepareRenamingAsync(RenamePsiElementProcessor.allForElement(element).iterator(), element, newName, allRenames);
    }

    @RequiredUIAccess
    private CompletableFuture<?> prepareRenamingAsync(
        Iterator<RenamePsiElementProcessor> processors,
        PsiElement element,
        String newName,
        Map<PsiElement, String> allRenames
    ) {
        while (processors.hasNext()) {
            RenamePsiElementProcessor processor = processors.next();
            if (!processor.canProcessElement(element)) {
                continue;
            }
            CompletableFuture<?> prepared = processor.prepareRenamingAsync(element, newName, allRenames);
            if (!prepared.isDone()) {
                return prepared.thenCompose(result -> {
                    myForceShowPreview |= processor.forcesShowPreview();
                    return prepareRenamingAsync(processors, element, newName, allRenames);
                });
            }
            joinRethrowing(prepared);
            myForceShowPreview |= processor.forcesShowPreview();
        }
        return CompletableFuture.completedFuture(null);
    }

    private @Nullable String getHelpID() {
        return RenamePsiElementProcessor.forElement(myPrimaryElement).getHelpID(myPrimaryElement);
    }

    @Override
    @RequiredUIAccess
    public boolean preprocessUsages(SimpleReference<UsageInfo[]> refUsages) {
        CompletableFuture<Boolean> result = preprocessUsagesAsync(refUsages);
        return result.isDone() && Boolean.TRUE.equals(joinRethrowing(result));
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Boolean> preprocessUsagesAsync(SimpleReference<UsageInfo[]> refUsages) {
        UsageInfo[] usagesIn = refUsages.get();
        MultiMap<PsiElement, LocalizeValue> conflicts = new MultiMap<>();

        RenameUtil.addConflictDescriptions(usagesIn, conflicts);
        RenamePsiElementProcessor.forElement(myPrimaryElement)
            .findExistingNameConflicts(myPrimaryElement, myNewName, conflicts, myAllRenames);

        CompletableFuture<Boolean> conflictsResolved = CompletableFuture.completedFuture(Boolean.TRUE);
        if (!conflicts.isEmpty()) {
            RefactoringEventData conflictData = new RefactoringEventData();
            conflictData.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts.values());
            myProject.getMessageBus().syncPublisher(RefactoringEventListener.class)
                .conflictsDetected("refactoring.rename", conflictData);

            if (myProject.getApplication().isUnitTestMode()) {
                throw new ConflictsInTestsException(conflicts.values());
            }
            ConflictsDialogDescriptor conflictsDialog = prepareConflictsDialog(conflicts, refUsages.get());
            conflictsResolved = conflictsDialog.showAsync().thenApply(proceed -> {
                if (!proceed && conflictsDialog.isShowConflicts()) {
                    prepareSuccessful();
                }
                return proceed;
            });
        }

        return conflictsResolved.thenCompose(proceed -> {
            if (!proceed) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            return preprocessRenames(refUsages, usagesIn);
        });
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> preprocessRenames(SimpleReference<UsageInfo[]> refUsages, UsageInfo[] usagesIn) {
        List<UsageInfo> variableUsages = new ArrayList<>();

        CompletableFuture<Boolean> variablesFound = CompletableFuture.completedFuture(Boolean.TRUE);
        if (!myRenamers.isEmpty()) {
            variablesFound = findRenamedVariables(variableUsages).thenCompose(
                found -> found ? addRenamedVariables(variableUsages) : CompletableFuture.completedFuture(Boolean.FALSE)
            );
        }

        return variablesFound
            .thenCompose(found -> {
                if (!found) {
                    return CompletableFuture.completedFuture(Boolean.FALSE);
                }
                int[] choice = myAllRenames.size() > 1 ? new int[]{-1} : null;
                return checkRenames(myAllRenames.entrySet().iterator(), choice);
            })
            .thenApply(checked -> checked && finishPreprocess(refUsages, usagesIn, variableUsages));
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> addRenamedVariables(List<UsageInfo> variableUsages) {
        Map<PsiElement, String> renames = new LinkedHashMap<>();
        List<Map.Entry<PsiNamedElement, String>> variables = new ArrayList<>();
        for (AutomaticRenamer renamer : myRenamers) {
            for (PsiNamedElement variable : renamer.getElements()) {
                String newName = renamer.getNewName(variable);
                if (newName != null) {
                    variables.add(Map.entry(variable, newName));
                }
            }
        }

        return prepareVariables(variables.iterator(), renames).thenCompose(prepared -> {
            if (renames.isEmpty()) {
                return CompletableFuture.completedFuture(Boolean.TRUE);
            }

            for (PsiElement element : renames.keySet()) {
                assertNonCompileElement(element);
            }
            myAllRenames.putAll(renames);
            Runnable runnable = () -> {
                for (Map.Entry<PsiElement, String> entry : renames.entrySet()) {
                    UsageInfo[] usages = ReadAction.compute(() -> RenameUtil.findUsages(
                        entry.getKey(),
                        entry.getValue(),
                        mySearchInComments,
                        mySearchTextOccurrences,
                        myAllRenames
                    ));
                    Collections.addAll(variableUsages, usages);
                }
            };
            return CommonRefactoringUtil.runWithProgress(myProject, RefactoringLocalize.searchingForVariables(), runnable);
        });
    }

    @RequiredUIAccess
    private CompletableFuture<?> prepareVariables(Iterator<Map.Entry<PsiNamedElement, String>> variables, Map<PsiElement, String> renames) {
        while (variables.hasNext()) {
            Map.Entry<PsiNamedElement, String> variable = variables.next();
            addElement(variable.getKey(), variable.getValue());
            CompletableFuture<?> prepared = prepareRenamingAsync(variable.getKey(), variable.getValue(), renames);
            if (!prepared.isDone()) {
                return prepared.thenCompose(result -> prepareVariables(variables, renames));
            }
            joinRethrowing(prepared);
        }
        return CompletableFuture.completedFuture(null);
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> checkRenames(Iterator<Map.Entry<PsiElement, String>> iterator, int @Nullable [] choice) {
        while (iterator.hasNext()) {
            Map.Entry<PsiElement, String> entry = iterator.next();
            if (entry.getKey() instanceof PsiFile file) {
                return CommonRefactoringUtil.checkFileExistAsync(
                    file.getContainingDirectory(),
                    choice,
                    file,
                    entry.getValue(),
                    RefactoringLocalize.commandNameRename()
                ).thenCompose(skip -> {
                    if (skip) {
                        iterator.remove();
                    }
                    else if (!checkRename(entry)) {
                        return CompletableFuture.completedFuture(Boolean.FALSE);
                    }
                    return checkRenames(iterator, choice);
                });
            }
            if (!checkRename(entry)) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
        }
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    @RequiredUIAccess
    private boolean checkRename(Map.Entry<PsiElement, String> entry) {
        try {
            RenameUtil.checkRename(entry.getKey(), entry.getValue());
            return true;
        }
        catch (IncorrectOperationException e) {
            LOG.warn("Rename failed", e);
            CommonRefactoringUtil.showErrorMessage(RefactoringLocalize.renameTitle(), LocalizeValue.of(e), getHelpID(), myProject);
            return false;
        }
    }

    @RequiredUIAccess
    private boolean finishPreprocess(SimpleReference<UsageInfo[]> refUsages, UsageInfo[] usagesIn, List<UsageInfo> variableUsages) {
        Set<UsageInfo> usagesSet = new LinkedHashSet<>(Arrays.asList(usagesIn));
        usagesSet.addAll(variableUsages);
        List<UnresolvableCollisionUsageInfo> conflictUsages = RenameUtil.removeConflictUsages(usagesSet);
        if (conflictUsages != null) {
            mySkippedUsages.addAll(conflictUsages);
        }
        refUsages.set(usagesSet.toArray(new UsageInfo[usagesSet.size()]));

        prepareSuccessful();
        return PsiElementRenameHandler.canRename(myProject, null, myPrimaryElement);
    }

    public static void assertNonCompileElement(PsiElement element) {
        LOG.assertTrue(!(element instanceof PsiCompiledElement), element);
    }

    @RequiredReadAction
    private void assertValidName(PsiElement element, String newName) {
        LOG.assertTrue(RenameUtil.isValidName(myProject, element, newName), "element: " + element + ", newName: " + newName);
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> findRenamedVariables(List<UsageInfo> variableUsages) {
        return askAutomaticRenamers(myRenamers.iterator()).thenCompose(asked -> {
            Runnable runnable = () -> myProject.getApplication().runReadAction(() -> {
                for (AutomaticRenamer renamer : myRenamers) {
                    renamer.findUsages(variableUsages, mySearchInComments, mySearchTextOccurrences, mySkippedUsages, myAllRenames);
                }
            });

            return CommonRefactoringUtil.runWithProgress(myProject, RefactoringLocalize.searchingForVariables(), runnable);
        });
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> askAutomaticRenamers(Iterator<AutomaticRenamer> iterator) {
        while (iterator.hasNext()) {
            AutomaticRenamer automaticVariableRenamer = iterator.next();
            if (!automaticVariableRenamer.hasAnythingToRename()) {
                continue;
            }
            return showAutomaticRenamingDialog(automaticVariableRenamer).thenCompose(accepted -> {
                if (!accepted) {
                    iterator.remove();
                }
                return askAutomaticRenamers(iterator);
            });
        }
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    @RequiredUIAccess
    protected CompletableFuture<Boolean> showAutomaticRenamingDialog(AutomaticRenamer automaticVariableRenamer) {
        if (myProject.getApplication().isUnitTestMode()) {
            for (PsiNamedElement element : automaticVariableRenamer.getElements()) {
                automaticVariableRenamer.setRename(element, automaticVariableRenamer.getNewName(element));
            }
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
        return new AutomaticRenamingDialog(myProject, automaticVariableRenamer).showAsync();
    }

    public void addElement(PsiElement element, String newName) {
        assertNonCompileElement(element);
        myAllRenames.put(element, newName);
    }

    @RequiredReadAction
    private void setNewName(String newName) {
        myNewName = newName;
        myAllRenames.put(myPrimaryElement, newName);
        myCommandName = RefactoringLocalize.renaming01To2(
            UsageViewUtil.getType(myPrimaryElement),
            DescriptiveNameUtil.getDescriptiveName(myPrimaryElement),
            newName
        );
    }

    
    @Override
    @RequiredReadAction
    protected UsageViewDescriptor createUsageViewDescriptor(UsageInfo[] usages) {
        return new RenameViewDescriptor(myAllRenames);
    }

    
    @Override
    @RequiredReadAction
    public UsageInfo[] findUsages() {
        myRenamers.clear();
        List<UsageInfo> result = new ArrayList<>();

        List<PsiElement> elements = new ArrayList<>(myAllRenames.keySet());
        ExtensionPoint<AutomaticRenamerFactory> renamerFactoryEP =
            myProject.getApplication().getExtensionPoint(AutomaticRenamerFactory.class);
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < elements.size(); i++) {
            PsiElement element = elements.get(i);
            if (element == null) {
                LOG.error("primary: " + myPrimaryElement + "; renamers: " + myRenamers);
                continue;
            }
            String newName = myAllRenames.get(element);
            UsageInfo[] usages = RenameUtil.findUsages(element, newName, mySearchInComments, mySearchTextOccurrences, myAllRenames);
            List<UsageInfo> usagesList = Arrays.asList(usages);
            result.addAll(usagesList);

            for (AutomaticRenamerFactory factory : myRenamerFactories) {
                if (factory.isApplicable(element)) {
                    myRenamers.add(factory.createRenamer(element, newName, usagesList));
                }
            }

            renamerFactoryEP.forEach(factory -> {
                if (factory.getOptionName().isEmpty() && factory.isApplicable(element)) {
                    myRenamers.add(factory.createRenamer(element, newName, usagesList));
                }
            });
        }
        UsageInfo[] usageInfos = result.toArray(new UsageInfo[result.size()]);
        usageInfos = UsageViewUtil.removeDuplicatedUsages(usageInfos);
        return usageInfos;
    }

    @Override
    protected void refreshElements(PsiElement[] elements) {
        LOG.assertTrue(elements.length > 0);
        myPrimaryElement = elements[0];

        Iterator<String> newNames = myAllRenames.values().iterator();
        Map<PsiElement, String> newAllRenames = new LinkedHashMap<>();
        for (PsiElement resolved : elements) {
            newAllRenames.put(resolved, newNames.next());
        }
        myAllRenames.clear();
        myAllRenames.putAll(newAllRenames);
    }

    @Override
    @RequiredReadAction
    protected boolean isPreviewUsages(UsageInfo[] usages) {
        return myForceShowPreview || super.isPreviewUsages(usages) || UsageViewUtil.reportNonRegularUsages(usages, myProject);
    }

    @Override
    protected @Nullable String getRefactoringId() {
        return "refactoring.rename";
    }

    @Override
    protected @Nullable RefactoringEventData getBeforeData() {
        RefactoringEventData data = new RefactoringEventData();
        data.addElement(myPrimaryElement);
        return data;
    }

    @Override
    protected @Nullable RefactoringEventData getAfterData(UsageInfo[] usages) {
        RefactoringEventData data = new RefactoringEventData();
        data.addElement(myPrimaryElement);
        return data;
    }

    @Override
    @RequiredUIAccess
    @RequiredWriteAction
    public void performRefactoring(UsageInfo[] usages) {
        List<Runnable> postRenameCallbacks = new ArrayList<>();

        MultiMap<PsiElement, UsageInfo> classified = classifyUsages(myAllRenames.keySet(), usages);
        for (PsiElement element : myAllRenames.keySet()) {
            String newName = myAllRenames.get(element);

            RefactoringElementListener elementListener = getTransaction().getElementListener(element);
            RenamePsiElementProcessor renamePsiElementProcessor = RenamePsiElementProcessor.forElement(element);
            Runnable postRenameCallback = renamePsiElementProcessor.getPostRenameCallback(element, newName, elementListener);
            Collection<UsageInfo> infos = classified.get(element);
            try {
                RenameUtil.doRename(element, newName, infos.toArray(new UsageInfo[infos.size()]), myProject, elementListener);
            }
            catch (IncorrectOperationException e) {
                LOG.warn("Rename failed", e);
                RenameUtil.showErrorMessage(e, element, myProject);
                return;
            }
            if (postRenameCallback != null) {
                postRenameCallbacks.add(postRenameCallback);
            }
        }

        for (Runnable runnable : postRenameCallbacks) {
            runnable.run();
        }

        List<NonCodeUsageInfo> nonCodeUsages = new ArrayList<>();
        for (UsageInfo usage : usages) {
            if (usage instanceof NonCodeUsageInfo nonCodeUsageInfo) {
                nonCodeUsages.add(nonCodeUsageInfo);
            }
        }
        myNonCodeUsages = nonCodeUsages.toArray(new NonCodeUsageInfo[nonCodeUsages.size()]);
        if (!mySkippedUsages.isEmpty()) {
            Application application = myProject.getApplication();
            if (!application.isUnitTestMode() && !application.isHeadlessEnvironment()) {
                application.invokeLater(
                    () -> {
                        IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(myProject);
                        if (ideFrame != null) {
                            StatusBar statusBar = ideFrame.getStatusBar();
                            @RequiredUIAccess
                            ComponentEventListener<Component, HyperlinkEvent> listener = e -> MessageBoxes.okInfo(
                                    LocalizeValue.localizeTODO("Following usages were safely skipped:")
                                )
                                .title(LocalizeValue.localizeTODO("Not All Usages Were Renamed"))
                                .detail(LocalizeValue.of(StringUtil.join(
                                    mySkippedUsages,
                                    unresolvableCollisionUsageInfo -> unresolvableCollisionUsageInfo.getDescription().get(),
                                    "\n"
                                )))
                                .showAsync(myProject);
                            statusBar.notifyByBalloon(
                                StatusBar.PROGRESS_WIDGET_ID,
                                NotificationType.WARNING,
                                "<html><body>Unable to rename certain usages. <a href=\"\">Browse</a></body></html>",
                                null,
                                listener
                            );
                        }
                    },
                    Application.get().getNoneModalityState()
                );
            }
        }
    }

    @Override
    @RequiredWriteAction
    protected void performPsiSpoilingRefactoring() {
        RenameUtil.renameNonCodeUsages(myProject, myNonCodeUsages);
    }

    
    @Override
    protected LocalizeValue getCommandName() {
        return myCommandName;
    }

    @RequiredReadAction
    public static MultiMap<PsiElement, UsageInfo> classifyUsages(Collection<? extends PsiElement> elements, UsageInfo[] usages) {
        MultiMap<PsiElement, UsageInfo> result = new MultiMap<>();
        for (UsageInfo usage : usages) {
            LOG.assertTrue(usage instanceof MoveRenameUsageInfo);
            if (usage.getReference() instanceof LightweightPsiElement) {
                continue; //filter out implicit references (e.g. from derived class to super class' default constructor)
            }
            MoveRenameUsageInfo usageInfo = (MoveRenameUsageInfo) usage;
            if (usage instanceof RelatedUsageInfo relatedUsageInfo) {
                PsiElement relatedElement = relatedUsageInfo.getRelatedElement();
                if (elements.contains(relatedElement)) {
                    result.putValue(relatedElement, usage);
                }
            }
            else {
                PsiElement referenced = usageInfo.getReferencedElement();
                if (elements.contains(referenced)) {
                    result.putValue(referenced, usage);
                }
                else if (referenced != null) {
                    PsiElement indirect = referenced.getNavigationElement();
                    if (elements.contains(indirect)) {
                        result.putValue(indirect, usage);
                    }
                }
            }
        }
        return result;
    }

    public Collection<String> getNewNames() {
        return myAllRenames.values();
    }

    public void setSearchInComments(boolean value) {
        mySearchInComments = value;
    }

    public void setSearchTextOccurrences(boolean searchTextOccurrences) {
        mySearchTextOccurrences = searchTextOccurrences;
    }

    public boolean isSearchInComments() {
        return mySearchInComments;
    }

    public boolean isSearchTextOccurrences() {
        return mySearchTextOccurrences;
    }

    public void setCommandName(LocalizeValue commandName) {
        myCommandName = commandName;
    }
}
