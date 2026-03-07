// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nullable;

import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Similar to {@link com.intellij.ui.RoundedLineBorder}, but uses component height as arc and uses component background color by default.
 */
final class PillButtonBorder extends AbstractBorder {
    private final @Nullable Color color;
    private final int thickness = 1;

    PillButtonBorder(@Nullable Color color) {
        this.color = color;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        if (!(c instanceof PillButton)) {
            return insets;
        }
        insets.set(thickness, thickness, thickness, thickness);
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create(x, y, width, height);
        try {
            Color borderColor = color != null ? color : c.getBackground();
            if (borderColor == null) {
                return;
            }
            g2d.setColor(borderColor);
            GraphicsUtil.setupRoundedBorderAntialiasing(g2d);
            float arc = height;
            Path2D border = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            border.append(new RoundRectangle2D.Float(0f, 0f, width, height, arc, arc), false);

            int coordinateDelta = thickness;
            int sizeDelta = thickness * 2;
            border.append(
                new RoundRectangle2D.Float(
                    coordinateDelta,
                    coordinateDelta,
                    width - sizeDelta,
                    height - sizeDelta,
                    arc - sizeDelta,
                    arc - sizeDelta
                ),
                false
            );
            g2d.fill(border);
        }
        finally {
            g2d.dispose();
        }
    }
}
