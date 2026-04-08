// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.border.Border;
import java.awt.*;

final class SimpleFocusBorder implements Border {
    @Override
    public void paintBorder(@Nullable Component c, @Nullable Graphics g, int x, int y, int width, int height) {
        if (c != null && c.hasFocus() && g instanceof Graphics2D g2d) {
            DarculaUIUtil.paintFocusBorder(g2d, width, height, 0f, true);
        }
    }

    @Override
    public @Nonnull Insets getBorderInsets(@Nullable Component c) {
        float bw = DarculaUIUtil.BW.getFloat();
        float lw = DarculaUIUtil.LW.getFloat();
        int insets = (int) (bw + lw);
        return JBUI.insets(insets);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
