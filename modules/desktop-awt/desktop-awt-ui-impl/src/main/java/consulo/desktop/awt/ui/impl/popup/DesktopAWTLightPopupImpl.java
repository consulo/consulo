/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl.popup;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.PopupCloseEvent;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class DesktopAWTLightPopupImpl extends SwingComponentDelegate<DesktopAWTPopupPanel> implements LightPopup {
    private final PopupOptions myOptions;

    private @Nullable ColorValue myBackground;

    private java.awt.@Nullable Component myTarget;
    private @Nullable Rectangle myPoint;
    private @Nullable JLayeredPane myLayeredPane;
    private boolean myOpen;

    private final AWTEventListener myDismissListener = this::dispatched;

    private final ComponentListener myLayeredPaneListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            relayout();
        }
    };

    private final HierarchyBoundsListener myTargetBoundsListener = new HierarchyBoundsAdapter() {
        @Override
        public void ancestorMoved(HierarchyEvent e) {
            relayout();
        }

        @Override
        public void ancestorResized(HierarchyEvent e) {
            relayout();
        }
    };

    private final HierarchyListener myTargetHierarchyListener = e -> {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            java.awt.Component target = myTarget;
            if (target != null && !target.isShowing()) {
                close();
            }
        }
    };

    public DesktopAWTLightPopupImpl(PopupOptions options) {
        myOptions = options;
    }

    @Override
    protected DesktopAWTPopupPanel createComponent() {
        return new DesktopAWTPopupPanel(this);
    }

    @Override
    @RequiredUIAccess
    public void setTitle(@Nullable String title) {
        toAWTComponent().setTitle(title);
        relayout();
    }

    @Override
    @RequiredUIAccess
    public void setContent(Component content) {
        java.awt.Component component = TargetAWT.to(content);
        if (component instanceof javax.swing.JComponent jComponent) {
            jComponent.setOpaque(false);
        }
        toAWTComponent().setPopupContent(component);
        relayout();
    }

    @Override
    @RequiredUIAccess
    public void setMinimumWidth(int width) {
        toAWTComponent().setMinimumWidth(width);
        relayout();
    }

    @Override
    @RequiredUIAccess
    public void pack() {
        relayout();
    }

    @Override
    public @Nullable ColorValue getBackgroundColor() {
        return myBackground;
    }

    @Override
    public void setBackgroundColor(@Nullable ColorValue background) {
        myBackground = background;
        toAWTComponent().repaint();
    }

    Color fillColor() {
        ColorValue background = myBackground;
        return TargetAWT.to(background != null ? background : ComponentColors.LAYOUT);
    }

    Color borderColor() {
        ColorValue background = myBackground;
        if (background == null) {
            return TargetAWT.to(ComponentColors.BORDER);
        }
        Color fill = TargetAWT.to(background);
        return ColorUtil.isDark(fill) ? ColorUtil.brighter(fill, 3) : ColorUtil.darker(fill, 3);
    }

    @Override
    @RequiredUIAccess
    public void showBy(Component target) {
        open(TargetAWT.to(target), null);
    }

    @Override
    @RequiredUIAccess
    public void showAt(Component target, int x, int y, int anchorHeight) {
        open(TargetAWT.to(target), new Rectangle(x, y, 0, Math.max(anchorHeight, 0)));
    }

    @RequiredUIAccess
    private void open(java.awt.@Nullable Component target, @Nullable Rectangle point) {
        if (target == null || !target.isShowing()) {
            return;
        }
        JRootPane rootPane = SwingUtilities.getRootPane(target);
        if (rootPane == null) {
            return;
        }
        if (myOpen) {
            detach();
        }

        myTarget = target;
        myPoint = point;
        JLayeredPane layeredPane = rootPane.getLayeredPane();
        myLayeredPane = layeredPane;

        DesktopAWTPopupPanel panel = toAWTComponent();
        layeredPane.add(panel, JLayeredPane.POPUP_LAYER, 0);
        layeredPane.addComponentListener(myLayeredPaneListener);
        target.addHierarchyBoundsListener(myTargetBoundsListener);
        target.addHierarchyListener(myTargetHierarchyListener);
        Toolkit.getDefaultToolkit().addAWTEventListener(myDismissListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        myOpen = true;

        relayout();

        if (myOptions.isRequestFocus()) {
            panel.requestFocusInWindow();
        }
    }

    private void relayout() {
        JLayeredPane layeredPane = myLayeredPane;
        java.awt.Component target = myTarget;
        if (!myOpen || layeredPane == null || target == null) {
            return;
        }

        DesktopAWTPopupPanel panel = toAWTComponent();
        Rectangle point = myPoint;
        Rectangle anchor = SwingUtilities.convertRectangle(
            target,
            point != null ? point : new Rectangle(0, 0, target.getWidth(), target.getHeight()),
            layeredPane
        );
        Rectangle area = new Rectangle(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        boolean arrow = myOptions.hasArrow();

        PopupPosition position = myOptions.getPosition();
        DesktopAWTPopupSide side = sideFor(position, arrow);
        Rectangle bounds = place(position, side, anchor, panel.sizeFor(side), point != null);
        if (!fits(bounds, area)) {
            PopupPosition flipped = flip(position);
            DesktopAWTPopupSide flippedSide = sideFor(flipped, arrow);
            Rectangle flippedBounds = place(flipped, flippedSide, anchor, panel.sizeFor(flippedSide), point != null);
            if (fits(flippedBounds, area)) {
                side = flippedSide;
                bounds = flippedBounds;
            }
        }

        Insets shadow = DesktopAWTPopupShadow.insets();
        bounds.x = clamp(bounds.x, area.x - shadow.left, area.x + area.width - bounds.width + shadow.right);
        bounds.y = clamp(bounds.y, area.y - shadow.top, area.y + area.height - bounds.height + shadow.bottom);

        panel.setBounds(bounds);
        panel.setArrow(side, tip(side, anchor, bounds, panel, point != null));
        panel.revalidate();
        panel.repaint();
        layeredPane.repaint();
    }

    private static DesktopAWTPopupSide sideFor(PopupPosition position, boolean arrow) {
        if (!arrow) {
            return DesktopAWTPopupSide.NONE;
        }
        return switch (position) {
            case BOTTOM -> DesktopAWTPopupSide.TOP;
            case TOP -> DesktopAWTPopupSide.BOTTOM;
            case END -> DesktopAWTPopupSide.LEFT;
            case START -> DesktopAWTPopupSide.RIGHT;
        };
    }

    private static PopupPosition flip(PopupPosition position) {
        return switch (position) {
            case BOTTOM -> PopupPosition.TOP;
            case TOP -> PopupPosition.BOTTOM;
            case END -> PopupPosition.START;
            case START -> PopupPosition.END;
        };
    }

    private static Rectangle place(PopupPosition position, DesktopAWTPopupSide side, Rectangle anchor, Dimension size, boolean point) {
        Insets shadow = DesktopAWTPopupShadow.insets();
        int startX = point && side != DesktopAWTPopupSide.NONE
            ? anchor.x - DesktopAWTPopupPanel.arc() / 2 - DesktopAWTPopupPanel.arrowWidth() / 2
            : anchor.x;
        return switch (position) {
            case BOTTOM -> new Rectangle(startX - shadow.left, anchor.y + anchor.height - shadow.top, size.width, size.height);
            case TOP -> new Rectangle(startX - shadow.left, anchor.y - size.height + shadow.bottom, size.width, size.height);
            case END -> new Rectangle(
                anchor.x + anchor.width - shadow.left,
                verticalStart(anchor, side, size, shadow),
                size.width,
                size.height
            );
            case START -> new Rectangle(
                anchor.x - size.width + shadow.right,
                verticalStart(anchor, side, size, shadow),
                size.width,
                size.height
            );
        };
    }

    private static int verticalStart(Rectangle anchor, DesktopAWTPopupSide side, Dimension size, Insets shadow) {
        if (side == DesktopAWTPopupSide.NONE) {
            return anchor.y - shadow.top;
        }
        int bodyHeight = size.height - shadow.top - shadow.bottom;
        return anchor.y + anchor.height / 2 - bodyHeight / 2 - shadow.top;
    }

    private static int tip(DesktopAWTPopupSide side, Rectangle anchor, Rectangle bounds, DesktopAWTPopupPanel panel, boolean point) {
        if (side == DesktopAWTPopupSide.NONE) {
            return 0;
        }
        Insets margins = panel.marginsFor(side);
        int edge = DesktopAWTPopupPanel.arc() / 2 + DesktopAWTPopupPanel.arrowWidth() / 2;
        if (side.isVertical()) {
            int x = (point ? anchor.x : anchor.x + anchor.width / 2) - bounds.x;
            return clamp(x, margins.left + edge, bounds.width - margins.right - edge);
        }
        int y = anchor.y + anchor.height / 2 - bounds.y;
        return clamp(y, margins.top + edge, bounds.height - margins.bottom - edge);
    }

    private static boolean fits(Rectangle bounds, Rectangle area) {
        Insets shadow = DesktopAWTPopupShadow.insets();
        return bounds.x + shadow.left >= area.x
            && bounds.y + shadow.top >= area.y
            && bounds.x + bounds.width - shadow.right <= area.x + area.width
            && bounds.y + bounds.height - shadow.bottom <= area.y + area.height;
    }

    private static int clamp(int value, int min, int max) {
        return max < min ? min : Math.max(min, Math.min(max, value));
    }

    private void dispatched(AWTEvent event) {
        if (!myOpen) {
            return;
        }
        DesktopAWTPopupPanel panel = toAWTComponent();
        if (event instanceof MouseEvent mouseEvent
            && mouseEvent.getID() == MouseEvent.MOUSE_PRESSED
            && myOptions.isCancelOnClickOutside()
            && isOutside(mouseEvent.getComponent(), panel)) {
            close();
        }
        else if (event instanceof KeyEvent keyEvent
            && keyEvent.getID() == KeyEvent.KEY_PRESSED
            && keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE
            && myOptions.isCancelOnEscape()
            && sameWindow(keyEvent.getComponent())) {
            keyEvent.consume();
            close();
        }
    }

    private boolean isOutside(java.awt.@Nullable Component component, DesktopAWTPopupPanel panel) {
        JLayeredPane layeredPane = myLayeredPane;
        if (component == null || layeredPane == null || SwingUtilities.isDescendingFrom(component, panel)) {
            return false;
        }
        if (!sameWindow(component)) {
            return false;
        }
        for (java.awt.Component current = component; current != null; current = current.getParent()) {
            if (current.getParent() == layeredPane) {
                return layeredPane.getLayer(current) < JLayeredPane.POPUP_LAYER;
            }
        }
        return true;
    }

    private boolean sameWindow(java.awt.@Nullable Component component) {
        JLayeredPane layeredPane = myLayeredPane;
        return component != null && layeredPane != null
            && SwingUtilities.getWindowAncestor(component) == SwingUtilities.getWindowAncestor(layeredPane);
    }

    private void detach() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(myDismissListener);
        java.awt.Component target = myTarget;
        if (target != null) {
            target.removeHierarchyBoundsListener(myTargetBoundsListener);
            target.removeHierarchyListener(myTargetHierarchyListener);
        }
        JLayeredPane layeredPane = myLayeredPane;
        if (layeredPane != null) {
            layeredPane.removeComponentListener(myLayeredPaneListener);
            DesktopAWTPopupPanel panel = toAWTComponent();
            Rectangle bounds = panel.getBounds();
            layeredPane.remove(panel);
            layeredPane.repaint(bounds);
        }
        myTarget = null;
        myLayeredPane = null;
        myOpen = false;
    }

    @Override
    @RequiredUIAccess
    public void close() {
        if (!myOpen) {
            return;
        }
        detach();
        getListenerDispatcher(PopupCloseEvent.class).onEvent(new PopupCloseEvent(this));
    }

    @Override
    public boolean isVisible() {
        return myOpen;
    }

    @Override
    public void dispose() {
        if (myOpen) {
            close();
        }
    }
}
