// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui;

import consulo.ide.impl.idea.codeInsight.hint.TooltipController;
import consulo.ide.impl.idea.ide.IdeTooltip;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.logging.Logger;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.OpaquePanel;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.hint.HintListener;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.hint.TooltipEvent;
import consulo.ui.ex.awt.popup.AWTPopupFactory;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.EventObject;
import java.util.function.Predicate;

public class LightweightHintImpl extends UserDataHolderBase implements LightweightHint {
    public static final Key<Boolean> SHOWN_AT_DEBUG = Key.create("shown.at.debug");
    private static final Logger LOG = Logger.getInstance(LightweightHintImpl.class);

    private final JComponent myComponent;
    private JComponent myFocusBackComponent;
    private final EventListenerList myListenerList = new EventListenerList();
    private MyEscListener myEscListener;
    private JBPopup myPopup;
    private JComponent myParentComponent;
    private boolean myIsRealPopup = false;
    private boolean myForceLightweightPopup = false;
    private boolean mySelectingHint;

    private boolean myForceShowAsPopup = false;
    private String myTitle = null;
    private boolean myCancelOnClickOutside = true;
    private boolean myCancelOnOtherWindowOpen = true;
    private boolean myResizable;

    private IdeTooltip myCurrentIdeTooltip;
    private HintHint myHintHint;
    private JComponent myFocusRequestor;

    private boolean myForceHideShadow = false;

    private boolean myAsyncHide;

    private Predicate<TooltipEvent> myAutoHideTester = tooltipEvent -> true;

    public LightweightHintImpl(@Nonnull JComponent component) {
        myComponent = component;
    }

    @Override
    public void setForceLightweightPopup(boolean forceLightweightPopup) {
        myForceLightweightPopup = forceLightweightPopup;
    }

    @Override
    public void setAsyncHide(boolean asyncHide) {
        myAsyncHide = asyncHide;
    }

    public void setForceShowAsPopup(boolean forceShowAsPopup) {
        myForceShowAsPopup = forceShowAsPopup;
    }

    public void setFocusRequestor(JComponent c) {
        myFocusRequestor = c;
    }

    public void setTitle(String title) {
        myTitle = title;
    }

    public boolean isSelectingHint() {
        return mySelectingHint;
    }

    public void setSelectingHint(boolean selectingHint) {
        mySelectingHint = selectingHint;
    }

    public void setCancelOnClickOutside(boolean b) {
        myCancelOnClickOutside = b;
    }

    public void setCancelOnOtherWindowOpen(boolean b) {
        myCancelOnOtherWindowOpen = b;
    }

    public void setResizable(boolean b) {
        myResizable = b;
    }

    protected boolean canAutoHideOn(TooltipEvent event) {
        return myAutoHideTester.test(event);
    }

    @Override
    public void setAutoHideTester(Predicate<TooltipEvent> autoHideTester) {
        myAutoHideTester = autoHideTester;
    }

    /**
     * Shows the hint in the layered pane. Coordinates {@code x} and {@code y}
     * are in {@code parentComponent} coordinate system. Note that the component
     * appears on 250 layer.
     */
    @Override
    public void show(@Nonnull JComponent parentComponent, int x, int y, JComponent focusBackComponent, @Nonnull final HintHint hintHint) {
        myParentComponent = parentComponent;
        myHintHint = hintHint;

        myFocusBackComponent = focusBackComponent;

        LOG.assertTrue(myParentComponent.isShowing());
        myEscListener = new MyEscListener();
        myComponent.registerKeyboardAction(myEscListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        myComponent.registerKeyboardAction(myEscListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
        JLayeredPane layeredPane = parentComponent.getRootPane().getLayeredPane();

        myComponent.validate();

        if (!myForceShowAsPopup && !hintHint.isPopupForced() && (myForceLightweightPopup || fitsLayeredPane(layeredPane, myComponent, new RelativePoint(parentComponent, new Point(x, y)), hintHint))) {
            beforeShow();
            Dimension preferredSize = myComponent.getPreferredSize();


            if (hintHint.isAwtTooltip()) {
                IdeTooltip tooltip = new IdeTooltip(hintHint.getOriginalComponent(), hintHint.getOriginalPoint(), myComponent, hintHint, myComponent) {
                    @Override
                    protected boolean canAutohideOn(TooltipEvent event) {
                        if (!LightweightHintImpl.this.canAutoHideOn(event)) {
                            return false;
                        }
                        else if (event.getInputEvent() instanceof MouseEvent) {
                            return !(hintHint.isContentActive() && event.isIsEventInsideBalloon());
                        }
                        else if (event.getAction() != null) {
                            return false;
                        }
                        else {
                            return true;
                        }
                    }

                    @Override
                    protected void onHidden() {
                        fireHintHidden();
                        TooltipController.getInstance().resetCurrent();
                    }

                    @Override
                    public boolean canBeDismissedOnTimeout() {
                        return false;
                    }
                }.setToCenterIfSmall(hintHint.isMayCenterTooltip())
                    .setPreferredPosition(hintHint.getPreferredPosition())
                    .setHighlighterType(hintHint.isHighlighterType())
                    .setTextForeground(hintHint.getTextForeground())
                    .setTextBackground(hintHint.getTextBackground())
                    .setBorderColor(hintHint.getBorderColor())
                    .setBorderInsets(hintHint.getBorderInsets())
                    .setFont(hintHint.getTextFont())
                    .setCalloutShift(hintHint.getCalloutShift())
                    .setPositionChangeShift(hintHint.getPositionChangeX(), hintHint.getPositionChangeY())
                    .setExplicitClose(hintHint.isExplicitClose())
                    .setRequestFocus(hintHint.isRequestFocus())
                    .setHint(true);
                myComponent.validate();
                Border border = hintHint.getComponentBorder();
                if (border != null) {
                    tooltip.setComponentBorder(border);
                }

                tooltip.setPointerSize(JBUI.size(16, 8)).setPointerShiftedToStart(true);

                myCurrentIdeTooltip = IdeTooltipManagerImpl.getInstanceImpl().show(tooltip, hintHint.isShowImmediately(), hintHint.isAnimationEnabled());
            }
            else {
                Point layeredPanePoint = SwingUtilities.convertPoint(parentComponent, x, y, layeredPane);
                myComponent.setBounds(layeredPanePoint.x, layeredPanePoint.y, preferredSize.width, preferredSize.height);
                layeredPane.add(myComponent, JLayeredPane.POPUP_LAYER);

                myComponent.validate();
                myComponent.repaint();
            }
        }
        else {
            myIsRealPopup = true;
            Point actualPoint = new Point(x, y);
            if (hintHint.getPreferredPosition() == Balloon.Position.atLeft) {
                int width = myComponent.getPreferredSize().width;
                actualPoint.translate(-width, 0);
            }
            JComponent actualComponent = new OpaquePanel(new BorderLayout());
            actualComponent.add(myComponent, BorderLayout.CENTER);
            if (isAwtTooltip()) {
                actualComponent.setBackground(hintHint.getTextBackground());
                actualComponent.validate();
            }

            myPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(actualComponent, myFocusRequestor).setRequestFocus(myFocusRequestor != null || hintHint.isRequestFocus())
                .setFocusable(myFocusRequestor != null || hintHint.isRequestFocus()).setResizable(myResizable).setMovable(myTitle != null).setTitle(myTitle).setModalContext(false)
                .setShowShadow(isRealPopup() && !isForceHideShadow()).setCancelKeyEnabled(false).setCancelOnClickOutside(myCancelOnClickOutside).setCancelCallback(() -> {
                    onPopupCancel();
                    return true;
                }).setCancelOnOtherWindowOpen(myCancelOnOtherWindowOpen).createPopup();

            beforeShow();
            myPopup.show(new RelativePoint(myParentComponent, new Point(actualPoint.x, actualPoint.y)));
        }
    }

    protected void onPopupCancel() {
    }

    private void fixActualPoint(Point actualPoint) {
        if (!isAwtTooltip()) {
            return;
        }
        if (!myIsRealPopup) {
            return;
        }

        Dimension size = myComponent.getPreferredSize();
        Balloon.Position position = myHintHint.getPreferredPosition();
        AWTPopupFactory factory = (AWTPopupFactory) JBPopupFactory.getInstance();
        int shift = factory.getPointerLength(position, false);
        switch (position) {
            case below:
                actualPoint.y += shift;
                break;
            case above:
                actualPoint.y -= (shift + size.height);
                break;
            case atLeft:
                actualPoint.x -= (shift + size.width);
                break;
            case atRight:
                actualPoint.y += shift;
                break;
        }
    }

    protected void beforeShow() {
    }

    public boolean vetoesHiding() {
        return false;
    }

    public boolean isForceHideShadow() {
        return myForceHideShadow;
    }

    public void setForceHideShadow(boolean forceHideShadow) {
        myForceHideShadow = forceHideShadow;
    }

    private static boolean fitsLayeredPane(JLayeredPane pane, JComponent component, RelativePoint desiredLocation, HintHint hintHint) {
        if (hintHint.isAwtTooltip()) {
            Dimension size = component.getPreferredSize();
            Dimension paneSize = pane.getSize();

            Point target = desiredLocation.getPointOn(pane).getPoint();
            Balloon.Position pos = hintHint.getPreferredPosition();
            AWTPopupFactory factory = (AWTPopupFactory) JBPopupFactory.getInstance();
            int pointer = factory.getPointerLength(pos, false) + Balloon.getNormalInset();
            if (pos == Balloon.Position.above || pos == Balloon.Position.below) {
                boolean heightFit = target.y - size.height - pointer > 0 || target.y + size.height + pointer < paneSize.height;
                return heightFit && size.width + pointer < paneSize.width;
            }
            else {
                boolean widthFit = target.x - size.width - pointer > 0 || target.x + size.width + pointer < paneSize.width;
                return widthFit && size.height + pointer < paneSize.height;
            }
        }
        else {
            Rectangle lpRect = new Rectangle(pane.getLocationOnScreen().x, pane.getLocationOnScreen().y, pane.getWidth(), pane.getHeight());
            Rectangle componentRect = new Rectangle(desiredLocation.getScreenPoint().x, desiredLocation.getScreenPoint().y, component.getPreferredSize().width, component.getPreferredSize().height);
            return lpRect.contains(componentRect);
        }
    }

    private void fireHintHidden() {
        EventListener[] listeners = myListenerList.getListeners(HintListener.class);
        for (EventListener listener : listeners) {
            ((HintListener) listener).hintHidden(new EventObject(this));
        }
    }

    /**
     * @return bounds of hint component in the parent component's layered pane coordinate system.
     */
    public final Rectangle getBounds() {
        Rectangle bounds = new Rectangle(myComponent.getBounds());
        JLayeredPane layeredPane = myParentComponent.getRootPane().getLayeredPane();
        return SwingUtilities.convertRectangle(myComponent, bounds, layeredPane);
    }

    @Override
    public boolean isVisible() {
        Boolean shownAtDebug = getUserData(SHOWN_AT_DEBUG);
        if (shownAtDebug != null) {
            return shownAtDebug;
        }

        if (myIsRealPopup) {
            return myPopup != null && myPopup.isVisible();
        }
        if (myCurrentIdeTooltip != null) {
            return myComponent.isShowing() || IdeTooltipManagerImpl.getInstanceImpl().isQueuedToShow(myCurrentIdeTooltip);
        }
        return myComponent.isShowing();
    }

    public final boolean isRealPopup() {
        return myIsRealPopup || myForceShowAsPopup;
    }

    @Override
    public void hide() {
        if (myAsyncHide) {
            SwingUtilities.invokeLater(() -> hide(false));
        }
        else {
            hide(false);
        }
    }

    public void hide(boolean ok) {
        if (isVisible()) {
            if (myIsRealPopup) {
                if (ok) {
                    myPopup.closeOk(null);
                }
                else {
                    myPopup.cancel();
                }
                myPopup = null;
            }
            else {
                if (myCurrentIdeTooltip != null) {
                    IdeTooltip tooltip = myCurrentIdeTooltip;
                    myCurrentIdeTooltip = null;
                    tooltip.hide();
                }
                else {
                    JRootPane rootPane = myComponent.getRootPane();
                    JLayeredPane layeredPane = rootPane == null ? null : rootPane.getLayeredPane();
                    if (layeredPane != null) {
                        Rectangle bounds = myComponent.getBounds();
                        try {
                            layeredPane.remove(myComponent);
                        }
                        finally {
                        }

                        layeredPane.paintImmediately(bounds.x, bounds.y, bounds.width, bounds.height);
                    }
                }
            }
        }
        if (myEscListener != null) {
            myComponent.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        }

        TooltipController.getInstance().hide(this);

        fireHintHidden();
    }

    @Override
    public void pack() {
        setSize(myComponent.getPreferredSize());
    }

    public void updateLocation(int x, int y) {
        Point point = new Point(x, y);
        fixActualPoint(point);
        setLocation(new RelativePoint(myParentComponent, point));
    }

    public void updatePosition(Balloon.Position position) {
        if (myHintHint != null) {
            myHintHint.setPreferredPosition(position);
        }
        if (myCurrentIdeTooltip != null) {
            myCurrentIdeTooltip.setPreferredPosition(position);
        }
    }

    public final JComponent getComponent() {
        return myComponent;
    }

    @Override
    public final void addHintListener(@Nonnull HintListener listener) {
        myListenerList.add(HintListener.class, listener);
    }

    @Override
    public final void removeHintListener(@Nonnull HintListener listener) {
        myListenerList.remove(HintListener.class, listener);
    }

    public Point getLocationOn(JComponent c) {
        Point location;
        if (isRealPopup() && !myPopup.isDisposed()) {
            location = myPopup.getLocationOnScreen();
            SwingUtilities.convertPointFromScreen(location, c);
        }
        else {
            if (myCurrentIdeTooltip != null) {
                Point tipPoint = myCurrentIdeTooltip.getPoint();
                Component tipComponent = myCurrentIdeTooltip.getComponent();
                return SwingUtilities.convertPoint(tipComponent, tipPoint, c);
            }
            else {
                location = SwingUtilities.convertPoint(myComponent.getParent(), myComponent.getLocation(), c);
            }
        }

        return location;
    }

    @Override
    public void setLocation(@Nonnull RelativePoint point) {
        if (isRealPopup()) {
            myPopup.setLocation(point.getScreenPoint());
        }
        else {
            if (myCurrentIdeTooltip != null) {
                myCurrentIdeTooltip.setPoint(point.getPoint());
                myCurrentIdeTooltip.setComponent(point.getComponent());
                IdeTooltipManagerImpl.getInstanceImpl().show(myCurrentIdeTooltip, true, false);
            }
            else {
                Point targetPoint = point.getPoint(myComponent.getParent());
                myComponent.setLocation(targetPoint);

                myComponent.revalidate();
                myComponent.repaint();
            }
        }
    }

    public void setSize(Dimension size) {
        if (myIsRealPopup && myPopup != null && !myPopup.isDisposed()) {
            // There is a possible case that a popup wraps target content component into other components which might have borders.
            // That's why we can't just apply component's size to the whole popup. It needs to be adjusted before that.
            JComponent popupContent = myPopup.getContent();
            int widthExpand = 0;
            int heightExpand = 0;
            boolean adjustSize = false;
            JComponent prev = myComponent;
            for (Container c = myComponent.getParent(); c != null; c = c.getParent()) {
                if (c == popupContent) {
                    adjustSize = true;
                    break;
                }
                if (c instanceof JComponent) {
                    Border border = ((JComponent) c).getBorder();
                    if (prev != null && border != null) {
                        Insets insets = border.getBorderInsets(prev);
                        widthExpand += insets.left + insets.right;
                        heightExpand += insets.top + insets.bottom;
                    }
                    prev = (JComponent) c;
                }
                else {
                    prev = null;
                }
            }
            Dimension sizeToUse = size;
            if (adjustSize && (widthExpand != 0 || heightExpand != 0)) {
                sizeToUse = new Dimension(size.width + widthExpand, size.height + heightExpand);
            }
            myPopup.setSize(sizeToUse);
        }
        else if (!isAwtTooltip()) {
            myComponent.setSize(size);

            myComponent.revalidate();
            myComponent.repaint();
        }
        else { // isAwtTooltip() case, we have to update Balloon size
            Component c = myComponent;
            while (c != null) {
                if (c.getParent() instanceof JLayeredPane) {
                    c.setSize(c.getPreferredSize());
                    c.revalidate();
                    c.repaint();
                    break;
                }
                c = c.getParent();
            }
        }
    }

    public boolean isAwtTooltip() {
        return myHintHint != null && myHintHint.isAwtTooltip();
    }

    public Dimension getSize() {
        return myComponent.getSize();
    }

    @Override
    public boolean isInsideHint(RelativePoint target) {
        if (!myComponent.isShowing()) {
            return false;
        }

        if (myIsRealPopup) {
            Window wnd = SwingUtilities.getWindowAncestor(myComponent);
            return wnd.getBounds().contains(target.getScreenPoint());
        }
        else if (myCurrentIdeTooltip != null) {
            return myCurrentIdeTooltip.isInside(target);
        }
        else {
            return new Rectangle(myComponent.getLocationOnScreen(), myComponent.getSize()).contains(target.getScreenPoint());
        }
    }

    private final class MyEscListener implements ActionListener {
        @Override
        public final void actionPerformed(ActionEvent e) {
            hide();
        }
    }

    @Override
    public String toString() {
        return getComponent().toString();
    }

    @Override
    public boolean canControlAutoHide() {
        return myCurrentIdeTooltip != null && myCurrentIdeTooltip.getTipComponent().isShowing();
    }

    public IdeTooltip getCurrentIdeTooltip() {
        return myCurrentIdeTooltip;
    }
}
