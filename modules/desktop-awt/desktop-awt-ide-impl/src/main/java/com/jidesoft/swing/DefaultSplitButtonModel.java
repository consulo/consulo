/*
 * @(#)DefaultSplitButtonModel.java 2/17/2005
 *
 * Copyright 2002 - 2005 JIDE Software Inc. All rights reserved.
 */
package com.jidesoft.swing;

import javax.swing.*;
import java.awt.event.ItemEvent;

/**
 *
 */
public class DefaultSplitButtonModel extends DefaultButtonModel implements SplitButtonModel {
    /**
     * Indicates that the button part of the split button has been selected.
     */
    public static final int BUTTON_SELECTED = 1 << 6;

    /**
     * Indicates that the button part of the split button is enabled or disabled.
     */
    public static final int BUTTON_ENABLED = 1 << 7;

    /**
     * Indicates that the button part of the split button is rollover.
     */
    public static final int BUTTON_ROLLOVER = 1 << 8;


    public DefaultSplitButtonModel() {
        setButtonEnabled(true);
    }

    /**
     * Selects or deselects the button part of the JideSplitButton.
     *
     * @param b true selects the button, false deselects the button
     */
    public void setButtonSelected(boolean b) {
        if (this.isButtonSelected() == b) {
            return;
        }

        if (b) {
            stateMask |= BUTTON_SELECTED;
        }
        else {
            stateMask &= ~BUTTON_SELECTED;
        }

        fireItemStateChanged(new ItemEvent(this,
            ItemEvent.ITEM_STATE_CHANGED,
            this,
            b ? ItemEvent.SELECTED : ItemEvent.DESELECTED));

        fireStateChanged();

    }

    /**
     * Indicates if the button part of the JideSplitButton has been selected.
     *
     * @return true if the button is selected
     */
    public boolean isButtonSelected() {
        return (stateMask & BUTTON_SELECTED) != 0;
    }

    /**
     * Selects or deselects the button part of the JideSplitButton.
     *
     * @param b true selects the button, false deselects the button
     */
    public void setButtonEnabled(boolean b) {
        if (this.isButtonEnabled() == b) {
            return;
        }

        if (b) {
            stateMask |= BUTTON_ENABLED;
        }
        else {
            stateMask &= ~BUTTON_ENABLED;
        }

        fireStateChanged();

    }

    /**
     * Indicates if the button part of the JideSplitButton has been enabled.
     *
     * @return true if the button is enabled
     */
    public boolean isButtonEnabled() {
        return (stateMask & BUTTON_ENABLED) != 0;
    }

    /**
     * Sets the button part of the JideSplitButton as rollover.
     *
     * @param b true set the button as rollover, false set the button as not rollover
     */
    public void setButtonRollover(boolean b) {
        if (this.isButtonRollover() == b) {
            return;
        }

        if (b) {
            stateMask |= BUTTON_ROLLOVER;
        }
        else {
            stateMask &= ~BUTTON_ROLLOVER;
        }

        fireStateChanged();
    }

    /**
     * Indicates if the button part of the JideSplitButton is rollover.
     *
     * @return true if the button is rollover
     */
    public boolean isButtonRollover() {
        return (stateMask & BUTTON_ROLLOVER) != 0;
    }

    @Override
    public void setRollover(boolean b) {
        super.setRollover(b);
        if (!b) {
            setButtonRollover(false);
        }
    }
}