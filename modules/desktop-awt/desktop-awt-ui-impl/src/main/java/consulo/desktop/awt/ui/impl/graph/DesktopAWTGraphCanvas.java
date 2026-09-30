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
package consulo.desktop.awt.ui.impl.graph;

import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.impl.graph.GraphEdgeStyle;

import java.awt.*;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
class DesktopAWTGraphCanvas<E> extends mxInteractiveCanvas {
    private final DesktopAWTGraphComponent<E> myComponent;

    DesktopAWTGraphCanvas(DesktopAWTGraphComponent<E> component) {
        myComponent = component;
    }

    @Override
    public Object drawCell(mxCellState state) {
        mxGraph graph = myComponent.getGraph();
        Object cell = state.getCell();
        if (graph.getModel().isVertex(cell)) {
            paintVertex(state, graph.getModel().getValue(cell), graph.isCellSelected(cell));
            return null;
        }

        Color color = UIUtil.getInactiveTextColor();
        if (graph.getModel().getValue(cell) instanceof GraphEdgeStyle style && style.getColor() != null) {
            color = TargetAWT.to(style.getColor());
        }

        Map<String, Object> style = state.getStyle();
        style.put(mxConstants.STYLE_STROKECOLOR, mxUtils.getHexColorString(color));
        style.put(mxConstants.STYLE_FILLCOLOR, mxUtils.getHexColorString(color));
        style.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(UIUtil.getLabelForeground()));
        return super.drawCell(state);
    }

    private void paintVertex(mxCellState state, Object value, boolean selected) {
        DesktopAWTGraphNodeView view = myComponent.getNodeView(value);
        if (g == null || view == null) {
            return;
        }

        Rectangle bounds = state.getRectangle();
        double scale = getScale();

        Graphics2D graphics = (Graphics2D) g.create();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = JBUI.scale(8);
            graphics.setColor(selected ? UIUtil.getListSelectionBackground(false) : UIUtil.getListBackground());
            graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);

            graphics.translate(bounds.x, bounds.y);
            graphics.scale(scale, scale);

            int width = (int) Math.round(bounds.width / scale);
            int height = (int) Math.round(bounds.height / scale);
            myComponent.getRendererPane().paintComponent(graphics, view, myComponent.getGraphControl(), 0, 0, width, height, true);

            graphics.setColor(selected ? UIUtil.getFocusedBoundsColor() : JBColor.border());
            graphics.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
        }
        finally {
            graphics.dispose();
        }
    }
}
