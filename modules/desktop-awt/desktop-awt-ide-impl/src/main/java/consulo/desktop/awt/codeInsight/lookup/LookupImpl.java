// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.desktop.awt.codeInsight.lookup;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.event.*;
import consulo.colorScheme.FontPreferences;
import consulo.colorScheme.impl.internal.FontPreferencesImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.disposer.util.DisposerUtil;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionFeatures;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionHandlerBase;
import consulo.ide.impl.idea.codeInsight.completion.CompletionLookupArrangerImpl;
import consulo.ide.impl.idea.codeInsight.completion.ShowHideIntentionIconLookupAction;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.lookup.impl.*;
import consulo.ide.impl.idea.codeInsight.lookup.impl.actions.ChooseItemAction;
import consulo.ide.impl.idea.codeInsight.lookup.impl.actions.FocusedOnlyChooseItemAction;
import consulo.ide.impl.idea.codeInsight.template.impl.actions.NextVariableAction;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.util.CollectConsumer;
import consulo.util.collection.ContainerUtil;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
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
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.update.Activatable;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class LookupImpl extends LightweightHintImpl implements LookupEx, Disposable, LookupElementListPresenter {
    private static final Logger LOG = Logger.getInstance(LookupImpl.class);
    private static final Key<Font> CUSTOM_FONT_KEY = Key.create("CustomLookupElementFont");

    private final LookupOffsets myOffsets;
    @Nonnull
    private final Project myProject;
    private final Editor myEditor;
    private final Object myArrangerLock = new Object();
    private final Object myUiLock = new Object();
    private final JBList myList = new JBList<LookupElement>(new CollectionListModel<>()) {
        // 'myList' is focused when "Screen Reader" mode is enabled
        @Override
        protected void processKeyEvent(@Nonnull KeyEvent e) {
            myEditor.getContentComponent().dispatchEvent(e); // let the editor handle actions properly for the lookup list
        }

        @Nonnull
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
    private final FontPreferences myFontPreferences = new FontPreferencesImpl();

    private long myStampShown = 0;
    private boolean myShown = false;
    private boolean myDisposed = false;
    private boolean myHidden = false;
    private boolean mySelectionTouched;
    private LookupFocusDegree myFocusDegree = LookupFocusDegree.FOCUSED;
    private volatile boolean myCalculating;
    private final Advertiser myAdComponent;
    private int myGuardedChanges;
    private volatile LookupArranger myArranger;
    private LookupArranger myPresentableArranger;
    private boolean myStartCompletionWhenNothingMatches;
    boolean myResizePending;
    private boolean myFinishing;
    boolean myUpdating;
    private LookupUi myUi;
    private Integer myLastVisibleIndex;
    private final AtomicInteger myDummyItemCount = new AtomicInteger();

    private final EmptyLookupItem myDummyItem = new EmptyLookupItem(CommonLocalize.treeNodeLoading().get(), true);

    @RequiredUIAccess
    public LookupImpl(@Nonnull Project project, Editor editor, @Nonnull LookupArranger arranger) {
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
        myList.setFixedCellWidth(50);
        myList.setBorder(JBUI.Borders.empty());

        // a new top level frame just got the focus. This is important to prevent screen readers
        // from announcing the title of the top level frame when the list is shown (or hidden),
        // as they usually do when a new top-level frame receives the focus.
        AccessibleContextUtil.setParent((Component)myList, myEditor.getContentComponent());

        myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myList.setBackground(LookupCellRenderer.BACKGROUND_COLOR);

        myAdComponent = new Advertiser();
        myAdComponent.setBackground(LookupCellRenderer.BACKGROUND_COLOR);

        myOffsets = new LookupOffsets(myEditor);

        CollectionListModel<LookupElement> model = getListModel();
        addEmptyItem(model);
        updateListHeight(model);

        addListeners();
    }

    @RequiredUIAccess
    public List<LookupElement> getVisibleItems() {
        UIAccess.assertIsUIThread();

        var itemsCount = myList.getItemsCount();
        if (!myShown || itemsCount == 0) {
            return Collections.emptyList();
        }

        synchronized (myUiLock) {
            int lowerItemIndex = myList.getFirstVisibleIndex();
            int higherItemIndex = myList.getLastVisibleIndex();
            if (lowerItemIndex < 0 || higherItemIndex < 0) {
                return Collections.emptyList();
            }

            return getListModel().toList().subList(lowerItemIndex, Math.min(higherItemIndex + 1, itemsCount));
        }
    }

    private CollectionListModel<LookupElement> getListModel() {
        //noinspection unchecked
        return (CollectionListModel<LookupElement>)myList.getModel();
    }

    public LookupArranger getArranger() {
        return myArranger;
    }

    @Override
    public void setArranger(LookupArranger arranger) {
        myArranger = arranger;
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
    public boolean isCalculating() {
        return myCalculating;
    }

    @Override
    public void setCalculating(boolean calculating) {
        myCalculating = calculating;
        if (myUi != null) {
            myUi.setCalculating(calculating);
        }
    }

    @Override
    @RequiredUIAccess
    public void markSelectionTouched() {
        UIAccess.assertIsUIThread();
        mySelectionTouched = true;
        myList.repaint();
    }

    @TestOnly
    public void setSelectionTouched(boolean selectionTouched) {
        mySelectionTouched = selectionTouched;
    }

    @Override
    public int getSelectedIndex() {
        return myList.getSelectedIndex();
    }

    public void setSelectedIndex(int index) {
        myList.setSelectedIndex(index);
        myList.ensureIndexIsVisible(index);
    }

    public void setDummyItemCount(int count) {
        myDummyItemCount.set(count);
    }

    public void repaintLookup(boolean onExplicitAction, boolean reused, boolean selectionVisible, boolean itemsChanged) {
        myUi.refreshUi(selectionVisible, itemsChanged, reused, onExplicitAction);
    }

    @RequiredUIAccess
    public void resort(boolean addAgain) {
        List<LookupElement> items = getItems();

        withLock(() -> {
            myPresentableArranger.prefixChanged(this);
            getListModel().removeAll();
            return null;
        });

        if (addAgain) {
            for (LookupElement item : items) {
                addItem(item, itemMatcher(item));
            }
        }
        refreshUi(true, true);
    }

    @Override
    public boolean addItem(LookupElement item, PrefixMatcher matcher) {
        LookupElementPresentation presentation = renderItemApproximately(item);
        if (containsDummyIdentifier(presentation.getItemText()) || containsDummyIdentifier(presentation.getTailText()) || containsDummyIdentifier(
            presentation.getTypeText())) {
            return false;
        }

        myCellRenderer.itemAdded(item, presentation);
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

    private void addDummyItems(int count) {
        EmptyLookupItem dummy = new EmptyLookupItem("loading...", true);
        for (int i = count; i > 0; i--) {
            getListModel().add(dummy);
        }
    }

    private static boolean containsDummyIdentifier(@Nullable String s) {
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

    @Override
    @RequiredUIAccess
    public void requestResize() {
        UIAccess.assertIsUIThread();
        myResizePending = true;
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
    public List<LookupElement> getItems() {
        return withLock(() -> ContainerUtil.findAll(getListModel().toList(), element -> !(element instanceof EmptyLookupItem)));
    }

    @Override
    @Nonnull
    public String getAdditionalPrefix() {
        return myOffsets.getAdditionalPrefix();
    }

    @Override
    public void fireBeforeAppendPrefix(char c) {
        myPrefixChangeListeners.forEach((listener -> listener.beforeAppend(c)));
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
        myPrefixChangeListeners.forEach((listener -> listener.afterAppend(c)));
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
    public void ensureSelectionVisible(boolean forceTopSelection) {
        if (isSelectionVisible() && !forceTopSelection) {
            return;
        }

        if (!forceTopSelection) {
            ScrollingUtil.ensureIndexIsVisible(myList, myList.getSelectedIndex(), 1);
            return;
        }

        // selected item should be at the top of the visible list
        int top = myList.getSelectedIndex();
        if (top > 0) {
            top--; // show one element above the selected one to give the hint that there are more available via scrolling
        }

        int firstVisibleIndex = myList.getFirstVisibleIndex();
        if (firstVisibleIndex == top) {
            return;
        }

        ScrollingUtil.ensureRangeIsVisible(myList, top, top + myList.getLastVisibleIndex() - firstVisibleIndex);
    }

    @Override
    @RequiredUIAccess
    public void truncatePrefix(boolean preserveSelection, int hideOffset) {
        if (!myOffsets.truncatePrefix()) {
            myArranger.prefixTruncated(this, hideOffset);
            return;
        }
        myPrefixChangeListeners.forEach((PrefixChangeListener::beforeTruncate));

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

        myPrefixChangeListeners.forEach((PrefixChangeListener::afterTruncate));
    }

    @RequiredUIAccess
    void moveToCaretPosition() {
        myOffsets.destabilizeLookupStart();
        refreshUi(false, true);
    }

    @RequiredUIAccess
    private boolean updateList(boolean onExplicitAction, boolean reused) {
        if (!Application.get().isUnitTestMode()) {
            UIAccess.assertIsUIThread();
        }
        checkValid();

        CollectionListModel<LookupElement> listModel = getListModel();

        Pair<List<LookupElement>, Integer> pair = withLock(() -> myPresentableArranger.arrangeItems(this, onExplicitAction || reused));
        List<LookupElement> items = pair.first;
        Integer toSelect = pair.second;
        if (toSelect == null || toSelect < 0 || items.size() > 0 && toSelect >= items.size()) {
            LOG.error("Arranger " + myPresentableArranger + " returned invalid selection index=" + toSelect + "; items=" + items);
            toSelect = 0;
        }

        myOffsets.checkMinPrefixLengthChanges(items, this);
        List<LookupElement> oldModel = listModel.toList();

        synchronized (myUiLock) {
            listModel.removeAll();
            if (!items.isEmpty()) {
                listModel.add(items);
                addDummyItems(myDummyItemCount.get());
            }
            else {
                addEmptyItem(listModel);
            }
        }

        updateListHeight(listModel);

        myList.setSelectedIndex(toSelect);
        return !consulo.ide.impl.idea.util.containers.ContainerUtil.equalsIdentity(oldModel, items);
    }

    public boolean isSelectionVisible() {
        return ScrollingUtil.isIndexFullyVisible(myList, myList.getSelectedIndex());
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
    //so these 'out of dated' items which were matched against an old prefix, should be now matched against the new, updated lookup prefix.
    private void clearIfLookupAndArrangerPrefixesMatch() {
        if (myArranger instanceof CompletionLookupArrangerImpl completionArranger) {
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

    private void updateListHeight(ListModel<LookupElement> model) {
        myList.setFixedCellHeight(myCellRenderer.getListCellRendererComponent(myList, model.getElementAt(0), 0, false, false)
            .getPreferredSize().height);
        myList.setVisibleRowCount(Math.min(model.getSize(), UISettings.getInstance().getMaxLookupListHeight()));
    }

    @RequiredUIAccess
    private void addEmptyItem(CollectionListModel<? super LookupElement> model) {
        LookupElement item = new EmptyLookupItem(myCalculating ? " " : LanguageLocalize.completionNoSuggestions().get(), false);
        model.add(item);

        myCellRenderer.itemAdded(item, LookupElementPresentation.renderElement(item));

        requestResize();
    }

    private static LookupElementPresentation renderItemApproximately(LookupElement item) {
        LookupElementPresentation p = new LookupElementPresentation();
        item.renderElement(p);
        return p;
    }

    @Nonnull
    @Override
    public String itemPattern(@Nonnull LookupElement element) {
        return element instanceof EmptyLookupItem ? "" : myPresentableArranger.itemPattern(element);
    }

    @Override
    @Nonnull
    public PrefixMatcher itemMatcher(@Nonnull LookupElement item) {
        return item instanceof EmptyLookupItem ? new CamelHumpMatcher("") : myPresentableArranger.itemMatcher(item);
    }

    @Override
    @RequiredUIAccess
    public void finishLookup(char completionChar) {
        finishLookup(completionChar, (LookupElement)myList.getSelectedValue());
    }

    @RequiredUIAccess
    @Override
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
        if (item.getUserData(CodeCompletionHandlerBase.DIRECT_INSERTION) != null) {
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

    private void hideWithItemSelected(LookupElement lookupItem, char completionChar) {
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

    @Override
    public int getLookupStart() {
        return myOffsets.getLookupStart(disposeTrace);
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
            HintManagerImpl.getInstanceImpl().updateLocation(this, myEditor, myUi.calculatePosition().getLocation());
        }
        checkValid();
        return true;
    }

    @Override
    public boolean vetoesHiding() {
        return myGuardedChanges > 0;
    }

    @Override
    public boolean isAvailableToUser() {
        if (Application.get().isHeadlessEnvironment()) {
            return myShown;
        }
        return isVisible();
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

            AnActionEvent actionEvent =
                AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_POPUP, null, myEditor.getDataContext());
            delegateActionToEditor(IdeActions.ACTION_EDITOR_BACKSPACE, null, actionEvent);
            delegateActionToEditor(IdeActions.ACTION_EDITOR_ESCAPE, null, actionEvent);
            delegateActionToEditor(IdeActions.ACTION_EDITOR_TAB, ChooseItemAction.Replacing::new, actionEvent);
            delegateActionToEditor(
                IdeActions.ACTION_EDITOR_ENTER,
                /* e.g. rename popup comes initially unfocused */
                () -> getLookupFocusDegree() == LookupFocusDegree.UNFOCUSED ? new NextVariableAction() : new FocusedOnlyChooseItemAction(),
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

        if (!isVisible() || !myList.isShowing()) {
            hideLookup(false);
            return false;
        }

        return true;
    }

    private void fireLookupShown() {
        if (!myListeners.isEmpty()) {
            LookupEvent event = new LookupEvent(this, false);
            for (LookupListener listener : myListeners) {
                listener.lookupShown(event);
            }
        }
    }

    private void delegateActionToEditor(
        @Nonnull String actionID,
        @Nullable Supplier<? extends AnAction> delegateActionSupplier,
        @Nonnull AnActionEvent actionEvent
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

    @Override
    public boolean mayBeNoticed() {
        return myStampShown > 0 && System.currentTimeMillis() - myStampShown > 300;
    }

    private void addListeners() {
        myEditor.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                @RequiredUIAccess
                public void documentChanged(@Nonnull DocumentEvent e) {
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
            public void mouseClicked(@Nonnull EditorMouseEvent e) {
                e.consume();
                hideLookup(false);
            }
        };

        myEditor.getCaretModel().addCaretListener(
            new CaretListener() {
                @Override
                @RequiredUIAccess
                public void caretPositionChanged(@Nonnull CaretEvent e) {
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
                public void selectionChanged(@Nonnull SelectionEvent e) {
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
            public void valueChanged(@Nonnull ListSelectionEvent e) {
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
            public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
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
    @Nullable
    public LookupElement getCurrentItem() {
        synchronized (myUiLock) {
            LookupElement item = (LookupElement)myList.getSelectedValue();
            return item instanceof EmptyLookupItem ? null : item;
        }
    }

    @Override
    public LookupElement getCurrentItemOrEmpty() {
        return (LookupElement)myList.getSelectedValue();
    }

    @Override
    @RequiredUIAccess
    public void setCurrentItem(LookupElement item) {
        markSelectionTouched();
        myList.setSelectedValue(item, false);
    }

    @Override
    public void addLookupListener(LookupListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeLookupListener(LookupListener listener) {
        myListeners.remove(listener);
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

    private boolean fireBeforeItemSelected(@Nullable LookupElement item, char completionChar) {
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

    private void fireLookupCanceled(boolean explicitly) {
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

    private void fireCurrentItemChanged(@Nullable LookupElement oldItem, @Nullable LookupElement currentItem) {
        if (oldItem != currentItem && !myListeners.isEmpty()) {
            LookupEvent event = new LookupEvent(this, currentItem, (char)0);
            for (LookupListener listener : myListeners) {
                listener.currentItemChanged(event);
            }
        }
        myPreview.updatePreview(currentItem);
    }

    private void fireUiRefreshed() {
        for (LookupListener listener : myListeners) {
            listener.uiRefreshed();
        }
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

    @Override
    @Nullable
    public PsiFile getPsiFile() {
        return PsiDocumentManager.getInstance(myProject).getPsiFile(getEditor().getDocument());
    }

    @Override
    public boolean isCompletion() {
        return myArranger.isCompletion();
    }

    @Override
    @RequiredReadAction
    public PsiElement getPsiElement() {
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

    @Nullable
    private static DocumentWindow getInjectedDocument(Project project, Editor editor, int offset) {
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

    @Override
    @Nonnull
    public Editor getEditor() {
        DocumentWindow documentWindow = getInjectedDocument(myProject, myEditor, myEditor.getCaretModel().getOffset());
        if (documentWindow != null) {
            PsiFile injectedFile = PsiDocumentManager.getInstance(myProject).getPsiFile(documentWindow);
            return InjectedLanguageUtil.getInjectedEditorForInjectedFile(myEditor, injectedFile);
        }
        return myEditor;
    }

    @Override
    @Nonnull
    public Editor getTopLevelEditor() {
        return myEditor;
    }

    @Nonnull
    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean isPositionedAboveCaret() {
        return myUi != null && myUi.isPositionedAboveCaret();
    }

    @Override
    public boolean isSelectionTouched() {
        return mySelectionTouched;
    }

    @Override
    public int getLastVisibleIndex() {
        if (myLastVisibleIndex != null) {
            return myLastVisibleIndex;
        }
        return myList.getLastVisibleIndex();
    }

    public void setLastVisibleIndex(int lastVisibleIndex) {
        myLastVisibleIndex = lastVisibleIndex;
    }

    @Override
    public List<String> getAdvertisements() {
        return myAdComponent.getAdvertisements();
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

    private void doHide(boolean fireCanceled, boolean explicitly) {
        if (myDisposed) {
            LOG.error(formatDisposeTrace());
        }
        else {
            myHidden = true;

            try {
                super.hide();

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
    protected void onPopupCancel() {
        hide();
    }

    private Throwable disposeTrace = null;

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
        disposeTrace = new Throwable();
        myDisposed = true;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Disposing lookup:", disposeTrace);
        }

        LookupDispose.staticDisposeTrace = disposeTrace;
    }

    private String formatDisposeTrace() {
        return ExceptionUtil.getThrowableText(disposeTrace) + "\n============";
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
                myUi.refreshUi(selectionVisible, itemsChanged, reused, onExplicitAction);
            }
        }
        finally {
            myUpdating = false;
            fireCurrentItemChanged(prevItem, getCurrentItem());
            fireUiRefreshed();
        }
    }

    @Override
    @RequiredUIAccess
    public void markReused() {
        withLock(() -> myArranger = myArranger.createEmptyCopy());
        requestResize();
    }

    @Override
    @RequiredUIAccess
    public void addAdvertisement(@Nonnull String text, @Nullable Image icon) {
        if (!containsDummyIdentifier(text)) {
            myAdComponent.addAdvertisement(text, icon);
            requestResize();
        }
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

    @Override
    @Nonnull
    public Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(
        @Nonnull Iterable<LookupElement> items,
        boolean hideSingleValued
    ) {
        return withLock(() -> myPresentableArranger.getRelevanceObjects(items, hideSingleValued));
    }

    private <T> T withLock(Supplier<T> computable) {
        synchronized (myArrangerLock) {
            return computable.get();
        }
    }

    public void setPrefixChangeListener(PrefixChangeListener listener) {
        myPrefixChangeListeners.add(listener);
    }

    public void addPrefixChangeListener(PrefixChangeListener listener, Disposable parentDisposable) {
        DisposerUtil.add(listener, myPrefixChangeListeners, parentDisposable);
    }

    FontPreferences getFontPreferences() {
        return myFontPreferences;
    }
}
