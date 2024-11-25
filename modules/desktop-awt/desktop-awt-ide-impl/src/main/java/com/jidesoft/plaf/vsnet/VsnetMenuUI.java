/*
 * @(#)VsnetMenuUI.java
 *
 * Copyright 2002 JIDE Software Inc. All rights reserved.
 */

package com.jidesoft.plaf.vsnet;

import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.basic.ThemePainter;
import com.jidesoft.plaf.windows.WindowsGraphicsUtilsPort;
import com.jidesoft.swing.ButtonStyle;
import com.jidesoft.swing.JideSwingUtilities;
import com.jidesoft.swing.TopLevelMenuContainer;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Menu UI implementation
 */
public class VsnetMenuUI extends VsnetMenuItemUI {
    protected ChangeListener changeListener;
    protected PropertyChangeListener propertyChangeListener;
    protected MenuListener menuListener;

    private int lastMnemonic = 0;

    private static final boolean DEBUG = false;  // show bad params, misc.

    private static boolean crossMenuMnemonic = true;

    private boolean isMouseOver = false;
    private PopupMenuListener popupMenuListener;

    public static ComponentUI createUI(JComponent x) {
        return new VsnetMenuUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        if (menuItem instanceof JMenu) {
            ((JMenu) menuItem).setDelay(200);
        }
        crossMenuMnemonic = UIDefaultsLookup.getBoolean("Menu.crossMenuMnemonic");
    }

    @Override
    protected String getPropertyPrefix() {
        return "Menu";
    }

    @Override
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
        if (!(menuItem instanceof JMenu) || !((JMenu) menuItem).isTopLevelMenu()) {
            super.paintBackground(g, menuItem, bgColor);
            return;
        }

        ButtonModel model = menuItem.getModel();
        Color oldColor = g.getColor();
        int menuWidth;
        int menuHeight;
        if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL) {
            menuWidth = menuItem.getWidth();
            menuHeight = menuItem.getHeight();
        }
        else {
            menuWidth = menuItem.getHeight();
            menuHeight = menuItem.getWidth();
        }

        Color borderColor = getPainter().getMenuItemBorderColor();
//        Color mouseHoverBackground = UIDefaultsLookup.getColor("Menu.mouseHoverBackground");
//        Border mouseHoverBorder = UIDefaultsLookup.getBorder("Menu.mouseHoverBorder");

        if (menuItem.isOpaque()) {
            if (menuItem.getParent() != null) {
                g.setColor(menuItem.getParent().getBackground());
            }
            else {
                g.setColor(menuItem.getBackground());
            }
            g.fillRect(0, 0, menuWidth, menuHeight);
        }

        Rectangle rect = new Rectangle(0, 0, menuWidth, menuHeight);
        if (menuItem instanceof JMenu && model.isSelected()) {
            // Draw a dark shadow border without bottom
            // TODO: we should consider the location of popup menu and decide which side not paint.
            // TODO: for example if the menu is going upward, we shouldn't paint the top.
            if (((JMenu) menuItem).getPopupMenu().isVisible()) { // paint differently if popup menu is visible
                if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL) {
                    getPainter().paintMenuItemBackground(menuItem, g, rect, JideSwingUtilities.getOrientationOf(menuItem), ThemePainter.STATE_DEFAULT, false);
                    g.setColor(borderColor);
                    g.drawLine(0, 0, menuWidth - 1, 0);
                    g.drawLine(menuWidth - 1, 0, menuWidth - 1, menuHeight - 1);
                    g.drawLine(0, menuHeight - 1, 0, 1);
                    g.drawLine(0, menuHeight - 1, menuWidth - 1, menuHeight - 1);
                }
                else {
                    getPainter().paintMenuItemBackground(menuItem, g, rect, JideSwingUtilities.getOrientationOf(menuItem), ThemePainter.STATE_DEFAULT, false);
                    g.setColor(borderColor);
                    g.drawLine(0, menuHeight - 1, menuWidth - 1, menuHeight - 1);
                    g.drawLine(menuWidth - 1, 0, menuWidth - 1, menuHeight);
                    g.drawLine(0, menuHeight - 1, 0, 1);
                    g.drawLine(0, 0, menuWidth - 1, 0);
                }
            }
            else {
                getPainter().paintMenuItemBackground(menuItem, g, rect, JideSwingUtilities.getOrientationOf(menuItem), ThemePainter.STATE_ROLLOVER);
            }
        }
        else if (isMouseOver() && model.isEnabled()) {
            getPainter().paintMenuItemBackground(menuItem, g, rect, JideSwingUtilities.getOrientationOf(menuItem), ThemePainter.STATE_ROLLOVER);
        }

        if (isDownArrowVisible(menuItem.getParent())) {
            g.setColor(menuItem.isEnabled() ? Color.BLACK : Color.GRAY);
            int middle = menuWidth - 9;
            g.drawLine(middle - 2, menuHeight / 2 - 1, middle + 2, menuHeight / 2 - 1);
            g.drawLine(middle - 1, menuHeight / 2, middle + 1, menuHeight / 2);
            g.drawLine(middle, menuHeight / 2 + 1, middle, menuHeight / 2 + 1);
        }
        g.setColor(oldColor);
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        if (changeListener == null)
            changeListener = createChangeListener(menuItem);

        if (changeListener != null)
            menuItem.addChangeListener(changeListener);

        if (propertyChangeListener == null)
            propertyChangeListener = createPropertyChangeListener(menuItem);

        if (propertyChangeListener != null)
            menuItem.addPropertyChangeListener(propertyChangeListener);

        if (menuListener == null)
            menuListener = createMenuListener(menuItem);

        if (menuListener != null && menuItem instanceof JMenu) {
            ((JMenu) menuItem).addMenuListener(menuListener);
        }

        if (popupMenuListener == null) {
            popupMenuListener = createPopupMenuListener();
        }
        if (popupMenuListener != null && menuItem instanceof JMenu) {
            ((JMenu) menuItem).getPopupMenu().addPopupMenuListener(popupMenuListener);
        }
    }

    protected PopupMenuListener createPopupMenuListener() {
        return new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                repaint();
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                repaint();
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
                repaint();
            }

            private void repaint() {
                if (menuItem != null) {
                    menuItem.repaint();
                }
            }

        };
    }

    @Override
    protected void installKeyboardActions() {
        super.installKeyboardActions();
        updateMnemonicBinding();
    }

    protected void updateMnemonicBinding() {
        int mnemonic = menuItem.getModel().getMnemonic();
        int[] shortcutKeys = (int[]) UIDefaultsLookup.get("Menu.shortcutKeys");
        if (shortcutKeys == null) {
            shortcutKeys = new int[]{KeyEvent.ALT_MASK};
        }
        if (mnemonic == lastMnemonic) {
            return;
        }
        if (lastMnemonic != 0 && windowInputMap != null) {
            for (int shortcutKey : shortcutKeys) {
                windowInputMap.remove(KeyStroke.getKeyStroke
                    (lastMnemonic, shortcutKey, false));
                windowInputMap.remove(KeyStroke.getKeyStroke
                    (lastMnemonic, shortcutKey, true));
            }
        }
        if (mnemonic != 0) {
            if (windowInputMap == null) {
                windowInputMap = createInputMap(JComponent.
                    WHEN_IN_FOCUSED_WINDOW);
                SwingUtilities.replaceUIInputMap(menuItem, JComponent.
                    WHEN_IN_FOCUSED_WINDOW, windowInputMap);
            }
            for (int shortcutKey : shortcutKeys) {
                windowInputMap.put(KeyStroke.getKeyStroke(mnemonic,
                    shortcutKey, false), "selectMenu");
            }
        }
        lastMnemonic = mnemonic;
    }

    @Override
    protected void uninstallKeyboardActions() {
        super.uninstallKeyboardActions();
    }

    /**
     * The ActionMap for BasicMenUI can not be shared, this is subclassed to create a new one for each invocation.
     */
    @Override
    protected ActionMap getActionMap() {
        return createActionMap();
    }

    /**
     * Invoked to create the ActionMap.
     */
    @Override
    protected ActionMap createActionMap() {
        ActionMap am = super.createActionMap();
        if (am != null && menuItem instanceof JMenu) {
            am.put("selectMenu", new PostAction((JMenu) menuItem, true));
        }
        return am;
    }

    @Override
    protected MouseInputListener createMouseInputListener(JComponent c) {
        return new MouseInputHandler();
    }

    protected MenuListener createMenuListener(JComponent c) {
        return new MenuHandler();
    }

    protected ChangeListener createChangeListener(JComponent c) {
        return null;
    }

    protected PropertyChangeListener createPropertyChangeListener(JComponent c) {
        return new PropertyChangeHandler();
    }

    @Override
    protected void uninstallDefaults() {
        menuItem.setArmed(false);
        menuItem.setSelected(false);
        menuItem.resetKeyboardActions();
        super.uninstallDefaults();
    }

    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();

        if (changeListener != null)
            menuItem.removeChangeListener(changeListener);

        changeListener = null;

        if (propertyChangeListener != null)
            menuItem.removePropertyChangeListener(propertyChangeListener);

        propertyChangeListener = null;

        if (menuListener != null && menuItem instanceof JMenu) {
            ((JMenu) menuItem).removeMenuListener(menuListener);
        }

        menuListener = null;

        if (popupMenuListener != null && menuItem instanceof JMenu) {
            ((JMenu) menuItem).getPopupMenu().removePopupMenuListener(popupMenuListener);
        }

        popupMenuListener = null;
    }

    @Override
    protected MenuDragMouseListener createMenuDragMouseListener(JComponent c) {
        return new MenuDragMouseHandler();
    }

    @Override
    protected MenuKeyListener createMenuKeyListener(JComponent c) {
        return new MenuKeyHandler();
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        if (menuItem instanceof JMenu && ((JMenu) menuItem).isTopLevelMenu()) {
            Dimension d = c.getPreferredSize();
            if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL) {
                return new Dimension(d.width, Short.MAX_VALUE);
            }
            else {
                return new Dimension(Short.MAX_VALUE, d.height);
            }
        }
        return null;
    }

    protected static void setupPostTimer(JMenu menu) {
        Timer timer = new Timer(menu.getDelay(), new PostAction(menu, false));
        timer.setRepeats(false);
        timer.start();
    }

    protected static void appendPath(MenuElement[] path, MenuElement elem) {
        MenuElement newPath[] = new MenuElement[path.length + 1];
        System.arraycopy(path, 0, newPath, 0, path.length);
        newPath[path.length] = elem;
        MenuSelectionManager.defaultManager().setSelectedPath(newPath);
    }

    protected static class PostAction extends AbstractAction {
        JMenu menu;
        boolean force = false;
        private static final long serialVersionUID = 8723756757971612903L;

        PostAction(JMenu menu, boolean shouldForce) {
            this.menu = menu;
            this.force = shouldForce;
        }

        public void actionPerformed(ActionEvent e) {
            if (!crossMenuMnemonic) {
                JPopupMenu pm = getActivePopupMenu();
                if (pm != null && pm != menu.getParent()) {
                    return;
                }
            }

            final MenuSelectionManager defaultManager = MenuSelectionManager.defaultManager();
            if (force) {
                Container cnt = menu.getParent();
                if (cnt != null && cnt instanceof JMenuBar) {
                    MenuElement me[];
                    MenuElement subElements[];

                    subElements = menu.getPopupMenu().getSubElements();
                    if (subElements.length > 0) {
                        me = new MenuElement[4];
                        me[0] = (MenuElement) cnt;
                        me[1] = menu;
                        me[2] = menu.getPopupMenu();
                        me[3] = subElements[0];
                    }
                    else {
                        me = new MenuElement[3];
                        me[0] = (MenuElement) cnt;
                        me[1] = menu;
                        me[2] = menu.getPopupMenu();
                    }
                    defaultManager.setSelectedPath(me);
                }
            }
            else {
                MenuElement path[] = defaultManager.getSelectedPath();
                if (path.length > 0 && path[path.length - 1] == menu) {
                    appendPath(path, menu.getPopupMenu());
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return menu.getModel().isEnabled();
        }
    }


    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (prop.equals(AbstractButton.MNEMONIC_CHANGED_PROPERTY)) {
                updateMnemonicBinding();
            }
            else if (prop.equals(ButtonStyle.BUTTON_STYLE_PROPERTY)) {
                AbstractButton b = (AbstractButton) e.getSource();
                b.repaint();
            }
            else if ("verticalTextPosition".equals(prop)
                || "horizontalTextPosition".equals(prop)) {
                AbstractButton b = (AbstractButton) e.getSource();
                b.updateUI();
            }
        }
    }

    /**
     * Instantiated and used by a menu item to handle the current menu selection from mouse events. A MouseInputHandler
     * processes and forwards all mouse events to a shared instance of the MenuSelectionManager.
     * <p/>
     * This class is protected so that it can be subclassed by other look and feels to implement their own mouse
     * handling behavior. All overridden methods should call the parent methods so that the menu selection is correct.
     *
     * @see MenuSelectionManager
     * @since 1.4
     */
    protected class MouseInputHandler implements MouseInputListener {

        public void mouseClicked(MouseEvent e) {
        }

        /**
         * Invoked when the mouse has been clicked on the menu. This method clears or sets the selection path of the
         * MenuSelectionManager.
         *
         * @param e the mouse event
         */
        public void mousePressed(MouseEvent e) {
            if (!(menuItem instanceof JMenu)) {
                return;
            }

            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled())
                return;

            MenuSelectionManager manager =
                MenuSelectionManager.defaultManager();
            if (menu.getParent() instanceof JMenuBar || menu.getParent() instanceof TopLevelMenuContainer) {
                if (menu.isSelected()) {
                    manager.clearSelectedPath();
                }
                else {
                    //Container cnt = menu.getParent();
                    Container cnt = getFirstParentMenuElement(menu);

                    if (cnt != null && cnt instanceof MenuElement) {
                        ArrayList<Component> parents = new ArrayList<Component>();
                        while (cnt instanceof MenuElement) {
                            parents.add(0, cnt);
                            if (cnt instanceof JPopupMenu) {
                                cnt = (Container) ((JPopupMenu) cnt).getInvoker();
                            }
                            else {
                                //cnt = cnt.getParent();
                                cnt = getFirstParentMenuElement(cnt);
                            }
                        }

                        MenuElement me[] = new MenuElement[parents.size() + 1];
                        for (int i = 0; i < parents.size(); i++) {
                            Container container = (Container) parents.get(i);
                            me[i] = (MenuElement) container;
                        }
                        me[parents.size()] = menu;
                        manager.setSelectedPath(me);
                    }
                    else {
                        MenuElement me[] = new MenuElement[1];
                        me[0] = menu;
                        manager.setSelectedPath(me);
                    }
                }
            }

            MenuElement selectedPath[] = manager.getSelectedPath();
            if (selectedPath.length > 0 &&
                selectedPath[selectedPath.length - 1] != menu.getPopupMenu()) {

                if (menu.isTopLevelMenu() ||
                    menu.getDelay() == 0) {
                    appendPath(selectedPath, menu.getPopupMenu());
                }
                else {
                    setupPostTimer(menu);
                }
            }
        }

        protected Container getFirstParentMenuElement(Component comp) {
            Container parent = comp.getParent();

            while (parent != null) {
                if (parent instanceof MenuElement)
                    return parent;

                parent = parent.getParent();
            }

            return null;
        }

        /**
         * Invoked when the mouse has been released on the menu. Delegates the mouse event to the MenuSelectionManager.
         *
         * @param e the mouse event
         */
        public void mouseReleased(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            if (!(menuItem instanceof JMenu)) {
                return;
            }

            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled())
                return;
            MenuSelectionManager manager =
                MenuSelectionManager.defaultManager();
            manager.processMouseEvent(e);
            if (!e.isConsumed())
                manager.clearSelectedPath();
        }

        /**
         * Invoked when the cursor enters the menu. This method sets the selected path for the MenuSelectionManager and
         * handles the case in which a menu item is used to pop up an additional menu, as in a hierarchical menu
         * system.
         *
         * @param e the mouse event; not used
         */
        public void mouseEntered(MouseEvent e) {
            if (!(menuItem instanceof JMenu)) {
                return;
            }
            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled())
                return;

            MenuSelectionManager manager =
                MenuSelectionManager.defaultManager();
            MenuElement selectedPath[] = manager.getSelectedPath();
            if (!menu.isTopLevelMenu()) {
                if (!(selectedPath.length > 0 &&
                    selectedPath[selectedPath.length - 1] ==
                        menu.getPopupMenu())) {
                    if (menu.getDelay() == 0) {
                        appendPath(getPath(), menu.getPopupMenu());
                    }
                    else {
                        manager.setSelectedPath(getPath());
                        setupPostTimer(menu);
                    }
                }
            }
            else {
                if (selectedPath.length > 0 &&
                    selectedPath[0] == menu.getParent()) {
                    MenuElement newPath[] = new MenuElement[3];
                    // A top level menu's parent is by definition
                    // a JMenuBar
                    newPath[0] = (MenuElement) menu.getParent();
                    newPath[1] = menu;
                    newPath[2] = menu.getPopupMenu();
                    manager.setSelectedPath(newPath);
                }
            }

            if (!SwingUtilities.isLeftMouseButton(e)) {
                setMouseOver(true);
            }
            menuItem.repaint();
        }

        public void mouseExited(MouseEvent e) {
            setMouseOver(false);
            menuItem.repaint();
        }

        /**
         * Invoked when a mouse button is pressed on the menu and then dragged. Delegates the mouse event to the
         * MenuSelectionManager.
         *
         * @param e the mouse event
         * @see MouseMotionListener#mouseDragged
         */
        public void mouseDragged(MouseEvent e) {
            if (!(menuItem instanceof JMenu)) {
                return;
            }
            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled())
                return;
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseMoved(MouseEvent e) {
        }
    }

    private class MenuHandler implements MenuListener {
        public void menuSelected(MenuEvent e) {
        }

        public void menuDeselected(MenuEvent e) {
        }

        public void menuCanceled(MenuEvent e) {
            if (!(menuItem instanceof JMenu)) {
                return;
            }
            JMenu m = (JMenu) e.getSource();
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            if (manager.isComponentPartOfCurrentMenu(m))
                MenuSelectionManager.defaultManager().clearSelectedPath();
        }

    }

    private class MenuDragMouseHandler implements MenuDragMouseListener {
        public void menuDragMouseEntered(MenuDragMouseEvent e) {
        }

        public void menuDragMouseDragged(MenuDragMouseEvent e) {
            if (!(menuItem instanceof JMenu)) {
                return;
            }
            if (!menuItem.isEnabled())
                return;

            MenuSelectionManager manager = e.getMenuSelectionManager();
            MenuElement path[] = e.getPath();

            Point p = e.getPoint();
            if (p.x >= 0 && p.x < menuItem.getWidth() &&
                p.y >= 0 && p.y < menuItem.getHeight()) {
                JMenu menu = (JMenu) menuItem;
                MenuElement selectedPath[] = manager.getSelectedPath();
                if (!(selectedPath.length > 0 &&
                    selectedPath[selectedPath.length - 1] ==
                        menu.getPopupMenu())) {
                    if (menu.isTopLevelMenu() ||
                        menu.getDelay() == 0 ||
                        e.getID() == MouseEvent.MOUSE_DRAGGED) {
                        appendPath(path, menu.getPopupMenu());
                    }
                    else {
                        manager.setSelectedPath(path);
                        setupPostTimer(menu);
                    }
                }
            }
            else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                Component comp = manager.componentForPoint(e.getComponent(), e.getPoint());
                if (comp == null)
                    manager.clearSelectedPath();
            }

        }

        public void menuDragMouseExited(MenuDragMouseEvent e) {
        }

        public void menuDragMouseReleased(MenuDragMouseEvent e) {
        }
    }

    static JPopupMenu getActivePopupMenu() {
        MenuElement[] path = MenuSelectionManager.defaultManager().
            getSelectedPath();
        for (int i = path.length - 1; i >= 0; i--) {
            MenuElement elem = path[i];
            if (elem instanceof JPopupMenu) {
                return (JPopupMenu) elem;
            }
        }
        return null;
    }

    /**
     * Handles the mnemonic handling for the JMenu and JMenuItems.
     */
    private class MenuKeyHandler implements MenuKeyListener {

        /**
         * Opens the SubMenu
         */
        public void menuKeyTyped(MenuKeyEvent e) {
            if (DEBUG) {
                System.out.println("in BasicMenuUI.menuKeyTyped for " + menuItem.getText());
            }
            if (!crossMenuMnemonic) {
                JPopupMenu pm = getActivePopupMenu();
                if (pm != null && pm != menuItem.getParent()) {
                    return;
                }
            }

            int key = menuItem.getMnemonic();
            if (key == 0)
                return;
            MenuElement path[] = e.getPath();
            if (lower((char) key) == lower(e.getKeyChar())) {
                JPopupMenu popupMenu = ((JMenu) menuItem).getPopupMenu();
                ArrayList newList = new ArrayList(Arrays.asList(path));
                newList.add(popupMenu);
                MenuElement sub[] = popupMenu.getSubElements();
                if (sub.length > 0) {
                    newList.add(sub[0]);
                }
                MenuSelectionManager manager = e.getMenuSelectionManager();
                MenuElement newPath[] = new MenuElement[0];
                newPath = (MenuElement[]) newList.toArray(newPath);
                manager.setSelectedPath(newPath);
                e.consume();
            }
        }

        /**
         * Handles the mnemonics for the menu items. Will also handle duplicate mnemonics. Perhaps this should be moved
         * into BasicPopupMenuUI. See 4670831
         */
        public void menuKeyPressed(MenuKeyEvent e) {
            if (DEBUG) {
                System.out.println("in BasicMenuUI.menuKeyPressed for " + menuItem.getText());
            }
            // Handle the case for Escape or Enter...
            char keyChar = e.getKeyChar();
            if (!Character.isLetterOrDigit(keyChar))
                return;

            MenuSelectionManager manager = e.getMenuSelectionManager();
            MenuElement path[] = e.getPath();
            MenuElement selectedPath[] = manager.getSelectedPath();

            for (int i = selectedPath.length - 1; i >= 0; i--) {
                if (selectedPath[i] == menuItem) {
                    JPopupMenu popupMenu = ((JMenu) menuItem).getPopupMenu();
                    if (!popupMenu.isVisible()) {
                        return; // Do not invoke items from invisible popup
                    }
                    MenuElement items[] = popupMenu.getSubElements();

                    MenuElement currentItem = selectedPath[selectedPath.length - 1];
                    int currentIndex = -1;
                    int matches = 0;
                    int firstMatch = -1;
                    int indexes[] = null;

                    for (int j = 0; j < items.length; j++) {
                        if (!(items[j] instanceof JMenuItem)) {
                            continue;
                        }
                        int key = ((JMenuItem) items[j]).getMnemonic();
                        if (lower((char) key) == lower(keyChar)) {
                            if (matches == 0) {
                                firstMatch = j;
                                matches++;
                            }
                            else {
                                if (indexes == null) {
                                    indexes = new int[items.length];
                                    indexes[0] = firstMatch;
                                }
                                indexes[matches++] = j;
                            }
                        }
                        if (currentItem == items[j]) {
                            currentIndex = matches - 1;
                        }
                    }

                    if (matches == 0) {
                        // no op (consume)
                    }
                    else if (matches == 1) {
                        // Invoke the menu action
                        JMenuItem item = (JMenuItem) items[firstMatch];
                        if (!(item instanceof JMenu)) {
                            // Let Submenus be handled by menuKeyTyped
                            manager.clearSelectedPath();
                            item.doClick();
                        }
                    }
                    else {
                        // Select the menu item with the matching mnemonic. If
                        // the same mnemonic has been invoked then select the next
                        // menu item in the cycle.
                        MenuElement newItem = null;
                        if (indexes != null) {
                            newItem = items[indexes[(currentIndex + 1) % matches]];
                        }

                        MenuElement newPath[] = new MenuElement[path.length + 2];
                        System.arraycopy(path, 0, newPath, 0, path.length);
                        newPath[path.length] = popupMenu;
                        newPath[path.length + 1] = newItem;
                        manager.setSelectedPath(newPath);
                    }
                    e.consume();
                    return;
                }
            }
        }

        public void menuKeyReleased(MenuKeyEvent e) {
        }

        private char lower(char keyChar) {
            return Character.toLowerCase(keyChar);
        }
    }

    /**
     * Method which renders the text of the current menu item.
     * <p/>
     *
     * @param g        Graphics context
     * @param menuItem Current menu item to render
     * @param textRect Bounding rectangle to render the text.
     * @param text     String to render
     * @since 1.4
     */
    @Override
    protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
        // Note: This method is almost identical to the same method in WindowsMenuItemUI
        ButtonModel model = menuItem.getModel();

        if (!(menuItem instanceof JMenu) || !((JMenu) menuItem).isTopLevelMenu()) {
            if (menuItem.getComponentOrientation().isLeftToRight()) {
                int defaultTextIconGap = UIDefaultsLookup.getInt("MenuItem.textIconGap");
                int defaultShadowWidth = UIDefaultsLookup.getInt("MenuItem.shadowWidth");
                textRect.x = defaultShadowWidth + defaultTextIconGap;
            }
            else {
                int defaultTextIconGap = UIDefaultsLookup.getInt("MenuItem.textIconGap");
                int defaultShadowWidth = UIDefaultsLookup.getInt("MenuItem.shadowWidth");
                textRect.x = viewRect.width - (defaultShadowWidth + defaultTextIconGap + textRect.width);
            }
        }

        if (!model.isEnabled()) {
            // *** paint the text disabled
            WindowsGraphicsUtilsPort.paintText((Graphics2D) g, menuItem, textRect, text, 0);
        }
        else {
            FontMetrics fm = g.getFontMetrics();
            int mnemonicIndex = menuItem.getDisplayedMnemonicIndex();
            // W2K Feature: Check to see if the Underscore should be rendered.
            if (LookAndFeelFactory.isMnemonicHidden()) {
                mnemonicIndex = -1;
            }

            Color oldColor = g.getColor();

            // For Win95, the selected text color is the selection foreground color
            if (model.isSelected()) {
                if (/*SystemInfo.isClassicWindows() ||
                 */menuItem instanceof JMenu && !((JMenu) menuItem).isTopLevelMenu()) {

                    g.setColor(selectionForeground); // Uses protected field.
                }
            }
            BasicGraphicsUtils.drawStringUnderlineCharAt(menuItem, (Graphics2D) g, text,
                mnemonicIndex,
                textRect.x,
                textRect.y + fm.getAscent());
            g.setColor(oldColor);
        }
//        }
    }

    /**
     * Set the temporary flag to indicate if the mouse has entered the menu.
     *
     * @param over the flag
     */
    protected void setMouseOver(boolean over) {
        isMouseOver = over;
        menuItem.getModel().setRollover(isMouseOver);
    }

    /**
     * Get the temporary flag to indicate if the mouse has entered the menu.
     *
     * @return the flag.
     */
    protected boolean isMouseOver() {
        return isMouseOver;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);
        if (menuItem instanceof JMenu && ((JMenu) menuItem).isTopLevelMenu() &&
            isDownArrowVisible(menuItem.getParent())) {
            if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL)
                size.width += 11;
            else
                size.height += 11;
        }
        return size;
    }
}



