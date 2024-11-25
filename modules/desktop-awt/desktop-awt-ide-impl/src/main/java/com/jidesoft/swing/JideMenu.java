/*
 * @(#)JideMenu.java
 *
 * Copyright 2002 - 2004 JIDE Software Inc. All rights reserved.
 */
package com.jidesoft.swing;

import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.utils.SystemInfo;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A special implementation of JMenu. It is used to replace JMenu in order to use with CommandBar. <br> It has two
 * special features. First, it has a PopupMenuCustomizer for lazy menu creation. Instead of creating menu upfront which
 * might be quite expensive, you can create it using PopupMenuCustomizer. PopupMenuCustomizer is called before the menu
 * is set visible. Please note, when you use PopupMenuCustomizer, you need to remove the old menu items you added
 * previously using PopupMenuCustomizer. Otherwise, you will see a menu which gets longer and longer when you show it.
 * See below for an example.
 * <code><pre>
 * JideMenu jideMenu = new JideMenu("Dynamic");
 * jideMenu.setPopupMenuCustomizer(new JideMenu.PopupMenuCustomizer(){
 *     public void customize(JPopupMenu menu) {
 *         menu.add("item 1");
 *         menu.add("item 2");
 *         menu.add("item 3");
 *         menu.add("item 4");
 *         menu.add("item 5");
 *     }
 * });
 * </pre></code>
 * <p/>
 * Second feature is popup alignment. Usually menu and its popup align to the left side. In our case, we hope they align
 * to right side. So we added a method call setPreferredPopupHorizontalAlignment(). You can set to RIGHT if you want
 * to.
 * <p/>
 */
public class JideMenu extends JMenu implements Alignable {

    private int _preferredPopupHorizontalAlignment = LEFT;

    private int _preferredPopupVerticalAlignment = BOTTOM;

    private MenuCreator _menuCreator;

    private PopupMenuCustomizer _customizer;

    private PopupMenuOriginCalculator _originCalculator;

    public static int DELAY = 400;

    private int _orientation;

    public JideMenu() {
        initMenu();
    }

    public JideMenu(String s) {
        super(s);
        initMenu();
    }

    public JideMenu(Action a) {
        super(a);
        initMenu();
    }

    public JideMenu(String s, boolean b) {
        super(s, b);
        initMenu();
    }

    protected void initMenu() {
//        setDelay(DELAY);
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                MenuCreator menuCreator;
                if ((menuCreator = getMenuCreator()) != null) {
                    menuCreator.createMenu();
                    if (getPopupMenu().getComponentCount() == 0) {
                        return;
                    }
                }

                PopupMenuCustomizer customizer;
                if ((customizer = getPopupMenuCustomizer()) != null) {
                    customizer.customize(getPopupMenu());
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
    }

    /**
     * Checks if the menu is added to a top level menu container. It will be consider as top level menu when <br> 1.
     * getParent() equals null, or <br> 2. getParent() is not an instance of JPopupMenu <br> Please note, the definition
     * of topLevelMenu is different from that of JMenu.
     *
     * @return true if it's top level menu.
     */
    @Override
    public boolean isTopLevelMenu() {
        return getParent() == null || !(getParent() instanceof JPopupMenu);//TopLevelMenuContainer || getParent() instanceof JMenuBar;
    }

    /**
     * @deprecated The createMenu method of MenuCreator should JPopupMenu as parameter. Since it's a public API we have
     * to deprecated this one and ask users to use {@link PopupMenuCustomizer} instead.
     */
    @Deprecated
    public interface MenuCreator {
        void createMenu();
    }

    /**
     * Customizes the popup menu. This method will be called every time before popup menu is set visible.
     */
    public interface PopupMenuCustomizer {
        void customize(JPopupMenu menu);
    }

    /**
     * Calculates the origin of the popup menu if specified.
     */
    public interface PopupMenuOriginCalculator {
        Point getPopupMenuOrigin(JideMenu menu);
    }

    /**
     * Gets the PopupMenuOriginCalculator or <code>null</code>, if none has been specified.
     *
     * @return the calculator
     */
    public PopupMenuOriginCalculator getOriginCalculator() {
        return _originCalculator;
    }

    /**
     * Sets the PopupMenuOriginCalculator that will be used to determine the popup menu origin.
     *
     * @param originCalculator the calculator
     */
    public void setOriginCalculator(PopupMenuOriginCalculator originCalculator) {
        this._originCalculator = originCalculator;
    }

    /**
     * Gets the MenuCreator.
     *
     * @return the MenuCreator.
     * @deprecated use{@link PopupMenuCustomizer} and {@link #getPopupMenuCustomizer()} instead.
     */
    @Deprecated
    public MenuCreator getMenuCreator() {
        return _menuCreator;
    }

    /**
     * Sets the MenuCreator. MenuCreator can be used to do lazy menu creation. If you put code in the MenuCreator, it
     * won't be called until before the menu is set visible.
     *
     * @param menuCreator he menu creator
     * @deprecated use{@link PopupMenuCustomizer} and {@link #setPopupMenuCustomizer(com.jidesoft.swing.JideMenu.PopupMenuCustomizer)}
     * instead.
     */
    @Deprecated
    public void setMenuCreator(MenuCreator menuCreator) {
        _menuCreator = menuCreator;
    }

    /**
     * Gets the PopupMenuCustomizer.
     *
     * @return the PopupMenuCustomizer.
     */
    public PopupMenuCustomizer getPopupMenuCustomizer() {
        return _customizer;
    }

    /**
     * Sets the PopupMenuCustomizer. PopupMenuCustomizer can be used to do lazy menu creation. If you put code in the
     * MenuCreator, it won't be called until before the menu is set visible.
     * <p/>
     * PopupMenuCustomizer has a customize method. The popup menu of this menu will be passed in. You can
     * add/remove/change the menu items in customize method. For example, instead of
     * <code><pre>
     * JideMenu menu = new JideMenu();
     * menu.add(new JMenuItem("..."));
     * menu.add(new JMenuItem("..."));
     * </pre></code>
     * You can do
     * <code><pre>
     * JideMenu menu = new JideMenu();
     * menu.setPopupMenuCustomzier(new JideMenu.PopupMenuCustomizer() {
     *     void customize(JPopupMenu popupMenu) {
     *         poupMenu.removeAll();
     *         popupMenu.add(new JMenuItem("..."));
     *         popupMenu.add(new JMenuItem("..."));
     *     }
     * }
     * </pre></code>
     * If the menu is never used, the two add methods will never be called thus improve the performance.
     *
     * @param customizer the popup menu customizer
     */
    public void setPopupMenuCustomizer(PopupMenuCustomizer customizer) {
        _customizer = customizer;
    }

    @Override
    protected Point getPopupMenuOrigin() {
        if (_originCalculator != null) {
            return _originCalculator.getPopupMenuOrigin(this);
        }
        int x;
        int y;
        JPopupMenu pm = getPopupMenu();

        // Figure out the sizes needed to calculate the menu position
        Dimension s = getSize();
        Dimension pmSize = pm.getPreferredSize();

        Point position = getLocationOnScreen();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle screenBounds = new Rectangle(toolkit.getScreenSize());
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        for (GraphicsDevice gd : gds) {
            if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                GraphicsConfiguration dgc =
                    gd.getDefaultConfiguration();
                if (dgc.getBounds().contains(position)) {
                    gc = dgc;
                    break;
                }
            }
        }


        if (gc != null) {
            screenBounds = gc.getBounds();
            // take screen insets (e.g. task bar) into account
            Insets screenInsets = toolkit.getScreenInsets(gc);

            screenBounds.width -=
                Math.abs(screenInsets.left + screenInsets.right);
            screenBounds.height -=
                Math.abs(screenInsets.top + screenInsets.bottom);
            position.x -= Math.abs(screenInsets.left);
            position.y -= Math.abs(screenInsets.top);
        }

        Container parent = getParent();
        if (parent instanceof JPopupMenu) {
            // We are a sub-menu (pull-right)
            int xOffset = UIDefaultsLookup.getInt("Menu.submenuPopupOffsetX");
            int yOffset = UIDefaultsLookup.getInt("Menu.submenuPopupOffsetY");

            if (this.getComponentOrientation().isLeftToRight()) {
                if (JideSwingUtilities.getOrientationOf(this) == HORIZONTAL) {
                    // First determine x:
                    x = s.width + xOffset;   // Prefer placement to the right
                    if (position.x + x + pmSize.width >= screenBounds.width
                        + screenBounds.x &&
                        // popup doesn't fit - place it wherever there's more room
                        screenBounds.width - s.width < 2 * (position.x
                            - screenBounds.x)) {

                        x = 0 - xOffset - pmSize.width;
                    }
                }
                else {
                    // First determine x:
                    x = s.width + xOffset;   // Prefer placement to the right
                    if (position.x + x + pmSize.width >= screenBounds.width + screenBounds.x &&
                        // popup doesn't fit - place it wherever there's more room
                        screenBounds.width - s.width < 2 * (position.x - screenBounds.x)) {

                        x = 0 - xOffset - pmSize.width;
                    }
                }
            }
            else {
                // First determine x:
                x = 0 - xOffset - pmSize.width; // Prefer placement to the left
                if (position.x + x < screenBounds.x &&
                    // popup doesn't fit - place it wherever there's more room
                    screenBounds.width - s.width > 2 * (position.x -
                        screenBounds.x)) {

                    x = s.width + xOffset;
                }
            }
            // Then the y:
            y = yOffset;                     // Prefer dropping down
            if (position.y + y + pmSize.height >= screenBounds.height + screenBounds.y &&
                // popup doesn't fit - place it wherever there's more room
                screenBounds.height - s.height < 2 * (position.y - screenBounds.y)) {

                y = s.height - yOffset - pmSize.height;
            }
        }
        else {
            // We are a top level menu (pull-down)
            int xOffset = UIDefaultsLookup.getInt("Menu.menuPopupOffsetX");
            int yOffset = UIDefaultsLookup.getInt("Menu.menuPopupOffsetY");

            if (this.getComponentOrientation().isLeftToRight()) {
                if (JideSwingUtilities.getOrientationOf(this) == HORIZONTAL) {
                    // First determine the x:
                    if (getPreferredPopupHorizontalAlignment() == LEFT) {
                        x = xOffset;                   // Extend to the right
                        if (position.x + x + pmSize.width >= screenBounds.width
                            + screenBounds.x &&
                            // popup doesn't fit - place it wherever there's more room
                            screenBounds.width - s.width < 2 * (position.x
                                - screenBounds.x)) {

                            x = s.width - xOffset - pmSize.width;
                        }
                    }
                    else {
                        x = -pmSize.width + xOffset + s.width;                   // align right
                        if (position.x + x < screenBounds.x) {
                            x = screenBounds.x - position.x;
                        }
                    }
                }
                else {
                    // First determine the x:
                    x = 1 - xOffset - pmSize.width; // Extend to the left
                    if (position.x + x < screenBounds.x &&
                        // popup doesn't fit - place it wherever there's more room
                        screenBounds.width - s.width > 2 * (position.x
                            - screenBounds.x)) {

                        x = s.width + xOffset - 1;
                    }
                }
            }
            else {
                // First determine the x:
                if (getPreferredPopupHorizontalAlignment() == LEFT) {
                    x = s.width - xOffset - pmSize.width; // Extend to the left
                    if (position.x + x < screenBounds.x &&
                        // popup doesn't fit - place it wherever there's more room
                        screenBounds.width - s.width > 2 * (position.x
                            - screenBounds.x)) {

                        x = xOffset;
                    }
                }
                else {
                    x = xOffset;
                }
            }

            // Then the y:
            if (JideSwingUtilities.getOrientationOf(this) == HORIZONTAL) {
                y = s.height + yOffset - 1;    // Prefer dropping down
                if (getPreferredPopupVerticalAlignment() == TOP || // If forced to be on TOP
                    (position.y + y + pmSize.height >= screenBounds.height &&
                        // ...or popup doesn't fit - place it wherever there's more room
                        screenBounds.height - s.height < 2 * (position.y
                            - screenBounds.y))) {

                    y = 1 - yOffset - pmSize.height;   // Otherwise drop 'up'
                }
            }
            else {
                y = -yOffset;    // Prefer dropping up
                if (position.y + y + pmSize.height >= screenBounds.height &&
                    // popup doesn't fit - place it wherever there's more room
                    screenBounds.height - s.height < 2 * (position.y
                        - screenBounds.y)) {

                    y = 0 - yOffset - pmSize.height;   // Otherwise drop 'up'
                }
            }
        }

        return new Point(x, y);
    }

    /**
     * Checks if the
     *
     * @return false if it's top level menu. Otherwise, it will return what super.isOpaque().
     */
    @Override
    public boolean isOpaque() {
        return SystemInfo.isMacOSX() && isSelected() ? super.isOpaque() : !isTopLevelMenu() && super.isOpaque();
    }

    public boolean originalIsOpaque() {
        return super.isOpaque();
    }

    protected void hideMenu() {
        MenuSelectionManager msm = MenuSelectionManager.defaultManager();
        msm.clearSelectedPath();
    }

    public int getPreferredPopupHorizontalAlignment() {
        return _preferredPopupHorizontalAlignment;
    }

    public void setPreferredPopupHorizontalAlignment(int preferredPopupHorizontalAlignment) {
        _preferredPopupHorizontalAlignment = preferredPopupHorizontalAlignment;
    }

    public int getPreferredPopupVerticalAlignment() {
        return _preferredPopupVerticalAlignment;
    }

    public void setPreferredPopupVerticalAlignment(int preferredPopupVerticalAlignment) {
        _preferredPopupVerticalAlignment = preferredPopupVerticalAlignment;
    }

    @Override
    public boolean supportVerticalOrientation() {
        return true;
    }

    @Override
    public boolean supportHorizontalOrientation() {
        return true;
    }

    @Override
    public void setOrientation(int orientation) {
        int old = _orientation;
        if (old != orientation) {
            _orientation = orientation;
            firePropertyChange(PROPERTY_ORIENTATION, old, orientation);
        }
    }

    @Override
    public int getOrientation() {
        return _orientation;
    }

    private static JideMenu _pendingMenu;

    private static HideTimer _timer;

    // use this flag to disable the hide timer as there are quite a few bugs on it that we don't know how to solve.
    private static final boolean DISABLE_TIMER = true;

    @Override
    public void setPopupMenuVisible(boolean b) {
        MenuCreator menuCreator;
        if (b && (menuCreator = getMenuCreator()) != null) {
            menuCreator.createMenu();
        }

        PopupMenuCustomizer customizer;
        if (b && (customizer = getPopupMenuCustomizer()) != null) {
            customizer.customize(getPopupMenu());
            if (shouldHidePopupMenu()) {
                return;
            }
        }
        else if (b && shouldHidePopupMenu()) {
            return;
        }


        if (!DISABLE_TIMER) {
            if (isTopLevelMenu()) {
                setPopupMenuVisibleImmediately(b);
            }
            else {
                if (b) {
//                    System.out.println("show new menu");
                    stopTimer();
                    setPopupMenuVisibleImmediately(b);
                }
                else {
                    // HACK: check if the calling stack has clearSelectedPath method.
                    StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
                    for (StackTraceElement stackTraceElement : stackTraceElements) {
                        if (stackTraceElement.getMethodName().equals("clearSelectedPath")) {
                            setPopupMenuVisibleImmediately(b);
                            return;
                        }
                    }
                    startTimer();
                }
            }
        }
        else {
            setPopupMenuVisibleImmediately(b);
        }
    }

    /**
     * Check if the popup menu should stay hidden although {@link #setPopupMenuVisible(boolean)} is invoked.
     * <p/>
     * The default implementation is to check if it contains any menu items. You could override this method to change the
     * default behavior.
     *
     * @return true if the popup menu should stay invisible. Otherwise false.
     */
    protected boolean shouldHidePopupMenu() {
        return getPopupMenu().getComponentCount() == 0;
    }

    void setPopupMenuVisibleImmediately(boolean b) {
        super.setPopupMenuVisible(b);
    }

    private class HideTimer extends Timer implements ActionListener {
        private static final long serialVersionUID = 561631364532967870L;

        public HideTimer() {
            super(DELAY + 300, null);
            addActionListener(this);
            setRepeats(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            stopTimer();
        }
    }

    private void startTimer() {
//        System.out.println("timer started");
        if (_timer != null) {
            stopTimer();
        }
        _pendingMenu = this;
        _timer = new HideTimer();
        _timer.start();
    }

    private void stopTimer() {
        if (_timer != null) {
//            System.out.println("timer stopped");
            if (_pendingMenu != null) {
//                System.out.println("hidding pending menu");
                _pendingMenu.setPopupMenuVisibleImmediately(false);
                _pendingMenu = null;
            }
            _timer.stop();
            _timer = null;
        }
    }

}