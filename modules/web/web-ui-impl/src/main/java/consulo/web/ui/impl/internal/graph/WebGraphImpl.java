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
package consulo.web.ui.impl.internal.graph;

import com.vaadin.flow.component.html.Div;
import consulo.ui.RenderItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.graph.Graph;
import consulo.ui.graph.GraphEdgeRender;
import consulo.ui.graph.GraphGroup;
import consulo.ui.graph.GraphModel;
import consulo.ui.graph.GraphNodeRender;
import consulo.ui.impl.graph.GraphEdgeStyle;
import consulo.ui.impl.graph.GraphNodeContent;
import consulo.ui.impl.graph.LayeredGraphLayout;
import consulo.web.ui.impl.internal.WebColors;
import consulo.web.ui.impl.internal.WebItemPresentationImpl;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class WebGraphImpl<E> extends VaadinComponentDelegate<WebGraphVaadin> implements Graph<E> {
    private static final String DRAW_EDGES_SCRIPT = """
        const host = this;
        host.$graphEdges = JSON.parse($0);
        host.$graphGroups = JSON.parse($1);
        const ns = 'http://www.w3.org/2000/svg';
        if (!host.$graphId) {
            host.$graphId = 'consulo-graph-' + Math.random().toString(36).slice(2);
        }
        const defaultColor = 'var(--consulo-label-disabled-foreground, gray)';
        const surface = 'var(--vaadin-background-color, Canvas)';

        const marker = (defs, markers, kind, color) => {
            if (kind === 'NONE') {
                return null;
            }
            const key = kind + '|' + color;
            if (markers[key]) {
                return markers[key];
            }
            const id = host.$graphId + '-' + Object.keys(markers).length;
            const element = document.createElementNS(ns, 'marker');
            element.setAttribute('id', id);
            element.setAttribute('orient', 'auto-start-reverse');
            element.setAttribute('markerUnits', 'userSpaceOnUse');
            let shape;
            if (kind === 'DIAMOND') {
                element.setAttribute('viewBox', '0 0 20 10');
                element.setAttribute('refX', '20');
                element.setAttribute('refY', '5');
                element.setAttribute('markerWidth', '20');
                element.setAttribute('markerHeight', '10');
                shape = document.createElementNS(ns, 'path');
                shape.setAttribute('d', 'M0,5 L10,0 L20,5 L10,10 z');
            }
            else if (kind === 'CIRCLE') {
                element.setAttribute('viewBox', '0 0 10 10');
                element.setAttribute('refX', '10');
                element.setAttribute('refY', '5');
                element.setAttribute('markerWidth', '10');
                element.setAttribute('markerHeight', '10');
                shape = document.createElementNS(ns, 'circle');
                shape.setAttribute('cx', '5');
                shape.setAttribute('cy', '5');
                shape.setAttribute('r', '4');
            }
            else {
                element.setAttribute('viewBox', '0 0 10 10');
                element.setAttribute('refX', '10');
                element.setAttribute('refY', '5');
                element.setAttribute('markerWidth', '11');
                element.setAttribute('markerHeight', '11');
                shape = document.createElementNS(ns, 'path');
                shape.setAttribute('d', kind === 'OPEN' ? 'M0,0 L10,5 L0,10' : 'M0,0 L10,5 L0,10 z');
            }
            shape.style.stroke = color;
            shape.style.strokeWidth = '1';
            shape.style.fill = kind === 'FILLED' ? color : (kind === 'OPEN' ? 'none' : surface);
            element.append(shape);
            defs.append(element);
            markers[key] = 'url(#' + id + ')';
            return markers[key];
        };

        const draw = () => {
            let svg = host.querySelector(':scope > svg.consulo-graph-edges');
            if (!svg) {
                svg = document.createElementNS(ns, 'svg');
                svg.classList.add('consulo-graph-edges');
                svg.style.position = 'absolute';
                svg.style.left = '0';
                svg.style.top = '0';
                svg.style.pointerEvents = 'none';
                svg.style.overflow = 'visible';
                host.prepend(svg);
            }
            svg.setAttribute('width', host.scrollWidth);
            svg.setAttribute('height', host.scrollHeight);

            const defs = document.createElementNS(ns, 'defs');
            svg.replaceChildren(defs);
            const markers = {};

            const origin = host.getBoundingClientRect();
            const nodes = {};
            host.querySelectorAll('[data-graph-node]').forEach(node => nodes[node.dataset.graphNode] = node);
            const box = node => {
                const r = node.getBoundingClientRect();
                const left = r.left - origin.left + host.scrollLeft;
                const top = r.top - origin.top + host.scrollTop;
                return {left, top, right: left + r.width, bottom: top + r.height, x: left + r.width / 2, y: top + r.height / 2};
            };

            for (const group of host.$graphGroups) {
                let frame = null;
                for (const member of group.members) {
                    const node = nodes[member];
                    if (!node) {
                        continue;
                    }
                    const b = box(node);
                    frame = frame === null ? {...b} : {
                        left: Math.min(frame.left, b.left),
                        top: Math.min(frame.top, b.top),
                        right: Math.max(frame.right, b.right),
                        bottom: Math.max(frame.bottom, b.bottom)
                    };
                }
                if (frame === null) {
                    continue;
                }
                const padding = 12;
                const title = 16;
                const rect = document.createElementNS(ns, 'rect');
                rect.setAttribute('x', frame.left - padding);
                rect.setAttribute('y', frame.top - padding - title);
                rect.setAttribute('width', frame.right - frame.left + padding * 2);
                rect.setAttribute('height', frame.bottom - frame.top + padding * 2 + title);
                rect.setAttribute('rx', '10');
                rect.style.fill = 'none';
                rect.style.stroke = 'var(--consulo-component-border-color, lightgray)';
                rect.style.strokeDasharray = '4 3';
                svg.append(rect);

                const text = document.createElementNS(ns, 'text');
                text.setAttribute('x', frame.left);
                text.setAttribute('y', frame.top - padding - title + 14);
                text.style.fill = defaultColor;
                text.style.fontSize = '12px';
                text.textContent = group.name;
                svg.append(text);
            }

            for (const edge of host.$graphEdges) {
                const source = nodes[edge.from];
                const target = nodes[edge.to];
                if (!source || !target) {
                    continue;
                }
                const a = box(source);
                const b = box(target);
                let x1 = a.x, y1 = a.y, x2 = b.x, y2 = b.y;
                if (b.top >= a.bottom) {
                    y1 = a.bottom;
                    y2 = b.top;
                }
                else if (b.bottom <= a.top) {
                    y1 = a.top;
                    y2 = b.bottom;
                }
                else if (b.left >= a.right) {
                    x1 = a.right;
                    x2 = b.left;
                }
                else {
                    x1 = a.left;
                    x2 = b.right;
                }
                const color = edge.color || defaultColor;
                const line = document.createElementNS(ns, 'line');
                line.setAttribute('x1', x1);
                line.setAttribute('y1', y1);
                line.setAttribute('x2', x2);
                line.setAttribute('y2', y2);
                line.style.stroke = color;
                line.style.strokeWidth = '1';
                if (edge.line === 'DASHED') {
                    line.style.strokeDasharray = '5 4';
                }
                else if (edge.line === 'DOTTED') {
                    line.style.strokeDasharray = '1 3';
                }
                if (edge.tooltip) {
                    const hint = document.createElementNS(ns, 'title');
                    hint.textContent = edge.tooltip;
                    line.append(hint);
                    line.style.pointerEvents = 'stroke';
                    line.style.strokeWidth = '1';
                }
                const end = marker(defs, markers, edge.targetArrow, color);
                if (end) {
                    line.setAttribute('marker-end', end);
                }
                const start = marker(defs, markers, edge.sourceArrow, color);
                if (start) {
                    line.setAttribute('marker-start', start);
                }
                svg.append(line);

                if (edge.label) {
                    const text = document.createElementNS(ns, 'text');
                    text.setAttribute('x', (x1 + x2) / 2 + 4);
                    text.setAttribute('y', (y1 + y2) / 2);
                    text.style.fill = 'var(--vaadin-body-text-color, currentColor)';
                    text.style.fontSize = '11px';
                    text.textContent = edge.label;
                    svg.append(text);
                }
            }
        };

        if (host.$graphObserver) {
            host.$graphObserver.disconnect();
        }
        host.$graphObserver = new ResizeObserver(() => draw());
        host.$graphObserver.observe(host);
        host.querySelectorAll('[data-graph-node]').forEach(node => host.$graphObserver.observe(node));
        requestAnimationFrame(draw);
        """;

    private final GraphModel<E> myModel;

    private GraphNodeRender<E> myNodeRender = GraphNodeRender.defaultRender();
    private GraphEdgeRender<E> myEdgeRender = GraphEdgeRender.defaultRender();

    private final Div myCanvas = new Div();
    private final List<Div> myLayers = new ArrayList<>();
    private final Map<E, Div> myNodeDivs = new HashMap<>();
    private final List<E> mySelection = new ArrayList<>();

    public WebGraphImpl(GraphModel<E> model) {
        myModel = model;

        WebGraphVaadin component = toVaadinComponent();
        component.getStyle()
            .set("overflow", "auto")
            .set("width", "100%")
            .set("height", "100%")
            .set("box-sizing", "border-box");

        myCanvas.getStyle()
            .set("position", "relative")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("align-items", "center")
            .set("gap", "60px")
            .set("padding", "20px")
            .set("min-width", "max-content")
            .set("box-sizing", "border-box");
        component.add(myCanvas);

        myCanvas.getElement().addEventListener("click", event -> setSelection(List.of()))
            .setFilter("!event.target.closest('[data-graph-node]')");
        myCanvas.getElement().addEventListener("contextmenu", event -> setSelection(List.of()))
            .setFilter("!event.target.closest('[data-graph-node]')");

        rebuild();
    }

    @Override
    public List<E> getSelectedValues() {
        return List.copyOf(mySelection);
    }

    private void setSelection(List<E> selection) {
        mySelection.clear();
        mySelection.addAll(selection);

        for (Map.Entry<E, Div> entry : myNodeDivs.entrySet()) {
            entry.getValue().getStyle().set("outline", mySelection.contains(entry.getKey())
                ? "2px solid var(--vaadin-focus-ring-color, #3b82f6)"
                : "none");
        }
    }

    private void onNodeClick(E node, boolean toggle) {
        List<E> selection = new ArrayList<>(mySelection);
        if (toggle) {
            if (!selection.remove(node)) {
                selection.add(node);
            }
        }
        else {
            selection = List.of(node);
        }
        setSelection(selection);
    }

    @Override
    public WebGraphVaadin createVaadinComponent() {
        return new WebGraphVaadin(this);
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

    @RequiredUIAccess
    @Override
    public void refresh() {
        rebuild();
    }

    private void rebuild() {
        for (Div layer : myLayers) {
            myCanvas.remove(layer);
        }
        myLayers.clear();
        myNodeDivs.clear();
        mySelection.clear();

        List<List<E>> layers = LayeredGraphLayout.layers(myModel);

        Map<E, Integer> ids = new HashMap<>();
        for (List<E> nodes : layers) {
            Div layer = new Div();
            layer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "row")
                .set("align-items", "flex-start")
                .set("justify-content", "center")
                .set("gap", "40px");

            for (E node : nodes) {
                int id = ids.size();
                ids.put(node, id);
                layer.add(createNode(node, id));
            }

            myLayers.add(layer);
            myCanvas.add(layer);
        }

        ArrayNode edges = JsonNodeFactory.instance.arrayNode();
        for (Map.Entry<E, Integer> entry : ids.entrySet()) {
            for (E target : myModel.getArrows(entry.getKey())) {
                Integer targetId = ids.get(target);
                if (targetId == null || targetId.equals(entry.getValue())) {
                    continue;
                }

                GraphEdgeStyle style = GraphEdgeStyle.of(myEdgeRender, entry.getKey(), target);
                ObjectNode edge = edges.addObject();
                edge.put("from", entry.getValue());
                edge.put("to", targetId);
                edge.put("line", style.getLineStyle().name());
                edge.put("sourceArrow", style.getSourceArrow().name());
                edge.put("targetArrow", style.getTargetArrow().name());
                edge.put("label", style.getLabel().get());
                edge.put("tooltip", style.getTooltip().get());
                String color = WebColors.toCssColor(style.getColor());
                if (color != null) {
                    edge.put("color", color);
                }
            }
        }

        Map<GraphGroup, ArrayNode> groupMembers = new LinkedHashMap<>();
        for (Map.Entry<E, Integer> entry : ids.entrySet()) {
            GraphGroup group = myModel.getGroup(entry.getKey());
            if (group != null) {
                groupMembers.computeIfAbsent(group, it -> JsonNodeFactory.instance.arrayNode()).add(entry.getValue());
            }
        }
        ArrayNode groups = JsonNodeFactory.instance.arrayNode();
        for (Map.Entry<GraphGroup, ArrayNode> entry : groupMembers.entrySet()) {
            ObjectNode group = groups.addObject();
            group.put("name", entry.getKey().getName().get());
            group.set("members", entry.getValue());
        }

        myCanvas.getElement().executeJs(DRAW_EDGES_SCRIPT, edges.toString(), groups.toString());
    }

    private Div createNode(E value, int id) {
        GraphNodeContent<WebItemPresentationImpl> content = new GraphNodeContent<>(WebItemPresentationImpl::new);
        myNodeRender.render(content, RenderItem.of(value, false));

        Div header = new Div(content.getHeader().toComponent());
        header.getStyle()
            .set("padding", "6px 10px")
            .set("text-align", "center");

        Div node = new Div(header);
        node.getElement().setAttribute("data-graph-node", String.valueOf(id));

        String tooltip = content.getTooltip().get();
        if (!tooltip.isEmpty()) {
            node.getElement().setAttribute("title", tooltip);
        }

        node.getElement().addEventListener("click", event -> onNodeClick(value, event.getEventData().path("event.ctrlKey").asBoolean(false)))
            .addEventData("event.ctrlKey");
        node.getElement().addEventListener("contextmenu", event -> {
            if (!mySelection.contains(value)) {
                setSelection(List.of(value));
            }
        });
        myNodeDivs.put(value, node);
        node.getStyle()
            .set("border", "1px solid var(--consulo-component-border-color, lightgray)")
            .set("border-radius", "8px")
            .set("white-space", "nowrap")
            .set("overflow", "hidden")
            .set("background", "var(--vaadin-background-color, transparent)");

        for (List<WebItemPresentationImpl> section : content.getSections()) {
            Div rows = new Div();
            rows.getStyle()
                .set("border-top", "1px solid var(--consulo-component-border-color, lightgray)")
                .set("padding", "2px 0");
            for (WebItemPresentationImpl row : section) {
                Div rowDiv = new Div(row.toComponent());
                rowDiv.getStyle().set("padding", "2px 10px");
                rows.add(rowDiv);
            }
            node.add(rows);
        }
        return node;
    }
}
