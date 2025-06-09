/*
 * Copyright 2013-2024 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.action.toolbar;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.RelativeRectangle;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-12-31
 */
public class AdvancedActionToolbarImpl extends SimpleActionToolbarImpl {
    private static final Logger LOG = Logger.getInstance(AdvancedActionToolbarImpl.class);
    /**
     * This array contains Rectangles which define bounds of the corresponding
     * components in the toolbar. This list can be consider as a cache of the
     * Rectangle objects that are used in calculation of preferred sizes and
     * components layout.
     */
    private final List<Rectangle> myComponentBounds = new ArrayList<>();

    private Size2D myMinimumButtonSize = Size2D.ZERO;

    /**
     * @see ActionToolbar#getLayoutPolicy()
     */
    private int myLayoutPolicy;

    private Rectangle myAutoPopupRec;

    private int myFirstOutsideIndex = -1;

    private boolean myForceShowFirstComponent;

    private JBPopup myPopup;

    private final ActionManagerEx myActionManager;

    public AdvancedActionToolbarImpl(@Nonnull String place,
                                     @Nonnull ActionGroup actionGroup,
                                     @Nonnull Style style,
                                     @Nonnull ActionManager actionManager) {
        super(place, actionGroup, style);
        myActionManager = (ActionManagerEx) actionManager;
    }

    @Override
    public void setMiniMode(boolean minimalMode) {
        if (minimalMode) {
            setLayoutPolicy(NOWRAP_LAYOUT_POLICY);
            setBorder(JBUI.Borders.empty());
            setOpaque(false);
        }
        else {
            setBorder(JBUI.Borders.empty(2));
            setOpaque(true);
            setLayoutPolicy(AUTO_LAYOUT_POLICY);
        }
    }

    @Override
    protected void tweakActionComponentUI(@Nonnull Component actionComponent) {
        super.tweakActionComponentUI(actionComponent);

        actionComponent.setMinimumSize(TargetAWT.to(myMinimumButtonSize));
    }

    @Override
    public Dimension getMinimumSize() {
        if (myLayoutPolicy == AUTO_LAYOUT_POLICY) {
            Dimension minSize = TargetAWT.to(myMinimumButtonSize);

            final Insets i = getInsets();
            if (myForceShowFirstComponent && getComponentCount() > 0) {
                Component c = getComponent(0);
                Dimension firstSize = c.getPreferredSize();
                if (getOrientation() == SwingConstants.HORIZONTAL) {
                    return new Dimension(firstSize.width + AllIcons.Ide.Link.getWidth() + i.left + i.right,
                        Math.max(firstSize.height, minSize.height) + i.top + i.bottom);
                }
                else {
                    return new Dimension(Math.max(firstSize.width, AllIcons.Ide.Link.getWidth()) + i.left + i.right,
                        firstSize.height + minSize.height + i.top + i.bottom);
                }
            }
            return new Dimension(AllIcons.Ide.Link.getWidth() + i.left + i.right, minSize.height + i.top + i.bottom);
        }
        else {
            return super.getMinimumSize();
        }
    }

    @Override
    protected void processMouseMotionEvent(final MouseEvent e) {
        super.processMouseMotionEvent(e);

        if (getLayoutPolicy() != AUTO_LAYOUT_POLICY) {
            return;
        }
        if (myAutoPopupRec != null && myAutoPopupRec.contains(e.getPoint())) {
            ApplicationIdeFocusManager.getInstance().doWhenFocusSettlesDown(() -> showAutoPopup());
        }
    }

    private void showAutoPopup() {
        if (isPopupShowing()) {
            return;
        }

        final ActionGroup group;
        int orientation = getOrientation();
        if (orientation == SwingConstants.HORIZONTAL) {
            group = myEngine.getActionGroup();
        }
        else {
            List<? extends AnAction> visibleActions = myEngine.getVisibleActions();

            final DefaultActionGroup outside = new DefaultActionGroup();
            for (int i = myFirstOutsideIndex; i < visibleActions.size(); i++) {
                outside.add(visibleActions.get(i), myActionManager);
            }
            group = outside;
        }

        PopupToolbar popupToolbar = new PopupToolbar(myEngine.getPlace(), group, this, myActionManager) {
            @Override
            protected void onOtherActionPerformed() {
                hidePopup();
            }

            @Nonnull
            @Override
            protected DataContext getDataContext() {
                return AdvancedActionToolbarImpl.this.getDataContext();
            }
        };
        popupToolbar.setLayoutPolicy(NOWRAP_LAYOUT_POLICY);
        popupToolbar.updateActionsImmediately();

        Point location;
        if (orientation == SwingConstants.HORIZONTAL) {
            location = getLocationOnScreen();
        }
        else {
            location = getLocationOnScreen();
            location.y = location.y + getHeight() - popupToolbar.getPreferredSize().height;
        }


        final ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(popupToolbar, null);
        builder.setResizable(false)
            .setMovable(true) // fit the screen automatically
            .setRequestFocus(false)
            .setTitle(null)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setCancelCallback(() -> {
                final boolean toClose = myActionManager.isActionPopupStackEmpty();
                if (toClose) {
                    myEngine.updateActionsAsync();
                }
                return toClose;
            })
            .setCancelOnMouseOutCallback(event -> myAutoPopupRec != null && myActionManager.isActionPopupStackEmpty() && !new RelativeRectangle(
                this,
                myAutoPopupRec).contains(new RelativePoint(event)));

        builder.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(@Nonnull LightweightWindowEvent event) {
                processClosed();
            }
        });
        myPopup = builder.createPopup();
        Disposer.register(myPopup, popupToolbar);

        myPopup.showInScreenCoordinates(this, location);

        final Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            final ComponentAdapter componentAdapter = new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) {
                    hidePopup();
                }

                @Override
                public void componentMoved(final ComponentEvent e) {
                    hidePopup();
                }

                @Override
                public void componentShown(final ComponentEvent e) {
                    hidePopup();
                }

                @Override
                public void componentHidden(final ComponentEvent e) {
                    hidePopup();
                }
            };
            window.addComponentListener(componentAdapter);
            Disposer.register(popupToolbar, () -> window.removeComponentListener(componentAdapter));
        }
    }


    private boolean isPopupShowing() {
        return myPopup != null && !myPopup.isDisposed();
    }

    private void hidePopup() {
        if (myPopup != null) {
            myPopup.cancel();
            processClosed();
        }
    }

    private void processClosed() {
        if (myPopup == null) {
            return;
        }
        if (myPopup.isVisible()) {
            // setCancelCallback(..) can override cancel()
            return;
        }
        // cancel() already called Disposer.dispose()
        myPopup = null;
        myEngine.updateActionsAsync();
    }

    abstract static class PopupToolbar extends AdvancedActionToolbarImpl implements AnActionListener, DataProvider, Disposable {
        private final JComponent myParent;

        PopupToolbar(@Nonnull String place,
                     @Nonnull ActionGroup actionGroup,
                     @Nonnull JComponent parent,
                     @Nonnull ActionManager actionManager) {
            super(place, actionGroup, Style.HORIZONTAL, actionManager);
            ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AnActionListener.class, this);
            myParent = parent;
            setBorder(myParent.getBorder());
        }

        @Nullable
        @Override
        public Object getData(@Nonnull Key dataId) {
            return getDataContext().getData(dataId);
        }

        @Override
        public Container getParent() {
            Container parent = super.getParent();
            return parent != null ? parent : myParent;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void afterActionPerformed(@Nonnull final AnAction action, @Nonnull final DataContext dataContext, @Nonnull AnActionEvent event) {
            List<? extends AnAction> visibleActions = myEngine.getVisibleActions();

            if (!visibleActions.contains(action)) {
                onOtherActionPerformed();
            }
        }

        protected abstract void onOtherActionPerformed();
    }

    /**
     * By default minimum size is to show chevron only.
     * If this option is {@code true} toolbar shows at least one (the first) component plus chevron (if need)
     */
    @Override
    public void setForceShowFirstComponent(boolean showFirstComponent) {
        myForceShowFirstComponent = showFirstComponent;
    }

    @Override
    public void setMinimumButtonSize(@Nonnull final Size2D size) {
        myMinimumButtonSize = size;
        for (int i = getComponentCount() - 1; i >= 0; i--) {
            final Component component = getComponent(i);
            if (component instanceof ActionButton button) {
                button.getComponent().setMinimumSize(TargetAWT.to(size));
            }
        }
        revalidate();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (myLayoutPolicy == AUTO_LAYOUT_POLICY && myAutoPopupRec != null) {
            Image link = PlatformIconGroup.ideLink();

            if (getOrientation() == SwingConstants.HORIZONTAL) {
                final int dy = myAutoPopupRec.height / 2 - link.getHeight() / 2;
                TargetAWT.to(link)
                    .paintIcon(this, g, (int) myAutoPopupRec.getMaxX() - link.getWidth() - 1, myAutoPopupRec.y + dy);
            }
            else {
                final int dx = myAutoPopupRec.width / 2 - link.getWidth() / 2;
                TargetAWT.to(link)
                    .paintIcon(this, g, myAutoPopupRec.x + dx, (int) myAutoPopupRec.getMaxY() - link.getWidth() - 1);
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void removeNotify() {
        super.removeNotify();

        if (myPopup != null) {
            myPopup.cancel();
            myPopup = null;
        }
    }

    @Override
    public int getLayoutPolicy() {
        return myLayoutPolicy;
    }

    @Override
    public void setLayoutPolicy(int layoutPolicy) {
        if (layoutPolicy != NOWRAP_LAYOUT_POLICY && layoutPolicy != WRAP_LAYOUT_POLICY && layoutPolicy != AUTO_LAYOUT_POLICY) {
            throw new IllegalArgumentException("wrong layoutPolicy: " + layoutPolicy);
        }
        myLayoutPolicy = layoutPolicy;
    }

    @Override
    public void validate() {
        if (!isValid()) {
            calculateBounds(getSize(), myComponentBounds);
            super.validate();
        }
    }

    @Override
    @Nonnull
    public Dimension getPreferredSize() {
        final ArrayList<Rectangle> bounds = new ArrayList<>();
        calculateBounds(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE), bounds);//it doesn't take into account wrapping
        if (bounds.isEmpty()) {
            return JBUI.emptySize();
        }
        int forcedHeight = 0;
        int orientation = getOrientation();
        if (getWidth() > 0 && getLayoutPolicy() == ActionToolbar.WRAP_LAYOUT_POLICY && orientation == SwingConstants.HORIZONTAL) {
            final ArrayList<Rectangle> limitedBounds = new ArrayList<>();
            calculateBounds(new Dimension(getWidth(), Integer.MAX_VALUE), limitedBounds);
            Rectangle union = null;
            for (Rectangle bound : limitedBounds) {
                union = union == null ? bound : union.union(bound);
            }
            forcedHeight = union != null ? union.height : 0;
        }
        int xLeft = Integer.MAX_VALUE;
        int yTop = Integer.MAX_VALUE;
        int xRight = Integer.MIN_VALUE;
        int yBottom = Integer.MIN_VALUE;
        for (int i = bounds.size() - 1; i >= 0; i--) {
            final Rectangle each = bounds.get(i);
            if (each.x == Integer.MAX_VALUE) {
                continue;
            }
            xLeft = Math.min(xLeft, each.x);
            yTop = Math.min(yTop, each.y);
            xRight = Math.max(xRight, each.x + each.width);
            yBottom = Math.max(yBottom, each.y + each.height);
        }
        final Dimension dimension = new Dimension(xRight - xLeft, Math.max(yBottom - yTop, forcedHeight));

        JBInsets.addTo(dimension, getInsets());

        return dimension;
    }

    @Override
    public void doLayout() {
        if (!isValid()) {
            calculateBounds(getSize(), myComponentBounds);
        }
        final int componentCount = getComponentCount();
        LOG.assertTrue(componentCount <= myComponentBounds.size());
        for (int i = componentCount - 1; i >= 0; i--) {
            final Component component = getComponent(i);
            component.setBounds(myComponentBounds.get(i));
        }
    }

    /**
     * Calculates bounds of all the components in the toolbar
     */
    private void calculateBounds(@Nonnull Dimension size2Fit, @Nonnull List<Rectangle> bounds) {
        bounds.clear();
        for (int i = 0; i < getComponentCount(); i++) {
            bounds.add(new Rectangle());
        }

        if (myLayoutPolicy == NOWRAP_LAYOUT_POLICY) {
            calculateBoundsNowrapImpl(bounds);
        }
        else if (myLayoutPolicy == WRAP_LAYOUT_POLICY) {
            calculateBoundsWrapImpl(size2Fit, bounds);
        }
        else if (myLayoutPolicy == AUTO_LAYOUT_POLICY) {
            calculateBoundsAutoImp(size2Fit, bounds);
        }
        else {
            throw new IllegalStateException("unknown layoutPolicy: " + myLayoutPolicy);
        }


        if (getComponentCount() > 0 && size2Fit.width < Integer.MAX_VALUE) {
            int maxHeight = 0;
            for (int i = 0; i < bounds.size() - 2; i++) {
                maxHeight = Math.max(maxHeight, bounds.get(i).height);
            }

            int rightOffset = 0;
            Insets insets = getInsets();
            for (int i = getComponentCount() - 1, j = 1; i > 0; i--, j++) {
                final Component component = getComponent(i);
                if (component instanceof JComponent jComponent && jComponent.getClientProperty(RIGHT_ALIGN_KEY) == Boolean.TRUE) {
                    rightOffset += bounds.get(i).width;
                    Rectangle r = bounds.get(bounds.size() - j);
                    r.x = size2Fit.width - rightOffset;
                    r.y = insets.top + (getHeight() - insets.top - insets.bottom - bounds.get(i).height) / 2;
                }
            }
        }
    }

    private void calculateBoundsAutoImp(@Nonnull Dimension sizeToFit, @Nonnull List<? extends Rectangle> bounds) {
        final int componentCount = getComponentCount();
        LOG.assertTrue(componentCount <= bounds.size());

        final boolean actualLayout = bounds == myComponentBounds;

        if (actualLayout) {
            myAutoPopupRec = null;
        }

        int autoButtonSize = AllIcons.Ide.Link.getWidth();
        boolean full = false;

        final Insets insets = getInsets();
        int widthToFit = sizeToFit.width - insets.left - insets.right;
        int heightToFit = sizeToFit.height - insets.top - insets.bottom;

        if (getOrientation() == SwingConstants.HORIZONTAL) {
            int eachX = 0;
            int maxHeight = heightToFit;
            for (int i = 0; i < componentCount; i++) {
                final Component eachComp = getComponent(i);
                final boolean isLast = i == componentCount - 1;

                final Rectangle eachBound = new Rectangle(getChildPreferredSize(i));
                maxHeight = Math.max(eachBound.height, maxHeight);

                if (!full) {
                    boolean inside = isLast ? eachX + eachBound.width <= widthToFit : eachX + eachBound.width + autoButtonSize <= widthToFit;

                    if (inside) {
                        eachBound.x = insets.left + eachX;
                        eachX += eachBound.width;
                        eachBound.y = insets.top;
                    }
                    else {
                        full = true;
                    }
                }

                if (full) {
                    if (myAutoPopupRec == null) {
                        myAutoPopupRec = new Rectangle(insets.left + eachX, insets.top, widthToFit - eachX, heightToFit);
                        myFirstOutsideIndex = i;
                    }
                    eachBound.x = Integer.MAX_VALUE;
                    eachBound.y = Integer.MAX_VALUE;
                }

                bounds.get(i).setBounds(eachBound);
            }

            for (final Rectangle r : bounds) {
                if (r.height < maxHeight) {
                    r.y += (maxHeight - r.height) / 2;
                }
            }

        }
        else {
            int eachY = 0;
            for (int i = 0; i < componentCount; i++) {
                final Rectangle eachBound = new Rectangle(getChildPreferredSize(i));
                if (!full) {
                    boolean outside;
                    if (i < componentCount - 1) {
                        outside = eachY + eachBound.height + autoButtonSize < heightToFit;
                    }
                    else {
                        outside = eachY + eachBound.height < heightToFit;
                    }
                    if (outside) {
                        eachBound.x = insets.left;
                        eachBound.y = insets.top + eachY;
                        eachY += eachBound.height;
                    }
                    else {
                        full = true;
                    }
                }

                if (full) {
                    if (myAutoPopupRec == null) {
                        myAutoPopupRec = new Rectangle(insets.left, insets.top + eachY, widthToFit, heightToFit - eachY);
                        myFirstOutsideIndex = i;
                    }
                    eachBound.x = Integer.MAX_VALUE;
                    eachBound.y = Integer.MAX_VALUE;
                }

                bounds.get(i).setBounds(eachBound);
            }
        }

    }

    private void calculateBoundsNowrapImpl(@Nonnull List<? extends Rectangle> bounds) {
        final int componentCount = getComponentCount();
        LOG.assertTrue(componentCount <= bounds.size());

        final Insets insets = getInsets();

        if (getOrientation() == SwingConstants.HORIZONTAL) {
            final int maxHeight = getMaxButtonHeight();
            int offset = 0;
            for (int i = 0; i < componentCount; i++) {
                final Dimension d = getChildPreferredSize(i);
                final Rectangle r = bounds.get(i);
                r.setBounds(insets.left + offset, insets.top + (maxHeight - d.height) / 2, d.width, d.height);
                offset += d.width;
            }
        }
        else {
            final int maxWidth = getMaxButtonWidth();
            int offset = 0;
            for (int i = 0; i < componentCount; i++) {
                final Dimension d = getChildPreferredSize(i);
                final Rectangle r = bounds.get(i);
                r.setBounds(insets.left + (maxWidth - d.width) / 2, insets.top + offset, d.width, d.height);
                offset += d.height;
            }
        }
    }


    private void calculateBoundsWrapImpl(@Nonnull Dimension sizeToFit, @Nonnull List<? extends Rectangle> bounds) {
        // We have to graceful handle case when toolbar was not laid out yet.
        // In this case we calculate bounds as it is a NOWRAP toolbar.
        if (getWidth() == 0 || getHeight() == 0) {
            try {
                setLayoutPolicy(NOWRAP_LAYOUT_POLICY);
                calculateBoundsNowrapImpl(bounds);
            }
            finally {
                setLayoutPolicy(WRAP_LAYOUT_POLICY);
            }
            return;
        }

        Dimension minSize = TargetAWT.to(myMinimumButtonSize);

        final int componentCount = getComponentCount();
        LOG.assertTrue(componentCount <= bounds.size());

        final Insets insets = getInsets();
        int widthToFit = sizeToFit.width - insets.left - insets.right;
        int heightToFit = sizeToFit.height - insets.top - insets.bottom;

        int orientation = getOrientation();
        if (orientation == SwingConstants.HORIZONTAL) {
            // Calculate row height
            int rowHeight = 0;
            final Dimension[] dims = new Dimension[componentCount]; // we will use this dimensions later
            for (int i = 0; i < componentCount; i++) {
                dims[i] = getChildPreferredSize(i);
                final int height = dims[i].height;
                rowHeight = Math.max(rowHeight, height);
            }

            // Lay components out
            int xOffset = 0;
            int yOffset = 0;
            // Calculate max size of a row. It's not possible to make more then 3 row toolbar
            int maxRowWidth = getMaxRowWidth(widthToFit, minSize.width);

            for (int i = 0; i < componentCount; i++) {
                final Dimension d = dims[i];
                if (xOffset + d.width > maxRowWidth) { // place component at new row
                    xOffset = 0;
                    yOffset += rowHeight;
                }

                final Rectangle each = bounds.get(i);
                each.setBounds(insets.left + xOffset, insets.top + yOffset + (rowHeight - d.height) / 2, d.width, d.height);

                xOffset += d.width;
            }
        }
        else {
            // Calculate row width
            int rowWidth = 0;
            final Dimension[] dims = new Dimension[componentCount]; // we will use this dimensions later
            for (int i = 0; i < componentCount; i++) {
                dims[i] = getChildPreferredSize(i);
                final int width = dims[i].width;
                rowWidth = Math.max(rowWidth, width);
            }

            // Lay components out
            int xOffset = 0;
            int yOffset = 0;
            // Calculate max size of a row. It's not possible to make more then 3 column toolbar
            final int maxRowHeight = Math.max(heightToFit, componentCount * minSize.height / 3);
            for (int i = 0; i < componentCount; i++) {
                final Dimension d = dims[i];
                if (yOffset + d.height > maxRowHeight) { // place component at new row
                    yOffset = 0;
                    xOffset += rowWidth;
                }

                final Rectangle each = bounds.get(i);
                each.setBounds(insets.left + xOffset + (rowWidth - d.width) / 2, insets.top + yOffset, d.width, d.height);

                yOffset += d.height;
            }
        }
    }

    private int getMaxRowWidth(int widthToFit, int maxWidth) {
        int componentCount = getComponentCount();
        // Calculate max size of a row. It's not possible to make more than 3 row toolbar
        int maxRowWidth = Math.max(widthToFit, componentCount * maxWidth / 3);
        for (int i = 0; i < componentCount; i++) {
            final Component component = getComponent(i);
            if (component instanceof JComponent jComponent && jComponent.getClientProperty(RIGHT_ALIGN_KEY) == Boolean.TRUE) {
                maxRowWidth -= getChildPreferredSize(i).width;
            }
        }
        return maxRowWidth;
    }
}
