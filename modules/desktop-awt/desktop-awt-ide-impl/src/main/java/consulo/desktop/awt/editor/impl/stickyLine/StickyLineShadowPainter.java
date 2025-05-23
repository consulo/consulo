// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.stickyLine;

import consulo.application.util.registry.Registry;

import java.awt.*;

public class StickyLineShadowPainter {
    private boolean isDarkColorScheme;

    // shadow settings
    private static final double SHADOW_HEIGHT_FACTOR_LIGHT = 0.17;
    private static final double SHADOW_HEIGHT_FACTOR_DARK = 0.25;
    private static final int SHADOW_COLOR_ALPHA_LIGHT = 8;
    private static final int SHADOW_COLOR_ALPHA_DARK = 32;
    private static final Color SHADOW_COLOR_LIGHT = new Color(0, 0, 0, SHADOW_COLOR_ALPHA_LIGHT);
    private static final Color SHADOW_COLOR_DARK = new Color(0, 0, 0, SHADOW_COLOR_ALPHA_DARK);
    private static final Color SHADOW_COLOR_TRANSPARENT = new Color(0, 0, 0, 0);

    public StickyLineShadowPainter() {
        this(false);
    }

    public StickyLineShadowPainter(boolean isDarkColorScheme) {
        this.isDarkColorScheme = isDarkColorScheme;
    }

    public void setDarkColorScheme(boolean darkColorScheme) {
        isDarkColorScheme = darkColorScheme;
    }

    public void paintShadow(Graphics2D g, int panelHeight, int panelWidth, int lineHeight) {
        if (isEnabled()) {
            int shadowHeight = shadowHeight(lineHeight);
            Paint prevPaint = g.getPaint();
            g.setClip(0, 0, panelWidth, panelHeight + shadowHeight);
            g.translate(0, panelHeight);
            g.setPaint(new GradientPaint(
                0.0f,
                0.0f,
                shadowColor(),
                0.0f,
                (float) shadowHeight,
                SHADOW_COLOR_TRANSPARENT
            ));
            g.fillRect(0, 0, panelWidth, shadowHeight);
            g.setPaint(prevPaint);
            g.translate(0, -panelHeight);
            g.setClip(0, 0, panelWidth, panelHeight);
        }
    }

    private int shadowHeight(int lineHeight) {
        double factor = isDarkColorScheme ? SHADOW_HEIGHT_FACTOR_DARK : SHADOW_HEIGHT_FACTOR_LIGHT;
        return (int) (lineHeight * factor);
    }

    private Color shadowColor() {
        return isDarkColorScheme ? SHADOW_COLOR_DARK : SHADOW_COLOR_LIGHT;
    }

    private boolean isEnabled() {
        return Registry.is("editor.show.sticky.lines.shadow", true);
    }
}
