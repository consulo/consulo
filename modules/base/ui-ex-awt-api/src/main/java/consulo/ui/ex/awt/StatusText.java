// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.localize.UILocalize;
import consulo.util.collection.Lists;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public abstract class StatusText {
    public static final SimpleTextAttributes DEFAULT_ATTRIBUTES = SimpleTextAttributes.GRAYED_ATTRIBUTES;
    public static final LocalizeValue DEFAULT_EMPTY_LOC_TEXT = UILocalize.messageNothingtoshow();
    @Deprecated
    @DeprecationInfo("Use #DEFAULT_EMPTY_LOC_TEXT")
    public static final String DEFAULT_EMPTY_TEXT = UIBundle.message("message.nothingToShow");

    public static final LocalizeValue DEFAULT_EMPTY_TEXT_VALUE = UILocalize.messageNothingtoshow();

    private static final int Y_GAP = 2;

    public static String getDefaultEmptyText() {
        return DEFAULT_EMPTY_TEXT_VALUE.get();
    }

    @Nullable
    private Component myOwner;
    private Component myMouseTarget;
    @Nonnull
    private final MouseMotionListener myMouseMotionListener;
    @Nonnull
    private final ClickListener myClickListener;

    private boolean myIsDefaultText;

    private String myText = "";
    @Nonnull
    protected final SimpleColoredComponent myComponent = new SimpleColoredComponent();
    @Nonnull
    private final SimpleColoredComponent mySecondaryComponent = new SimpleColoredComponent();
    private final List<ActionListener> myClickListeners = Lists.newLockFreeCopyOnWriteList();
    private final List<ActionListener> mySecondaryListeners = Lists.newLockFreeCopyOnWriteList();
    private boolean myHasActiveClickListeners; // calculated field for performance optimization
    private boolean myShowAboveCenter = true;
    private boolean myVerticalFlow = true;
    private boolean myFontSet = false;

    protected StatusText(JComponent owner) {
        this();
        attachTo(owner);
    }

    public StatusText() {
        myClickListener = new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
                if (e.getButton() == MouseEvent.BUTTON1 && clickCount == 1) {
                    ActionListener actionListener = findActionListenerAt(e.getPoint());
                    if (actionListener != null) {
                        actionListener.actionPerformed(new ActionEvent(this, 0, ""));
                        return true;
                    }
                }
                return false;
            }
        };

        myMouseMotionListener = new MouseAdapter() {
            private Cursor myOriginalCursor;

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isStatusVisible()) {
                    if (findActionListenerAt(e.getPoint()) != null) {
                        if (myOriginalCursor == null) {
                            myOriginalCursor = myMouseTarget.getCursor();
                            myMouseTarget.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }
                    else if (myOriginalCursor != null) {
                        myMouseTarget.setCursor(myOriginalCursor);
                        myOriginalCursor = null;
                    }
                }
            }
        };

        myComponent.setOpaque(false);
        myComponent.setFont(UIUtil.getLabelFont());
        setText(DEFAULT_EMPTY_LOC_TEXT, DEFAULT_ATTRIBUTES);
        myIsDefaultText = true;

        mySecondaryComponent.setOpaque(false);
        mySecondaryComponent.setFont(UIUtil.getLabelFont());
    }

    protected boolean isFontSet() {
        return myFontSet;
    }

    public void setFont(@Nonnull Font font) {
        myComponent.setFont(font);
        mySecondaryComponent.setFont(font);
        myFontSet = true;
    }

    public void attachTo(@Nullable Component owner) {
        attachTo(owner, owner);
    }

    public void attachTo(@Nullable Component owner, @Nullable Component mouseTarget) {
        if (myMouseTarget != null) {
            myClickListener.uninstall(myMouseTarget);
            myMouseTarget.removeMouseMotionListener(myMouseMotionListener);
        }

        myOwner = owner;
        myMouseTarget = mouseTarget;

        if (myMouseTarget != null) {
            myClickListener.installOn(myMouseTarget);
            myMouseTarget.addMouseMotionListener(myMouseMotionListener);
        }
    }

    protected abstract boolean isStatusVisible();

    @Nullable
    private static ActionListener findListener(
        @Nonnull SimpleColoredComponent component,
        @Nonnull List<? extends ActionListener> listeners,
        int xCoord
    ) {
        int index = component.findFragmentAt(xCoord);
        if (index >= 0 && index < listeners.size()) {
            return listeners.get(index);
        }
        return null;
    }

    @Nullable
    private ActionListener findActionListenerAt(Point point) {
        if (!myHasActiveClickListeners || !isStatusVisible()) {
            return null;
        }

        point = SwingUtilities.convertPoint(myMouseTarget, point, myOwner);

        Rectangle commonBounds = getTextComponentBound();
        if (commonBounds.contains(point)) {
            Rectangle bounds;
            if (myComponent.getPreferredSize().height >= point.y - commonBounds.y) {
                bounds = adjustComponentBounds(myComponent, commonBounds);
                return findListener(myComponent, myClickListeners, point.x - bounds.x);
            }
            bounds = adjustComponentBounds(mySecondaryComponent, commonBounds);
            return findListener(mySecondaryComponent, mySecondaryListeners, point.x - bounds.x);
        }
        return null;
    }

    protected Rectangle getTextComponentBound() {
        Rectangle ownerRec = myOwner == null ? new Rectangle(0, 0, 0, 0) : myOwner.getBounds();

        Dimension size = getPreferredSize();
        int x = (ownerRec.width - size.width) / 2;
        int y = (ownerRec.height - size.height) / (myShowAboveCenter ? 3 : 2);
        return new Rectangle(x, y, size.width, size.height);
    }

    public final boolean isShowAboveCenter() {
        return myShowAboveCenter;
    }

    public final StatusText setShowAboveCenter(boolean showAboveCenter) {
        myShowAboveCenter = showAboveCenter;
        return this;
    }

    @Nonnull
    public String getText() {
        return myText;
    }

    public StatusText setText(@Nonnull LocalizeValue text) {
        return setText(text, DEFAULT_ATTRIBUTES);
    }

    public StatusText setText(@Nonnull LocalizeValue text, SimpleTextAttributes attrs) {
        return clear().appendText(text, attrs);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @SuppressWarnings("deprecation")
    public StatusText setText(String text) {
        return setText(text, DEFAULT_ATTRIBUTES);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @SuppressWarnings("deprecation")
    public StatusText setText(String text, SimpleTextAttributes attrs) {
        return clear().appendText(text, attrs);
    }

    public StatusText clear() {
        myText = "";
        myComponent.clear();
        myClickListeners.clear();
        mySecondaryComponent.clear();
        mySecondaryListeners.clear();
        myHasActiveClickListeners = false;
        repaintOwner();
        return this;
    }

    private void repaintOwner() {
        if (myOwner != null && isStatusVisible()) {
            myOwner.repaint();
        }
    }

    public StatusText appendText(@Nonnull LocalizeValue text) {
        return appendText(text, DEFAULT_ATTRIBUTES);
    }

    public StatusText appendText(@Nonnull LocalizeValue text, SimpleTextAttributes attrs) {
        return appendText(text, attrs, null);
    }

    public StatusText appendText(@Nonnull LocalizeValue text, SimpleTextAttributes attrs, ActionListener listener) {
        if (myIsDefaultText) {
            clear();
            myIsDefaultText = false;
        }

        myText += text;
        myComponent.append(text, attrs);
        myClickListeners.add(listener);
        if (listener != null) {
            myHasActiveClickListeners = true;
        }
        repaintOwner();
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @SuppressWarnings("deprecation")
    public StatusText appendText(String text) {
        return appendText(text, DEFAULT_ATTRIBUTES);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @SuppressWarnings("deprecation")
    public StatusText appendText(String text, SimpleTextAttributes attrs) {
        return appendText(text, attrs, null);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public StatusText appendText(String text, SimpleTextAttributes attrs, ActionListener listener) {
        return appendText(LocalizeValue.ofNullable(text), attrs, listener);
    }

    public void setIsVerticalFlow(boolean isVerticalFlow) {
        myVerticalFlow = isVerticalFlow;
    }

    @Nonnull
    public StatusText appendSecondaryText(@Nonnull LocalizeValue text, @Nonnull SimpleTextAttributes attrs, @Nullable ActionListener listener) {
        mySecondaryComponent.append(text, attrs);
        mySecondaryListeners.add(listener);
        if (listener != null) {
            myHasActiveClickListeners = true;
        }
        repaintOwner();
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Nonnull
    public StatusText appendSecondaryText(@Nonnull String text, @Nonnull SimpleTextAttributes attrs, @Nullable ActionListener listener) {
        return appendSecondaryText(LocalizeValue.of(text), attrs, listener);
    }

    public void paint(Component owner, Graphics g) {
        if (!isStatusVisible()) {
            return;
        }

        if (owner == myOwner) {
            doPaintStatusText(g, getTextComponentBound());
        }
        else {
            paintOnComponentUnderViewport(owner, g);
        }
    }

    private void paintOnComponentUnderViewport(Component component, Graphics g) {
        JBViewport viewport = ObjectUtil.tryCast(myOwner, JBViewport.class);
        if (viewport == null || viewport.getView() != component || viewport.isPaintingNow()) {
            return;
        }

        // We're painting a component which has a viewport as it's ancestor.
        // As the viewport paints status text, we'll erase it, so we need to schedule a repaint for the viewport with status text's bounds.
        // But it causes flicker, so we paint status text over the component first and then schedule the viewport repaint.

        Rectangle textBoundsInViewport = getTextComponentBound();

        int xInOwner = textBoundsInViewport.x - component.getX();
        int yInOwner = textBoundsInViewport.y - component.getY();
        Rectangle textBoundsInOwner = new Rectangle(xInOwner, yInOwner, textBoundsInViewport.width, textBoundsInViewport.height);
        doPaintStatusText(g, textBoundsInOwner);

        viewport.repaint(textBoundsInViewport);
    }

    private void doPaintStatusText(@Nonnull Graphics g, @Nonnull Rectangle bounds) {
        if (!hasSecondaryText()) {
            paintComponentInBounds(myComponent, g, bounds);
        }
        else {
            Rectangle primaryBounds = adjustComponentBounds(myComponent, bounds);
            Rectangle secondaryBounds = adjustComponentBounds(mySecondaryComponent, bounds);
            if (myVerticalFlow) {
                secondaryBounds.y += primaryBounds.height + JBUIScale.scale(Y_GAP);
            }

            paintComponentInBounds(myComponent, g, primaryBounds);
            paintComponentInBounds(mySecondaryComponent, g, secondaryBounds);
        }
    }

    @Nonnull
    protected Rectangle adjustComponentBounds(@Nonnull JComponent component, @Nonnull Rectangle bounds) {
        Dimension size = component.getPreferredSize();

        if (myVerticalFlow) {
            return new Rectangle(bounds.x + (bounds.width - size.width) / 2, bounds.y, size.width, size.height);
        }
        else {
            return component == myComponent
                ? new Rectangle(bounds.x, bounds.y, size.width, size.height)
                : new Rectangle(bounds.x + bounds.width - size.width, bounds.y, size.width, size.height);
        }
    }

    private boolean hasSecondaryText() {
        return mySecondaryComponent.getCharSequence(false).length() > 0;
    }

    private static void paintComponentInBounds(@Nonnull SimpleColoredComponent component, @Nonnull Graphics g, @Nonnull Rectangle bounds) {
        Graphics2D g2 = (Graphics2D) g.create(bounds.x, bounds.y, bounds.width, bounds.height);
        component.setBounds(0, 0, bounds.width, bounds.height);
        component.paint(g2);
        g2.dispose();
    }

    @Nonnull
    public SimpleColoredComponent getComponent() {
        return myComponent;
    }

    @Nonnull
    public SimpleColoredComponent getSecondaryComponent() {
        return mySecondaryComponent;
    }

    public Dimension getPreferredSize() {
        Dimension componentSize = myComponent.getPreferredSize();
        if (!hasSecondaryText()) {
            return componentSize;
        }
        Dimension secondaryComponentSize = mySecondaryComponent.getPreferredSize();

        if (myVerticalFlow) {
            return new Dimension(
                Math.max(componentSize.width, secondaryComponentSize.width),
                componentSize.height + secondaryComponentSize.height + JBUIScale.scale(Y_GAP)
            );
        }
        else {
            return new Dimension(
                componentSize.width + secondaryComponentSize.width,
                Math.max(componentSize.height, secondaryComponentSize.height)
            );
        }
    }

    public boolean isVerticalFlow() {
        return myVerticalFlow;
    }
}
