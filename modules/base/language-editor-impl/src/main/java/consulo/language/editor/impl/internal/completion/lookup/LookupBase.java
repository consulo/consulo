/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.completion.lookup;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.disposer.util.DisposerUtil;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.impl.internal.completion.CodeCompletionFeatures;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.lookup.DeferredUserLookupValue;
import consulo.language.editor.completion.lookup.LookupArranger;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementListPresenter;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.completion.lookup.LookupElementRenderer;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupFocusDegree;
import consulo.language.editor.completion.lookup.LookupItem;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.DumbModeAccessType;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.dataholder.Key;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Everything a lookup is which is not the widget it is drawn with - the offsets it tracks in the document, the prefix
 * the user has typed since it opened, the arranger which orders and filters, and what selecting an item does to the
 * text. A frontend supplies the list and the surface it floats on through the {@code *Ui} methods and nothing else.
 * <p/>
 * The item list is deliberately not held here. Swing keeps it inside the {@code JList} model and the selection inside
 * the list's selection model, and a second copy on this side would have to be kept in step with it - so the list is
 * asked for instead, and stays the one place the items live.
 *
 * @author VISTALL
 */
public abstract class LookupBase implements LookupEx, Disposable, LookupElementListPresenter {
    private static final Logger LOG = Logger.getInstance(LookupBase.class);

    private static final Key<LookupElementPresentation> FAST_PRESENTATION = Key.create("FAST_PRESENTATION");
    private static final Key<LookupElementPresentation> LAST_COMPUTED_PRESENTATION = Key.create("LAST_COMPUTED_PRESENTATION");
    private static final Key<CancellablePromise<?>> LAST_COMPUTATION = Key.create("LAST_COMPUTATION");

    /**
     * Where an element which cannot say how it renders without reading the psi is rendered - one thread, off the ui,
     * under a read action. Nothing on the ui side ever calls {@link LookupElement#renderElement} itself.
     */
    private static final Executor ourExpensiveRenderingExecutor =
        SequentialTaskExecutor.createSequentialApplicationPoolExecutor("ExpensiveRendering");

    protected final Project myProject;
    protected final Editor myEditor;

    private final Object myArrangerLock = new Object();
    protected final Object myUiLock = new Object();

    private final LookupOffsets myOffsets;

    private final List<LookupListener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private final List<PrefixChangeListener> myPrefixChangeListeners = Lists.newLockFreeCopyOnWriteList();

    private volatile LookupArranger myArranger;
    private LookupArranger myPresentableArranger;

    private final AtomicInteger myDummyItemCount = new AtomicInteger();

    private long myStampShown = 0;
    private boolean myShown = false;
    private boolean myDisposed = false;
    private boolean myHidden = false;
    private boolean mySelectionTouched;
    private LookupFocusDegree myFocusDegree = LookupFocusDegree.FOCUSED;
    private volatile boolean myCalculating;
    private int myGuardedChanges;
    private boolean myStartCompletionWhenNothingMatches;
    private boolean myFinishing;
    private boolean myUpdating;
    private boolean myResizePending;
    private @Nullable Integer myLastVisibleIndex;

    private @Nullable Throwable myDisposeTrace = null;

    @RequiredUIAccess
    protected LookupBase(Project project, Editor editor, LookupArranger arranger) {
        myProject = project;
        myEditor = EditorWindow.getTopLevelEditor(editor);
        myArranger = arranger;
        myPresentableArranger = arranger;

        myOffsets = new LookupOffsets(myEditor);
    }

    // ------------------------------------------------------------------------------------------------
    // what a frontend supplies - the list widget and the surface the lookup floats on
    // ------------------------------------------------------------------------------------------------

    /**
     * Replaces everything the list shows. Called with the arranged items, or with a single placeholder when there is
     * nothing to show yet.
     */
    protected abstract void setItemsUi(List<LookupElement> items);

    protected abstract List<LookupElement> getItemsUi();

    protected abstract int getItemsCountUi();

    protected abstract int getSelectedIndexUi();

    protected abstract void setSelectedIndexUi(int index);

    protected abstract @Nullable LookupElement getSelectedValueUi();

    protected abstract void setSelectedValueUi(LookupElement item);

    /**
     * -1 when nothing is on screen, which is what a list that has not been laid out yet answers.
     */
    protected abstract int getFirstVisibleIndexUi();

    protected abstract int getLastVisibleIndexUi();

    protected abstract boolean isSelectionVisibleUi();

    protected abstract void ensureIndexVisibleUi(int index);

    protected abstract void ensureRangeVisibleUi(int from, int to);

    /**
     * An item is about to be shown for the first time. A frontend which caches how an element renders fills that cache
     * here, so the rendering is not redone on every repaint.
     */
    protected abstract void itemAddedUi(LookupElement item, LookupElementPresentation presentation);

    /**
     * How an element renders was worked out again off the ui thread and is not what its row was drawn from, so that
     * one row has to be drawn again. Called on the ui thread.
     */
    @RequiredUIAccess
    protected abstract void presentationChangedUi(LookupElement item);

    /**
     * The same, for every row whose render finished since the last time the ui was told. One call rather than one per
     * item, so a frontend which pays per message pays once - see {@link #schedulePresentationFlush}.
     */
    @RequiredUIAccess
    protected void presentationsChangedUi(List<LookupElement> items) {
        for (LookupElement item : items) {
            presentationChangedUi(item);
        }
    }

    /**
     * The number of items changed, so the height the list asks for may have to change with it.
     */
    protected abstract void updateListHeightUi();

    protected abstract void repaintUi();

    /**
     * The model has been rearranged and the popup has to be drawn again against it - resized, scrolled, and moved if
     * it no longer fits where it was. Named apart from {@link #refreshUi(boolean, boolean)}, which is the step before
     * it: that one rebuilds the model and then calls this.
     */
    protected abstract void repaintLookupUi(boolean selectionVisible, boolean itemsChanged, boolean reused, boolean onExplicitAction);

    /**
     * The document moved under the lookup and the popup has to follow the caret.
     */
    protected abstract void repositionUi();

    /**
     * Opens the popup. {@code false} when it could not be shown, which hides the lookup.
     */
    @RequiredUIAccess
    protected abstract boolean showUi();

    @RequiredUIAccess
    protected abstract void hideUi();

    protected abstract void setCalculatingUi(boolean calculating);

    // ------------------------------------------------------------------------------------------------
    // arranger, items, prefix
    // ------------------------------------------------------------------------------------------------

    public LookupArranger getArranger() {
        return myArranger;
    }

    @Override
    public void setArranger(LookupArranger arranger) {
        myArranger = arranger;
    }

    protected LookupArranger getPresentableArranger() {
        return myPresentableArranger;
    }

    protected <T> T withLock(Supplier<T> computable) {
        synchronized (myArrangerLock) {
            return computable.get();
        }
    }

    @Override
    public boolean addItem(LookupElement item, PrefixMatcher matcher) {
        LookupElementPresentation presentation = renderItemApproximately(item);
        if (containsDummyIdentifier(presentation.getItemText())
            || containsDummyIdentifier(presentation.getTailText())
            || containsDummyIdentifier(presentation.getTypeText())) {
            return false;
        }

        // an item added a second time by a re-sort already has the better render of the two, so it is left alone
        if (item.getUserData(LAST_COMPUTED_PRESENTATION) == null) {
            rememberPresentation(item, presentation);
        }
        itemAddedUi(item, presentation);
        withLock(() -> {
            myArranger.registerMatcher(item, matcher);
            myArranger.addElement(item, presentation);
            return null;
        });

        scheduleExpensiveRendering(item);
        return true;
    }

    // ------------------------------------------------------------------------------------------------
    // how an element renders
    // ------------------------------------------------------------------------------------------------

    /**
     * How an element renders, as it was last worked out off the ui thread. Drawing a row replays this and nothing
     * else - an element which has not been added yet has nothing worked out for it, and working it out here would
     * read the psi on the ui thread without a read action, so an empty presentation is answered instead.
     */
    public LookupElementPresentation getPresentation(LookupElement item) {
        LookupElementPresentation presentation = item.getUserData(LAST_COMPUTED_PRESENTATION);
        return presentation == null ? new LookupElementPresentation() : presentation;
    }

    protected static void rememberPresentation(LookupElement item, LookupElementPresentation presentation) {
        item.putUserData(LAST_COMPUTED_PRESENTATION, presentation);
    }

    /**
     * An element which reads more of the psi than the fast render may is rendered again on a pool thread, and the row
     * redrawn once the answer is in. Everything an element wants to show which costs a resolve arrives this way.
     */
    private void scheduleExpensiveRendering(LookupElement item) {
        LookupElementRenderer<? extends LookupElement> renderer = item.getExpensiveRenderer();
        if (renderer == null) {
            return;
        }

        synchronized (LAST_COMPUTATION) {
            cancelExpensiveRendering(item);

            Ref<CancellablePromise<?>> promiseRef = Ref.create();
            CancellablePromise<Void> promise = ReadAction.nonBlocking(() -> {
                if (item.isValid()) {
                    renderExpensively(item, renderer);
                }
                synchronized (LAST_COMPUTATION) {
                    item.replace(LAST_COMPUTATION, promiseRef.get(), null);
                }
            }).expireWith(this).submit(ourExpensiveRenderingExecutor);

            item.putUserData(LAST_COMPUTATION, promise);
            promiseRef.set(promise);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @RequiredReadAction
    private void renderExpensively(LookupElement item, LookupElementRenderer renderer) {
        LookupElementPresentation presentation = new LookupElementPresentation();
        DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(() -> renderer.renderElement(item, presentation));
        presentation.freeze();

        rememberPresentation(item, presentation);

        schedulePresentationFlush(item);
    }

    /**
     * Items whose render finished and whose row has not been told yet.
     */
    private final Set<LookupElement> myChangedPresentations = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean myPresentationFlushScheduled = new AtomicBoolean();

    /**
     * Collects the finished renders and hands them over in one go.
     * <p>
     * Every item of a completion gets an expensive render, and a completion runs to hundreds of them - but only the
     * dozen on screen can be read. The awt lookup answers a finished render by writing the presentation onto the
     * element and nothing else; the row picks it up the next time it is painted, so a hundred renders finishing cost
     * a hundred field writes and one paint.
     * <p>
     * A frontend which draws over a wire has no next paint to wait for and must say something - but it may say it
     * once. One task per item meant one round trip per item, which is why a list of a few visible rows over a long
     * model froze while a short one did not: the cost followed the model, not the screen.
     */
    private void schedulePresentationFlush(LookupElement item) {
        myChangedPresentations.add(item);

        if (!myPresentationFlushScheduled.compareAndSet(false, true)) {
            return;
        }

        UIAccess uiAccess = Application.get().getLastUIAccess();
        uiAccess.give(() -> {
            myPresentationFlushScheduled.set(false);

            List<LookupElement> items = new ArrayList<>(myChangedPresentations);
            myChangedPresentations.removeAll(items);

            if (myDisposed || items.isEmpty()) {
                return;
            }

            presentationsChangedUi(items);
        });
    }

    private static void cancelExpensiveRendering(LookupElement item) {
        synchronized (LAST_COMPUTATION) {
            CancellablePromise<?> promise = item.getUserData(LAST_COMPUTATION);
            if (promise != null) {
                promise.cancel();
                item.putUserData(LAST_COMPUTATION, null);
            }
        }
    }

    public void clear() {
        withLock(() -> {
            myArranger.clear();
            return null;
        });
    }

    @Override
    public List<LookupElement> getItems() {
        return withLock(() -> ContainerUtil.findAll(getItemsUi(), element -> !(element instanceof EmptyLookupItem)));
    }

    @RequiredUIAccess
    public List<LookupElement> getVisibleItems() {
        UIAccess.assertIsUIThread();

        int itemsCount = getItemsCountUi();
        if (!myShown || itemsCount == 0) {
            return Collections.emptyList();
        }

        synchronized (myUiLock) {
            int lowerItemIndex = getFirstVisibleIndexUi();
            int higherItemIndex = getLastVisibleIndexUi();
            if (lowerItemIndex < 0 || higherItemIndex < 0) {
                return Collections.emptyList();
            }

            return getItemsUi().subList(lowerItemIndex, Math.min(higherItemIndex + 1, itemsCount));
        }
    }

    public void setDummyItemCount(int count) {
        myDummyItemCount.set(count);
    }

    @RequiredUIAccess
    public void resort(boolean addAgain) {
        List<LookupElement> items = getItems();

        withLock(() -> {
            myPresentableArranger.prefixChanged(this);
            setItemsUi(List.of());
            return null;
        });

        if (addAgain) {
            for (LookupElement item : items) {
                addItem(item, itemMatcher(item));
            }
        }
        refreshUi(true, true);
    }

    protected static boolean containsDummyIdentifier(@Nullable String s) {
        return s != null && s.contains(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED);
    }

    /**
     * The cheap render of an element, kept on the element itself. Re-sorting adds every item a second time and runs on
     * the ui thread, where reading the psi is not allowed - so an element is only ever rendered once, on the thread
     * which found it.
     */
    private static LookupElementPresentation renderItemApproximately(LookupElement item) {
        LookupElementPresentation cached = item.getUserData(FAST_PRESENTATION);
        if (cached != null) {
            return cached;
        }

        LookupElementPresentation p = new LookupElementPresentation();
        item.renderElement(p);
        item.putUserData(FAST_PRESENTATION, p);
        return p;
    }

    @Override
    public String itemPattern(LookupElement element) {
        return element instanceof EmptyLookupItem ? "" : myPresentableArranger.itemPattern(element);
    }

    @Override
    public PrefixMatcher itemMatcher(LookupElement item) {
        return item instanceof EmptyLookupItem ? new CamelHumpMatcher("") : myPresentableArranger.itemMatcher(item);
    }

    @Override
    public String getAdditionalPrefix() {
        return myOffsets.getAdditionalPrefix();
    }

    @Override
    public void fireBeforeAppendPrefix(char c) {
        myPrefixChangeListeners.forEach(listener -> listener.beforeAppend(c));
    }

    @Override
    @RequiredUIAccess
    public void appendPrefix(char c) {
        checkValid();
        myOffsets.appendPrefix(c);
        withLock(() -> {
            myPresentableArranger.prefixChanged(this);
            return null;
        });
        requestResize();
        refreshUi(false, true);
        ensureSelectionVisible(true);
        myPrefixChangeListeners.forEach(listener -> listener.afterAppend(c));
    }

    @Override
    @RequiredUIAccess
    public void truncatePrefix(boolean preserveSelection, int hideOffset) {
        if (!myOffsets.truncatePrefix()) {
            myArranger.prefixTruncated(this, hideOffset);
            return;
        }
        myPrefixChangeListeners.forEach(PrefixChangeListener::beforeTruncate);

        if (preserveSelection) {
            markSelectionTouched();
        }

        boolean shouldUpdate = withLock(() -> {
            myPresentableArranger.prefixChanged(this);
            return myPresentableArranger == myArranger;
        });
        requestResize();
        if (shouldUpdate) {
            refreshUi(false, true);
            ensureSelectionVisible(true);
        }

        myPrefixChangeListeners.forEach(PrefixChangeListener::afterTruncate);
    }

    @Override
    @RequiredUIAccess
    public void replacePrefix(String presentPrefix, String newPrefix) {
        if (!performGuardedChange(() -> {
            EditorModificationUtil.deleteSelectedText(myEditor);
            int offset = myEditor.getCaretModel().getOffset();
            int start = offset - presentPrefix.length();
            myEditor.getDocument().replaceString(start, offset, newPrefix);
            myOffsets.clearAdditionalPrefix();
            myEditor.getCaretModel().moveToOffset(start + newPrefix.length());
        })) {
            return;
        }
        withLock(() -> {
            myPresentableArranger.prefixReplaced(this, newPrefix);
            return null;
        });
        refreshUi(true, true);
    }

    @RequiredUIAccess
    public void moveToCaretPosition() {
        myOffsets.destabilizeLookupStart();
        refreshUi(false, true);
    }

    public void setPrefixChangeListener(PrefixChangeListener listener) {
        myPrefixChangeListeners.add(listener);
    }

    public void addPrefixChangeListener(PrefixChangeListener listener, Disposable parentDisposable) {
        DisposerUtil.add(listener, myPrefixChangeListeners, parentDisposable);
    }

    // ------------------------------------------------------------------------------------------------
    // the model refresh
    // ------------------------------------------------------------------------------------------------

    @Override
    @RequiredUIAccess
    public void refreshUi(boolean mayCheckReused, boolean onExplicitAction) {
        assert !myUpdating;
        LookupElement prevItem = getCurrentItem();
        myUpdating = true;
        try {
            boolean reused = mayCheckReused && checkReused();
            boolean selectionVisible = isSelectionVisible();
            boolean itemsChanged = updateList(onExplicitAction, reused);
            if (isVisible()) {
                LOG.assertTrue(!Application.get().isUnitTestMode());
                repaintLookupUi(selectionVisible, itemsChanged, reused, onExplicitAction);
            }
        }
        finally {
            myUpdating = false;
            fireCurrentItemChanged(prevItem, getCurrentItem());
            fireUiRefreshed();
        }
    }

    protected boolean isUpdating() {
        return myUpdating;
    }

    @RequiredUIAccess
    private boolean updateList(boolean onExplicitAction, boolean reused) {
        if (!Application.get().isUnitTestMode()) {
            UIAccess.assertIsUIThread();
        }
        checkValid();

        Pair<List<LookupElement>, Integer> pair = withLock(() -> myPresentableArranger.arrangeItems(this, onExplicitAction || reused));
        List<LookupElement> items = pair.first;
        Integer toSelect = pair.second;
        if (toSelect == null || toSelect < 0 || !items.isEmpty() && toSelect >= items.size()) {
            LOG.error("Arranger " + myPresentableArranger + " returned invalid selection index=" + toSelect + "; items=" + items);
            toSelect = 0;
        }

        myOffsets.checkMinPrefixLengthChanges(items, this);
        List<LookupElement> oldModel = getItemsUi();

        synchronized (myUiLock) {
            if (!items.isEmpty()) {
                List<LookupElement> shown = new ArrayList<>(items);
                addDummyItems(shown, myDummyItemCount.get());
                setItemsUi(shown);
            }
            else {
                setItemsUi(List.of(createEmptyItem()));
                requestResize();
            }
        }

        updateListHeightUi();

        setSelectedIndexUi(toSelect);
        return !isSameItems(oldModel, items);
    }

    /**
     * By identity rather than by equals - the arranger swapping one element for another which compares equal still has
     * to redraw.
     */
    private static boolean isSameItems(List<LookupElement> first, List<LookupElement> second) {
        if (first.size() != second.size()) {
            return false;
        }

        for (int i = 0; i < first.size(); i++) {
            if (first.get(i) != second.get(i)) {
                return false;
            }
        }

        return true;
    }

    private static void addDummyItems(List<LookupElement> target, int count) {
        EmptyLookupItem dummy = new EmptyLookupItem("loading...", true);
        rememberPresentation(dummy, LookupElementPresentation.renderElement(dummy));
        for (int i = count; i > 0; i--) {
            target.add(dummy);
        }
    }

    /**
     * The row a lookup with nothing in it still shows - blank while it is still calculating, so a result which is about
     * to arrive does not flash "no suggestions" first.
     */
    @RequiredUIAccess
    protected LookupElement createEmptyItem() {
        LookupElement item = new EmptyLookupItem(myCalculating ? " " : LanguageLocalize.completionNoSuggestions().get(), false);

        LookupElementPresentation presentation = LookupElementPresentation.renderElement(item);
        rememberPresentation(item, presentation);
        itemAddedUi(item, presentation);

        return item;
    }

    protected EmptyLookupItem createLoadingItem() {
        EmptyLookupItem item = new EmptyLookupItem(CommonLocalize.treeNodeLoading().get(), true);
        rememberPresentation(item, LookupElementPresentation.renderElement(item));
        return item;
    }

    private boolean checkReused() {
        return withLock(() -> {
            if (myPresentableArranger != myArranger) {
                myPresentableArranger = myArranger;

                clearIfLookupAndArrangerPrefixesMatch();

                myPresentableArranger.prefixChanged(this);
                return true;
            }

            return false;
        });
    }

    // some items may have passed to myArranger from CompletionProgressIndicator for an older prefix
    // these items won't be cleared during appending a new prefix (mayCheckReused = false)
    // so these 'out of dated' items which were matched against an old prefix, should be now matched against the new,
    // updated lookup prefix.
    private void clearIfLookupAndArrangerPrefixesMatch() {
        if (myArranger instanceof PrefixTrackingLookupArranger completionArranger) {
            String lastLookupArrangersPrefix = completionArranger.getLastLookupPrefix();
            if (lastLookupArrangersPrefix != null && !lastLookupArrangersPrefix.equals(getAdditionalPrefix())) {
                LOG.trace("prefixes don't match, do not clear lookup additional prefix");
            }
            else {
                myOffsets.clearAdditionalPrefix();
            }
        }
        else {
            myOffsets.clearAdditionalPrefix();
        }
    }

    @Override
    @RequiredUIAccess
    public void markReused() {
        withLock(() -> myArranger = myArranger.createEmptyCopy());
        requestResize();
    }

    @Override
    public Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(
        Iterable<LookupElement> items,
        boolean hideSingleValued
    ) {
        return withLock(() -> myPresentableArranger.getRelevanceObjects(items, hideSingleValued));
    }

    // ------------------------------------------------------------------------------------------------
    // selection
    // ------------------------------------------------------------------------------------------------

    @Override
    public @Nullable LookupElement getCurrentItem() {
        synchronized (myUiLock) {
            LookupElement item = getSelectedValueUi();
            return item instanceof EmptyLookupItem ? null : item;
        }
    }

    @Override
    public LookupElement getCurrentItemOrEmpty() {
        return getSelectedValueUi();
    }

    @Override
    @RequiredUIAccess
    public void setCurrentItem(LookupElement item) {
        markSelectionTouched();
        setSelectedValueUi(item);
    }

    @Override
    public int getSelectedIndex() {
        return getSelectedIndexUi();
    }

    public void setSelectedIndex(int index) {
        setSelectedIndexUi(index);
        ensureIndexVisibleUi(index);
    }

    @Override
    public int getLastVisibleIndex() {
        if (myLastVisibleIndex != null) {
            return myLastVisibleIndex;
        }
        return getLastVisibleIndexUi();
    }

    public void setLastVisibleIndex(int lastVisibleIndex) {
        myLastVisibleIndex = lastVisibleIndex;
    }

    public boolean isSelectionVisible() {
        return isSelectionVisibleUi();
    }

    @Override
    public void ensureSelectionVisible(boolean forceTopSelection) {
        if (isSelectionVisible() && !forceTopSelection) {
            return;
        }

        if (!forceTopSelection) {
            ensureIndexVisibleUi(getSelectedIndexUi());
            return;
        }

        // selected item should be at the top of the visible list
        int top = getSelectedIndexUi();
        if (top > 0) {
            top--; // show one element above the selected one to give the hint that there are more available via scrolling
        }

        int firstVisibleIndex = getFirstVisibleIndexUi();
        if (firstVisibleIndex == top) {
            return;
        }

        ensureRangeVisibleUi(top, top + getLastVisibleIndexUi() - firstVisibleIndex);
    }

    @Override
    @RequiredUIAccess
    public void markSelectionTouched() {
        UIAccess.assertIsUIThread();
        mySelectionTouched = true;
        repaintUi();
    }

    public void setSelectionTouched(boolean selectionTouched) {
        mySelectionTouched = selectionTouched;
    }

    @Override
    public boolean isSelectionTouched() {
        return mySelectionTouched;
    }

    @Override
    public LookupFocusDegree getLookupFocusDegree() {
        return myFocusDegree;
    }

    @Override
    public boolean isFocused() {
        return getLookupFocusDegree() == LookupFocusDegree.FOCUSED;
    }

    @Override
    public void setFocusDegree(LookupFocusDegree focusDegree) {
        myFocusDegree = focusDegree;
        for (LookupListener listener : myListeners) {
            listener.focusDegreeChanged();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // selecting an item, and what it does to the document
    // ------------------------------------------------------------------------------------------------

    @Override
    @RequiredUIAccess
    public void finishLookup(char completionChar) {
        finishLookup(completionChar, getSelectedValueUi());
    }

    @Override
    @RequiredUIAccess
    public void finishLookup(char completionChar, @Nullable LookupElement item) {
        LOG.assertTrue(!Application.get().isWriteAccessAllowed(), "finishLookup should be called without a write action");
        PsiFile file = getPsiFile();
        boolean writableOk = file == null || FileModificationService.getInstance().prepareFileForWrite(file);
        if (myDisposed) { // ensureFilesWritable could close us by showing a dialog
            return;
        }

        if (!writableOk) {
            hideWithItemSelected(null, completionChar);
            return;
        }
        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .run(() -> finishLookupInWritableFile(completionChar, item));
    }

    @Override
    @RequiredUIAccess
    public void finishLookupInWritableFile(char completionChar, @Nullable LookupElement item) {
        //noinspection deprecation,unchecked
        if (item == null ||
            !item.isValid() ||
            item instanceof EmptyLookupItem
            || item.getObject() instanceof DeferredUserLookupValue deferredUserLookupValue
            && item.as(LookupItem.CLASS_CONDITION_KEY) != null
            && !deferredUserLookupValue.handleUserSelection(item.as(LookupItem.CLASS_CONDITION_KEY), myProject)) {
            hideWithItemSelected(null, completionChar);
            return;
        }
        if (item.getUserData(CompletionUtil.DIRECT_INSERTION) != null) {
            hideWithItemSelected(item, completionChar);
            return;
        }

        if (myDisposed) { // DeferredUserLookupValue could close us in any way
            return;
        }

        String prefix = itemPattern(item);
        boolean plainMatch = ContainerUtil.or(item.getAllLookupStrings(), s -> StringUtil.containsIgnoreCase(s, prefix));
        if (!plainMatch) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_CAMEL_HUMPS);
        }

        myFinishing = true;
        if (fireBeforeItemSelected(item, completionChar)) {
            Application.get().runWriteAction(() -> {
                myEditor.getDocument().startGuardedBlockChecking();
                try {
                    insertLookupString(item, getPrefixLength(item));
                }
                finally {
                    myEditor.getDocument().stopGuardedBlockChecking();
                }
            });
        }

        if (myDisposed) { // any document listeners could close us
            return;
        }

        doHide(false, true);

        fireItemSelected(item, completionChar);
    }

    private void hideWithItemSelected(@Nullable LookupElement lookupItem, char completionChar) {
        fireBeforeItemSelected(lookupItem, completionChar);
        doHide(false, true);
        fireItemSelected(lookupItem, completionChar);
    }

    public int getPrefixLength(LookupElement item) {
        return myOffsets.getPrefixLength(item, this);
    }

    protected void insertLookupString(LookupElement item, int prefix) {
        insertLookupString(myProject, getTopLevelEditor(), item, itemMatcher(item), itemPattern(item), prefix);
    }

    public static void insertLookupString(
        Project project,
        Editor editor,
        LookupElement item,
        PrefixMatcher matcher,
        String itemPattern,
        int prefixLength
    ) {
        String lookupString = getCaseCorrectedLookupString(item, matcher, itemPattern);

        Editor hostEditor = editor;
        hostEditor.getCaretModel().runForEachCaret(__ -> {
            EditorModificationUtil.deleteSelectedText(hostEditor);
            int caretOffset = hostEditor.getCaretModel().getOffset();

            int offset = insertLookupInDocumentWindowIfNeeded(project, editor, caretOffset, prefixLength, lookupString);
            hostEditor.getCaretModel().moveToOffset(offset);
            hostEditor.getSelectionModel().removeSelection();
        });

        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }

    private static int insertLookupInDocumentWindowIfNeeded(
        Project project,
        Editor editor,
        int caretOffset,
        int prefix,
        String lookupString
    ) {
        DocumentWindow document = getInjectedDocument(project, editor, caretOffset);
        if (document == null) {
            return insertLookupInDocument(caretOffset, editor.getDocument(), prefix, lookupString);
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        int offset = document.hostToInjected(caretOffset);
        int lookupStart = Math.min(offset, Math.max(offset - prefix, 0));
        int diff = -1;
        if (file != null) {
            List<TextRange> ranges =
                InjectedLanguageManager.getInstance(project).intersectWithAllEditableFragments(file, TextRange.create(lookupStart, offset));
            if (!ranges.isEmpty()) {
                diff = ranges.get(0).getStartOffset() - lookupStart;
                if (ranges.size() == 1 && diff == 0) {
                    diff = -1;
                }
            }
        }
        if (diff == -1) {
            return insertLookupInDocument(caretOffset, editor.getDocument(), prefix, lookupString);
        }
        return document.injectedToHost(insertLookupInDocument(
            offset,
            document,
            prefix - diff,
            diff == 0 ? lookupString : lookupString.substring(diff)
        ));
    }

    private static int insertLookupInDocument(int caretOffset, Document document, int prefix, String lookupString) {
        int lookupStart = Math.min(caretOffset, Math.max(caretOffset - prefix, 0));
        int len = document.getTextLength();
        LOG.assertTrue(
            lookupStart >= 0 && lookupStart <= len,
            "ls: " + lookupStart + " caret: " + caretOffset + " prefix:" + prefix + " doc: " + len
        );
        LOG.assertTrue(caretOffset >= 0 && caretOffset <= len, "co: " + caretOffset + " doc: " + len);
        document.replaceString(lookupStart, caretOffset, lookupString);
        return lookupStart + lookupString.length();
    }

    private static String getCaseCorrectedLookupString(LookupElement item, PrefixMatcher prefixMatcher, String prefix) {
        String lookupString = item.getLookupString();
        if (item.isCaseSensitive()) {
            return lookupString;
        }

        int length = prefix.length();
        if (length == 0 || !prefixMatcher.prefixMatches(prefix)) {
            return lookupString;
        }
        boolean isAllLower = true;
        boolean isAllUpper = true;
        boolean sameCase = true;
        for (int i = 0; i < length && (isAllLower || isAllUpper || sameCase); i++) {
            char c = prefix.charAt(i);
            boolean isLower = Character.isLowerCase(c);
            boolean isUpper = Character.isUpperCase(c);
            // do not take this kind of symbols into account ('_', '@', etc.)
            if (!isLower && !isUpper) {
                continue;
            }
            isAllLower = isAllLower && isLower;
            isAllUpper = isAllUpper && isUpper;
            sameCase = sameCase && i < lookupString.length() && isLower == Character.isLowerCase(lookupString.charAt(i));
        }
        if (sameCase) {
            return lookupString;
        }
        if (isAllLower) {
            return StringUtil.toLowerCase(lookupString);
        }
        if (isAllUpper) {
            return StringUtil.toUpperCase(lookupString);
        }
        return lookupString;
    }

    // ------------------------------------------------------------------------------------------------
    // offsets and guarded changes
    // ------------------------------------------------------------------------------------------------

    @Override
    public int getLookupStart() {
        return myOffsets.getLookupStart(myDisposeTrace);
    }

    @Override
    public int getLookupOriginalStart() {
        return myOffsets.getLookupOriginalStart();
    }

    @Override
    @RequiredUIAccess
    public boolean performGuardedChange(Runnable change) {
        checkValid();

        myEditor.getDocument().startGuardedBlockChecking();
        myGuardedChanges++;
        boolean result;
        try {
            result = myOffsets.performGuardedChange(change);
        }
        finally {
            myEditor.getDocument().stopGuardedBlockChecking();
            myGuardedChanges--;
        }
        if (!result || myDisposed) {
            hideLookup(false);
            return false;
        }
        if (isVisible()) {
            repositionUi();
        }
        checkValid();
        return true;
    }

    public boolean vetoesHiding() {
        return myGuardedChanges > 0;
    }

    protected boolean isFinishing() {
        return myFinishing;
    }

    protected int getGuardedChanges() {
        return myGuardedChanges;
    }

    // ------------------------------------------------------------------------------------------------
    // showing and hiding
    // ------------------------------------------------------------------------------------------------

    @Override
    @RequiredUIAccess
    public boolean showLookup() {
        UIAccess.assertIsUIThread();
        checkValid();
        LOG.assertTrue(!myShown);
        myShown = true;
        myStampShown = System.currentTimeMillis();

        fireLookupShown();

        if (Application.get().isUnitTestMode()) {
            return true;
        }

        boolean shown = showUi();
        if (!shown) {
            hideLookup(false);
            return false;
        }

        return true;
    }

    @Override
    @RequiredUIAccess
    public boolean isShown() {
        if (!Application.get().isUnitTestMode()) {
            UIAccess.assertIsUIThread();
        }
        return myShown;
    }

    @Override
    public boolean isAvailableToUser() {
        if (Application.get().isHeadlessEnvironment()) {
            return myShown;
        }
        return isVisible();
    }

    @Override
    public boolean mayBeNoticed() {
        return myStampShown > 0 && System.currentTimeMillis() - myStampShown > 300;
    }

    @Override
    @RequiredUIAccess
    public void hideLookup(boolean explicitly) {
        UIAccess.assertIsUIThread();

        if (myHidden) {
            return;
        }

        doHide(true, explicitly);
    }

    @RequiredUIAccess
    protected void doHide(boolean fireCanceled, boolean explicitly) {
        if (myDisposed) {
            LOG.error(formatDisposeTrace());
        }
        else {
            myHidden = true;

            try {
                hideUi();

                Disposer.dispose(this);
                assert myDisposed;
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        if (fireCanceled) {
            fireLookupCanceled(explicitly);
        }
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        UIAccess.assertIsUIThread();
        assert myHidden;
        if (myDisposed) {
            LOG.error(formatDisposeTrace());
            return;
        }

        myOffsets.disposeMarkers();
        myDisposeTrace = new Throwable();
        myDisposed = true;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Disposing lookup:", myDisposeTrace);
        }

        LookupDispose.staticDisposeTrace = myDisposeTrace;
    }

    @Override
    public boolean isLookupDisposed() {
        return myDisposed;
    }

    @Override
    public void checkValid() {
        if (myDisposed) {
            throw new AssertionError("Disposed at: " + formatDisposeTrace());
        }
    }

    protected String formatDisposeTrace() {
        return ExceptionUtil.getThrowableText(myDisposeTrace) + "\n============";
    }

    // ------------------------------------------------------------------------------------------------
    // state
    // ------------------------------------------------------------------------------------------------

    @Override
    public boolean isCalculating() {
        return myCalculating;
    }

    @Override
    public void setCalculating(boolean calculating) {
        myCalculating = calculating;
        setCalculatingUi(calculating);
    }

    @Override
    public void setStartCompletionWhenNothingMatches(boolean startCompletionWhenNothingMatches) {
        myStartCompletionWhenNothingMatches = startCompletionWhenNothingMatches;
    }

    @Override
    public boolean isStartCompletionWhenNothingMatches() {
        return myStartCompletionWhenNothingMatches;
    }

    @Override
    @RequiredUIAccess
    public void requestResize() {
        UIAccess.assertIsUIThread();
        myResizePending = true;
    }

    protected boolean isResizePending() {
        return myResizePending;
    }

    protected void setResizePending(boolean resizePending) {
        myResizePending = resizePending;
    }

    @Override
    public boolean isCompletion() {
        return myArranger.isCompletion();
    }

    // ------------------------------------------------------------------------------------------------
    // editor and psi
    // ------------------------------------------------------------------------------------------------

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public Editor getEditor() {
        DocumentWindow documentWindow = getInjectedDocument(myProject, myEditor, myEditor.getCaretModel().getOffset());
        if (documentWindow != null) {
            PsiFile injectedFile = PsiDocumentManager.getInstance(myProject).getPsiFile(documentWindow);
            return InjectedEditorManager.getInstance(myProject).getInjectedEditorForInjectedFile(myEditor, injectedFile);
        }
        return myEditor;
    }

    @Override
    public Editor getTopLevelEditor() {
        return myEditor;
    }

    @Override
    public @Nullable PsiFile getPsiFile() {
        return PsiDocumentManager.getInstance(myProject).getPsiFile(getEditor().getDocument());
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getPsiElement() {
        PsiFile file = getPsiFile();
        if (file == null) {
            return null;
        }

        int offset = getLookupStart();
        Editor editor = getEditor();
        if (editor instanceof EditorWindow editorWindow) {
            offset = editor.logicalPositionToOffset(editorWindow.hostToInjected(myEditor.offsetToLogicalPosition(offset)));
        }
        if (offset > 0) {
            return file.findElementAt(offset - 1);
        }

        return file.findElementAt(0);
    }

    private static @Nullable DocumentWindow getInjectedDocument(Project project, Editor editor, int offset) {
        PsiFile hostFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (hostFile != null) {
            // inspired by consulo.ide.impl.idea.codeInsight.editorActions.TypedHandler.injectedEditorIfCharTypedIsSignificant()
            List<DocumentWindow> injected =
                InjectedLanguageManager.getInstance(project).getCachedInjectedDocumentsInRange(hostFile, TextRange.create(offset, offset));
            for (DocumentWindow documentWindow : injected) {
                if (documentWindow.isValid() && documentWindow.containsRange(offset, offset)) {
                    return documentWindow;
                }
            }
        }
        return null;
    }

    /**
     * The document, the caret and the selection all moving out from under the lookup mean it no longer describes what
     * it opened on, so it goes away. A change the lookup itself is making is exempt - it is the one making it.
     */
    protected void addEditorListeners() {
        myEditor.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                @RequiredUIAccess
                public void documentChanged(DocumentEvent e) {
                    if (myGuardedChanges == 0 && !myFinishing) {
                        hideLookup(false);
                    }
                }
            },
            this
        );

        myEditor.getCaretModel().addCaretListener(
            new CaretListener() {
                @Override
                @RequiredUIAccess
                public void caretPositionChanged(CaretEvent e) {
                    if (myGuardedChanges == 0 && !myFinishing) {
                        hideLookup(false);
                    }
                }
            },
            this
        );

        myEditor.getSelectionModel().addSelectionListener(
            new SelectionListener() {
                @Override
                @RequiredUIAccess
                public void selectionChanged(SelectionEvent e) {
                    if (myGuardedChanges == 0 && !myFinishing) {
                        hideLookup(false);
                    }
                }
            },
            this
        );

        myEditor.addEditorMouseListener(
            new EditorMouseListener() {
                @Override
                @RequiredUIAccess
                public void mouseClicked(EditorMouseEvent e) {
                    e.consume();
                    hideLookup(false);
                }
            },
            this
        );
    }

    // ------------------------------------------------------------------------------------------------
    // listeners
    // ------------------------------------------------------------------------------------------------

    @Override
    public void addLookupListener(LookupListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeLookupListener(LookupListener listener) {
        myListeners.remove(listener);
    }

    private void fireLookupShown() {
        if (!myListeners.isEmpty()) {
            LookupEvent event = new LookupEvent(this, false);
            for (LookupListener listener : myListeners) {
                listener.lookupShown(event);
            }
        }
    }

    protected boolean fireBeforeItemSelected(@Nullable LookupElement item, char completionChar) {
        boolean result = true;
        if (!myListeners.isEmpty()) {
            LookupEvent event = new LookupEvent(this, item, completionChar);
            for (LookupListener listener : myListeners) {
                try {
                    if (!listener.beforeItemSelected(event)) {
                        result = false;
                    }
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }
        }
        return result;
    }

    public void fireItemSelected(@Nullable LookupElement item, char completionChar) {
        if (item != null && item.requiresCommittedDocuments()) {
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        }
        myArranger.itemSelected(item, completionChar);
        if (!myListeners.isEmpty()) {
            LookupEvent event = new LookupEvent(this, item, completionChar);
            for (LookupListener listener : myListeners) {
                try {
                    listener.itemSelected(event);
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }
        }
    }

    protected void fireLookupCanceled(boolean explicitly) {
        if (!myListeners.isEmpty()) {
            LookupEvent event = new LookupEvent(this, explicitly);
            for (LookupListener listener : myListeners) {
                try {
                    listener.lookupCanceled(event);
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }
        }
    }

    protected void fireCurrentItemChanged(@Nullable LookupElement oldItem, @Nullable LookupElement currentItem) {
        if (oldItem != currentItem && !myListeners.isEmpty()) {
            LookupEvent event = new LookupEvent(this, currentItem, (char)0);
            for (LookupListener listener : myListeners) {
                listener.currentItemChanged(event);
            }
        }
    }

    protected void fireUiRefreshed() {
        for (LookupListener listener : myListeners) {
            listener.uiRefreshed();
        }
    }
}
