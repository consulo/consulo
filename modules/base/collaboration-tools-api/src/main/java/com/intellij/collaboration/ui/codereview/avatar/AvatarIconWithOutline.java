// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.avatar;

import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * @param avatarIcon Avatar icon without any outlines, but scaled.
 */
final class AvatarIconWithOutline implements Icon {
    private final Icon avatarIcon;
    private final Color outlineColor;

    AvatarIconWithOutline(@Nonnull Icon avatarIcon, @Nonnull Color outlineColor) {
        this.avatarIcon = avatarIcon;
        this.outlineColor = outlineColor;
    }

    @Override
    public int getIconWidth() {
        return avatarIcon.getIconWidth() + 2 * JBUI.scale(CodeReviewAvatarUtils.INNER_WIDTH + CodeReviewAvatarUtils.OUTLINE_WIDTH);
    }

    @Override
    public int getIconHeight() {
        return avatarIcon.getIconHeight() + 2 * JBUI.scale(CodeReviewAvatarUtils.INNER_WIDTH + CodeReviewAvatarUtils.OUTLINE_WIDTH);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.translate(x, y);

        float width = getIconWidth();
        float height = getIconHeight();

        int innerIconOffset = JBUI.scale(CodeReviewAvatarUtils.INNER_WIDTH + CodeReviewAvatarUtils.OUTLINE_WIDTH);
        float outlineThickness = JBUIScale.scale((float)CodeReviewAvatarUtils.OUTLINE_WIDTH);

        try {
            GraphicsUtil.setupRoundedBorderAntialiasing(g2d);

            Path2D.Float border = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            border.append(new Ellipse2D.Float(0f, 0f, width, height), false);

            float innerWidth = width - outlineThickness * 2;
            float innerHeight = height - outlineThickness * 2;
            border.append(new Ellipse2D.Float(outlineThickness, outlineThickness, innerWidth, innerHeight), false);

            g2d.setColor(outlineColor);
            g2d.fill(border);

            avatarIcon.paintIcon(c, g2d, innerIconOffset, innerIconOffset);
        }
        finally {
            g2d.dispose();
        }
    }
}
