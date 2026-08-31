// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup.list;

import consulo.application.AccessToken;
import consulo.application.ApplicationManager;
import consulo.application.util.ClientId;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ide.impl.idea.ui.ListActions;
import consulo.ide.impl.idea.ui.UiInterceptors;
import consulo.ide.impl.idea.ui.popup.ClosableByLeftArrow;
import consulo.ide.impl.idea.ui.popup.NextStepHandler;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.ui.ex.impl.internal.popup.action.ActionPopupItem;
import consulo.ui.ex.impl.internal.popup.action.ActionPopupStep;
import consulo.ide.impl.idea.ui.popup.actionPopup.PopupInlineActionsSupportKt;
import consulo.ide.ui.popup.HintUpdateSupply;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.statistician.StatisticsInfo;
import consulo.language.statistician.StatisticsManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.RenderItem;
import consulo.ui.TextItemPresentation;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.ex.awt.internal.ListWithInlineButtons;
import consulo.ui.ex.awt.internal.PopupInlineActionsSupport;
import consulo.ui.ex.awt.popup.AWTListPopup;
import consulo.ui.ex.awt.popup.ListPopupModel;
import consulo.disposer.Disposer;
import consulo.ui.model.FlatDataModel;
import consulo.ui.ex.awt.popup.ListPopupStepEx;
import consulo.ui.ex.awt.popup.PopupListElementRenderer;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.MultiSelectionListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ListPopupImpl extends WizardPopup implements AWTListPopup, NextStepHandler {
    private static final Logger LOG = Logger.getInstance(ListPopupImpl.class);

    protected final LazyValue<PopupInlineActionsSupport> myInlineActionsSupport =
        LazyValue.notNull(() -> PopupInlineActionsSupportKt.createSupport(this));

    private MyList myList;
    private @Nullable TextItemRender<Object> myTextItemRender;

    private MyMouseMotionListener myMouseMotionListener;
    private MyMouseListener myMouseListener;
    private final MouseMovementTracker myMouseMovementTracker = new MouseMovementTracker();

    private ListPopupModel myListModel;

    private int myIndexForShowingChild = -1;
    private int myMaxRowCount = 30;
    private boolean myAutoHandleBeforeShow;

    private boolean myShowSubmenuOnHover;
    private boolean myExecuteExpandedItemOnClick;

    private int myMinimumWidth = -1;

    /**
     * @deprecated use {@link #ListPopupImpl(Project, ListPopupStep)} + {@link #setMaxRowCount(int)}
     */
    @Deprecated
    public ListPopupImpl(ListPopupStep aStep, int maxRowCount) {
        this(aStep);
        setMaxRowCount(maxRowCount);
    }

    /**
     * @deprecated use {@link #ListPopupImpl(Project, ListPopupStep)}
     */
    @Deprecated
    public ListPopupImpl(ListPopupStep aStep) {
        this(DataManager.getInstance().getDataContext().getData(Project.KEY), null, aStep, null);
    }

    public ListPopupImpl(@Nullable Project project, ListPopupStep aStep) {
        this(project, null, aStep, null);
    }

    public ListPopupImpl(@Nullable Project project,
                         @Nullable WizardPopup aParent,
                         ListPopupStep aStep,
                         Object parentValue) {
        this(project, aParent, aStep, parentValue, true);
    }

    public ListPopupImpl(@Nullable Project project,
                         @Nullable WizardPopup aParent,
                         ListPopupStep aStep,
                         Object parentValue,
                         boolean forceHeavyPopup) {
        super(project, aParent, aStep, forceHeavyPopup);
        setParentValue(parentValue);
        replacePasteAction();
    }

    public void setMaxRowCount(int maxRowCount) {
        if (maxRowCount <= 0) {
            return;
        }
        myMaxRowCount = maxRowCount;
    }

    public void showUnderneathOfLabel(JLabel label) {
        int offset = -UIUtil.getListCellHPadding() - UIUtil.getListViewportPadding().left;
        if (label.getIcon() != null) {
            offset += label.getIcon().getIconWidth() + label.getIconTextGap();
        }
        show(new RelativePoint(label, new Point(offset, label.getHeight() + 1)));
    }

    @Override
    public ListPopupModel getListModel() {
        return myListModel;
    }

    @Override
    public PopupInlineActionsSupport getPopupInlineActionsSupport() {
        return myInlineActionsSupport.get();
    }

    @Override
    protected boolean beforeShow() {
        myList.addMouseMotionListener(myMouseMotionListener);
        myList.addMouseListener(myMouseListener);

        myList.setVisibleRowCount(Math.min(myMaxRowCount, myListModel.getSize()));

        boolean shouldShow = super.beforeShow();
        if (myAutoHandleBeforeShow) {
            boolean toDispose = tryToAutoSelect(true);
            shouldShow &= !toDispose;
        }

        return shouldShow;
    }

    @Override
    public void goBack() {
        myList.clearSelection();
        super.goBack();
    }

    @Override
    protected void afterShow() {
        tryToAutoSelect(false);
    }

    private boolean tryToAutoSelect(boolean handleFinalChoices) {
        ListPopupStep<Object> listStep = getListStep();
        boolean selected = false;
        if (listStep instanceof MultiSelectionListPopupStep<?>) {
            int[] indices = ((MultiSelectionListPopupStep) listStep).getDefaultOptionIndices();
            if (indices.length > 0) {
                ScrollingUtil.ensureIndexIsVisible(myList, indices[0], 0);
                myList.setSelectedIndices(indices);
                selected = true;
            }
        }
        else {
            int defaultIndex = listStep.getDefaultOptionIndex();
            if (isSelectableAt(defaultIndex)) {
                ScrollingUtil.selectItem(myList, defaultIndex);
                selected = true;
            }
        }

        if (!selected) {
            selectFirstSelectableItem();
        }

        if (listStep.isAutoSelectionEnabled()) {
            if (!isVisible() && getSelectableCount() == 1) {
                return _handleSelect(handleFinalChoices, null);
            }
            else if (isVisible() && hasSingleSelectableItemWithSubmenu()) {
                return _handleSelect(handleFinalChoices, null);
            }
        }

        return false;
    }

    protected boolean shouldUseStatistics() {
        return true;
    }

    private boolean autoSelectUsingStatistics() {
        String filter = getSpeedSearch().getFilter();
        if (!StringUtil.isEmpty(filter)) {
            int maxUseCount = -1;
            int mostUsedValue = -1;
            int elementsCount = myListModel.getSize();
            for (int i = 0; i < elementsCount; i++) {
                Object value = myListModel.getElementAt(i);
                if (!isSelectable(value)) {
                    continue;
                }
                String text = getListStep().getTextFor(value);
                int count = StatisticsManager.getInstance().getUseCount(new StatisticsInfo("#list_popup:" + myStep.getTitle() + "#" + filter, text));
                if (count > maxUseCount) {
                    maxUseCount = count;
                    mostUsedValue = i;
                }
            }

            if (mostUsedValue > 0) {
                ScrollingUtil.selectItem(myList, mostUsedValue);
                return true;
            }
        }

        return false;
    }

    private void selectFirstSelectableItem() {
        for (int i = 0; i < myListModel.getSize(); i++) {
            if (getListStep().isSelectable(myListModel.getElementAt(i))) {
                myList.setSelectedIndex(i);
                break;
            }
        }
    }

    private boolean hasSingleSelectableItemWithSubmenu() {
        boolean oneSubmenuFound = false;
        int countSelectables = 0;
        for (int i = 0; i < myListModel.getSize(); i++) {
            Object elementAt = myListModel.getElementAt(i);
            if (getListStep().isSelectable(elementAt)) {
                countSelectables++;
                if (getStep().hasSubstep(elementAt)) {
                    if (oneSubmenuFound) {
                        return false;
                    }
                    oneSubmenuFound = true;
                }
            }
        }
        return oneSubmenuFound && countSelectables == 1;
    }

    private int getSelectableCount() {
        int count = 0;
        for (int i = 0; i < myListModel.getSize(); i++) {
            Object each = myListModel.getElementAt(i);
            if (getListStep().isSelectable(each)) {
                count++;
            }
        }

        return count;
    }

    @Override
    public JList getList() {
        return myList;
    }

    @Override
    protected JComponent createContent() {
        myMouseMotionListener = new MyMouseMotionListener();
        myMouseListener = new MyMouseListener();

        ListPopupStep<Object> step = getListStep();
        myListModel = new ListPopupModel(this, getSpeedSearch(), step);

        FlatDataModel<Object> stepModel = step.getModel();
        if (stepModel != null) {
            Disposer.register(this, stepModel.addListener(event -> {
                myListModel.syncModel();
                myList.setVisibleRowCount(Math.min(myMaxRowCount, myListModel.getSize()));
            }));
        }

        myList = new MyList();
        if (myStep.getTitle() != null) {
            myList.getAccessibleContext().setAccessibleName(myStep.getTitle());
        }
        if (step instanceof ListPopupStepEx) {
            ((ListPopupStepEx) step).setEmptyText(myList.getEmptyText());
        }

        myList.setSelectionModel(new MyListSelectionModel());

        selectFirstSelectableItem();

        myList.setBorder(JBUI.Borders.empty(6));

        ScrollingUtil.installActions(myList);

        myList.setCellRenderer(getListElementRenderer());

        registerAction("handleSelection1", KeyEvent.VK_ENTER, 0, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSelect(true);
            }
        });

        myList.getActionMap().put(ListActions.Right.ID, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selected = myList.getSelectedValue();
                if (selected != null && myInlineActionsSupport.get().hasExtraButtons(selected)) {
                    if (nextExtendedButton(selected)) {
                        return;
                    }
                }

                handleRightKeyPressed(createKeyEvent(e, KeyEvent.VK_RIGHT));
            }
        });

        myList.getActionMap().put(ListActions.Left.ID, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selected = myList.getSelectedValue();
                if (selected != null && myInlineActionsSupport.get().hasExtraButtons(selected)) {
                    if (prevExtendedButton(selected)) {
                        return;
                    }
                }

                disposeAsyncPopupWaiter();
                if (isClosableByLeftArrow()) {
                    goBack();
                }
                else {
                    handleLeftKeyPressed(createKeyEvent(e, KeyEvent.VK_LEFT));
                }
            }
        });

        myList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return myList;
    }

    protected KeyEvent createKeyEvent(ActionEvent e, int keyCode) {
        return new KeyEvent(myList, KeyEvent.KEY_PRESSED, e.getWhen(), e.getModifiers(), keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    protected void handleLeftKeyPressed(KeyEvent keyEvent) {
    }

    protected void handleRightKeyPressed(KeyEvent keyEvent) {
        handleSelect(false, keyEvent);
    }

    private boolean nextExtendedButton(Object selected) {
        Integer currentIndex = myList.getSelectedButtonIndex();
        PopupInlineActionsSupport inlineActionsSupport = myInlineActionsSupport.get();
        int buttonsCount = inlineActionsSupport.calcExtraButtonsCount(selected);
        if (currentIndex == null) {
            currentIndex = -1;
        }
        if (currentIndex >= buttonsCount - 1) {
            return false;
        }

        boolean changed = myList.setSelectedButtonIndex(++currentIndex);
        if (changed) {
            getContent().repaint();

            if (inlineActionsSupport.isMoreButton(selected, currentIndex) && buttonsCount == 1) {
                inlineActionsSupport.performAction(selected, currentIndex, null);
            }
        }
        return true;
    }

    private boolean prevExtendedButton(Object selected) {
        Integer currentIndex = myList.getSelectedButtonIndex();
        if (currentIndex == null) {
            return false;
        }

        if (--currentIndex < 0) {
            currentIndex = null;
        }

        boolean changed = myList.setSelectedButtonIndex(currentIndex);
        if (changed) {
            getContent().repaint();
        }
        return true;
    }

    private boolean isMultiSelectionEnabled() {
        return getListStep() instanceof MultiSelectionListPopupStep<?>;
    }

    private boolean isClosableByLeftArrow() {
        return getParent() != null || myStep instanceof ClosableByLeftArrow;
    }

    @Override
    protected ActionMap getActionMap() {
        return myList.getActionMap();
    }

    @Override
    protected InputMap getInputMap() {
        return myList.getInputMap();
    }

    protected ListCellRenderer getListElementRenderer() {
        TextItemRender<Object> render = myTextItemRender;
        if (render != null) {
            return new TextItemRenderCellRenderer(render, getSpeedSearch());
        }
        return new PopupListElementRenderer(this);
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public void setRender(TextItemRender<?> render) {
        myTextItemRender = (TextItemRender<Object>) render;
        if (myList != null) {
            myList.setCellRenderer(getListElementRenderer());
        }
    }

    /**
     * The rows of a render-driven popup. The render paints in platform terms into a colored component;
     * a background it sets stands only while the row is unselected, so the selection never loses its
     * own color to a row's. Whatever the render pins as a suffix is drawn on the trailing edge, past
     * the space the main text leaves.
     */
    private static class TextItemRenderCellRenderer extends JPanel implements ListCellRenderer<Object> {
        private final TextItemRender<Object> myRender;
        private final @Nullable SpeedSearchSupply mySpeedSearch;

        private final MainComponent myMainComponent = new MainComponent();
        private final SimpleColoredComponent mySuffixComponent = new SimpleColoredComponent();

        private LocalizeValue mySuffixText = LocalizeValue.empty();
        private Image mySuffixIcon;

        private TextItemRenderCellRenderer(TextItemRender<Object> render, @Nullable SpeedSearchSupply speedSearch) {
            super(new BorderLayout());
            myRender = render;
            mySpeedSearch = speedSearch;
            setOpaque(true);
            mySuffixComponent.setIpad(JBUI.insets(2, 8));
            mySuffixComponent.setOpaque(false);
        }

        private class MainComponent extends ColoredListCellRenderer<Object> {
            private MainComponent() {
                setIpad(JBUI.insets(2, 8));
            }

            @Override
            protected void customizeCellRenderer(JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
                myRender.render(new ColoredTextItemPresentation(this) {
                    @Override
                    public TextItemPresentation withBackgroundColor(@Nullable ColorValue color) {
                        if (color != null && !selected) {
                            super.withBackgroundColor(color);
                        }
                        return this;
                    }

                    @Override
                    public TextItemPresentation withSuffix(LocalizeValue text, @Nullable Image icon) {
                        mySuffixText = text;
                        mySuffixIcon = icon;
                        return this;
                    }
                }, RenderItem.of(value, selected));

                SpeedSearchUtil.applySpeedSearchHighlighting(mySpeedSearch, this, false, selected);
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
            removeAll();

            mySuffixText = LocalizeValue.empty();
            mySuffixIcon = null;

            Component main = myMainComponent.getListCellRendererComponent(list, value, index, selected, hasFocus);
            Color background = ObjectUtil.notNull(main.getBackground(), list.getBackground());
            myMainComponent.setOpaque(false);
            add(main, BorderLayout.WEST);
            setBackground(background);

            if (mySuffixText.isNotEmpty() || mySuffixIcon != null) {
                mySuffixComponent.clear();
                mySuffixComponent.setIcon(mySuffixIcon);
                if (mySuffixText.isNotEmpty()) {
                    mySuffixComponent.append(
                        mySuffixText,
                        selected
                            ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.getSelectionForeground())
                            : SimpleTextAttributes.GRAYED_ATTRIBUTES
                    );
                }
                add(mySuffixComponent, BorderLayout.EAST);
            }

            return this;
        }
    }

    @Override
    public void setMinimumWidth(int width) {
        myMinimumWidth = width;
    }

    /**
     * The items of an action group arrive after the popup is up, so whatever it was packed to is a size for a list
     * it no longer holds.
     */
    protected void updatePopupSize() {
        int minimumWidth = myMinimumWidth <= 0 ? 0 : JBUIScale.scale(myMinimumWidth);

        ApplicationManager.getApplication().invokeLater(() -> {
            JComponent content = getContent();

            // a size set here is the one handed back next time, so the content is asked what it wants before
            // anything is forced on it
            content.setPreferredSize(null);

            Dimension preferred = content.getPreferredSize();
            Dimension size = new Dimension(Math.max(preferred.width, minimumWidth), preferred.height);

            content.setPreferredSize(size);
            content.setSize(size);
            setSize(size);
        });
    }

    @Override
    public ListPopupStep getListStep() {
        return (ListPopupStep) myStep;
    }

    @Override
    public void dispose() {
        myList.removeMouseMotionListener(myMouseMotionListener);
        myList.removeMouseListener(myMouseListener);
        super.dispose();
    }

    @Override
    public int getSelectedIndex() {
        return myList.getSelectedIndex();
    }

    protected Rectangle getCellBounds(int i) {
        return myList.getCellBounds(i, i);
    }

    @Override
    public void disposeChildren() {
        setIndexForShowingChild(-1);
        super.disposeChildren();
    }

    @Override
    protected void onAutoSelectionTimer() {
        if (myList.getModel().getSize() > 0 && !myList.isSelectionEmpty()) {
            handleSelect(false);
        }
        else {
            disposeChildren();
            setIndexForShowingChild(-1);
        }
    }

    @Override
    public void handleSelect(boolean handleFinalChoices) {
        _handleSelect(handleFinalChoices, null);
    }

    @Override
    public void handleSelect(boolean handleFinalChoices, InputEvent e) {
        _handleSelect(handleFinalChoices, e);
    }

    @SuppressWarnings("unchecked")
    private boolean _handleSelect(boolean handleFinalChoices, @Nullable InputEvent e) {
        if (myList.getSelectedIndex() == -1) {
            return false;
        }

        if (getSpeedSearch().isHoldingFilter() && myList.getModel().getSize() == 0) {
            return false;
        }

        if (myList.getSelectedIndex() == getIndexForShowingChild()) {
            if (myChild != null && !myChild.isVisible()) {
                setIndexForShowingChild(-1);
            }
            return false;
        }

        List<Object> selection = myList.getSelectedValuesList();
        Object selectedValue = selection.getFirst();

        ListPopupStep<Object> listStep = getListStep();
        if (!listStep.isSelectable(selectedValue)) {
            return false;
        }

        if ((listStep instanceof MultiSelectionListPopupStep<?> && !((MultiSelectionListPopupStep<Object>) listStep).hasSubstep(selection) || !listStep.hasSubstep(selectedValue)) &&
            !handleFinalChoices) {
            return false;
        }

        disposeChildren();

        if (myListModel.getSize() == 0) {
            setFinalRunnable(myStep.getFinalRunnable());
            setOk(true);
            disposeAllParents(e);
            setIndexForShowingChild(-1);
            return true;
        }

        valuesSelected(selection);

        Object onlyItem = ContainerUtil.getOnlyItem(selection);
        if (e instanceof MouseEvent me && onlyItem != null) {
            myMouseMotionListener.updateSelectedButtonIndex(me.getPoint());
        }

        Integer inlineButtonIndex = myList.getSelectedButtonIndex();
        if (inlineButtonIndex != null) {
            KeepPopupOnPerform keepPopup = myInlineActionsSupport.get().getKeepPopupOnPerform(selectedValue, inlineButtonIndex);

            if (!ActionImplUtil.isKeepPopupOpen(keepPopup, e)) {
                disposePopup(e);
            }
            myInlineActionsSupport.get().performAction(selectedValue, inlineButtonIndex, e);
            return true;
        }

        AtomicBoolean insideOnChosen = new AtomicBoolean(true);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (insideOnChosen.get()) {
                LOG.error("Showing dialogs from popup onChosen can result in focus issues. Please put the handler into BaseStep.doFinalStep or PopupStep.getFinalRunnable.");
            }
        }, ModalityState.any());

        PopupStep nextStep;
        try {
            if (listStep instanceof MultiSelectionListPopupStep<?>) {
                nextStep = ((MultiSelectionListPopupStep<Object>) listStep).onChosen(Arrays.asList(selection), handleFinalChoices);
            }
            else if (e != null && listStep instanceof ListPopupStepEx<?>) {
                nextStep = ((ListPopupStepEx<Object>) listStep).onChosen(selectedValue, handleFinalChoices, e);
            }
            else {
                nextStep = listStep.onChosen(selectedValue, handleFinalChoices);
            }
        }
        finally {
            insideOnChosen.set(false);
        }

        if (nextStep == PopupStep.FINAL_CHOICE &&
            onlyItem instanceof ActionPopupItem item &&
            ActionImplUtil.isKeepPopupOpen(item.getKeepPopupOnPerform(), e)) {
            ActionPopupStep actionPopupStep = ObjectUtil.tryCast(getListStep(), ActionPopupStep.class);
            if (actionPopupStep != null && actionPopupStep.isSelectable(item)) {
                actionPopupStep.updateStepItems(getList());
                return false;
            }
        }

        return handleNextStep(nextStep, selection.size() == 1 ? selectedValue : null, e);
    }

    private void valuesSelected(List<Object> values) {
        if (shouldUseStatistics()) {
            String filter = getSpeedSearch().getFilter();
            if (!StringUtil.isEmpty(filter)) {
                for (Object value : values) {
                    String text = getListStep().getTextFor(value);
                    StatisticsManager.getInstance().incUseCount(new StatisticsInfo("#list_popup:" + getListStep().getTitle() + "#" + filter, text));
                }
            }
        }
    }

    private void disposePopup(@Nullable InputEvent e) {
        if (myStep.getFinalRunnable() != null) {
            setFinalRunnable(myStep.getFinalRunnable());
        }
        setOk(true);
        disposeAllParents(e);
        setIndexForShowingChild(-1);
    }

    @Override
    public void handleNextStep(PopupStep nextStep, Object parentValue) {
        handleNextStep(nextStep, parentValue, null);
    }

    public boolean handleNextStep(PopupStep nextStep, Object parentValue, InputEvent e) {
        if (nextStep != PopupStep.FINAL_CHOICE) {
            Point point = myList.indexToLocation(myList.getSelectedIndex());
            SwingUtilities.convertPointToScreen(point, myList);
            myChild = createPopup(this, nextStep, parentValue);
            if (myChild instanceof ListPopupImpl) {
                for (ListSelectionListener listener : myList.getListSelectionListeners()) {
                    ((ListPopupImpl) myChild).addListSelectionListener(listener);
                }
            }
            JComponent container = getContent();

            myChild.show(container, container.getLocationOnScreen().x + container.getWidth() - STEP_X_PADDING, point.y, true);
            setIndexForShowingChild(myList.getSelectedIndex());
            return false;
        }
        else {
            setOk(true);
            setFinalRunnable(myStep.getFinalRunnable());
            disposeAllParents(e);
            setIndexForShowingChild(-1);
            return true;
        }
    }

    @Override
    public void addListSelectionListener(ListSelectionListener listSelectionListener) {
        myList.addListSelectionListener(listSelectionListener);
    }

    @Override
    public void addSelectionListener(Consumer<Object> selectionListener) {
        myList.addListSelectionListener(e -> selectionListener.accept(myList.getSelectedValue()));
    }

    private enum ExtendMode {
        NO_EXTEND,
        EXTEND_ON_HOVER
    }

    private class MyMouseMotionListener extends MouseMotionAdapter {
        private int myLastSelectedIndex = -2;
        private ExtendMode myExtendMode = ExtendMode.NO_EXTEND;
        private Point myLastMouseLocation;
        private Timer myShowSubmenuTimer;

        private boolean isMouseMoved(Point location) {
            if (myLastMouseLocation == null) {
                myLastMouseLocation = location;
                return false;
            }
            return !myLastMouseLocation.equals(location);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (!isMouseMoved(e.getLocationOnScreen())) {
                return;
            }
            Point point = e.getPoint();
            int index = myList.locationToIndex(point);
            if (isSelectableAt(index)) {
                if (index != myLastSelectedIndex && !isMovingToSubmenu(e)) {
                    myExtendMode = calcExtendMode(index);
                    if (!isMultiSelectionEnabled() || !UIUtil.isSelectionButtonDown(e) && myList.getSelectedIndices().length <= 1) {
                        myList.setSelectedIndex(index);
                        if (myShowSubmenuOnHover) {
                            disposeChildren();
                        }
                        if (myExtendMode == ExtendMode.EXTEND_ON_HOVER) {
                            showSubMenu(index, true);
                        }
                    }
                    restartTimer();
                    myLastSelectedIndex = index;
                }
            }
            else {
                myList.clearSelection();
                myLastSelectedIndex = -1;
            }
            if (updateSelectedButtonIndex(point)) {
                getContent().repaint();
            }

            notifyParentOnChildSelection();
        }

        @SuppressWarnings("unchecked")
        private void showSubMenu(int forIndex, boolean withTimer) {
            if (getIndexForShowingChild() == forIndex) {
                return;
            }

            disposeChildren();

            if (myShowSubmenuTimer != null && myShowSubmenuTimer.isRunning()) {
                myShowSubmenuTimer.stop();
                myShowSubmenuTimer = null;
            }

            ListPopupStep<Object> listStep = getListStep();
            Object selectedValue = myListModel.getElementAt(forIndex);
            if (withTimer) {
                ClientId currentClientId = ClientId.getCurrent();
                myShowSubmenuTimer = new Timer(250, e -> {
                    try (AccessToken ignore = ClientId.withClientId(currentClientId)) {
                        if (!isDisposed() && myLastSelectedIndex == forIndex) {
                            disposeChildren();
                            showNextStepPopup(listStep.onChosen(selectedValue, false), selectedValue);
                        }
                    }
                });
                myShowSubmenuTimer.setRepeats(false);
                myShowSubmenuTimer.start();
            }
            else {
                showNextStepPopup(listStep.onChosen(selectedValue, false), selectedValue);
            }
        }

        @SuppressWarnings("unchecked")
        private ExtendMode calcExtendMode(int index) {
            ListPopupStep<Object> listStep = getListStep();
            Object selectedValue = myListModel.getElementAt(index);
            if (selectedValue == null || !listStep.hasSubstep(selectedValue)) {
                return ExtendMode.NO_EXTEND;
            }

            return myShowSubmenuOnHover ? ExtendMode.EXTEND_ON_HOVER : ExtendMode.NO_EXTEND;
        }

        boolean updateSelectedButtonIndex(Point point) {
            int index = myList.locationToIndex(point);
            if (index < 0 || !isSelectableAt(index)) {
                return myList.setSelectedButtonIndex(null);
            }
            else {
                Object element = myListModel.getElementAt(index);
                if (element != null && myInlineActionsSupport.get().hasExtraButtons(element)) {
                    Integer buttonIndex = myInlineActionsSupport.get().calcButtonIndex(element, point);
                    return myList.setSelectedButtonIndex(buttonIndex);
                }
            }
            return false;
        }
    }

    private boolean isMovingToSubmenu(MouseEvent e) {
        if (myChild == null || myChild.isDisposed()) {
            return false;
        }

        JComponent childContent = myChild.getContent();
        if (childContent == null || !childContent.isShowing()) {
            return false;
        }

        Rectangle childBounds = myChild.getBounds();
        childBounds.setLocation(myChild.getLocationOnScreen());

        return myMouseMovementTracker.isMovingTowards(e, childBounds);
    }

    public void showNextStepPopup(PopupStep nextStep, Object parentValue) {
        if (nextStep == null) {
            String valueText = getListStep().getTextFor(parentValue);
            String message = String.format("Cannot open submenu for '%s' item. PopupStep is null", valueText);
            LOG.warn(message);
            return;
        }

        if ((PopupStep<?>) myStep instanceof ActionPopupStep o &&
            nextStep instanceof ActionPopupStep oo) {
            oo.setSubStepContextAdjuster(o.getSubStepContextAdjuster());
        }

        Point point = myList.indexToLocation(myList.getSelectedIndex());
        SwingUtilities.convertPointToScreen(point, myList);
        myChild = createPopup(this, nextStep, parentValue);
        if (ScreenReader.isActive()) {
            myChild.setRequestFocus(true);
        }
        if (myChild instanceof ListPopup child) {
            for (ListSelectionListener listener : myList.getListSelectionListeners()) {
                child.addListSelectionListener(listener);
            }
            child.setShowSubmenuOnHover(myShowSubmenuOnHover);
            child.setExecuteExpandedItemOnClick(myExecuteExpandedItemOnClick);
        }
        JComponent container = getContent();

        int y = point.y;
        if (parentValue != null && getListModel().isSeparatorAboveOf(parentValue)) {
            SeparatorWithText swt = new SeparatorWithText();
            swt.setCaption(getListModel().getCaptionAboveOf(parentValue));
            y += swt.getPreferredSize().height - 1;
        }

        if (!UiInterceptors.tryIntercept(myChild)) {
            // Intercept child popup in tests because it is impossible to calculate location on screen there
            myChild.show(container, container.getLocationOnScreen().x + container.getWidth() - STEP_X_PADDING, y, true);
        }
        setIndexForShowingChild(myList.getSelectedIndex());
        myMouseMovementTracker.reset();
    }

    protected boolean isActionClick(MouseEvent e) {
        return UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED, true);
    }

    @Override
    public Object[] getSelectedValues() {
        return myList.getSelectedValues();
    }

    private class MyMouseListener extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!isActionClick(e) || isMultiSelectionEnabled() && UIUtil.isSelectionButtonDown(e)) {
                return;
            }
            IdeEventQueueProxy.getInstance().blockNextEvents(e); // sometimes, after popup close, MOUSE_RELEASE event delivers to other components

            Object selectedValue = myList.getSelectedValue();

            ListPopupStep<Object> listStep = getListStep();

            handleSelect(handleFinalChoices(e, selectedValue, listStep), e);

            stopTimer();
        }
    }

    protected boolean handleFinalChoices(MouseEvent e, Object selectedValue, ListPopupStep<Object> listStep) {
        return selectedValue == null || !listStep.hasSubstep(selectedValue) || !listStep.isSelectable(selectedValue) || !isOnNextStepButton(e);
    }

    private boolean isOnNextStepButton(MouseEvent e) {
        int index = myList.getSelectedIndex();
        Rectangle bounds = myList.getCellBounds(index, index);
        if (bounds != null) {
            JBInsets.removeFrom(bounds, UIUtil.getListCellPadding());
        }
        Point point = e.getPoint();
        return bounds != null && point.getX() > bounds.width + bounds.getX() - UIUtil.getMenuArrowIcon(false).getIconWidth();
    }

    @Override
    protected void process(KeyEvent aEvent) {
        myList.processKeyEvent(aEvent);
    }

    @Override
    public void setShowSubmenuOnHover(boolean showSubmenuOnHover) {
        myShowSubmenuOnHover = showSubmenuOnHover;
    }

    @Override
    public void setExecuteExpandedItemOnClick(boolean executeExpandedItemOnClick) {
        myExecuteExpandedItemOnClick = executeExpandedItemOnClick;
    }

    private int getIndexForShowingChild() {
        return myIndexForShowingChild;
    }

    private void setIndexForShowingChild(int aIndexForShowingChild) {
        myIndexForShowingChild = aIndexForShowingChild;
    }

    private class MyList extends JBList implements UiDataProvider, ListWithInlineButtons {
        private @Nullable Integer selectedButtonIndex;

        MyList() {
            super(myListModel);
            HintUpdateSupply.installSimpleHintUpdateSupply(this);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            int visibleRows = Math.min(myMaxRowCount, getVisibleRowCount());
            if (getModel().getSize() <= visibleRows) {
                return getPreferredSize();
            }

            Dimension size = super.getPreferredScrollableViewportSize();
            Rectangle cellBounds = getCellBounds(0, visibleRows - 1);
            if (cellBounds != null) {
                Insets insets = getInsets();
                size.height = cellBounds.height + insets.top + insets.bottom;
            }
            return size;
        }

        @Override
        public void setSelectedIndex(int index) {
            Object o = getListModel().get(index);
            if (o != null && getListModel().isSeparator(o)) {
                return;
            }
            super.setSelectedIndex(index);
        }

        @Override
        public void processKeyEvent(KeyEvent e) {
            e.setSource(this);
            super.processKeyEvent(e);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (!isMultiSelectionEnabled() && (e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0) {
                // do not toggle selection with ctrl+click event in single-selection mode
                e.consume();
            }
            if (UIUtil.isActionClick(e, MouseEvent.MOUSE_PRESSED) && isOnNextStepButton(e)) {
                e.consume();
            }

            boolean isClick = UIUtil.isActionClick(e, MouseEvent.MOUSE_PRESSED) || UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED);
            if (!isClick || myList.locationToIndex(e.getPoint()) == myList.getSelectedIndex()) {
                super.processMouseEvent(e);
            }
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            sink.set(PlatformDataKeys.SELECTED_ITEM, myList.getSelectedValue());
            sink.set(PlatformDataKeys.SELECTED_ITEMS, myList.getSelectedValues());
            if (mySpeedSearchPatternField != null && mySpeedSearchPatternField.isVisible()) {
                sink.set(PlatformDataKeys.SPEED_SEARCH_COMPONENT, mySpeedSearchPatternField);
            }
        }

        @Override
        public @Nullable Integer getSelectedButtonIndex() {
            return selectedButtonIndex;
        }

        private boolean setSelectedButtonIndex(@Nullable Integer index) {
            if (Objects.compare(index, selectedButtonIndex, Comparator.nullsFirst(Integer::compare)) == 0) {
                return false;
            }

            selectedButtonIndex = index;
            return true;
        }
    }

    private class MyListSelectionModel extends DefaultListSelectionModel {
        private MyListSelectionModel() {
            setSelectionMode(isMultiSelectionEnabled() ? MULTIPLE_INTERVAL_SELECTION : SINGLE_SELECTION);
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (getSelectionMode() == SINGLE_SELECTION) {
                if (index0 > getLeadSelectionIndex()) {
                    for (int i = index0; i < myListModel.getSize(); i++) {
                        if (getListStep().isSelectable(myListModel.getElementAt(i))) {
                            super.setSelectionInterval(i, i);
                            break;
                        }
                    }
                }
                else {
                    for (int i = index0; i >= 0; i--) {
                        if (getListStep().isSelectable(myListModel.getElementAt(i))) {
                            super.setSelectionInterval(i, i);
                            break;
                        }
                    }
                }
            }
            else {
                super.setSelectionInterval(index0, index1); // TODO: support when needed
            }
        }
    }

    private void disposeAsyncPopupWaiter() {
        // FIXME [VISTALL] unsupported
    }

    @Override
    protected void onSpeedSearchPatternChanged() {
        myListModel.refilter();
        if (myListModel.getSize() > 0) {
            if (!(shouldUseStatistics() && autoSelectUsingStatistics())) {
                selectBestMatch();
            }
        }
    }

    private void selectBestMatch() {
        int fullMatchIndex = myListModel.getClosestMatchIndex();
        if (fullMatchIndex != -1 && isSelectableAt(fullMatchIndex)) {
            myList.setSelectedIndex(fullMatchIndex);
        }

        if (myListModel.getSize() <= myList.getSelectedIndex() || !myListModel.isVisible(myList.getSelectedValue())) {
            selectFirstSelectableItem();
        }
    }

    @Override
    protected void onSelectByMnemonic(Object value) {
        if (myListModel.isVisible(value) && isSelectable(value)) {
            myList.setSelectedValue(value, true);
            myList.repaint();
            handleSelect(true);
        }
    }

    @Override
    protected JComponent getPreferredFocusableComponent() {
        return myList;
    }

    @Override
    public void onChildSelectedFor(Object value) {
        if (myList.getSelectedValue() != value && isSelectable(value)) {
            myList.setSelectedValue(value, false);
        }
    }

    @Override
    public void setHandleAutoSelectionBeforeShow(boolean autoHandle) {
        myAutoHandleBeforeShow = autoHandle;
    }

    @Override
    public boolean isModalContext() {
        return true;
    }

    private void replacePasteAction() {
        if (myStep.isSpeedSearchEnabled()) {
            getList().getActionMap().put(TransferHandler.getPasteAction().getValue(Action.NAME), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    getSpeedSearch().type(CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor));
                    getSpeedSearch().update();
                }
            });
        }
    }

    private boolean isSelectable(@Nullable Object value) {
        return value != null && getListStep().isSelectable(value);
    }

    private @Nullable Object getSelectableAt(int index) {
        if (0 <= index && index < myListModel.getSize()) {
            Object value = myListModel.getElementAt(index);
            if (isSelectable(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isSelectableAt(int index) {
        return null != getSelectableAt(index);
    }
}
