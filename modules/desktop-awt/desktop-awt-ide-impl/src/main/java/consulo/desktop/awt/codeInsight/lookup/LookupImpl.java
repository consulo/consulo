// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.codeInsight.lookup;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.event.*;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.colorScheme.internal.FontPreferences;
import consulo.colorScheme.internal.FontPreferencesManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.disposer.util.DisposerUtil;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.ShowHideIntentionIconLookupAction;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.lookup.impl.CompletionExtender;
import consulo.ide.impl.idea.codeInsight.lookup.impl.LookupActionsStep;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.util.CollectConsumer;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.CodeCompletionFeatures;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.impl.internal.completion.lookup.*;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.internal.action.LanguageEditorActions;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.update.Activatable;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class LookupImpl extends LightweightHintImpl implements LookupEx, Disposable, LookupElementListPresenter {
    private static final Logger LOG = Logger.getInstance(LookupImpl.class);
    private static final Key<Font> CUSTOM_FONT_KEY = Key.create("CustomLookupElementFont");

    protected final Project myProject;
    protected final Editor myEditor;

    private final Object myArrangerLock = new Object();
    protected final Object myUiLock = new Object();

    private final LookupOffsets myOffsets;

    private final JBList<LookupElement> myList = new JBList<LookupElement>(new CollectionListModel<>()) {
        // 'myList' is focused when "Screen Reader" mode is enabled
        @Override
        protected void processKeyEvent(KeyEvent e) {
            myEditor.getContentComponent().dispatchEvent(e); // let the editor handle actions properly for the lookup list
        }

        @Override
        protected ExpandableItemsHandler<Integer> createExpandableItemsHandler() {
            return new CompletionExtender(this);
        }
    };
    LookupCellRenderer myCellRenderer;

    private final List<LookupListener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private final List<PrefixChangeListener> myPrefixChangeListeners = Lists.newLockFreeCopyOnWriteList();
    private final LookupPreview myPreview = new LookupPreview(this);
    // keeping our own copy of editor's font preferences, which can be used in non-EDT threads (to avoid race conditions)
    private final FontPreferences myFontPreferences = Application.get().getInstance(FontPreferencesManager.class).newFontPreferences();

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
    private final Advertiser myAdComponent;
    private int myGuardedChanges;
    private boolean myStartCompletionWhenNothingMatches;
    private boolean myFinishing;
    private boolean myUpdating;
    boolean myResizePending;
    private @Nullable LookupUi myUi;
    private @Nullable Integer myLastVisibleIndex;

    private @Nullable Throwable myDisposeTrace = null;

    private final EmptyLookupItem myDummyItem = new EmptyLookupItem(CommonLocalize.treeNodeLoading().get(), true);

    @RequiredUIAccess
    public LookupImpl(Project project, Editor editor, LookupArranger arranger) {
        super(new JPanel(new BorderLayout()));
        setForceShowAsPopup(true);
        setCancelOnClickOutside(false);
        setResizable(true);

        myProject = project;
        myEditor = EditorWindow.getTopLevelEditor(editor);
        myArranger = arranger;
        myPresentableArranger = arranger;
        myEditor.getColorsScheme().getFontPreferences().copyTo(myFontPreferences);

        DaemonCodeAnalyzer.getInstance(myProject).disableUpdateByTimer(this);

        myCellRenderer = new LookupCellRenderer(this, editor.getContentComponent());
        myCellRenderer.itemAdded(myDummyItem, LookupElementPresentation.renderElement(myDummyItem));
        myList.setCellRenderer(myCellRenderer);

        myList.setFocusable(false);
        myList.setFixedCellWidth(JBUI.scale(54));
        myList.setBorder(JBUI.Borders.empty(6));

        // a new top level frame just got the focus. This is important to prevent screen readers
        // from announcing the title of the top level frame when the list is shown (or hidden),
        // as they usually do when a new top-level frame receives the focus.
        AccessibleContextUtil.setParent((Component) myList, myEditor.getContentComponent());

        myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myList.setBackground(LookupCellRenderer.BACKGROUND_COLOR);

        myAdComponent = new Advertiser();
        myAdComponent.setBackground(LookupCellRenderer.BACKGROUND_COLOR);

        myOffsets = new LookupOffsets(myEditor);

        CollectionListModel<LookupElement> model = getListModel();
        addEmptyItem(model);
        updateListHeightUi();

        addEditorListeners();
    }

    private CollectionListModel<LookupElement> getListModel() {
        //noinspection unchecked
        return (CollectionListModel<LookupElement>) myList.getModel();
    }

    /**
     * Replaces everything the list shows. Called with the arranged items, or with a single placeholder when there is
     * nothing to show yet.
     */
    @RequiredUIAccess
    protected void setItemsUi(List<LookupElement> items) {
        CollectionListModel<LookupElement> listModel = getListModel();
        listModel.removeAll();
        if (!items.isEmpty()) {
            listModel.add(items);
        }
    }

    protected List<LookupElement> getItemsUi() {
        return getListModel().toList();
    }

    protected int getItemsCountUi() {
        return myList.getItemsCount();
    }

    protected int getSelectedIndexUi() {
        return myList.getSelectedIndex();
    }

    @RequiredUIAccess
    protected void setSelectedIndexUi(int index) {
        myList.setSelectedIndex(index);
    }

    protected @Nullable LookupElement getSelectedValueUi() {
        return (LookupElement) myList.getSelectedValue();
    }

    protected void setSelectedValueUi(LookupElement item) {
        myList.setSelectedValue(item, false);
    }

    /**
     * -1 when nothing is on screen, which is what a list that has not been laid out yet answers.
     */
    protected int getFirstVisibleIndexUi() {
        return myList.getFirstVisibleIndex();
    }

    protected int getLastVisibleIndexUi() {
        return myList.getLastVisibleIndex();
    }

    protected boolean isSelectionVisibleUi() {
        return ScrollingUtil.isIndexFullyVisible(myList, myList.getSelectedIndex());
    }

    protected void ensureIndexVisibleUi(int index) {
        myList.ensureIndexIsVisible(index);
    }

    protected void ensureRangeVisibleUi(int from, int to) {
        ScrollingUtil.ensureRangeIsVisible(myList, from, to);
    }

    /**
     * An item is about to be shown for the first time. A frontend which caches how an element renders fills that cache
     * here, so the rendering is not redone on every repaint.
     */
    protected void itemAddedUi(LookupElement item, LookupElementPresentation presentation) {
        myCellRenderer.itemAdded(item, presentation);
    }

    /**
     * The number of items changed, so the height the list asks for may have to change with it.
     */
    protected void updateListHeightUi() {
        ListModel<LookupElement> model = getListModel();
        myList.setFixedCellHeight(
            myCellRenderer.getListCellRendererComponent(myList, model.getElementAt(0), 0, false, false).getPreferredSize().height
        );
        myList.setVisibleRowCount(Math.min(model.getSize(), UISettings.getInstance().getMaxLookupListHeight()));
    }

    protected void repaintUi() {
        myList.repaint();
    }

    /**
     * The model has been rearranged and the popup has to be drawn again against it - resized, scrolled, and moved if
     * it no longer fits where it was. Named apart from {@link #refreshUi(boolean, boolean)}, which is the step before
     * it: that one rebuilds the model and then calls this.
     */
    protected void repaintLookupUi(boolean selectionVisible, boolean itemsChanged, boolean reused, boolean onExplicitAction) {
        myUi.refreshUi(selectionVisible, itemsChanged, reused, onExplicitAction);
    }

    /**
     * The document moved under the lookup and the popup has to follow the caret.
     */
    @RequiredUIAccess
    protected void repositionUi() {
        HintManagerImpl.getInstanceImpl().updateLocation(this, myEditor, myUi.calculatePosition().getLocation());
    }

    /**
     * Opens the popup. {@code false} when it could not be shown, which hides the lookup.
     */
    @RequiredUIAccess
    protected boolean showUi() {
        if (!myEditor.getContentComponent().isShowing()) {
            hideLookup(false);
            return false;
        }

        myAdComponent.showRandomText();
        if (Boolean.TRUE.equals(myEditor.getUserData(AutoPopupController.NO_ADS))) {
            myAdComponent.clearAdvertisements();
        }

        myUi = new LookupUi(this, myAdComponent, myList);//, myProject);
        myUi.setCalculating(myCalculating);
        Point p = myUi.calculatePosition().getLocation();
        if (ScreenReader.isActive()) {
            myList.setFocusable(true);
            setFocusRequestor(myList);

            ActionManager actionManager = ActionManager.getInstance();
            AnActionEvent actionEvent =
                AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_POPUP, null, myEditor.getDataContext());
            delegateActionToEditor(IdeActions.ACTION_EDITOR_BACKSPACE, null, actionEvent);
            delegateActionToEditor(IdeActions.ACTION_EDITOR_ESCAPE, null, actionEvent);
            delegateActionToEditor(
                IdeActions.ACTION_EDITOR_TAB,
                () -> actionManager.getAction(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE),
                actionEvent
            );
            delegateActionToEditor(
                IdeActions.ACTION_EDITOR_ENTER,
                /* e.g. rename popup comes initially unfocused */
                () -> getLookupFocusDegree() == LookupFocusDegree.UNFOCUSED
                    ? actionManager.getAction(LanguageEditorActions.NEXT_LIVE_TEMPLATE_VARIABLE)
                    : actionManager.getAction(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM),
                actionEvent
            );
            delegateActionToEditor(IdeActions.ACTION_EDITOR_MOVE_CARET_UP, null, actionEvent);
            delegateActionToEditor(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN, null, actionEvent);
            delegateActionToEditor(IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT, null, actionEvent);
            delegateActionToEditor(IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT, null, actionEvent);
            delegateActionToEditor(IdeActions.ACTION_RENAME, null, actionEvent);
        }
        try {
            HintManagerImpl.getInstanceImpl().showEditorHint(
                this,
                myEditor,
                p,
                HintManager.HIDE_BY_ESCAPE | HintManager.UPDATE_BY_SCROLLING,
                0,
                false,
                HintManagerImpl.getInstanceImpl().createHintHint(myEditor, p, this, HintManager.UNDER)
                    .setRequestFocus(ScreenReader.isActive())
                    .setAwtTooltip(false)
            );
        }
        catch (Exception e) {
            LOG.error(e);
        }

        return isVisible() && myList.isShowing();
    }

    @RequiredUIAccess
    protected void hideUi() {
        super.hide();
    }

    protected void setCalculatingUi(boolean calculating) {
        if (myUi != null) {
            myUi.setCalculating(calculating);
        }
    }

    public LookupArranger getArranger() {
        return myArranger;
    }

    @Override
    public void setArranger(LookupArranger arranger) {
        myArranger = arranger;
    }

    protected <T> T withLock(Supplier<T> computable) {
        synchronized (myArrangerLock) {
            return computable.get();
        }
    }

    @Override
    public boolean addItem(LookupElement item, PrefixMatcher matcher) {
        LookupElementPresentation presentation = renderItemApproximately(item);
        if (containsDummyIdentifier(presentation.getItemText()) || containsDummyIdentifier(presentation.getTailText()) || containsDummyIdentifier(
            presentation.getTypeText())) {
            return false;
        }

        itemAddedUi(item, presentation);
        withLock(() -> {
            myArranger.registerMatcher(item, matcher);
            myArranger.addElement(item, presentation);
            return null;
        });
        return true;
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
    public void repaintLookup(boolean onExplicitAction, boolean reused, boolean selectionVisible, boolean itemsChanged) {
        myUi.refreshUi(selectionVisible, itemsChanged, reused, onExplicitAction);
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

    @Override
    public void updateLookupWidth() {
        myCellRenderer.scheduleUpdateLookupWidthFromVisibleItems();
    }

    @Nullable
    Font getCustomFont(LookupElement item, boolean bold) {
        Font font = item.getUserData(CUSTOM_FONT_KEY);
        return font == null ? null : bold ? font.deriveFont(Font.BOLD) : font;
    }

    public Collection<LookupElementAction> getActionsFor(LookupElement element) {
        CollectConsumer<LookupElementAction> consumer = new CollectConsumer<>();

        myProject.getApplication().getExtensionPoint(LookupActionProvider.class)
            .forEach(it -> it.fillActions(element, this, consumer));
        if (!consumer.getResult().isEmpty()) {
            consumer.accept(new ShowHideIntentionIconLookupAction());
        }
        return consumer.getResult();
    }

    public JList getList() {
        return myList;
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
        itemAddedUi(item, presentation);

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

    //some items may have passed to myArranger from CompletionProgressIndicator for an older prefix
    //these items won't be cleared during appending a new prefix (mayCheckReused = false)
    //so these 'out of dated' items which were matched against an old prefix, should be now matched against the new,
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
    public Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(Iterable<LookupElement> items, boolean hideSingleValued) {
        return withLock(() -> myPresentableArranger.getRelevanceObjects(items, hideSingleValued));
    }

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

    @RequiredUIAccess
    public void setSelectedIndex(int index) {
        setSelectedIndexUi(index);
        ensureIndexVisibleUi(index);
    }

    @RequiredUIAccess
    private void addEmptyItem(CollectionListModel<? super LookupElement> model) {
        model.add(createEmptyItem());
        requestResize();
    }

    private static LookupElementPresentation renderItemApproximately(LookupElement item) {
        LookupElementPresentation p = new LookupElementPresentation();
        item.renderElement(p);
        return p;
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

    @TestOnly
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

    @RequiredUIAccess
    private void hideWithItemSelected(@Nullable LookupElement lookupItem, char completionChar) {
        fireBeforeItemSelected(lookupItem, completionChar);
        doHide(false, true);
        fireItemSelected(lookupItem, completionChar);
    }

    public int getPrefixLength(LookupElement item) {
        return myOffsets.getPrefixLength(item, this);
    }

    @RequiredReadAction
    protected void insertLookupString(LookupElement item, int prefix) {
        insertLookupString(myProject, getTopLevelEditor(), item, itemMatcher(item), itemPattern(item), prefix);
    }

    @RequiredReadAction
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

    @RequiredWriteAction
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

    @RequiredWriteAction
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

    @Override
    public boolean vetoesHiding() {
        return myGuardedChanges > 0;
    }

    @Override
    @RequiredUIAccess
    public boolean showLookup() {
        UIAccess.assertIsUIThread();
        checkValid();
        LOG.assertTrue(!myShown);
        myShown = true;
        myStampShown = System.currentTimeMillis();

        fireLookupShown();

        if (Application.get().isHeadlessEnvironment()) {
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
    public void hide() {
        hideLookup(true);
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
                ToolTipManager.sharedInstance().unregisterComponent(myList);
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

    @Override
    public boolean isCompletion() {
        return myArranger.isCompletion();
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    @RequiredReadAction
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
    @RequiredReadAction
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

    @RequiredReadAction
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

    private void delegateActionToEditor(
        String actionID,
        @Nullable Supplier<? extends AnAction> delegateActionSupplier,
        AnActionEvent actionEvent
    ) {
        AnAction action = ActionManager.getInstance().getAction(actionID);
        DumbAwareAction.create(e -> ActionImplUtil.performActionDumbAware(
                delegateActionSupplier == null ? action : delegateActionSupplier.get(),
                actionEvent
            ))
            .registerCustomShortcutSet(action.getShortcutSet(), myList);
    }

    @Override
    public Advertiser getAdvertiser() {
        return myAdComponent;
    }

    @Override
    public void moveUp() {
        ScrollingUtil.moveUp(getList(), 0);
    }

    @Override
    public void moveDown() {
        ScrollingUtil.moveDown(getList(), 0);
    }

    @Override
    public void movePageUp() {
        ScrollingUtil.movePageUp(getList());
    }

    @Override
    public void movePageDown() {
        ScrollingUtil.movePageDown(getList());
    }

    @Override
    public void moveHome() {
        ScrollingUtil.moveHome(getList());
    }

    @Override
    public void moveEnd() {
        ScrollingUtil.moveEnd(getList());
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

        EditorMouseListener mouseListener = new EditorMouseListener() {
            @Override
            @RequiredUIAccess
            public void mouseClicked(EditorMouseEvent e) {
                e.consume();
                hideLookup(false);
            }
        };

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
        myEditor.addEditorMouseListener(mouseListener, this);

        JComponent editorComponent = myEditor.getContentComponent();
        if (editorComponent.isShowing()) {
            Disposer.register(this, new UiNotifyConnector(editorComponent, new Activatable() {
                @Override
                public void showNotify() {
                }

                @Override
                @RequiredUIAccess
                public void hideNotify() {
                    hideLookup(false);
                }
            }));

            Window window = ComponentUtil.getWindow(editorComponent);
            if (window != null) {
                ComponentListener windowListener = new ComponentAdapter() {
                    @Override
                    @RequiredUIAccess
                    public void componentMoved(ComponentEvent event) {
                        hideLookup(false);
                    }
                };

                window.addComponentListener(windowListener);
                Disposer.register(this, () -> window.removeComponentListener(windowListener));
            }
        }

        myList.addListSelectionListener(new ListSelectionListener() {
            private LookupElement oldItem = null;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!myUpdating) {
                    LookupElement item = getCurrentItem();
                    fireCurrentItemChanged(oldItem, item);
                    oldItem = item;
                }
            }
        });

        new ClickListener() {
            @Override
            @RequiredUIAccess
            public boolean onClick(MouseEvent e, int clickCount) {
                setFocusDegree(LookupFocusDegree.FOCUSED);
                markSelectionTouched();

                if (clickCount == 2) {
                    CommandProcessor.getInstance().newCommand()
                        .project(myProject)
                        .document(myEditor.getDocument())
                        .run(() -> finishLookup(NORMAL_SELECT_CHAR));
                }
                return true;
            }
        }.installOn(myList);
    }

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

    @Override
    public Rectangle getCurrentItemBounds() {
        int index = myList.getSelectedIndex();
        if (index < 0) {
            LOG.error("No selected element, size=" + getListModel().getSize() + "; items" + getItems());
        }
        Rectangle itemBounds = myList.getCellBounds(index, index);
        if (itemBounds == null) {
            LOG.error("No bounds for " + index + "; size=" + getListModel().getSize());
            return null;
        }

        return SwingUtilities.convertRectangle(myList, itemBounds, getComponent());
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
            LookupEvent event = new LookupEvent(this, currentItem, (char) 0);
            for (LookupListener listener : myListeners) {
                listener.currentItemChanged(event);
            }
        }
        myPreview.updatePreview(currentItem);
    }

    protected void fireUiRefreshed() {
        for (LookupListener listener : myListeners) {
            listener.uiRefreshed();
        }
    }

    @Override
    public boolean isPositionedAboveCaret() {
        return myUi != null && myUi.isPositionedAboveCaret();
    }

    @Override
    public List<String> getAdvertisements() {
        return myAdComponent.getAdvertisements();
    }

    @Override
    @RequiredUIAccess
    protected void onPopupCancel() {
        hide();
    }

    @Override
    @RequiredUIAccess
    public void addAdvertisement(String text, @Nullable Image icon) {
        if (!containsDummyIdentifier(text)) {
            myAdComponent.addAdvertisement(text, icon);
            requestResize();
        }
    }

    @Override
    public void showElementActions(@Nullable InputEvent event) {
        if (!isVisible()) {
            return;
        }

        LookupElement element = getCurrentItem();
        if (element == null) {
            return;
        }

        Collection<LookupElementAction> actions = getActionsFor(element);
        if (actions.isEmpty()) {
            return;
        }

        //UIEventLogger.logUIEvent(UIEventId.LookupShowElementActions);

        Rectangle itemBounds = getCurrentItemBounds();
        Rectangle visibleRect = SwingUtilities.convertRectangle(myList, myList.getVisibleRect(), getComponent());
        ListPopup listPopup = JBPopupFactory.getInstance().createListPopup(new LookupActionsStep(actions, this, element));
        Point p = (itemBounds.intersects(visibleRect) || event == null)
            ? new Point(itemBounds.x + itemBounds.width, itemBounds.y)
            : SwingUtilities.convertPoint(
            event.getComponent(),
            new Point(0, event.getComponent().getHeight() + JBUIScale.scale(2)),
            getComponent()
        );

        listPopup.show(new RelativePoint(getComponent(), p));
    }

    FontPreferences getFontPreferences() {
        return myFontPreferences;
    }
}
