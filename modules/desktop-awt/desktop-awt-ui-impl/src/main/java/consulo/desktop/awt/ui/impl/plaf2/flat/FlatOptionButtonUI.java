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
package consulo.desktop.awt.ui.impl.plaf2.flat;

import com.formdev.flatlaf.ui.FlatArrowButton;
import com.formdev.flatlaf.ui.FlatButtonUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class FlatOptionButtonUI extends FlatButtonUI {
    public static final String UI_CLASS_ID = "OptionButtonUI";

    public static final String HAS_OPTIONS = "OptionButton.hasOptions";

    private static final int DEFAULT_ARROW_AREA_WIDTH = 20;

    private static final float DEFAULT_SEPARATOR_ALPHA = 0.4f;

    public static ComponentUI createUI(JComponent c) {
        return new FlatOptionButtonUI();
    }

    protected FlatOptionButtonUI() {
        super(false);
    }

    public static boolean hasOptions(Component c) {
        return c instanceof JComponent component && Boolean.TRUE.equals(component.getClientProperty(HAS_OPTIONS));
    }

    public static int arrowAreaWidth() {
        return UIScale.scale(FlatUIUtils.getUIInt("OptionButton.arrowAreaWidth", DEFAULT_ARROW_AREA_WIDTH));
    }

    public static boolean isOverArrow(JComponent c, int x) {
        if (!hasOptions(c) || !(c.getBorder() instanceof FlatOptionButtonBorder border)) {
            return false;
        }

        Rectangle area = arrowArea(c, border);
        return x >= area.x && x < area.x + area.width;
    }

    private static Rectangle arrowArea(JComponent c, FlatOptionButtonBorder border) {
        int outer = border.getOuterWidth(c);
        int width = arrowAreaWidth();
        int x = c.getComponentOrientation().isLeftToRight() ? c.getWidth() - outer - width : outer;
        return new Rectangle(x, outer, width, Math.max(0, c.getHeight() - outer * 2));
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);

        Border border = b.getBorder();
        if (border == null || border instanceof UIResource) {
            b.setBorder(new FlatOptionButtonBorder());
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        if (!hasOptions(c) || !(c.getBorder() instanceof FlatOptionButtonBorder border)) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FlatUIUtils.setRenderingHints(g2);

            Rectangle area = arrowArea(c, border);
            boolean ltr = c.getComponentOrientation().isLeftToRight();

            g2.setPaint(separatorPaint(c, border));
            float lineWidth = UIScale.scale(1f);
            float separatorX = ltr ? area.x : area.x + area.width - lineWidth;
            g2.fill(new Rectangle2D.Float(separatorX, area.y, lineWidth, area.height));

            g2.setColor(getForeground(c));
            boolean chevron = FlatUIUtils.isChevron(UIManager.getString("Component.arrowType"));
            FlatUIUtils.paintArrow(g2, area.x, area.y, area.width, area.height, SwingConstants.SOUTH, chevron,
                FlatArrowButton.DEFAULT_ARROW_WIDTH, 1f, 0, 0);
        }
        finally {
            g2.dispose();
        }
    }

    private Paint separatorPaint(JComponent c, FlatOptionButtonBorder border) {
        if (c instanceof JButton button && button.isDefaultButton()) {
            Color color = UIManager.getColor("OptionButton.default.separatorColor");
            if (color != null) {
                return color;
            }

            Color foreground = getForeground(c);
            return new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(),
                Math.round(foreground.getAlpha() * DEFAULT_SEPARATOR_ALPHA));
        }

        Color color = UIManager.getColor("OptionButton.separatorColor");
        return color != null ? color : border.getSeparatorPaint(c);
    }
}
