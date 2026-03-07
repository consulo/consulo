// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.timeline;

import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUIScale;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.geom.Line2D;

final class VerticalRoundedLineComponent extends JComponent {
    private final int lineWidth;

    VerticalRoundedLineComponent(int lineWidth) {
        this.lineWidth = lineWidth;
        updateUI();
    }

    int getLineWidth() {
        return lineWidth;
    }

    @Override
    public void updateUI() {
        setUI(new VerticalRoundedLineUI());
    }

    private static final class VerticalRoundedLineUI extends ComponentUI {
        @Override
        public void installUI(@Nonnull JComponent c) {
            c.setOpaque(false);
        }

        @Override
        public void paint(@Nonnull Graphics g, @Nonnull JComponent c) {
            VerticalRoundedLineComponent comp = (VerticalRoundedLineComponent) c;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(c.getForeground());

            int width = JBUIScale.scale(comp.getLineWidth());
            g2.setStroke(new BasicStroke(width / 2f + 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));

            int center = width / 2;
            g2.draw(new Line2D.Float(center, center, center, c.getHeight() - center));
        }

        @Override
        public Dimension getMinimumSize(@Nonnull JComponent c) {
            return getPreferredSize(c);
        }

        @Override
        public Dimension getPreferredSize(@Nonnull JComponent c) {
            VerticalRoundedLineComponent comp = (VerticalRoundedLineComponent) c;
            return new JBDimension(comp.getLineWidth(), 0);
        }

        @Override
        public Dimension getMaximumSize(@Nonnull JComponent c) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
    }
}
