// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import com.intellij.ui.RoundedLineBorder;
import com.intellij.util.ui.JBUI;
import jakarta.annotation.Nonnull;

import java.awt.*;

final class FocusAwareRoundedLineBorder extends RoundedLineBorder {
    FocusAwareRoundedLineBorder(@Nonnull Color color) {
        this(color, 1, 1);
    }

    FocusAwareRoundedLineBorder(@Nonnull Color color, int arcDiameter) {
        this(color, arcDiameter, 1);
    }

    FocusAwareRoundedLineBorder(@Nonnull Color color, int arcDiameter, int thickness) {
        super(color, arcDiameter, thickness);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        super.paintBorder(c, g, x, y, width, height);
    }

    @Override
    protected Color getColorToDraw(@Nonnull Component c) {
        if (c.hasFocus()) {
            return JBUI.CurrentTheme.Focus.focusColor();
        }
        else {
            return super.getColorToDraw(c);
        }
    }
}
