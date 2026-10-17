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

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.AttachEvent;
import consulo.ui.event.DetachEvent;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.MemoryIndicator;
import consulo.ui.style.ComponentColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class DesktopMemoryIndicatorImpl extends SwingComponentDelegate<JComponent> implements MemoryIndicator {
    private static final int HORIZONTAL_PADDING = 6;
    private static final int VERTICAL_PADDING = 2;

    private class MyIndicator extends JComponent implements FromSwingComponentWrapper {
        private boolean myHovered;

        MyIndicator() {
            setOpaque(false);
            updateUI();

            addHierarchyListener(event -> {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                    return;
                }

                if (isShowing()) {
                    getListenerDispatcher(AttachEvent.class).onEvent(new AttachEvent(DesktopMemoryIndicatorImpl.this));
                }
                else {
                    getListenerDispatcher(DetachEvent.class).onEvent(new DetachEvent(DesktopMemoryIndicatorImpl.this));
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setHovered(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setHovered(false);
                }
            });
        }

        private void setHovered(boolean hovered) {
            if (myHovered != hovered) {
                myHovered = hovered;
                repaint();
            }
        }

        @Override
        public Component toUIComponent() {
            return DesktopMemoryIndicatorImpl.this;
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setFont(UIUtil.getLabelFont());
            setForeground(UIUtil.getLabelForeground());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Insets insets = getInsets();
            FontMetrics metrics = getFontMetrics(getFont());
            int width = getWidth() - insets.left - insets.right;
            int height = Math.min(getHeight() - insets.top - insets.bottom, metrics.getHeight() + JBUI.scale(2 * VERTICAL_PADDING));
            if (width <= 0 || height <= 0) {
                return;
            }

            int x = insets.left;
            int y = insets.top + (getHeight() - insets.top - insets.bottom - height) / 2;
            int arc = JBUI.scale(UIManager.getInt("Component.arc"));

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                GraphicsUtil.setupAAPainting(g);
                UISettingsUtil.setupAntialiasing(g);

                Area shape = new Area(new RoundRectangle2D.Float(x, y, width, height, arc, arc));

                if (myHovered) {
                    g.setColor(TargetAWT.to(ComponentColors.HOVER_BACKGROUND));
                    g.fill(shape);
                }

                if (myMax > 0) {
                    g.setColor(TargetAWT.to(ComponentColors.MEMORY_INDICATOR_ALLOCATED_BACKGROUND));
                    g.fill(part(shape, x, y, width, height, myAllocated));
                    g.setColor(TargetAWT.to(ComponentColors.MEMORY_INDICATOR_USED_BACKGROUND));
                    g.fill(part(shape, x, y, width, height, myUsed));
                }

                g.setColor(TargetAWT.to(ComponentColors.BORDER));
                g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1, height - 1, arc, arc));

                String text = myText.get();
                g.setFont(getFont());
                g.setColor(getForeground());
                int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
                g.drawString(text, x + (width - metrics.stringWidth(text)) / 2, textY);
            }
            finally {
                g.dispose();
            }
        }

        private Area part(Area shape, int x, int y, int width, int height, long value) {
            Area part = new Area(new Rectangle2D.Float(x, y, width * (float) Math.min(value, myMax) / myMax, height));
            part.intersect(shape);
            return part;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());
            Insets insets = getInsets();
            int width = metrics.stringWidth(myWidestText.get()) + JBUI.scale(2 * HORIZONTAL_PADDING) + insets.left + insets.right;
            int height = metrics.getHeight() + JBUI.scale(2 * VERTICAL_PADDING) + insets.top + insets.bottom;
            return new Dimension(width, height);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

    private final LocalizeValue myWidestText;

    private LocalizeValue myText = LocalizeValue.empty();
    private long myUsed;
    private long myAllocated;
    private long myMax;

    public DesktopMemoryIndicatorImpl(LocalizeValue widestText) {
        myWidestText = widestText;
    }

    @Override
    protected JComponent createComponent() {
        return new MyIndicator();
    }

    @RequiredUIAccess
    @Override
    public void setUsage(long used, long allocated, long max, LocalizeValue text) {
        myUsed = used;
        myAllocated = allocated;
        myMax = max;
        myText = text;

        toAWTComponent().repaint();
    }
}
