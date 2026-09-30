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

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.Separator;
import consulo.ui.SeparatorStyle;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.paint.LinePainter2D;

import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
class DesktopSeparatorImpl extends SwingComponentDelegate<DesktopSeparatorImpl.MySeparator> implements Separator {
    private static final int LINE_MARGIN = 4;
    private static final int LINE_INSET = 4;
    private static final int THICKNESS = LINE_MARGIN * 2 + 1;

    class MySeparator extends JSeparator implements FromSwingComponentWrapper {
        MySeparator(int orientation) {
            super(orientation);

            // only the line is painted, so the area around it must be left to whatever stands behind the separator
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Insets insets = getInsets();
            int inset = JBUIScale.scale(LINE_INSET);
            boolean vertical = getOrientation() == SwingConstants.VERTICAL;

            int left = insets.left + (vertical ? 0 : inset);
            int top = insets.top + (vertical ? inset : 0);
            int right = getWidth() - insets.right - 1 - (vertical ? 0 : inset);
            int bottom = getHeight() - insets.bottom - 1 - (vertical ? inset : 0);
            if (right < left || bottom < top) {
                return;
            }

            g.setColor(JBColor.border());

            if (vertical) {
                int x = (left + right) / 2;
                LinePainter2D.paint((Graphics2D) g, x, top, x, bottom);
            }
            else {
                int y = (top + bottom) / 2;
                LinePainter2D.paint((Graphics2D) g, left, y, right, y);
            }
        }

        @Override
        public Component toUIComponent() {
            return DesktopSeparatorImpl.this;
        }
    }

    private final SeparatorStyle myStyle;

    DesktopSeparatorImpl(SeparatorStyle style) {
        myStyle = style;
    }

    @Override
    protected MySeparator createComponent() {
        boolean vertical = myStyle == SeparatorStyle.VERTICAL;

        MySeparator separator = new MySeparator(vertical ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL);
        separator.setPreferredSize(vertical ? new JBDimension(THICKNESS, 0) : new JBDimension(0, THICKNESS));
        return separator;
    }

    @Override
    public SeparatorStyle getSeparatorStyle() {
        return myStyle;
    }
}
