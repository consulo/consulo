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
package consulo.desktop.awt.ui.impl.messageList;

import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.impl.messageList.MessageListPresentation;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * The picture of the author of a message: the image when there is one, the initials on a colour of the name otherwise.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
class MessageListAvatar extends JComponent {
    private static final int SIZE = 28;

    private String myAuthorName = "";
    private @Nullable Icon myIcon;

    void setAuthorName(String name) {
        myAuthorName = name;
        repaint();
    }

    void setIcon(@Nullable Icon icon) {
        myIcon = icon;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(JBUI.scale(SIZE), JBUI.scale(SIZE));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int size = JBUI.scale(SIZE);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float stroke = JBUI.scale(1f);
            Ellipse2D circle = new Ellipse2D.Float(stroke / 2, stroke / 2, size - stroke, size - stroke);
            g2.setColor(TargetAWT.to(ComponentColors.COMPONENT_BACKGROUND));
            g2.fill(circle);
            g2.setColor(TargetAWT.to(ComponentColors.BORDER));
            g2.setStroke(new BasicStroke(stroke));
            g2.draw(circle);

            Icon icon = myIcon;
            if (icon != null) {
                icon.paintIcon(this, g2, (size - icon.getIconWidth()) / 2, (size - icon.getIconHeight()) / 2);
                return;
            }

            String initials = MessageListPresentation.initialsOf(myAuthorName);
            g2.setFont(UIUtil.getLabelFont().deriveFont(Font.PLAIN, size * 0.42f));
            FontMetrics metrics = g2.getFontMetrics();
            g2.setColor(colorOf(myAuthorName));
            g2.drawString(initials, (size - metrics.stringWidth(initials)) / 2f, (size - metrics.getHeight()) / 2f + metrics.getAscent());
        }
        finally {
            g2.dispose();
        }
    }

    static Color colorOf(String name) {
        boolean dark = StyleManager.get().getCurrentStyle().isDark();
        return Color.getHSBColor(
            MessageListPresentation.hueOf(name),
            MessageListPresentation.saturation(dark),
            MessageListPresentation.brightness(dark)
        );
    }
}
