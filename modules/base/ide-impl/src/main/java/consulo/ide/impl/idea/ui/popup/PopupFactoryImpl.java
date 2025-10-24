// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.VisualPosition;
import consulo.component.ComponentManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ide.impl.idea.ui.popup.actionPopup.ActionGroupPopup;
import consulo.ide.impl.idea.ui.popup.actionPopup.ActionPopupItem;
import consulo.ide.impl.idea.ui.popup.actionPopup.ActionPopupStep;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ide.impl.idea.ui.popup.mock.MockConfirmation;
import consulo.ide.impl.idea.ui.popup.tree.TreePopupImpl;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.NotificationType;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.BasePresentationFactory;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import consulo.util.lang.EmptyRunnable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class PopupFactoryImpl extends JBPopupFactory {
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
                Object balloon = ((JComponent) eachParent).getClientProperty(Balloon.KEY);
                if (balloon instanceof Balloon) {
                    return (Balloon) balloon;
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

    @Nonnull
    public static Supplier<DataContext> getComponentContextSupplier(Component component) {
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
        @Nullable String actionPlace,
        @Nonnull BiPredicate<Object, Boolean> customFilter
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
            actionPlace,
            true,
            customFilter
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
        Predicate<? super AnAction> preselectActionCondition,
        boolean forceHeavyPopup
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
            null,
            forceHeavyPopup,
            (o, aBoolean) -> true
        );
    }

    @Nonnull
    @Override
    public ListPopupStep<ActionPopupItem> createActionsStep(
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
            new BasePresentationFactory()
        );
    }

    @Nonnull
    @Override
    public ListPopup createListPopup(@Nonnull ListPopupStep step) {
        return new ListPopupImpl(step);
    }

    @Nonnull
    @Override
    public ListPopup createListPopup(@Nonnull ComponentManager project, @Nonnull ListPopupStep step) {
        return new ListPopupImpl((Project) project, step);
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
        return new TreePopupImpl((Project) aStep.getProject(), parent, aStep, parentValue);
    }

    @Nonnull
    @Override
    public TreePopup createTree(@Nonnull TreePopupStep aStep) {
        return new TreePopupImpl((Project) aStep.getProject(), null, aStep, null);
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
        JComponent focusOwner = component instanceof JComponent ? (JComponent) component : null;

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
            JList list = (JList) component;
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
            JTree tree = (JTree) component;
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
            JTable table = (JTable) component;
            int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            int row = Math.max(table.getSelectionModel().getLeadSelectionIndex(), table.getSelectionModel().getAnchorSelectionIndex());
            Rectangle rect = table.getCellRect(row, column, false);
            if (!visibleRect.intersects(rect)) {
                table.scrollRectToVisible(rect);
            }
            popupMenuPoint = new Point(rect.x, rect.y + rect.height - 1);
        }
        else if (component instanceof PopupOwner) {
            popupMenuPoint = ((PopupOwner) component).getBestPopupPosition();
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
            new NonOpaquePanel(new BorderLayout((int) (label.getIconTextGap() * 1.5), (int) (label.getIconTextGap() * 1.5)));

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
}
