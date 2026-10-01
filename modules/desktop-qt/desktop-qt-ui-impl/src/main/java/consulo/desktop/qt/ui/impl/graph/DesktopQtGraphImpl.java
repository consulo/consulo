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
package consulo.desktop.qt.ui.impl.graph;

import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
import consulo.desktop.qt.ui.impl.DesktopQtTextItemPresentation;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Rectangle2D;
import consulo.ui.RenderItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.graph.Graph;
import consulo.ui.graph.GraphEdgeRender;
import consulo.ui.graph.GraphGroup;
import consulo.ui.graph.GraphModel;
import consulo.ui.graph.GraphNodeRender;
import consulo.ui.impl.graph.GraphEdgeStyle;
import consulo.ui.impl.graph.GraphNodeContent;
import consulo.ui.impl.graph.LayeredGraphLayout;
import io.qt.core.QRect;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class DesktopQtGraphImpl<E> extends QtComponentDelegate<QScrollArea> implements Graph<E> {
    private static final int NODE_GAP = 40;
    private static final int LAYER_GAP = 60;

    private final GraphModel<E> myModel;

    private GraphNodeRender<E> myNodeRender = GraphNodeRender.defaultRender();
    private GraphEdgeRender<E> myEdgeRender = GraphEdgeRender.defaultRender();

    private @Nullable DesktopQtGraphCanvas myCanvas;

    public DesktopQtGraphImpl(GraphModel<E> model) {
        myModel = model;
    }

    @Override
    protected QScrollArea createQt(QWidget parent) {
        QScrollArea area = new QScrollArea(parent);
        area.setWidgetResizable(true);

        DesktopQtGraphCanvas canvas = new DesktopQtGraphCanvas(area, this::rebuild, this::fireContextMenu);
        area.setWidget(canvas);
        myCanvas = canvas;
        return area;
    }

    @Override
    protected void initialize(QScrollArea component) {
        rebuild();
    }

    @RequiredUIAccess
    @Override
    public void setNodeRender(GraphNodeRender<E> render) {
        myNodeRender = render;
        rebuild();
    }

    @RequiredUIAccess
    @Override
    public void setEdgeRender(GraphEdgeRender<E> render) {
        myEdgeRender = render;
        rebuild();
    }

    private void fireContextMenu() {
        QScrollArea component = myComponent;
        if (component == null || component.isDisposed()) {
            return;
        }
        getListenerDispatcher(ContextMenuEvent.class).onEvent(new ContextMenuEvent(this, DesktopQtInputDetails.mouseAtCursor(component)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<E> getSelectedValues() {
        DesktopQtGraphCanvas canvas = myCanvas;
        if (canvas == null || canvas.isDisposed()) {
            return List.of();
        }
        return (List<E>) canvas.getSelectedValues();
    }

    @RequiredUIAccess
    @Override
    public void refresh() {
        rebuild();
    }

    private void rebuild() {
        DesktopQtGraphCanvas canvas = myCanvas;
        if (canvas == null || canvas.isDisposed() || myComponent == null) {
            return;
        }

        Map<E, GraphNodeContent<DesktopQtTextItemPresentation>> contents = new HashMap<>();
        for (E node : myModel.getNodes()) {
            GraphNodeContent<DesktopQtTextItemPresentation> content = new GraphNodeContent<>(DesktopQtTextItemPresentation::new);
            myNodeRender.render(content, RenderItem.of(node, false));
            contents.put(node, content);
        }

        Map<E, Rectangle2D> bounds = LayeredGraphLayout.layout(myModel, node -> canvas.measure(contents.get(node)), NODE_GAP, LAYER_GAP);

        List<DesktopQtGraphNode> nodes = new ArrayList<>();
        Map<E, Integer> indexes = new HashMap<>();
        int width = 0;
        int height = 0;
        for (Map.Entry<E, Rectangle2D> entry : bounds.entrySet()) {
            Rectangle2D rectangle = entry.getValue();

            indexes.put(entry.getKey(), nodes.size());
            nodes.add(new DesktopQtGraphNode(entry.getKey(), new QRect(rectangle.minX(), rectangle.minY(), rectangle.width(), rectangle.height()),
                contents.get(entry.getKey())));

            width = Math.max(width, rectangle.maxX());
            height = Math.max(height, rectangle.maxY());
        }

        List<DesktopQtGraphEdge> edges = new ArrayList<>();
        for (Map.Entry<E, Integer> entry : indexes.entrySet()) {
            for (E target : myModel.getArrows(entry.getKey())) {
                Integer targetIndex = indexes.get(target);
                if (targetIndex != null && !targetIndex.equals(entry.getValue())) {
                    edges.add(new DesktopQtGraphEdge(entry.getValue(), targetIndex, GraphEdgeStyle.of(myEdgeRender, entry.getKey(), target)));
                }
            }
        }

        Map<GraphGroup, List<Integer>> members = new LinkedHashMap<>();
        for (Map.Entry<E, Integer> entry : indexes.entrySet()) {
            GraphGroup group = myModel.getGroup(entry.getKey());
            if (group != null) {
                members.computeIfAbsent(group, it -> new ArrayList<>()).add(entry.getValue());
            }
        }
        List<DesktopQtGraphGroup> groups = new ArrayList<>();
        for (Map.Entry<GraphGroup, List<Integer>> entry : members.entrySet()) {
            groups.add(new DesktopQtGraphGroup(entry.getKey().getName().get(), entry.getValue()));
        }

        canvas.setGraph(nodes, edges, groups, width, height);
    }
}
