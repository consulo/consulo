/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.ui.impl.tabs;

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.paint.RectanglePainter2D;
import consulo.ui.ex.awt.util.ColorUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
public final class TabPainter {
    private TabPainter() {
    }

    public static void paintTabSelection(Graphics2D g2d, int x, int y, int width, int height, boolean focused) {
        paintTabSelection(g2d, x, y, width, height, focused, SwingConstants.TOP);
    }

    public static void paintTabSelection(Graphics2D g2d, int x, int y, int width, int height, boolean focused, int tabPlacement) {
        paintTabSelection(g2d, x, y, width, height, null, focused, tabPlacement);
    }

    public static void paintTabSelection(Graphics2D g2d, int x, int y, int width, int height, @Nullable Color color, boolean focused, int tabPlacement) {
        Color fill = color;
        if (fill == null) {
            fill = focused ? null : UIManager.getColor("TabbedPane.inactiveSelectedBackground");
        }
        if (fill == null) {
            fill = UIManager.getColor("TabbedPane.selectedBackground");
        }
        if (fill == null) {
            fill = JBColor.border();
        }

        paintTab(g2d, x, y, width, height, fill, tabPlacement);
    }

    public static void paintTabHover(Graphics2D g2d, int x, int y, int width, int height) {
        paintTabHover(g2d, x, y, width, height, SwingConstants.TOP);
    }

    public static void paintTabHover(Graphics2D g2d, int x, int y, int width, int height, int tabPlacement) {
        Color color = UIManager.getColor("TabbedPane.hoverColor");
        if (color == null) {
            return;
        }

        paintTab(g2d, x, y, width, height, color, tabPlacement);
    }

    public static Color getTabBorderColor(Color fill) {
        return ColorUtil.isDark(fill) ? ColorUtil.shift(fill, 1.2) : ColorUtil.shift(fill, 0.9);
    }

    public static void paintTab(Graphics2D g2d, int x, int y, int width, int height, Color color, int tabPlacement) {
        paintTab(g2d, x, y, width, height, color, tabPlacement, true);
    }

    public static void paintTab(Graphics2D g2d, int x, int y, int width, int height, Color color, int tabPlacement, boolean withBorder) {
        Insets insets = UIManager.getInsets("TabbedPane.selectedInsets");

        paintTab(g2d, x, y, width, height, color, insets == null ? null : rotateInsets(insets, tabPlacement), withBorder);
    }

    public static void paintTab(Graphics2D g2d, int x, int y, int width, int height, Color color, @Nullable Insets insets, boolean withBorder) {
        int fillX = x;
        int fillY = y;
        int fillWidth = width;
        int fillHeight = height;

        if (insets != null) {
            fillX += JBUI.scale(insets.left);
            fillY += JBUI.scale(insets.top);
            fillWidth -= JBUI.scale(insets.left + insets.right);
            fillHeight -= JBUI.scale(insets.top + insets.bottom);
        }

        g2d.setColor(color);

        int arc = JBUI.scale(UIManager.getInt("TabbedPane.tabArc"));
        if (arc > 0) {
            RectanglePainter2D.FILL.paint(g2d, fillX, fillY, fillWidth, fillHeight, (double) arc);
        }
        else {
            RectanglePainter2D.FILL.paint(g2d, fillX, fillY, fillWidth, fillHeight);
        }

        if (withBorder) {
            g2d.setColor(getTabBorderColor(color));
            if (arc > 0) {
                RectanglePainter2D.DRAW.paint(g2d, fillX, fillY, fillWidth, fillHeight, (double) arc);
            }
            else {
                RectanglePainter2D.DRAW.paint(g2d, fillX, fillY, fillWidth, fillHeight);
            }
        }
    }

    private static Insets rotateInsets(Insets insets, int tabPlacement) {
        switch (tabPlacement) {
            case SwingConstants.LEFT:
                return new Insets(insets.left, insets.top, insets.right, insets.bottom);
            case SwingConstants.BOTTOM:
                return new Insets(insets.bottom, insets.left, insets.top, insets.right);
            case SwingConstants.RIGHT:
                return new Insets(insets.left, insets.bottom, insets.right, insets.top);
            default:
                return insets;
        }
    }
}
