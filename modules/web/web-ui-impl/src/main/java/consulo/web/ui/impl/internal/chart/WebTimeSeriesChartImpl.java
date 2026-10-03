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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeSeries;
import consulo.ui.chart.TimeSeriesChart;
import consulo.ui.chart.TimeSeriesKind;
import consulo.ui.impl.chart.TimeSeriesChartModel;
import consulo.ui.impl.chart.TimeSeriesData;
import consulo.ui.impl.chart.TimeSeriesImpl;
import consulo.ui.impl.chart.TimeSeriesListener;
import consulo.ui.impl.chart.model.Range;
import consulo.ui.impl.chart.model.StreamingTimeline;
import consulo.ui.impl.chart.model.updater.Updatable;
import consulo.ui.util.ColorValueUtil;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.vaadin.echart.WebEChartVaadin;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class WebTimeSeriesChartImpl extends VaadinComponentDelegate<WebEChartVaadin> implements TimeSeriesChart, TimeSeriesListener {
    private final Updatable myPush = new Updatable() {
        @Override
        public void update(long elapsedNs) {
        }

        @Override
        public void postUpdate() {
            push();
        }
    };
    private final TimeSeriesChartModel myModel;

    public WebTimeSeriesChartImpl(TimeAxis axis, ChartUnit unit) {
        myModel = new TimeSeriesChartModel(axis, unit, this, myPush);

        WebEChartVaadin component = getVaadinComponent();
        component.addAttachListener(event -> {
            myModel.activate();
            push();
        });
        component.addDetachListener(event -> myModel.deactivate());
        component.getElement().addEventListener("consulo-chart-wheel", event -> {
            JsonNode data = event.getEventData();
            myModel.handleMouseWheel(data.path("event.detail.count").asDouble(0),
                data.path("event.detail.zoom").asBoolean(false),
                data.path("event.detail.anchor").asDouble(0.5));
        }).addEventData("event.detail.count").addEventData("event.detail.anchor").addEventData("event.detail.zoom");
        component.getElement().addEventListener("consulo-chart-selection", event -> {
            JsonNode data = event.getEventData();
            double from = data.path("event.detail.from").asDouble(-1);
            double to = data.path("event.detail.to").asDouble(-1);
            if (from < 0 || to < 0) {
                myModel.clearSelection();
            }
            else {
                myModel.select(from, to);
            }
            push();
        }).addEventData("event.detail.from").addEventData("event.detail.to");
    }

    @Override
    public WebEChartVaadin createVaadinComponent() {
        return new WebEChartVaadin(this);
    }

    private void push() {
        if (!myModel.isActive()) {
            return;
        }
        StreamingTimeline timeline = myModel.getTimeline();
        Range view = timeline.getViewRange();
        Range yRange = myModel.getYRange();
        double zero = timeline.getDataRange().getMin();
        double scale = myModel.getStorageScale();

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("type", "timeSeries");
        root.put("xMin", (view.getMin() - zero) / 1e6);
        root.put("xMax", (view.getMax() - zero) / 1e6);
        WebChartTicks.put(root, "xTicks", myModel.getTimeAxisModel(), (view.getMin() - zero) / 1e6, view.getLength() / 1e6);
        if (yRange.getMax() > 0) {
            root.put("yMax", yRange.getMax() / scale);
        }
        else {
            root.putNull("yMax");
        }
        WebChartTicks.put(root, "yTicks", myModel.getValueAxisModel(), yRange.getMin() / scale, yRange.getLength() / scale);

        Range selection = timeline.getSelectionRange();
        if (selection.isEmpty()) {
            root.putNull("selection");
        }
        else {
            ArrayNode selectionNode = root.putArray("selection");
            selectionNode.add((selection.getMin() - zero) / 1e6);
            selectionNode.add((selection.getMax() - zero) / 1e6);
        }

        ArrayNode seriesArray = root.putArray("series");
        for (TimeSeriesImpl series : myModel.getSeries()) {
            TimeSeriesData data = series.getData();
            ObjectNode seriesNode = seriesArray.addObject();
            seriesNode.put("name", series.getName().get());
            seriesNode.put("kind", series.getKind().name());
            seriesNode.put("color", ColorValueUtil.toHtmlColor(series.getColor()));
            seriesNode.put("last", myModel.formatLatest(series));

            ArrayNode points = seriesNode.putArray("data");
            if (data.size() > 0) {
                int from = data.firstIndexAtOrBefore(view.getMin());
                int to = data.lastIndexAtOrAfter(view.getMax());
                for (int i = from; i <= to; i++) {
                    ArrayNode point = points.addArray();
                    point.add((data.getTime(i) - zero) / 1e6);
                    point.add(data.getValue(i));
                }
            }
        }

        getVaadinComponent().update(root);
    }

    @Override
    public void dataChanged(TimeSeriesImpl series) {
    }

    @Override
    public void colorChanged(TimeSeriesImpl series) {
        push();
    }

    @Override
    public TimeAxis getAxis() {
        return myModel.getAxis();
    }

    @Override
    public ChartUnit getUnit() {
        return myModel.getUnit();
    }

    @RequiredUIAccess
    @Override
    public TimeSeries addSeries(LocalizeValue name, TimeSeriesKind kind) {
        TimeSeriesImpl series = myModel.addSeries(name, kind);
        push();
        return series;
    }

    @Override
    public List<TimeSeries> getSeries() {
        return List.copyOf(myModel.getSeries());
    }
}
