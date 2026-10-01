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
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.RenderItem;
import consulo.ui.Size2D;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.graph.GraphArrow;
import consulo.ui.graph.GraphEdgeRender;
import consulo.ui.graph.GraphGroup;
import consulo.ui.graph.GraphLineStyle;
import consulo.ui.graph.GraphModel;
import consulo.ui.graph.GraphNodeRender;
import consulo.ui.impl.graph.GraphEdgeStyle;
import consulo.ui.impl.graph.GraphNodeContent;
import consulo.ui.impl.graph.LayeredGraphLayout;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final Map<Object, LocalizeValue> myNodeTooltips = new HashMap<>();
    private final Map<Object, GraphGroup> myGroups = new HashMap<>();
    private @Nullable CellRendererPane myRendererPane;

    public DesktopAWTGraphComponent(DesktopAWTGraphImpl<E> graph,
                                    GraphModel<E> model,
                                    Supplier<GraphNodeRender<E>> nodeRender,
                                    Supplier<GraphEdgeRender<E>> edgeRender) {
        super(new DesktopAWTMxGraph());
        myGraph = graph;
        myModel = model;
        myNodeRender = nodeRender;
        myEdgeRender = edgeRender;

        setGridVisible(false);
        setConnectable(false);
        setToolTips(true);
        setBorder(JBUI.Borders.empty());
        getViewport().setBackground(UIUtil.getPanelBackground());

        ((DesktopAWTMxGraph) getGraph()).setVertexTooltip(value -> {
            LocalizeValue tooltip = myNodeTooltips.get(value);
            return tooltip == null || tooltip.get().isEmpty() ? null : tooltip.get();
        });

        getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                fireContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                fireContextMenu(e);
            }
        });
    }

    private void fireContextMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        mxGraph graph = getGraph();
        Object cell = getCellAt(e.getX(), e.getY(), false);
        if (cell != null && graph.getModel().isVertex(cell)) {
            if (!graph.isCellSelected(cell)) {
                graph.setSelectionCell(cell);
            }
        }
        else {
            graph.clearSelection();
        }

        myGraph.getListenerDispatcher(ContextMenuEvent.class)
            .onEvent(new ContextMenuEvent(myGraph, DesktopAWTInputDetails.convert(getGraphControl(), e)));
    }

    @Override
    protected mxGraphControl createGraphControl() {
        return new mxGraphControl() {
            @Override
            protected void drawFromRootCell() {
                paintGroups(canvas.getGraphics());
                super.drawFromRootCell();
            }
        };
    }

    private void paintGroups(@Nullable Graphics2D g) {
        if (g == null || myGroups == null || myGroups.isEmpty()) {
            return;
        }

        mxGraph graph = getGraph();
        Map<GraphGroup, Rectangle> frames = new LinkedHashMap<>();
        for (Object cell : graph.getChildVertices(graph.getDefaultParent())) {
            GraphGroup group = myGroups.get(graph.getModel().getValue(cell));
            mxCellState state = graph.getView().getState(cell);
            if (group != null && state != null) {
                Rectangle bounds = state.getRectangle();
                frames.merge(group, bounds, Rectangle::union);
            }
        }

        Graphics2D graphics = (Graphics2D) g.create();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int padding = JBUI.scale(12);
            int arc = JBUI.scale(10);
            FontMetrics metrics = graphics.getFontMetrics(UIUtil.getLabelFont());
            for (Map.Entry<GraphGroup, Rectangle> entry : frames.entrySet()) {
                Rectangle frame = new Rectangle(entry.getValue());
                frame.grow(padding, padding);
                frame.y -= metrics.getHeight();
                frame.height += metrics.getHeight();

                graphics.setColor(JBColor.border());
                graphics.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
                graphics.drawRoundRect(frame.x, frame.y, frame.width, frame.height, arc, arc);

                graphics.setColor(UIUtil.getInactiveTextColor());
                graphics.setFont(UIUtil.getLabelFont());
                graphics.drawString(entry.getKey().getName().get(), frame.x + padding, frame.y + metrics.getAscent() + JBUI.scale(4));
            }
        }
        finally {
            graphics.dispose();
        }
    }

    List<E> getSelectedValues() {
        mxGraph graph = getGraph();
        List<E> values = new ArrayList<>();
        for (Object cell : graph.getSelectionCells()) {
            if (graph.getModel().isVertex(cell)) {
                @SuppressWarnings("unchecked")
                E value = (E) graph.getModel().getValue(cell);
                values.add(value);
            }
        }
        return values;
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
        myNodeTooltips.clear();
        myGroups.clear();

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
                myNodeTooltips.put(node, content.getTooltip());

                GraphGroup group = myModel.getGroup(node);
                if (group != null) {
                    myGroups.put(node, group);
                }
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
