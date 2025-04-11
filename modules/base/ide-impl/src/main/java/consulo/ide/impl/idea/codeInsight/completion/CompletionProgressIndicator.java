// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.impl.internal.progress.ProgressWrapper;
import consulo.application.internal.JobScheduler;
import consulo.application.progress.ProgressManager;
import consulo.application.util.Semaphore;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.util.TextRange;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionSorterImpl;
import consulo.ide.impl.idea.codeInsight.hint.EditorHintListener;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.util.collection.ContainerUtil;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.impl.internal.completion.OffsetsInFile;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.localize.LanguageLocalize;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceRange;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Please don't use this class directly from plugins.
 */
public class CompletionProgressIndicator extends ProgressIndicatorBase implements CompletionProcessEx, Disposable {
    private static final Logger LOG = Logger.getInstance(CompletionProgressIndicator.class);
    private final Editor myEditor;
    @Nonnull
    private final Caret myCaret;
    @Nullable
    private CompletionParameters myParameters;
    private final CodeCompletionHandlerBase myHandler;
    private final CompletionLookupArrangerImpl myArranger;
    private final CompletionType myCompletionType;
    private final int myInvocationCount;
    private OffsetsInFile myHostOffsets;
    private final LookupEx myLookup;
    private final Alarm mySuppressTimeoutAlarm = new Alarm(this);
    private final MergingUpdateQueue myQueue;
    private final Update myUpdate = new Update("update") {
        @Override
        @RequiredUIAccess
        public void run() {
            updateLookup(myIsUpdateSuppressed);
            myQueue.setMergingTimeSpan(ourShowPopupGroupingTime);
        }
    };
    private final Semaphore myFreezeSemaphore = new Semaphore(1);
    private final Semaphore myFinishSemaphore = new Semaphore(1);
    @Nonnull
    private final OffsetMap myOffsetMap;
    private final Set<Pair<Integer, ElementPattern<String>>> myRestartingPrefixConditions = ContainerUtil.newConcurrentSet();
    private final LookupListener myLookupListener = new LookupListener() {
        @Override
        @RequiredUIAccess
        public void lookupCanceled(@Nonnull LookupEvent event) {
            finishCompletionProcess(true);
        }
    };

    private volatile boolean myIsUpdateSuppressed = false;
    private static int ourInsertSingleItemTimeSpan = 300;

    //temp external setters to make Rider autopopup more reactive
    private static int ourShowPopupGroupingTime = 300;
    private static int ourShowPopupAfterFirstItemGroupingTime = 100;

    private volatile int myCount;
    private volatile boolean myHasPsiElements;
    private boolean myLookupUpdated;
    private final PropertyChangeListener myLookupManagerListener;
    private final Queue<Runnable> myAdvertiserChanges = new ConcurrentLinkedQueue<>();
    private final List<CompletionResult> myDelayedMiddleMatches = new ArrayList<>();
    private final int myStartCaret;
    private final CompletionThreadingBase myThreading;
    private final Object myLock = ObjectUtil.sentinel("CompletionProgressIndicator");

    @RequiredUIAccess
    CompletionProgressIndicator(
        Editor editor,
        @Nonnull Caret caret,
        int invocationCount,
        CodeCompletionHandlerBase handler,
        @Nonnull OffsetMap offsetMap,
        @Nonnull OffsetsInFile hostOffsets,
        boolean hasModifiers,
        @Nonnull LookupEx lookup
    ) {
        myEditor = editor;
        myCaret = caret;
        myHandler = handler;
        myCompletionType = handler.completionType;
        myInvocationCount = invocationCount;
        myOffsetMap = offsetMap;
        myHostOffsets = hostOffsets;
        myLookup = lookup;
        myStartCaret = myEditor.getCaretModel().getOffset();
        myThreading = Application.get().isWriteAccessAllowed() || myHandler.isTestingCompletionQualityMode()
            ? new SyncCompletion()
            : new AsyncCompletion();

        myAdvertiserChanges.offer(() -> myLookup.getAdvertiser().clearAdvertisements());

        myArranger = new CompletionLookupArrangerImpl(this);
        myLookup.setArranger(myArranger);

        myLookup.addLookupListener(myLookupListener);
        myLookup.setCalculating(true);

        myLookupManagerListener = evt -> {
            if (evt.getNewValue() != null) {
                LOG.error("An attempt to change the lookup during completion, phase = " + CompletionServiceImpl.getCompletionPhase());
            }
        };
        LookupManager.getInstance(getProject()).addPropertyChangeListener(myLookupManagerListener);

        myQueue = new MergingUpdateQueue(
            "completion lookup progress",
            ourShowPopupAfterFirstItemGroupingTime,
            true,
            myEditor.getContentComponent()
        );
        myQueue.setPassThrough(false);

        Application.get().assertIsDispatchThread();

        if (hasModifiers && !Application.get().isUnitTestMode()) {
            trackModifiers();
        }
    }

    @Override
    @RequiredUIAccess
    public void itemSelected(@Nullable LookupElement lookupItem, char completionChar) {
        boolean dispose = lookupItem == null;
        finishCompletionProcess(dispose);
        if (dispose) {
            return;
        }

        setMergeCommand();

        myHandler.lookupItemSelected(this, lookupItem, completionChar, myLookup.getItems());
    }

    @Override
    @Nonnull
    @SuppressWarnings("WeakerAccess")
    public OffsetMap getOffsetMap() {
        return myOffsetMap;
    }

    @Nonnull
    @Override
    public OffsetsInFile getHostOffsets() {
        return myHostOffsets;
    }

    @RequiredReadAction
    void duringCompletion(CompletionInitializationContext initContext, CompletionParameters parameters) {
        if (isAutopopupCompletion() && shouldPreselectFirstSuggestion(parameters)) {
            myLookup.setFocusDegree(
                CodeInsightSettings.getInstance().isSelectAutopopupSuggestionsByChars()
                    ? LookupFocusDegree.FOCUSED
                    : LookupFocusDegree.SEMI_FOCUSED
            );
        }
        addDefaultAdvertisements(parameters);

        ProgressManager.checkCanceled();

        Document document = initContext.getEditor().getDocument();
        if (!initContext.getOffsetMap().wasModified(CompletionInitializationContext.IDENTIFIER_END_OFFSET)) {
            try {
                int selectionEndOffset = initContext.getSelectionEndOffset();
                PsiReference reference = TargetElementUtil.findReference(myEditor, selectionEndOffset);
                if (reference != null) {
                    int replacementOffset = findReplacementOffset(selectionEndOffset, reference);
                    if (replacementOffset > document.getTextLength()) {
                        LOG.error(
                            "Invalid replacementOffset: " + replacementOffset + " returned by reference " + reference +
                                " of " + reference.getClass() +
                                "; doc=" + document +
                                "; doc actual=" + (document == initContext.getFile().getViewProvider().getDocument()) +
                                "; doc committed=" + PsiDocumentManager.getInstance(getProject()).isCommitted(document)
                        );
                    }
                    else {
                        initContext.setReplacementOffset(replacementOffset);
                    }
                }
            }
            catch (IndexNotReadyException ignored) {
            }
        }

        for (CompletionContributor contributor
            : CompletionContributor.forLanguageHonorDumbness(initContext.getPositionLanguage(), initContext.getProject())) {
            ProgressManager.checkCanceled();
            contributor.duringCompletion(initContext);
        }
        if (document instanceof DocumentWindow) {
            myHostOffsets = new OffsetsInFile(initContext.getFile(), initContext.getOffsetMap()).toTopLevelFile();
        }
    }


    private void addDefaultAdvertisements(CompletionParameters parameters) {
        if (DumbService.isDumb(getProject())) {
            addAdvertisement("Results might be incomplete while indexing is in progress", PlatformIconGroup.generalWarning());
            return;
        }

        String enterShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM);
        String tabShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE);
        addAdvertisement("Press " + enterShortcut + " to insert, " + tabShortcut + " to replace", null);

        advertiseTabReplacement(parameters);
        if (isAutopopupCompletion()) {
            if (shouldPreselectFirstSuggestion(parameters) && !CodeInsightSettings.getInstance().isSelectAutopopupSuggestionsByChars()) {
                advertiseCtrlDot();
            }
            advertiseCtrlArrows();
        }
    }

    private void advertiseTabReplacement(CompletionParameters parameters) {
        if (CompletionUtil.shouldShowFeature(parameters, CodeCompletionFeatures.EDITING_COMPLETION_REPLACE)
            && myOffsetMap.getOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET) !=
            myOffsetMap.getOffset(CompletionInitializationContext.SELECTION_END_OFFSET)) {
            String shortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE);
            if (StringUtil.isNotEmpty(shortcut)) {
                addAdvertisement("Use " + shortcut + " to overwrite the current identifier with the chosen variant", null);
            }
        }
    }

    private void advertiseCtrlDot() {
        if (FeatureUsageTracker.getInstance()
            .isToBeAdvertisedInLookup(CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_CONTROL_DOT, getProject())) {
            String dotShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_DOT);
            if (StringUtil.isNotEmpty(dotShortcut)) {
                addAdvertisement(
                    "Press " + dotShortcut + " to choose the selected (or first) suggestion and insert a dot afterwards",
                    null
                );
            }
        }
    }

    private void advertiseCtrlArrows() {
        if (!myEditor.isOneLineMode() && FeatureUsageTracker.getInstance()
            .isToBeAdvertisedInLookup(CodeCompletionFeatures.EDITING_COMPLETION_CONTROL_ARROWS, getProject())) {
            String downShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_LOOKUP_DOWN);
            String upShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_LOOKUP_UP);
            if (StringUtil.isNotEmpty(downShortcut) && StringUtil.isNotEmpty(upShortcut)) {
                addAdvertisement(downShortcut + " and " + upShortcut + " will move caret down and up in the editor", null);
            }
        }
    }

    @Override
    public void dispose() {
    }

    @RequiredReadAction
    private static int findReplacementOffset(int selectionEndOffset, PsiReference reference) {
        List<TextRange> ranges = ReferenceRange.getAbsoluteRanges(reference);
        for (TextRange range : ranges) {
            if (range.contains(selectionEndOffset)) {
                return range.getEndOffset();
            }
        }

        return selectionEndOffset;
    }


    void scheduleAdvertising(CompletionParameters parameters) {
        if (myLookup.isAvailableToUser()) {
            return;
        }
        for (CompletionContributor contributor : CompletionContributor.forParameters(parameters)) {
            if (!myLookup.isCalculating() && !myLookup.isVisible()) {
                return;
            }

            String s = contributor.advertise(parameters);
            if (s != null) {
                addAdvertisement(s, null);
            }
        }
    }

    private boolean isOutdated() {
        return CompletionServiceImpl.getCompletionPhase().indicator != this;
    }

    private void trackModifiers() {
        assert !isAutopopupCompletion();

        JComponent contentComponent = myEditor.getContentComponent();
        contentComponent.addKeyListener(new ModifierTracker(contentComponent));
    }

    void setMergeCommand() {
        CommandProcessor.getInstance().setCurrentCommandGroupId(getCompletionCommandName());
    }

    private String getCompletionCommandName() {
        return "Completion" + hashCode();
    }

    @RequiredUIAccess
    void showLookup() {
        updateLookup(myIsUpdateSuppressed);
    }

    // non-null when running generators and adding elements to lookup
    @Override
    @Nullable
    public CompletionParameters getParameters() {
        return myParameters;
    }

    @Override
    public void setParameters(@Nonnull CompletionParameters parameters) {
        myParameters = parameters;
    }

    @Override
    @Nonnull
    public LookupEx getLookup() {
        return myLookup;
    }

    void withSingleUpdate(Runnable action) {
        try {
            myIsUpdateSuppressed = true;
            action.run();
        }
        finally {
            myIsUpdateSuppressed = false;
            myQueue.queue(myUpdate);
        }
    }

    @RequiredUIAccess
    private void updateLookup(boolean isUpdateSuppressed) {
        Application.get().assertIsDispatchThread();
        if (isOutdated() || !shouldShowLookup() || isUpdateSuppressed) {
            return;
        }

        while (true) {
            Runnable action = myAdvertiserChanges.poll();
            if (action == null) {
                break;
            }
            action.run();
        }

        if (!myLookupUpdated) {
            if (myLookup.getAdvertisements().isEmpty() && !isAutopopupCompletion() && !DumbService.isDumb(getProject())) {
                DefaultCompletionContributor.addDefaultAdvertisements(myLookup, myHasPsiElements);
            }
            myLookup.getAdvertiser().showRandomText();
        }

        boolean justShown = false;
        if (!myLookup.isShown()) {
            if (hideAutopopupIfMeaningless()) {
                return;
            }

            if (!myLookup.showLookup()) {
                return;
            }
            justShown = true;
        }
        myLookupUpdated = true;
        myLookup.refreshUi(true, justShown);
        hideAutopopupIfMeaningless();
        if (justShown) {
            myLookup.ensureSelectionVisible(true);
        }
    }

    private boolean shouldShowLookup() {
        if (isAutopopupCompletion()) {
            if (myCount == 0) {
                return false;
            }
            if (myLookup.isCalculating() && Registry.is("ide.completion.delay.autopopup.until.completed")) {
                return false;
            }
        }
        return true;
    }

    void addItem(CompletionResult item) {
        if (!isRunning()) {
            return;
        }
        ProgressManager.checkCanceled();

        if (!myHandler.isTestingMode()) {
            LOG.assertTrue(!Application.get().isDispatchThread());
        }

        LookupElement lookupElement = item.getLookupElement();
        if (!myHasPsiElements && lookupElement.getPsiElement() != null) {
            myHasPsiElements = true;
        }

        boolean forceMiddleMatch = lookupElement.getUserData(CompletionLookupArrangerImpl.FORCE_MIDDLE_MATCH) != null;
        if (forceMiddleMatch) {
            myArranger.associateSorter(lookupElement, (CompletionSorterImpl)item.getSorter());
            addItemToLookup(item);
            return;
        }

        boolean allowMiddleMatches = myCount > CompletionLookupArrangerImpl.MAX_PREFERRED_COUNT * 2;
        if (allowMiddleMatches) {
            addDelayedMiddleMatches();
        }

        myArranger.associateSorter(lookupElement, (CompletionSorterImpl)item.getSorter());
        if (item.isStartMatch() || allowMiddleMatches) {
            addItemToLookup(item);
        }
        else {
            synchronized (myDelayedMiddleMatches) {
                myDelayedMiddleMatches.add(item);
            }
        }
    }

    private void addItemToLookup(CompletionResult item) {
        if (!myLookup.addItem(item.getLookupElement(), item.getPrefixMatcher())) {
            return;
        }

        myArranger.setLastLookupPrefix(myLookup.getAdditionalPrefix());

        //noinspection NonAtomicOperationOnVolatileField
        myCount++; // invoked from a single thread

        if (myCount == 1) {
            JobScheduler.getScheduler().schedule(myFreezeSemaphore::up, ourInsertSingleItemTimeSpan, TimeUnit.MILLISECONDS);
        }
        myQueue.queue(myUpdate);
    }

    void addDelayedMiddleMatches() {
        ArrayList<CompletionResult> delayed;
        synchronized (myDelayedMiddleMatches) {
            if (myDelayedMiddleMatches.isEmpty()) {
                return;
            }
            delayed = new ArrayList<>(myDelayedMiddleMatches);
            myDelayedMiddleMatches.clear();
        }
        for (CompletionResult item : delayed) {
            ProgressManager.checkCanceled();
            addItemToLookup(item);
        }
    }

    @RequiredUIAccess
    public void closeAndFinish(boolean hideLookup) {
        if (!myLookup.isLookupDisposed()) {
            Lookup lookup = LookupManager.getActiveLookup(myEditor);
            LOG.assertTrue(lookup == myLookup, "lookup changed: " + lookup + "; " + this);
        }
        myLookup.removeLookupListener(myLookupListener);
        finishCompletionProcess(true);
        CompletionServiceImpl.assertPhase(CompletionPhase.NoCompletion.getClass());

        if (hideLookup) {
            myLookup.hideLookup(true);
        }
    }

    @RequiredUIAccess
    private void finishCompletionProcess(boolean disposeOffsetMap) {
        cancel();

        Application.get().assertIsDispatchThread();
        Disposer.dispose(myQueue);
        LookupManager.getInstance(getProject()).removePropertyChangeListener(myLookupManagerListener);

        CompletionServiceImpl.assertPhase(
            CompletionPhase.BgCalculation.class,
            CompletionPhase.ItemsCalculated.class,
            CompletionPhase.Synchronous.class,
            CompletionPhase.CommittingDocuments.class
        );

        CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
        LOG.assertTrue(currentCompletion == this, currentCompletion + "!=" + this);

        CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
        if (oldPhase instanceof CompletionPhase.CommittingDocuments committingDocuments) {
            LOG.assertTrue(committingDocuments.indicator != null, oldPhase);
            committingDocuments.replaced = true;
        }
        CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
        if (disposeOffsetMap) {
            disposeIndicator();
        }
    }

    void disposeIndicator() {
        synchronized (myLock) {
            Disposer.dispose(this);
        }
    }

    @Override
    public void registerChildDisposable(@Nonnull Supplier<? extends Disposable> child) {
        synchronized (myLock) {
            // avoid registering stuff on an indicator being disposed concurrently
            checkCanceled();
            Disposer.register(this, child.get());
        }
    }

    @RequiredUIAccess
    @TestOnly
    public static void cleanupForNextTest() {
        CompletionService completionService = ServiceManager.getService(CompletionService.class);
        if (!(completionService instanceof CompletionServiceImpl)) {
            return;
        }

        CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
        if (currentCompletion != null) {
            currentCompletion.finishCompletionProcess(true);
            CompletionServiceImpl.assertPhase(CompletionPhase.NoCompletion.getClass());
        }
        else {
            CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
        }
        StatisticsUpdate.cancelLastCompletionStatisticsUpdate();
    }

    boolean blockingWaitForFinish(int timeoutMs) {
        //if (myHandler.isTestingMode() && !TestModeFlags.is(CompletionAutoPopupHandler.ourTestingAutopopup)) {
        //    if (!myFinishSemaphore.waitFor(100 * 1000)) {
        //        throw new AssertionError("Too long completion");
        //    }
        //    return true;
        //}
        if (myFreezeSemaphore.waitFor(timeoutMs)) {
            // the completion is really finished, now we may auto-insert or show lookup
            return !isRunning() && !isCanceled();
        }
        return false;
    }

    @Override
    public void stop() {
        super.stop();

        myQueue.cancelAllUpdates();
        myFreezeSemaphore.up();
        myFinishSemaphore.up();

        GuiUtils.invokeLaterIfNeeded(
            () -> {
                CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();
                if (!(phase instanceof CompletionPhase.BgCalculation) || phase.indicator != this) {
                    return;
                }

                LOG.assertTrue(!getProject().isDisposed(), "project disposed");

                if (myEditor.isDisposed()) {
                    myLookup.hideLookup(false);
                    CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
                    return;
                }

                if (myEditor instanceof EditorWindow editorWindow) {
                    LOG.assertTrue(editorWindow.getInjectedFile().isValid(), "injected file !valid");
                    LOG.assertTrue(editorWindow.getDocument().isValid(), "docWindow !valid");
                }
                PsiFile file = myLookup.getPsiFile();
                LOG.assertTrue(file == null || file.isValid(), "file !valid");

                myLookup.setCalculating(false);

                if (myCount == 0) {
                    myLookup.hideLookup(false);
                    if (!isAutopopupCompletion()) {
                        CompletionProgressIndicator current = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
                        LOG.assertTrue(current == null, current + "!=" + this);

                        handleEmptyLookup(!((CompletionPhase.BgCalculation)phase).modifiersChanged);
                    }
                }
                else {
                    updateLookup(myIsUpdateSuppressed);
                    if (CompletionServiceImpl.getCompletionPhase() != CompletionPhase.NoCompletion) {
                        CompletionServiceImpl.setCompletionPhase(new CompletionPhase.ItemsCalculated(this));
                    }
                }
            },
            myQueue.getModalityState()
        );
    }

    private boolean hideAutopopupIfMeaningless() {
        if (!myLookup.isLookupDisposed() && isAutopopupCompletion() && !myLookup.isSelectionTouched() && !myLookup.isCalculating()) {
            myLookup.refreshUi(true, false);
            List<LookupElement> items = myLookup.getItems();

            for (LookupElement item : items) {
                if (!isAlreadyInTheEditor(item)) {
                    return false;
                }

                if (item.isValid() && item.isWorthShowingInAutoPopup()) {
                    return false;
                }
            }

            myLookup.hideLookup(false);
            LOG.assertTrue(CompletionServiceImpl.getCompletionService().getCurrentCompletion() == null);
            CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
            return true;
        }
        return false;
    }

    private boolean isAlreadyInTheEditor(LookupElement item) {
        Editor editor = myLookup.getEditor();
        int start = editor.getCaretModel().getOffset() - myLookup.itemPattern(item).length();
        Document document = editor.getDocument();
        return start >= 0 && StringUtil.startsWith(
            document.getImmutableCharSequence().subSequence(start, document.getTextLength()),
            item.getLookupString()
        );
    }

    @RequiredUIAccess
    void restorePrefix(@Nonnull Runnable customRestore) {
        CommandProcessor.getInstance().newCommand()
            .project(getProject())
            .inWriteAction()
            .run(() -> {
                setMergeCommand();
                customRestore.run();
            });
    }

    int nextInvocationCount(int invocation, boolean reused) {
        return reused ? Math.max(myInvocationCount + 1, 2) : invocation;
    }

    @Override
    @Nonnull
    public Editor getEditor() {
        return myEditor;
    }

    @Override
    @Nonnull
    public Caret getCaret() {
        return myCaret;
    }

    boolean isRepeatedInvocation(CompletionType completionType, Editor editor) {
        return !(completionType != myCompletionType || editor != myEditor)
            && (!isAutopopupCompletion() || myLookup.mayBeNoticed());
    }

    @Override
    public boolean isAutopopupCompletion() {
        return myInvocationCount == 0;
    }

    int getInvocationCount() {
        return myInvocationCount;
    }

    @Override
    @Nonnull
    public Project getProject() {
        return ObjectUtil.assertNotNull(myEditor.getProject());
    }

    @Override
    public void addWatchedPrefix(int startOffset, ElementPattern<String> restartCondition) {
        myRestartingPrefixConditions.add(Pair.create(startOffset, restartCondition));
    }

    @Override
    @RequiredUIAccess
    public void prefixUpdated() {
        int caretOffset = myEditor.getCaretModel().getOffset();
        if (caretOffset < myStartCaret) {
            scheduleRestart();
            myRestartingPrefixConditions.clear();
            return;
        }

        CharSequence text = myEditor.getDocument().getCharsSequence();
        for (Pair<Integer, ElementPattern<String>> pair : myRestartingPrefixConditions) {
            int start = pair.first;
            if (caretOffset >= start && start >= 0 && caretOffset <= text.length()) {
                String newPrefix = text.subSequence(start, caretOffset).toString();
                if (pair.second.accepts(newPrefix)) {
                    scheduleRestart();
                    myRestartingPrefixConditions.clear();
                    return;
                }
            }
        }

        hideAutopopupIfMeaningless();
    }

    @Override
    @RequiredUIAccess
    public void scheduleRestart() {
        Application.get().assertIsDispatchThread();
        /*if (myHandler.isTestingMode() && !TestModeFlags.is(CompletionAutoPopupHandler.ourTestingAutopopup)) {
            closeAndFinish(false);
            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
            new CodeCompletionHandlerBase(myCompletionType, false, false, true).invokeCompletion(getProject(), myEditor, myInvocationCount);
            return;
        }*/

        cancel();

        CompletionProgressIndicator current = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
        if (this != current) {
            LOG.error(current + "!=" + this);
        }

        hideAutopopupIfMeaningless();

        CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
        if (oldPhase instanceof CompletionPhase.CommittingDocuments committingDocuments) {
            committingDocuments.replaced = true;
        }

        CompletionPhase.CommittingDocuments.scheduleAsyncCompletion(myEditor, myCompletionType, null, getProject(), this);
    }

    @Override
    public String toString() {
        return "CompletionProgressIndicator[count=" + myCount + ",phase=" + CompletionServiceImpl.getCompletionPhase() + "]@" +
            System.identityHashCode(this);
    }

    @RequiredUIAccess
    void handleEmptyLookup(boolean awaitSecondInvocation) {
        if (isAutopopupCompletion() && Application.get().isUnitTestMode()) {
            return;
        }

        LOG.assertTrue(!isAutopopupCompletion());

        CompletionParameters parameters = getParameters();
        if (myHandler.invokedExplicitly && parameters != null) {
            LightweightHintImpl hint = showErrorHint(getProject(), getEditor(), getNoSuggestionsMessage(parameters));
            if (awaitSecondInvocation) {
                CompletionServiceImpl.setCompletionPhase(new CompletionPhase.NoSuggestionsHint(hint, this));
                return;
            }
        }
        CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
    }

    private String getNoSuggestionsMessage(CompletionParameters parameters) {
        String text = CompletionContributor.forParameters(parameters).stream()
            .map(c -> c.handleEmptyLookup(parameters, getEditor()))
            .filter(StringUtil::isNotEmpty)
            .findFirst()
            .orElse(LanguageLocalize.completionNoSuggestions().get());
        return DumbService.isDumb(getProject()) ? text + "; results might be incomplete while indexing is in progress" : text;
    }

    @RequiredUIAccess
    private static LightweightHintImpl showErrorHint(Project project, Editor editor, String text) {
        LightweightHintImpl[] result = {null};
        EditorHintListener listener = (project1, hint, flags) -> result[0] = hint;
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(EditorHintListener.class, listener);
        assert text != null;
        HintManager.getInstance().showInformationHint(editor, StringUtil.escapeXmlEntities(text), HintManager.UNDER);
        connection.disconnect();
        return result[0];
    }

    private static boolean shouldPreselectFirstSuggestion(CompletionParameters parameters) {
        //if (Registry.is("ide.completion.lookup.element.preselect.depends.on.context")) {
        //    for (CompletionPreselectionBehaviourProvider provider : CompletionPreselectionBehaviourProvider.EP_NAME.getExtensionList()) {
        //        if (!provider.shouldPreselectFirstSuggestion(parameters)) {
        //            return false;
        //        }
        //    }
        //}

        return true;
    }

    @RequiredReadAction
    void runContributors(CompletionInitializationContext initContext) {
        CompletionParameters parameters = Objects.requireNonNull(myParameters);
        myThreading.startThread(
            ProgressWrapper.wrap(this),
            () -> AsyncCompletion.tryReadOrCancel(this, () -> scheduleAdvertising(parameters))
        );
        WeighingDelegate weigher = myThreading.delegateWeighing(this);

        try {
            calculateItems(initContext, weigher, parameters);
        }
        catch (ProcessCanceledException ignore) {
            cancel(); // some contributor may just throw PCE; if indicator is not canceled everything will hang
        }
        catch (Throwable t) {
            cancel();
            LOG.error(t);
        }
    }

    @RequiredReadAction
    private void calculateItems(CompletionInitializationContext initContext, WeighingDelegate weigher, CompletionParameters parameters) {
        duringCompletion(initContext, parameters);
        ProgressManager.checkCanceled();

        CompletionService.getCompletionService().performCompletion(parameters, weigher);
        ProgressManager.checkCanceled();

        weigher.waitFor();
        ProgressManager.checkCanceled();
    }

    @Nonnull
    CompletionThreadingBase getCompletionThreading() {
        return myThreading;
    }

    @Override
    public void addAdvertisement(@Nonnull String text, @Nullable Image icon) {
        myAdvertiserChanges.offer(() -> myLookup.addAdvertisement(text, icon));

        myQueue.queue(myUpdate);
    }

    @SuppressWarnings("unused") // for Rider
    @TestOnly
    public static void setGroupingTimeSpan(int timeSpan) {
        ourInsertSingleItemTimeSpan = timeSpan;
    }

    @Deprecated
    public static void setAutopopupTriggerTime(int timeSpan) {
        ourShowPopupGroupingTime = timeSpan;
        ourShowPopupAfterFirstItemGroupingTime = timeSpan;
    }

    void makeSureLookupIsShown(int timeout) {
        mySuppressTimeoutAlarm.addRequest(this::showIfSuppressed, timeout);
    }

    @RequiredUIAccess
    private void showIfSuppressed() {
        Application.get().assertIsDispatchThread();

        if (myLookup.isShown()) {
            return;
        }

        updateLookup(false);
    }

    private static class ModifierTracker extends KeyAdapter {
        private final JComponent myContentComponent;

        ModifierTracker(JComponent contentComponent) {
            myContentComponent = contentComponent;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            processModifier(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            processModifier(e);
        }

        private void processModifier(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_META || code == KeyEvent.VK_ALT || code == KeyEvent.VK_SHIFT) {
                myContentComponent.removeKeyListener(this);
                CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();
                if (phase instanceof CompletionPhase.BgCalculation bgCalculation) {
                    bgCalculation.modifiersChanged = true;
                }
                else if (phase instanceof CompletionPhase.InsertedSingleItem) {
                    CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
                }
            }
        }
    }
}
