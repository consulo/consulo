// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.completion;

import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.internal.DocumentEx;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.actions.BaseCodeCompletionAction;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.ide.impl.idea.openapi.application.impl.ApplicationInfoImpl;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.language.Language;
import consulo.language.codeStyle.PostprocessReformattingAspect;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.action.SmartEnterProcessor;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.impl.internal.completion.CompletionAssertions;
import consulo.language.editor.impl.internal.completion.CompletionAssertions.WatchingInsertionContext;
import consulo.language.editor.impl.internal.completion.OffsetsInFile;
import consulo.language.editor.impl.internal.completion.StatisticsUpdate;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.impl.internal.psi.stub.StubTextInconsistencyException;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Supplier;

@SuppressWarnings("deprecation")
public class CodeCompletionHandlerBase {
    private static final Logger LOG = Logger.getInstance(CodeCompletionHandlerBase.class);
    private static final Key<Boolean> CARET_PROCESSED = Key.create("CodeCompletionHandlerBase.caretProcessed");

    /**
     * If this key is set for a lookup element, the framework will only call handleInsert() on the lookup element when it is selected,
     * and will not perform any additional processing such as multi-caret handling or insertion of completion character.
     */
    public static final Key<Boolean> DIRECT_INSERTION = Key.create("CodeCompletionHandlerBase.directInsertion");

    @Nonnull
    final CompletionType completionType;
    final boolean invokedExplicitly;
    final boolean synchronous;
    final boolean autopopup;
    private static int ourAutoInsertItemTimeout = 2000;

    public static CodeCompletionHandlerBase createHandler(@Nonnull CompletionType completionType) {
        return createHandler(completionType, true, false, true);
    }

    public static CodeCompletionHandlerBase createHandler(
        @Nonnull CompletionType completionType,
        boolean invokedExplicitly,
        boolean autopopup,
        boolean synchronous
    ) {
        AnAction codeCompletionAction = ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION);
        //if (codeCompletionAction instanceof OverridingAction) {
        //  codeCompletionAction = ((ActionManagerImpl)ActionManager.getInstance()).getBaseAction((OverridingAction)codeCompletionAction);
        //}
        assert (codeCompletionAction instanceof BaseCodeCompletionAction);
        BaseCodeCompletionAction baseCodeCompletionAction = (BaseCodeCompletionAction)codeCompletionAction;
        return baseCodeCompletionAction.createHandler(completionType, invokedExplicitly, autopopup, synchronous);
    }

    public CodeCompletionHandlerBase(@Nonnull CompletionType completionType) {
        this(completionType, true, false, true);
    }

    public CodeCompletionHandlerBase(
        @Nonnull CompletionType completionType,
        boolean invokedExplicitly,
        boolean autopopup,
        boolean synchronous
    ) {
        this.completionType = completionType;
        this.invokedExplicitly = invokedExplicitly;
        this.autopopup = autopopup;
        this.synchronous = synchronous;

        if (invokedExplicitly) {
            assert synchronous;
        }
        if (autopopup) {
            assert !invokedExplicitly;
        }
    }

    @RequiredUIAccess
    public final void invokeCompletion(Project project, Editor editor) {
        invokeCompletion(project, editor, 1);
    }

    @RequiredUIAccess
    public final void invokeCompletion(@Nonnull Project project, @Nonnull Editor editor, int time) {
        invokeCompletion(project, editor, time, false);
    }

    @RequiredUIAccess
    public final void invokeCompletion(@Nonnull Project project, @Nonnull Editor editor, int time, boolean hasModifiers) {
        clearCaretMarkers(editor);
        invokeCompletion(project, editor, time, hasModifiers, editor.getCaretModel().getPrimaryCaret());
    }

    @RequiredUIAccess
    private void invokeCompletion(@Nonnull Project project, @Nonnull Editor editor, int time, boolean hasModifiers, @Nonnull Caret caret) {
        markCaretAsProcessed(caret);

        if (invokedExplicitly) {
            StatisticsUpdate.applyLastCompletionStatisticsUpdate();
        }

        checkNoWriteAccess();

        CompletionAssertions.checkEditorValid(editor);

        int offset = editor.getCaretModel().getOffset();
        if (editor.isViewer() || editor.getDocument().getRangeGuard(offset, offset) != null) {
            editor.getDocument().fireReadOnlyModificationAttempt();
            EditorModificationUtil.checkModificationAllowed(editor);
            return;
        }

        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
            return;
        }

        CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();
        boolean repeated = phase.indicator != null && phase.indicator.isRepeatedInvocation(completionType, editor);

        int newTime = phase.newCompletionStarted(time, repeated);
        if (invokedExplicitly) {
            time = newTime;
        }
        int invocationCount = time;
        if (CompletionServiceImpl.isPhase(CompletionPhase.InsertedSingleItem.class)) {
            CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
        }
        CompletionServiceImpl.assertPhase(CompletionPhase.NoCompletion.getClass(), CompletionPhase.CommittingDocuments.class);

        if (invocationCount > 1 && completionType == CompletionType.BASIC) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.SECOND_BASIC_COMPLETION);
        }

        long startingTime = System.currentTimeMillis();

        @RequiredUIAccess  Runnable initCmd = () -> {
            CompletionInitializationContextImpl context = withTimeout(
                calcSyncTimeOut(startingTime),
                () -> CompletionInitializationUtil.createCompletionInitializationContext(
                    project,
                    editor,
                    caret,
                    invocationCount,
                    completionType
                )
            );

            boolean hasValidContext = context != null;
            if (!hasValidContext) {
                PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(caret, project);
                context = new CompletionInitializationContextImpl(editor, caret, psiFile, completionType, invocationCount);
            }

            doComplete(context, hasModifiers, hasValidContext, startingTime);
        };
        try {
            if (autopopup) {
                CommandProcessor.getInstance().runUndoTransparentAction(initCmd);
            }
            else {
                CommandProcessor.getInstance().newCommand()
                    .project(project)
                    .document(editor.getDocument())
                    .run(initCmd);
            }
        }
        catch (IndexNotReadyException e) {
            if (invokedExplicitly) {
                DumbService.getInstance(project)
                    .showDumbModeNotification("Code completion is not available here while indices are being built");
            }
        }
    }

    private static void checkNoWriteAccess() {
        Application application = Application.get();
        if (!application.isUnitTestMode() && application.isWriteAccessAllowed()) {
            throw new AssertionError("Completion should not be invoked inside write action");
        }
    }

    @Nonnull
    private LookupEx obtainLookup(Editor editor, Project project) {
        CompletionAssertions.checkEditorValid(editor);
        LookupEx existing = LookupManager.getActiveLookup(editor);
        if (existing != null && existing.isCompletion()) {
            existing.markReused();
            if (!autopopup) {
                existing.setFocusDegree(LookupFocusDegree.FOCUSED);
            }
            return existing;
        }

        LookupEx lookup = (LookupEx)LookupManager.getInstance(project)
            .createLookup(editor, LookupElement.EMPTY_ARRAY, "", new LookupArranger.DefaultArranger());
        if (editor.isOneLineMode()) {
            lookup.setCancelOnClickOutside(true);
            lookup.setCancelOnOtherWindowOpen(true);
        }
        lookup.setFocusDegree(autopopup ? LookupFocusDegree.UNFOCUSED : LookupFocusDegree.FOCUSED);
        return lookup;
    }

    @RequiredUIAccess
    private void doComplete(
        CompletionInitializationContextImpl initContext,
        boolean hasModifiers,
        boolean isValidContext,
        long startingTime
    ) {
        Editor editor = initContext.getEditor();
        CompletionAssertions.checkEditorValid(editor);

        LookupEx lookup = obtainLookup(editor, initContext.getProject());

        CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();
        if (phase instanceof CompletionPhase.CommittingDocuments) {
            if (phase.indicator != null) {
                phase.indicator.closeAndFinish(false);
            }
            ((CompletionPhase.CommittingDocuments)phase).replaced = true;
        }
        else {
            CompletionServiceImpl.assertPhase(CompletionPhase.NoCompletion.getClass());
        }

        CompletionProgressIndicator indicator = new CompletionProgressIndicator(
            editor,
            initContext.getCaret(),
            initContext.getInvocationCount(),
            this,
            initContext.getOffsetMap(),
            initContext.getHostOffsets(),
            hasModifiers,
            lookup
        );

        OffsetsInFile hostCopyOffsets =
            WriteAction.compute(() -> CompletionInitializationUtil.insertDummyIdentifier(initContext, indicator));

        if (synchronous && isValidContext && commitDocumentsWithTimeout(initContext, startingTime)) {
            trySynchronousCompletion(initContext, hasModifiers, startingTime, indicator, hostCopyOffsets);
        }
        else {
            scheduleContributorsAfterAsyncCommit(initContext, indicator, hostCopyOffsets, hasModifiers);
        }
    }

    private void scheduleContributorsAfterAsyncCommit(
        CompletionInitializationContextImpl initContext,
        CompletionProgressIndicator indicator,
        OffsetsInFile hostCopyOffsets,
        boolean hasModifiers
    ) {
        CompletionPhase phase;
        if (synchronous) {
            phase = new CompletionPhase.BgCalculation(indicator);
            indicator.makeSureLookupIsShown(0);
        }
        else {
            phase = new CompletionPhase.CommittingDocuments(indicator, EditorWindow.getTopLevelEditor(indicator.getEditor()));
        }
        CompletionServiceImpl.setCompletionPhase(phase);

        AppUIExecutor.onUiThread().withDocumentsCommitted(initContext.getProject()).expireWith(phase).execute(() -> {
            if (phase instanceof CompletionPhase.CommittingDocuments committingDocuments) {
                committingDocuments.replaced = true;
            }
            CompletionServiceImpl.setCompletionPhase(new CompletionPhase.BgCalculation(indicator));
            startContributorThread(initContext, indicator, hostCopyOffsets, hasModifiers);
        });
    }

    private boolean commitDocumentsWithTimeout(CompletionInitializationContextImpl initContext, long startingTime) {
        return withTimeout(calcSyncTimeOut(startingTime), () -> {
            PsiDocumentManager.getInstance(initContext.getProject()).commitAllDocuments();
            return true;
        }) != null;
    }

    @RequiredUIAccess
    private void trySynchronousCompletion(
        CompletionInitializationContextImpl initContext,
        boolean hasModifiers,
        long startingTime,
        CompletionProgressIndicator indicator,
        OffsetsInFile hostCopyOffsets
    ) {
        CompletionServiceImpl.setCompletionPhase(new CompletionPhase.Synchronous(indicator));

        Future<?> future = startContributorThread(initContext, indicator, hostCopyOffsets, hasModifiers);
        if (future == null) {
            return;
        }

        int timeout = calcSyncTimeOut(startingTime);
        indicator.makeSureLookupIsShown(timeout);
        if (indicator.blockingWaitForFinish(timeout)) {
            checkForExceptions(future);
            try {
                indicator.getLookup().refreshUi(true, false);
                completionFinished(indicator, hasModifiers);
            }
            catch (Throwable e) {
                indicator.closeAndFinish(true);
                CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
                LOG.error(e);
            }
            return;
        }

        CompletionServiceImpl.setCompletionPhase(new CompletionPhase.BgCalculation(indicator));
        indicator.showLookup();
    }

    @Nullable
    @RequiredUIAccess
    private Future<?> startContributorThread(
        CompletionInitializationContextImpl initContext,
        CompletionProgressIndicator indicator,
        OffsetsInFile hostCopyOffsets,
        boolean hasModifiers
    ) {
        if (!hostCopyOffsets.getFile().isValid()) {
            completionFinished(indicator, hasModifiers);
            return null;
        }

        return indicator.getCompletionThreading().startThread(indicator, () -> AsyncCompletion.tryReadOrCancel(indicator, () -> {
            OffsetsInFile finalOffsets = CompletionInitializationUtil.toInjectedIfAny(initContext.getFile(), hostCopyOffsets);
            indicator.registerChildDisposable(finalOffsets::getOffsets);

            CompletionParameters parameters = CompletionInitializationUtil.createCompletionParameters(initContext, indicator, finalOffsets);
            parameters.setIsTestingMode(isTestingMode());
            indicator.setParameters(parameters);

            indicator.runContributors(initContext);
        }));
    }

    private static void checkForExceptions(Future<?> future) {
        if (Application.get().isUnitTestMode()) {
            try {
                future.get();
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    private static void checkNotSync(CompletionProgressIndicator indicator, List<LookupElement> allItems) {
        if (CompletionServiceImpl.isPhase(CompletionPhase.Synchronous.class)) {
            LOG.error(
                "sync phase survived: " + allItems +
                    "; indicator=" + CompletionServiceImpl.getCompletionPhase().indicator +
                    "; myIndicator=" + indicator
            );
            CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
        }
    }

    private AutoCompletionDecision shouldAutoComplete(
        @Nonnull CompletionProgressIndicator indicator,
        @Nonnull List<LookupElement> items,
        @Nonnull CompletionParameters parameters
    ) {
        if (!invokedExplicitly) {
            return AutoCompletionDecision.SHOW_LOOKUP;
        }
        LookupElement item = items.get(0);
        if (items.size() == 1) {
            AutoCompletionPolicy policy = getAutocompletionPolicy(item);
            if (policy == AutoCompletionPolicy.NEVER_AUTOCOMPLETE) {
                return AutoCompletionDecision.SHOW_LOOKUP;
            }
            if (policy == AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE) {
                return AutoCompletionDecision.insertItem(item);
            }
            if (!indicator.getLookup().itemMatcher(item).isStartMatch(item)) {
                return AutoCompletionDecision.SHOW_LOOKUP;
            }
        }
        if (!isAutocompleteOnInvocation(parameters.getCompletionType())) {
            return AutoCompletionDecision.SHOW_LOOKUP;
        }
        if (isInsideIdentifier(indicator.getOffsetMap())) {
            return AutoCompletionDecision.SHOW_LOOKUP;
        }
        if (items.size() == 1 && getAutocompletionPolicy(item) == AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE) {
            return AutoCompletionDecision.insertItem(item);
        }

        AutoCompletionContext context = new AutoCompletionContext(
            parameters,
            items.toArray(LookupElement.EMPTY_ARRAY),
            indicator.getOffsetMap(),
            indicator.getLookup()
        );
        for (CompletionContributor contributor : CompletionContributor.forParameters(parameters)) {
            AutoCompletionDecision decision = contributor.handleAutoCompletionPossibility(context);
            if (decision != null) {
                return decision;
            }
        }

        return AutoCompletionDecision.SHOW_LOOKUP;
    }

    @Nullable
    private static AutoCompletionPolicy getAutocompletionPolicy(LookupElement element) {
        return element.getAutoCompletionPolicy();
    }

    private static boolean isInsideIdentifier(OffsetMap offsetMap) {
        return offsetMap.getOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET) !=
            offsetMap.getOffset(CompletionInitializationContext.SELECTION_END_OFFSET);
    }

    @RequiredUIAccess
    protected void completionFinished(CompletionProgressIndicator indicator, boolean hasModifiers) {
        List<LookupElement> items = indicator.getLookup().getItems();
        if (items.isEmpty()) {
            LookupManager.hideActiveLookup(indicator.getProject());

            Caret nextCaret = getNextCaretToProcess(indicator.getEditor());
            if (nextCaret != null) {
                invokeCompletion(indicator.getProject(), indicator.getEditor(), indicator.getInvocationCount(), hasModifiers, nextCaret);
            }
            else {
                indicator.handleEmptyLookup(true);
                checkNotSync(indicator, items);
            }
            return;
        }

        LOG.assertTrue(!indicator.isRunning(), "running");
        LOG.assertTrue(!indicator.isCanceled(), "canceled");

        try {
            CompletionParameters parameters = indicator.getParameters();
            AutoCompletionDecision decision =
                parameters == null ? AutoCompletionDecision.CLOSE_LOOKUP : shouldAutoComplete(indicator, items, parameters);
            if (decision == AutoCompletionDecision.SHOW_LOOKUP) {
                indicator.getLookup().setCalculating(false);
                indicator.showLookup();
                CompletionServiceImpl.setCompletionPhase(new CompletionPhase.ItemsCalculated(indicator));
            }
            else if (decision instanceof AutoCompletionDecision.InsertItem insertItem) {
                Runnable restorePrefix = rememberDocumentState(indicator.getEditor());

                LookupElement item = insertItem.getElement();
                CommandProcessor.getInstance().newCommand()
                    .project(indicator.getProject())
                    .name(CodeInsightLocalize.completionAutomaticCommandName())
                    .run(() -> {
                        indicator.setMergeCommand();
                        indicator.getLookup().finishLookup(Lookup.AUTO_INSERT_SELECT_CHAR, item);
                    });

                // the insert handler may have started a live template with completion
                if (CompletionService.getCompletionService().getCurrentCompletion() == null
                    // ...or scheduled another autopopup
                    && !CompletionServiceImpl.isPhase(CompletionPhase.CommittingDocuments.class)) {
                    CompletionServiceImpl.setCompletionPhase(
                        hasModifiers
                            ? new CompletionPhase.InsertedSingleItem(indicator, restorePrefix)
                            : CompletionPhase.NoCompletion
                    );
                }
            }
            else if (decision == AutoCompletionDecision.CLOSE_LOOKUP) {
                LookupManager.hideActiveLookup(indicator.getProject());
            }
        }
        catch (Throwable e) {
            CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
            LOG.error(e);
        }
        finally {
            checkNotSync(indicator, items);
        }
    }

    protected void lookupItemSelected(
        CompletionProgressIndicator indicator,
        @Nonnull LookupElement item,
        char completionChar,
        List<LookupElement> items
    ) {
        if (indicator.isAutopopupCompletion()) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_BASIC);
        }

        WatchingInsertionContext context = null;
        try {
            StatisticsUpdate update = StatisticsUpdate.collectStatisticChanges(item);
            if (item.getUserData(DIRECT_INSERTION) != null) {
                context = callHandleInsert(indicator, item, completionChar);
            }
            else {
                context = insertItemHonorBlockSelection(indicator, item, completionChar, update);
            }
            update.trackStatistics(context);
        }
        finally {
            afterItemInsertion(indicator, context == null ? null : context.getLaterRunnable());
        }
    }

    private static WatchingInsertionContext insertItemHonorBlockSelection(
        CompletionProcessEx indicator,
        LookupElement item,
        char completionChar,
        StatisticsUpdate update
    ) {
        Editor editor = indicator.getEditor();

        int caretOffset = indicator.getCaret().getOffset();
        int idEndOffset = calcIdEndOffset(indicator);
        int idEndOffsetDelta = idEndOffset - caretOffset;

        WatchingInsertionContext context;
        if (editor.getCaretModel().supportsMultipleCarets()) {
            InjectedEditorManager injectedEditorManager = InjectedEditorManager.getInstance(indicator.getProject());
            SimpleReference<WatchingInsertionContext> lastContext = SimpleReference.create();
            Editor hostEditor = EditorWindow.getTopLevelEditor(editor);
            boolean wasInjected = hostEditor != editor;
            OffsetsInFile topLevelOffsets = indicator.getHostOffsets();
            hostEditor.getCaretModel().runForEachCaret(caret -> {
                OffsetsInFile targetOffsets = findInjectedOffsetsIfAny(caret, wasInjected, topLevelOffsets, hostEditor);
                PsiFile targetFile = targetOffsets.getFile();
                Editor targetEditor = injectedEditorManager.getInjectedEditorForInjectedFile(hostEditor, targetFile);
                int targetCaretOffset = targetEditor.getCaretModel().getOffset();
                int idEnd = targetCaretOffset + idEndOffsetDelta;
                if (idEnd > targetEditor.getDocument().getTextLength()) {
                    idEnd = targetCaretOffset; // no replacement by Tab when offsets gone wrong for some reason
                }
                WatchingInsertionContext currentContext = insertItem(
                    indicator.getLookup(),
                    item,
                    completionChar,
                    update,
                    targetEditor,
                    targetFile,
                    targetCaretOffset,
                    idEnd,
                    targetOffsets.getOffsets()
                );
                lastContext.set(currentContext);
            });
            context = lastContext.get();
        }
        else {
            PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, indicator.getProject());
            context = insertItem(
                indicator.getLookup(),
                item,
                completionChar,
                update,
                editor,
                psiFile,
                caretOffset,
                idEndOffset,
                indicator.getOffsetMap()
            );
        }
        if (context.shouldAddCompletionChar()) {
            WriteAction.run(() -> addCompletionChar(context, item));
        }
        checkPsiTextConsistency(indicator);

        return context;
    }

    private static OffsetsInFile findInjectedOffsetsIfAny(
        @Nonnull Caret caret,
        boolean wasInjected,
        @Nonnull OffsetsInFile topLevelOffsets,
        @Nonnull Editor hostEditor
    ) {
        if (!wasInjected) {
            return topLevelOffsets;
        }

        PsiDocumentManager.getInstance(topLevelOffsets.getFile().getProject()).commitDocument(hostEditor.getDocument());
        return topLevelOffsets.toInjectedIfAny(caret.getOffset());
    }

    private static int calcIdEndOffset(CompletionProcessEx indicator) {
        return indicator.getOffsetMap().containsOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET)
            ? indicator.getOffsetMap().getOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET)
            : CompletionInitializationContext.calcDefaultIdentifierEnd(indicator.getEditor(), indicator.getCaret().getOffset());
    }

    private static void checkPsiTextConsistency(CompletionProcessEx indicator) {
        PsiFile psiFile =
            PsiUtilBase.getPsiFileInEditor(EditorWindow.getTopLevelEditor(indicator.getEditor()), indicator.getProject());
        if (psiFile != null) {
            if (Registry.is("ide.check.stub.text.consistency")
                || Application.get().isUnitTestMode() && !ApplicationInfoImpl.isInPerformanceTest()) {
                StubTextInconsistencyException.checkStubTextConsistency(psiFile);
                if (PsiDocumentManager.getInstance(psiFile.getProject()).hasUncommitedDocuments()) {
                    PsiDocumentManager.getInstance(psiFile.getProject()).commitAllDocuments();
                    StubTextInconsistencyException.checkStubTextConsistency(psiFile);
                }
            }
        }
    }

    public void afterItemInsertion(CompletionProgressIndicator indicator, Runnable laterRunnable) {
        if (laterRunnable != null) {
            ActionTracker tracker = new ActionTracker(indicator.getEditor(), indicator);
            Runnable wrapper = () -> {
                if (!indicator.getProject().isDisposed() && !tracker.hasAnythingHappened()) {
                    laterRunnable.run();
                }
                indicator.disposeIndicator();
            };
            Application.get().invokeLater(wrapper);
        }
        else {
            indicator.disposeIndicator();
        }
    }

    @RequiredUIAccess
    private static WatchingInsertionContext insertItem(
        @Nullable Lookup lookup,
        LookupElement item,
        char completionChar,
        StatisticsUpdate update,
        Editor editor,
        PsiFile psiFile,
        int caretOffset,
        int idEndOffset,
        OffsetMap offsetMap
    ) {
        editor.getCaretModel().moveToOffset(caretOffset);
        WatchingInsertionContext context =
            createInsertionContext(lookup, item, completionChar, editor, psiFile, caretOffset, idEndOffset, offsetMap);
        int initialStartOffset = Math.max(0, caretOffset - item.getLookupString().length());
        Application.get().runWriteAction(() -> {
            try {
                if (caretOffset < idEndOffset && completionChar == Lookup.REPLACE_SELECT_CHAR) {
                    Document document = editor.getDocument();
                    if (document.getRangeGuard(caretOffset, idEndOffset) == null) {
                        document.deleteString(caretOffset, idEndOffset);
                    }
                }

                assert context.getStartOffset() >= 0
                    : "stale startOffset: was " + initialStartOffset +
                    "; selEnd=" + caretOffset +
                    "; idEnd=" + idEndOffset +
                    "; file=" + psiFile;
                assert context.getTailOffset() >= 0
                    : "stale tail: was " + initialStartOffset +
                    "; selEnd=" + caretOffset +
                    "; idEnd=" + idEndOffset +
                    "; file=" + psiFile;

                Project project = psiFile.getProject();
                if (item.requiresCommittedDocuments()) {
                    PsiDocumentManager.getInstance(project).commitAllDocuments();
                }
                item.handleInsert(context);
                PostprocessReformattingAspect.getInstance(project).doPostponedFormatting();
            }
            finally {
                context.stopWatching();
            }

            EditorModificationUtil.scrollToCaret(editor);
        });
        if (lookup != null) {
            update.addSparedChars(lookup, item, context);
        }
        return context;
    }

    @Nonnull
    private static WatchingInsertionContext createInsertionContext(
        @Nullable Lookup lookup,
        LookupElement item,
        char completionChar,
        Editor editor,
        PsiFile psiFile,
        int caretOffset,
        int idEndOffset,
        OffsetMap offsetMap
    ) {
        int initialStartOffset = Math.max(0, caretOffset - item.getLookupString().length());

        offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, initialStartOffset);
        offsetMap.addOffset(CompletionInitializationContext.SELECTION_END_OFFSET, caretOffset);
        offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, idEndOffset);

        List<LookupElement> items = lookup != null ? lookup.getItems() : Collections.emptyList();
        return new WatchingInsertionContext(offsetMap, psiFile, completionChar, items, editor);
    }

    private static WatchingInsertionContext callHandleInsert(
        CompletionProgressIndicator indicator,
        LookupElement item,
        char completionChar
    ) {
        Editor editor = indicator.getEditor();

        int caretOffset = indicator.getCaret().getOffset();
        int idEndOffset = calcIdEndOffset(indicator);
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, indicator.getProject());

        WatchingInsertionContext context = createInsertionContext(indicator.getLookup(),
            item,
            completionChar,
            editor,
            psiFile,
            caretOffset,
            idEndOffset,
            indicator.getOffsetMap()
        );
        try {
            item.handleInsert(context);
        }
        finally {
            context.stopWatching();
        }
        return context;
    }

    public static void addCompletionChar(InsertionContext context, LookupElement item) {
        if (!context.getOffsetMap().containsOffset(InsertionContext.TAIL_OFFSET)) {
            String message = "tailOffset<0 after inserting " + item + " of " + item.getClass();
            if (context instanceof WatchingInsertionContext) {
                message += "; invalidated at: " + ((WatchingInsertionContext)context).invalidateTrace + "\n--------";
            }
            LOG.info(message);
        }
        else if (!CompletionAssertions.isEditorValid(context.getEditor())) {
            LOG.info("Injected editor invalidated " + context.getEditor());
        }
        else {
            context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
        }
        if (context.getCompletionChar() == Lookup.COMPLETE_STATEMENT_SELECT_CHAR) {
            Language language = PsiUtilBase.getLanguageInEditor(context.getEditor(), context.getFile().getProject());
            if (language != null) {
                for (SmartEnterProcessor processor : SmartEnterProcessor.forLanguage(language)) {
                    if (processor.processAfterCompletion(context.getEditor(), context.getFile())) {
                        break;
                    }
                }
            }
        }
        else {
            DataContext dataContext = DataManager.getInstance().getDataContext(context.getEditor().getContentComponent());
            EditorActionManager.getInstance()
                .getTypedAction()
                .getHandler()
                .execute(context.getEditor(), context.getCompletionChar(), dataContext);
        }
    }

    private static boolean isAutocompleteOnInvocation(CompletionType type) {
        CodeInsightSettings settings = CodeInsightSettings.getInstance();
        if (type == CompletionType.SMART) {
            return settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION;
        }
        return settings.AUTOCOMPLETE_ON_CODE_COMPLETION;
    }

    private static Runnable rememberDocumentState(Editor _editor) {
        Editor editor = EditorWindow.getTopLevelEditor(_editor);
        String documentText = editor.getDocument().getText();
        int caret = editor.getCaretModel().getOffset();
        int selStart = editor.getSelectionModel().getSelectionStart();
        int selEnd = editor.getSelectionModel().getSelectionEnd();

        int vOffset = editor.getScrollingModel().getVerticalScrollOffset();
        int hOffset = editor.getScrollingModel().getHorizontalScrollOffset();

        return () -> {
            DocumentEx document = (DocumentEx)editor.getDocument();

            document.replaceString(0, document.getTextLength(), documentText);
            editor.getCaretModel().moveToOffset(caret);
            editor.getSelectionModel().setSelection(selStart, selEnd);

            editor.getScrollingModel().scrollHorizontally(hOffset);
            editor.getScrollingModel().scrollVertically(vOffset);
        };
    }

    private static void clearCaretMarkers(@Nonnull Editor editor) {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            caret.putUserData(CARET_PROCESSED, null);
        }
    }

    private static void markCaretAsProcessed(@Nonnull Caret caret) {
        caret.putUserData(CARET_PROCESSED, Boolean.TRUE);
    }

    private static Caret getNextCaretToProcess(@Nonnull Editor editor) {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            if (caret.getUserData(CARET_PROCESSED) == null) {
                return caret;
            }
        }
        return null;
    }

    @Nullable
    private <T> T withTimeout(long maxDurationMillis, @Nonnull Supplier<T> task) {
        if (isTestingMode()) {
            return task.get();
        }

        return ProgressIndicatorUtils.withTimeout(maxDurationMillis, task);
    }

    private static int calcSyncTimeOut(long startTime) {
        return (int)Math.max(300, ourAutoInsertItemTimeout - (System.currentTimeMillis() - startTime));
    }

    @SuppressWarnings("unused") // for Rider
    @TestOnly
    public static void setAutoInsertTimeout(int timeout) {
        ourAutoInsertItemTimeout = timeout;
    }

    protected boolean isTestingCompletionQualityMode() {
        return false;
    }

    protected boolean isTestingMode() {
        return Application.get().isUnitTestMode() || isTestingCompletionQualityMode();
    }
}
