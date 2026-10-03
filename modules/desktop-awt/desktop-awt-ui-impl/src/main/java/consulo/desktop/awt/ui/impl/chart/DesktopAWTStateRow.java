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

import consulo.desktop.awt.ui.impl.adtui.chart.statechart.StateChart;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.StateRow;
import consulo.ui.impl.chart.ChartTime;
import consulo.ui.impl.chart.StatePresentation;
import consulo.ui.impl.chart.StateRowData;
import consulo.ui.impl.chart.model.RangedSeries;
import consulo.ui.impl.chart.model.StateChartModel;
import consulo.ui.impl.chart.model.StreamingTimeline;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopAWTStateRow<S> implements StateRow<S> {
    private final DesktopAWTStateChartImpl<S> myChart;
    private final LocalizeValue myName;
    private final StateRowData<S> myData = new StateRowData<>();
    private final RangedSeries<S> myRanged;
    private final StateChartModel<S> myModel = new StateChartModel<>();
    private final StateChart<S> myComponent;

    DesktopAWTStateRow(DesktopAWTStateChartImpl<S> chart, LocalizeValue name) {
        myChart = chart;
        myName = name;

        StreamingTimeline timeline = chart.getTimeAxis().getTimeline();
        myRanged = new RangedSeries<>(timeline.getViewRange(), myData.asDataSeries(), timeline.getDataRange());
        myModel.addSeries(myRanged);

        myComponent = new StateChart<>(myModel, chart.getColorProvider());
        myComponent.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                S state = myComponent.itemAtMouse(e.getPoint());
                StatePresentation presentation = state == null ? null : myChart.getPresentation(state);
                myComponent.setToolTipText(presentation == null ? null : presentation.label().get());
            }
        });
    }

    StateChart<S> getComponent() {
        return myComponent;
    }

    void repaint() {
        myComponent.repaint();
    }

    @Override
    public LocalizeValue getName() {
        return myName;
    }

    @RequiredUIAccess
    @Override
    public void set(Instant time, S state) {
        if (myData.set(ChartTime.toMicros(time), state)) {
            myRanged.invalidate();
            myModel.changed(StateChartModel.Aspect.MODEL_CHANGED);
        }
    }
}
