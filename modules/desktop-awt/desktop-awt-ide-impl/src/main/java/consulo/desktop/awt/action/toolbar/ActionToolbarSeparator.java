/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.action.toolbar;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.paint.LinePainter2D;
import consulo.ui.ex.awt.util.UISettingsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
* @author VISTALL
* @since 2024-12-31
*/
final class ActionToolbarSeparator extends JComponent {
    private DesktopAWTActionToolbar myActionToolbar;
    @Nonnull
    private final LocalizeValue myTextValue;

    ActionToolbarSeparator(DesktopAWTActionToolbar actionToolbar, @Nonnull LocalizeValue textValue) {
        myActionToolbar = actionToolbar;
        myTextValue = textValue;
        setFont(JBUI.Fonts.toolbarSmallComboBoxFont());
    }

    @Override
    public Dimension getPreferredSize() {
        int gap = JBUIScale.scale(2);
        int center = JBUIScale.scale(3);
        int width = gap * 2 + center;
        int height = JBUIScale.scale(24);

        if (myActionToolbar.getStyle().isHorizontal()) {
            if (myTextValue != LocalizeValue.empty()) {
                FontMetrics fontMetrics = getFontMetrics(getFont());

                int textWidth = getTextWidth(fontMetrics, myTextValue.get(), getGraphics());
                return new JBDimension(width + gap * 2 + textWidth, Math.max(fontMetrics.getHeight(), height), true);
            }
            else {
                return new JBDimension(width, height, true);
            }
        }
        else {
            //noinspection SuspiciousNameCombination
            return new JBDimension(height, width, true);
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        if (getParent() == null) {
            return;
        }

        int gap = JBUIScale.scale(2);
        int center = JBUIScale.scale(3);
        int offset;
        
        boolean horizontal = myActionToolbar.getStyle().isHorizontal();
        if (horizontal) {
            offset = myActionToolbar.getHeight() - myActionToolbar.getMaxButtonHeight() - 1;
        }
        else {
            offset = myActionToolbar.getWidth() - myActionToolbar.getMaxButtonWidth() - 1;
        }

        g.setColor(JBColor.border());
        if (horizontal) {
            int y2 = myActionToolbar.getHeight() - gap * 2 - offset;
            LinePainter2D.paint((Graphics2D) g, center, gap, center, y2);

            if (myTextValue != LocalizeValue.empty()) {
                FontMetrics fontMetrics = getFontMetrics(getFont());
                int top = (getHeight() - fontMetrics.getHeight()) / 2;
                UISettingsUtil.setupAntialiasing(g);
                g.setColor(JBColor.foreground());
                g.drawString(myTextValue.getValue(), gap * 2 + center + gap, top + fontMetrics.getAscent());
            }
        }
        else {
            LinePainter2D.paint((Graphics2D) g, gap, center, myActionToolbar.getWidth() - gap * 2 - offset, center);
        }
    }

    private int getTextWidth(@Nonnull FontMetrics fontMetrics, @Nonnull String text, @Nullable Graphics graphics) {
        if (graphics == null) {
            return fontMetrics.stringWidth(text);
        }
        else {
            Graphics g = graphics.create();
            try {
                UISettingsUtil.setupAntialiasing(g);
                return fontMetrics.getStringBounds(text, g).getBounds().width;
            }
            finally {
                g.dispose();
            }
        }
    }
}
