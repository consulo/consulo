/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 *
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
package consulo.desktop.awt.navbar.ui;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.navigationBar.model.NavBarItemPresentationData;
import consulo.navigationBar.model.NavBarItemVm;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @see NewNavBarPanel
 */
public final class NavBarItemComponent extends SimpleColoredComponent {
    private final NavBarItemVm myVm;
    private final NewNavBarPanel myPanel;

    private boolean myHovered;

    NavBarItemComponent(NavBarItemVm vm, NewNavBarPanel panel, boolean installPopupHandler) {
        myVm = vm;
        myPanel = panel;

        setOpaque(false);
        setIpad(NavBarUi.navBarItemInsets());
        setIconTextGap(JBUI.scale(4));
        setBorder(null);
        if (isItemComponentFocusable()) {
            // Take ownership of Tab/Shift-Tab navigation (to move focus out of nav bar panel), as
            // navigation between items is handled by the Left/Right cursor keys. This is similar
            // to the behavior a JRadioButton contained inside a GroupBox.
            setFocusable(true);
            setFocusTraversalKeysEnabled(false);
            addKeyListener(new NavBarItemComponentTabKeyListener(panel));
            if (isFloating()) {
                addFocusListener(new NavBarDialogFocusListener(panel));
            }
        }
        else {
            setFocusable(false);
        }

        if (installPopupHandler) {
            addMouseListener(new ItemPopupHandler());
        }

        addMouseListener(new ItemMouseListener());
    }

    static boolean isItemComponentFocusable() {
        return ScreenReader.isActive();
    }

    private class ItemPopupHandler extends PopupHandler {
        @Override
        public void invokePopup(Component comp, int x, int y) {
            focusItem();
            myVm.select();
            ActionPopupMenu popupMenu = ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.NAVIGATION_BAR_POPUP, new NavBarContextMenuActionGroup());
            popupMenu.setTargetComponent(myPanel);
            popupMenu.getComponent().show(myPanel, NavBarItemComponent.this.getX() + x, NavBarItemComponent.this.getY() + y);
        }
    }

    private class ItemMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (!Platform.current().os().isWindows()) {
                click(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (Platform.current().os().isWindows()) {
                click(e);
            }
        }

        private void click(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            if (e.isPopupTrigger()) {
                return;
            }
            if (e.getClickCount() == 1) {
                focusItem();
                myVm.select();
                myVm.showPopup();
                e.consume();
            }
            else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                myVm.activate();
                e.consume();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            myHovered = true;
            update();
            e.consume();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            myHovered = false;
            update();
            e.consume();
        }
    }

    public String getText() {
        return myVm.getPresentation().text();
    }

    NavBarItemVm getVm() {
        return myVm;
    }

    private boolean isFloating() {
        return myPanel.isFloating();
    }

    private boolean isItemSelected() {
        return myVm.isSelected();
    }

    private boolean isItemFocused() {
        return myPanel.isItemFocused();
    }

    @Override
    public Font getFont() {
        Font font = NavBarUi.navBarItemFont();
        return font != null ? font : super.getFont();
    }

    @Override
    public void setOpaque(boolean isOpaque) {
        super.setOpaque(false);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // uniform item height regardless of icon presence, otherwise icon-less items get a different height
        // and their text is no longer on the same line as the icons of the siblings
        size.height = Math.max(size.height, JBUI.scale(Image.DEFAULT_ICON_SIZE) + getIpad().top + getIpad().bottom);
        Dimension offsets = new Dimension();
        JBInsets.addTo(offsets, NavBarUi.navBarItemPadding(isFloating()));
        if (myVm.isFirst()) {
            offsets.width += NavBarUi.firstElementLeftOffset();
        }
        else {
            offsets.width += chevron().getWidth() + NavBarUi.chevronInset();
        }
        return new Dimension(size.width + offsets.width, size.height + offsets.height);
    }

    void focusItem() {
        JComponent focusComponent = isFocusable() ? this : myPanel;
        IdeFocusManager.getGlobalInstance().requestFocus(focusComponent, true);
    }

    void update() {
        clear();

        boolean selected = isItemSelected();
        boolean focused = isItemFocused();

        NavBarItemPresentationData presentation = myVm.getPresentation();
        SimpleTextAttributes attributes =
            presentation.textAttributes() != null ? presentation.textAttributes() : SimpleTextAttributes.REGULAR_ATTRIBUTES;
        Color fg;
        if (myHovered) {
            fg = NavBarUi.hoverForeground();
        }
        else if (selected && focused) {
            fg = NavBarUi.selectionForeground();
        }
        else {
            fg = NavBarUi.navBarItemForeground(selected, focused, myVm.isInactive());
            if (fg == null) {
                fg = attributes.getFgColor();
            }
            if (fg == null) {
                fg = isFloating() ? NavBarUi.floatingForeground() : NavBarUi.foreground();
            }
        }
        Color bg = NavBarUi.navBarItemBackground(selected, focused);

        setIcon(effectiveIcon(presentation));
        setBackground(bg);
        append(presentation.text(), new SimpleTextAttributes(bg, fg, attributes.getWaveColor(), attributes.getStyle()));
        revalidate();
        repaint();
    }

    private @Nullable Image effectiveIcon(NavBarItemPresentationData presentation) {
        if (myVm.isLast() || presentation.hasContainingFile()) {
            return presentation.icon();
        }
        else {
            return null;
        }
    }

    @Override
    protected boolean shouldDrawBackground() {
        return isItemSelected() && isItemFocused();
    }

    private static Image chevron() {
        return PlatformIconGroup.generalArrowright();
    }

    @Override
    protected void doPaint(Graphics2D g) {
        Insets paddings = JBInsets.create(NavBarUi.navBarItemPadding(isFloating()));
        boolean isFirst = myVm.isFirst();

        Rectangle rect = new Rectangle(getSize());
        JBInsets.removeFrom(rect, paddings);
        int offset = rect.x;
        if (isFirst) {
            int delta = NavBarUi.firstElementLeftOffset();
            offset += delta;
            rect.width -= delta;
        }
        else {
            Image chevron = chevron();
            paintIcon(g, chevron, offset);
            int delta = chevron.getWidth() + NavBarUi.chevronInset();
            offset += delta;
            rect.width -= delta;
        }
        Color highlightColor = highlightColor();
        if (highlightColor != null) {
            g.setColor(highlightColor);
            int arc = JBUI.scale(8);
            g.fillRoundRect(offset, rect.y, rect.width, rect.height, arc, arc);
        }

        Image icon = getIcon();
        offset += getIpad().left;
        if (icon != null) {
            paintIcon(g, icon, offset);
            offset += icon.getWidth();
            offset += getIconTextGap();
        }
        doPaintText(g, offset, false);
    }

    private @Nullable Color highlightColor() {
        if (myHovered) {
            return NavBarUi.hoverBackground();
        }
        else if (isItemSelected() && isItemFocused()) {
            // no highlight for a selected but unfocused item — matches the legacy nav bar,
            // where the selection disappears once the focus leaves the bar (and its popup)
            return NavBarUi.selectionBackground();
        }
        else {
            return null;
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleNavBarItem();
        }
        return accessibleContext;
    }

    private class AccessibleNavBarItem extends AccessibleSimpleColoredComponent implements AccessibleAction {
        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PUSH_BUTTON;
        }

        @Override
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        @Override
        public int getAccessibleActionCount() {
            return 1;
        }

        @Override
        public @Nullable String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return UIManager.getString("AbstractButton.clickText");
            }
            return null;
        }

        @Override
        public boolean doAccessibleAction(int i) {
            if (i == 0) {
                myVm.select();
                return true;
            }
            return false;
        }
    }
}
