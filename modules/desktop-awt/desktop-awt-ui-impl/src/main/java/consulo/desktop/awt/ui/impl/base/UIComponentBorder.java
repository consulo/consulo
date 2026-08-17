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

import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.impl.BorderInfo;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

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
    private final Map<BorderPosition, BorderInfo> myBorders;

    UIComponentBorder(Map<BorderPosition, BorderInfo> borders) {
        myBorders = borders;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        BorderInfo rounded = findRounded();
        if (rounded != null) {
            paintRounded(rounded, g, x, y, width, height);
            return;
        }

        Color oldColor = g.getColor();
        paintBorder(BorderPosition.LEFT, g, (thickness) -> g.fillRect(x, y, thickness, height));
        paintBorder(BorderPosition.TOP, g, (thickness) -> g.fillRect(x, y, width, thickness));
        paintBorder(BorderPosition.RIGHT, g, (thickness) -> g.fillRect(x + width - thickness, y, thickness, height));
        paintBorder(BorderPosition.BOTTOM, g, (thickness) -> g.fillRect(x, y + height - thickness, width, thickness));
        g.setColor(oldColor);
    }

    /**
     * A corner is shared by two sides, so a rounded border is drawn as one shape around the whole component rather
     * than side by side.
     */
    private @Nullable BorderInfo findRounded() {
        for (BorderInfo borderInfo : myBorders.values()) {
            if (borderInfo.getBorderStyle() == BorderStyle.LINE_ROUNDED) {
                return borderInfo;
            }
        }
        return null;
    }

    private static void paintRounded(BorderInfo borderInfo, Graphics g, int x, int y, int width, int height) {
        int thickness = JBUI.scale(borderInfo.getWidth());
        if (thickness <= 0 || width <= 0 || height <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TargetAWT.to(color(borderInfo)));
            g2.setStroke(new BasicStroke(thickness));

            int arc = JBUI.scale(BorderStyle.DEFAULT_ARC);
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
        BorderInfo borderInfo = myBorders.get(position);
        if (borderInfo == null) {
            return;
        }

        BorderStyle borderStyle = borderInfo.getBorderStyle();
        if (borderStyle != BorderStyle.LINE) {
            return;
        }

        g.setColor(TargetAWT.to(color(borderInfo)));

        consumer.accept(JBUI.scale(borderInfo.getWidth()));
    }

    private static ColorValue color(BorderInfo borderInfo) {
        ColorValue colorValue = borderInfo.getColorValue();
        return colorValue == null ? ComponentColors.BORDER : colorValue;
    }

    @Override
    public Insets getBorderInsets(Component component) {
        //noinspection UseDPIAwareInsets
        Insets insets = new Insets(0, 0, 0, 0);
        insets.top = getBorderSize(myBorders, BorderPosition.TOP);
        insets.left = getBorderSize(myBorders, BorderPosition.LEFT);
        insets.bottom = getBorderSize(myBorders, BorderPosition.BOTTOM);
        insets.right = getBorderSize(myBorders, BorderPosition.RIGHT);
        return insets;
    }

    static int getBorderSize(Map<BorderPosition, BorderInfo> map, BorderPosition position) {
        BorderInfo borderInfo = map.get(position);
        if (borderInfo == null) {
            return 0;
        }
        return JBUI.scale(borderInfo.getWidth());
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
