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
package consulo.web.ui.impl.internal.chart;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.FlameGraph;
import consulo.ui.chart.FlameGraphDoubleClickEvent;
import consulo.ui.chart.FlameGraphModel;
import consulo.ui.chart.FlameGraphOrientation;
import consulo.ui.chart.FlameGraphSelectEvent;
import consulo.ui.color.ColorValue;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.impl.chart.ChartFormatters;
import consulo.ui.impl.chart.ChartPalette;
import consulo.ui.impl.chart.FlameGraphIndex;
import consulo.ui.impl.chart.FlameGraphNode;
import consulo.ui.util.ColorValueUtil;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.vaadin.echart.WebEChartVaadin;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class WebFlameGraphImpl<E> extends VaadinComponentDelegate<WebEChartVaadin> implements FlameGraph<E> {
    private static final double MIN_VISIBLE_FRACTION = 1.0 / 4000;

    private final FlameGraphModel<E> myModel;
    private final FlameGraphIndex<E> myIndex;
    private final List<FlameGraphNode<E>> myPushed = new ArrayList<>();

    private FlameGraphOrientation myOrientation = FlameGraphOrientation.FLAME;
    private @Nullable E myFocused;
    private @Nullable E mySelected;
    private @Nullable Predicate<E> myHighlight;

    public WebFlameGraphImpl(FlameGraphModel<E> model) {
        myModel = model;
        myIndex = new FlameGraphIndex<>(model);

        WebEChartVaadin component = getVaadinComponent();
        component.addAttachListener(event -> push());
        component.getElement().addEventListener("consulo-chart-select", event -> {
            FlameGraphNode<E> node = nodeById(event.getEventData().path("event.detail.id").asInt(-1));
            mySelected = node == null ? null : node.getValue();
            push();
            getListenerDispatcher(FlameGraphSelectEvent.class)
                .onEvent(new FlameGraphSelectEvent<>(this, mySelected, ProgrammaticInputDetails.INSTANCE));
        }).addEventData("event.detail.id");
        component.getElement().addEventListener("consulo-chart-dblclick", event -> {
            FlameGraphNode<E> node = nodeById(event.getEventData().path("event.detail.id").asInt(-1));
            if (node != null) {
                getListenerDispatcher(FlameGraphDoubleClickEvent.class)
                    .onEvent(new FlameGraphDoubleClickEvent<>(this, node.getValue(), ProgrammaticInputDetails.INSTANCE));
            }
        }).addEventData("event.detail.id");
    }

    @Override
    public WebEChartVaadin createVaadinComponent() {
        return new WebEChartVaadin(this);
    }

    private @Nullable FlameGraphNode<E> nodeById(int id) {
        return id >= 0 && id < myPushed.size() ? myPushed.get(id) : null;
    }

    private void push() {
        FlameGraphNode<E> rootNode = myIndex.getRoot();
        FlameGraphNode<E> focused = myIndex.getNode(myFocused);
        FlameGraphNode<E> focus = focused == null ? rootNode : focused;
        long xMin = focus.getStart();
        long xMax = Math.max(focus.getEnd(), focus.getStart() + 1);
        double minWidth = (xMax - xMin) * MIN_VISIBLE_FRACTION;

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("type", "flame");
        root.put("orientation", myOrientation.name());
        root.put("xMin", xMin);
        root.put("xMax", xMax);
        root.put("maxDepth", myIndex.getMaxDepth());

        myPushed.clear();
        ArrayNode nodes = root.putArray("nodes");
        addNode(nodes, rootNode, xMin, xMax, minWidth);

        getVaadinComponent().update(root);
    }

    private void addNode(ArrayNode nodes, FlameGraphNode<E> node, long xMin, long xMax, double minWidth) {
        long start = Math.max(node.getStart(), xMin);
        long end = Math.min(node.getEnd(), xMax);
        if (end - start < minWidth || end <= start) {
            return;
        }

        E value = node.getValue();
        String name = myModel.getName(value);
        ColorValue modelColor = myModel.getColor(value);

        ArrayNode item = nodes.addArray();
        item.add(myPushed.size());
        item.add(node.getDepth());
        item.add(start);
        item.add(end);
        item.add(name);
        item.add(ColorValueUtil.toCssColor(modelColor != null ? modelColor : ChartPalette.flame(name)));
        item.add(myHighlight != null && !myHighlight.test(value));
        item.add(value.equals(mySelected));
        item.add(tooltip(node));
        myPushed.add(node);

        for (FlameGraphNode<E> child : node.getChildren()) {
            addNode(nodes, child, xMin, xMax, minWidth);
        }
    }

    private String tooltip(FlameGraphNode<E> node) {
        long total = myIndex.getRoot().getDuration();
        long weight = node.getDuration();
        double scale = ChartFormatters.storageScale(myModel.getWeightUnit());
        String value = ChartFormatters.forUnit(myModel.getWeightUnit()).getFormattedString(total * scale, weight * scale, true);
        double percent = total == 0 ? 0 : weight * 100.0 / total;
        return String.format("%s<br>%s (%.2f%%)", escape(myModel.getName(node.getValue())), escape(value), percent);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public FlameGraphModel<E> getModel() {
        return myModel;
    }

    @RequiredUIAccess
    @Override
    public void setOrientation(FlameGraphOrientation orientation) {
        myOrientation = orientation;
        push();
    }

    @Override
    public FlameGraphOrientation getOrientation() {
        return myOrientation;
    }

    @RequiredUIAccess
    @Override
    public void focus(@Nullable E node) {
        myFocused = myIndex.retain(node);
        push();
    }

    @Override
    public @Nullable E getFocused() {
        return myFocused;
    }

    @RequiredUIAccess
    @Override
    public void setHighlight(@Nullable Predicate<E> filter) {
        myHighlight = filter;
        push();
    }

    @Override
    public @Nullable E getSelectedValue() {
        return mySelected;
    }

    @RequiredUIAccess
    @Override
    public void refresh() {
        myIndex.rebuild();
        myFocused = myIndex.retain(myFocused);
        mySelected = myIndex.retain(mySelected);
        push();
    }
}
