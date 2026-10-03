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
package consulo.desktop.qt.ui.impl.chart;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
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
import consulo.ui.impl.chart.model.updater.Updatable;
import io.qt.widgets.QWidget;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtTimeSeriesChartImpl extends QtComponentDelegate<DesktopQtTimeSeriesChartWidget>
    implements TimeSeriesChart, TimeSeriesListener {
    private final Updatable myRepaint = elapsedNs -> repaint();
    private final TimeSeriesChartModel myModel;

    public DesktopQtTimeSeriesChartImpl(TimeAxis axis, ChartUnit unit) {
        myModel = new TimeSeriesChartModel(axis, unit, this, myRepaint);
    }

    @Override
    protected DesktopQtTimeSeriesChartWidget createQt(QWidget parent) {
        return new DesktopQtTimeSeriesChartWidget(parent, this);
    }

    void repaint() {
        DesktopQtTimeSeriesChartWidget widget = myComponent;
        if (widget != null && !widget.isDisposed()) {
            widget.update();
        }
    }

    TimeSeriesChartModel getModel() {
        return myModel;
    }

    @Override
    public void dataChanged(TimeSeriesImpl series) {
        repaint();
    }

    @Override
    public void colorChanged(TimeSeriesImpl series) {
        repaint();
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
        repaint();
        return series;
    }

    @Override
    public List<TimeSeries> getSeries() {
        return List.copyOf(myModel.getSeries());
    }
}
