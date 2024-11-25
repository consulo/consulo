/*
 * @(#)JideSplitButton.java 2/26/2005
 *
 * Copyright 2002 - 2005 JIDE Software Inc. All rights reserved.
 */
package com.jidesoft.swing;

import com.formdev.flatlaf.ui.FlatButtonBorder;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.basic.ThemePainter;
import com.jidesoft.utils.SystemInfo;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * <code>JideSplitButton</code> is a combination of button and menu. There is a line in the middle of the button that
 * splits the button into two portions. The portion before the line is a button. User can click on it and trigger an
 * action. The portion after the line is a menu. User can click on it to show a normal menu.
 * <p/>
 * Please be noted that, when you try to use JideSplitButton as a menu item, please make sure that you will re-configure
 * its font with the following code. Otherwise, it may look different with the other JMenuItems.
 * <code><pre>
 *         splitButton.setFont((Font) JideSwingUtilities.getMenuFont(Toolkit.getDefaultToolkit(), UIManager.getDefaults()));
 * </pre></code>
 */
public class JideSplitButton extends JideMenu implements ButtonStyle, ComponentStateSupport {

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "JideSplitButtonUI";

    private int _buttonStyle = TOOLBAR_STYLE;

    private boolean _alwaysDropdown = false;

    public static final String PROPERTY_ALWAYS_DROPDOWN = "alwaysDropdown";
    public static final String ACTION_PROPERTY_SPLIT_BUTTON_ENABLED = "JideSplitButtonEnabled";

    public JideSplitButton() {
        initComponent();
    }

    public JideSplitButton(String s) {
        super(s);
        initComponent();
    }

    public JideSplitButton(String s, Icon icon) {
        super(s);
        setIcon(icon);
        initComponent();
    }

    public JideSplitButton(Icon icon) {
        super("");
        setIcon(icon);
        initComponent();
    }

    public JideSplitButton(Action a) {
        super(a);
        initComponent();
    }

    protected void initComponent() {
        setModel(new DefaultSplitButtonModel());
        if (getAction() != null) {
            configurePropertiesFromAction(getAction());
        }
        setFocusable(true);
        setRequestFocusEnabled(false);
    }

    /**
     * Returns the split button 's current UI.
     *
     * @see #setUI
     */
    @Override
    public ButtonUI getUI() {
        return (ButtonUI) ui;
    }

    /**
     * Sets the L&F object that renders this component.
     *
     * @param ui the <code>JideSplitButtonUI</code> L&F object
     * @see javax.swing.UIDefaults#getUI
     */
    @Override
    public void setUI(ButtonUI ui) {
        super.setUI(ui);
    }

    /**
     * Notification from the <code>UIFactory</code> that the L&F has changed. Called to replace the UI with the latest
     * version from the <code>UIFactory</code>.
     *
     * @see javax.swing.JComponent#updateUI
     */
    @Override
    public void updateUI() {
        if (UIDefaultsLookup.get(uiClassID) == null) {
            LookAndFeelFactory.installJideExtension();
        }
        
        setUI(UIManager.getUI(this));
        invalidate();
        setBorder(new FlatButtonBorder());
    }


    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "JideSplitButtonUI"
     * @see javax.swing.JComponent#getUIClassID
     * @see javax.swing.UIDefaults#getUI
     */
    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Returns the state of the button part of the JideSplitButton. True if the toggle button is selected, false if it's
     * not.
     *
     * @return true if the toggle button is selected, otherwise false
     */
    public boolean isButtonSelected() {
        return model instanceof SplitButtonModel && ((DefaultSplitButtonModel) model).isButtonSelected();
    }

    /**
     * Sets the state of the button part of the JideSplitButton. Note that this method does not trigger an
     * <code>actionEvent</code>. Call <code>doClick</code> to perform a programmatic action change.
     *
     * @param b true if the button is selected, otherwise false
     */
    public void setButtonSelected(boolean b) {
        if (model instanceof SplitButtonModel) {
            ((DefaultSplitButtonModel) model).setButtonSelected(b);
        }
    }

    /**
     * Returns the state of the button part of the JideSplitButton. True if the button is enabled, false if it's not.
     *
     * @return true if the button is enabled, otherwise false
     */
    public boolean isButtonEnabled() {
        return model instanceof SplitButtonModel && ((DefaultSplitButtonModel) model).isButtonEnabled();
    }

    /**
     * Sets the state of the button part of the JideSplitButton.
     *
     * @param b true if the button is enabled, otherwise false
     */
    public void setButtonEnabled(boolean b) {
        if (model instanceof SplitButtonModel) {
            ((DefaultSplitButtonModel) model).setButtonEnabled(b);
        }
    }

    /**
     * Gets the button style.
     *
     * @return the button style.
     */
    @Override
    public int getButtonStyle() {
        return _buttonStyle;
    }

    /**
     * Sets the button style.
     *
     * @param buttonStyle the new button style.
     */
    @Override
    public void setButtonStyle(int buttonStyle) {
        if (buttonStyle < 0 || buttonStyle > FLAT_STYLE) {
            throw new IllegalArgumentException("Only TOOLBAR_STYLE, TOOLBOX_STYLE, and FLAT_STYLE are supported");
        }
        if (buttonStyle == _buttonStyle)
            return;

        int oldStyle = _buttonStyle;
        _buttonStyle = buttonStyle;

        firePropertyChange(BUTTON_STYLE_PROPERTY, oldStyle, _buttonStyle);
    }

    /**
     * Checks the alwaysDropdown property value.
     *
     * @return true or false. If true, the split button doesn't have default action. It always drops down the menu when
     * mouse clicks
     */
    public boolean isAlwaysDropdown() {
        return _alwaysDropdown;
    }

    /**
     * If the property is true, the split button doesn't have default action. It always drops down the menu when mouse
     * clicks. By default, this value is false.
     *
     * @param alwaysDropdown true or false.
     */
    public void setAlwaysDropdown(boolean alwaysDropdown) {
        if (_alwaysDropdown != alwaysDropdown) {
            boolean old = _alwaysDropdown;
            _alwaysDropdown = alwaysDropdown;
            firePropertyChange(PROPERTY_ALWAYS_DROPDOWN, old, alwaysDropdown);
        }
    }

    @Override
    public void setText(String text) {
        // this code is to fix a bug in JDK1.5's JMenuItem line 385. It should check hideActionText first
        Boolean hide = (Boolean) getClientProperty("hideActionText");
        if (hide == null || Boolean.FALSE.equals(hide)) {
            super.setText(text);
        }
    }

    private Color _defaultForeground;
    private Color _rolloverBackground;
    private Color _selectedBackground;
    private Color _pressedBackground;
    private Color _rolloverForeground;
    private Color _selectedForeground;
    private Color _pressedForeground;

    public Color getDefaultForeground() {
        return _defaultForeground;
    }

    public void setDefaultForeground(Color defaultForeground) {
        _defaultForeground = defaultForeground;
    }

    private Color getRolloverBackground() {
        return _rolloverBackground;
    }

    private void setRolloverBackground(Color rolloverBackground) {
        _rolloverBackground = rolloverBackground;
    }

    private Color getSelectedBackground() {
        return _selectedBackground;
    }

    private void setSelectedBackground(Color selectedBackground) {
        _selectedBackground = selectedBackground;
    }

    private Color getPressedBackground() {
        return _pressedBackground;
    }

    private void setPressedBackground(Color pressedBackground) {
        _pressedBackground = pressedBackground;
    }

    private Color getRolloverForeground() {
        return _rolloverForeground;
    }

    private void setRolloverForeground(Color rolloverForeground) {
        _rolloverForeground = rolloverForeground;
    }

    private Color getSelectedForeground() {
        return _selectedForeground;
    }

    private void setSelectedForeground(Color selectedForeground) {
        _selectedForeground = selectedForeground;
    }

    private Color getPressedForeground() {
        return _pressedForeground;
    }

    private void setPressedForeground(Color pressedForeground) {
        _pressedForeground = pressedForeground;
    }

    /**
     * Gets the background for different states. The states are defined in ThemePainter as constants. Not all states are
     * supported by all components. If the state is not supported or background is never set, it will return null.
     * <p/>
     * Please note, each L&F will have its own way to paint the different backgrounds. This method allows you to
     * customize it for each component to use a different background. So if you want the background to be used, don't
     * use a ColorUIResource because UIResource is considered as a setting set by the L&F and any L&F can choose to
     * ignore it.
     *
     * @param state the button state. Please refer to {@link com.jidesoft.plaf.basic.ThemePainter} to see the list of
     *              available states.
     * @return the background for different states.
     */
    @Override
    public Color getBackgroundOfState(int state) {
        switch (state) {
            case ThemePainter.STATE_DEFAULT:
                return getBackground();
            case ThemePainter.STATE_ROLLOVER:
                return getRolloverBackground();
            case ThemePainter.STATE_SELECTED:
                return getSelectedBackground();
            case ThemePainter.STATE_PRESSED:
                return getPressedBackground();
        }
        return null;
    }

    /**
     * Sets the background for different states.  The states are defined in ThemePainter as constants. Not all states
     * are supported by all components. If the state is not supported or background is never set, it will return null.
     * <p/>
     * Please note, each L&F will have its own way to paint the different backgrounds. This method allows you to
     * customize it for each component to use a different background. So if you want the background to be used, don't
     * use a ColorUIResource because UIResource is considered as a setting set by the L&F and any L&F can choose to
     * ignore it.
     *
     * @param state the button state. Please refer to {@link com.jidesoft.plaf.basic.ThemePainter} to see the list of
     *              available states.
     * @param color the background color
     */
    @Override
    public void setBackgroundOfState(int state, Color color) {
        switch (state) {
            case ThemePainter.STATE_DEFAULT:
                setBackground(color);
                break;
            case ThemePainter.STATE_ROLLOVER:
                setRolloverBackground(color);
                break;
            case ThemePainter.STATE_SELECTED:
                setSelectedBackground(color);
                break;
            case ThemePainter.STATE_PRESSED:
                setPressedBackground(color);
                break;
        }
    }

    /**
     * Gets the foreground for different states. The states are defined in ThemePainter as constants. Not all states are
     * supported by all components. If the state is not supported or foreground is never set, it will return null.
     * <p/>
     * Please note, each L&F will have its own way to paint the different foregrounds. This method allows you to
     * customize it for each component to use a different foreground. So if you want the foreground to be used, don't
     * use a ColorUIResource because UIResource is considered as a setting set by the L&F and any L&F can choose to
     * ignore it.
     *
     * @param state the button state. Please refer to {@link com.jidesoft.plaf.basic.ThemePainter} to see the list of
     *              available states.
     * @return the foreground for different states.
     */
    @Override
    public Color getForegroundOfState(int state) {
        switch (state) {
            case ThemePainter.STATE_DEFAULT:
                return getDefaultForeground();
            case ThemePainter.STATE_ROLLOVER:
                return getRolloverForeground();
            case ThemePainter.STATE_SELECTED:
                return getSelectedForeground();
            case ThemePainter.STATE_PRESSED:
                return getPressedForeground();
        }
        return null;
    }


    /**
     * Sets the foreground for different states.  The states are defined in ThemePainter as constants. Not all states
     * are supported by all components. If the state is not supported or foreground is never set, it will return null.
     * <p/>
     * Please note, each L&F will have its own way to paint the different foregrounds. This method allows you to
     * customize it for each component to use a different foreground. So if you want the foreground to be used, don't
     * use a ColorUIResource because UIResource is considered as a setting set by the L&F and any L&F can choose to
     * ignore it.
     *
     * @param state the button state. Please refer to {@link com.jidesoft.plaf.basic.ThemePainter} to see the list of
     *              available states.
     * @param color the background color
     */
    @Override
    public void setForegroundOfState(int state, Color color) {
        switch (state) {
            case ThemePainter.STATE_DEFAULT:
                setDefaultForeground(color);
                break;
            case ThemePainter.STATE_ROLLOVER:
                setRolloverForeground(color);
                break;
            case ThemePainter.STATE_SELECTED:
                setSelectedForeground(color);
                break;
            case ThemePainter.STATE_PRESSED:
                setPressedForeground(color);
                break;
        }
    }

    /**
     * Clicks on the button part of the <code>JideSplitButton</code>.
     */
    @Override
    public void doClick() {
        Action action = getActionMap().get("pressed");
        if (action != null) {
            action.actionPerformed(new ActionEvent(this, 0, ""));
        }
    }

    /**
     * Clicks on the drop down menu part of the <code>JideSplitButton</code>.
     */
    public void doClickOnMenu() {
        Action action = getActionMap().get("downPressed");
        if (action != null) {
            action.actionPerformed(new ActionEvent(this, 0, ""));
        }
    }

    @Override
    protected void configurePropertiesFromAction(Action action) {
        super.configurePropertiesFromAction(action);
        setButtonEnabled(isSplitButtonEnabled(action));
        setIconFromAction(action);
    }

    /**
     * By default, we will use large icon instead of small icon in the JMenuItem. You could override this method to set
     * your own icon size.
     *
     * @param action the action.
     */
    protected void setIconFromAction(Action action) {
        Icon icon = null;
        if (action != null) {
            icon = SystemInfo.isJdk6Above() && !(getParent() instanceof JPopupMenu) ? (Icon) action.getValue(Action.LARGE_ICON_KEY) : null;
            if (icon == null) {
                icon = (Icon) action.getValue(Action.SMALL_ICON);
            }
        }
        setIcon(icon);
    }

    @Override
    protected void actionPropertyChanged(Action action, String propertyName) {
        super.actionPropertyChanged(action, propertyName);

        if (ACTION_PROPERTY_SPLIT_BUTTON_ENABLED.equals(propertyName) || "enabled".equals(propertyName)) {
            setButtonEnabled(isSplitButtonEnabled(action));
        }
        else if (Action.SMALL_ICON.equals(propertyName)) {
            setIconFromAction(action);
        }
        else if (SystemInfo.isJdk6Above() && Action.LARGE_ICON_KEY.equals(propertyName)) {
            setIconFromAction(action);
        }
    }

    /**
     * Get if the split button is enable from the property stored inside the action.
     *
     * @param action the action
     * @return true if the split button is enabled. Otherwise false.
     */
    public static boolean isSplitButtonEnabled(Action action) {
        if (action == null) {
            return true;
        }
        else {
            Object value = action.getValue(ACTION_PROPERTY_SPLIT_BUTTON_ENABLED);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            else {
                return action.isEnabled();
            }
        }
    }

    void largeIconChanged(Action a) {
    }

    void smallIconChanged(Action a) {
    }
}
