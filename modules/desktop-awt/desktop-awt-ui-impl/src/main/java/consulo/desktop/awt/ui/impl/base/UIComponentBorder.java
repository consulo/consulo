/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.awt.ui.impl.base;

import consulo.desktop.awt.ui.impl.DesktopSpace;
import consulo.ui.Space;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.internal.BorderPosition;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
class UIComponentBorder implements Border {
    private static final int ARC = 4;

    private final Map<BorderPosition, ColorValue> myBorders;
    private final Map<BorderPosition, Space> myPaddings;

    UIComponentBorder(Map<BorderPosition, ColorValue> borders, Map<BorderPosition, Space> paddings) {
        myBorders = borders;
        myPaddings = paddings;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();

        if (isFullBorder()) {
            paintRounded(myBorders.get(BorderPosition.TOP), g, x, y, width, height);
        }
        else {
            paintBorder(BorderPosition.LEFT, g, (thickness) -> g.fillRect(x, y, thickness, height));
            paintBorder(BorderPosition.TOP, g, (thickness) -> g.fillRect(x, y, width, thickness));
            paintBorder(BorderPosition.RIGHT, g, (thickness) -> g.fillRect(x + width - thickness, y, thickness, height));
            paintBorder(BorderPosition.BOTTOM, g, (thickness) -> g.fillRect(x, y + height - thickness, width, thickness));
        }


        g.setColor(oldColor);
    }

    /**
     * A corner is shared by two sides, so a component asking for every side is drawn as one shape around the whole
     * of it, with whatever corner the style rounds it to, rather than side by side.
     */
    private boolean isFullBorder() {
        return myBorders.size() == BorderPosition.values().length;
    }

    private static void paintRounded(ColorValue colorValue, Graphics g, int x, int y, int width, int height) {
        int thickness = JBUI.scale(1);
        if (width <= 0 || height <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TargetAWT.to(colorValue));
            g2.setStroke(new BasicStroke(thickness));

            int arc = JBUI.scale(ARC);
            // the stroke is centered on the shape, so the shape is inset by half of it to keep the line inside
            float offset = thickness / 2f;
            g2.draw(new RoundRectangle2D.Float(
                x + offset,
                y + offset,
                width - thickness,
                height - thickness,
                arc,
                arc
            ));
        }
        finally {
            g2.dispose();
        }
    }

    private void paintBorder(BorderPosition position, Graphics g, IntConsumer consumer) {
        ColorValue colorValue = myBorders.get(position);
        if (colorValue == null) {
            return;
        }

        g.setColor(TargetAWT.to(colorValue));

        consumer.accept(JBUI.scale(1));
    }

    @Override
    public Insets getBorderInsets(Component component) {
        //noinspection UseDPIAwareInsets
        Insets insets = new Insets(0, 0, 0, 0);
        insets.top = edgeSize(BorderPosition.TOP);
        insets.left = edgeSize(BorderPosition.LEFT);
        insets.bottom = edgeSize(BorderPosition.BOTTOM);
        insets.right = edgeSize(BorderPosition.RIGHT);


        return insets;
    }

    private int edgeSize(BorderPosition position) {
        int size = myBorders.containsKey(position) ? JBUI.scale(1) : 0;

        Space space = myPaddings.get(position);
        if (space != null) {
            size += DesktopSpace.toPixels(space);
        }

        return size;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
