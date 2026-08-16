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

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
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

/**
 * @author VISTALL
 * @since 2026-08-16
 */
class DesktopSeparatorImpl extends SwingComponentDelegate<DesktopSeparatorImpl.MySeparator> implements Separator {
    private static final int LINE_LENGTH = 16;
    private static final int LINE_MARGIN = 4;
    private static final int THICKNESS = LINE_MARGIN * 2 + 1;

    class MySeparator extends JSeparator implements FromSwingComponentWrapper {
        MySeparator(int orientation) {
            super(orientation);

            // only the line is painted, so the area around it must be left to whatever stands behind the separator
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            // a layout stretches the separator to the cross size of the row it sits in, so the line is kept short
            // by the painting rather than by the size of the component
            g.setColor(JBColor.border());

            if (getOrientation() == SwingConstants.VERTICAL) {
                int length = Math.min(JBUIScale.scale(LINE_LENGTH), getHeight() - JBUIScale.scale(LINE_MARGIN) * 2);
                if (length <= 0) {
                    return;
                }

                int x = getWidth() / 2;
                int y = (getHeight() - length) / 2;
                LinePainter2D.paint((Graphics2D) g, x, y, x, y + length);
            }
            else {
                int length = Math.min(JBUIScale.scale(LINE_LENGTH), getWidth() - JBUIScale.scale(LINE_MARGIN) * 2);
                if (length <= 0) {
                    return;
                }

                int x = (getWidth() - length) / 2;
                int y = getHeight() / 2;
                LinePainter2D.paint((Graphics2D) g, x, y, x + length, y);
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
        separator.setPreferredSize(vertical ? new JBDimension(THICKNESS, LINE_LENGTH) : new JBDimension(LINE_LENGTH, THICKNESS));
        return separator;
    }

    @Override
    public SeparatorStyle getSeparatorStyle() {
        return myStyle;
    }
}
