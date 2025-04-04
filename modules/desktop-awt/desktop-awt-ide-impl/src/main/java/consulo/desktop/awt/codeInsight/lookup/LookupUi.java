// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.codeInsight.lookup;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.UISettings;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposer;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionFeatures;
import consulo.ide.impl.idea.codeInsight.completion.ShowHideIntentionIconLookupAction;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.lookup.impl.CompletionExtender;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementAction;
import consulo.language.editor.inject.EditorWindow;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author peter
 */
class LookupUi {
    private static final Logger LOG = Logger.getInstance(LookupUi.class);

    @Nonnull
    private final LookupImpl myLookup;
    private final Advertiser myAdvertiser;
    private final JBList myList;
    private final ModalityState myModalityState;
    private final Alarm myHintAlarm = new Alarm();
    private final JScrollPane myScrollPane;
    private final AsyncProcessIcon myProcessIcon = new AsyncProcessIcon("Completion progress");
    private final JComponent myBottomPanel;

    private int myMaximumHeight = Integer.MAX_VALUE;
    private Boolean myPositionedAbove = null;

    private AtomicBoolean myHintCalculating = new AtomicBoolean(true);

    LookupUi(@Nonnull LookupImpl lookup, Advertiser advertiser, JBList list) {
        myLookup = lookup;
        myAdvertiser = advertiser;
        myList = list;

        myProcessIcon.setVisible(false);
        myLookup.resort(false);

        final ActionManager actionManager = ActionManager.getInstance();

        MoreActionGroup moreActionGroup = new MoreActionGroup();
        moreActionGroup.add(new HintAction());
        moreActionGroup.add(new ChangeSortingAction());
        moreActionGroup.add(new DelegatedAction(actionManager.getAction(IdeActions.ACTION_QUICK_JAVADOC)) {
            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setVisible(!CodeInsightSettings.getInstance().AUTO_POPUP_JAVADOC_INFO);
            }
        });
        moreActionGroup.add(new DelegatedAction(actionManager.getAction(IdeActions.ACTION_QUICK_IMPLEMENTATIONS)));

        ActionGroup toolbarGroup = ActionGroup.newImmutableBuilder().add(moreActionGroup).build();
        ActionToolbar lookupUIToolbar = actionManager.createActionToolbar("LookupUIToolbar", toolbarGroup, true);
        lookupUIToolbar.setMiniMode(true);
        lookupUIToolbar.setTargetComponent(list);

        myBottomPanel = new NonOpaquePanel(new BorderLayout());
        myBottomPanel.add(myAdvertiser.getAdComponent(), BorderLayout.WEST);

        JPanel rightPanel = new NonOpaquePanel(new HorizontalLayout(4, SwingConstants.CENTER));
        rightPanel.add(myProcessIcon);
        JComponent toolbarComponent = lookupUIToolbar.getComponent();
        toolbarComponent.setBorder(JBUI.Borders.empty());
        rightPanel.add(toolbarComponent);

        myBottomPanel.add(rightPanel, BorderLayout.EAST);

        LookupLayeredPane layeredPane = new LookupLayeredPane();
        layeredPane.mainPanel.add(myBottomPanel, BorderLayout.SOUTH);

        myScrollPane = ScrollPaneFactory.createScrollPane(lookup.getList(), true);
        myScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        UIUtil.putClientProperty(myScrollPane.getVerticalScrollBar(), JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, true);

        lookup.getComponent().add(layeredPane, BorderLayout.CENTER);

        layeredPane.mainPanel.add(myScrollPane, BorderLayout.CENTER);

        myModalityState = IdeaModalityState.stateForComponent(lookup.getTopLevelEditor().getComponent());

        addListeners();

        Disposer.register(lookup, myProcessIcon);
        Disposer.register(lookup, myHintAlarm);
    }

    private void addListeners() {
        myList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (myLookup.isLookupDisposed()) {
                    return;
                }

                myHintAlarm.cancelAllRequests();
                updateHint();
            }
        });
    }

    private void updateHint() {
        myLookup.checkValid();

        myHintCalculating.set(true);

        LookupElement item = myLookup.getCurrentItem();
        if (item != null && item.isValid()) {
            Collection<LookupElementAction> actions = myLookup.getActionsFor(item);
            if (!actions.isEmpty()) {
                myHintAlarm.addRequest(() -> {
                    if (ShowHideIntentionIconLookupAction.shouldShowLookupHint() && !((CompletionExtender) myList.getExpandableItemsHandler()).isShowing() && !myProcessIcon.isVisible()) {
                        myHintCalculating.set(false);
                    }
                }, 500, myModalityState);
            }
        }
    }

    void setCalculating(boolean calculating) {
        Runnable iconUpdater = () -> {
            if (calculating) {
                myHintCalculating.set(true);
            }
            myProcessIcon.setVisible(calculating);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (!calculating && !myLookup.isLookupDisposed()) {
                    updateHint();
                }
            }, myModalityState);
        };

        if (calculating) {
            myProcessIcon.resume();
        }
        else {
            myProcessIcon.suspend();
        }
        new Alarm(myLookup).addRequest(iconUpdater, 100, myModalityState);
    }

    void refreshUi(boolean selectionVisible, boolean itemsChanged, boolean reused, boolean onExplicitAction) {
        Editor editor = myLookup.getTopLevelEditor();
        if (editor.getComponent().getRootPane() == null || editor instanceof EditorWindow && !((EditorWindow) editor).isValid()) {
            return;
        }

        if (myLookup.myResizePending || itemsChanged) {
            myMaximumHeight = Integer.MAX_VALUE;
        }
        Rectangle rectangle = calculatePosition();
        myMaximumHeight = rectangle.height;

        if (myLookup.myResizePending || itemsChanged) {
            myLookup.myResizePending = false;
            myLookup.pack();
        }
        HintManagerImpl.getInstanceImpl().updateLocation(myLookup, editor, rectangle.getLocation());

        if (reused || selectionVisible || onExplicitAction) {
            myLookup.ensureSelectionVisible(false);
        }
    }

    boolean isPositionedAboveCaret() {
        return myPositionedAbove != null && myPositionedAbove.booleanValue();
    }

    // in layered pane coordinate system.
    Rectangle calculatePosition() {
        final JComponent lookupComponent = myLookup.getComponent();
        Dimension dim = lookupComponent.getPreferredSize();
        int lookupStart = myLookup.getLookupStart();
        Editor editor = myLookup.getTopLevelEditor();
        if (lookupStart < 0 || lookupStart > editor.getDocument().getTextLength()) {
            LOG.error(lookupStart + "; offset=" + editor.getCaretModel().getOffset() + "; element=" + myLookup.getPsiElement());
        }

        LogicalPosition pos = editor.offsetToLogicalPosition(lookupStart);
        Point location = editor.logicalPositionToXY(pos);
        location.y += editor.getLineHeight();
        location.x -= myLookup.myCellRenderer.getTextIndent();
        // extra check for other borders
        final Window window = UIUtil.getWindow(lookupComponent);
        if (window != null) {
            final Point point = SwingUtilities.convertPoint(lookupComponent, 0, 0, window);
            location.x -= point.x;
        }

        SwingUtilities.convertPointToScreen(location, editor.getContentComponent());
        final Rectangle screenRectangle = ScreenUtil.getScreenRectangle(editor.getContentComponent());

        if (!isPositionedAboveCaret()) {
            int shiftLow = screenRectangle.y + screenRectangle.height - (location.y + dim.height);
            myPositionedAbove = shiftLow < 0 && shiftLow < location.y - dim.height && location.y >= dim.height;
        }
        if (isPositionedAboveCaret()) {
            location.y -= dim.height + editor.getLineHeight();
            if (pos.line == 0) {
                location.y += 1;
                //otherwise the lookup won't intersect with the editor and every editor's resize (e.g. after typing in console) will close the lookup
            }
        }

        if (!screenRectangle.contains(location)) {
            location = ScreenUtil.findNearestPointOnBorder(screenRectangle, location);
        }

        Rectangle candidate = new Rectangle(location, dim);
        ScreenUtil.cropRectangleToFitTheScreen(candidate);

        JRootPane rootPane = editor.getComponent().getRootPane();
        if (rootPane != null) {
            SwingUtilities.convertPointFromScreen(location, rootPane.getLayeredPane());
        }
        else {
            LOG.error("editor.disposed=" + editor.isDisposed() + "; lookup.disposed=" + myLookup.isLookupDisposed() + "; editorShowing=" + editor.getContentComponent().isShowing());
        }

        myMaximumHeight = candidate.height;
        return new Rectangle(location.x, location.y, dim.width, candidate.height);
    }

    private class LookupLayeredPane extends JBLayeredPane {
        final JPanel mainPanel = new JPanel(new BorderLayout());

        private LookupLayeredPane() {
            mainPanel.setBackground(LookupCellRenderer.BACKGROUND_COLOR);
            add(mainPanel, 0, 0);

            setLayout(new AbstractLayoutManager() {
                @Override
                public Dimension preferredLayoutSize(@Nullable Container parent) {
                    int maxCellWidth = myLookup.myCellRenderer.getLookupTextWidth() + myLookup.myCellRenderer.getTextIndent();
                    int scrollBarWidth = myScrollPane.getVerticalScrollBar().getWidth();
                    int listWidth = Math.min(scrollBarWidth + maxCellWidth, UISettings.getInstance().getMaxLookupWidth());

                    Dimension bottomPanelSize = myBottomPanel.getPreferredSize();

                    int panelHeight = myScrollPane.getPreferredSize().height + bottomPanelSize.height;
                    int width = Math.max(listWidth, bottomPanelSize.width);
                    width = Math.min(width, Registry.intValue("ide.completion.max.width"));
                    int height = Math.min(panelHeight, myMaximumHeight);

                    return new Dimension(width, height);
                }

                @Override
                public void layoutContainer(Container parent) {
                    Dimension size = getSize();
                    mainPanel.setSize(size);
                    mainPanel.validate();

                    if (IdeEventQueue.getInstance().getTrueCurrentEvent().getID() == MouseEvent.MOUSE_DRAGGED) {
                        Dimension preferredSize = preferredLayoutSize(null);
                        if (preferredSize.width != size.width) {
                            UISettings.getInstance().setMaxLookupWidth(Math.max(500, size.width));
                        }

                        int listHeight = myList.getLastVisibleIndex() - myList.getFirstVisibleIndex() + 1;
                        if (listHeight != myList.getModel().getSize() && listHeight != myList.getVisibleRowCount() && preferredSize.height != size.height) {
                            UISettings.getInstance().setMaxLookupListHeight(Math.max(5, listHeight));
                        }
                    }

                    myList.setFixedCellWidth(myScrollPane.getViewport().getWidth());
                }
            });
        }
    }

    private class HintAction extends DumbAwareAction {
        private HintAction() {
            super(LocalizeValue.empty(), LocalizeValue.empty(), AllIcons.Actions.IntentionBulb);

            AnAction showIntentionAction = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
            if (showIntentionAction != null) {
                copyShortcutFrom(showIntentionAction);
                getTemplatePresentation().setText("Click or Press");
            }
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setVisible(!myHintCalculating.get());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myLookup.showElementActions(e.getInputEvent());
        }
    }

    private static class MoreActionGroup extends ActionGroup implements DumbAware, HintManagerImpl.ActionToIgnore {
        private List<AnAction> myActions = new ArrayList<>();

        private MoreActionGroup() {
            setPopup(true);
        }

        public void add(AnAction action) {
            myActions.add(action);
        }

        @Nonnull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return myActions.toArray(AnAction[]::new);
        }

        @Nullable
        @Override
        protected Image getTemplateIcon() {
            return PlatformIconGroup.actionsMorevertical();
        }

        @Override
        public boolean showBelowArrow() {
            return false;
        }
    }

    private class ChangeSortingAction extends DumbAwareAction implements HintManagerImpl.ActionToIgnore {
        private boolean sortByName = UISettings.getInstance().getSortLookupElementsLexicographically();

        private ChangeSortingAction() {
            super("Sort by Name");
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (e.getPlace() == ActionPlaces.EDITOR_POPUP) {
                sortByName = !sortByName;

                FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_CHANGE_SORTING);
                UISettings.getInstance().setSortLookupElementsLexicographically(sortByName);
                myLookup.resort(false);
            }
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setIcon(sortByName ? AllIcons.Actions.Checked : null);
        }
    }

    private static class DelegatedAction extends DumbAwareAction implements HintManagerImpl.ActionToIgnore {
        private final AnAction delegateAction;

        private DelegatedAction(AnAction action) {
            delegateAction = action;
            getTemplatePresentation().setText(delegateAction.getTemplateText(), true);
            copyShortcutFrom(delegateAction);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (e.getPlace() == ActionPlaces.EDITOR_POPUP) {
                delegateAction.actionPerformed(e);
            }
        }
    }
}
