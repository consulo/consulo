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

import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;

import javax.swing.AbstractButton;
import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Similar to a rounded line border, but uses component height as arc and uses component background color by default
 */
class PillButtonBorder extends AbstractBorder {
    private static final int THICKNESS = 1;

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(THICKNESS, THICKNESS, THICKNESS, THICKNESS);
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color color = c instanceof AbstractButton button && button.getModel().isSelected()
            ? TargetAWT.to(ComponentColors.REACTION_SELECTED_BORDER)
            : c.getBackground();
        if (color == null) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g.create(x, y, width, height);
        try {
            g2d.setColor(color);
            GraphicsUtil.setupRoundedBorderAntialiasing(g2d);
            float arc = height;
            Path2D border = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            border.append(new RoundRectangle2D.Float(0f, 0f, width, height, arc, arc), false);

            int coordinateDelta = THICKNESS;
            int sizeDelta = THICKNESS * 2;
            border.append(new RoundRectangle2D.Float(coordinateDelta, coordinateDelta, width - sizeDelta, height - sizeDelta,
                arc - sizeDelta, arc - sizeDelta), false);
            g2d.fill(border);
        }
        finally {
            g2d.dispose();
        }
    }
}
