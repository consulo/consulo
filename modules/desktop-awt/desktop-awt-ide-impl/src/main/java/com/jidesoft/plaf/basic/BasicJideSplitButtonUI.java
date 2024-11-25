/*
 * @(#)VsnetMenuUI.java
 *
 * Copyright 2002 JIDE Software Inc. All rights reserved.
 */

package com.jidesoft.plaf.basic;

import com.jidesoft.icons.IconsFactory;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.vsnet.VsnetMenuUI;
import com.jidesoft.swing.*;
import org.jdesktop.swingx.plaf.basic.core.LazyActionMap;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


/**
 * SplitButtonUI implementation
 */
public class BasicJideSplitButtonUI extends VsnetMenuUI {
    protected ThemePainter _painter;

    protected Color _shadowColor;
    protected Color _darkShadowColor;
    protected Color _highlight;
    protected Color _lightHighlightColor;

    protected int _splitButtonMargin = 12;
    protected int _splitButtonMarginOnMenu = 20;

    protected boolean _isFloatingIcon = false;

    private FocusListener _focusListener;

    private static final String propertyPrefix = "JideSplitButton";

    @SuppressWarnings({"UnusedDeclaration"})
    public static ComponentUI createUI(JComponent x) {
        return new BasicJideSplitButtonUI();
    }

    @Override
    protected String getPropertyPrefix() {
        return propertyPrefix;
    }

    @Override
    protected void installDefaults() {
        _painter = (ThemePainter) UIDefaultsLookup.get("Theme.painter");
        _isFloatingIcon = UIDefaultsLookup.getBoolean("Icon.floating");

        _shadowColor = UIDefaultsLookup.getColor("JideButton.shadow");
        _darkShadowColor = UIDefaultsLookup.getColor("JideButton.darkShadow");
        _highlight = UIDefaultsLookup.getColor("JideButton.highlight");
        _lightHighlightColor = UIDefaultsLookup.getColor("JideButton.light");

        menuItem.setRolloverEnabled(true);

        super.installDefaults();
    }

    @Override
    protected void uninstallDefaults() {
        _painter = null;

        _shadowColor = null;
        _highlight = null;
        _lightHighlightColor = null;
        _darkShadowColor = null;

        super.uninstallDefaults();
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        if (_focusListener == null) {
            _focusListener = new FocusListener() {
                public void focusGained(FocusEvent e) {
                    menuItem.repaint();
                }

                public void focusLost(FocusEvent e) {
                    menuItem.repaint();
                }
            };
        }
        menuItem.addFocusListener(_focusListener);
    }

    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        if (_focusListener != null) {
            menuItem.removeFocusListener(_focusListener);
        }
    }

    /**
     * Returns the ui that is of type <code>clazz</code>, or null if one can not be found.
     *
     * @param ui    the ui
     * @param clazz the class
     * @return the actual ui for the class.
     */
    static Object getUIOfType(ComponentUI ui, Class clazz) {
        if (clazz.isInstance(ui)) {
            return ui;
        }
        return null;
    }

    /**
     * Returns the InputMap for condition <code>condition</code>. Called as part of
     * <code>installKeyboardActions</code>.
     *
     * @param condition the condition
     * @param c         the component
     * @return the input map.
     */
    public InputMap getInputMap(int condition, JComponent c) {
        if (condition == JComponent.WHEN_FOCUSED) {
            BasicJideSplitButtonUI ui = (BasicJideSplitButtonUI) getUIOfType(
                ((JideSplitButton) c).getUI(), BasicJideSplitButtonUI.class);
            if (ui != null) {
                return (InputMap) UIDefaultsLookup.get(ui.getPropertyPrefix() + ".focusInputMap");
            }
        }
        return null;
    }

    @Override
    protected void installKeyboardActions() {
        super.installKeyboardActions();
        AbstractButton b = menuItem;

        LazyActionMap.installLazyActionMap(b, BasicJideSplitButtonUI.class,
            "JideSplitButton.actionMap");

        InputMap km = getInputMap(JComponent.WHEN_FOCUSED, b);

        SwingUtilities.replaceUIInputMap(b, JComponent.WHEN_FOCUSED, km);
    }

    @Override
    protected void uninstallKeyboardActions() {
        AbstractButton b = menuItem;
        SwingUtilities.replaceUIInputMap(b, JComponent.
            WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIInputMap(b, JComponent.WHEN_FOCUSED, null);
        SwingUtilities.replaceUIActionMap(b, null);
        super.uninstallKeyboardActions();
    }

    @Override
    protected MouseInputListener createMouseInputListener(JComponent c) {
        return new MouseInputHandler();
    }

    @Override
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
        ButtonModel model = menuItem.getModel();
        int menuWidth;
        int menuHeight;
        int orientation = JideSwingUtilities.getOrientationOf(menuItem);
        if (orientation == SwingConstants.HORIZONTAL) {
            menuWidth = menuItem.getWidth();
            menuHeight = menuItem.getHeight();
        }
        else {
            menuWidth = menuItem.getHeight();
            menuHeight = menuItem.getWidth();
        }
        // have to change to HORIZONTAL because we rotate the Graphics already
        orientation = SwingConstants.HORIZONTAL;

        boolean paintBackground;
        Object o = menuItem.getClientProperty("JideSplitButton.alwaysPaintBackground");
        if (o instanceof Boolean) {
            paintBackground = (Boolean) o;
        }
        else {
            paintBackground = menuItem.isOpaque();
        }

        if (!((JMenu) menuItem).isTopLevelMenu()) {
            super.paintBackground(g, menuItem, bgColor);
            if (menuItem.isEnabled()) {
                if (model.isArmed() || model.isPressed() || isMouseOver()) {
                    g.setColor(selectionForeground);
                    g.drawLine(menuWidth - _splitButtonMarginOnMenu, 0, menuWidth - _splitButtonMarginOnMenu, menuHeight - 2);
                    JideSwingUtilities.paintArrow(g, selectionForeground, menuWidth - _splitButtonMarginOnMenu / 2 - 2, menuHeight / 2 - 3, 7, SwingConstants.VERTICAL);
                }
                else {
                    g.setColor(getForegroundOfState(menuItem));
                    g.drawLine(menuWidth - _splitButtonMarginOnMenu, 0, menuWidth - _splitButtonMarginOnMenu, menuHeight - 2);
                    JideSwingUtilities.paintArrow(g, getForegroundOfState(menuItem), menuWidth - _splitButtonMarginOnMenu / 2 - 2, menuHeight / 2 - 3, 7, SwingConstants.VERTICAL);
                }
            }
            else {
                g.setColor(UIDefaultsLookup.getColor("controlDkShadow"));
                g.drawLine(menuWidth - _splitButtonMarginOnMenu, 0, menuWidth - _splitButtonMarginOnMenu, menuHeight - 2);
                JideSwingUtilities.paintArrow(g, UIDefaultsLookup.getColor("controlDkShadow"), menuWidth - _splitButtonMarginOnMenu / 2 - 2, menuHeight / 2 - 3, 7, SwingConstants.VERTICAL);
            }
            return;
        }

        if (paintBackground) {
            if (menuItem.getParent() != null) {
                g.setColor(menuItem.getParent().getBackground());
            }
            else {
                g.setColor(menuItem.getBackground());
            }
            g.fillRect(0, 0, menuWidth, menuHeight);
        }

        JideSplitButton b = (JideSplitButton) menuItem;
        if (b.getButtonStyle() == ButtonStyle.TOOLBAR_STYLE) {
            if ((model.isSelected())) {
                if (isAlwaysDropdown(b)) {
                    Rectangle rect = new Rectangle(0, 0, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_SELECTED);
                }
                else if (b.getClientProperty(JideButton.CLIENT_PROPERTY_SEGMENT_POSITION) != null) {
                    Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isButtonEnabled()) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                    }
                    else if (paintBackground) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DISABLE_ROLLOVER);
                    }
                    rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_PRESSED);
                }
                else {
                    Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isButtonEnabled()) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_SELECTED);
                    }
                    else if (paintBackground) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DISABLE_SELECTED);
                    }
                    rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_SELECTED);
                    getPainter().paintSelectedMenu(b, g, new Rectangle(0, 0, menuWidth, menuHeight), orientation, ThemePainter.STATE_SELECTED);
                }
            }
            else if (model.isArmed() || model.isPressed()) {
                Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                if (b.isButtonEnabled()) {
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_PRESSED);
                }
                else if (paintBackground) {
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DISABLE);
                }
                rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
            }
            else if (model instanceof SplitButtonModel && ((DefaultSplitButtonModel) model).isButtonSelected()) {
                if ((isMouseOver() || b.hasFocus()) && model.isEnabled()) {
                    Rectangle rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                    rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isButtonEnabled()) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_PRESSED);
                    }
                    else if (paintBackground) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DISABLE);
                    }
                }
                else {
                    Rectangle rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DEFAULT);
                    rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isButtonEnabled()) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_SELECTED);
                    }
                    else if (paintBackground) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DISABLE_SELECTED);
                    }
                }
            }
            else if (((b.isRolloverEnabled() && isMouseOver()) || b.hasFocus()) && model.isEnabled()) {
                if (isAlwaysDropdown(b)) {
                    Rectangle rect = new Rectangle(0, 0, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                }
                else {
                    // Draw a line border with background
                    Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isButtonEnabled()) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                    }
                    else if (paintBackground) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DISABLE_ROLLOVER);
                    }
                    rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                }
            }
            else {
                if (paintBackground) {
                    Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isEnabled() && b.isButtonEnabled()) {
                        getPainter().paintButtonBackground(b, g, rect, 0, ThemePainter.STATE_DEFAULT);
                    }
                    else {
                        getPainter().paintButtonBackground(b, g, rect, 0, ThemePainter.STATE_DISABLE);
                    }

                    rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    if (b.isEnabled()) {
                        getPainter().paintButtonBackground(b, g, rect, 0, ThemePainter.STATE_DEFAULT);
                    }
                    else {
                        getPainter().paintButtonBackground(b, g, rect, 0, ThemePainter.STATE_DISABLE);
                    }
                }
            }
        }
        else if (b.getButtonStyle() == ButtonStyle.FLAT_STYLE) {
            if ((model.isSelected())) {
                // Draw a dark shadow border without bottom
                getPainter().paintSelectedMenu(b, g, new Rectangle(0, 0, menuWidth, menuHeight), orientation, ThemePainter.STATE_SELECTED);
            }
            else if (model.isArmed() || model.isPressed()) {
                Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                if (b.isButtonEnabled()) {
                    JideSwingUtilities.paintBackground(g, rect, _highlight, _highlight);
                }
                rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                JideSwingUtilities.paintBackground(g, rect, _highlight, _highlight);

                if (!b.isOpaque()) {
                    rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    paintSunkenBorder(g, rect);
                    rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    paintRaisedBorder(g, rect);
                }
            }
            else if (model instanceof SplitButtonModel && ((DefaultSplitButtonModel) model).isButtonSelected()) {
                if ((isMouseOver() || b.hasFocus()) && model.isEnabled()) {
                    Rectangle rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    JideSwingUtilities.paintBackground(g, rect, _highlight, _highlight);
                    rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isButtonEnabled()) {
                        JideSwingUtilities.paintBackground(g, rect, _highlight, _highlight);
                    }
                    if (!b.isOpaque()) {
                        rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        paintSunkenBorder(g, rect);
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        paintRaisedBorder(g, rect);
                    }
                }
                else {
                    Rectangle rect;
                    if (b.isOpaque()) {
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        JideSwingUtilities.paintBackground(g, rect, _highlight, _highlight);
                    }
                    rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    JideSwingUtilities.paintBackground(g, rect, _highlight, _highlight);

                    if (!b.isOpaque()) {
                        rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        paintSunkenBorder(g, rect);
//                        rect = new Rectangle(menuWidth - _splitButtonMargin + getOffset(), 0, _splitButtonMargin - getOffset(), menuHeight);
//                        paintRaisedBorder(g, rect);
                    }
                }

            }
            else {
                if (((b.isRolloverEnabled() && isMouseOver()) || b.hasFocus()) && model.isEnabled()) {
                    // Draw a line border with background
                    Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isButtonEnabled()) {
                        JideSwingUtilities.paintBackground(g, rect, _highlight, _highlight);
                    }
                    rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    JideSwingUtilities.paintBackground(g, rect, _highlight, _highlight);

                    if (isAlwaysDropdown(b)) {
                        rect = new Rectangle(0, 0, menuWidth, menuHeight);
                        paintRaisedBorder(g, rect);
                    }
                    else {
                        rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        paintRaisedBorder(g, rect);
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        paintRaisedBorder(g, rect);
                    }
                }
                else {
                    if (b.isOpaque()) {
                        Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        if (b.isButtonEnabled()) {
                            getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DEFAULT);
                        }
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DEFAULT);
                    }
                }
            }
        }
        else if (b.getButtonStyle() == ButtonStyle.TOOLBOX_STYLE) {
            if ((model.isSelected())) {
                // Draw a dark shadow border without bottom
                getPainter().paintSelectedMenu(b, g, new Rectangle(0, 0, menuWidth, menuHeight), orientation, ThemePainter.STATE_SELECTED);
            }
            else if (model.isArmed() || model.isPressed()) {
                Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                if (b.isButtonEnabled()) {
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_PRESSED);
                }
                rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);

                if (!b.isOpaque()) {
                    rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    paintSunken2Border(g, rect);
                    rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    paintRaisedBorder(g, rect);
                }
            }
            else if (model instanceof SplitButtonModel && ((DefaultSplitButtonModel) model).isButtonSelected()) {
                if (isMouseOver() && model.isEnabled()) {
                    Rectangle rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                    rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    if (b.isButtonEnabled()) {
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_PRESSED);
                    }
                    if (!b.isOpaque()) {
                        rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        paintSunken2Border(g, rect);
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        paintRaisedBorder(g, rect);
                    }
                }
                else {
                    Rectangle rect;
                    if (b.isOpaque()) {
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DEFAULT);
                    }
                    rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                    getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_SELECTED);

                    if (!b.isOpaque()) {
                        rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        paintSunken2Border(g, rect);
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        paintRaisedBorder(g, rect);
                    }
                }

            }
            else {
                if (b.isRolloverEnabled() && isMouseOver() && model.isEnabled()) {
                    // Draw a line border with background
                    if (isAlwaysDropdown(b)) {
                        Rectangle rect = new Rectangle(0, 0, menuWidth, menuHeight);
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                        paintRaised2Border(g, rect);
                    }
                    else {
                        Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        if (b.isButtonEnabled()) {
                            getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                        }
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_ROLLOVER);
                        rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        paintRaised2Border(g, rect);
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        paintRaised2Border(g, rect);
                    }
                }
                else {
                    if (b.isOpaque()) {
                        Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                        if (b.isButtonEnabled()) {
                            getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DEFAULT);
                        }
                        rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                        getPainter().paintButtonBackground(b, g, rect, orientation, ThemePainter.STATE_DEFAULT);
                    }
                    else {
                        if (isAlwaysDropdown(b)) {
                            Rectangle rect = new Rectangle(0, 0, menuWidth, menuHeight);
                            paintRaisedBorder(g, rect);
                        }
                        else {
                            Rectangle rect = getButtonRect(b, orientation, menuWidth, menuHeight);
                            paintRaisedBorder(g, rect);
                            rect = getDropDownRect(b, orientation, menuWidth, menuHeight);
                            paintRaisedBorder(g, rect);
                        }
                    }
                }
            }
        }

        paintArrow(menuItem, g);
    }

    protected void paintArrow(JMenuItem menuItem, Graphics g) {
        int menuWidth;
        int menuHeight;
        int orientation = JideSwingUtilities.getOrientationOf(menuItem);
        if (orientation == SwingConstants.HORIZONTAL) {
            menuWidth = menuItem.getWidth();
            menuHeight = menuItem.getHeight();
        }
        else {
            menuWidth = menuItem.getHeight();
            menuHeight = menuItem.getWidth();
        }
        int startX;
        if (menuItem.getComponentOrientation().isLeftToRight()) {
            startX = menuWidth - 9;
        }
        else {
            startX = 4;
        }
        if (menuItem.isEnabled()) {
            JideSwingUtilities.paintArrow(g, getForegroundOfState(menuItem), startX, menuHeight / 2 - 1, 5, SwingConstants.HORIZONTAL);
        }
        else {
            JideSwingUtilities.paintArrow(g, UIDefaultsLookup.getColor("controlShadow"), startX, menuHeight / 2 - 1, 5, SwingConstants.HORIZONTAL);
        }
    }

    /**
     * Gets the bounds for the drop down part of the <code>JideSplitButton</code>.
     *
     * @param c           the component. In this case, it is the <code>JideSplitButton</code>.
     * @param orientation the orientation.
     * @param width       the width of the <code>JideSplitButton</code>
     * @param height      the height of the <code>JideSplitButton</code>.
     * @return the bounds for the drop down part of the <code>JideSplitButton</code>.
     */
    protected Rectangle getDropDownRect(JComponent c, int orientation, int width, int height) {
        Object position = c.getClientProperty(JideButton.CLIENT_PROPERTY_SEGMENT_POSITION);
        Rectangle rect;
        if (c.getComponentOrientation().isLeftToRight()) {
            rect = new Rectangle(width - _splitButtonMargin - 1 + getOffset(), 0, _splitButtonMargin - getOffset(), height);
        }
        else {
            rect = new Rectangle(0, 0, _splitButtonMargin - getOffset(), height);
        }
        if (position == null || JideButton.SEGMENT_POSITION_ONLY.equals(position)) {
        }
        else if (JideButton.SEGMENT_POSITION_FIRST.equals(position)) {
            if (orientation == SwingConstants.HORIZONTAL) {
                rect.width++;
            }
            else {
                rect.height++;
            }
        }
        else if (JideButton.SEGMENT_POSITION_MIDDLE.equals(position)) {
            if (orientation == SwingConstants.HORIZONTAL) {
                rect.width++;
            }
            else {
                rect.height++;
            }
        }
        else if (JideButton.SEGMENT_POSITION_LAST.equals(position)) {
        }
        return rect;
    }

    /**
     * Gets the bounds for the button part of the <code>JideSplitButton</code>.
     *
     * @param c           the component. In this case, it is the <code>JideSplitButton</code>.
     * @param orientation the orientation.
     * @param width       the width of the <code>JideSplitButton</code>
     * @param height      the height of the <code>JideSplitButton</code>.
     * @return the bounds for the button part of the <code>JideSplitButton</code>.
     */
    protected Rectangle getButtonRect(JComponent c, int orientation, int width, int height) {
        Rectangle rect;
        if (orientation == SwingConstants.HORIZONTAL && c.getComponentOrientation().isLeftToRight()) {
            rect = new Rectangle(0, 0, width - _splitButtonMargin, height);
        }
        else {
            rect = new Rectangle(_splitButtonMargin - 1, 0, width - _splitButtonMargin, height);
        }
        return rect;
    }

    protected void paintSunkenBorder(Graphics g, Rectangle b) {
        Color old = g.getColor();
        g.setColor(_shadowColor);    // inner 3D border
        g.drawLine(b.x, b.y, b.x + b.width - 1, b.y);
        g.drawLine(b.x, b.y, b.x, b.y + b.height - 1);

        g.setColor(_lightHighlightColor);     // black drop shadow  __|
        g.drawLine(b.x, b.y + b.height - 1, b.x + b.width - 1, b.y + b.height - 1);
        g.drawLine(b.x + b.width - 1, b.y, b.x + b.width - 1, b.y + b.height - 1);
        g.setColor(old);
    }

    protected void paintSunken2Border(Graphics g, Rectangle b) {
        Color old = g.getColor();
        g.setColor(_darkShadowColor);    // inner 3D border
        g.drawLine(b.x, b.y, b.x + b.width - 2, b.y);
        g.drawLine(b.x, b.y, b.x, b.y + b.height - 2);

        g.setColor(_shadowColor);    // inner 3D border
        g.drawLine(b.x + 1, b.y + 1, b.x + b.width - 3, b.y + 1);
        g.drawLine(b.x + 1, b.y + 1, b.x + 1, b.y + b.height - 3);

        g.setColor(_lightHighlightColor);     // black drop shadow  __|
        g.drawLine(b.x, b.y + b.height - 1, b.x + b.width - 1, b.y + b.height - 1);
        g.drawLine(b.x + b.width - 1, b.x, b.x + b.width - 1, b.y + b.height - 1);
        g.setColor(old);
    }

    protected void paintRaised2Border(Graphics g, Rectangle b) {
        Color old = g.getColor();
        g.setColor(_lightHighlightColor);    // inner 3D border
        g.drawLine(b.x, b.y, b.x + b.width - 1, b.y);
        g.drawLine(b.x, b.y, b.x, b.y + b.height - 1);

        g.setColor(_shadowColor);     // gray drop shadow  __|
        g.drawLine(b.x + 1, b.y + b.height - 2, b.x + b.width - 2, b.y + b.height - 2);
        g.drawLine(b.x + b.width - 2, 1, b.x + b.width - 2, b.y + b.height - 2);

        g.setColor(_darkShadowColor);     // black drop shadow  __|
        g.drawLine(b.x, b.y + b.height - 1, b.x + b.width - 1, b.y + b.height - 1);
        g.drawLine(b.x + b.width - 1, b.y, b.x + b.width - 1, b.y + b.height - 1);
        g.setColor(old);
    }

    protected void paintRaisedBorder(Graphics g, Rectangle b) {
        Color old = g.getColor();
        g.setColor(_lightHighlightColor);    // inner 3D border
        g.drawLine(b.x, b.y, b.x + b.width - 1, b.y);
        g.drawLine(b.x, b.y, b.x, b.y + b.height - 1);

        g.setColor(_shadowColor);     // black drop shadow  __|
        g.drawLine(b.x, b.y + b.height - 1, b.x + b.width - 1, b.y + b.height - 1);
        g.drawLine(b.x + b.width - 1, b.y, b.x + b.width - 1, b.y + b.height - 1);
        g.setColor(old);
    }

    protected class MouseInputHandler implements MouseInputListener {
        public void mouseClicked(MouseEvent e) {
            cancelMenuIfNecessary(e);
        }

        /**
         * Invoked when the mouse has been clicked on the menu. This method clears or sets the selection path of the
         * MenuSelectionManager.
         *
         * @param e the mouse event
         */
        public void mousePressed(MouseEvent e) {
            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled())
                return;

            setMouseOver(true);

            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            if (isClickOnButton(e, menu)) {
                if (((JideSplitButton) menuItem).isButtonEnabled()) {
                    // click button
                    menu.getModel().setArmed(true);
                    menu.getModel().setPressed(true);
                }
                if (!menu.hasFocus() && menu.isRequestFocusEnabled()) {
                    menu.requestFocus();
                }
            }
            else {
                downButtonPressed(menu);
            }
        }

        private boolean isClickOnButton(MouseEvent e, JMenu menu) {
            if (((JideSplitButton) menu).isAlwaysDropdown()) {
                return false;
            }

            boolean clickOnDropDown = false;
            if (BasicJideButtonUI.shouldWrapText(menuItem)) {
                int size = 27;
                if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL) {
                    if (e.getPoint().getY() < menu.getHeight() - size) {
                        clickOnDropDown = true;
                    }
                }
                else {
                    if (e.getPoint().getY() < menu.getHeight() - size) {
                        clickOnDropDown = true;
                    }
                }
            }
            else {
                int size = ((JMenu) menuItem).isTopLevelMenu() ? _splitButtonMargin : _splitButtonMarginOnMenu;
                if (JideSwingUtilities.getOrientationOf(menuItem) == SwingConstants.HORIZONTAL) {
                    if (menu.getComponentOrientation().isLeftToRight()) {
                        if (e.getPoint().getX() < menu.getWidth() - size) {
                            clickOnDropDown = true;
                        }
                    }
                    else {
                        if (e.getPoint().getX() >= size) {
                            clickOnDropDown = true;
                        }
                    }
                }
                else {
                    if (e.getPoint().getY() < menu.getHeight() - size) {
                        clickOnDropDown = true;
                    }
                }
            }
            return clickOnDropDown;
        }

        /**
         * Invoked when the mouse has been released on the menu. Delegates the mouse event to the MenuSelectionManager.
         *
         * @param e the mouse event
         */
        public void mouseReleased(MouseEvent e) {
            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled()) {
                return;
            }
            if (!isClickOnButton(e, menu)) {
                // these two lines order matters. In this order, it would not trigger actionPerformed.
                menuItem.getModel().setArmed(false);
                menuItem.getModel().setPressed(false);
            }
            cancelMenuIfNecessary(e);
        }

        private void cancelMenuIfNecessary(MouseEvent e) {
            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled())
                return;
            if (isClickOnButton(e, menu)) {
                if (((JideSplitButton) menuItem).isButtonEnabled()) {
                    // click button
                    // these two lines order matters. In this order, it would trigger actionPerformed.
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        menu.getModel().setPressed(false);
                        menu.getModel().setArmed(false);
                    }
                    else {
                        menu.getModel().setArmed(false);
                        menu.getModel().setPressed(false);
                    }

                    MenuSelectionManager manager = MenuSelectionManager.defaultManager();
                    MenuElement[] menuElements = manager.getSelectedPath();
                    for (int i = menuElements.length - 1; i >= 0; i--) {
                        MenuElement menuElement = menuElements[i];
                        if (menuElement instanceof JPopupMenu && ((JPopupMenu) menuElement).isAncestorOf(menu)) {
                            menu.getModel().setRollover(false);
                            setMouseOver(false);
                            manager.clearSelectedPath();
                        }
                    }
                }
            }
            else {
//                MenuSelectionManager manager =
//                        MenuSelectionManager.defaultManager();
//                manager.processMouseEvent(e);
//                if (!e.isConsumed())
//                    manager.clearSelectedPath();
            }
        }

        /**
         * Invoked when the cursor enters the menu. This method sets the selected path for the MenuSelectionManager and
         * handles the case in which a menu item is used to pop up an additional menu, as in a hierarchical menu
         * system.
         *
         * @param e the mouse event; not used
         */
        public void mouseEntered(MouseEvent e) {
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
         * @see java.awt.event.MouseMotionListener#mouseDragged
         */
        public void mouseDragged(MouseEvent e) {
            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled())
                return;
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseMoved(MouseEvent e) {
            JMenu menu = (JMenu) menuItem;
            if (!menu.isEnabled())
                return;

            if (menuItem instanceof JideSplitButton) {
                if (isClickOnButton(e, ((JMenu) menuItem))) {
                    ((SplitButtonModel) ((JideSplitButton) menuItem).getModel()).setButtonRollover(true);
                }
                else {
                    ((SplitButtonModel) ((JideSplitButton) menuItem).getModel()).setButtonRollover(false);
                }
            }
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (!(c instanceof JMenu) || !((JMenu) c).isTopLevelMenu()) {
            return super.getPreferredSize(c);
        }

        AbstractButton b = (AbstractButton) c;

        boolean isHorizontal = JideSwingUtilities.getOrientationOf(c) == SwingConstants.HORIZONTAL;

        Dimension d = JideSwingUtilities.getPreferredButtonSize(b, defaultTextIconGap, true); // TODO: we should use isHorizontal when JideSwingUtilities.getPreferredButtonSize supports it

        if (BasicJideButtonUI.shouldWrapText(c)) {
            if (c instanceof JideSplitButton) {
                d.width += getAdjustExtraWidth(b, b.getText(), 8);
            }
        }
        else {
            d.width += getRightMargin();

            if (isDownArrowVisible(b.getParent())) {
                d.width += 1;
            }
        }

        if (isHorizontal)
            return d;
        else
            //noinspection SuspiciousNameCombination
            return new Dimension(d.height, d.width); // swap width and height
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        if (!(c instanceof JMenu) || !((JMenu) c).isTopLevelMenu()) {
            return super.getMinimumSize(c);
        }

        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            if (JideSwingUtilities.getOrientationOf(c) == SwingConstants.HORIZONTAL)
                d.width -= v.getPreferredSpan(View.X_AXIS) - v.getMinimumSpan(View.X_AXIS);
            else        // TODO: not sure if this is correct
                d.height -= v.getPreferredSpan(View.X_AXIS) - v.getMinimumSpan(View.X_AXIS);
        }

        return d;
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        if (!(c instanceof JMenu) || !((JMenu) c).isTopLevelMenu()) {
            return super.getMaximumSize(c);
        }

        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            if (JideSwingUtilities.getOrientationOf(c) == SwingConstants.HORIZONTAL)
                d.width += v.getMaximumSpan(View.X_AXIS) - v.getPreferredSpan(View.X_AXIS);
            else        // TODO: not sure if this is correct
                d.height += v.getMaximumSpan(View.X_AXIS) - v.getPreferredSpan(View.X_AXIS);
        }

        return d;
    }

    @Override
    protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
        // Note: This method is almost identical to the same method in WindowsMenuItemUI
        ButtonModel model = menuItem.getModel();

        FontMetrics fm = menuItem.getFontMetrics(menuItem.getFont());
        if (!(menuItem instanceof JMenu) || !((JMenu) menuItem).isTopLevelMenu()) {
            int defaultTextIconGap = UIDefaultsLookup.getInt("MenuItem.textIconGap");
            int defaultShadowWidth = UIDefaultsLookup.getInt("MenuItem.shadowWidth");
            if (menuItem.getComponentOrientation().isLeftToRight()) {
                textRect.x = defaultShadowWidth + defaultTextIconGap;
            }
            else {
                // isLeftToRight is false
                Rectangle2D rectText = fm.getStringBounds(text, g);
                textRect.x = (int) (menuItem.getWidth() - defaultShadowWidth - defaultTextIconGap + rectText.getWidth() + (4 + menuItem.getHeight() / 2 - 1));
            }
        }
        else if (!menuItem.getComponentOrientation().isLeftToRight()) {
            if (menuItem.getComponentOrientation().isHorizontal()) {
                Rectangle2D rectText = fm.getStringBounds(text, g);
                textRect.x = (int) (menuItem.getWidth() - textRect.x - rectText.getWidth() + (4 + menuItem.getHeight() / 2 - 1));
            }
        }

        int mnemonicIndex = menuItem.getDisplayedMnemonicIndex();
        // W2K Feature: Check to see if the Underscore should be rendered.
        if (LookAndFeelFactory.isMnemonicHidden()) {
            mnemonicIndex = -1;
        }

        Color oldColor = g.getColor();

        if (!model.isEnabled() || (menuItem instanceof JideSplitButton && !((JideSplitButton) menuItem).isButtonEnabled())) {
            if (menuItem.getParent() != null) {
                // *** paint the text disabled
                g.setColor(menuItem.getParent().getBackground().brighter());

                // JDK PORTING HINT
                // JDK1.3: No drawStringUnderlineCharAt, draw the string then draw the underline
                drawStringUnderlineCharAt(menuItem, g, text, mnemonicIndex,
                    textRect.x, textRect.y + fm.getAscent());
                g.setColor(menuItem.getParent().getBackground().darker());
            }

            // JDK PORTING HINT
            // JDK1.3: No drawStringUnderlineCharAt, draw the string then draw the underline
            drawStringUnderlineCharAt(menuItem, g, text, mnemonicIndex,
                textRect.x - 1, textRect.y + fm.getAscent() - 1);
        }
        else {
            // For Win95, the selected text color is the selection foreground color
            Color color = getForegroundOfState(menuItem);
            if (color == null || color instanceof UIResource) {
                if (model.isSelected()) {
                    g.setColor(selectionForeground);
                }
                else {
                    g.setColor(color);
                }
            }
            else {
                g.setColor(color);
            }
            drawStringUnderlineCharAt(menuItem, g, text,
                mnemonicIndex,
                textRect.x,
                textRect.y + fm.getAscent());
        }
        g.setColor(oldColor);
    }

    private Color getForegroundOfState(JMenuItem menuItem) {
        int state = JideSwingUtilities.getButtonState(menuItem);
        Color foreground = null;
        if (menuItem instanceof ComponentStateSupport) {
            foreground = ((ComponentStateSupport) menuItem).getForegroundOfState(state);
        }
        if (foreground == null || foreground instanceof UIResource) {
            foreground = menuItem.getForeground();
        }
        return foreground;
    }

    protected void drawStringUnderlineCharAt(JComponent c, Graphics g, String text,
                                             int underlinedIndex, int x, int y) {
        BasicGraphicsUtils.drawStringUnderlineCharAt(c, (Graphics2D) g, text, underlinedIndex, x, y);
    }

    @Override
    protected void paintIcon(JMenuItem b, Graphics g) {
        ButtonModel model = b.getModel();

        // Paint the Icon
        if (b.getIcon() != null) {
//            if (JideSwingUtilities.getOrientationOf(b) == SwingConstants.VERTICAL) {
//                ((Graphics2D) g).translate(0, b.getWidth() - 1);
//                ((Graphics2D) g).rotate(-Math.PI / 2);
//            }

            Icon icon = getIcon(b);

            if (icon != null) {
                if (!b.getComponentOrientation().isLeftToRight()) {
                    if (b.getComponentOrientation().isHorizontal()) {
                        iconRect.x = b.getWidth() - iconRect.x - icon.getIconWidth() + (4 + b.getHeight() / 2 - 1);
                    }
                }

                boolean enabled = model.isEnabled() && (!(model instanceof SplitButtonModel) || ((SplitButtonModel) model).isButtonEnabled());
                if (isFloatingIcon() && enabled) {
                    if (model.isRollover() && !model.isPressed() && !model.isSelected()) {
                        icon.paintIcon(b, g, iconRect.x, iconRect.y);
                    }
                    else {
                        icon.paintIcon(b, g, iconRect.x, iconRect.y);
                    }
                }
                else {
                    icon.paintIcon(b, g, iconRect.x, iconRect.y);
                }
            }

//            if (JideSwingUtilities.getOrientationOf(b) == SwingConstants.VERTICAL) {
//                ((Graphics2D) g).rotate(Math.PI / 2);
//                ((Graphics2D) g).translate(0, -b.getHeight() + 1);
//            }
        }
    }

    @Override
    protected boolean isFloatingIcon() {
        return _isFloatingIcon;
    }

    @Override
    protected Icon getIcon(AbstractButton b) {
        ButtonModel model = b.getModel();
        Icon icon = b.getIcon();
        Icon tmpIcon = null;
        if (!model.isEnabled() || !((JideSplitButton) menuItem).isButtonEnabled()) {
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
                // revert back to 0 offset
                // clearTextShiftOffset();
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

    /**
     * The gap between the button part and the drop down menu part.
     *
     * @return the gap.
     */
    protected int getOffset() {
        return 0;
    }

    protected boolean isAlwaysDropdown(JMenuItem menuItem) {
        return menuItem instanceof JideSplitButton && ((JideSplitButton) menuItem).isAlwaysDropdown();
    }

    @Override
    protected int getRightMargin() {
        return _splitButtonMargin - 1;
    }

    /**
     * Actions for Buttons. Two type of action are supported: pressed: Moves the button to a pressed state released:
     * Disarms the button.
     */
    private static class Actions extends UIAction {
        private static final String PRESS = "pressed";
        private static final String RELEASE = "released";
        private static final String DOWN_PRESS = "downPressed";
        private static final String DOWN_RELEASE = "downReleased";

        Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            AbstractButton b = (AbstractButton) e.getSource();
            String key = getName();

            // if isAlwaysDropDown it true, treat PRESS as DOWN_PRESS
            if (PRESS.equals(key) && ((JideSplitButton) b).isAlwaysDropdown()) {
                key = DOWN_PRESS;
            }

            if (PRESS.equals(key)) {
                ButtonModel model = b.getModel();
                model.setArmed(true);
                model.setPressed(true);
                if (!b.hasFocus()) {
                    b.requestFocus();
                }
            }
            else if (RELEASE.equals(key)) {
                ButtonModel model = b.getModel();
                model.setPressed(false);
                model.setArmed(false);
            }
            else if (DOWN_PRESS.equals(key)) {
                downButtonPressed((JMenu) b);
            }
            else if (DOWN_RELEASE.equals(key)) {
            }
        }

        @Override
        public boolean isEnabled(Object sender) {
            return !(sender != null && (sender instanceof AbstractButton) &&
                !((AbstractButton) sender).getModel().isEnabled());
        }
    }

    /**
     * Populates Buttons actions.
     *
     * @param map the map
     */
    public static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.PRESS));
        map.put(new Actions(Actions.RELEASE));
        map.put(new Actions(Actions.DOWN_PRESS));
        map.put(new Actions(Actions.DOWN_RELEASE));
    }

    @Override
    protected void updateMnemonicBinding() {
        super.updateMnemonicBinding();
        int mnemonic = menuItem.getModel().getMnemonic();
        if (mnemonic != 0 && windowInputMap != null) {
            int[] shortcutKeys = (int[]) UIDefaultsLookup.get("Menu.shortcutKeys");
            if (shortcutKeys == null) {
                shortcutKeys = new int[]{KeyEvent.ALT_MASK};
            }
            for (int shortcutKey : shortcutKeys) {
                windowInputMap.put(KeyStroke.getKeyStroke(mnemonic,
                        shortcutKey, false),
                    "pressed");
                windowInputMap.put(KeyStroke.getKeyStroke(mnemonic,
                        shortcutKey, true),
                    "released");
            }
        }
    }

    protected static void downButtonPressed(JMenu menu) {
        MenuSelectionManager manager = MenuSelectionManager.defaultManager();
        if (menu.isTopLevelMenu()) {
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

    protected static Container getFirstParentMenuElement(Component comp) {
        Container parent = comp.getParent();

        while (parent != null) {
            if (parent instanceof MenuElement)
                return parent;

            parent = parent.getParent();
        }

        return null;
    }


    /**
     * @param c          the component
     * @param text       the text
     * @param extraWidth the extra width
     * @return the adjusted width.
     */
    public static int getAdjustExtraWidth(Component c, String text, int extraWidth) {
        String[] lines = getWrappedText(text);
        Font font = c.getFont();
        FontMetrics fm = c.getFontMetrics(font);
        int line1Width = fm.stringWidth(lines[0]);
        int line2Width = lines.length <= 1 ? 0 : fm.stringWidth(lines[1]);
        int oldMaxWidth = Math.max(line1Width, line2Width);
        line2Width += extraWidth;
        int maxWidth = Math.max(line1Width, line2Width);
        return maxWidth - oldMaxWidth;
    }

    public static String getMaxLengthWord(String text) {
        if (text.indexOf(' ') == -1) {
            return text;
        }
        else {
            int minDiff = text.length();
            int minPos = -1;
            int mid = text.length() / 2;

            int pos = -1;
            while (true) {
                pos = text.indexOf(' ', pos + 1);
                if (pos == -1) {
                    break;
                }
                int diff = Math.abs(pos - mid);
                if (diff < minDiff) {
                    minDiff = diff;
                    minPos = pos;
                }
            }
            return minPos >= mid ? text.substring(0, minPos) : text.substring(minPos + 1);
        }
    }

    /**
     * Gets the text after wrapping. Please note, it will only wrap text into two lines thus it is not designed for
     * general usage.
     *
     * @param text the text
     * @return the two lines.
     */
    public static String[] getWrappedText(String text) {
        String[] words = text.split(" ");
        if (words.length <= 2) {
            return words; // no line break
        }
        else if (words.length >= 3) {
            int minDiff = text.length();
            int minPos = -1;
            int pos = -1;
            int mid = text.length() / 2;
            while (true) {
                pos = text.indexOf(' ', pos + 1);
                if (pos == -1) {
                    break;
                }
                int diff = Math.abs(pos - mid);
                if (diff < minDiff) {
                    minDiff = diff;
                    minPos = pos;
                }
            }
            return new String[]{text.substring(0, minPos), text.substring(minPos + 1)};
        }

        return words;
    }
}


