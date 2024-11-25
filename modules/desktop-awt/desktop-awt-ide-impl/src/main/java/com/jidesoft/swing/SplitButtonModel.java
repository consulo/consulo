/*
 * @(#)SplitButtonModel.java 2/18/2005
 *
 * Copyright 2002 - 2005 JIDE Software Inc. All rights reserved.
 */
package com.jidesoft.swing;

import javax.swing.*;

/**
 * SplitButtonModel is for JideSplitButton. Because SplitButton has two parts - button part and dropdown menu part.
 * setSelected() and isSelected() is used by dropdown menu part. However in order to support togglable button,
 * we have to make the button part selected or not selected. That's why we create SplitButtonModel and added two
 * methods for the selection of button part.
 */
public interface SplitButtonModel extends ButtonModel {
    /**
     * Selects or deselects the button.
     *
     * @param b true selects the button,
     *          false deselects the button.
     */
    void setButtonSelected(boolean b);

    /**
     * Indicates if the button has been selected. Only needed for
     * certain types of buttons - such as radio buttons and check boxes.
     *
     * @return true if the button is selected
     */
    boolean isButtonSelected();

    /**
     * Enables or disables the button.
     *
     * @param b true enables the button,
     *          false disables the button.
     */
    void setButtonEnabled(boolean b);

    /**
     * Indicates if the button is enabled.
     *
     * @return true if the button is enabled.
     */
    boolean isButtonEnabled();

    /**
     * Sets the button part of the JideSplitButton as rollover.
     *
     * @param b true set the button as rollover,
     *          false set the button as not rollover
     */
    void setButtonRollover(boolean b);

    /**
     * Indicates if the button part of the JideSplitButton is rollover.
     *
     * @return true if the button is rollover
     */
    boolean isButtonRollover();
}