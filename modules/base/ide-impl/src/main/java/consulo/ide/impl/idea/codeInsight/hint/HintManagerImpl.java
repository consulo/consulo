// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.*;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.ide.IdeTooltip;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.hint.HintColorUtil;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.hint.QuestionAction;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.editor.ui.internal.HintManagerEx;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.hint.HintListener;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ListenerUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awt.util.TimerUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

@Singleton
@ServiceImpl
public class HintManagerImpl implements HintManagerEx {
    private static final Logger LOG = Logger.getInstance(HintManager.class);

    private final Application myApplication;
    private final MyEditorManagerListener myEditorManagerListener;
    private final EditorMouseListener myEditorMouseListener;

    private final DocumentListener myEditorDocumentListener;
    private final VisibleAreaListener myVisibleAreaListener;
    private final CaretListener myCaretMoveListener;
    private final SelectionListener mySelectionListener;

    private LightweightHintImpl myQuestionHint;
    private QuestionAction myQuestionAction;

    private final List<HintInfo> myHintsStack = new ArrayList<>();
    private Editor myLastEditor;
    private final Alarm myHideAlarm = new Alarm();
    private boolean myRequestFocusForNextHint;

    private static int getPriority(QuestionAction action) {
        return action instanceof PriorityQuestionAction priorityQuestionAction ? priorityQuestionAction.getPriority() : 0;
    }

    @RequiredUIAccess
    public boolean canShowQuestionAction(QuestionAction action) {
        UIAccess.assertIsUIThread();
        return myQuestionAction == null || getPriority(myQuestionAction) <= getPriority(action);
    }

    public interface ActionToIgnore {
    }

    private static class HintInfo {
        final LightweightHintImpl hint;
        @HideFlags
        final int flags;
        private final boolean reviveOnEditorChange;

        private HintInfo(LightweightHintImpl hint, @HideFlags int flags, boolean reviveOnEditorChange) {
            this.hint = hint;
            this.flags = flags;
            this.reviveOnEditorChange = reviveOnEditorChange;
        }
    }

    public static HintManagerImpl getInstanceImpl() {
        return (HintManagerImpl) ServiceManager.getService(HintManager.class);
    }

    @Inject
    public HintManagerImpl(Application application) {
        myApplication = application;
        myEditorManagerListener = new MyEditorManagerListener();

        myCaretMoveListener = new CaretListener() {
            @Override
            @RequiredUIAccess
            public void caretPositionChanged(@Nonnull CaretEvent e) {
                hideHints(HIDE_BY_ANY_KEY | HIDE_BY_CARET_MOVE, false, false);
            }
        };

        mySelectionListener = new SelectionListener() {
            @Override
            @RequiredUIAccess
            public void selectionChanged(@Nonnull SelectionEvent e) {
                hideHints(HIDE_BY_CARET_MOVE, false, false);
            }
        };

        MyProjectManagerListener projectManagerListener = new MyProjectManagerListener();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            projectManagerListener.projectOpened(project);
        }

        MessageBusConnection busConnection = myApplication.getMessageBus().connect();
        busConnection.subscribe(ProjectManagerListener.class, projectManagerListener);
        busConnection.subscribe(AnActionListener.class, new MyAnActionListener());

        myEditorMouseListener = new EditorMouseListener() {
            @Override
            @RequiredUIAccess
            public void mousePressed(@Nonnull EditorMouseEvent event) {
                hideAllHints();
            }
        };

        myVisibleAreaListener = e -> {
            //noinspection RequiredXAction
            updateScrollableHints(e);
            if (e.getOldRectangle() == null || e.getOldRectangle().x != e.getNewRectangle().x || e.getOldRectangle().y != e.getNewRectangle().y) {
                //noinspection RequiredXAction
                hideHints(HIDE_BY_SCROLLING, false, false);
            }
        };

        myEditorDocumentListener = new DocumentListener() {
            @Override
            public void documentChanged(@Nonnull DocumentEvent event) {
                LOG.assertTrue(SwingUtilities.isEventDispatchThread());
                if (event.getOldLength() == 0 && event.getNewLength() == 0) {
                    return;
                }
                HintInfo[] infos = getHintsStackArray();
                for (HintInfo info : infos) {
                    if (BitUtil.isSet(info.flags, HIDE_BY_TEXT_CHANGE)) {
                        if (info.hint.isVisible()) {
                            info.hint.hide();
                        }
                        myHintsStack.remove(info);
                    }
                }

                if (myHintsStack.isEmpty()) {
                    updateLastEditor(null);
                }
            }
        };
    }

    /**
     * Sets whether the next {@code showXxx} call will request the focus to the
     * newly shown tooltip. Note the flag applies only to the next call, i.e. is
     * reset to {@code false} after any {@code showXxx} is called.
     *
     * <p>Note: This method was created to avoid the code churn associated with
     * creating an overload to every {@code showXxx} method with an additional
     * {@code boolean requestFocus} parameter </p>
     */
    @Override
    @RequiredUIAccess
    public void setRequestFocusForNextHint(boolean requestFocus) {
        myRequestFocusForNextHint = requestFocus;
    }

    @Nonnull
    private HintInfo[] getHintsStackArray() {
        return myHintsStack.toArray(new HintInfo[0]);
    }

    @RequiredUIAccess
    public boolean performCurrentQuestionAction() {
        UIAccess.assertIsUIThread();
        if (myQuestionAction != null && myQuestionHint != null) {
            if (myQuestionHint.isVisible()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("performing an action:" + myQuestionAction);
                }
                if (myQuestionAction.execute() && myQuestionHint != null) {
                    myQuestionHint.hide();
                }
                return true;
            }

            myQuestionAction = null;
            myQuestionHint = null;
        }

        return false;
    }

    @RequiredUIAccess
    private void updateScrollableHints(VisibleAreaEvent e) {
        LOG.assertTrue(SwingUtilities.isEventDispatchThread());
        for (HintInfo info : getHintsStackArray()) {
            if (info.hint != null && BitUtil.isSet(info.flags, UPDATE_BY_SCROLLING)) {
                updateScrollableHintPosition(e, info.hint, BitUtil.isSet(info.flags, HIDE_IF_OUT_OF_EDITOR));
            }
        }
    }

    @Override
    @RequiredUIAccess
    public boolean hasShownHintsThatWillHideByOtherHint(boolean willShowTooltip) {
        LOG.assertTrue(SwingUtilities.isEventDispatchThread());
        for (HintInfo hintInfo : getHintsStackArray()) {
            if (hintInfo.hint.isVisible() && BitUtil.isSet(hintInfo.flags, HIDE_BY_OTHER_HINT)
                || willShowTooltip && hintInfo.hint.isAwtTooltip()) {
                return true;
            }
        }
        return false;
    }

    private static void updateScrollableHintPosition(VisibleAreaEvent e, LightweightHintImpl hint, boolean hideIfOutOfEditor) {
        if (hint.getComponent() instanceof ScrollAwareHint scrollAwareHint) {
            scrollAwareHint.editorScrolled();
        }

        if (!hint.isVisible()) {
            return;
        }

        Editor editor = e.getEditor();
        if (!editor.getComponent().isShowing() || editor.isOneLineMode()) {
            return;
        }
        Rectangle newRectangle = e.getOldRectangle();
        Rectangle oldRectangle = e.getNewRectangle();

        Point location = hint.getLocationOn(editor.getContentComponent());
        Dimension size = hint.getSize();

        int xOffset = location.x - oldRectangle.x;
        int yOffset = location.y - oldRectangle.y;
        location = new Point(newRectangle.x + xOffset, newRectangle.y + yOffset);

        Rectangle newBounds = new Rectangle(location.x, location.y, size.width, size.height);
        //in some rare cases lookup can appear just on the edge with the editor, so don't hide it on every typing
        Rectangle newBoundsForIntersectionCheck =
            new Rectangle(location.x - 1, location.y - 1, size.width + 2, size.height + 2);

        boolean okToUpdateBounds =
            hideIfOutOfEditor ? oldRectangle.contains(newBounds) : oldRectangle.intersects(newBoundsForIntersectionCheck);
        if (okToUpdateBounds || hint.vetoesHiding()) {
            hint.setLocation(new RelativePoint(editor.getContentComponent(), location));
        }
        else {
            hint.hide();
        }
    }

    /**
     * In this method the point to show hint depends on current caret position.
     * So, first of all, editor will be scrolled to make the caret position visible.
     */
    @Override
    @RequiredUIAccess
    public void showEditorHint(
        LightweightHint hint,
        Editor editor,
        @PositionFlags short constraint,
        @HideFlags int flags,
        int timeout,
        boolean reviveOnEditorChange
    ) {
        UIAccess.assertIsUIThread();
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        editor.getScrollingModel().runActionOnScrollingFinished(() -> {
            LogicalPosition pos = editor.getCaretModel().getLogicalPosition();
            @SuppressWarnings("RequiredXAction")
            Point p = getHintPosition(hint, editor, pos, constraint);
            //noinspection RequiredXAction
            showEditorHint(hint, editor, p, flags, timeout, reviveOnEditorChange, createHintHint(editor, p, hint, constraint));
        });
    }

    /**
     * @param p point in layered pane coordinate system.
     */
    @Override
    @RequiredUIAccess
    public void showEditorHint(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull Point p,
        @HideFlags int flags,
        int timeout,
        boolean reviveOnEditorChange
    ) {
        showEditorHint(hint, editor, p, flags, timeout, reviveOnEditorChange, HintManager.ABOVE);
    }

    @Override
    @RequiredUIAccess
    public void showEditorHint(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull Point p,
        @HideFlags int flags,
        int timeout,
        boolean reviveOnEditorChange,
        @PositionFlags short position
    ) {
        HintHint hintHint = createHintHint(editor, p, hint, position).setShowImmediately(true);
        showEditorHint(hint, editor, p, flags, timeout, reviveOnEditorChange, hintHint);
    }

    @Override
    @RequiredUIAccess
    public void showEditorHint(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull Point p,
        @HideFlags int flags,
        int timeout,
        boolean reviveOnEditorChange,
        HintHint hintInfo
    ) {
        LOG.assertTrue(SwingUtilities.isEventDispatchThread());
        myHideAlarm.cancelAllRequests();

        LightweightHintImpl impl = (LightweightHintImpl) hint;

        hideHints(HIDE_BY_OTHER_HINT, false, false);

        if (editor != myLastEditor) {
            hideAllHints();
        }

        if (!myApplication.isUnitTestMode() && !editor.getContentComponent().isShowing()
            || !myApplication.isActive()) {
            return;
        }

        updateLastEditor(editor);

        getPublisher().hintShown(editor.getProject(), impl, flags);

        Component component = impl.getComponent();

        // Set focus to control so that screen readers will announce the tooltip contents.
        // Users can press "ESC" to return to the editor.
        if (myRequestFocusForNextHint) {
            hintInfo.setRequestFocus(true);
            myRequestFocusForNextHint = false;
        }
        doShowInGivenLocation(impl, editor, p, hintInfo, true);

        ListenerUtil.addMouseListener(component, new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                myHideAlarm.cancelAllRequests();
            }
        });
        ListenerUtil.addFocusListener(component, new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                myHideAlarm.cancelAllRequests();
            }
        });

        if (BitUtil.isSet(flags, HIDE_BY_MOUSEOVER)) {
            ListenerUtil.addMouseMotionListener(component, new MouseMotionAdapter() {
                @Override
                @RequiredUIAccess
                public void mouseMoved(MouseEvent e) {
                    hideHints(HIDE_BY_MOUSEOVER, true, false);
                }
            });
        }

        myHintsStack.add(new HintInfo(impl, flags, reviveOnEditorChange));
        if (timeout > 0) {
            Timer timer = TimerUtil.createNamedTimer("Hint timeout", timeout, event -> hint.hide());
            timer.setRepeats(false);
            timer.start();
        }
    }

    @Override
    @RequiredUIAccess
    public void showHint(@Nonnull final JComponent component, @Nonnull RelativePoint p, int flags, int timeout) {
        LOG.assertTrue(SwingUtilities.isEventDispatchThread());
        myHideAlarm.cancelAllRequests();

        hideHints(HIDE_BY_OTHER_HINT, false, false);

        final JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, null)
            .setRequestFocus(false).setResizable(false).setMovable(false).createPopup();
        popup.show(p);

        ListenerUtil.addMouseListener(component, new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                myHideAlarm.cancelAllRequests();
            }
        });
        ListenerUtil.addFocusListener(component, new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                myHideAlarm.cancelAllRequests();
            }
        });

        HintInfo info = new HintInfo(
            new LightweightHintImpl(component) {
                @Override
                public void hide() {
                    popup.cancel();
                }
            },
            flags,
            false
        );
        myHintsStack.add(info);
        if (timeout > 0) {
            Timer timer = TimerUtil.createNamedTimer("Popup timeout", timeout, event -> Disposer.dispose(popup));
            timer.setRepeats(false);
            timer.start();
        }
    }

    @RequiredUIAccess
    private static void doShowInGivenLocation(
        LightweightHint hint,
        Editor editor,
        Point p,
        HintHint hintInfo,
        boolean updateSize
    ) {
        LightweightHintImpl impl = (LightweightHintImpl) hint;
        JComponent externalComponent = getExternalComponent(editor);
        Dimension size = updateSize ? impl.getComponent().getPreferredSize() : impl.getComponent().getSize();

        if (impl.isRealPopup() || hintInfo.isPopupForced()) {
            Point point = new Point(p);
            SwingUtilities.convertPointToScreen(point, externalComponent);
            Rectangle editorScreen = ScreenUtil.getScreenRectangle(point.x, point.y);

            p = new Point(p);
            if (hintInfo.getPreferredPosition() == Balloon.Position.atLeft) {
                p.x -= size.width;
            }
            SwingUtilities.convertPointToScreen(p, externalComponent);
            Rectangle rectangle = new Rectangle(p, size);
            ScreenUtil.moveToFit(rectangle, editorScreen, null);
            p = rectangle.getLocation();
            SwingUtilities.convertPointFromScreen(p, externalComponent);
            if (hintInfo.getPreferredPosition() == Balloon.Position.atLeft) {
                p.x += size.width;
            }
        }
        else if (externalComponent.getWidth() < p.x + size.width && !hintInfo.isAwtTooltip()) {
            p.x = Math.max(0, externalComponent.getWidth() - size.width);
        }

        if (hint.isVisible()) {
            if (updateSize) {
                hint.pack();
            }
            impl.updatePosition(hintInfo.getPreferredPosition());
            impl.updateLocation(p.x, p.y);
        }
        else {
            hint.show(externalComponent, p.x, p.y, editor.getContentComponent(), hintInfo);
        }
    }

    @Override
    @RequiredUIAccess
    public void updateLocation(LightweightHint hint, Editor editor, Point p) {
        doShowInGivenLocation(hint, editor, p, createHintHint(editor, p, hint, UNDER), false);
    }

    @Override
    @RequiredUIAccess
    public void adjustEditorHintPosition(
        LightweightHint hint,
        Editor editor,
        Point p,
        @PositionFlags short constraint
    ) {
        doShowInGivenLocation(hint, editor, p, createHintHint(editor, p, hint, constraint), true);
    }

    @Override
    @RequiredUIAccess
    public void hideAllHints() {
        LOG.assertTrue(SwingUtilities.isEventDispatchThread());
        for (HintInfo info : getHintsStackArray()) {
            if (!info.hint.vetoesHiding()) {
                info.hint.hide();
            }
        }
        cleanup();
    }

    public void cleanup() {
        myHintsStack.clear();
        updateLastEditor(null);
    }

    /**
     * @return coordinates in layered pane coordinate system.
     */
    @Override
    @RequiredUIAccess
    public Point getHintPosition(@Nonnull LightweightHint hint, @Nonnull Editor editor, @PositionFlags short constraint) {
        LogicalPosition pos = editor.getCaretModel().getLogicalPosition();
        DataContext dataContext = editor.getDataContext();
        Rectangle dominantArea = dataContext.getData(UIExAWTDataKey.DOMINANT_HINT_AREA_RECTANGLE);

        LOG.assertTrue(SwingUtilities.isEventDispatchThread());
        if (dominantArea != null) {
            return getHintPositionRelativeTo(hint, editor, constraint, dominantArea, pos);
        }

        JRootPane rootPane = editor.getComponent().getRootPane();
        if (rootPane != null) {
            JLayeredPane lp = rootPane.getLayeredPane();
            for (HintInfo info : getHintsStackArray()) {
                if (!info.hint.isSelectingHint()) {
                    continue;
                }
                IdeTooltip tooltip = info.hint.getCurrentIdeTooltip();
                if (tooltip != null) {
                    Point p = tooltip.getShowingPoint().getPoint(lp);
                    if (info.hint != hint) {
                        switch (constraint) {
                            case ABOVE:
                                if (tooltip.getPreferredPosition() == Balloon.Position.below) {
                                    p.y -= tooltip.getPositionChangeY();
                                }
                                break;
                            case UNDER:
                            case RIGHT_UNDER:
                                if (tooltip.getPreferredPosition() == Balloon.Position.above) {
                                    p.y += tooltip.getPositionChangeY();
                                }
                                break;
                            case RIGHT:
                                if (tooltip.getPreferredPosition() == Balloon.Position.atLeft) {
                                    p.x += tooltip.getPositionChangeX();
                                }
                                break;
                            case LEFT:
                                if (tooltip.getPreferredPosition() == Balloon.Position.atRight) {
                                    p.x -= tooltip.getPositionChangeX();
                                }
                                break;
                        }
                    }
                    return p;
                }

                Rectangle rectangle = info.hint.getBounds();
                JComponent c = info.hint.getComponent();
                rectangle = SwingUtilities.convertRectangle(c.getParent(), rectangle, lp);

                return getHintPositionRelativeTo(hint, editor, constraint, rectangle, pos);
            }
        }

        return getHintPosition(hint, editor, pos, constraint);
    }

    @RequiredUIAccess
    private Point getHintPositionRelativeTo(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @PositionFlags short constraint,
        @Nonnull Rectangle lookupBounds,
        LogicalPosition pos
    ) {
        JComponent externalComponent = getExternalComponent(editor);

        LightweightHintImpl hitImpl = (LightweightHintImpl) hint;

        IdeTooltip ideTooltip = hitImpl.getCurrentIdeTooltip();
        if (ideTooltip != null) {
            Point point = ideTooltip.getPoint();
            return SwingUtilities.convertPoint(ideTooltip.getComponent(), point, externalComponent);
        }

        Dimension hintSize = hitImpl.getComponent().getPreferredSize();
        int layeredPaneHeight = externalComponent.getHeight();

        switch (constraint) {
            case LEFT: {
                int y = lookupBounds.y;
                if (y < 0) {
                    y = 0;
                }
                else if (y + hintSize.height >= layeredPaneHeight) {
                    y = layeredPaneHeight - hintSize.height;
                }
                return new Point(lookupBounds.x - hintSize.width, y);
            }

            case RIGHT:
                int y = lookupBounds.y;
                if (y < 0) {
                    y = 0;
                }
                else if (y + hintSize.height >= layeredPaneHeight) {
                    y = layeredPaneHeight - hintSize.height;
                }
                return new Point(lookupBounds.x + lookupBounds.width, y);

            case ABOVE:
                Point posAboveCaret = getHintPosition(hint, editor, pos, ABOVE);
                return new Point(lookupBounds.x, Math.min(posAboveCaret.y, lookupBounds.y - hintSize.height));

            case UNDER:
                Point posUnderCaret = getHintPosition(hint, editor, pos, UNDER);
                return new Point(lookupBounds.x, Math.max(posUnderCaret.y, lookupBounds.y + lookupBounds.height));

            default:
                LOG.error("");
                return null;
        }
    }

    /**
     * @return position of hint in layered pane coordinate system
     */
    @Override
    @RequiredUIAccess
    public Point getHintPosition(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull LogicalPosition pos,
        @PositionFlags short constraint
    ) {
        VisualPosition visualPos = editor.logicalToVisualPosition(pos);
        return getHintPosition(hint, editor, visualPos, visualPos, constraint);
    }

    /**
     * @return position of hint in layered pane coordinate system
     */
    @Override
    @RequiredUIAccess
    public Point getHintPosition(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull VisualPosition pos,
        @PositionFlags short constraint
    ) {
        return getHintPosition(hint, editor, pos, pos, constraint);
    }

    @RequiredUIAccess
    private static Point getHintPosition(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull VisualPosition pos1,
        @Nonnull VisualPosition pos2,
        @PositionFlags short constraint
    ) {
        return getHintPosition(hint, editor, pos1, pos2, constraint, Registry.is("editor.balloonHints"));
    }

    @RequiredUIAccess
    private static Point getHintPosition(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull VisualPosition pos1,
        @Nonnull VisualPosition pos2,
        @PositionFlags short constraint,
        boolean showByBalloon
    ) {
        LightweightHintImpl impl = (LightweightHintImpl) hint;
        Point p = _getHintPosition(impl, editor, pos1, pos2, constraint, showByBalloon);
        JComponent externalComponent = getExternalComponent(editor);
        Dimension hintSize = impl.getComponent().getPreferredSize();
        if (constraint == ABOVE) {
            if (p.y < 0) {
                Point p1 = _getHintPosition(impl, editor, pos1, pos2, UNDER, showByBalloon);
                if (p1.y + hintSize.height <= externalComponent.getSize().height) {
                    return p1;
                }
            }
        }
        else if (constraint == UNDER) {
            if (p.y + hintSize.height > externalComponent.getSize().height) {
                Point p1 = _getHintPosition(impl, editor, pos1, pos2, ABOVE, showByBalloon);
                if (p1.y >= 0) {
                    return p1;
                }
            }
        }

        return p;
    }

    @Nonnull
    @RequiredUIAccess
    public static JComponent getExternalComponent(@Nonnull Editor editor) {
        JComponent externalComponent = editor.getComponent();
        JRootPane rootPane = externalComponent.getRootPane();
        if (rootPane == null) {
            return externalComponent;
        }
        JLayeredPane layeredPane = rootPane.getLayeredPane();
        return layeredPane != null ? layeredPane : rootPane;
    }

    @RequiredUIAccess
    private static Point _getHintPosition(
        @Nonnull LightweightHintImpl hint,
        @Nonnull Editor editor,
        @Nonnull VisualPosition pos1,
        @Nonnull VisualPosition pos2,
        @PositionFlags short constraint,
        boolean showByBalloon
    ) {
        Dimension hintSize = hint.getComponent().getPreferredSize();

        Point location;
        JComponent externalComponent = getExternalComponent(editor);
        JComponent internalComponent = editor.getContentComponent();
        if (constraint == RIGHT_UNDER) {
            Point p = editor.visualPositionToXY(pos2);
            if (!showByBalloon) {
                p.y += editor.getLineHeight();
            }
            location = SwingUtilities.convertPoint(internalComponent, p, externalComponent);
        }
        else {
            Point p = editor.visualPositionToXY(pos1);
            if (constraint == UNDER) {
                p.y += editor.getLineHeight();
            }
            location = SwingUtilities.convertPoint(internalComponent, p, externalComponent);
        }

        if (constraint == ABOVE && !showByBalloon) {
            location.y -= hintSize.height;
            int diff = location.x + hintSize.width - externalComponent.getWidth();
            if (diff > 0) {
                location.x = Math.max(location.x - diff, 0);
            }
        }

        if ((constraint == LEFT || constraint == RIGHT) && !showByBalloon) {
            location.y -= hintSize.height / 2;
            if (constraint == LEFT) {
                location.x -= hintSize.width;
            }
        }

        return location;
    }

    @Override
    @RequiredUIAccess
    public void showErrorHint(@Nonnull Editor editor, @Nonnull String text) {
        showErrorHint(editor, text, ABOVE);
    }

    @Override
    @RequiredUIAccess
    public void showErrorHint(@Nonnull Editor editor, @Nonnull String text, short position) {
        JComponent label = HintUtil.createErrorLabel(text);
        LightweightHintImpl hint = new LightweightHintImpl(label);
        Point p = getHintPosition(hint, editor, position);
        showEditorHint(hint, editor, p, HIDE_BY_ANY_KEY | HIDE_BY_TEXT_CHANGE | HIDE_BY_SCROLLING, 0, false, position);
    }

    @Override
    @RequiredUIAccess
    public void showInformationHint(@Nonnull Editor editor, @Nonnull String text, @PositionFlags short position) {
        showInformationHint(editor, text, null, position);
    }

    @Override
    @RequiredUIAccess
    public void showInformationHint(@Nonnull Editor editor, @Nonnull String text, @Nullable HyperlinkListener listener) {
        showInformationHint(editor, text, listener, ABOVE);
    }

    @RequiredUIAccess
    private void showInformationHint(
        @Nonnull Editor editor,
        @Nonnull String text,
        @Nullable HyperlinkListener listener,
        @PositionFlags short position
    ) {
        JComponent label = HintUtil.createInformationLabel(text, listener, null, null);
        showInformationHint(editor, label, position);
    }

    @Override
    @RequiredUIAccess
    public void showInformationHint(@Nonnull Editor editor, @Nonnull JComponent component) {
        // Set the accessible name so that screen readers announce the panel type (e.g. "Hint panel")
        // when the tooltip gets the focus.
        showInformationHint(editor, component, ABOVE);
    }

    @RequiredUIAccess
    public void showInformationHint(@Nonnull Editor editor, @Nonnull JComponent component, @PositionFlags short position) {
        AccessibleContextUtil.setName(component, "Hint");
        LightweightHintImpl hint = new LightweightHintImpl(component);
        Point p = getHintPosition(hint, editor, position);
        showEditorHint(hint, editor, p, HIDE_BY_ANY_KEY | HIDE_BY_TEXT_CHANGE | HIDE_BY_SCROLLING, 0, false, position);
    }

    @Override
    @RequiredUIAccess
    public void showErrorHint(
        @Nonnull Editor editor,
        @Nonnull String hintText,
        int offset1,
        int offset2,
        short constraint,
        int flags,
        int timeout
    ) {
        JComponent label = HintUtil.createErrorLabel(hintText);
        LightweightHintImpl hint = new LightweightHintImpl(label);
        VisualPosition pos1 = editor.offsetToVisualPosition(offset1);
        VisualPosition pos2 = editor.offsetToVisualPosition(offset2);
        Point p = getHintPosition(hint, editor, pos1, pos2, constraint);
        showEditorHint(hint, editor, p, flags, timeout, false);
    }


    @Override
    @RequiredUIAccess
    public void showQuestionHint(
        @Nonnull Editor editor,
        @Nonnull String hintText,
        int offset1,
        int offset2,
        @Nonnull QuestionAction action
    ) {
        JComponent label = HintUtil.createQuestionLabel(hintText);
        LightweightHintImpl hint = new LightweightHintImpl(label);

        if (hint.getComponent() instanceof HintUtil.HintLabel hintLabel) {
            JEditorPane pane = hintLabel.getPane();
            if (pane != null) {
                pane.addHyperlinkListener(e -> {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && "action".equals(e.getDescription()) && hint.isVisible()) {
                        boolean execute = action.execute();

                        if (execute) {
                            hint.hide();
                        }
                    }
                });
            }
        }

        showQuestionHint(editor, offset1, offset2, hint, action, ABOVE);
    }

    @RequiredUIAccess
    public void showQuestionHint(
        @Nonnull Editor editor,
        int offset1,
        int offset2,
        @Nonnull LightweightHintImpl hint,
        @Nonnull QuestionAction action,
        @PositionFlags short constraint
    ) {
        VisualPosition pos1 = editor.offsetToVisualPosition(offset1);
        VisualPosition pos2 = editor.offsetToVisualPosition(offset2);
        Point p = getHintPosition(hint, editor, pos1, pos2, constraint);
        showQuestionHint(editor, p, offset1, offset2, hint, action, constraint);
    }

    @RequiredUIAccess
    public void showQuestionHint(
        @Nonnull Editor editor,
        @Nonnull Point p,
        int offset1,
        int offset2,
        @Nonnull final LightweightHintImpl hint,
        @Nonnull QuestionAction action,
        @PositionFlags short constraint
    ) {
        UIAccess.assertIsUIThread();
        hideQuestionHint();
        TextAttributes attributes = new TextAttributes();
        attributes.setEffectColor(TargetAWT.from(HintColorUtil.QUESTION_UNDERSCORE_COLOR));
        attributes.setEffectType(EffectType.LINE_UNDERSCORE);
        final RangeHighlighter highlighter = editor.getMarkupModel()
            .addRangeHighlighter(offset1, offset2, HighlighterLayer.ERROR + 1, attributes, HighlighterTargetArea.EXACT_RANGE);

        hint.addHintListener(new HintListener() {
            @Override
            public void hintHidden(@Nonnull EventObject event) {
                hint.removeHintListener(this);
                highlighter.dispose();

                if (myQuestionHint == hint) {
                    myQuestionAction = null;
                    myQuestionHint = null;
                }
            }
        });

        showEditorHint(
            hint,
            editor,
            p,
            HIDE_BY_ANY_KEY | HIDE_BY_TEXT_CHANGE | UPDATE_BY_SCROLLING | HIDE_IF_OUT_OF_EDITOR | DONT_CONSUME_ESCAPE,
            0,
            false,
            createHintHint(editor, p, hint, constraint)
        );
        myQuestionAction = action;
        myQuestionHint = hint;
    }

    @RequiredUIAccess
    private void hideQuestionHint() {
        UIAccess.assertIsUIThread();
        if (myQuestionHint != null) {
            myQuestionHint.hide();
            myQuestionHint = null;
            myQuestionAction = null;
        }
    }

    @Override
    @RequiredUIAccess
    public HintHint createHintHint(Editor editor, Point p, LightweightHint hint, @PositionFlags short constraint) {
        return createHintHint(editor, p, hint, constraint, false);
    }

    //todo[nik,kirillk] perhaps 'createInEditorComponent' parameter should always be 'true'
    //old 'createHintHint' method uses LayeredPane as original component for HintHint so IdeTooltipManager.eventDispatched()
    //wasn't able to correctly hide tooltip after mouse move.
    @Override
    @RequiredUIAccess
    public HintHint createHintHint(
        Editor editor,
        Point p,
        LightweightHint hint,
        @PositionFlags short constraint,
        boolean createInEditorComponent
    ) {
        JRootPane rootPane = editor.getComponent().getRootPane();
        if (rootPane == null) {
            return new HintHint(editor.getContentComponent(), p);
        }

        JLayeredPane lp = rootPane.getLayeredPane();
        HintHint hintInfo = new HintHint(editor.getContentComponent(), SwingUtilities.convertPoint(lp, p, editor.getContentComponent()));
        boolean showByBalloon = Registry.is("editor.balloonHints");
        if (showByBalloon) {
            if (!createInEditorComponent) {
                hintInfo = new HintHint(lp, p);
            }
            hintInfo.setAwtTooltip(true).setHighlighterType(true);
        }

        hintInfo.initStyleFrom(((LightweightHintImpl) hint).getComponent());
        if (showByBalloon) {
            hintInfo.setFont(hintInfo.getTextFont().deriveFont(Font.PLAIN));
            hintInfo.setCalloutShift((int) (editor.getLineHeight() * 0.1));
        }
        hintInfo.setPreferredPosition(Balloon.Position.above);
        if (constraint == UNDER || constraint == RIGHT_UNDER) {
            hintInfo.setPreferredPosition(Balloon.Position.below);
        }
        else if (constraint == RIGHT) {
            hintInfo.setPreferredPosition(Balloon.Position.atRight);
        }
        else if (constraint == LEFT) {
            hintInfo.setPreferredPosition(Balloon.Position.atLeft);
        }

        if (((LightweightHintImpl) hint).isAwtTooltip()) {
            hintInfo.setAwtTooltip(true);
        }

        hintInfo.setPositionChangeShift(0, editor.getLineHeight());

        return hintInfo;
    }

    protected void updateLastEditor(Editor editor) {
        if (myLastEditor != editor) {
            if (myLastEditor != null) {
                myLastEditor.removeEditorMouseListener(myEditorMouseListener);
                myLastEditor.getDocument().removeDocumentListener(myEditorDocumentListener);
                myLastEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
                myLastEditor.getCaretModel().removeCaretListener(myCaretMoveListener);
                myLastEditor.getSelectionModel().removeSelectionListener(mySelectionListener);
            }

            myLastEditor = editor;
            if (myLastEditor != null) {
                myLastEditor.addEditorMouseListener(myEditorMouseListener);
                myLastEditor.getDocument().addDocumentListener(myEditorDocumentListener);
                myLastEditor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
                myLastEditor.getCaretModel().addCaretListener(myCaretMoveListener);
                myLastEditor.getSelectionModel().addSelectionListener(mySelectionListener);
            }
        }
    }

    private class MyAnActionListener implements AnActionListener {
        @Override
        @RequiredUIAccess
        public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
            if (action instanceof ActionToIgnore) {
                return;
            }

            AnAction escapeAction = ActionManagerEx.getInstanceEx().getAction(IdeActions.ACTION_EDITOR_ESCAPE);
            if (action == escapeAction) {
                return;
            }

            hideHints(HIDE_BY_ANY_KEY, false, false);
        }
    }

    /**
     * Hides all hints when selected editor changes. Unfortunately  user can change
     * selected editor by mouse. These clicks are not AnActions so they are not
     * fired by ActionManager.
     */
    private final class MyEditorManagerListener implements FileEditorManagerListener {
        @Override
        @RequiredUIAccess
        public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
            hideHints(0, false, true);
        }
    }

    /**
     * We have to spy for all opened projects to register MyEditorManagerListener into
     * all opened projects.
     */
    private final class MyProjectManagerListener implements ProjectManagerListener {
        @Override
        public void projectOpened(@Nonnull Project project) {
            project.getMessageBus().connect().subscribe(FileEditorManagerListener.class, myEditorManagerListener);
        }

        @Override
        @RequiredUIAccess
        public void projectClosed(@Nonnull Project project) {
            UIAccess.assertIsUIThread();

            // avoid leak through consulo.ide.impl.idea.codeInsight.hint.TooltipController.myCurrentTooltip
            TooltipController.getInstance().cancelTooltips();
            myApplication.invokeLater(() -> hideHints(0, false, false));

            myQuestionAction = null;
            myQuestionHint = null;
            if (myLastEditor != null && project == myLastEditor.getProject()) {
                updateLastEditor(null);
            }
        }
    }

    boolean isEscapeHandlerEnabled() {
        LOG.assertTrue(SwingUtilities.isEventDispatchThread());
        for (int i = myHintsStack.size() - 1; i >= 0; i--) {
            HintInfo info = myHintsStack.get(i);
            if (!info.hint.isVisible()) {
                myHintsStack.remove(i);

                // We encountered situation when 'hint' instances use 'hide()' method as object destruction callback
                // (e.g. LineTooltipRenderer creates hint that overrides keystroke of particular action that produces hint and
                // de-registers it inside 'hide()'. That means that the hint can 'stuck' to old editor location if we just remove
                // it but don't call hide())
                info.hint.hide();
                continue;
            }

            if ((info.flags & (HIDE_BY_ESCAPE | HIDE_BY_ANY_KEY)) != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public boolean hideHints(int mask, boolean onlyOne, boolean editorChanged) {
        LOG.assertTrue(SwingUtilities.isEventDispatchThread());
        try {
            boolean done = false;

            for (int i = myHintsStack.size() - 1; i >= 0; i--) {
                HintInfo info = myHintsStack.get(i);
                if (!info.hint.isVisible() && !info.hint.vetoesHiding()) {
                    myHintsStack.remove(i);

                    // We encountered situation when 'hint' instances use 'hide()' method as object destruction callback
                    // (e.g. LineTooltipRenderer creates hint that overrides keystroke of particular action that produces hint and
                    // de-registers it inside 'hide()'. That means that the hint can 'stuck' to old editor location if we just remove
                    // it but don't call hide())
                    info.hint.hide();
                    continue;
                }

                if ((info.flags & mask) != 0 || editorChanged && !info.reviveOnEditorChange) {
                    info.hint.hide();
                    myHintsStack.remove(info);
                    if ((mask & HIDE_BY_ESCAPE) == 0 || (info.flags & DONT_CONSUME_ESCAPE) == 0) {
                        if (onlyOne) {
                            return true;
                        }
                        done = true;
                    }
                }
            }

            return done;
        }
        finally {
            if (myHintsStack.isEmpty()) {
                updateLastEditor(null);
            }
        }
    }

    private static class EditorHintListenerHolder {
        private static final EditorHintListener ourEditorHintPublisher =
            Application.get().getMessageBus().syncPublisher(EditorHintListener.class);

        private EditorHintListenerHolder() {
        }
    }

    private static EditorHintListener getPublisher() {
        return EditorHintListenerHolder.ourEditorHintPublisher;
    }
}
