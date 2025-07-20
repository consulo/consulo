// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.refactoring;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.component.ProcessCanceledException;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataManager;
import consulo.language.Language;
import consulo.language.editor.refactoring.event.RefactoringEventData;
import consulo.language.editor.refactoring.event.RefactoringEventListener;
import consulo.language.editor.refactoring.event.RefactoringListenerManager;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.editor.refactoring.internal.RefactoringListenerManagerEx;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.ConflictsDialog;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.ModuleManager;
import consulo.module.UnloadedModuleDescription;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.*;
import consulo.usage.*;
import consulo.usage.internal.UsageViewEx;
import consulo.usage.rule.PsiElementUsage;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class BaseRefactoringProcessor implements Runnable {
    private static final Logger LOG = Logger.getInstance(BaseRefactoringProcessor.class);
    private static boolean PREVIEW_IN_TESTS = true;

    protected final Project myProject;
    protected final SearchScope myRefactoringScope;

    private RefactoringTransaction myTransaction;
    private boolean myIsPreviewUsages;
    protected Runnable myPrepareSuccessfulSwingThreadCallback;
    private UsageView myUsageView = null;

    protected BaseRefactoringProcessor(@Nonnull Project project) {
        this(project, null);
    }

    protected BaseRefactoringProcessor(@Nonnull Project project, @Nullable Runnable prepareSuccessfulCallback) {
        this(project, GlobalSearchScope.projectScope(project), prepareSuccessfulCallback);
    }

    protected BaseRefactoringProcessor(
        @Nonnull Project project,
        @Nonnull SearchScope refactoringScope,
        @Nullable Runnable prepareSuccessfulCallback
    ) {
        myProject = project;
        myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulCallback;
        myRefactoringScope = refactoringScope;
    }

    @Nonnull
    protected abstract UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages);

    /**
     * Is called inside atomic action.
     */
    @Nonnull
    protected abstract UsageInfo[] findUsages();

    /**
     * is called when usage search is re-run.
     *
     * @param elements - refreshed elements that are returned by UsageViewDescriptor.getElements()
     */
    protected void refreshElements(@Nonnull PsiElement[] elements) {
    }

    /**
     * Is called inside atomic action.
     *
     * @param refUsages usages to be filtered
     * @return true if preprocessed successfully
     */
    @RequiredUIAccess
    protected boolean preprocessUsages(@Nonnull SimpleReference<UsageInfo[]> refUsages) {
        prepareSuccessful();
        return true;
    }

    /**
     * Is called inside atomic action.
     */
    protected boolean isPreviewUsages(@Nonnull UsageInfo[] usages) {
        return myIsPreviewUsages;
    }

    protected boolean isPreviewUsages() {
        return myIsPreviewUsages;
    }

    private Set<UnloadedModuleDescription> computeUnloadedModulesFromUseScope(UsageViewDescriptor descriptor) {
        if (ModuleManager.getInstance(myProject).getUnloadedModuleDescriptions().isEmpty()) {
            //optimization
            return Collections.emptySet();
        }

        Set<UnloadedModuleDescription> unloadedModulesInUseScope = new LinkedHashSet<>();
        for (PsiElement element : descriptor.getElements()) {
            SearchScope useScope = element.getUseScope();
            if (useScope instanceof GlobalSearchScope globalSearchScope) {
                unloadedModulesInUseScope.addAll(globalSearchScope.getUnloadedModulesBelongingToScope());
            }
        }
        return unloadedModulesInUseScope;
    }


    public void setPreviewUsages(boolean isPreviewUsages) {
        myIsPreviewUsages = isPreviewUsages;
    }

    public void setPrepareSuccessfulSwingThreadCallback(Runnable prepareSuccessfulSwingThreadCallback) {
        myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulSwingThreadCallback;
    }

    protected RefactoringTransaction getTransaction() {
        return myTransaction;
    }

    /**
     * Is called in a command and inside atomic action.
     */
    protected abstract void performRefactoring(@Nonnull UsageInfo[] usages);

    @Nonnull
    protected abstract String getCommandName();

    @RequiredUIAccess
    protected void doRun() {
        if (!PsiDocumentManager.getInstance(myProject).commitAllDocumentsUnderProgress()) {
            return;
        }
        SimpleReference<UsageInfo[]> refUsages = new SimpleReference<>();
        SimpleReference<Language> refErrorLanguage = new SimpleReference<>();
        SimpleReference<Boolean> refProcessCanceled = new SimpleReference<>();
        SimpleReference<Boolean> anyException = new SimpleReference<>();

        DumbService.getInstance(myProject).completeJustSubmittedTasks();

        Runnable findUsagesRunnable = () -> {
            try {
                refUsages.set(AccessRule.read(this::findUsages));
            }
            catch (UnknownReferenceTypeException e) {
                refErrorLanguage.set(e.getElementLanguage());
            }
            catch (ProcessCanceledException e) {
                refProcessCanceled.set(Boolean.TRUE);
            }
            catch (Throwable e) {
                anyException.set(Boolean.TRUE);
                LOG.error(e);
            }
        };

        if (!ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(findUsagesRunnable, RefactoringLocalize.progressText(), true, myProject)) {
            return;
        }

        if (!refErrorLanguage.isNull()) {
            Messages.showErrorDialog(
                myProject,
                RefactoringLocalize.unsupportedRefsFound(refErrorLanguage.get().getDisplayName()).get(),
                RefactoringLocalize.errorTitle().get()
            );
            return;
        }
        if (DumbService.isDumb(myProject)) {
            DumbService.getInstance(myProject).showDumbModeNotification("Refactoring is not available until indices are ready");
            return;
        }
        if (!refProcessCanceled.isNull()) {
            Messages.showErrorDialog(
                myProject,
                "Index corruption detected. Please retry the refactoring - indexes will be rebuilt automatically",
                RefactoringLocalize.errorTitle().get()
            );
            return;
        }

        if (!anyException.isNull()) {
            //do not proceed if find usages fails
            return;
        }
        assert !refUsages.isNull() : "Null usages from processor " + this;
        if (!preprocessUsages(refUsages)) {
            return;
        }
        UsageInfo[] usages = refUsages.get();
        assert usages != null;
        UsageViewDescriptor descriptor = createUsageViewDescriptor(usages);
        boolean isPreview = isPreviewUsages(usages) || !computeUnloadedModulesFromUseScope(descriptor).isEmpty();
        if (!isPreview) {
            isPreview = !ensureElementsWritable(usages, descriptor) || UsageViewUtil.hasReadOnlyUsages(usages);
        }
        if (isPreview) {
            for (UsageInfo usage : usages) {
                LOG.assertTrue(usage != null, getClass());
            }
            previewRefactoring(usages);
        }
        else {
            execute(usages);
        }
    }

    @TestOnly
    public static <T extends Throwable> void runWithDisabledPreview(ThrowableRunnable<T> runnable) throws T {
        PREVIEW_IN_TESTS = false;
        try {
            runnable.run();
        }
        finally {
            PREVIEW_IN_TESTS = true;
        }
    }

    @RequiredUIAccess
    protected void previewRefactoring(@Nonnull UsageInfo[] usages) {
        if (myProject.getApplication().isUnitTestMode()) {
            if (!PREVIEW_IN_TESTS) {
                throw new RuntimeException("Unexpected preview in tests: " + StringUtil.join(usages, UsageInfo::toString, ", "));
            }
            ensureElementsWritable(usages, createUsageViewDescriptor(usages));
            execute(usages);
            return;
        }
        UsageViewDescriptor viewDescriptor = createUsageViewDescriptor(usages);
        PsiElement[] elements = viewDescriptor.getElements();
        PsiElementUsageTarget[] targets = PsiElementUsageTargetFactory.getInstance().create(elements);
        Supplier<UsageSearcher> factory = () -> new UsageInfoSearcherAdapter() {
            @Override
            public void generate(@Nonnull Predicate<Usage> processor) {
                myProject.getApplication().runReadAction(() -> {
                    for (int i = 0; i < elements.length; i++) {
                        elements[i] = targets[i].getElement();
                    }
                    refreshElements(elements);
                });
                processUsages(processor, myProject);
            }

            @Nonnull
            @Override
            protected UsageInfo[] findUsages() {
                return BaseRefactoringProcessor.this.findUsages();
            }
        };

        showUsageView(viewDescriptor, factory, usages);
    }

    protected boolean skipNonCodeUsages() {
        return false;
    }

    @RequiredUIAccess
    private boolean ensureElementsWritable(@Nonnull UsageInfo[] usages, @Nonnull UsageViewDescriptor descriptor) {
        Set<PsiElement> elements = ContainerUtil.newIdentityTroveSet(); // protect against poorly implemented equality
        for (UsageInfo usage : usages) {
            assert usage != null : "Found null element in usages array";
            if (skipNonCodeUsages() && usage.isNonCodeUsage()) {
                continue;
            }
            PsiElement element = usage.getElement();
            if (element != null) {
                elements.add(element);
            }
        }
        elements.addAll(getElementsToWrite(descriptor));
        return ensureFilesWritable(myProject, elements);
    }

    @RequiredUIAccess
    private static boolean ensureFilesWritable(@Nonnull Project project, @Nonnull Collection<? extends PsiElement> elements) {
        PsiElement[] psiElements = PsiUtilCore.toPsiElementArray(elements);
        return CommonRefactoringUtil.checkReadOnlyStatus(project, psiElements);
    }

    @RequiredUIAccess
    protected void execute(@Nonnull UsageInfo[] usages) {
        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(LocalizeValue.ofNullable(getCommandName()))
            .undoConfirmationPolicy(getUndoConfirmationPolicy())
            .inGlobalUndoActionIf(isGlobalUndoAction())
            .run(() -> doRefactoring(new LinkedHashSet<>(Arrays.asList(usages))));
    }

    protected boolean isGlobalUndoAction() {
        return !DataManager.getInstance().getDataContext().hasData(Editor.KEY);
    }

    @Nonnull
    protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
        return UndoConfirmationPolicy.DEFAULT;
    }

    @Nonnull
    private static UsageViewPresentation createPresentation(@Nonnull UsageViewDescriptor descriptor, @Nonnull Usage[] usages) {
        UsageViewPresentation presentation = new UsageViewPresentation();
        presentation.setTabText(RefactoringLocalize.usageviewTabtext());
        presentation.setTargetsNodeText(descriptor.getProcessedElementsHeader());
        presentation.setShowReadOnlyStatusAsRed(true);
        presentation.setShowCancelButton(true);
        presentation.setUsagesString(RefactoringLocalize.usageviewUsagestext());
        int codeUsageCount = 0;
        int nonCodeUsageCount = 0;
        int dynamicUsagesCount = 0;
        Set<PsiFile> codeFiles = new HashSet<>();
        Set<PsiFile> nonCodeFiles = new HashSet<>();
        Set<PsiFile> dynamicUsagesCodeFiles = new HashSet<>();

        for (Usage usage : usages) {
            if (usage instanceof PsiElementUsage elementUsage) {
                PsiElement element = elementUsage.getElement();
                if (element == null) {
                    continue;
                }
                PsiFile containingFile = element.getContainingFile();
                if (elementUsage instanceof UsageInfo2UsageAdapter usageAdapter && usageAdapter.getUsageInfo().isDynamicUsage()) {
                    dynamicUsagesCount++;
                    dynamicUsagesCodeFiles.add(containingFile);
                }
                else if (elementUsage.isNonCodeUsage()) {
                    nonCodeUsageCount++;
                    nonCodeFiles.add(containingFile);
                }
                else {
                    codeUsageCount++;
                    codeFiles.add(containingFile);
                }
            }
        }
        codeFiles.remove(null);
        nonCodeFiles.remove(null);
        dynamicUsagesCodeFiles.remove(null);

        String codeReferencesText = descriptor.getCodeReferencesText(codeUsageCount, codeFiles.size());
        presentation.setCodeUsagesString(codeReferencesText);
        String commentReferencesText = descriptor.getCommentReferencesText(nonCodeUsageCount, nonCodeFiles.size());
        if (commentReferencesText != null) {
            presentation.setNonCodeUsagesString(commentReferencesText);
        }
        presentation.setDynamicUsagesString("Dynamic " + StringUtil.decapitalize(descriptor.getCodeReferencesText(
            dynamicUsagesCount,
            dynamicUsagesCodeFiles.size()
        )));
        String generatedCodeString;
        if (codeReferencesText.contains("in code")) {
            generatedCodeString = StringUtil.replace(codeReferencesText, "in code", "in generated code");
        }
        else {
            generatedCodeString = codeReferencesText + " in generated code";
        }
        presentation.setUsagesInGeneratedCodeString(generatedCodeString);
        return presentation;
    }

    /**
     * Processes conflicts (possibly shows UI). In case we're running in unit test mode this method will
     * throw {@link BaseRefactoringProcessor.ConflictsInTestsException} that can be handled inside a test.
     * Thrown exception would contain conflicts' messages.
     *
     * @param project   project
     * @param conflicts map with conflict messages and locations
     * @return true if refactoring could proceed or false if refactoring should be cancelled
     */
    @RequiredUIAccess
    public static boolean processConflicts(@Nonnull Project project, @Nonnull MultiMap<PsiElement, String> conflicts) {
        if (conflicts.isEmpty()) {
            return true;
        }

        if (project.getApplication().isUnitTestMode()) {
            if (BaseRefactoringProcessor.ConflictsInTestsException.isTestIgnore()) {
                return true;
            }
            throw new BaseRefactoringProcessor.ConflictsInTestsException(conflicts.values());
        }

        ConflictsDialog conflictsDialog = new ConflictsDialog(project, conflicts);
        return conflictsDialog.showAndGet();
    }

    private void showUsageView(
        @Nonnull UsageViewDescriptor viewDescriptor,
        @Nonnull Supplier<UsageSearcher> factory,
        @Nonnull UsageInfo[] usageInfos
    ) {
        UsageViewManager viewManager = UsageViewManager.getInstance(myProject);

        PsiElement[] initialElements = viewDescriptor.getElements();
        UsageTarget[] targets = PsiElementUsageTargetFactory.getInstance().create(initialElements);
        SimpleReference<Usage[]> convertUsagesRef = new SimpleReference<>();
        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
            () -> myProject.getApplication().runReadAction(() -> convertUsagesRef.set(UsageInfo2UsageAdapter.convert(usageInfos))),
            "Preprocess Usages",
            true,
            myProject
        )) {
            return;
        }

        if (convertUsagesRef.isNull()) {
            return;
        }

        Usage[] usages = convertUsagesRef.get();

        UsageViewPresentation presentation = createPresentation(viewDescriptor, usages);
        if (myUsageView == null) {
            myUsageView = viewManager.showUsages(targets, usages, presentation, factory);
            customizeUsagesView(viewDescriptor, myUsageView);
        }
        else {
            myUsageView.removeUsagesBulk(myUsageView.getUsages());
            ((UsageViewEx)myUsageView).appendUsagesInBulk(Arrays.asList(usages));
        }
        Set<UnloadedModuleDescription> unloadedModules = computeUnloadedModulesFromUseScope(viewDescriptor);
        if (!unloadedModules.isEmpty()) {
            myUsageView.appendUsage(new UnknownUsagesInUnloadedModules(unloadedModules));
        }
    }

    protected void customizeUsagesView(@Nonnull UsageViewDescriptor viewDescriptor, @Nonnull UsageView usageView) {
        @RequiredUIAccess
        Runnable refactoringRunnable = () -> {
            Set<UsageInfo> usagesToRefactor = UsageViewUtil.getNotExcludedUsageInfos(usageView);
            UsageInfo[] infos = usagesToRefactor.toArray(UsageInfo.EMPTY_ARRAY);
            if (ensureElementsWritable(infos, viewDescriptor)) {
                execute(infos);
            }
        };

        LocalizeValue canNotMakeString = RefactoringLocalize.usageviewNeedRerun();

        addDoRefactoringAction(usageView, refactoringRunnable, canNotMakeString.get());
        usageView.setRerunAction(new AbstractAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                doRun();
            }
        });
    }

    private void addDoRefactoringAction(
        @Nonnull UsageView usageView,
        @Nonnull Runnable refactoringRunnable,
        @Nonnull String canNotMakeString
    ) {
        usageView.addPerformOperationAction(
            refactoringRunnable,
            getCommandName(),
            canNotMakeString,
            RefactoringLocalize.usageviewDoaction(),
            false
        );
    }

    @RequiredUIAccess
    private void doRefactoring(@Nonnull Collection<UsageInfo> usageInfoSet) {
        for (Iterator<UsageInfo> iterator = usageInfoSet.iterator(); iterator.hasNext(); ) {
            UsageInfo usageInfo = iterator.next();
            PsiElement element = usageInfo.getElement();
            if (element == null || !isToBeChanged(usageInfo)) {
                iterator.remove();
            }
        }

        String commandName = getCommandName();
        LocalHistoryAction action = LocalHistory.getInstance().startAction(commandName);

        UsageInfo[] writableUsageInfos = usageInfoSet.toArray(UsageInfo.EMPTY_ARRAY);
        try {
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
            RefactoringListenerManagerEx listenerManager = (RefactoringListenerManagerEx)RefactoringListenerManager.getInstance(myProject);
            myTransaction = listenerManager.startTransaction();
            Map<RefactoringHelper, Object> preparedData = new LinkedHashMap<>();
            Runnable prepareHelpersRunnable = () -> {
                for (RefactoringHelper helper : RefactoringHelper.EP_NAME.getExtensionList()) {
                    Object operation = AccessRule.read(() -> helper.prepareOperation(writableUsageInfos));
                    preparedData.put(helper, operation);
                }
            };

            ProgressManager.getInstance().runProcessWithProgressSynchronously(prepareHelpersRunnable, "Prepare ...", false, myProject);

            Runnable performRefactoringRunnable = () -> {
                String refactoringId = getRefactoringId();
                if (refactoringId != null) {
                    RefactoringEventData data = getBeforeData();
                    if (data != null) {
                        data.addUsages(usageInfoSet);
                    }
                    myProject.getMessageBus().syncPublisher(RefactoringEventListener.class).refactoringStarted(refactoringId, data);
                }

                try {
                    if (refactoringId != null) {
                        UndoableAction action1 = new UndoRefactoringAction(myProject, refactoringId);
                        ProjectUndoManager.getInstance(myProject).undoableActionPerformed(action1);
                    }

                    performRefactoring(writableUsageInfos);
                }
                finally {
                    if (refactoringId != null) {
                        myProject.getMessageBus()
                            .syncPublisher(RefactoringEventListener.class)
                            .refactoringDone(refactoringId, getAfterData(writableUsageInfos));
                    }
                }
            };
            ApplicationEx app = (ApplicationEx)Application.get();
            if (Registry.is("run.refactorings.under.progress")) {
                app.runWriteActionWithNonCancellableProgressInDispatchThread(
                    commandName,
                    myProject,
                    null,
                    indicator -> performRefactoringRunnable.run()
                );
            }
            else {
                app.runWriteAction(performRefactoringRunnable);
            }

            DumbService.getInstance(myProject).completeJustSubmittedTasks();

            for (Map.Entry<RefactoringHelper, Object> e : preparedData.entrySet()) {
                //noinspection unchecked
                e.getKey().performOperation(myProject, e.getValue());
            }
            myTransaction.commit();
            if (Registry.is("run.refactorings.under.progress")) {
                app.runWriteActionWithNonCancellableProgressInDispatchThread(
                    commandName,
                    myProject,
                    null,
                    indicator -> performPsiSpoilingRefactoring()
                );
            }
            else {
                app.runWriteAction(this::performPsiSpoilingRefactoring);
            }
        }
        finally {
            action.finish();
        }
    }

    @RequiredReadAction
    protected boolean isToBeChanged(@Nonnull UsageInfo usageInfo) {
        return usageInfo.isWritable();
    }

    /**
     * Refactorings that spoil PSI (write something directly to documents etc.) should
     * do that in this method.<br>
     * This method is called immediately after
     * <code>{@link #performRefactoring(UsageInfo[])}</code>.
     */
    protected void performPsiSpoilingRefactoring() {
    }

    @RequiredUIAccess
    protected void prepareSuccessful() {
        if (myPrepareSuccessfulSwingThreadCallback != null) {
            myProject.getApplication().invokeAndWait(myPrepareSuccessfulSwingThreadCallback);
        }
    }

    @Override
    @RequiredUIAccess
    public final void run() {
        @RequiredUIAccess
        Runnable runnable = this::doRun;
        if (shouldDisableAccessChecks()) {
            runnable = () -> RefactoringInternalHelper.getInstance().disableWriteChecksDuring(this::doRun);
        }
        Application application = myProject.getApplication();
        if (application.isUnitTestMode()) {
            UIAccess.assertIsUIThread();
            runnable.run();
            return;
        }
        if (application.isWriteAccessAllowed()) {
            LOG.error(
                "Refactorings should not be started inside write action\n" +
                    " because they start progress inside and any read action from the progress task would cause the deadlock",
                new Exception()
            );
            DumbService.getInstance(myProject).smartInvokeLater(runnable);
        }
        else {
            runnable.run();
        }
    }

    protected boolean shouldDisableAccessChecks() {
        return false;
    }

    public static class ConflictsInTestsException extends RuntimeException {
        private final Collection<? extends String> messages;

        private static boolean myTestIgnore;

        public ConflictsInTestsException(@Nonnull Collection<? extends String> messages) {
            this.messages = messages;
        }

        public static boolean isTestIgnore() {
            return myTestIgnore;
        }

        @TestOnly
        public static <T extends Throwable> void withIgnoredConflicts(@Nonnull ThrowableRunnable<T> r) throws T {
            try {
                myTestIgnore = true;
                r.run();
            }
            finally {
                myTestIgnore = false;
            }
        }

        @Nonnull
        public Collection<String> getMessages() {
            List<String> result = new ArrayList<>(messages);
            for (int i = 0; i < messages.size(); i++) {
                result.set(i, result.get(i).replaceAll("<[^>]+>", ""));
            }
            return result;
        }

        @Override
        public String getMessage() {
            List<String> result = new ArrayList<>(messages);
            Collections.sort(result);
            return StringUtil.join(result, "\n");
        }
    }

    /**
     * @deprecated use {@link #showConflicts(MultiMap, UsageInfo[])}
     */
    @Deprecated
    @RequiredUIAccess
    protected boolean showConflicts(@Nonnull MultiMap<PsiElement, String> conflicts) {
        return showConflicts(conflicts, null);
    }

    @RequiredUIAccess
    protected boolean showConflicts(@Nonnull MultiMap<PsiElement, String> conflicts, @Nullable UsageInfo[] usages) {
        if (!conflicts.isEmpty() && myProject.getApplication().isUnitTestMode()) {
            if (!ConflictsInTestsException.isTestIgnore()) {
                throw new ConflictsInTestsException(conflicts.values());
            }
            return true;
        }

        if (myPrepareSuccessfulSwingThreadCallback != null && !conflicts.isEmpty()) {
            String refactoringId = getRefactoringId();
            if (refactoringId != null) {
                RefactoringEventData conflictUsages = new RefactoringEventData();
                conflictUsages.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts.values());
                myProject.getMessageBus().syncPublisher(RefactoringEventListener.class).conflictsDetected(refactoringId, conflictUsages);
            }
            ConflictsDialog conflictsDialog = prepareConflictsDialog(conflicts, usages);
            if (!conflictsDialog.showAndGet()) {
                if (conflictsDialog.isShowConflicts()) {
                    prepareSuccessful();
                }
                return false;
            }
        }

        prepareSuccessful();
        return true;
    }

    @Nonnull
    protected ConflictsDialog prepareConflictsDialog(@Nonnull MultiMap<PsiElement, String> conflicts, @Nullable UsageInfo[] usages) {
        ConflictsDialog conflictsDialog = createConflictsDialog(conflicts, usages);
        conflictsDialog.setCommandName(getCommandName());
        return conflictsDialog;
    }

    @Nullable
    protected RefactoringEventData getBeforeData() {
        return null;
    }

    @Nullable
    protected RefactoringEventData getAfterData(@Nonnull UsageInfo[] usages) {
        return null;
    }

    @Nullable
    protected String getRefactoringId() {
        return null;
    }

    @Nonnull
    protected ConflictsDialog createConflictsDialog(@Nonnull MultiMap<PsiElement, String> conflicts, @Nullable UsageInfo[] usages) {
        return new ConflictsDialog(myProject, conflicts, usages == null ? null : () -> execute(usages), false, true);
    }

    @Nonnull
    protected Collection<? extends PsiElement> getElementsToWrite(@Nonnull UsageViewDescriptor descriptor) {
        return Arrays.asList(descriptor.getElements());
    }

    public static class UnknownReferenceTypeException extends RuntimeException {
        private final Language myElementLanguage;

        public UnknownReferenceTypeException(@Nonnull Language elementLanguage) {
            myElementLanguage = elementLanguage;
        }

        @Nonnull
        Language getElementLanguage() {
            return myElementLanguage;
        }
    }

    private static class UndoRefactoringAction extends BasicUndoableAction {
        private final Project myProject;
        private final String myRefactoringId;

        UndoRefactoringAction(@Nonnull Project project, @Nonnull String refactoringId) {
            myProject = project;
            myRefactoringId = refactoringId;
        }

        @Override
        public void undo() {
            myProject.getMessageBus().syncPublisher(RefactoringEventListener.class).undoRefactoring(myRefactoringId);
        }

        @Override
        public void redo() {
        }
    }
}
