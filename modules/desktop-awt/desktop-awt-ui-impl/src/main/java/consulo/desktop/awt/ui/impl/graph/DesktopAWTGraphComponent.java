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

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.RenderItem;
import consulo.ui.Size2D;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.graph.GraphArrow;
import consulo.ui.graph.GraphEdgeRender;
import consulo.ui.graph.GraphLineStyle;
import consulo.ui.graph.GraphModel;
import consulo.ui.graph.GraphNodeRender;
import consulo.ui.impl.graph.GraphEdgeStyle;
import consulo.ui.impl.graph.GraphNodeContent;
import consulo.ui.impl.graph.LayeredGraphLayout;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class DesktopAWTGraphComponent<E> extends mxGraphComponent implements FromSwingComponentWrapper {
    private final DesktopAWTGraphImpl<E> myGraph;
    private final GraphModel<E> myModel;
    private final Supplier<GraphNodeRender<E>> myNodeRender;
    private final Supplier<GraphEdgeRender<E>> myEdgeRender;

    private final Map<Object, DesktopAWTGraphNodeView> myNodeViews = new HashMap<>();
    private @Nullable CellRendererPane myRendererPane;

    public DesktopAWTGraphComponent(DesktopAWTGraphImpl<E> graph,
                                    GraphModel<E> model,
                                    Supplier<GraphNodeRender<E>> nodeRender,
                                    Supplier<GraphEdgeRender<E>> edgeRender) {
        super(createGraph());
        myGraph = graph;
        myModel = model;
        myNodeRender = nodeRender;
        myEdgeRender = edgeRender;

        setGridVisible(false);
        setConnectable(false);
        setToolTips(false);
        setBorder(JBUI.Borders.empty());
        getViewport().setBackground(UIUtil.getPanelBackground());
    }

    private static mxGraph createGraph() {
        mxGraph graph = new mxGraph() {
            @Override
            public String convertValueToString(Object cell) {
                return getModel().getValue(cell) instanceof GraphEdgeStyle style ? style.getLabel().get() : "";
            }
        };
        graph.setCellsResizable(false);
        graph.setCellsEditable(false);
        graph.setCellsDeletable(false);
        graph.setCellsCloneable(false);
        graph.setCellsDisconnectable(false);
        graph.setCellsBendable(false);
        graph.setConnectableEdges(false);
        graph.setAllowDanglingEdges(false);
        graph.setEdgeLabelsMovable(false);
        graph.setDropEnabled(false);
        graph.setSplitEnabled(false);
        return graph;
    }

    @Override
    public mxInteractiveCanvas createCanvas() {
        return new DesktopAWTGraphCanvas<>(this);
    }

    @Override
    public Component toUIComponent() {
        return myGraph;
    }

    @Override
    public void updateUI() {
        super.updateUI();

        if (getViewport() != null) {
            getViewport().setBackground(UIUtil.getPanelBackground());
        }

        if (myNodeViews != null && !myNodeViews.isEmpty()) {
            rebuild();
        }
    }

    void rebuild() {
        myNodeViews.clear();

        mxGraph graph = getGraph();
        Object parent = graph.getDefaultParent();

        GraphNodeRender<E> nodeRender = myNodeRender.get();
        GraphEdgeRender<E> edgeRender = myEdgeRender.get();

        graph.getModel().beginUpdate();
        try {
            graph.removeCells(graph.getChildCells(parent, true, true));

            for (E node : myModel.getNodes()) {
                GraphNodeContent<DesktopAWTGraphRowPresentation> content =
                    new GraphNodeContent<>(() -> new DesktopAWTGraphRowPresentation(DesktopAWTGraphNodeView.createRowComponent()));
                nodeRender.render(content, RenderItem.of(node, false));
                myNodeViews.put(node, new DesktopAWTGraphNodeView(content));
            }

            Map<E, Rectangle2D> bounds = LayeredGraphLayout.layout(myModel, this::measure, JBUI.scale(40), JBUI.scale(60));

            int margin = JBUI.scale(20);
            Map<E, Object> vertices = new HashMap<>();
            for (Map.Entry<E, Rectangle2D> entry : bounds.entrySet()) {
                Rectangle2D rectangle = entry.getValue();
                Object vertex = graph.insertVertex(parent,
                    null,
                    entry.getKey(),
                    rectangle.minX() + margin,
                    rectangle.minY() + margin,
                    rectangle.width(),
                    rectangle.height());
                vertices.put(entry.getKey(), vertex);
            }

            for (Map.Entry<E, Object> entry : vertices.entrySet()) {
                for (E target : myModel.getArrows(entry.getKey())) {
                    Object targetVertex = vertices.get(target);
                    if (targetVertex != null && targetVertex != entry.getValue()) {
                        GraphEdgeStyle style = GraphEdgeStyle.of(edgeRender, entry.getKey(), target);
                        graph.insertEdge(parent, null, style, entry.getValue(), targetVertex, toMxStyle(style));
                    }
                }
            }
        }
        finally {
            graph.getModel().endUpdate();
        }
    }

    private static String toMxStyle(GraphEdgeStyle style) {
        StringBuilder builder = new StringBuilder();
        builder.append(mxConstants.STYLE_STARTSIZE).append("=10;");
        builder.append(mxConstants.STYLE_ENDSIZE).append("=10;");
        appendArrow(builder, mxConstants.STYLE_STARTARROW, "startFill", style.getSourceArrow());
        appendArrow(builder, mxConstants.STYLE_ENDARROW, "endFill", style.getTargetArrow());

        GraphLineStyle lineStyle = style.getLineStyle();
        if (lineStyle != GraphLineStyle.SOLID) {
            builder.append(mxConstants.STYLE_DASHED).append("=1;");
            builder.append(mxConstants.STYLE_DASH_PATTERN).append('=').append(lineStyle == GraphLineStyle.DOTTED ? "1 3" : "5 4").append(';');
        }
        return builder.toString();
    }

    private static void appendArrow(StringBuilder builder, String key, String fillKey, GraphArrow arrow) {
        String type = switch (arrow) {
            case NONE -> mxConstants.NONE;
            case FILLED -> mxConstants.ARROW_CLASSIC;
            case OPEN -> mxConstants.ARROW_OPEN;
            case TRIANGLE -> mxConstants.ARROW_BLOCK;
            case DIAMOND -> mxConstants.ARROW_DIAMOND;
            case CIRCLE -> mxConstants.ARROW_OVAL;
        };
        boolean filled = arrow == GraphArrow.FILLED;
        builder.append(key).append('=').append(type).append(';');
        builder.append(fillKey).append('=').append(filled ? '1' : '0').append(';');
    }

    private Size2D measure(E node) {
        DesktopAWTGraphNodeView view = myNodeViews.get(node);
        if (view == null) {
            return Size2D.ZERO;
        }
        Dimension size = view.getPreferredSize();
        return new Size2D(size.width, size.height);
    }

    @Nullable DesktopAWTGraphNodeView getNodeView(@Nullable Object value) {
        return value == null ? null : myNodeViews.get(value);
    }

    CellRendererPane getRendererPane() {
        if (myRendererPane == null) {
            myRendererPane = new CellRendererPane();
            getGraphControl().add(myRendererPane);
        }
        return myRendererPane;
    }
}
