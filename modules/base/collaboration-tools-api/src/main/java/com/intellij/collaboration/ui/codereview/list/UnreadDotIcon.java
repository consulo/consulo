// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

// Taken from com.jetbrains.rd.platform.codeWithMe.control.icons.CircleIcon
final class UnreadDotIcon implements Icon {
    static final int DEFAULT_UNSCALED_DIAMETER = 6;

    private static final Color oldColor = JBColor.namedColor("Review.Notification.Blue", 0x40B6E0);
    private static final Color newColor = JBColor.namedColor("Review.Notification.Blue", new JBColor(0x3574F0, 0x548AF7));

    private final int unscaledDiameter;
    private Color cachedColor;

    UnreadDotIcon() {
        this(DEFAULT_UNSCALED_DIAMETER);
    }

    UnreadDotIcon(int unscaledDiameter) {
        this.unscaledDiameter = unscaledDiameter;
    }

    private @Nonnull Color getColor() {
        if (cachedColor == null) {
            cachedColor = NewUI.isEnabled() ? newColor : oldColor;
        }
        return cachedColor;
    }

    private int getDiameter() {
        return JBUI.scale(unscaledDiameter);
    }

    @Override
    public int getIconHeight() {
        return getDiameter();
    }

    @Override
    public int getIconWidth() {
        return getIconHeight();
    }

    @Override
    public void paintIcon(@Nullable Component c, @Nonnull Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float cx = (float) x;
        float cy = (float) y;
        float cd = (float) getDiameter();

        Ellipse2D.Float circle = new Ellipse2D.Float(cx, cy, cd, cd);
        g2d.setColor(getColor());
        g2d.fill(circle);

        g2d.dispose();
    }
}
