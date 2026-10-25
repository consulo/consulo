// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

class PillButtonUI extends BasicButtonUI {
    private static final String PROPERTY_PREFIX = "PillButton.";

    @Override
    protected String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        LookAndFeel.installProperty(b, "opaque", false);
        LookAndFeel.installProperty(b, "rolloverEnabled", true);
        LookAndFeel.installProperty(b, "iconTextGap", 4);
        if (b.getFont() == null || b.getFont() instanceof UIResource) {
            b.setFont(UIUtil.getLabelFont());
        }
        b.setBackground(TargetAWT.to(ComponentColors.REACTION_BACKGROUND));
        b.setBorder(new PillButtonBorder());
        if (b.getMargin() == null || b.getMargin() instanceof UIResource) {
            b.setMargin(JBInsets.create(1, 6).asUIResource());
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);
        if (size != null && c instanceof AbstractButton button) {
            JBInsets.addTo(size, button.getMargin());
        }
        return size;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (!(c instanceof AbstractButton button)) {
            return;
        }
        if (button.isContentAreaFilled()) {
            paintBackground(g, button);
        }
        super.paint(g, c);
    }

    private static void paintBackground(Graphics g, AbstractButton c) {
        Rectangle r = new Rectangle(c.getSize());
        JBInsets.removeFrom(r, c.getInsets());
        Graphics2D g2d = (Graphics2D) g.create(r.x, r.y, r.width, r.height);
        try {
            GraphicsUtil.setupAAPainting(g2d);
            int arc = r.height;
            g2d.setColor(getBackgroundColor(c));
            g2d.fill(new RoundRectangle2D.Float(0f, 0f, r.width, r.height, arc, arc));
        }
        finally {
            g2d.dispose();
        }
    }

    private static Color getBackgroundColor(AbstractButton c) {
        ButtonModel model = c.getModel();
        Color background = null;
        if (c.isRolloverEnabled() && c.isEnabled()) {
            if (model.isRollover()) {
                background = TargetAWT.to(ComponentColors.REACTION_HOVERED_BACKGROUND);
            }
            else if (model.isArmed() && model.isPressed()) {
                background = TargetAWT.to(ComponentColors.REACTION_PRESSED_BACKGROUND);
            }
        }
        return background != null ? background : c.getBackground();
    }
}
