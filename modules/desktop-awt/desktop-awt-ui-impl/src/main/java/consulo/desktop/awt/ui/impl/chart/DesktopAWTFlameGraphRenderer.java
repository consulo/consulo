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
package consulo.desktop.awt.ui.impl.chart;

import consulo.desktop.awt.ui.impl.adtui.chart.hchart.HRenderer;
import consulo.desktop.awt.ui.impl.adtui.common.AdtUiUtils;
import consulo.ui.chart.FlameGraphModel;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.impl.chart.ChartPalette;
import consulo.ui.impl.chart.FlameGraphNode;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopAWTFlameGraphRenderer<E> implements HRenderer<FlameGraphNode<E>> {
    private static final int TEXT_PADDING = 3;

    private final FlameGraphModel<E> myModel;
    private @Nullable Predicate<E> myHighlight;
    private @Nullable E mySelected;

    DesktopAWTFlameGraphRenderer(FlameGraphModel<E> model) {
        myModel = model;
    }

    void setHighlight(@Nullable Predicate<E> highlight) {
        myHighlight = highlight;
    }

    void setSelected(@Nullable E selected) {
        mySelected = selected;
    }

    @Override
    public void render(Graphics2D g,
                       FlameGraphNode<E> node,
                       Rectangle2D fullDrawingArea,
                       Rectangle2D drawingArea,
                       boolean isFocused,
                       boolean isDeselected) {
        E value = node.getValue();
        String name = myModel.getName(value);

        ColorValue modelColor = myModel.getColor(value);
        Color fill = TargetAWT.to(modelColor != null ? modelColor : ChartPalette.flame(name));
        boolean dimmed = myHighlight != null && !myHighlight.test(value);
        if (dimmed) {
            fill = ColorUtil.toAlpha(fill, 90);
        }

        g.setColor(fill);
        g.fill(drawingArea);

        if (value.equals(mySelected)) {
            g.setColor(JBColor.BLACK);
            g.draw(new Rectangle2D.Double(drawingArea.getX(), drawingArea.getY(), drawingArea.getWidth() - 1, drawingArea.getHeight() - 1));
        }

        FontMetrics metrics = g.getFontMetrics();
        String text = AdtUiUtils.shrinkToFit(name, metrics, (float) drawingArea.getWidth() - TEXT_PADDING * 2);
        if (!text.isEmpty()) {
            g.setColor(dimmed ? JBColor.GRAY : JBColor.BLACK);
            float textY = (float) (drawingArea.getY() + (drawingArea.getHeight() - metrics.getHeight()) * 0.5 + metrics.getAscent());
            g.drawString(text, (float) drawingArea.getX() + TEXT_PADDING, textY);
        }
    }
}
