/*
 * @(#)VsnetMenuItemUI.java
 *
 * Copyright 2002 JIDE Software Inc. All rights reserved.
 */

package com.jidesoft.plaf.vsnet;

import com.jidesoft.icons.IconsFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.basic.ThemePainter;
import com.jidesoft.plaf.windows.WindowsGraphicsUtilsPort;
import com.jidesoft.swing.ButtonStyle;
import com.jidesoft.swing.JideSplitButton;
import com.jidesoft.swing.JideSwingUtilities;
import com.jidesoft.swing.TopLevelMenuContainer;
import com.jidesoft.utils.SystemInfo;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * MenuItem UI implementation
 */
public class VsnetMenuItemUI extends MenuItemUI {
    protected JMenuItem menuItem = null;
    protected Color selectionBackground;
    protected Color selectionForeground;
    protected Color disabledForeground;
    protected Color acceleratorForeground;
    protected Color acceleratorSelectionForeground;
    private String acceleratorDelimiter;

    protected int defaultTextIconGap;
    protected Font acceleratorFont;

    protected MouseInputListener mouseInputListener;
    protected MenuDragMouseListener menuDragMouseListener;
    protected MenuKeyListener menuKeyListener;
    private PropertyChangeListener propertyChangeListener;

    protected Icon arrowIcon = null;
    protected Icon checkIcon = null;

    protected boolean oldBorderPainted;

    /**
     * Used for accelerator binding, lazily created.
     */
    protected InputMap windowInputMap;

    /* diagnostic aids -- should be false for production builds. */
    private static final boolean DEBUG = false;  // show bad params, misc.

    // added for VsnetMenuItemUI
    protected Color shadowColor;
    protected int defaultAccelEndGap;
    protected int defaultShadowWidth;
    protected Color borderColor;
    protected Color backgroundColor;
    // end added for VsnetMenuItemUI

    /* Client Property keys for text and accelerator text widths */
    static final String MAX_TEXT_WIDTH = "maxTextWidth";
    static final String MAX_ACC_WIDTH = "maxAccWidth";

    protected boolean _isFloatingIcon = false;

    private ThemePainter _painter = (ThemePainter) UIDefaultsLookup.get("Theme.painter");

    public static ComponentUI createUI(JComponent c) {
        return new VsnetMenuItemUI();
    }

    @Override
    public void installUI(JComponent c) {
        menuItem = (JMenuItem) c;

        installDefaults();
        installComponents(menuItem);
        installListeners();
        installKeyboardActions();
    }


    protected void installDefaults() {
        String prefix = getPropertyPrefix();

        acceleratorFont = UIDefaultsLookup.getFont("MenuItem.acceleratorFont");
        if (acceleratorFont == null) {
            acceleratorFont = UIManager.getFont("MenuItem.font");
        }

        menuItem.setOpaque(true);
        if (menuItem.getMargin() == null ||
            (menuItem.getMargin() instanceof UIResource)) {
            menuItem.setMargin(UIDefaultsLookup.getInsets(prefix + ".margin"));
        }

        // added for VsnetMenuItemUI
        defaultTextIconGap = UIDefaultsLookup.getInt(prefix + ".textIconGap");
        defaultAccelEndGap = UIDefaultsLookup.getInt("MenuItem.accelEndGap");
        defaultShadowWidth = UIDefaultsLookup.getInt("MenuItem.shadowWidth");
        // end add

        borderColor = UIDefaultsLookup.getColor("MenuItem.selectionBorderColor");
        backgroundColor = UIDefaultsLookup.getColor("MenuItem.background");
        shadowColor = UIDefaultsLookup.getColor("MenuItem.shadowColor");

        LookAndFeel.installBorder(menuItem, prefix + ".border");
        oldBorderPainted = menuItem.isBorderPainted();
        Object borderPainted = UIDefaultsLookup.get(prefix + ".borderPainted");
        if (borderPainted instanceof Boolean) {
            menuItem.setBorderPainted((Boolean) borderPainted);
        }
        LookAndFeel.installColorsAndFont(menuItem,
            prefix + ".background",
            prefix + ".foreground",
            prefix + ".font");

        // MenuItem specific defaults
        if (selectionBackground == null ||
            selectionBackground instanceof UIResource) {
            selectionBackground =
                UIDefaultsLookup.getColor(prefix + ".selectionBackground");
        }
        if (selectionForeground == null ||
            selectionForeground instanceof UIResource) {
            selectionForeground =
                UIDefaultsLookup.getColor(prefix + ".selectionForeground");
        }
        if (disabledForeground == null ||
            disabledForeground instanceof UIResource) {
            disabledForeground =
                UIDefaultsLookup.getColor(prefix + ".disabledForeground");
        }
        if (acceleratorForeground == null ||
            acceleratorForeground instanceof UIResource) {
            acceleratorForeground =
                UIDefaultsLookup.getColor(prefix + ".acceleratorForeground");
        }
        if (acceleratorSelectionForeground == null ||
            acceleratorSelectionForeground instanceof UIResource) {
            acceleratorSelectionForeground =
                UIDefaultsLookup.getColor(prefix + ".acceleratorSelectionForeground");
        }
        // Get accelerator delimiter
        acceleratorDelimiter =
            UIDefaultsLookup.getString("MenuItem.acceleratorDelimiter");
        if (acceleratorDelimiter == null) {
            acceleratorDelimiter = "+";
        }
        // Icons
        if (arrowIcon == null ||
            arrowIcon instanceof UIResource) {
            arrowIcon = UIDefaultsLookup.getIcon(prefix + ".arrowIcon");
        }
        if (checkIcon == null ||
            checkIcon instanceof UIResource) {
            checkIcon = UIDefaultsLookup.getIcon(prefix + ".checkIcon");
        }

        _isFloatingIcon = UIDefaultsLookup.getBoolean("Icon.floating");
    }

    /**
     * @param menuItem the menu item
     * @since 1.3
     */
    protected void installComponents(JMenuItem menuItem) {
        BasicHTML.updateRenderer(menuItem, menuItem.getText());
    }

    protected String getPropertyPrefix() {
        return "MenuItem";
    }

    protected void installListeners() {
        if ((mouseInputListener = createMouseInputListener(menuItem)) != null) {
            menuItem.addMouseListener(mouseInputListener);
            menuItem.addMouseMotionListener(mouseInputListener);
        }
        if ((menuDragMouseListener = createMenuDragMouseListener(menuItem)) != null) {
            menuItem.addMenuDragMouseListener(menuDragMouseListener);
        }
        if ((menuKeyListener = createMenuKeyListener(menuItem)) != null) {
            menuItem.addMenuKeyListener(menuKeyListener);
        }
        if ((propertyChangeListener = createPropertyChangeListener(menuItem)) != null) {
            menuItem.addPropertyChangeListener(propertyChangeListener);
        }
    }

    protected void installKeyboardActions() {
        ActionMap actionMap = getActionMap();

        SwingUtilities.replaceUIActionMap(menuItem, actionMap);
        updateAcceleratorBinding();
    }

    @Override
    public void uninstallUI(JComponent c) {
        menuItem = (JMenuItem) c;
        uninstallDefaults();
        uninstallComponents(menuItem);
        uninstallListeners();
        uninstallKeyboardActions();

        //Remove the textWidth and accWidth values from the parent's Client Properties.
        Container parent = menuItem.getParent();
        if ((parent != null && parent instanceof JComponent) &&
            !(menuItem instanceof JMenu && ((JMenu) menuItem).isTopLevelMenu())) {
            JComponent p = (JComponent) parent;
            p.putClientProperty(VsnetMenuItemUI.MAX_ACC_WIDTH, null);
            p.putClientProperty(VsnetMenuItemUI.MAX_TEXT_WIDTH, null);
        }

        menuItem = null;
    }


    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(menuItem);
        menuItem.setBorderPainted(oldBorderPainted);
        if (menuItem.getMargin() instanceof UIResource)
            menuItem.setMargin(null);
        if (arrowIcon instanceof UIResource)
            arrowIcon = null;
        if (checkIcon instanceof UIResource)
            checkIcon = null;
    }

    /**
     * @param menuItem the menu item
     * @since 1.3
     */
    protected void uninstallComponents(JMenuItem menuItem) {
        BasicHTML.updateRenderer(menuItem, "");
    }

    protected void uninstallListeners() {
        if (mouseInputListener != null) {
            menuItem.removeMouseListener(mouseInputListener);
            menuItem.removeMouseMotionListener(mouseInputListener);
        }
        if (menuDragMouseListener != null) {
            menuItem.removeMenuDragMouseListener(menuDragMouseListener);
        }
        if (menuKeyListener != null) {
            menuItem.removeMenuKeyListener(menuKeyListener);
        }
        if (propertyChangeListener != null) {
            menuItem.removePropertyChangeListener(propertyChangeListener);
        }

        mouseInputListener = null;
        menuDragMouseListener = null;
        menuKeyListener = null;
        propertyChangeListener = null;
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIActionMap(menuItem, null);
        if (windowInputMap != null) {
            SwingUtilities.replaceUIInputMap(menuItem, JComponent.
                WHEN_IN_FOCUSED_WINDOW, null);
            windowInputMap = null;
        }
    }

    protected MouseInputListener createMouseInputListener(JComponent c) {
        return new MouseInputHandler();
    }

    protected MenuDragMouseListener createMenuDragMouseListener(JComponent c) {
        return new MenuDragMouseHandler();
    }

    protected MenuKeyListener createMenuKeyListener(JComponent c) {
        return new MenuKeyHandler();
    }

    private PropertyChangeListener createPropertyChangeListener(JComponent c) {
        return new PropertyChangeHandler();
    }

    protected ActionMap getActionMap() {
        String propertyPrefix = getPropertyPrefix();
        String uiKey = propertyPrefix + ".actionMap";
        ActionMap am = (ActionMap) UIDefaultsLookup.get(uiKey);
        if (am == null) {
            am = createActionMap();
            UIManager.getLookAndFeelDefaults().put(uiKey, am);
        }
        return am;
    }

    protected ActionMap createActionMap() {
        ActionMap map = new ActionMapUIResource();
        map.put("doClick", new ClickAction());

// removed for VsnetMenuItem. it's protected method
        // Set the ActionMap's parent to the Auditory Feedback Action Map
//        BasicLookAndFeel lf = (BasicLookAndFeel) UIManager.getLookAndFeel();
//        ActionMap audioMap = lf.getAudioActionMap();
//        map.setParent(audioMap);

        return map;
    }

    protected InputMap createInputMap(int condition) {
        if (condition == JComponent.WHEN_IN_FOCUSED_WINDOW) {
            return new ComponentInputMapUIResource(menuItem);
        }
        return null;
    }

    void updateAcceleratorBinding() {
        KeyStroke accelerator = menuItem.getAccelerator();

        if (windowInputMap != null) {
            windowInputMap.clear();
        }
        if (accelerator != null) {
            if (windowInputMap == null) {
                windowInputMap = createInputMap(JComponent.
                    WHEN_IN_FOCUSED_WINDOW);
                SwingUtilities.replaceUIInputMap(menuItem,
                    JComponent.WHEN_IN_FOCUSED_WINDOW, windowInputMap);
            }
            windowInputMap.put(accelerator, "doClick");
        }
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        Dimension d = null;
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d = getPreferredSize(c);
            d.width -= v.getPreferredSpan(View.X_AXIS) - v.getMinimumSpan(View.X_AXIS);
        }
        return d;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return getPreferredMenuItemSize(c,
            checkIcon,
            arrowIcon,
            defaultTextIconGap);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        Dimension d = null;
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d = getPreferredSize(c);
            d.width += v.getMaximumSpan(View.X_AXIS) - v.getPreferredSpan(View.X_AXIS);
        }
        return d;
    }

    // these rects are used for painting and preferredsize calculations.
    // they used to be regenerated constantly.  Now they are reused.
    protected static Rectangle zeroRect = new Rectangle(0, 0, 0, 0);
    protected static Rectangle iconRect = new Rectangle();
    protected static Rectangle textRect = new Rectangle();
    protected static Rectangle acceleratorRect = new Rectangle();
    protected static Rectangle checkIconRect = new Rectangle();
    protected static Rectangle arrowIconRect = new Rectangle();
    protected static Rectangle viewRect = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);
    static Rectangle r = new Rectangle();

    private void resetRects() {
        iconRect.setBounds(zeroRect);
        textRect.setBounds(zeroRect);
        acceleratorRect.setBounds(zeroRect);
        checkIconRect.setBounds(zeroRect);
        arrowIconRect.setBounds(zeroRect);
        viewRect.setBounds(0, 0, Short.MAX_VALUE, Short.MAX_VALUE);
        r.setBounds(zeroRect);
    }

    protected Dimension getPreferredMenuItemSize(JComponent c,
                                                 Icon checkIcon,
                                                 Icon arrowIcon,
                                                 int textIconGap) {
        JMenuItem b = (JMenuItem) c;
        Icon icon = b.getIcon();
        String text = b.getText();
        KeyStroke accelerator = b.getAccelerator();
        String acceleratorText = "";

        if (accelerator != null) {
            int modifiers = accelerator.getModifiers();
            if (modifiers > 0) {
                acceleratorText = KeyEvent.getKeyModifiersText(modifiers);
                //acceleratorText += "-";
                acceleratorText += acceleratorDelimiter;
            }
            int keyCode = accelerator.getKeyCode();
            if (keyCode != 0) {
                acceleratorText += KeyEvent.getKeyText(keyCode);
            }
            else {
                acceleratorText += accelerator.getKeyChar();
            }
        }

        Font font = b.getFont();
        FontMetrics fm = b.getFontMetrics(font);
        FontMetrics fmAccel = b.getFontMetrics(acceleratorFont);

        resetRects();

        layoutMenuItem(fm, text, fmAccel, acceleratorText, icon, checkIcon, arrowIcon,
            b.getVerticalAlignment(), b.getHorizontalAlignment(),
            b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
            viewRect, iconRect, textRect, acceleratorRect, checkIconRect, arrowIconRect,
            text == null ? 0 : textIconGap,
            defaultTextIconGap);

        // find the union of the icon and text rects
        if (text != null && text.trim().length() != 0) {
            r.setBounds(textRect);
        }
        if (!iconRect.isEmpty()) {
            r = SwingUtilities.computeUnion(iconRect.x,
                iconRect.y,
                iconRect.width,
                iconRect.height,
                r);
        }

        // To make the accelerator texts appear in a column, find the widest MenuItem text
        // and the widest accelerator text.

        //Get the parent, which stores the information.
        Container parent = menuItem.getParent();

        //Check the parent, and see that it is not a top-level menu.
        if (parent != null && parent instanceof JComponent &&
            !(menuItem instanceof JMenu && ((JMenu) menuItem).isTopLevelMenu())) {
            JComponent p = (JComponent) parent;

            //Get widest text so far from parent, if no one exists null is returned.
            Integer maxTextWidth = (Integer) p.getClientProperty(VsnetMenuItemUI.MAX_TEXT_WIDTH);
            Integer maxAccWidth = (Integer) p.getClientProperty(VsnetMenuItemUI.MAX_ACC_WIDTH);

            int maxTextValue = maxTextWidth != null ? maxTextWidth : 0;
            int maxAccValue = maxAccWidth != null ? maxAccWidth : 0;

            //Compare the text widths, and adjust the r.width to the widest.
            if (r.width < maxTextValue) {
                r.width = maxTextValue;
            }
            else {
                p.putClientProperty(VsnetMenuItemUI.MAX_TEXT_WIDTH, r.width);
            }

            //Compare the accelerator widths.
            if (acceleratorRect.width > maxAccValue) {
                maxAccValue = acceleratorRect.width;
                p.putClientProperty(VsnetMenuItemUI.MAX_ACC_WIDTH, acceleratorRect.width);
            }

            r.width += maxAccValue;
            r.width += textIconGap;
            r.width += defaultAccelEndGap;
        }

        if (text != null && text.trim().length() != 0) {
            if (icon != null) {
                r.width += textIconGap;
            }
        }

        Insets insets = b.getInsets();

        if (useCheckAndArrow()) {
            insets.left = 0;
            insets.right = 0;
            r.width += 5;
        }

        if (isDownArrowVisible(parent)) {
            insets.left = 0;
            insets.right = 0;
            r.width += 7;
        }

        if (insets != null) {
            r.width += insets.left + insets.right;
            r.height += insets.top + insets.bottom;
        }

        // if the width is even, bump it up one. This is critical
        // for the focus dash line to draw properly
        if (r.width % 2 == 0) {
            r.width++;
        }

        // if the height is even, bump it up one. This is critical
        // for the text to center properly
//        if (r.height % 2 == 0) {
//            r.height++;
//        }

        if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL) {
            return r.getSize();
        }
        else {
            //noinspection SuspiciousNameCombination
            return new Dimension(r.height, r.width);
        }
    }

    /**
     * We draw the background in paintMenuItem() so override update (which fills the background of opaque components by
     * default) to just call paint().
     */
    @Override
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        paintMenuItem((Graphics2D) g, c, checkIcon, arrowIcon,
            selectionBackground, selectionForeground,
            defaultTextIconGap);
    }


    protected void paintMenuItem(Graphics2D g, JComponent c,
                                 Icon checkIcon, Icon arrowIcon,
                                 Color background, Color foreground,
                                 int defaultTextIconGap) {
        JMenuItem b = (JMenuItem) c;
        ButtonModel model = b.getModel();
        Object property = b.getClientProperty("MenuItem.shadowWidth");
        if (property instanceof Number) {
            defaultShadowWidth = ((Number) property).intValue();
            if (b instanceof JMenu) {
                for (Component comp : ((JMenu) b).getMenuComponents()) {
                    if (comp instanceof JMenuItem && ((JMenuItem) comp).getClientProperty("MenuItem.shadowWidth") == null) {
                        ((JMenuItem) comp).putClientProperty("MenuItem.shadowWidth", defaultShadowWidth);
                    }
                }
            }
        }

        int menuWidth;
        int menuHeight;

        if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL) {
            menuWidth = b.getWidth();
            menuHeight = b.getHeight();
        }
        else {
            //   Dimension size = b.getSize();
            menuWidth = b.getHeight();
            menuHeight = b.getWidth();
            Graphics2D g2d = (Graphics2D) g;
            g2d.rotate(Math.PI / 2);
            g2d.translate(0, -menuHeight + 1);
        }

        Insets i = c.getInsets();

        resetRects();

        viewRect.setBounds(0, 0, menuWidth, menuHeight);

        if (isDownArrowVisible(b.getParent())) {
            viewRect.x += 3;
            viewRect.width -= 7;
        }
        else {
            viewRect.x += i.left;
            viewRect.width -= (i.right + viewRect.x);
        }
        viewRect.y += i.top;
        viewRect.height -= (i.bottom + viewRect.y);


        Font holdf = g.getFont();
        Font f = c.getFont();
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics(f);
        FontMetrics fmAccel = g.getFontMetrics(acceleratorFont);

        // get Accelerator text
        KeyStroke accelerator = b.getAccelerator();
        String acceleratorText = "";
        if (accelerator != null) {
            int modifiers = accelerator.getModifiers();
            if (modifiers > 0) {
                acceleratorText = KeyEvent.getKeyModifiersText(modifiers);
                //acceleratorText += "-";
                acceleratorText += acceleratorDelimiter;
            }

            int keyCode = accelerator.getKeyCode();
            if (keyCode != 0) {
                acceleratorText += KeyEvent.getKeyText(keyCode);
            }
            else {
                acceleratorText += accelerator.getKeyChar();
            }
        }

        // layout the text and icon
        String text = layoutMenuItem(fm, b.getText(), fmAccel, acceleratorText, b.getIcon(),
            checkIcon, arrowIcon,
            b.getVerticalAlignment(), b.getHorizontalAlignment(),
            b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
            viewRect, iconRect, textRect, acceleratorRect,
            checkIconRect, arrowIconRect,
            b.getText() == null ? 0 : defaultTextIconGap,
            defaultTextIconGap);

        // Paint background
        paintBackground(g, b, background);

        Color holdc = g.getColor();

        // Paint the Check
        if ((c.getUIClassID().indexOf("CheckBoxMenu") >= 0 || c.getUIClassID().indexOf("RadioButtonMenu") >= 0) && checkIcon != null) {
            paintCheckBox(b, g, checkIcon);
            g.setColor(holdc);
        }

        // rotate back since we don't want to paint icon in rotated way.
        if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.VERTICAL) {
            //   Dimension size = b.getSize();
            Graphics2D g2d = (Graphics2D) g;
            g2d.rotate(-Math.PI / 2);
            g2d.translate(-menuHeight + 1, 0);
            int save = iconRect.x;
            //noinspection SuspiciousNameCombination
            iconRect.x = iconRect.y;
            iconRect.y = save;
        }

        paintIcon(b, g);

        if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.VERTICAL) {
            int save = iconRect.x;
            //noinspection SuspiciousNameCombination
            iconRect.x = iconRect.y;
            iconRect.y = save;
            //   Dimension size = b.getSize();
            Graphics2D g2d = (Graphics2D) g;
            g2d.rotate(Math.PI / 2);
            g2d.translate(0, -menuHeight + 1);
        }

        // Draw the Text
        if (text != null) {
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                v.paint(g, textRect);
            }
            else {
                paintText(g, b, textRect, text);
            }
        }

        // Draw the Accelerator Text
        if (acceleratorText != null && !acceleratorText.equals("")) {

            //Get the maxAccWidth from the parent to calculate the offset.
            int accOffset = 0;
            Container parent = menuItem.getParent();
            if (parent != null && parent instanceof JComponent) {
                JComponent p = (JComponent) parent;
                Integer maxValueInt = (Integer) p.getClientProperty(VsnetMenuItemUI.MAX_ACC_WIDTH);
                int maxValue = maxValueInt != null ?
                    maxValueInt : acceleratorRect.width;

                //Calculate the offset, with which the accelerator texts will be drawn with.
                accOffset = maxValue - acceleratorRect.width;
            }
            if (!parent.getComponentOrientation().isLeftToRight()) {
                // in right to left case, no need to offset the accelerator, always start from the same X
                accOffset = 0;
            }

            g.setFont(acceleratorFont);
            if (!model.isEnabled()) {
                // *** paint the acceleratorText disabled
                if (disabledForeground != null) {
                    g.setColor(disabledForeground);
                    BasicGraphicsUtils.drawString(menuItem, g, acceleratorText,
                        acceleratorRect.x - accOffset,
                        acceleratorRect.y + fmAccel.getAscent());
                }
                else {
                    g.setColor(b.getBackground().brighter());
                    BasicGraphicsUtils.drawString(menuItem, g, acceleratorText,
                        acceleratorRect.x - accOffset,
                        acceleratorRect.y + fmAccel.getAscent());
                    g.setColor(b.getBackground().darker());
                    BasicGraphicsUtils.drawString(menuItem, g, acceleratorText,
                        acceleratorRect.x - accOffset - 1,
                        acceleratorRect.y + fmAccel.getAscent() - 1);
                }
            }
            else {
                // *** paint the acceleratorText normally
                if (model.isArmed() || (c instanceof JMenu && model.isSelected())) {
                    g.setColor(acceleratorSelectionForeground);
                }
                else {
                    g.setColor(acceleratorForeground);
                }
                BasicGraphicsUtils.drawString(menuItem, g, acceleratorText,
                    acceleratorRect.x - accOffset,
                    acceleratorRect.y + fmAccel.getAscent());
            }
        }

        // Paint the Arrow
        if (arrowIcon != null) {
            if (model.isArmed() || (c instanceof JMenu && model.isSelected()))
                g.setColor(foreground);
            if (useCheckAndArrow()) {
                arrowIcon.paintIcon(c, g, arrowIconRect.x, arrowIconRect.y);
            }
        }
        g.setColor(holdc);
        g.setFont(holdf);
    }

    protected void paintCheckBox(JMenuItem b, Graphics g, Icon checkIcon) {
        ButtonModel model = b.getModel();
        boolean isSelected = false;
        if (b instanceof JCheckBoxMenuItem)
            isSelected = b.isSelected();
        else if (b instanceof JRadioButtonMenuItem)
            isSelected = b.isSelected();
        if (isSelected) {
            if (model.isArmed() || model.isSelected()) {
                getPainter().paintMenuItemBackground(b, g, checkIconRect, SwingConstants.HORIZONTAL, ThemePainter.STATE_PRESSED);
                if (b.getIcon() == null) {
                    checkIcon.paintIcon(b, g, checkIconRect.x, checkIconRect.y);
                }
            }
            else {
                getPainter().paintMenuItemBackground(b, g, checkIconRect, SwingConstants.HORIZONTAL, ThemePainter.STATE_SELECTED);
                if (b.getIcon() == null) {
                    checkIcon.paintIcon(b, g, checkIconRect.x, checkIconRect.y);
                }
            }
        }
    }

    protected void paintIcon(JMenuItem b, Graphics g) {
        if (b.getIcon() != null) {
            Icon icon = getIcon(b);

            ButtonModel model = b.getModel();
            if (icon != null) {
                if (isFloatingIcon()) {
                    if (model.isArmed() || (b instanceof JMenu && model.isSelected())) {
                        if (icon instanceof ImageIcon) {
                            ImageIcon shadow = IconsFactory.createGrayImage(((ImageIcon) icon).getImage());
                            shadow.paintIcon(b, g, iconRect.x + 1, iconRect.y + 1);
                        }
                        else {
                            ImageIcon shadow = IconsFactory.createGrayImage(b, icon);
                            shadow.paintIcon(b, g, iconRect.x + 1, iconRect.y + 1);
                        }
                        icon.paintIcon(b, g, iconRect.x - 1, iconRect.y - 1);
                    }
                    else {
                        icon.paintIcon(b, g, iconRect.x, iconRect.y);
                    }
                }
                else {
                    icon.paintIcon(b, g, iconRect.x, iconRect.y);
                }
            }
        }
    }

    /**
     * Draws the background of the menu item.
     *
     * @param g        the paint graphics
     * @param menuItem menu item to be painted
     * @param bgColor  selection background color
     * @since 1.4
     */
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
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

        if (menuItem.isOpaque()) {
            if (menuItem.getComponentOrientation().isLeftToRight()) {
                if (menuItem.getBackground() instanceof UIResource) {
                    g.setColor(getPainter().getMenuItemBackground());
                }
                else {
                    g.setColor(menuItem.getBackground());
                }
                g.fillRect(defaultShadowWidth, 0, menuWidth - defaultShadowWidth, menuHeight);
                getPainter().paintMenuShadow(menuItem, g, new Rectangle(0, 0, defaultShadowWidth, menuHeight), SwingConstants.HORIZONTAL, ThemePainter.STATE_DEFAULT);

                if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
                    getPainter().paintMenuItemBackground(menuItem, g, new Rectangle(1, 0, menuWidth - 2, menuHeight), SwingConstants.HORIZONTAL, ThemePainter.STATE_ROLLOVER);
                }


                g.setColor(oldColor);
            }
            else {
                if (menuItem.getBackground() instanceof UIResource) {
                    g.setColor(getPainter().getMenuItemBackground());
                }
                else {
                    g.setColor(menuItem.getBackground());
                }
                g.fillRect(0, 0, menuWidth - defaultShadowWidth, menuHeight);
                getPainter().paintMenuShadow(menuItem, g, new Rectangle(menuWidth - defaultShadowWidth, 0, defaultShadowWidth, menuHeight), SwingConstants.HORIZONTAL, ThemePainter.STATE_DEFAULT);

                if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
                    getPainter().paintMenuItemBackground(menuItem, g, new Rectangle(0, 0, menuWidth, menuHeight), SwingConstants.HORIZONTAL, ThemePainter.STATE_ROLLOVER);
                }

                g.setColor(oldColor);
            }
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
     */
    protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
        // Note: This method is almost identical to the same method in WindowsMenuUI
        ButtonModel model = menuItem.getModel();

        FontMetrics fm = g.getFontMetrics();

        if (!model.isEnabled()) {
            // *** paint the text disabled
            WindowsGraphicsUtilsPort.paintText((Graphics2D) g, menuItem, textRect, text, 0);
        }
        else {
            int mnemonicIndex = menuItem.getDisplayedMnemonicIndex();
            // W2K Feature: Check to see if the Underscore should be rendered.
            if (SystemInfo.isMnemonicHidden()) {
                mnemonicIndex = -1;
            }

            Color oldColor = g.getColor();

            // *** paint the text normally
            if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
                g.setColor(selectionForeground); // Uses protected field.
            }
            BasicGraphicsUtils.drawStringUnderlineCharAt(menuItem, (Graphics2D) g, text,
                mnemonicIndex,
                textRect.x,
                textRect.y + fm.getAscent());
            g.setColor(oldColor);
        }
    }

    private String layoutMenuItem(FontMetrics fm,
                                  String text,
                                  FontMetrics fmAccel,
                                  String acceleratorText,
                                  Icon icon,
                                  Icon checkIcon,
                                  Icon arrowIcon,
                                  int verticalAlignment,
                                  int horizontalAlignment,
                                  int verticalTextPosition,
                                  int horizontalTextPosition,
                                  Rectangle viewRect,
                                  Rectangle iconRect,
                                  Rectangle textRect,
                                  Rectangle acceleratorRect,
                                  Rectangle checkIconRect,
                                  Rectangle arrowIconRect,
                                  int textIconGap,
                                  int menuItemGap) {
        if (icon != null)
            if ((icon.getIconHeight() == 0) || (icon.getIconWidth() == 0))
                icon = null;    // An exception may occur in case of zero sized icons

        viewRect.width -= getRightMargin(); // this line is mainly for JideSplitButton
        String newText = SwingUtilities.layoutCompoundLabel(menuItem, fm, text, icon, verticalAlignment,
            horizontalAlignment, verticalTextPosition,
            horizontalTextPosition, viewRect, iconRect, textRect,
            textIconGap);
        // only do it for split menu
        if (getRightMargin() > 0) {
            text = newText;
        }
        viewRect.width += getRightMargin(); // this line is mainly for JideSplitButton

        Insets insets = isDownArrowVisible(menuItem.getParent()) ? new Insets(0, 0, 0, 0) : menuItem.getInsets();
        // get viewRect which is the bounds of menuitem
        viewRect.x = insets.left;
        viewRect.y = insets.top;
        if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL) {
            //   Dimension size = b.getSize();
            viewRect.height = menuItem.getHeight() - insets.top - insets.bottom;
            viewRect.width = menuItem.getWidth() - insets.left - insets.right;
        }
        else {
            //   Dimension size = b.getSize();
            viewRect.height = menuItem.getWidth() - insets.top - insets.bottom;
            viewRect.width = menuItem.getHeight() - insets.left - insets.right;
        }

        /* Initialize the acceelratorText bounds rectangle textRect.  If a null
         * or and empty String was specified we substitute "" here
         * and use 0,0,0,0 for acceleratorTextRect.
         */
        if ((acceleratorText == null) || acceleratorText.equals("")) {
            acceleratorRect.width = acceleratorRect.height = 0;
        }
        else {
            acceleratorRect.width = SwingUtilities.computeStringWidth(fmAccel, acceleratorText);
            acceleratorRect.height = fmAccel.getHeight();
        }

        if ((text == null) || text.equals("")) {
            textRect.width = textRect.height = 0;
            text = "";
        }
        else {
            View v;
            v = (menuItem != null) ? (View) menuItem.getClientProperty("html") : null;
            if (v != null) {
                textRect.width = (int) v.getPreferredSpan(View.X_AXIS);
                textRect.height = (int) v.getPreferredSpan(View.Y_AXIS);
            }
            else {
                textRect.width = SwingUtilities.computeStringWidth(fm, text);
                textRect.height = fm.getHeight();
            }
        }

        if (icon == null) {
            if (useCheckAndArrow())
                iconRect.width = iconRect.height = 16;
            else
                iconRect.width = iconRect.height = 0;
        }
        else {
            iconRect.width = icon.getIconWidth();
            iconRect.height = icon.getIconHeight();
        }

        if (arrowIcon == null) {
            arrowIconRect.width = arrowIconRect.height = 0;
        }
        else {
            try {
                arrowIconRect.width = arrowIcon.getIconWidth();
                arrowIconRect.height = arrowIcon.getIconHeight();
            }
            catch (Exception e) {
                arrowIconRect.width = arrowIconRect.height = 0;
            }
        }

        if (checkIcon == null) {
            checkIconRect.width = checkIconRect.height = 0;
        }
        else {
            try {
                checkIconRect.width = icon != null ? icon.getIconWidth() : checkIcon.getIconWidth();
                checkIconRect.height = icon != null ? icon.getIconHeight() : checkIcon.getIconHeight();
            }
            catch (Exception e) {
                checkIconRect.width = checkIconRect.height = 0;
            }
        }

        // left a shadow for non-top level menu
        if (useCheckAndArrow()) {
            iconRect.x = (defaultShadowWidth - iconRect.width) >> 1;
            if (text != null && !text.equals("")) {
                textRect.x = viewRect.x + defaultShadowWidth + textIconGap;
            }
        }
        else {
//                if (icon != null) {
//                    if (isDownArrowVisible(menuItem.getParent())) {
//                        iconRect.x = 3;
//                    }
//                    else {
//                        iconRect.x = menuItem.getInsets().left;
//                    }
//                    if (text != null && !text.equals("")) {
//                        textRect.x = iconRect.x + iconRect.width + textIconGap;
//                    }
//                }
//                else {
//                    if (text != null && !text.equals("")) {
//                        if (isDownArrowVisible(menuItem.getParent())) {
//                            textRect.x = 3;
//                        }
//                        else {
//                            textRect.x = menuItem.getInsets().left;
//                        }
//                    }
//                }
        }

        // Position the Accelerator text rect
        acceleratorRect.x = viewRect.x + viewRect.width - menuItemGap
            - acceleratorRect.width;

        // Position the Check and Arrow Icons
        if (useCheckAndArrow()) {
            checkIconRect.x = viewRect.x + (defaultShadowWidth - checkIconRect.width) >> 1;
            arrowIconRect.x = viewRect.x + viewRect.width - menuItemGap - 5
                - arrowIconRect.width;
        }

        if (verticalTextPosition == SwingConstants.CENTER && verticalAlignment == SwingConstants.CENTER) {
            // put it in the middle
            if (text != null && !text.equals("")) {
                textRect.y = viewRect.y + ((viewRect.height - textRect.height) >> 1);
            }
            iconRect.y = viewRect.y + ((viewRect.height - iconRect.height) >> 1);
        }

        Rectangle labelRect = iconRect.union(textRect);

        // Align the accelerator text and the check and arrow icons vertically
        // with the center of the label rect.
        acceleratorRect.y = labelRect.y + (labelRect.height >> 1) - (acceleratorRect.height >> 1);

        if (useCheckAndArrow()) {
            arrowIconRect.y = viewRect.y + ((viewRect.height - arrowIconRect.height) >> 1);//
            checkIconRect.y = viewRect.y + ((viewRect.height - checkIconRect.height) >> 1);//labelRect.y + (labelRect.height / 2) - (checkIconRect.height / 2);
        }

        if ((useCheckAndArrow() || menuItem instanceof JideSplitButton) && !menuItem.getComponentOrientation().isLeftToRight()) {
            checkIconRect.x = viewRect.width - checkIconRect.width - checkIconRect.x;
            iconRect.x = viewRect.width - iconRect.width - iconRect.x;
            textRect.x = viewRect.width - textRect.width - textRect.x;
            acceleratorRect.x = viewRect.width - acceleratorRect.width - acceleratorRect.x;
            arrowIconRect.x = viewRect.width - arrowIconRect.width - arrowIconRect.x;
        }

//        System.out.println("Layout: text=" + menuItem.getText() + "\n\tv="
//                + viewRect + "\n\tc=" + checkIconRect + "\n\ti="
//                + iconRect + "\n\tt=" + textRect + "\n\tacc="
//                + acceleratorRect + "\n\ta=" + arrowIconRect + "\n");

        return text;
    }

    /*
     * Returns false if the component is a JMenu and it is a top
     * level menu (on the menubar).
     */
    protected boolean useCheckAndArrow() {
        boolean b = true;
        if ((menuItem instanceof JMenu) &&
            (((JMenu) menuItem).isTopLevelMenu())) {
            b = false;
        }
        return b;
    }

    public MenuElement[] getPath() {
        MenuSelectionManager m = MenuSelectionManager.defaultManager();
        MenuElement oldPath[] = m.getSelectedPath();
        MenuElement newPath[];
        int i = oldPath.length;
        if (i == 0)
            return new MenuElement[0];
        Component parent = menuItem.getParent();
        if (oldPath[i - 1].getComponent() == parent) {
            // The parent popup menu is the last so far
            newPath = new MenuElement[i + 1];
            System.arraycopy(oldPath, 0, newPath, 0, i);
            newPath[i] = menuItem;
        }
        else {
            // A sibling menuitem is the current selection
            //
            //  This probably needs to handle 'exit submenu into
            // a menu item.  Search backwards along the current
            // selection until you find the parent popup menu,
            // then copy up to that and add yourself...
            int j;
            for (j = oldPath.length - 1; j >= 0; j--) {
                if (oldPath[j].getComponent() == parent)
                    break;
            }
            newPath = new MenuElement[j + 2];
            System.arraycopy(oldPath, 0, newPath, 0, j + 1);
            newPath[j + 1] = menuItem;
            /*
            System.out.println("Sibling condition -- ");
            System.out.println("Old array : ");
            printMenuElementArray(oldPath, false);
            System.out.println("New array : ");
            printMenuElementArray(newPath, false);
            */
        }
        return newPath;
    }

/*
    void printMenuElementArray(MenuElement path[], boolean dumpStack) {
        System.out.println("Path is(");
        int i, j;
        for (i = 0, j = path.length; i < j; i++) {
            for (int k = 0; k <= i; k++)
                System.out.print("  ");
            MenuElement me = (MenuElement) path[i];
            if (me instanceof JMenuItem)
                System.out.println(((JMenuItem) me).getText() + ", ");
            else if (me == null)
                System.out.println("NULL , ");
            else
                System.out.println("" + me + ", ");
        }
        System.out.println(")");

        if (dumpStack == true)
            Thread.dumpStack();
    }

*/

    protected class MouseInputHandler implements MouseInputListener {
        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            if (menuItem != null && menuItem.isEnabled()) {
                MenuSelectionManager manager = MenuSelectionManager.defaultManager();
                Point p = e.getPoint();
                if (p.x >= 0 && p.x < menuItem.getWidth() &&
                    p.y >= 0 && p.y < menuItem.getHeight()) {
                    doClick(manager);
                }
                else {
                    manager.processMouseEvent(e);
                }
            }
        }

        public void mouseEntered(MouseEvent e) {
            if (menuItem != null && menuItem.isEnabled()) {
                MenuSelectionManager manager = MenuSelectionManager.defaultManager();
                int modifiers = e.getModifiers();
                // 4188027: drag enter/exit added in JDK 1.1.7A, JDK1.2
                if ((modifiers & (InputEvent.BUTTON1_MASK |
                    InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK)) != 0) {
                    MenuSelectionManager.defaultManager().processMouseEvent(e);
                }
                else {
                    manager.setSelectedPath(getPath());
                }
            }
        }

        public void mouseExited(MouseEvent e) {
            if (menuItem != null && menuItem.isEnabled()) {
                MenuSelectionManager manager = MenuSelectionManager.defaultManager();

                int modifiers = e.getModifiers();
                // 4188027: drag enter/exit added in JDK 1.1.7A, JDK1.2
                if ((modifiers & (InputEvent.BUTTON1_MASK |
                    InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK)) != 0) {
                    MenuSelectionManager.defaultManager().processMouseEvent(e);
                }
                else {

                    MenuElement path[] = manager.getSelectedPath();
                    if (path.length > 1) {
                        MenuElement newPath[] = new MenuElement[path.length - 1];
                        int i, c;
                        for (i = 0, c = path.length - 1; i < c; i++)
                            newPath[i] = path[i];
                        manager.setSelectedPath(newPath);
                    }
                }
            }
        }

        public void mouseDragged(MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseMoved(MouseEvent e) {
            if (menuItem != null && menuItem.isEnabled()) {
                MenuSelectionManager manager = MenuSelectionManager.defaultManager();
                int modifiers = e.getModifiers();
                // 4188027: drag enter/exit added in JDK 1.1.7A, JDK1.2
                if ((modifiers & (InputEvent.BUTTON1_MASK |
                    InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK)) == 0) {
                    MenuElement[] path = manager.getSelectedPath();
                    if (getPath().length > path.length) { // MOUSE ENTER event is missed because of overlap menu items, for example the JideSplitButton
                        manager.setSelectedPath(getPath());
                    }
                }
            }
        }
    }


    private class MenuDragMouseHandler implements MenuDragMouseListener {
        public void menuDragMouseEntered(MenuDragMouseEvent e) {
        }

        public void menuDragMouseDragged(MenuDragMouseEvent e) {
            if (menuItem != null && menuItem.isEnabled()) {
                MenuSelectionManager manager = e.getMenuSelectionManager();
                MenuElement path[] = e.getPath();
                manager.setSelectedPath(path);
            }
        }

        public void menuDragMouseExited(MenuDragMouseEvent e) {
        }

        public void menuDragMouseReleased(MenuDragMouseEvent e) {
            if (menuItem != null && menuItem.isEnabled()) {
                MenuSelectionManager manager = e.getMenuSelectionManager();
                Point p = e.getPoint();
                if (p.x >= 0 && p.x < menuItem.getWidth() &&
                    p.y >= 0 && p.y < menuItem.getHeight()) {
                    doClick(manager);
                }
                else {
                    manager.clearSelectedPath();
                }
            }
        }
    }

    private class MenuKeyHandler implements MenuKeyListener {

        /**
         * Handles the mnemonic key typed in the MenuItem if this menuItem is in a standalone popup menu. This
         * invocation normally handled in BasicMenuUI.MenuKeyHandler.menuKeyPressed. Ideally, the MenuKeyHandlers for
         * both BasicMenuItemUI and BasicMenuUI can be consolidated into BasicPopupMenuUI but that would require an
         * semantic change. This would result in a performance win since we can shortcut a lot of the needless
         * processing from MenuSelectionManager.processKeyEvent(). See 4670831.
         */
        public void menuKeyTyped(MenuKeyEvent e) {
            if (menuItem != null && menuItem.isEnabled()) {
                int key = menuItem.getMnemonic();
                if (key == 0 || e.getPath().length != 2) // Hack! Only proceed if in a JPopupMenu
                    return;
                if (lower((char) key) == lower(e.getKeyChar())) {
                    MenuSelectionManager manager =
                        e.getMenuSelectionManager();
                    doClick(manager);
                    e.consume();
                }
            }
        }

        public void menuKeyPressed(MenuKeyEvent e) {
        }

        public void menuKeyReleased(MenuKeyEvent e) {
        }

        private char lower(char keyChar) {
            return Character.toLowerCase(keyChar);
        }
    }

    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();

            if (name.equals("labelFor") || name.equals("displayedMnemonic") ||
                name.equals("accelerator")) {
                updateAcceleratorBinding();
            }
            else if (ButtonStyle.BUTTON_STYLE_PROPERTY.equals(name)
                || "opaque".equals(name)
                || AbstractButton.CONTENT_AREA_FILLED_CHANGED_PROPERTY.equals(name)
            ) {
                JMenuItem lbl = ((JMenuItem) e.getSource());
                lbl.repaint();
            }
            else if (name.equals("orientation")) {
                JMenuItem lbl = ((JMenuItem) e.getSource());
                lbl.invalidate();
                lbl.repaint();
            }
            else if (name.equals("text") || "font".equals(name) ||
                "foreground".equals(name)) {
                // remove the old html view client property if one
                // existed, and install a new one if the text installed
                // into the JLabel is html source.
                JMenuItem lbl = ((JMenuItem) e.getSource());
                String text = lbl.getText();
                BasicHTML.updateRenderer(lbl, text);
            }
        }
    }

    private static class ClickAction extends AbstractAction {
        private static final long serialVersionUID = -8707660318949717143L;

        public void actionPerformed(ActionEvent e) {
            JMenuItem mi = (JMenuItem) e.getSource();
            MenuSelectionManager.defaultManager().clearSelectedPath();
            mi.doClick();
        }
    }

    /**
     * Call this method when a menu item is to be activated. This method handles some of the details of menu item
     * activation such as clearing the selected path and messaging the JMenuItem's doClick() method.
     *
     * @param msm A MenuSelectionManager. The visual feedback and internal bookkeeping tasks are delegated to this
     *            MenuSelectionManager. If <code>null</code> is passed as this argument, the
     *            <code>MenuSelectionManager.defaultManager</code> is used.
     * @see MenuSelectionManager
     * @see JMenuItem#doClick(int)
     * @since 1.4
     */
    protected void doClick(MenuSelectionManager msm) {
        // Auditory cue
//        if (!isInternalFrameSystemMenu()) {
//            ActionMap map = menuItem.getActionMap();
//            if (map != null) {
//                Action audioAction = map.get(getPropertyPrefix() +
//                        ".commandSound");
//                if (audioAction != null) {
// pass off firing the Action to a utility method
//                    BasicLookAndFeel lf = (BasicLookAndFeel)
//                            UIManager.getLookAndFeel();
//                    lf.playSound(audioAction);
//                }
//            }
//        }
//        // Visual feedback
        if (msm == null) {
            msm = MenuSelectionManager.defaultManager();
        }
        msm.clearSelectedPath();
        menuItem.doClick(0);
    }

    protected ThemePainter getPainter() {
        return _painter;
    }

    protected boolean isDownArrowVisible(Container c) {
        if (c instanceof TopLevelMenuContainer && ((TopLevelMenuContainer) c).isMenuBar()) {
            return false;
        }
        else if (c instanceof TopLevelMenuContainer && !((TopLevelMenuContainer) c).isMenuBar()) {
            return true;
        }
        else if (c instanceof JMenuBar) {
            return false;
        }
        else {
            return true;
        }
    }

    protected boolean isFloatingIcon() {
        return _isFloatingIcon;
    }

    protected Icon getIcon(AbstractButton b) {
        ButtonModel model = b.getModel();
        Icon icon = b.getIcon();
        Icon tmpIcon = null;
        if (!model.isEnabled()) {
            if (model.isSelected()) {
                tmpIcon = b.getDisabledSelectedIcon();
            }
            else {
                tmpIcon = b.getDisabledIcon();
            }

            // create default disabled icon
            if (tmpIcon == null) {
                if (icon instanceof ImageIcon) {
                    icon = IconsFactory.createGrayImage(((ImageIcon) icon).getImage());
                }
                else {
                    icon = IconsFactory.createGrayImage(b, icon);
                }
            }
        }
        else if (model.isPressed() && model.isArmed()) {
            tmpIcon = b.getPressedIcon();
            if (tmpIcon != null) {
            }
        }
        else if (b.isRolloverEnabled() && model.isRollover()) {
            if (model.isSelected()) {
                tmpIcon = b.getRolloverSelectedIcon();
            }
            else {
                tmpIcon = b.getRolloverIcon();
            }
        }
        else if (model.isSelected()) {
            tmpIcon = b.getSelectedIcon();
        }

        if (tmpIcon != null) {
            icon = tmpIcon;
        }
        return icon;
    }

    protected int getRightMargin() {
        return 0;
    }
}