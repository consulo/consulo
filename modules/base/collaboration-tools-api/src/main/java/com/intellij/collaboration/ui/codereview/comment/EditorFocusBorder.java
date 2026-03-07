// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.comment;

import consulo.ui.ex.awt.ErrorBorderCapable;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

// can't use DarculaTextBorderNew because of nested focus and because it's a UIResource
final class EditorFocusBorder implements Border, ErrorBorderCapable {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        boolean hasFocus = UIUtil.isFocusAncestor(c);

        Rectangle rect = new Rectangle(x, y, width, height);
        int maxBorderThickness = DarculaUIUtil.BW.get();
        JBInsets.removeFrom(rect, JBInsets.create(maxBorderThickness, maxBorderThickness));
        DarculaNewUIUtil.fillInsideComponentBorder(g, rect, c.getBackground());
        DarculaNewUIUtil.paintComponentBorder(g, rect, DarculaUIUtil.getOutline((JComponent) c), hasFocus, c.isEnabled());
    }

    // the true vertical inset would be 7, but Editor has 1px padding above and below the line
    @Override
    public Insets getBorderInsets(Component c) {
        return JBInsets.create(6, 10);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
