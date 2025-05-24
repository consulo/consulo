/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.ui;

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.paint.RectanglePainter2D;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 22-Jun-17
 */
public class ToolwindowPaintUtil {
    public static void drawHeader(Graphics g, int x, int width, int height, boolean active, boolean drawTopLine) {
        drawHeader(g, x, width, height, active, false, drawTopLine, true);
    }

    public static void drawHeader(Graphics g, int x, int width, int height, boolean active, boolean toolWindow, boolean drawTopLine, boolean drawBottomLine) {
        g.setColor(UIUtil.getPanelBackground());
        g.fillRect(x, 0, width, height);

        if (active) {
            g.setColor(getActiveToolWindowHeaderColor());
            g.fillRect(x, 0, width, height);
        }

        g.setColor(UIUtil.getBorderColor());
        if (drawTopLine) {
            g.drawLine(x, 0, width, 0);
        }
        if (drawBottomLine) {
            g.drawLine(x, height - JBUI.scale(1), width, height - JBUI.scale(1));
        }
    }

    public static void paintUnderlineColor(Graphics2D g2d, int x, int y, int width, int height, boolean focused) {
        int underlineSize = JBUI.scale(3);

        g2d.setColor(focused
            ? getFocusColor()
            : UIManager.getColor("TabbedPane.focusColor"));

        int arc = UIManager.getInt("Component.arc");
        if (arc != 0) {
            RectanglePainter2D.FILL.paint(g2d, x, y + height - underlineSize, width, underlineSize, 3d);
        } else {
            RectanglePainter2D.FILL.paint(g2d, x, y + height - underlineSize, width, underlineSize);
        }
    }

    public static Color getFocusColor() {
        Color color = UIManager.getColor("TabbedPane.underlineColor");
        if (color == null) {
            color = UIManager.getColor("Component.focusColor");
        }

        if (color == null) {
            color = JBColor.BLUE;
        }
        return color;
    }

    public static Color getActiveToolWindowHeaderColor() {
        Color color = UIManager.getColor("TabbedPane.focusColor");
        if (color == null) {
            color = UIUtil.getPanelBackground();
        }
        return color;
    }
}
