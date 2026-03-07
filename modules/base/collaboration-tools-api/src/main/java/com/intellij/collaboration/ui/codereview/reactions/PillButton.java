// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * A custom button with round corners and hovered/pressed state
 */
final class PillButton extends AbstractButton {
    static final String ROLLOVER_BACKGROUND_PROPERTY = "rolloverBackground";
    static final String PRESSED_BACKGROUND_PROPERTY = "pressedBackground";

    private Color rolloverBackground;
    private Color pressedBackground;

    PillButton() {
        setModel(new DefaultButtonModel());
        setUI(new PillButtonUI());

        setAlignmentX(LEFT_ALIGNMENT);
        setAlignmentY(CENTER_ALIGNMENT);
    }

    @Nullable
    Color getRolloverBackground() {
        return rolloverBackground;
    }

    void setRolloverBackground(@Nullable Color rolloverBackground) {
        Color old = this.rolloverBackground;
        this.rolloverBackground = rolloverBackground;
        firePropertyChange(ROLLOVER_BACKGROUND_PROPERTY, old, rolloverBackground);
    }

    @Nullable
    Color getPressedBackground() {
        return pressedBackground;
    }

    void setPressedBackground(@Nullable Color pressedBackground) {
        Color old = this.pressedBackground;
        this.pressedBackground = pressedBackground;
        firePropertyChange(PRESSED_BACKGROUND_PROPERTY, old, pressedBackground);
    }
}
