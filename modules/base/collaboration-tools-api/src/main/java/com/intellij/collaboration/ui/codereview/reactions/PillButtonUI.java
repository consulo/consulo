// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import com.intellij.collaboration.ui.util.CodeReviewColorUtil;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

final class PillButtonUI extends BasicButtonUI {
    private static final String PROPERTY_PREFIX = "PillButton.";

    @Override
    protected String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        LookAndFeel.installProperty(b, "opaque", false);
        LookAndFeel.installProperty(b, "rolloverEnabled", true);
        LookAndFeel.installProperty(b, "iconTextGap", 4);
        if (b.getFont() == null || b.getFont() instanceof UIResource) {
            b.setFont(UIUtil.getLabelFont());
        }
        b.setBackground(CodeReviewColorUtil.Reaction.background);
        if (b.getBorder() == null || b.getBorder() instanceof UIResource) {
            b.setBorder(new UIResourceBorder(new PillButtonBorder(null)));
        }
        if (!(b instanceof PillButton pb)) {
            return;
        }
        if (pb.getMargin() == null || pb.getMargin() instanceof UIResource) {
            pb.setMargin(JBInsets.create(1, 6));
        }
        pb.setRolloverBackground(CodeReviewColorUtil.Reaction.backgroundHovered);
        pb.setPressedBackground(CodeReviewColorUtil.Reaction.backgroundPressed);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension dim = super.getPreferredSize(c);
        if (c instanceof AbstractButton btn) {
            JBInsets.addTo(dim, btn.getMargin());
        }
        return dim;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (!(c instanceof PillButton button)) {
            return;
        }
        if (button.isContentAreaFilled()) {
            paintBackground(g, button);
        }
        super.paint(g, c);
    }

    private void paintBackground(Graphics g, PillButton c) {
        Rectangle r = new Rectangle(c.getSize());
        JBInsets.removeFrom(r, c.getInsets());
        Graphics2D g2d = (Graphics2D) g.create(r.x, r.y, r.width, r.height);
        try {
            GraphicsUtil.setupAAPainting(g2d);
            float arc = r.height;
            g2d.setColor(getBackgroundColor(c));
            g2d.fill(new RoundRectangle2D.Float(0f, 0f, r.width, r.height, arc, arc));
        }
        finally {
            g2d.dispose();
        }
    }

    private @Nullable Color getBackgroundColor(@Nonnull PillButton c) {
        ButtonModel model = c.getModel();
        Color background = null;
        if (c.isRolloverEnabled() && c.isEnabled()) {
            if (model.isRollover()) {
                background = c.getRolloverBackground();
            }
            else if (model.isArmed() && model.isPressed()) {
                background = c.getPressedBackground();
            }
        }
        return background != null ? background : c.getBackground();
    }
}
