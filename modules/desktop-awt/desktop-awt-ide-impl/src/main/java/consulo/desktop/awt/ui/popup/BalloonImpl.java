// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.popup;

import consulo.application.ApplicationManager;
import consulo.application.ui.FrameStateManager;
import consulo.application.ui.RemoteDesktopService;
import consulo.application.ui.event.FrameStateListener;
import consulo.application.ui.wm.ExpirableRunnable;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.document.util.UnfairTextRange;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.ide.IdeTooltip;
import consulo.ide.impl.idea.ide.ui.PopupLocationTracker;
import consulo.ide.impl.idea.ide.ui.ScreenAreaConsumer;
import consulo.ide.impl.idea.openapi.wm.WeakFocusStackManager;
import consulo.ide.impl.idea.ui.ComponentWithMnemonics;
import consulo.ide.impl.idea.util.ui.BaseButtonBehavior;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.IdeGlassPane;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awt.util.TimedDeadzone;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.concurrent.ActionCallback;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static consulo.ui.ex.awt.UIUtil.useSafely;

public class BalloonImpl implements Balloon, IdeTooltip.Ui, ScreenAreaConsumer {
    private class BalloonEventListener implements AWTEventListener {
        @Override
        public void eventDispatched(AWTEvent e) {
            if (mySmartFadeoutDelay > 0) {
                startFadeoutTimer(mySmartFadeoutDelay);
                mySmartFadeoutDelay = 0;
            }

            if (e instanceof MouseEvent me) {
                onMouseEvent(me);
            }
            else if (e instanceof KeyEvent ke && e.getID() == KeyEvent.KEY_PRESSED) {
                onKeyPressedEvent(ke);
            }
        }

        private boolean onMouseEvent(MouseEvent me) {
            int id = me.getID();
            boolean forcedExit = id == MouseEvent.MOUSE_EXITED && me.getButton() != MouseEvent.NOBUTTON && !myBlockClicks;
            boolean insideBalloon = isInsideBalloon(me);
            if (myHideOnMouse && (id == MouseEvent.MOUSE_PRESSED || forcedExit)) {
                if ((!insideBalloon || forcedExit) && !isWithinChildWindow(me)) {
                    if (myHideListener == null) {
                        onHideDefault(me, forcedExit);
                    }
                    else {
                        myHideListener.run();
                    }
                }
                return true;
            }

            if (myClickHandler != null && id == MouseEvent.MOUSE_CLICKED) {
                if (!(me.getComponent() instanceof CloseButton) && insideBalloon) {
                    myClickHandler.actionPerformed(new ActionEvent(me, ActionEvent.ACTION_PERFORMED, "click", me.getModifiersEx()));
                    if (myCloseOnClick) {
                        hide();
                        return true;
                    }
                }
            }

            if (myEnableButtons && id == MouseEvent.MOUSE_MOVED) {
                boolean moveChanged = insideBalloon != myLastMoveWasInsideBalloon;
                myLastMoveWasInsideBalloon = insideBalloon;
                if (moveChanged) {
                    if (insideBalloon && myFadeoutAlarm.getActiveRequestCount() > 0) {
                        //Pause hiding timer when mouse is hover
                        myFadeoutAlarm.cancelAllRequests();
                        myFadeoutRequestDelay -= System.currentTimeMillis() - myFadeoutRequestMillis;
                    }
                    if (!insideBalloon && myFadeoutRequestDelay > 0) {
                        startFadeoutTimer(myFadeoutRequestDelay);
                    }
                    myComp.repaintButton();
                }
            }

            if (myHideOnCloseClick && UIUtil.isCloseClick(me)) {
                if (isInsideBalloon(me)) {
                    hide();
                    me.consume();
                }
                return true;
            }
            return false;
        }

        private void onHideDefault(MouseEvent me, boolean forcedExit) {
            hide();
            if (forcedExit) {
                for (int id_ : FORCED_EXIT_DISPATCH_EVENT_IDS) {
                    IdeEventQueueProxy.getInstance().dispatchEvent(new MouseEvent(
                        me.getComponent(),
                        id_,
                        me.getWhen(),
                        me.getModifiers(),
                        me.getX(),
                        me.getY(),
                        me.getClickCount(),
                        me.isPopupTrigger(),
                        me.getButton()
                    ));
                }
            }
        }

        private void onKeyPressedEvent(KeyEvent ke) {
            if (!myHideOnKey && myHideListener == null) {
                return;
            }

            int keyCode = ke.getKeyCode();
            if (myHideListener != null) {
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    myHideListener.run();
                }
                return;
            }
            if (keyCode != KeyEvent.VK_SHIFT
                && keyCode != KeyEvent.VK_CONTROL
                && keyCode != KeyEvent.VK_ALT
                && keyCode != KeyEvent.VK_META) {
                // Close the balloon is ESC is pressed inside the balloon
                if ((keyCode == KeyEvent.VK_ESCAPE && SwingUtilities.isDescendingFrom(ke.getComponent(), myComp))
                    // Close the balloon if any key is pressed outside the balloon
                    || (myHideOnKey && !SwingUtilities.isDescendingFrom(ke.getComponent(), myComp))) {
                    hide();
                }
            }
        }

        private boolean isInsideBalloon(@Nonnull MouseEvent me) {
            return isInside(new RelativePoint(me));
        }
    }

    private static final Logger LOG = Logger.getInstance(BalloonImpl.class);

    private static final int[] FORCED_EXIT_DISPATCH_EVENT_IDS = {
        MouseEvent.MOUSE_ENTERED,
        MouseEvent.MOUSE_PRESSED,
        MouseEvent.MOUSE_RELEASED,
        MouseEvent.MOUSE_CLICKED
    };

    /**
     * This key is supposed to be used as client property of content component (with value Boolean.TRUE) to suppress shadow painting
     * when builder is being created indirectly and client cannot call its methods
     */
    public static final Key<Boolean> FORCED_NO_SHADOW = Key.create("BALLOON_FORCED_NO_SHADOW");

    private static final JBValue DIALOG_ARC = new JBValue.Float(6);
    public static final JBValue ARC = new JBValue.UIInteger("TextComponent.arc", 4);
    private static final JBValue DIALOG_TOPBOTTOM_POINTER_WIDTH = new JBValue.Float(24);
    public static final JBValue DIALOG_POINTER_WIDTH = new JBValue.Float(17);
    private static final JBValue TOPBOTTOM_POINTER_WIDTH = new JBValue.Float(14);
    private static final JBValue POINTER_WIDTH = new JBValue.Float(11);
    private static final JBValue DIALOG_TOPBOTTOM_POINTER_LENGTH = new JBValue.Float(16);
    private static final JBValue DIALOG_POINTER_LENGTH = new JBValue.Float(14);
    private static final JBValue TOPBOTTOM_POINTER_LENGTH = new JBValue.Float(10);
    public static final JBValue POINTER_LENGTH = new JBValue.Float(8);
    private static final JBValue BORDER_STROKE_WIDTH = new JBValue.Float(1);

    private final Alarm myFadeoutAlarm = new Alarm(this);
    private long myFadeoutRequestMillis;
    private int myFadeoutRequestDelay;

    private boolean mySmartFadeout;
    private boolean mySmartFadeoutPaused;
    private int mySmartFadeoutDelay;

    private MyComponent myComp;
    private JLayeredPane myLayeredPane;
    private AbstractPosition myPosition;
    private Point myTargetPoint;
    private final boolean myHideOnFrameResize;
    private final boolean myHideOnLinkClick;

    private final Color myBorderColor;
    private final Insets myBorderInsets;
    private Color myFillColor;
    private Color myPointerColor;

    private final Insets myContainerInsets;

    private boolean myLastMoveWasInsideBalloon;

    private Rectangle myForcedBounds;

    private ActionProvider myActionProvider;
    private List<ActionButton> myActionButtons;
    private boolean invalidateShadow;

    private final AWTEventListener myAwtActivityListener = new BalloonEventListener();

    private boolean isWithinChildWindow(@Nonnull MouseEvent event) {
        Component owner = UIUtil.getWindow(myContent);
        if (owner != null) {
            Component child = UIUtil.getWindow(event.getComponent());
            if (child != owner) {
                for (; child != null; child = child.getParent()) {
                    if (child == owner) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void setFillColor(Color fillColor) {
        myFillColor = fillColor;
    }

    public Color getPointerColor() {
        return myPointerColor;
    }

    public void setPointerColor(Color pointerColor) {
        myPointerColor = pointerColor;
    }

    private final long myFadeoutTime;
    private Dimension myDefaultPrefSize;
    private final ActionListener myClickHandler;
    private final boolean myCloseOnClick;
    private final int myShadowSize;
    private ShadowBorderProvider myShadowBorderProvider;

    private final Collection<JBPopupListener> myListeners = new CopyOnWriteArraySet<>();
    private boolean myVisible;
    private PositionTracker<Balloon> myTracker;
    private final int myAnimationCycle;

    private boolean myFadedIn;
    private boolean myFadedOut;
    private final int myCalloutShift;

    private final int myPositionChangeXShift;
    private final int myPositionChangeYShift;
    private boolean myDialogMode;
    private IdeFocusManager myFocusManager;
    private final String myTitle;
    private JLabel myTitleLabel;

    private boolean myAnimationEnabled = true;
    private final boolean myShadow;
    private final Layer myLayer;
    private final boolean myBlockClicks;
    private RelativePoint myPrevMousePoint;

    @Override
    public boolean isInside(@Nonnull RelativePoint target) {
        if (myComp == null) {
            return false;
        }
        Component cmp = target.getOriginalComponent();

        if (!cmp.isShowing()) {
            return true;
        }
        if (cmp instanceof MenuElement) {
            return false;
        }
        if (myActionButtons != null) {
            for (ActionButton button : myActionButtons) {
                if (cmp == button) {
                    return true;
                }
            }
        }
        if (UIUtil.isDescendingFrom(cmp, myComp)) {
            return true;
        }
        if (myComp == null || !myComp.isShowing()) {
            return false;
        }
        Point point = target.getScreenPoint();
        SwingUtilities.convertPointFromScreen(point, myComp);
        return myComp.contains(point);
    }

    @Override
    public boolean isMovingForward(@Nonnull RelativePoint target) {
        try {
            if (myComp == null || !myComp.isShowing()) {
                return false;
            }
            if (myPrevMousePoint == null) {
                return true;
            }
            if (myPrevMousePoint.getComponent() != target.getComponent()) {
                return false;
            }
            Rectangle rectangleOnScreen = new Rectangle(myComp.getLocationOnScreen(), myComp.getSize());
            return ScreenUtil.isMovementTowards(myPrevMousePoint.getScreenPoint(), target.getScreenPoint(), rectangleOnScreen);
        }
        finally {
            myPrevMousePoint = target;
        }
    }

    private final ComponentAdapter myComponentListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            if (myHideOnFrameResize) {
                hide();
            }
        }
    };
    private Animator myAnimator;
    private boolean myShowPointer;

    private boolean myDisposed;
    private final JComponent myContent;
    private boolean myHideOnMouse;
    private Runnable myHideListener;
    private final boolean myHideOnKey;
    private final boolean myHideOnAction;
    private final boolean myHideOnCloseClick;
    private final boolean myRequestFocus;
    private Component myOriginalFocusOwner;
    private final boolean myEnableButtons;
    private final Dimension myPointerSize;
    private boolean myPointerShiftedToStart;
    private final int myCornerToPointerDistance;

    public BalloonImpl(
        @Nonnull JComponent content,
        @Nonnull Color borderColor,
        Insets borderInsets,
        @Nonnull Color fillColor,
        boolean hideOnMouse,
        boolean hideOnKey,
        boolean hideOnAction,
        boolean hideOnCloseClick,
        boolean showPointer,
        boolean enableButtons,
        long fadeoutTime,
        boolean hideOnFrameResize,
        boolean hideOnLinkClick,
        ActionListener clickHandler,
        boolean closeOnClick,
        int animationCycle,
        int calloutShift,
        int positionChangeXShift,
        int positionChangeYShift,
        boolean dialogMode,
        String title,
        Insets contentInsets,
        boolean shadow,
        boolean smallVariant,
        boolean blockClicks,
        Layer layer,
        boolean requestFocus,
        Dimension pointerSize,
        int cornerToPointerDistance
    ) {
        myBorderColor = borderColor;
        myBorderInsets = borderInsets != null ? borderInsets : JBInsets.create(5, 8);
        myFillColor = fillColor;
        myContent = content;
        myHideOnMouse = hideOnMouse;
        myHideOnKey = hideOnKey;
        myHideOnAction = hideOnAction;
        myHideOnCloseClick = hideOnCloseClick;
        myShowPointer = showPointer;
        myEnableButtons = enableButtons;
        myHideOnFrameResize = hideOnFrameResize;
        myHideOnLinkClick = hideOnLinkClick;
        myClickHandler = clickHandler;
        myCloseOnClick = closeOnClick;
        myCalloutShift = calloutShift;
        myPositionChangeXShift = positionChangeXShift;
        myPositionChangeYShift = positionChangeYShift;
        myDialogMode = dialogMode;
        myTitle = title;
        myLayer = layer != null ? layer : Layer.normal;
        myBlockClicks = blockClicks;
        myRequestFocus = requestFocus;
        MnemonicHelper.init(content);

        if (!myDialogMode) {
            for (Component component : UIUtil.uiTraverser(myContent)) {
                if (component instanceof JLabel label) {
                    if (label.getDisplayedMnemonic() != '\0' || label.getDisplayedMnemonicIndex() >= 0) {
                        myDialogMode = true;
                        break;
                    }
                }
                else if (component instanceof JCheckBox checkBox) {
                    if (checkBox.getMnemonic() >= 0 || checkBox.getDisplayedMnemonicIndex() >= 0) {
                        myDialogMode = true;
                        break;
                    }
                }
            }
        }

        myShadow = shadow;
        myShadowSize = Registry.intValue("ide.balloon.shadow.size");
        myContainerInsets = contentInsets;

        myFadeoutTime = fadeoutTime;
        myAnimationCycle = animationCycle;

        myPointerSize = pointerSize;
        myCornerToPointerDistance = cornerToPointerDistance;

        if (smallVariant) {
            for (Component component : UIUtil.uiTraverser(myContent)) {
                UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, component);
            }
        }
    }

    @Override
    public void show(RelativePoint target, Position position) {
        show(target, AbstractPosition.of(position));
    }

    public int getLayer() {
        return switch (myLayer) {
            case normal -> JLayeredPane.POPUP_LAYER;
            case top -> JLayeredPane.DRAG_LAYER;
            default -> JLayeredPane.DEFAULT_LAYER;
        };
    }

    @Override
    public void show(PositionTracker<Balloon> tracker, Position position) {
        show(tracker, AbstractPosition.of(position));
    }

    private Insets getInsetsCopy() {
        return JBUI.insets(myBorderInsets);
    }

    private void show(RelativePoint target, AbstractPosition position) {
        show(new PositionTracker.Static<>(target), position);
    }

    private void show(PositionTracker<Balloon> tracker, AbstractPosition position) {
        assert !myDisposed : "Balloon is already disposed";

        if (isVisible()) {
            return;
        }
        Component comp = tracker.getComponent();
        if (!comp.isShowing()) {
            return;
        }

        myTracker = tracker;
        myTracker.init(this);

        JRootPane root = ObjectUtil.notNull(UIUtil.getRootPane(comp));

        myVisible = true;

        myLayeredPane = root.getLayeredPane();
        myPosition = position;
        UIUtil.setFutureRootPane(myContent, root);

        myFocusManager = IdeFocusManager.findInstanceByComponent(myLayeredPane);
        final SimpleReference<Component> originalFocusOwner = new SimpleReference<>();
        final SimpleReference<ActionCallback> proxyFocusRequest = new SimpleReference<>(ActionCallback.DONE);

        boolean mnemonicsFix = myDialogMode && Platform.current().os().isMac() && Registry.is("ide.mac.inplaceDialogMnemonicsFix");
        if (mnemonicsFix) {
            proxyFocusRequest.set(new ActionCallback());

            myFocusManager.doWhenFocusSettlesDown(new ExpirableRunnable() {
                @Override
                public boolean isExpired() {
                    return isDisposed();
                }

                @Override
                public void run() {
                    IdeEventQueueProxy.getInstance().disableInputMethods(BalloonImpl.this);
                    originalFocusOwner.set(myFocusManager.getFocusOwner());
                }
            });
        }
        myLayeredPane.addComponentListener(myComponentListener);

        myTargetPoint = myPosition.getShiftedPoint(myTracker.recalculateLocation(this).getPoint(myLayeredPane), myCalloutShift);
        if (myDisposed) {
            return; //tracker may dispose the balloon
        }

        int positionChangeFix = 0;
        if (myShowPointer) {
            Rectangle rec = getRecForPosition(myPosition, true);

            if (!myPosition.createDimensions(this, rec, myTargetPoint).isOkToHavePointer()) {
                rec = getRecForPosition(myPosition, false);

                Rectangle lp = new Rectangle(new Point(myContainerInsets.left, myContainerInsets.top), myLayeredPane.getSize());
                lp.width -= myContainerInsets.right;
                lp.height -= myContainerInsets.bottom;

                if (!lp.contains(rec) || !PopupLocationTracker.canRectangleBeUsed(myLayeredPane, rec, this)) {
                    Rectangle2D currentSquare = lp.createIntersection(rec);

                    double maxSquare = currentSquare.getWidth() * currentSquare.getHeight();
                    AbstractPosition targetPosition = myPosition;

                    for (AbstractPosition eachPosition : myPosition.getOtherPositions()) {
                        Rectangle2D eachIntersection = lp.createIntersection(getRecForPosition(eachPosition, false));
                        double eachSquare = eachIntersection.getWidth() * eachIntersection.getHeight();
                        if (maxSquare < eachSquare) {
                            maxSquare = eachSquare;
                            targetPosition = eachPosition;
                        }
                    }

                    myPosition = targetPosition;
                    positionChangeFix = myPosition.getChangeShift(position, myPositionChangeXShift, myPositionChangeYShift);
                }
            }
        }

        if (myPosition != position) {
            myTargetPoint = myPosition.getShiftedPoint(
                myTracker.recalculateLocation(this).getPoint(myLayeredPane),
                myCalloutShift > 0 ? myCalloutShift + positionChangeFix : positionChangeFix
            );
            position = myPosition;
        }

        createComponent();
        Rectangle r = getRecForPosition(myPosition, false);
        Point location = r.getLocation();
        SwingUtilities.convertPointToScreen(location, myLayeredPane);
        r.setLocation(location);
        if (!PopupLocationTracker.canRectangleBeUsed(myLayeredPane, r, this)) {
            for (AbstractPosition eachPosition : myPosition.getOtherPositions()) {
                r = getRecForPosition(eachPosition, false);
                location = r.getLocation();
                SwingUtilities.convertPointToScreen(location, myLayeredPane);
                r.setLocation(location);
                if (PopupLocationTracker.canRectangleBeUsed(myLayeredPane, r, this)) {
                    myPosition = eachPosition;
                    positionChangeFix = myPosition.getChangeShift(position, myPositionChangeXShift, myPositionChangeYShift);
                    myTargetPoint = myPosition.getShiftedPoint(
                        myTracker.recalculateLocation(this).getPoint(myLayeredPane),
                        myCalloutShift > 0 ? myCalloutShift + positionChangeFix : positionChangeFix
                    );
                    myPosition.updateBounds(this);
                    break;
                }
            }
        }

        myComp.validate();

        Rectangle rec = myComp.getContentBounds();


        if (myShowPointer
            && !myPosition.createDimensions(this, rec, myTargetPoint).isOkToHavePointer()) {
            myShowPointer = false;
            myComp.removeAll();
            myLayeredPane.remove(myComp);

            createComponent();
            Dimension availSpace = myLayeredPane.getSize();
            Dimension reqSpace = myComp.getSize();
            if (!new Rectangle(availSpace).contains(new Rectangle(reqSpace))) {
                // Balloon is bigger than window, don't show it at all.
                LOG.warn(
                    "Not enough space to show: " +
                        "required [" + reqSpace.width + " x " + reqSpace.height + "], " +
                        "available [" + availSpace.width + " x " + availSpace.height + "]"
                );
                myComp.removeAll();
                myLayeredPane.remove(myComp);
                myLayeredPane = null;
                hide();
                return;
            }
        }

        for (JBPopupListener each : myListeners) {
            each.beforeShown(new LightweightWindowEvent(this));
        }

        if (isAnimationEnabled()) {
            runAnimation(true, myLayeredPane, null);
        }

        myLayeredPane.revalidate();
        myLayeredPane.repaint();

        if (myRequestFocus) {
            myFocusManager.doWhenFocusSettlesDown(new ExpirableRunnable() {
                @Override
                public boolean isExpired() {
                    return isDisposed();
                }

                @Override
                public void run() {
                    myOriginalFocusOwner = myFocusManager.getFocusOwner();

                    // Set the accessible parent so that screen readers don't announce
                    // a window context change -- the tooltip is "logically" hosted
                    // inside the component (e.g. editor) it appears on top of.
                    AccessibleContextUtil.setParent((Component)myContent, myOriginalFocusOwner);

                    // Set the focus to "myContent"
                    myFocusManager.requestFocus(getContentToFocus(), true);
                }
            });
        }

        if (mnemonicsFix) {
            proxyFocusRequest.get().doWhenDone(() -> myFocusManager.requestFocus(originalFocusOwner.get(), true));
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(
            myAwtActivityListener,
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK
        );

        if (ApplicationManager.getApplication() != null) {
            ApplicationManager.getApplication().getMessageBus()
                .connect(this)
                .subscribe(
                    AnActionListener.class,
                    new AnActionListener() {
                        @Override
                        public void beforeActionPerformed(
                            @Nonnull AnAction action,
                            @Nonnull DataContext dataContext,
                            @Nonnull AnActionEvent event
                        ) {
                            if (myHideOnAction && !(action instanceof HintManagerImpl.ActionToIgnore)) {
                                hide();
                            }
                        }
                    }
                );
        }

        if (myHideOnLinkClick) {
            JEditorPane editorPane = UIUtil.uiTraverser(myContent).traverse().filter(JEditorPane.class).first();
            if (editorPane != null) {
                editorPane.addHyperlinkListener(new HyperlinkAdapter() {
                    @Override
                    protected void hyperlinkActivated(HyperlinkEvent e) {
                        hide();
                    }
                });
            }
        }
    }

    /**
     * Figure out the component to focus inside the {@link #myContent} field.
     */
    @Nonnull
    private Component getContentToFocus() {
        Component focusComponent = myContent;
        FocusTraversalPolicy policy = myContent.getFocusTraversalPolicy();
        if (policy instanceof SortingFocusTraversalPolicy sortingFocusTraversalPolicy
            && sortingFocusTraversalPolicy.getImplicitDownCycleTraversal()) {
            focusComponent = policy.getDefaultComponent(myContent);
        }
        while (true) {
            // Setting focus to a JScrollPane is not very useful. Better setting focus to the
            // contained view. This is useful for Tooltip popups, for example.
            if (focusComponent instanceof JScrollPane scrollPane) {
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null) {
                    break;
                }
                Component child = viewport.getView();
                if (child == null) {
                    break;
                }
                focusComponent = child;
                continue;
            }

            // Done if we can't find anything to dive into
            break;
        }
        return focusComponent;
    }

    private Rectangle getRecForPosition(AbstractPosition position, boolean adjust) {
        Dimension size = getContentSizeFor(position);
        Rectangle rec = new Rectangle(new Point(0, 0), size);

        position.setRecToRelativePosition(rec, myTargetPoint);

        if (adjust) {
            rec = myPosition.getUpdatedBounds(this, rec.getSize(), getShadowBorderInsets());
        }
        else {
            JBInsets.addTo(rec, getShadowBorderInsets());
        }

        return rec;
    }

    private Dimension getContentSizeFor(AbstractPosition position) {
        Dimension size = myContent.getPreferredSize();
        if (myShadowBorderProvider == null) {
            JBInsets.addTo(size, position.createBorder(this).getBorderInsets());
        }
        return size;
    }

    private void disposeButton(ActionButton button) {
        if (button != null && button.getParent() != null) {
            Container parent = button.getParent();
            parent.remove(button);
            //noinspection RedundantCast
            ((JComponent)parent).revalidate();
            parent.repaint();
        }
    }

    public JComponent getContent() {
        return myContent;
    }

    @Override
    public JComponent getComponent() {
        return myComp;
    }

    private void createComponent() {
        myComp = new MyComponent(
            myContent,
            this,
            myShadowBorderProvider != null ? null
                : myShowPointer ? myPosition.createBorder(this) : getPointlessBorder()
        );

        if (myActionProvider == null) {
            final Consumer<MouseEvent> listener = event -> SwingUtilities.invokeLater(this::hide);

            myActionProvider = new ActionProvider() {
                private ActionButton myCloseButton;

                @Nonnull
                @Override
                public List<ActionButton> createActions() {
                    myCloseButton = new CloseButton(listener);
                    return Collections.singletonList(myCloseButton);
                }

                @Override
                public void layout(@Nonnull Rectangle lpBounds) {
                    if (!myCloseButton.isVisible()) {
                        return;
                    }

                    consulo.ui.image.Image icon = getCloseButton();
                    int iconWidth = icon.getWidth();
                    int iconHeight = icon.getHeight();
                    Insets borderInsets = getShadowBorderInsets();

                    myCloseButton.setBounds(
                        lpBounds.x + lpBounds.width - iconWidth - borderInsets.right - JBUIScale.scale(8),
                        lpBounds.y + borderInsets.top + JBUIScale.scale(6),
                        iconWidth,
                        iconHeight
                    );
                }
            };
        }

        myComp.clear();
        myComp.myAlpha = isAnimationEnabled() ? 0f : -1;

        myComp.setBorder(new EmptyBorder(getShadowBorderInsets()));

        myLayeredPane.add(myComp);
        myLayeredPane.setLayer(myComp, getLayer(), 0); // the second balloon must be over the first one
        myPosition.updateBounds(this);

        PopupLocationTracker.register(this);

        if (myBlockClicks) {
            myComp.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    e.consume();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    e.consume();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    e.consume();
                }
            });
        }
    }

    @Nonnull
    @Override
    public Rectangle getConsumedScreenBounds() {
        Rectangle bounds = myComp.getBounds();
        Point location = bounds.getLocation();
        SwingUtilities.convertPointToScreen(location, myLayeredPane);
        bounds.setLocation(location);
        return bounds;
    }

    @Override
    public Window getUnderlyingWindow() {
        return UIUtil.getWindow(myLayeredPane);
    }

    @Nonnull
    private EmptyBorder getPointlessBorder() {
        return new EmptyBorder(myBorderInsets);
    }

    @Override
    public void revalidate() {
        revalidate(myTracker);
    }

    @Override
    public void revalidate(@Nonnull PositionTracker<Balloon> tracker) {
        if (ApplicationManager.getApplication().isDisposeInProgress()) {
            return;
        }

        RelativePoint newPosition = tracker.recalculateLocation(this);

        if (newPosition != null) {
            Point newPoint = myPosition.getShiftedPoint(newPosition.getPoint(myLayeredPane), myCalloutShift);
            invalidateShadow = !Objects.equals(myTargetPoint, newPoint);
            myTargetPoint = newPoint;
            myPosition.updateBounds(this);
        }
    }

    public void setShadowBorderProvider(@Nonnull ShadowBorderProvider provider) {
        myShadowBorderProvider = provider;
    }

    private int getShadowBorderSize() {
        return hasShadow() ? myShadowSize : 0;
    }

    @Nonnull
    public Insets getShadowBorderInsets() {
        if (myShadowBorderProvider != null) {
            return myShadowBorderProvider.getInsets();
        }
        return JBUI.insets(getShadowBorderSize());
    }

    public boolean hasShadow() {
        return myShadowBorderProvider != null || myShadow && Registry.is("ide.balloon.shadowEnabled");
    }

    public interface ShadowBorderProvider {
        @Nonnull
        Insets getInsets();

        void paintShadow(@Nonnull JComponent component, @Nonnull Graphics g);

        void paintBorder(@Nonnull Rectangle bounds, @Nonnull Graphics2D g);

        void paintPointingShape(@Nonnull Rectangle bounds, @Nonnull Point pointTarget, @Nonnull Position position, @Nonnull Graphics2D g);
    }

    @Override
    public void show(JLayeredPane pane) {
        show(pane, null);
    }

    @Override
    public void showInCenterOf(JComponent component) {
        Dimension size = component.getSize();
        show(new RelativePoint(component, new Point(size.width / 2, size.height / 2)), Position.above);
    }

    public void show(JLayeredPane pane, @Nullable Rectangle bounds) {
        if (bounds != null) {
            myForcedBounds = bounds;
        }
        show(new RelativePoint(pane, new Point(0, 0)), Position.above);
    }

    private void runAnimation(boolean forward, final JLayeredPane layeredPane, @Nullable final Runnable onDone) {
        if (myAnimator != null) {
            Disposer.dispose(myAnimator);
        }

        myAnimator = new Animator("Balloon", 8, isAnimationEnabled() ? myAnimationCycle : 0, false, forward) {
            @Override
            public void paintNow(int frame, int totalFrames, int cycle) {
                if (myComp == null || myComp.getParent() == null || !isAnimationEnabled()) {
                    return;
                }
                myComp.setAlpha((float)frame / totalFrames);
            }

            @Override
            protected void paintCycleEnd() {
                if (myComp == null || myComp.getParent() == null) {
                    return;
                }

                if (isForward()) {
                    myComp.clear();
                    myComp.repaint();

                    myFadedIn = true;

                    if (!myFadeoutAlarm.isDisposed()) {
                        startFadeoutTimer((int)myFadeoutTime);
                    }
                }
                else {
                    layeredPane.remove(myComp);
                    layeredPane.revalidate();
                    layeredPane.repaint();
                }
                Disposer.dispose(this);
            }

            @Override
            public void dispose() {
                super.dispose();
                myAnimator = null;
                if (onDone != null) {
                    onDone.run();
                }
            }
        };

        myAnimator.resume();
    }

    public void runWithSmartFadeoutPause(@Nonnull Runnable handler) {
        if (mySmartFadeout) {
            mySmartFadeoutPaused = true;
            handler.run();
            if (mySmartFadeoutPaused) {
                mySmartFadeoutPaused = false;
            }
            else {
                setAnimationEnabled(true);
                hide();
            }
        }
        else {
            handler.run();
        }
    }

    public void startSmartFadeoutTimer(int delay) {
        mySmartFadeout = true;
        mySmartFadeoutDelay = delay;
        FrameStateManager.getInstance().addListener(
            new FrameStateListener() {
                @Override
                public void onFrameDeactivated() {
                    if (myFadeoutAlarm.getActiveRequestCount() > 0) {
                        myFadeoutAlarm.cancelAllRequests();
                        mySmartFadeoutDelay = myFadeoutRequestDelay - (int)(System.currentTimeMillis() - myFadeoutRequestMillis);
                        if (mySmartFadeoutDelay <= 0) {
                            mySmartFadeoutDelay = 1;
                        }
                    }
                }
            },
            this
        );
    }

    public void startFadeoutTimer(int fadeoutDelay) {
        if (fadeoutDelay > 0) {
            myFadeoutAlarm.cancelAllRequests();
            myFadeoutRequestMillis = System.currentTimeMillis();
            myFadeoutRequestDelay = fadeoutDelay;
            myFadeoutAlarm.addRequest(
                () -> {
                    if (mySmartFadeout) {
                        setAnimationEnabled(true);
                    }
                    hide();
                },
                fadeoutDelay,
                null
            );
        }
    }

    private int getArc() {
        return myDialogMode ? DIALOG_ARC.get() : ARC.get();
    }

    private int getPointerWidth(AbstractPosition position) {
        return myPointerSize == null || myPointerSize.width <= 0 ? position.getPointerWidth(myDialogMode) : myPointerSize.width;
    }

    private int getPointerLength(AbstractPosition position) {
        return myPointerSize == null || myPointerSize.height <= 0 ? position.getPointerLength(myDialogMode) : myPointerSize.height;
    }

    public static int getPointerLength(@Nonnull Position position, boolean dialogMode) {
        return AbstractPosition.of(position).getPointerLength(dialogMode);
    }

    @Override
    public void hide() {
        hide(false);
    }

    @Override
    public void hide(boolean ok) {
        hideAndDispose(ok);
    }

    @Override
    public void dispose() {
        hideAndDispose(false);
    }

    private void hideAndDispose(boolean ok) {
        if (myDisposed) {
            return;
        }

        if (mySmartFadeoutPaused) {
            mySmartFadeoutPaused = false;
            return;
        }

        myDisposed = true;
        hideComboBoxPopups();

        Runnable disposeRunnable = () -> {
            myFadedOut = true;
            if (myRequestFocus) {
                if (myOriginalFocusOwner != null) {
                    myFocusManager.requestFocus(myOriginalFocusOwner, false);
                }
            }

            for (JBPopupListener each : myListeners) {
                each.onClosed(new LightweightWindowEvent(this, ok));
            }

            Disposer.dispose(this);
            onDisposed();
        };

        Toolkit.getDefaultToolkit().removeAWTEventListener(myAwtActivityListener);
        if (myLayeredPane != null) {
            myLayeredPane.removeComponentListener(myComponentListener);

            if (isAnimationEnabled()) {
                runAnimation(false, myLayeredPane, disposeRunnable);
            }
            else {
                if (myAnimator != null) {
                    Disposer.dispose(myAnimator);
                }

                myLayeredPane.remove(myComp);
                myLayeredPane.revalidate();
                myLayeredPane.repaint();
                disposeRunnable.run();
            }
        }
        else {
            disposeRunnable.run();
        }

        myVisible = false;
        myTracker = null;
    }

    private void hideComboBoxPopups() {
        List<JComboBox> comboBoxes = UIUtil.findComponentsOfType(myComp, JComboBox.class);
        for (JComboBox box : comboBoxes) {
            box.hidePopup();
        }
    }

    private void onDisposed() {
    }

    @Override
    public void addListener(@Nonnull JBPopupListener listener) {
        myListeners.add(listener);
    }

    public boolean isVisible() {
        return myVisible;
    }

    public void setHideOnClickOutside(boolean hideOnMouse) {
        myHideOnMouse = hideOnMouse;
    }

    public void setHideListener(@Nonnull Runnable listener) {
        myHideListener = listener;
        myHideOnMouse = true;
    }

    public void setShowPointer(boolean show) {
        myShowPointer = show;
    }

    public void setPointerShiftedToStart(boolean pointerShiftedToStart) {
        myPointerShiftedToStart = pointerShiftedToStart;
    }

    public consulo.ui.image.Image getCloseButton() {
        return PlatformIconGroup.ideNotificationClose();
    }

    @Override
    public void setBounds(Rectangle bounds) {
        myForcedBounds = bounds;
        if (myPosition != null) {
            myPosition.updateBounds(this);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (myComp != null) {
            return myComp.getPreferredSize();
        }
        if (myDefaultPrefSize == null) {
            EmptyBorder border = myShadowBorderProvider == null ? getPointlessBorder() : null;
            MyComponent c = new MyComponent(myContent, this, border);

            c.setBorder(new EmptyBorder(getShadowBorderInsets()));
            myDefaultPrefSize = c.getPreferredSize();
        }
        return myDefaultPrefSize;
    }

    private abstract static class AbstractPosition {
        private static final AbstractPosition BELOW = new Below();
        private static final AbstractPosition ABOVE = new Above();
        private static final AbstractPosition AT_RIGHT = new AtRight();
        private static final AbstractPosition AT_LEFT = new AtLeft();

        private static AbstractPosition of(@Nonnull Position position) {
            return switch (position) {
                case atLeft -> AT_LEFT;
                case atRight -> AT_RIGHT;
                case above -> ABOVE;
                default -> BELOW;
            };
        }

        @Nonnull
        abstract Position getPosition();

        abstract EmptyBorder createBorder(BalloonImpl balloon);

        public BalloonDimensions createDimensions(BalloonImpl balloon, Rectangle bounds, Point pointTarget) {
            return balloon.new BalloonDimensions(this, bounds, pointTarget);
        }

        abstract void setRecToRelativePosition(Rectangle rec, Point targetPoint);

        abstract int getChangeShift(AbstractPosition original, int xShift, int yShift);

        public void updateBounds(@Nonnull BalloonImpl balloon) {
            if (balloon.myLayeredPane == null || balloon.myComp == null) {
                return;
            }

            Insets shadow = balloon.myComp.getInsets();
            Dimension prefSize = balloon.myComp.getPreferredSize();
            JBInsets.removeFrom(prefSize, shadow);
            Rectangle bounds = getUpdatedBounds(balloon, prefSize, shadow);

            if (balloon.myShadowBorderProvider == null && balloon.myForcedBounds != null) {
                bounds = new Rectangle(getShiftedPoint(bounds.getLocation(), balloon.getShadowBorderInsets()), bounds.getSize());
            }
            balloon.myComp._setBounds(bounds);
        }

        /**
         * @param contentSize size without shadow insets
         * @return adjusted size with shadow insets
         */
        @Nonnull
        public Rectangle getUpdatedBounds(BalloonImpl balloon, Dimension contentSize, Insets shadowInsets) {
            Dimension layeredPaneSize = balloon.myLayeredPane.getSize();
            Point point = balloon.myTargetPoint;

            Rectangle bounds = balloon.myForcedBounds;
            if (bounds == null) {
                int distance = getDistance(balloon, contentSize);
                Point location = balloon.myShowPointer
                    ? getLocation(layeredPaneSize, point, contentSize, distance)
                    // Now distance is used for pointer enabled balloons only
                    : new Point(point.x - contentSize.width / 2, point.y - contentSize.height / 2);
                if (balloon.myShowPointer && balloon.myPointerShiftedToStart) {
                    int offset = JBUI.scale(20);
                    if (isTopBottomPointer()) {
                        if (contentSize.width / 2 > offset) {
                            location.x = point.x - offset;
                        }
                    }
                    else {
                        if (contentSize.height / 2 > offset) {
                            location.y = point.y - offset;
                        }
                    }
                }
                bounds = new Rectangle(location.x, location.y, contentSize.width, contentSize.height);

                JBInsets.addTo(bounds, shadowInsets);
                ScreenUtil.moveToFit(
                    bounds,
                    new Rectangle(0, 0, layeredPaneSize.width, layeredPaneSize.height),
                    balloon.myContainerInsets,
                    true
                );
            }

            return bounds;
        }

        private int getDistance(@Nonnull BalloonImpl balloon, @Nonnull Dimension size) {
            if (balloon.myCornerToPointerDistance < 0) {
                return -1;
            }

            int indent = balloon.getArc() + balloon.getPointerWidth(this) / 2;
            if (balloon.myCornerToPointerDistance < indent) {
                return indent;
            }

            int limit = isTopBottomPointer() ? size.width - indent : size.height - indent;
            if (balloon.myCornerToPointerDistance > limit) {
                return limit;
            }

            return balloon.myCornerToPointerDistance;
        }

        abstract Point getLocation(Dimension containerSize, Point targetPoint, Dimension balloonSize, int distance);

        protected abstract Insets getTitleInsets(int normalInset, int pointerLength);

        boolean isOkToHavePointer(@Nonnull Point targetPoint, @Nonnull Rectangle bounds, int pointerLength, int pointerWidth, int arc) {
            if (bounds.x < targetPoint.x
                && bounds.x + bounds.width > targetPoint.x
                && bounds.y < targetPoint.y
                && bounds.y + bounds.height > targetPoint.y) {
                return false;
            }

            Rectangle pointless = getPointlessContentRec(bounds, pointerLength);

            int distance = getDistanceToTarget(pointless, targetPoint);
            if (distance < pointerLength - 1 || distance > 2 * pointerLength) {
                return false;
            }

            UnfairTextRange balloonRange;
            UnfairTextRange pointerRange;
            if (isTopBottomPointer()) {
                balloonRange = new UnfairTextRange(bounds.x + arc - 1, bounds.x + bounds.width - arc * 2 + 1);
                pointerRange = new UnfairTextRange(targetPoint.x - pointerWidth / 2, targetPoint.x + pointerWidth / 2);
            }
            else {
                balloonRange = new UnfairTextRange(bounds.y + arc - 1, bounds.y + bounds.height - arc * 2 + 1);
                pointerRange = new UnfairTextRange(targetPoint.y - pointerWidth / 2, targetPoint.y + pointerWidth / 2);
            }
            return balloonRange.contains(pointerRange);
        }

        protected abstract int getDistanceToTarget(Rectangle rectangle, Point targetPoint);

        boolean isTopBottomPointer() {
            return false;
        }

        protected abstract Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength);

        @Nonnull
        Set<AbstractPosition> getOtherPositions() {
            Set<AbstractPosition> all = new LinkedHashSet<>();
            all.add(BELOW);
            all.add(ABOVE);
            all.add(AT_RIGHT);
            all.add(AT_LEFT);

            all.remove(this);

            return all;
        }

        @Nonnull
        public abstract Point getShiftedPoint(@Nonnull Point targetPoint, int shift);

        @Nonnull
        public abstract Point getShiftedPoint(@Nonnull Point targetPoint, @Nonnull Insets shift);

        public int getPointerLength(boolean dialogMode) {
            if (dialogMode) {
                return isTopBottomPointer() ? DIALOG_TOPBOTTOM_POINTER_LENGTH.get() : DIALOG_POINTER_LENGTH.get();
            }
            else {
                return isTopBottomPointer() ? TOPBOTTOM_POINTER_LENGTH.get() : POINTER_LENGTH.get();
            }
        }

        public int getPointerWidth(boolean dialogMode) {
            if (dialogMode) {
                return isTopBottomPointer() ? DIALOG_TOPBOTTOM_POINTER_WIDTH.get() : DIALOG_POINTER_WIDTH.get();
            }
            else {
                return isTopBottomPointer() ? TOPBOTTOM_POINTER_WIDTH.get() : POINTER_WIDTH.get();
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    private static class Below extends AbstractPosition {
        @Nonnull
        @Override
        Position getPosition() {
            return Position.below;
        }

        @Override
        boolean isTopBottomPointer() {
            return true;
        }

        @Nonnull
        @Override
        public Point getShiftedPoint(@Nonnull Point targetPoint, int shift) {
            return new Point(targetPoint.x, targetPoint.y + shift);
        }

        @Nonnull
        @Override
        public Point getShiftedPoint(@Nonnull Point targetPoint, @Nonnull Insets shift) {
            return getShiftedPoint(targetPoint, -shift.top);
        }

        @Override
        int getChangeShift(AbstractPosition original, int xShift, int yShift) {
            return original.getPosition() == Position.above ? yShift : 0;
        }

        @Override
        protected int getDistanceToTarget(Rectangle rectangle, Point targetPoint) {
            return rectangle.y - targetPoint.y;
        }

        @Override
        protected Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength) {
            return new Rectangle(bounds.x, bounds.y + pointerLength, bounds.width, bounds.height - pointerLength);
        }

        @Override
        EmptyBorder createBorder(BalloonImpl balloon) {
            Insets insets = balloon.getInsetsCopy();
            insets.top += balloon.getPointerLength(this);
            return new EmptyBorder(insets);
        }

        @Override
        void setRecToRelativePosition(Rectangle rec, Point targetPoint) {
            rec.setLocation(new Point(targetPoint.x - rec.width / 2, targetPoint.y));
        }

        @Override
        Point getLocation(Dimension containerSize, Point targetPoint, Dimension balloonSize, int distance) {
            if (distance > 0) {
                return new Point(targetPoint.x - distance, targetPoint.y);
            }
            else {
                Point center = UIUtil.getCenterPoint(new Rectangle(targetPoint, JBUI.emptySize()), balloonSize);
                return new Point(center.x, targetPoint.y);
            }
        }

        @Override
        protected Insets getTitleInsets(int normalInset, int pointerLength) {
            //noinspection UseDPIAwareInsets
            return new Insets(pointerLength, JBUIScale.scale(normalInset), JBUIScale.scale(normalInset), JBUIScale.scale(normalInset));
        }
    }

    private static class Above extends AbstractPosition {
        @Nonnull
        @Override
        Position getPosition() {
            return Position.above;
        }

        @Override
        boolean isTopBottomPointer() {
            return true;
        }

        @Nonnull
        @Override
        public Point getShiftedPoint(@Nonnull Point targetPoint, int shift) {
            return new Point(targetPoint.x, targetPoint.y - shift);
        }

        @Nonnull
        @Override
        public Point getShiftedPoint(@Nonnull Point targetPoint, @Nonnull Insets shift) {
            return getShiftedPoint(targetPoint, -shift.top);
        }

        @Override
        int getChangeShift(AbstractPosition original, int xShift, int yShift) {
            return original.getPosition() == Position.below ? -yShift : 0;
        }

        @Override
        protected int getDistanceToTarget(Rectangle rectangle, Point targetPoint) {
            return targetPoint.y - (int)rectangle.getMaxY();
        }

        @Override
        protected Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength) {
            return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height - pointerLength);
        }

        @Override
        EmptyBorder createBorder(BalloonImpl balloon) {
            Insets insets = balloon.getInsetsCopy();
            insets.bottom += balloon.getPointerLength(this);
            return new EmptyBorder(insets);
        }

        @Override
        void setRecToRelativePosition(Rectangle rec, Point targetPoint) {
            rec.setLocation(targetPoint.x - rec.width / 2, targetPoint.y - rec.height);
        }

        @Override
        Point getLocation(Dimension containerSize, Point targetPoint, Dimension balloonSize, int distance) {
            if (distance > 0) {
                return new Point(targetPoint.x - distance, targetPoint.y - balloonSize.height);
            }
            else {
                Point center = UIUtil.getCenterPoint(new Rectangle(targetPoint, JBUI.emptySize()), balloonSize);
                return new Point(center.x, targetPoint.y - balloonSize.height);
            }
        }

        @Override
        protected Insets getTitleInsets(int normalInset, int pointerLength) {
            return JBUI.insets(normalInset, normalInset, normalInset, normalInset);
        }
    }

    private static class AtRight extends AbstractPosition {
        @Nonnull
        @Override
        Position getPosition() {
            return Position.atRight;
        }

        @Nonnull
        @Override
        public Point getShiftedPoint(@Nonnull Point targetPoint, int shift) {
            return new Point(targetPoint.x + shift, targetPoint.y);
        }

        @Nonnull
        @Override
        public Point getShiftedPoint(@Nonnull Point targetPoint, @Nonnull Insets shift) {
            return getShiftedPoint(targetPoint, -shift.left);
        }

        @Override
        int getChangeShift(AbstractPosition original, int xShift, int yShift) {
            return original.getPosition() == Position.atLeft ? xShift : 0;
        }

        @Override
        protected int getDistanceToTarget(Rectangle rectangle, Point targetPoint) {
            return rectangle.x - targetPoint.x;
        }

        @Override
        protected Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength) {
            return new Rectangle(bounds.x + pointerLength, bounds.y, bounds.width - pointerLength, bounds.height);
        }

        @Override
        EmptyBorder createBorder(BalloonImpl balloon) {
            Insets insets = balloon.getInsetsCopy();
            insets.left += balloon.getPointerLength(this);
            return new EmptyBorder(insets);
        }

        @Override
        void setRecToRelativePosition(Rectangle rec, Point targetPoint) {
            rec.setLocation(targetPoint.x, targetPoint.y - rec.height / 2);
        }

        @Override
        Point getLocation(Dimension containerSize, Point targetPoint, Dimension balloonSize, int distance) {
            if (distance > 0) {
                return new Point(targetPoint.x, targetPoint.y - distance);
            }
            else {
                Point center = UIUtil.getCenterPoint(new Rectangle(targetPoint, JBUI.emptySize()), balloonSize);
                return new Point(targetPoint.x, center.y);
            }
        }

        @Override
        protected Insets getTitleInsets(int normalInset, int pointerLength) {
            //noinspection UseDPIAwareInsets
            return new Insets(JBUIScale.scale(normalInset), pointerLength, JBUIScale.scale(normalInset), JBUIScale.scale(normalInset));
        }
    }

    private static class AtLeft extends AbstractPosition {
        @Nonnull
        @Override
        Position getPosition() {
            return Position.atLeft;
        }

        @Nonnull
        @Override
        public Point getShiftedPoint(@Nonnull Point targetPoint, int shift) {
            return new Point(targetPoint.x - shift, targetPoint.y);
        }

        @Nonnull
        @Override
        public Point getShiftedPoint(@Nonnull Point targetPoint, @Nonnull Insets shift) {
            return getShiftedPoint(targetPoint, -shift.left);
        }

        @Override
        int getChangeShift(AbstractPosition original, int xShift, int yShift) {
            return original.getPosition() == Position.atRight ? -xShift : 0;
        }

        @Override
        protected int getDistanceToTarget(Rectangle rectangle, Point targetPoint) {
            return targetPoint.x - (int)rectangle.getMaxX();
        }

        @Override
        protected Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength) {
            return new Rectangle(bounds.x, bounds.y, bounds.width - pointerLength, bounds.height);
        }

        @Override
        EmptyBorder createBorder(BalloonImpl balloon) {
            Insets insets = balloon.getInsetsCopy();
            insets.right += balloon.getPointerLength(this);
            return new EmptyBorder(insets);
        }

        @Override
        void setRecToRelativePosition(Rectangle rec, Point targetPoint) {
            rec.setLocation(targetPoint.x - rec.width, targetPoint.y - rec.height / 2);
        }

        @Override
        Point getLocation(Dimension containerSize, Point targetPoint, Dimension balloonSize, int distance) {
            if (distance > 0) {
                return new Point(targetPoint.x - balloonSize.width, targetPoint.y - distance);
            }
            else {
                Point center = UIUtil.getCenterPoint(new Rectangle(targetPoint, JBUI.emptySize()), balloonSize);
                return new Point(targetPoint.x - balloonSize.width, center.y);
            }
        }

        @Override
        protected Insets getTitleInsets(int normalInset, int pointerLength) {
            //noinspection UseDPIAwareInsets
            return new Insets(JBUIScale.scale(normalInset), pointerLength, JBUIScale.scale(normalInset), JBUIScale.scale(normalInset));
        }
    }

    public interface ActionProvider {
        @Nonnull
        List<ActionButton> createActions();

        void layout(@Nonnull Rectangle bounds);
    }

    public class ActionButton extends NonOpaquePanel implements IdeGlassPane.TopComponent {
        private final consulo.ui.image.Image myIcon;
        private final consulo.ui.image.Image myHoverIcon;
        private final Consumer<? super MouseEvent> myListener;
        protected final BaseButtonBehavior myButton;

        public ActionButton(
            @Nonnull consulo.ui.image.Image icon,
            @Nullable consulo.ui.image.Image hoverIcon,
            @Nullable String hint,
            @Nonnull Consumer<? super MouseEvent> listener
        ) {
            myIcon = icon;
            myHoverIcon = hoverIcon;
            myListener = listener;

            setToolTipText(hint);

            myButton = new BaseButtonBehavior(this, TimedDeadzone.NULL) {
                @Override
                protected void execute(MouseEvent e) {
                    myListener.accept(e);
                }
            };
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(myIcon.getWidth(), myIcon.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (hasPaint()) {
                paintIcon(g, myHoverIcon != null && myButton.isHovered() ? myHoverIcon : myIcon);
            }
        }

        public boolean hasPaint() {
            return getWidth() > 0 && myLastMoveWasInsideBalloon;
        }

        protected void paintIcon(@Nonnull Graphics g, @Nonnull consulo.ui.image.Image icon) {
            TargetAWT.to(icon).paintIcon(this, g, 0, 0);
        }

        @Override
        public boolean canBePreprocessed(@Nonnull MouseEvent e) {
            return false;
        }
    }

    private class CloseButton extends ActionButton {
        private CloseButton(@Nonnull Consumer<? super MouseEvent> listener) {
            super(getCloseButton(), null, null, listener);
            setVisible(myEnableButtons);
        }

        @Override
        protected void paintIcon(@Nonnull Graphics g, @Nonnull consulo.ui.image.Image icon) {
            if (myEnableButtons) {
                boolean pressed = myButton.isPressedByMouse();
                TargetAWT.to(icon).paintIcon(this, g, pressed ? JBUIScale.scale(1) : 0, pressed ? JBUIScale.scale(1) : 0);
            }
        }
    }

    private class MyComponent extends JPanel implements ComponentWithMnemonics {
        private BufferedImage myImage;
        private float myAlpha;
        private final BalloonImpl myBalloon;

        private final JComponent myContent;
        private ShadowBorderPainter.Shadow myShadow;

        private MyComponent(JComponent content, BalloonImpl balloon, EmptyBorder shapeBorder) {
            setOpaque(false);
            setLayout(null);
            putClientProperty(UIUtil.TEXT_COPY_ROOT, Boolean.TRUE);
            myBalloon = balloon;

            // When a screen reader is active, TAB/Shift-TAB should allow moving the focus
            // outside the balloon in the event the balloon acquired the focus.
            if (!ScreenReader.isActive()) {
                setFocusCycleRoot(true);
            }
            putClientProperty(Balloon.KEY, BalloonImpl.this);

            myContent = new JPanel(new BorderLayout(2, 2));
            Wrapper contentWrapper = new Wrapper(content);
            if (myTitle != null) {
                myTitleLabel = new JLabel(myTitle, SwingConstants.CENTER);
                myTitleLabel.setForeground(UIUtil.getListBackground());
                myTitleLabel.setBorder(JBUI.Borders.empty(0, 4));
                myContent.add(myTitleLabel, BorderLayout.NORTH);
                contentWrapper.setBorder(JBUI.Borders.empty(1));
            }
            myContent.add(contentWrapper, BorderLayout.CENTER);
            myContent.setBorder(shapeBorder);
            myContent.setOpaque(false);

            add(myContent);
            setFocusTraversalPolicyProvider(true);
            setFocusTraversalPolicy(new FocusTraversalPolicy() {
                @Override
                public Component getComponentAfter(Container aContainer, Component aComponent) {
                    return WeakFocusStackManager.getInstance().getLastFocusedOutside(MyComponent.this);
                }

                @Override
                public Component getComponentBefore(Container aContainer, Component aComponent) {
                    return WeakFocusStackManager.getInstance().getLastFocusedOutside(MyComponent.this);
                }

                @Override
                public Component getFirstComponent(Container aContainer) {
                    return WeakFocusStackManager.getInstance().getLastFocusedOutside(MyComponent.this);
                }

                @Override
                public Component getLastComponent(Container aContainer) {
                    return WeakFocusStackManager.getInstance().getLastFocusedOutside(MyComponent.this);
                }

                @Override
                public Component getDefaultComponent(Container aContainer) {
                    return WeakFocusStackManager.getInstance().getLastFocusedOutside(MyComponent.this);
                }
            });
        }

        @Nonnull
        Rectangle getContentBounds() {
            Rectangle bounds = getBounds();
            JBInsets.removeFrom(bounds, getInsets());
            return bounds;
        }

        public void clear() {
            myImage = null;
            myAlpha = -1;
        }

        @Override
        public void doLayout() {
            Rectangle bounds = new Rectangle(getWidth(), getHeight());
            JBInsets.removeFrom(bounds, getInsets());

            myContent.setBounds(bounds);
        }

        @Override
        public Dimension getPreferredSize() {
            return addInsets(myContent.getPreferredSize());
        }

        @Override
        public Dimension getMinimumSize() {
            return addInsets(myContent.getMinimumSize());
        }

        private Dimension addInsets(Dimension size) {
            JBInsets.addTo(size, getInsets());
            return size;
        }

        @Override
        protected void paintChildren(Graphics g) {
            if (myImage == null || myAlpha == -1) {
                super.paintChildren(g);
            }
        }

        private void paintChildrenImpl(Graphics g) {
            // Paint to an image without alpha to preserve fonts subpixel antialiasing
            BufferedImage image = ImageUtil.createImage(
                g,
                getWidth(),
                getHeight(),
                BufferedImage.TYPE_INT_RGB
            );
            //new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            useSafely(image.createGraphics(), imageGraphics -> {
                //noinspection UseJBColor
                imageGraphics.setPaint(new Color(myFillColor.getRGB())); // create a copy to remove alpha
                imageGraphics.fillRect(0, 0, getWidth(), getHeight());

                super.paintChildren(imageGraphics);
            });

            Graphics2D g2d = (Graphics2D)g.create();
            try {
                if (UIUtil.isJreHiDPI(g2d)) {
                    float s = 1 / JBUIScale.sysScale(g2d);
                    g2d.scale(s, s);
                }
                UIUtil.drawImage(g2d, makeColorTransparent(image, myFillColor), 0, 0, null);
            }
            finally {
                g2d.dispose();
            }
        }

        private Image makeColorTransparent(Image image, Color color) {
            final int markerRGB = color.getRGB() | 0xFF000000;
            ImageFilter filter = new RGBImageFilter() {
                @Override
                public int filterRGB(int x, int y, int rgb) {
                    if ((rgb | 0xFF000000) == markerRGB) {
                        return 0x00FFFFFF & rgb; // set alpha to 0
                    }
                    return rgb;
                }
            };
            return ImageUtil.filter(image, filter);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D)g;

            Point pointTarget = SwingUtilities.convertPoint(myLayeredPane, myBalloon.myTargetPoint, this);
            Rectangle shapeBounds = myContent.getBounds();
            int shadowSize = myBalloon.getShadowBorderSize();

            if (shadowSize > 0 && myShadow == null && myShadowBorderProvider == null) {
                initComponentImage(pointTarget, shapeBounds);
                myShadow = ShadowBorderPainter.createShadow(myImage, 0, 0, false, shadowSize / 2);
            }

            if (myImage == null && myAlpha != -1) {
                initComponentImage(pointTarget, shapeBounds);
            }

            if (myImage != null && myAlpha != -1) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myAlpha));
            }

            if (myShadowBorderProvider != null) {
                myShadowBorderProvider.paintShadow(this, g);
            }

            if (myImage != null && myAlpha != -1) {
                paintShadow(g);
                UIUtil.drawImage(g2d, myImage, 0, 0, null);
            }
            else {
                paintShadow(g);
                myBalloon.myPosition.createDimensions(myBalloon, shapeBounds, pointTarget).paintTo((Graphics2D)g);
            }
        }

        private void paintShadow(Graphics graphics) {
            if (myShadow != null) {
                Graphics2D g2d = (Graphics2D)graphics;
                try {
                    if (UIUtil.isJreHiDPI(g2d)) {
                        g2d = (Graphics2D)graphics.create();
                        float s = 1 / JBUIScale.sysScale(this);
                        g2d.scale(s, s);
                    }
                    UIUtil.drawImage(g2d, myShadow.getImage(), myShadow.getX(), myShadow.getY(), null);
                }
                finally {
                    if (g2d != graphics) {
                        g2d.dispose();
                    }
                }
            }
        }

        @Override
        public boolean contains(int x, int y) {
            Point pointTarget = SwingUtilities.convertPoint(myLayeredPane, myBalloon.myTargetPoint, this);
            Rectangle bounds = myContent.getBounds();
            Shape shape = myBalloon.myPosition.createDimensions(myBalloon, bounds, pointTarget).getForm().getShape();
            return shape.contains(x, y);
        }

        private void initComponentImage(Point pointTarget, Rectangle shapeBounds) {
            if (myImage != null) {
                return;
            }

            myImage = UIUtil.createImage(myComp, getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            useSafely(
                myImage.getGraphics(),
                imageGraphics -> {
                    myBalloon.myPosition.createDimensions(myBalloon, shapeBounds, pointTarget).paintTo(imageGraphics);
                    paintChildrenImpl(imageGraphics);
                }
            );
        }

        @Override
        public void removeNotify() {
            super.removeNotify();

            if (!ScreenUtil.isStandardAddRemoveNotify(this)) {
                return;
            }

            List<ActionButton> buttons = myActionButtons;
            myActionButtons = null;
            if (buttons != null) {
                //noinspection SSBasedInspection
                SwingUtilities.invokeLater(() -> {
                    for (ActionButton button : buttons) {
                        disposeButton(button);
                    }
                });
            }
        }

        public void setAlpha(float alpha) {
            myAlpha = alpha;
            paintImmediately(0, 0, getWidth(), getHeight());
        }

        void _setBounds(@Nonnull Rectangle bounds) {
            Rectangle currentBounds = getBounds();
            if (!currentBounds.equals(bounds) || invalidateShadow) {
                invalidateShadowImage();
                invalidateShadow = false;
            }

            setBounds(bounds);
            doLayout();

            if (getParent() != null) {
                if (myActionButtons == null) {
                    myActionButtons = myActionProvider.createActions();
                }

                for (ActionButton button : myActionButtons) {
                    if (button.getParent() == null) {
                        myLayeredPane.add(button);
                        myLayeredPane.setLayer(button, JLayeredPane.DRAG_LAYER);
                    }
                }
            }

            if (isVisible()) {
                Rectangle lpBounds = SwingUtilities.convertRectangle(getParent(), bounds, myLayeredPane);
                lpBounds = myPosition.getPointlessContentRec(
                    lpBounds,
                    myBalloon.myShadowBorderProvider == null ? myBalloon.getPointerLength(myPosition) : 0
                );
                myActionProvider.layout(lpBounds);
            }

            if (isVisible()) {
                revalidate();
                repaint();
            }
        }

        private void invalidateShadowImage() {
            myImage = null;
            myShadow = null;
        }

        void repaintButton() {
            if (myActionButtons != null) {
                for (ActionButton button : myActionButtons) {
                    button.repaint();
                }
            }
        }
    }

    private class BalloonDimensions {
        public final AbstractPosition myPosition;
        public final Rectangle myBounds;
        public final Point myTargetPoint;

        public BalloonDimensions(AbstractPosition position, Rectangle bounds, Point targetPoint) {
            myPosition = position;
            myBounds = bounds;
            myTargetPoint = targetPoint;
        }

        void paintTo(Graphics2D g) {
            GraphicsConfig cfg = new GraphicsConfig(g);
            cfg.setAntialiasing(true);

            if (myShadowBorderProvider != null) {
                myShadowBorderProvider.paintBorder(myBounds, g);
                if (myShowPointer) {
                    myShadowBorderProvider.paintPointingShape(myBounds, myTargetPoint, myPosition.getPosition(), g);
                }
                cfg.restore();
                return;
            }

            Shape shape = getForm().getShape();

            g.setPaint(myFillColor);
            g.fill(shape);
            if (myShowPointer && myPointerColor != null) {
                Shape balloonShape = myPosition.getPointlessContentRec(myBounds, this.myPosition.getPointerLength(myDialogMode) + 1);
                Area area = new Area(shape);
                area.subtract(new Area(balloonShape));
                g.setColor(myPointerColor);
                g.fill(area);
            }
            g.setColor(myBorderColor);

            if (myTitleLabel != null) {
                Rectangle titleBounds = myTitleLabel.getBounds();

                Insets inset = myPosition.getTitleInsets(Balloon.getNormalInset() - 1, getPointerLength(this.myPosition) + 50);
                Insets borderInsets = getShadowBorderInsets();
                inset.top += borderInsets.top;
                inset.bottom += borderInsets.bottom;
                inset.left += borderInsets.left;
                inset.right += borderInsets.right;

                titleBounds.x -= inset.left + JBUIScale.scale(1);
                titleBounds.width += inset.left + inset.right + JBUIScale.scale(50);
                titleBounds.y -= inset.top + JBUIScale.scale(1);
                titleBounds.height += inset.top + inset.bottom + JBUIScale.scale(1);

                Area area = new Area(shape);
                area.intersect(new Area(titleBounds));

                Color fgColor = UIManager.getColor("Label.foreground");
                fgColor = ColorUtil.toAlpha(fgColor, 140);
                g.setColor(fgColor);
                g.fill(area);

                g.setColor(myBorderColor);
                g.draw(area);
            }

            g.setStroke(new BasicStroke(BORDER_STROKE_WIDTH.get()));
            g.draw(shape);
            cfg.restore();
        }

        public BalloonForm getForm() {
            if (!myShowPointer) {
                return new BalloonWithoutPoint(myPosition, myBounds, myTargetPoint);
            }
            return switch (myPosition.getPosition()) {
                case below -> new BalloonBelow(myPosition, myBounds, myTargetPoint);
                case above -> new BalloonAbove(myPosition, myBounds, myTargetPoint);
                case atRight -> new BalloonAtRight(myPosition, myBounds, myTargetPoint);
                default -> new BalloonAtLeft(myPosition, myBounds, myTargetPoint);
            };
        }

        boolean isOkToHavePointer() {
            return myPosition.isOkToHavePointer(
                myTargetPoint,
                myBounds,
                getPointerLength(myPosition),
                getPointerWidth(myPosition),
                getArc()
            );
        }
    }

    private abstract class BalloonForm {
        public final AbstractPosition myPosition;
        public final Rectangle myBodyBounds;
        public final Point myTargetPoint;

        public BalloonForm(AbstractPosition position, Rectangle bodyBounds, Point targetPoint) {
            myPosition = position;
            myBodyBounds = bodyBounds;
            myTargetPoint = targetPoint;
        }

        public abstract Shape getShape();
    }

    private class BalloonWithoutPoint extends BalloonForm {
        private BalloonWithoutPoint(AbstractPosition position, Rectangle bounds, Point pointTarget) {
            super(position, bounds, pointTarget);
        }

        @Override
        public Shape getShape() {
            return new RoundRectangle2D.Double(
                myBodyBounds.x,
                myBodyBounds.y,
                myBodyBounds.width - JBUIScale.scale(1),
                myBodyBounds.height - JBUIScale.scale(1),
                getArc(),
                getArc()
            );
        }
    }

    private class BalloonBelow extends BalloonForm {
        private BalloonBelow(AbstractPosition position, Rectangle bounds, Point pointTarget) {
            super(
                position,
                new Rectangle(
                    bounds.x,
                    bounds.y + getPointerLength(position),
                    bounds.width,
                    bounds.height - getPointerLength(position)
                ),
                new Point(pointTarget.x, Math.min((int)bounds.getMinY(), pointTarget.y))
            );
        }

        @Override
        public Shape getShape() {
            int halfPointerWidth = getPointerWidth(myPosition) / 2;
            return new Shaper(myBodyBounds, myTargetPoint)
                .lineTo(myTargetPoint.x + halfPointerWidth, (int)myBodyBounds.getMinY())
                .toRightCurve()
                .roundRightDown()
                .toBottomCurve()
                .roundLeftDown()
                .toLeftCurve()
                .roundLeftUp()
                .toTopCurve()
                .roundUpRight()
                .horLineTo(myTargetPoint.x - halfPointerWidth)
                .lineToTargetPoint()
                .close();
        }
    }

    private class BalloonAbove extends BalloonForm {
        private BalloonAbove(AbstractPosition position, Rectangle bounds, Point pointTarget) {
            super(
                position,
                new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height - getPointerLength(position)),
                new Point(pointTarget.x, Math.max((int)bounds.getMaxY(), pointTarget.y))
            );
        }

        @Override
        public Shape getShape() {
            int halfPointerWidth = getPointerWidth(myPosition) / 2;
            return new Shaper(myBodyBounds, myTargetPoint)
                .lineTo(myTargetPoint.x - halfPointerWidth, (int)myBodyBounds.getMaxY() - JBUIScale.scale(1))
                .toLeftCurve()
                .roundLeftUp()
                .toTopCurve()
                .roundUpRight()
                .toRightCurve()
                .roundRightDown()
                .toBottomCurve()
                .roundLeftDown()
                .horLineTo(myTargetPoint.x + halfPointerWidth)
                .lineToTargetPoint()
                .close();
        }
    }

    private class BalloonAtRight extends BalloonForm {
        private BalloonAtRight(AbstractPosition position, Rectangle bounds, Point pointTarget) {
            super(
                position,
                new Rectangle(
                    bounds.x + getPointerLength(position),
                    bounds.y,
                    bounds.width - getPointerLength(position),
                    bounds.height
                ),
                new Point(Math.min((int)bounds.getMinX(), pointTarget.x), pointTarget.y)
            );
        }

        @Override
        public Shape getShape() {
            int halfPointerWidth = getPointerWidth(myPosition) / 2;
            return new Shaper(myBodyBounds, myTargetPoint)
                .lineTo((int)myBodyBounds.getMinX(), myTargetPoint.y - halfPointerWidth)
                .toTopCurve()
                .roundUpRight()
                .toRightCurve()
                .roundRightDown()
                .toBottomCurve()
                .roundLeftDown()
                .toLeftCurve()
                .roundLeftUp()
                .vertLineTo(myTargetPoint.y + halfPointerWidth)
                .lineToTargetPoint()
                .close();
        }
    }

    private class BalloonAtLeft extends BalloonForm {
        private BalloonAtLeft(AbstractPosition position, Rectangle bounds, Point pointTarget) {
            super(
                position,
                new Rectangle(bounds.x, bounds.y, bounds.width - getPointerLength(position), bounds.height),
                new Point(Math.max((int)bounds.getMaxX(), pointTarget.x), pointTarget.y)
            );
        }

        @Override
        public Shape getShape() {
            int halfPointerWidth = getPointerWidth(myPosition) / 2;
            return new Shaper(myBodyBounds, myTargetPoint)
                .lineTo((int)myBodyBounds.getMaxX() - JBUIScale.scale(1), myTargetPoint.y + halfPointerWidth)
                .toBottomCurve()
                .roundLeftDown()
                .toLeftCurve()
                .roundLeftUp()
                .toTopCurve()
                .roundUpRight()
                .toRightCurve()
                .roundRightDown()
                .vertLineTo(myTargetPoint.y - halfPointerWidth)
                .lineToTargetPoint()
                .close();
        }
    }

    private class Shaper {
        private final GeneralPath myPath = new GeneralPath();

        private Rectangle myBodyBounds;
        private Point myTargetPoint;

        public Shaper(Rectangle bodyBounds, Point targetPoint) {
            myTargetPoint = targetPoint;
            myBodyBounds = bodyBounds;
            start(targetPoint);
        }

        private void start(Point start) {
            myPath.moveTo(start.x, start.y);
        }

        public int getX() {
            return (int)myPath.getCurrentPoint().getX();
        }

        public int getY() {
            return (int)myPath.getCurrentPoint().getY();
        }

        @Nonnull
        public Shaper roundUpRight() {
            int x = getX(), y = getY(), r = getArc();
            myPath.quadTo(x, y - r, x + r, y - r);
            return this;
        }

        @Nonnull
        public Shaper roundRightDown() {
            int x = getX(), y = getY(), r = getArc();
            myPath.quadTo(x + r, y, x + r, y + r);
            return this;
        }

        @Nonnull
        public Shaper roundLeftUp() {
            int x = getX(), y = getY(), r = getArc();
            myPath.quadTo(x - r, y, x - r, y - r);
            return this;
        }

        @Nonnull
        public Shaper roundLeftDown() {
            int x = getX(), y = getY(), r = getArc();
            myPath.quadTo(x, y + r, x - r, y + r);
            return this;
        }

        public Shaper lineTo(int x, int y) {
            myPath.lineTo(x, y);
            return this;
        }

        public Shaper horLineTo(int x) {
            myPath.lineTo(x, getY());
            return this;
        }

        public Shaper vertLineTo(int y) {
            myPath.lineTo(getX(), y);
            return this;
        }

        public Shaper lineToTargetPoint() {
            return lineTo((int)myTargetPoint.getX(), (int)myTargetPoint.getY());
        }

        @Nonnull
        public Shaper toRightCurve() {
            return horLineTo((int)myBodyBounds.getMaxX() - getArc() - JBUIScale.scale(1));
        }

        @Nonnull
        public Shaper toBottomCurve() {
            return vertLineTo((int)myBodyBounds.getMaxY() - getArc() - JBUIScale.scale(1));
        }

        @Nonnull
        public Shaper toLeftCurve() {
            return horLineTo((int)myBodyBounds.getMinX() + getArc());
        }

        @Nonnull
        public Shaper toTopCurve() {
            return vertLineTo((int)myBodyBounds.getMinY() + getArc());
        }

        public Shape close() {
            myPath.closePath();
            return myPath;
        }
    }

    @Override
    public boolean wasFadedIn() {
        return myFadedIn;
    }

    @Override
    public boolean wasFadedOut() {
        return myFadedOut;
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public void setTitle(String title) {
        myTitleLabel.setText(title);
    }

    public void setActionProvider(@Nonnull ActionProvider actionProvider) {
        myActionProvider = actionProvider;
    }

    @Override
    public RelativePoint getShowingPoint() {
        Point p = myPosition.getShiftedPoint(myTargetPoint, myCalloutShift * -1);
        return new RelativePoint(myLayeredPane, p);
    }

    @Override
    public void setAnimationEnabled(boolean enabled) {
        myAnimationEnabled = enabled;
    }

    @Override
    public boolean isAnimationEnabled() {
        return myAnimationEnabled && myAnimationCycle > 0 && !RemoteDesktopService.isRemoteSession();
    }

    @Override
    public boolean isBlockClicks() {
        return myBlockClicks;
    }

    // Returns true if balloon is 'prepared' to process clicks by itself.
    // For example balloon would ignore clicks and won't hide explicitly or would trigger some actions/navigation
    @Override
    public boolean isClickProcessor() {
        return myClickHandler != null || !myCloseOnClick || isBlockClicks();
    }
}
