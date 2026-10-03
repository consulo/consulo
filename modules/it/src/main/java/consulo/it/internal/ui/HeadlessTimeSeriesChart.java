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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeSeries;
import consulo.ui.chart.TimeSeriesChart;
import consulo.ui.chart.TimeSeriesKind;
import consulo.ui.impl.chart.TimeSeriesChartModel;
import consulo.ui.impl.chart.TimeSeriesImpl;
import consulo.ui.impl.chart.TimeSeriesListener;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessTimeSeriesChart extends HeadlessComponentBase implements TimeSeriesChart, TimeSeriesListener {
    private final TimeSeriesChartModel myModel;

    public HeadlessTimeSeriesChart(TimeAxis axis, ChartUnit unit) {
        myModel = new TimeSeriesChartModel(axis, unit, this);
        myModel.activate();
    }

    public TimeSeriesChartModel getModel() {
        return myModel;
    }

    public void setAttached(boolean attached) {
        if (attached) {
            myModel.activate();
        }
        else {
            myModel.deactivate();
        }
    }

    @Override
    public void dataChanged(TimeSeriesImpl series) {
    }

    @Override
    public void colorChanged(TimeSeriesImpl series) {
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
        return myModel.addSeries(name, kind);
    }

    @Override
    public List<TimeSeries> getSeries() {
        return List.copyOf(myModel.getSeries());
    }
}
