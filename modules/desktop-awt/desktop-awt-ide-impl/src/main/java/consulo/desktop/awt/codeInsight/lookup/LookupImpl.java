// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.codeInsight.lookup;

import consulo.application.ui.UISettings;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.colorScheme.internal.FontPreferences;
import consulo.colorScheme.internal.FontPreferencesManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.codeInsight.completion.ShowHideIntentionIconLookupAction;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.lookup.impl.CompletionExtender;
import consulo.ide.impl.idea.codeInsight.lookup.impl.LookupActionsStep;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.util.CollectConsumer;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.impl.internal.completion.lookup.EmptyLookupItem;
import consulo.language.editor.impl.internal.completion.lookup.LookupBase;
import consulo.language.editor.internal.action.LanguageEditorActions;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.update.Activatable;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class LookupImpl extends LookupBase {
    private class MyHint extends LightweightHintImpl {
        private MyHint() {
            super(new JPanel(new BorderLayout()));
            setForceShowAsPopup(true);
            setCancelOnClickOutside(false);
            setResizable(true);
        }

        @Override
        @RequiredUIAccess
        protected void onPopupCancel() {
            LookupImpl.this.hide();
        }

        @Override
        public boolean vetoesHiding() {
            return getGuardedChanges() > 0;
        }
    }

    private static final Logger LOG = Logger.getInstance(LookupImpl.class);
    private static final Key<Font> CUSTOM_FONT_KEY = Key.create("CustomLookupElementFont");

    private final MyHint myHint = new MyHint();

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

    private final LookupPreview myPreview = new LookupPreview(this);
    // keeping our own copy of editor's font preferences, which can be used in non-EDT threads (to avoid race conditions)
    private final FontPreferences myFontPreferences;

    private final Advertiser myAdComponent;
    boolean myResizePending;
    private @Nullable LookupUi myUi;

    @RequiredUIAccess
    public LookupImpl(Project project, Editor editor, LookupArranger arranger) {
        super(project, editor, arranger);

        myFontPreferences = project.getApplication().getInstance(FontPreferencesManager.class).newFontPreferences();
        myEditor.getColorsScheme().getFontPreferences().copyTo(myFontPreferences);

        DaemonCodeAnalyzer.getInstance(myProject).disableUpdateByTimer(this);

        EmptyLookupItem dummyItem = new EmptyLookupItem(CommonLocalize.treeNodeLoading().get(), true);
        myCellRenderer = new LookupCellRenderer(this, editor.getContentComponent());
        myCellRenderer.itemAdded(dummyItem, LookupElementPresentation.renderElement(dummyItem));
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

        CollectionListModel<LookupElement> model = getListModel();
        addEmptyItem(model);
        updateListHeightUi();

        addEditorListeners();
    }

    @Override
    public void setCancelOnClickOutside(boolean cancelOnClickOutside) {
        myHint.setCancelOnClickOutside(cancelOnClickOutside);
    }

    @Override
    public void setCancelOnOtherWindowOpen(boolean cancelOnOtherWindowOpen) {
        myHint.setCancelOnOtherWindowOpen(cancelOnOtherWindowOpen);
    }

    public LightweightHint getHint() {
        return myHint;
    }

    @Override
    public JComponent getComponent() {
        return myHint.getComponent();
    }

    @Override
    public boolean isVisible() {
        return myHint.isVisible();
    }

    @Override
    public Rectangle getBounds() {
        return myHint.getBounds();
    }

    private CollectionListModel<LookupElement> getListModel() {
        //noinspection unchecked
        return (CollectionListModel<LookupElement>) myList.getModel();
    }

    /**
     * Replaces everything the list shows. Called with the arranged items, or with a single placeholder when there is
     * nothing to show yet.
     */
    @Override
    @RequiredUIAccess
    protected void setItemsUi(List<LookupElement> items) {
        CollectionListModel<LookupElement> listModel = getListModel();
        listModel.removeAll();
        if (!items.isEmpty()) {
            listModel.add(items);
        }
    }

    @Override
    @RequiredUIAccess
    protected void presentationChangedUi(LookupElement item) {
        getListModel().contentsChanged(item);
    }

    @Override
    protected List<LookupElement> getItemsUi() {
        return getListModel().toList();
    }

    @Override
    protected int getItemsCountUi() {
        return myList.getItemsCount();
    }

    @Override
    protected int getSelectedIndexUi() {
        return myList.getSelectedIndex();
    }

    @Override
    @RequiredUIAccess
    protected void setSelectedIndexUi(int index) {
        myList.setSelectedIndex(index);
    }

    @Override
    protected @Nullable LookupElement getSelectedValueUi() {
        return myList.getSelectedValue();
    }

    @Override
    @RequiredUIAccess
    protected void setSelectedValueUi(LookupElement item) {
        myList.setSelectedValue(item, false);
    }

    /**
     * -1 when nothing is on screen, which is what a list that has not been laid out yet answers.
     */
    @Override
    protected int getFirstVisibleIndexUi() {
        return myList.getFirstVisibleIndex();
    }

    @Override
    protected int getLastVisibleIndexUi() {
        return myList.getLastVisibleIndex();
    }

    @Override
    protected boolean isSelectionVisibleUi() {
        return ScrollingUtil.isIndexFullyVisible(myList, myList.getSelectedIndex());
    }

    @Override
    protected void ensureIndexVisibleUi(int index) {
        myList.ensureIndexIsVisible(index);
    }

    @Override
    protected void ensureRangeVisibleUi(int from, int to) {
        ScrollingUtil.ensureRangeIsVisible(myList, from, to);
    }

    /**
     * An item is about to be shown for the first time. A frontend which caches how an element renders fills that cache
     * here, so the rendering is not redone on every repaint.
     */
    @Override
    protected void itemAddedUi(LookupElement item, LookupElementPresentation presentation) {
        myCellRenderer.itemAdded(item, presentation);
    }

    /**
     * The number of items changed, so the height the list asks for may have to change with it.
     */
    @Override
    protected void updateListHeightUi() {
        ListModel<LookupElement> model = getListModel();
        myList.setFixedCellHeight(
            myCellRenderer.getListCellRendererComponent(myList, model.getElementAt(0), 0, false, false).getPreferredSize().height
        );
        myList.setVisibleRowCount(Math.min(model.getSize(), UISettings.getInstance().getMaxLookupListHeight()));
    }

    @Override
    protected void repaintUi() {
        myList.repaint();
    }

    /**
     * The model has been rearranged and the popup has to be drawn again against it - resized, scrolled, and moved if
     * it no longer fits where it was. Named apart from {@link #refreshUi(boolean, boolean)}, which is the step before
     * it: that one rebuilds the model and then calls this.
     */
    @Override
    @RequiredUIAccess
    protected void repaintLookupUi(boolean selectionVisible, boolean itemsChanged, boolean reused, boolean onExplicitAction) {
        Objects.requireNonNull(myUi).refreshUi(selectionVisible, itemsChanged, reused, onExplicitAction);
    }

    /**
     * The document moved under the lookup and the popup has to follow the caret.
     */
    @Override
    @RequiredUIAccess
    protected void repositionUi() {
        HintManagerImpl.getInstanceImpl().updateLocation(myHint, myEditor, Objects.requireNonNull(myUi).calculatePosition().getLocation());
    }

    /**
     * Opens the popup. {@code false} when it could not be shown, which hides the lookup.
     */
    @Override
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
        myUi.setCalculating(isCalculating());
        Point p = myUi.calculatePosition().getLocation();
        if (ScreenReader.isActive()) {
            myList.setFocusable(true);
            myHint.setFocusRequestor(myList);

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
                myHint,
                myEditor,
                p,
                HintManager.HIDE_BY_ESCAPE | HintManager.UPDATE_BY_SCROLLING,
                0,
                false,
                HintManagerImpl.getInstanceImpl().createHintHint(myEditor, p, myHint, HintManager.UNDER)
                    .setRequestFocus(ScreenReader.isActive())
                    .setAwtTooltip(false)
            );
        }
        catch (Exception e) {
            LOG.error(e);
        }

        return isVisible() && myList.isShowing();
    }

    @Override
    @RequiredUIAccess
    protected void hideUi() {
        myHint.hide();
    }

    @Override
    protected void setCalculatingUi(boolean calculating) {
        if (myUi != null) {
            myUi.setCalculating(calculating);
        }
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

    @RequiredUIAccess
    private void addEmptyItem(CollectionListModel<? super LookupElement> model) {
        model.add(createEmptyItem());
        requestResize();
    }

    @Override
    @RequiredUIAccess
    public void hide() {
        hideLookup(true);
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        boolean disposed = isLookupDisposed();
        super.dispose();
        if (!disposed) {
            ToolTipManager.sharedInstance().unregisterComponent(myList);
        }
    }

    public void pack() {
        myHint.pack();
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
    @Override
    protected void addEditorListeners() {
        super.addEditorListeners();
        EditorMouseListener mouseListener = new EditorMouseListener() {
            @Override
            @RequiredUIAccess
            public void mouseClicked(EditorMouseEvent e) {
                e.consume();
                hideLookup(false);
            }
        };

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
                if (!isUpdating()) {
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

    @Override
    protected void fireCurrentItemChanged(@Nullable LookupElement oldItem, @Nullable LookupElement currentItem) {
        super.fireCurrentItemChanged(oldItem, currentItem);
        myPreview.updatePreview(currentItem);
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
        ListPopup listPopup = JBPopupFactory.getInstance().createListPopup(myProject, new LookupActionsStep(actions, this, element));
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
