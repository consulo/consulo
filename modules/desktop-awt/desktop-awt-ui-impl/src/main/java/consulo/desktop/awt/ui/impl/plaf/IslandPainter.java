/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.desktop.awt.ui.impl.plaf;

import consulo.application.util.registry.Registry;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public final class IslandPainter {
    private IslandPainter() {
    }

    public static boolean isEnabled() {
        return Registry.is("ide.islands.ui", true);
    }

    public static Color getMainBackground() {
        return JBColor.namedColor("MainWindow.background", UIUtil.getPanelBackground());
    }

    public static int getGap() {
        return JBUI.scale(JBUI.getInt("Island.borderWidth", 6) / 2);
    }

    public static void paint(JComponent component, Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            doPaintIslandArea(component, g);
        }
        finally {
            g.dispose();
        }
    }

    private static void doPaintIslandArea(JComponent component, Graphics2D g) {
        int width = component.getWidth();
        int height = component.getHeight();
        int arcValue = JBUI.getInt("Island.arc", 20);
        Color borderColor = JBColor.namedColor("Island.borderColor", getMainBackground());

        if (arcValue == 0) {
            g.setColor(borderColor);
            g.drawRect(0, 0, width, height);
            return;
        }

        int offset = getGap();
        int offset2 = offset * 2;
        float offsetF = offset;
        float widthF = width;
        float heightF = height;
        float arcSizeF = JBUI.scale(arcValue / 2f);

        g.setColor(getMainBackground());

        g.fillRect(0, 0, width, offset);
        g.fillRect(0, height - offset, width, offset);
        g.fillRect(0, offset, offset, height - offset2);
        g.fillRect(width - offset, offset, offset, height - offset2);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        Path2D.Float path = new Path2D.Float();
        path.moveTo(offsetF, offsetF);
        path.lineTo(arcSizeF + offsetF, offsetF);
        path.quadTo(offsetF, offsetF, offsetF, arcSizeF + offsetF);
        path.closePath();
        g.fill(path);

        path.reset();
        path.moveTo(widthF - arcSizeF - offsetF, offsetF);
        path.quadTo(widthF - offsetF, offsetF, widthF - offsetF, arcSizeF + offsetF);
        path.lineTo(widthF - offsetF, offsetF);
        path.closePath();
        g.fill(path);

        path.reset();
        path.moveTo(offsetF, heightF - arcSizeF - offsetF);
        path.quadTo(offsetF, heightF - offsetF, arcSizeF + offsetF, heightF - offsetF);
        path.lineTo(offsetF, heightF - offsetF);
        path.closePath();
        g.fill(path);

        path.reset();
        path.moveTo(widthF - arcSizeF - offsetF, heightF - offsetF);
        path.quadTo(widthF - offsetF, heightF - offsetF, widthF - offsetF, heightF - arcSizeF - offsetF);
        path.lineTo(widthF - offsetF, heightF - offsetF);
        path.closePath();
        g.fill(path);

        if (borderColor.equals(getMainBackground())) {
            return;
        }

        int arcSize = JBUI.scale(arcValue);
        int halfArcSize = JBUI.scale(JBUI.getInt("Island.borderArcLength", 14));

        g.setColor(borderColor);

        g.drawLine(halfArcSize, offset, width - halfArcSize, offset);
        g.drawLine(halfArcSize, height - offset, width - halfArcSize, height - offset);
        g.drawLine(offset, halfArcSize, offset, height - halfArcSize);
        g.drawLine(width - offset, halfArcSize, width - offset, height - halfArcSize);

        g.drawArc(offset, offset, arcSize, arcSize, 90, 90);
        g.drawArc(width - arcSize - offset, offset, arcSize, arcSize, 0, 90);
        g.drawArc(width - arcSize - offset, height - arcSize - offset, arcSize, arcSize, 270, 90);
        g.drawArc(offset, height - arcSize - offset, arcSize, arcSize, 180, 90);
    }
}
