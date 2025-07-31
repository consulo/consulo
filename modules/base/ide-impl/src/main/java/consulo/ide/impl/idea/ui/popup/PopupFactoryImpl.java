// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.VisualPosition;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ide.impl.idea.ui.popup.mock.MockConfirmation;
import consulo.ide.impl.idea.ui.popup.tree.TreePopupImpl;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.NotificationType;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.popup.AWTPopupFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class PopupFactoryImpl extends JBPopupFactory implements AWTPopupFactory {
    private static final Logger LOG = Logger.getInstance(PopupFactoryImpl.class);

    @Nonnull
    @Override
    public ListPopup createConfirmation(String title, Runnable onYes, int defaultOptionIndex) {
        return createConfirmation(title, CommonLocalize.buttonYes().get(), CommonLocalize.buttonNo().get(), onYes, defaultOptionIndex);
    }

    @Nonnull
    @Override
    public ListPopup createConfirmation(String title, String yesText, String noText, Runnable onYes, int defaultOptionIndex) {
        return createConfirmation(title, yesText, noText, onYes, EmptyRunnable.getInstance(), defaultOptionIndex);
    }

    @Nonnull
    @Override
    public JBPopup createMessage(String text) {
        return createListPopup(new BaseListPopupStep<>(null, text));
    }

    @Override
    public Balloon getParentBalloonFor(@Nullable Component c) {
        if (c == null) {
            return null;
        }
        Component eachParent = c;
        while (eachParent != null) {
            if (eachParent instanceof JComponent) {
                Object balloon = ((JComponent)eachParent).getClientProperty(Balloon.KEY);
                if (balloon instanceof Balloon) {
                    return (Balloon)balloon;
                }
            }
            eachParent = eachParent.getParent();
        }

        return null;
    }

    @Nonnull
    @Override
    public ListPopup createConfirmation(
        String title,
        String yesText,
        String noText,
        Runnable onYes,
        Runnable onNo,
        int defaultOptionIndex
    ) {
        BaseListPopupStep<String> step = new BaseListPopupStep<String>(title, yesText, noText) {
            @Override
            public PopupStep onChosen(String selectedValue, boolean finalChoice) {
                return doFinalStep(selectedValue.equals(yesText) ? onYes : onNo);
            }

            @Override
            public void canceled() {
                onNo.run();
            }

            @Override
            public boolean isMnemonicsNavigationEnabled() {
                return true;
            }
        };
        step.setDefaultOptionIndex(defaultOptionIndex);

        Application app = ApplicationManager.getApplication();
        return app == null || !app.isUnitTestMode() ? new ListPopupImpl(step) : new MockConfirmation(step, yesText);
    }

    public static class ActionGroupPopup extends ListPopupImpl {
        private final Runnable myDisposeCallback;
        private final Component myComponent;
        private final String myActionPlace;

        public ActionGroupPopup(
            String title,
            @Nonnull ActionGroup actionGroup,
            @Nonnull DataContext dataContext,
            boolean showNumbers,
            boolean useAlphaAsNumbers,
            boolean showDisabledActions,
            boolean honorActionMnemonics,
            Runnable disposeCallback,
            int maxRowCount,
            Predicate<? super AnAction> preselectActionCondition,
            @Nullable String actionPlace
        ) {
            this(
                title,
                actionGroup,
                dataContext,
                showNumbers,
                useAlphaAsNumbers,
                showDisabledActions,
                honorActionMnemonics,
                disposeCallback,
                maxRowCount,
                preselectActionCondition,
                actionPlace,
                null,
                false
            );
        }

        public ActionGroupPopup(
            String title,
            @Nonnull ActionGroup actionGroup,
            @Nonnull DataContext dataContext,
            boolean showNumbers,
            boolean useAlphaAsNumbers,
            boolean showDisabledActions,
            boolean honorActionMnemonics,
            Runnable disposeCallback,
            int maxRowCount,
            Predicate<? super AnAction> preselectActionCondition,
            @Nullable String actionPlace,
            boolean autoSelection
        ) {
            this(
                title,
                actionGroup,
                dataContext,
                showNumbers,
                useAlphaAsNumbers,
                showDisabledActions,
                honorActionMnemonics,
                disposeCallback,
                maxRowCount,
                preselectActionCondition,
                actionPlace,
                null,
                autoSelection
            );
        }

        public ActionGroupPopup(
            String title,
            @Nonnull ActionGroup actionGroup,
            @Nonnull DataContext dataContext,
            boolean showNumbers,
            boolean useAlphaAsNumbers,
            boolean showDisabledActions,
            boolean honorActionMnemonics,
            Runnable disposeCallback,
            int maxRowCount,
            Predicate<? super AnAction> preselectActionCondition,
            @Nullable String actionPlace,
            @Nullable BasePresentationFactory presentationFactory,
            boolean autoSelection
        ) {
            this(
                null,
                createStep(
                    title,
                    actionGroup,
                    dataContext,
                    showNumbers,
                    useAlphaAsNumbers,
                    showDisabledActions,
                    honorActionMnemonics,
                    preselectActionCondition,
                    actionPlace,
                    presentationFactory,
                    autoSelection
                ),
                disposeCallback,
                dataContext,
                actionPlace,
                maxRowCount
            );
        }

        protected ActionGroupPopup(
            @Nullable WizardPopup aParent,
            @Nonnull ListPopupStep step,
            @Nullable Runnable disposeCallback,
            @Nonnull DataContext dataContext,
            @Nullable String actionPlace,
            int maxRowCount
        ) {
            super(dataContext.getData(Project.KEY), aParent, step, null);
            setMaxRowCount(maxRowCount);
            myDisposeCallback = disposeCallback;
            myComponent = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
            myActionPlace = actionPlace == null ? ActionPlaces.UNKNOWN : actionPlace;

            registerAction("handleActionToggle1", KeyEvent.VK_SPACE, 0, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleToggleAction();
                }
            });

            addListSelectionListener(e -> {
                JList list = (JList)e.getSource();
                ActionItem actionItem = (ActionItem)list.getSelectedValue();
                if (actionItem == null) {
                    return;
                }
                updateActionItem(actionItem);
            });
        }

        @Nonnull
        private Presentation updateActionItem(@Nonnull ActionItem actionItem) {
            AnAction action = actionItem.getAction();
            Presentation presentation = new Presentation();
            presentation.setDescription(action.getTemplatePresentation().getDescription());

            AnActionEvent actionEvent = new AnActionEvent(
                null,
                DataManager.getInstance().getDataContext(myComponent),
                myActionPlace,
                presentation,
                ActionManager.getInstance(),
                0
            );
            actionEvent.setInjectedContext(action.isInInjectedContext());
            ActionImplUtil.performDumbAwareUpdate(action, actionEvent, false);
            return presentation;
        }

        private static ListPopupStep<ActionItem> createStep(
            String title,
            @Nonnull ActionGroup actionGroup,
            @Nonnull DataContext dataContext,
            boolean showNumbers,
            boolean useAlphaAsNumbers,
            boolean showDisabledActions,
            boolean honorActionMnemonics,
            Predicate<? super AnAction> preselectActionCondition,
            @Nullable String actionPlace,
            @Nullable BasePresentationFactory presentationFactory,
            boolean autoSelection
        ) {
            Component component = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
            consulo.ui.Component uiCompoment = dataContext.getData(PlatformDataKeys.CONTEXT_UI_COMPONENT);
            if (component == null && uiCompoment != null) {
                component = TargetAWT.to(uiCompoment);
            }

            LOG.assertTrue(component != null, "dataContext has no component for new ListPopupStep");

            List<ActionItem> items = ActionPopupStep.createActionItems(
                actionGroup,
                dataContext,
                showNumbers,
                useAlphaAsNumbers,
                showDisabledActions,
                honorActionMnemonics,
                actionPlace,
                presentationFactory
            );

            return new ActionPopupStep(
                items,
                title,
                getComponentContextSupplier(component),
                actionPlace,
                showNumbers || honorActionMnemonics && itemsHaveMnemonics(items),
                preselectActionCondition,
                autoSelection,
                showDisabledActions,
                presentationFactory
            );
        }

        /**
         * @deprecated Use {@link ActionPopupStep#createActionItems(ActionGroup, DataContext, boolean, boolean, boolean, boolean, String, BasePresentationFactory)} instead.
         */
        @Deprecated
        @Nonnull
        public static List<ActionItem> getActionItems(
            @Nonnull ActionGroup actionGroup,
            @Nonnull DataContext dataContext,
            boolean showNumbers,
            boolean useAlphaAsNumbers,
            boolean showDisabledActions,
            boolean honorActionMnemonics,
            @Nullable String actionPlace
        ) {
            return ActionPopupStep.createActionItems(
                actionGroup,
                dataContext,
                showNumbers,
                useAlphaAsNumbers,
                showDisabledActions,
                honorActionMnemonics,
                actionPlace,
                null
            );
        }

        @Override
        public void dispose() {
            if (myDisposeCallback != null) {
                myDisposeCallback.run();
            }
            super.dispose();
        }

        @Override
        public void handleSelect(boolean handleFinalChoices, InputEvent e) {
            Object selectedValue = getList().getSelectedValue();
            ActionPopupStep actionPopupStep = ObjectUtil.tryCast(getListStep(), ActionPopupStep.class);

            if (actionPopupStep != null) {
                KeepingPopupOpenAction dontClosePopupAction =
                    getActionByClass(selectedValue, actionPopupStep, KeepingPopupOpenAction.class);
                if (dontClosePopupAction != null) {
                    actionPopupStep.performAction((AnAction)dontClosePopupAction, e != null ? e.getModifiers() : 0, e);
                    for (ActionItem item : actionPopupStep.getValues()) {
                        updateActionItem(item);
                    }
                    getList().repaint();
                    return;
                }
            }

            super.handleSelect(handleFinalChoices, e);
        }

        protected void handleToggleAction() {
            Object[] selectedValues = getList().getSelectedValues();

            ListPopupStep<Object> listStep = getListStep();
            ActionPopupStep actionPopupStep = ObjectUtil.tryCast(listStep, ActionPopupStep.class);
            if (actionPopupStep == null) {
                return;
            }

            List<ToggleAction> filtered =
                ContainerUtil.mapNotNull(selectedValues, o -> getActionByClass(o, actionPopupStep, ToggleAction.class));

            for (ToggleAction action : filtered) {
                actionPopupStep.performAction(action, 0);
            }

            for (ActionItem item : actionPopupStep.getValues()) {
                updateActionItem(item);
            }

            getList().repaint();
        }

        @Nullable
        private static <T> T getActionByClass(
            @Nullable Object value,
            @Nonnull ActionPopupStep actionPopupStep,
            @Nonnull Class<T> actionClass
        ) {
            ActionItem item = value instanceof ActionItem ? (ActionItem)value : null;
            if (item == null) {
                return null;
            }
            if (!actionPopupStep.isSelectable(item)) {
                return null;
            }
            return actionClass.isInstance(item.getAction()) ? actionClass.cast(item.getAction()) : null;
        }
    }

    @Nonnull
    private static Supplier<DataContext> getComponentContextSupplier(Component component) {
        return () -> DataManager.getInstance().getDataContext(component);
    }

    @Override
    @Nonnull
    public ListPopup createActionGroupPopup(
        String title,
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        ActionSelectionAid aid,
        boolean showDisabledActions,
        Runnable disposeCallback,
        int maxRowCount,
        Predicate<? super AnAction> preselectActionCondition,
        @Nullable String actionPlace
    ) {
        return new ActionGroupPopup(
            title,
            actionGroup,
            dataContext,
            aid == ActionSelectionAid.ALPHA_NUMBERING || aid == ActionSelectionAid.NUMBERING,
            aid == ActionSelectionAid.ALPHA_NUMBERING,
            showDisabledActions,
            aid == ActionSelectionAid.MNEMONICS,
            disposeCallback,
            maxRowCount,
            preselectActionCondition,
            actionPlace
        );
    }

    @Nonnull
    @Override
    public ListPopup createActionGroupPopup(
        String title,
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        Runnable disposeCallback,
        int maxRowCount,
        Predicate<? super AnAction> preselectActionCondition
    ) {
        return new ActionGroupPopup(
            title,
            actionGroup,
            dataContext,
            showNumbers,
            true,
            showDisabledActions,
            honorActionMnemonics,
            disposeCallback,
            maxRowCount,
            preselectActionCondition,
            null
        );
    }

    @Nonnull
    @Override
    public ListPopupStep<ActionItem> createActionsStep(
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        @Nullable String actionPlace,
        boolean showNumbers,
        boolean showDisabledActions,
        String title,
        Component component,
        boolean honorActionMnemonics,
        int defaultOptionIndex,
        boolean autoSelectionEnabled
    ) {
        return ActionPopupStep.createActionsStep(
            actionGroup,
            dataContext,
            showNumbers,
            true,
            showDisabledActions,
            title,
            honorActionMnemonics,
            autoSelectionEnabled,
            getComponentContextSupplier(component),
            actionPlace,
            null,
            defaultOptionIndex,
            null
        );
    }

    private static boolean itemsHaveMnemonics(List<? extends ActionItem> items) {
        for (ActionItem item : items) {
            if (TextWithMnemonic.parse(item.getAction().getTemplatePresentation().getTextWithMnemonic()).getMnemonic() != 0) {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    @Override
    public ListPopup createListPopup(@Nonnull ListPopupStep step) {
        return new ListPopupImpl(step);
    }

    @Nonnull
    @Override
    public ListPopup createListPopup(@Nonnull ListPopupStep step, int maxRowCount) {
        ListPopupImpl popup = new ListPopupImpl(step);
        popup.setMaxRowCount(maxRowCount);
        return popup;
    }

    @Nonnull
    @Override
    public TreePopup createTree(JBPopup parent, @Nonnull TreePopupStep aStep, Object parentValue) {
        return new TreePopupImpl((Project)aStep.getProject(), parent, aStep, parentValue);
    }

    @Nonnull
    @Override
    public TreePopup createTree(@Nonnull TreePopupStep aStep) {
        return new TreePopupImpl((Project)aStep.getProject(), null, aStep, null);
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder createComponentPopupBuilder(@Nonnull JComponent content, JComponent preferableFocusComponent) {
        return new ComponentPopupBuilderImpl(content, preferableFocusComponent);
    }


    @Nonnull
    @Override
    public RelativePoint guessBestPopupLocation(@Nonnull DataContext dataContext) {
        Component component = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        JComponent focusOwner = component instanceof JComponent ? (JComponent)component : null;

        if (focusOwner == null) {
            Project project = dataContext.getData(Project.KEY);
            JFrame frame = project == null ? null : WindowManager.getInstance().getFrame(project);
            focusOwner = frame == null ? null : frame.getRootPane();
            if (focusOwner == null) {
                throw new IllegalArgumentException("focusOwner cannot be null");
            }
        }

        Point point = dataContext.getData(UIExAWTDataKey.CONTEXT_MENU_POINT);
        if (point != null) {
            return new RelativePoint(focusOwner, point);
        }

        Editor editor = dataContext.getData(Editor.KEY);
        if (editor != null && focusOwner == editor.getContentComponent()) {
            return guessBestPopupLocation(editor);
        }
        return guessBestPopupLocation(focusOwner);
    }

    @Nonnull
    @Override
    public RelativePoint guessBestPopupLocation(@Nonnull JComponent component) {
        Point popupMenuPoint = null;
        Rectangle visibleRect = component.getVisibleRect();
        if (component instanceof JList) { // JList
            JList list = (JList)component;
            int firstVisibleIndex = list.getFirstVisibleIndex();
            int lastVisibleIndex = list.getLastVisibleIndex();
            int[] selectedIndices = list.getSelectedIndices();
            for (int index : selectedIndices) {
                if (firstVisibleIndex <= index && index <= lastVisibleIndex) {
                    Rectangle cellBounds = list.getCellBounds(index, index);
                    popupMenuPoint = new Point(visibleRect.x + visibleRect.width / 4, cellBounds.y + cellBounds.height - 1);
                    break;
                }
            }
        }
        else if (component instanceof JTree) { // JTree
            JTree tree = (JTree)component;
            int[] selectionRows = tree.getSelectionRows();
            if (selectionRows != null) {
                Arrays.sort(selectionRows);
                for (int row : selectionRows) {
                    Rectangle rowBounds = tree.getRowBounds(row);
                    if (visibleRect.contains(rowBounds)) {
                        popupMenuPoint = new Point(rowBounds.x + 2, rowBounds.y + rowBounds.height - 1);
                        break;
                    }
                }
                if (popupMenuPoint == null) {//All selected rows are out of visible rect
                    Point visibleCenter = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
                    double minDistance = Double.POSITIVE_INFINITY;
                    int bestRow = -1;
                    for (int row : selectionRows) {
                        Rectangle rowBounds = tree.getRowBounds(row);
                        Point rowCenter = new Point(rowBounds.x + rowBounds.width / 2, rowBounds.y + rowBounds.height / 2);
                        double distance = visibleCenter.distance(rowCenter);
                        if (minDistance > distance) {
                            minDistance = distance;
                            bestRow = row;
                        }
                    }

                    if (bestRow != -1) {
                        Rectangle rowBounds = tree.getRowBounds(bestRow);
                        tree.scrollRectToVisible(new Rectangle(
                            rowBounds.x,
                            rowBounds.y,
                            Math.min(visibleRect.width, rowBounds.width),
                            rowBounds.height
                        ));
                        popupMenuPoint = new Point(rowBounds.x + 2, rowBounds.y + rowBounds.height - 1);
                    }
                }
            }
        }
        else if (component instanceof JTable) {
            JTable table = (JTable)component;
            int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            int row = Math.max(table.getSelectionModel().getLeadSelectionIndex(), table.getSelectionModel().getAnchorSelectionIndex());
            Rectangle rect = table.getCellRect(row, column, false);
            if (!visibleRect.intersects(rect)) {
                table.scrollRectToVisible(rect);
            }
            popupMenuPoint = new Point(rect.x, rect.y + rect.height - 1);
        }
        else if (component instanceof PopupOwner) {
            popupMenuPoint = ((PopupOwner)component).getBestPopupPosition();
        }
        if (popupMenuPoint == null) {
            popupMenuPoint = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
        }

        return new RelativePoint(component, popupMenuPoint);
    }

    public boolean isBestPopupLocationVisible(@Nonnull Editor editor) {
        return getVisibleBestPopupLocation(editor) != null;
    }

    @Nonnull
    public RelativePoint guessBestPopupLocation(@Nonnull Editor editor) {
        Point p = getVisibleBestPopupLocation(editor);
        if (p == null) {
            Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
            p = new Point(visibleArea.x + visibleArea.width / 3, visibleArea.y + visibleArea.height / 2);
        }
        return new RelativePoint(editor.getContentComponent(), p);
    }

    @Nullable
    private static Point getVisibleBestPopupLocation(@Nonnull Editor editor) {
        VisualPosition visualPosition = editor.getUserData(EditorPopupHelper.ANCHOR_POPUP_POSITION);

        if (visualPosition == null) {
            CaretModel caretModel = editor.getCaretModel();
            if (caretModel.isUpToDate()) {
                visualPosition = caretModel.getVisualPosition();
            }
            else {
                visualPosition = editor.offsetToVisualPosition(caretModel.getOffset());
            }
        }

        int lineHeight = editor.getLineHeight();
        Point p = editor.visualPositionToXY(visualPosition);
        p.y += lineHeight;

        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        return !visibleArea.contains(p) && !visibleArea.contains(p.x, p.y - lineHeight) ? null : p;
    }

    @Override
    public Point getCenterOf(JComponent container, JComponent content) {
        return AbstractPopup.getCenterOf(container, content);
    }

    @Override
    @Nonnull
    public List<JBPopup> getChildPopups(@Nonnull Component component) {
        return AbstractPopup.getChildPopups(component);
    }

    @Override
    public boolean isPopupActive() {
        return IdeEventQueueProxy.getInstance().isPopupActive();
    }

    @Nonnull
    @Override
    public BalloonBuilder createHtmlTextBalloonBuilder(
        @Nonnull String htmlContent,
        @Nullable Image icon,
        Color textColor,
        Color fillColor,
        @Nullable HyperlinkListener listener
    ) {
        JEditorPane text = IdeTooltipManagerImpl.initPane(htmlContent, new HintHint().setTextFg(textColor).setAwtTooltip(true), null);

        if (listener != null) {
            text.addHyperlinkListener(listener);
        }
        text.setEditable(false);
        NonOpaquePanel.setTransparent(text);
        text.setBorder(null);


        JLabel label = new JLabel();
        JPanel content =
            new NonOpaquePanel(new BorderLayout((int)(label.getIconTextGap() * 1.5), (int)(label.getIconTextGap() * 1.5)));

        NonOpaquePanel textWrapper = new NonOpaquePanel(new GridBagLayout());
        JScrollPane scrolledText = ScrollPaneFactory.createScrollPane(text, true);
        scrolledText.setBackground(fillColor);
        scrolledText.getViewport().setBackground(fillColor);
        textWrapper.add(scrolledText);
        content.add(textWrapper, BorderLayout.CENTER);
        if (icon != null) {
            NonOpaquePanel north = new NonOpaquePanel(new BorderLayout());
            north.add(new JBLabel(icon), BorderLayout.NORTH);
            content.add(north, BorderLayout.WEST);
        }

        content.setBorder(JBUI.Borders.empty(2, 4));

        BalloonBuilder builder = createBalloonBuilder(content);

        builder.setFillColor(fillColor);

        return builder;
    }

    @Nonnull
    @Override
    public BalloonBuilder createHtmlTextBalloonBuilder(
        @Nonnull String htmlContent,
        NotificationType type,
        @Nullable HyperlinkListener listener
    ) {
        MessageType messageType = MessageType.from(type);
        return createHtmlTextBalloonBuilder(htmlContent, messageType.getDefaultIcon(), messageType.getPopupBackground(), listener);
    }


    public static class ActionItem implements ShortcutProvider {
        private final AnAction myAction;
        private LocalizeValue myTextValue;
        private final boolean myIsEnabled;
        private final Image myIcon;
        private final Image mySelectedIcon;
        private final String myDescription;
        private final boolean myIsSeparator;

        ActionItem(
            @Nonnull AnAction action,
            @Nonnull LocalizeValue textValue,
            @Nullable String description,
            boolean enabled,
            @Nullable Image icon,
            @Nullable Image selectedIcon,
            boolean isSeparator
        ) {
            myAction = action;
            myTextValue = textValue;
            myIsEnabled = enabled;
            myIcon = icon;
            mySelectedIcon = selectedIcon;
            myDescription = description;
            myIsSeparator = isSeparator;
            myAction.getTemplatePresentation().addPropertyChangeListener(evt -> {
                if (evt.getPropertyName().equals(Presentation.PROP_TEXT)) {
                    myTextValue = myAction.getTemplatePresentation().getTextValue();
                }
            });
        }

        @Nonnull
        public AnAction getAction() {
            return myAction;
        }

        @Nonnull
        public LocalizeValue getText() {
            return myTextValue;
        }

        @Nullable
        public Image getIcon(boolean selected) {
            return selected && mySelectedIcon != null ? mySelectedIcon : myIcon;
        }

        public boolean isSeparator() {
            return myIsSeparator;
        }

        public boolean isEnabled() {
            return myIsEnabled;
        }

        public String getDescription() {
            return myDescription;
        }

        @Nullable
        @Override
        public ShortcutSet getShortcut() {
            return myAction.getShortcutSet();
        }

        @Override
        public String toString() {
            return myTextValue.getValue();
        }
    }
}
