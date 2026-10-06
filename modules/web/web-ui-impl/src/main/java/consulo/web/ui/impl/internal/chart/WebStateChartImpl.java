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
import consulo.ui.chart.StateChart;
import consulo.ui.chart.StateRow;
import consulo.ui.chart.TimeAxis;
import consulo.ui.color.ColorValue;
import consulo.ui.impl.chart.StatePresentation;
import consulo.ui.impl.chart.StateRowImpl;
import consulo.ui.impl.chart.TimeAxisImpl;
import consulo.ui.impl.chart.TimeAxisSubscription;
import consulo.ui.impl.chart.model.Range;
import consulo.ui.impl.chart.model.SeriesData;
import consulo.ui.impl.chart.model.StreamingTimeline;
import consulo.ui.impl.chart.model.axis.ResizingAxisComponentModel;
import consulo.ui.impl.chart.model.updater.Updatable;
import consulo.ui.util.ColorValueUtil;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.vaadin.echart.WebEChartVaadin;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class WebStateChartImpl<S> extends VaadinComponentDelegate<WebEChartVaadin> implements StateChart<S> {
    private final TimeAxisImpl myAxis;
    private final ResizingAxisComponentModel myTimeAxisModel;
    private final Map<S, StatePresentation> myPresentations = new LinkedHashMap<>();
    private final List<StateRowImpl<S>> myRows = new ArrayList<>();
    private final Updatable myPush = new Updatable() {
        @Override
        public void update(long elapsedNs) {
        }

        @Override
        public void postUpdate() {
            push();
        }
    };

    private final TimeAxisSubscription mySubscription;

    public WebStateChartImpl(TimeAxis axis) {
        myAxis = (TimeAxisImpl) axis;
        myTimeAxisModel = myAxis.createTimeAxisModel();
        mySubscription = new TimeAxisSubscription(myAxis, List.of(myPush));

        WebEChartVaadin component = getVaadinComponent();
        component.addAttachListener(event -> {
            mySubscription.activate();
            push();
        });
        component.addDetachListener(event -> mySubscription.deactivate());
    }

    @Override
    public WebEChartVaadin createVaadinComponent() {
        return new WebEChartVaadin(this);
    }

    private void push() {
        if (!mySubscription.isActive()) {
            return;
        }
        StreamingTimeline timeline = myAxis.getTimeline();
        Range view = timeline.getViewRange();
        double zero = timeline.getDataRange().getMin();
        double dataMax = timeline.getDataRange().getMax();

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("type", "states");
        root.put("xMin", (view.getMin() - zero) / 1e6);
        root.put("xMax", (view.getMax() - zero) / 1e6);
        WebChartTicks.put(root, "xTicks", myTimeAxisModel, (view.getMin() - zero) / 1e6, view.getLength() / 1e6);

        ArrayNode rows = root.putArray("rows");
        for (StateRowImpl<S> row : myRows) {
            rows.add(row.getName().get());
        }

        List<S> stateOrder = new ArrayList<>(myPresentations.keySet());
        ArrayNode states = root.putArray("states");
        for (S state : stateOrder) {
            StatePresentation presentation = myPresentations.get(state);
            ObjectNode stateNode = states.addObject();
            stateNode.put("label", presentation.label().get());
            stateNode.put("color", ColorValueUtil.toCssColor(presentation.color()));
        }

        ArrayNode intervals = root.putArray("intervals");
        for (int rowIndex = 0; rowIndex < myRows.size(); rowIndex++) {
            List<SeriesData<S>> data = myRows.get(rowIndex).getData().getData();
            for (int i = 0; i < data.size(); i++) {
                SeriesData<S> item = data.get(i);
                double end = i + 1 < data.size() ? data.get(i + 1).x : dataMax;
                if (end < view.getMin()) {
                    continue;
                }
                if (item.x > view.getMax()) {
                    break;
                }
                ArrayNode interval = intervals.addArray();
                interval.add(rowIndex);
                interval.add((Math.max(item.x, view.getMin()) - zero) / 1e6);
                interval.add((Math.min(end, view.getMax()) - zero) / 1e6);
                interval.add(stateOrder.indexOf(item.value));
            }
        }

        getVaadinComponent().update(root);
    }

    @Override
    public TimeAxis getAxis() {
        return myAxis;
    }

    @RequiredUIAccess
    @Override
    public void setStatePresentation(S state, LocalizeValue label, ColorValue color) {
        myPresentations.put(state, new StatePresentation(label, color));
        push();
    }

    @RequiredUIAccess
    @Override
    public StateRow<S> addRow(LocalizeValue name) {
        StateRowImpl<S> row = new StateRowImpl<>(name, () -> {
        });
        myRows.add(row);
        push();
        return row;
    }

    @RequiredUIAccess
    @Override
    public void removeRow(StateRow<S> row) {
        if (myRows.remove(row)) {
            push();
        }
    }

    @Override
    public List<StateRow<S>> getRows() {
        return List.copyOf(myRows);
    }
}
