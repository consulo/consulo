/*
 * @(#)BackgroundSupport.java 1/15/2007
 *
 * Copyright 2002 - 2007 JIDE Software Inc. All rights reserved.
 */

package com.jidesoft.swing;

import java.awt.*;

/**
 * A component should implement this interface if it supports various background and foreground for different states.
 * Those states are defined in {@link com.jidesoft.plaf.basic.ThemePainter}.
 * <p/>
 * For components that implements this interface, you can use the methods to change its background or foreground for
 * different states such as rollover state, selected state or pressed state. JideButton and JideSplitButton are two
 * classes that support this.
 * <p/>
 * Please note, not all L&Fs support this. Vsnet and Office 2003 style support it but Xerto and Eclipse style don't.
 */
public interface ComponentStateSupport {
    /**
     * Gets the background for different states. The states are defined in ThemePainter as constants. Not all states are
     * supported by all components. If the state is not supported or background is never set, it will return null.
     * <p/>
     * Please note, each L&F will have its own way to paint the different backgrounds. This method allows you to
     * customize it for each component to use a different background. So if you want the background to be used, don't
     * use a ColorUIResource because UIResource is considered as a setting set by the L&F and any L&F can choose to
     * ignore it.
     *
     * @param state the button state. Valid values are ThemePainter.STATE_DEFAULT, ThemePainter.STATE_ROLLOVER,
     *              ThemePainter.STATE_SELECTED and ThemePainter.STATE_PRESSED.
     * @return the background for different states.
     */
    Color getBackgroundOfState(int state);

    /**
     * Sets the background for different states.
     *
     * @param state the button state. Valid values are ThemePainter.STATE_DEFAULT, ThemePainter.STATE_ROLLOVER,
     *              ThemePainter.STATE_SELECTED and ThemePainter.STATE_PRESSED.
     * @param color the new background for the state.
     */
    void setBackgroundOfState(int state, Color color);

    /**
     * Gets the foreground for different states. The states are defined in ThemePainter as constants. Not all states are
     * supported by all components. If the state is not supported or foreground is never set, it will return null.
     * <p/>
     * Please note, each L&F will have its own way to paint the different foregrounds. This method allows you to
     * customize it for each component to use a different foreground. So if you want the foreground to be used, don't
     * use a ColorUIResource because UIResource is considered as a setting set by the L&F and any L&F can choose to
     * ignore it.
     *
     * @param state the button state. Valid values are ThemePainter.STATE_DEFAULT, ThemePainter.STATE_ROLLOVER,
     *              ThemePainter.STATE_SELECTED and ThemePainter.STATE_PRESSED.
     * @return the foreground for different states.
     */
    Color getForegroundOfState(int state);

    /**
     * Sets the foreground for different states.
     *
     * @param state the button state. Valid values are ThemePainter.STATE_DEFAULT, ThemePainter.STATE_ROLLOVER,
     *              ThemePainter.STATE_SELECTED and ThemePainter.STATE_PRESSED.
     * @param color the new foreground for the state.
     */
    void setForegroundOfState(int state, Color color);
}