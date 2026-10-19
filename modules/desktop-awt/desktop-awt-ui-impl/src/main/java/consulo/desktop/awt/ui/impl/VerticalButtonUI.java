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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.plaf2.BasicStripeButtonUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.StringUtil;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
class VerticalButtonUI extends BasicButtonUI {
    private static final int PADDING = 6;
    private static final int GAP = 4;

    @Override
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setRolloverEnabled(true);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setFocusable(false);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        AbstractButton button = (AbstractButton) c;
        Icon icon = button.getIcon();
        FontMetrics fm = button.getFontMetrics(button.getFont());
        String text = getText(button);

        int padding = JBUIScale.scale(PADDING);
        int thickness = fm.getHeight();
        int length = 0;
        if (icon != null) {
            thickness = Math.max(thickness, icon.getIconWidth());
            length += icon.getIconHeight();
        }
        if (!text.isEmpty()) {
            if (length > 0) {
                length += JBUIScale.scale(GAP);
            }
            length += fm.stringWidth(text);
        }
        return new Dimension(thickness + padding * 2, length + padding * 2);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (model.isPressed() || model.isRollover()) {
                g2.setColor(model.isPressed() ? BasicStripeButtonUI.SELECTED_BACKGROUND_COLOR : BasicStripeButtonUI.BACKGROUND_COLOR);
                int arc = JBUIScale.scale(6);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);
            }

            int padding = JBUIScale.scale(PADDING);
            int top = padding;

            Icon icon = button.isEnabled() ? button.getIcon() : button.getDisabledIcon();
            if (icon != null) {
                icon.paintIcon(c, g2, (c.getWidth() - icon.getIconWidth()) / 2, top);
                top += icon.getIconHeight() + JBUIScale.scale(GAP);
            }

            String text = getText(button);
            if (!text.isEmpty()) {
                FontMetrics fm = button.getFontMetrics(button.getFont());
                UIUtil.applyRenderingHints(g2);
                g2.setFont(button.getFont());
                g2.setColor(button.isEnabled() ? button.getForeground() : UIUtil.getLabelDisabledForeground());
                g2.rotate(-Math.PI / 2);
                int baseline = (c.getWidth() - fm.getAscent() - fm.getDescent()) / 2 + fm.getAscent();
                g2.drawString(text, -(top + fm.stringWidth(text)), baseline);
            }
        }
        finally {
            g2.dispose();
        }
    }

    private static String getText(AbstractButton button) {
        if (button instanceof DesktopButtonImpl.MyButton myButton) {
            return myButton.getPlainText();
        }
        return StringUtil.notNullize(button.getText());
    }
}
