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
package consulo.ui.impl.chart;

import consulo.localize.LocalizeValue;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeSeriesKind;
import consulo.ui.impl.chart.model.LineChartModel;
import consulo.ui.impl.chart.model.Range;
import consulo.ui.impl.chart.model.RangedContinuousSeries;
import consulo.ui.impl.chart.model.StreamingTimeline;
import consulo.ui.impl.chart.model.axis.ClampedAxisComponentModel;
import consulo.ui.impl.chart.model.axis.ResizingAxisComponentModel;
import consulo.ui.impl.chart.model.formatter.BaseAxisFormatter;
import consulo.ui.impl.chart.model.updater.Updatable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class TimeSeriesChartModel {
    private final TimeAxisImpl myAxis;
    private final ChartUnit myUnit;
    private final BaseAxisFormatter myFormatter;
    private final double myStorageScale;
    private final Range myYRange;
    private final LineChartModel myLineModel;
    private final ClampedAxisComponentModel myValueAxisModel;
    private final ResizingAxisComponentModel myTimeAxisModel;
    private final List<TimeSeriesImpl> mySeries = new ArrayList<>();
    private final TimeSeriesListener myListener;
    private final TimeAxisSubscription mySubscription;

    public TimeSeriesChartModel(TimeAxis axis, ChartUnit unit, TimeSeriesListener listener, Updatable... frameUpdatables) {
        myAxis = (TimeAxisImpl) axis;
        myLineModel = new LineChartModel(myAxis.getModelExecutor());
        myUnit = unit;
        myFormatter = ChartFormatters.forUnit(unit);
        myStorageScale = ChartFormatters.storageScale(unit);
        myYRange = unit == ChartUnit.PERCENT ? new Range(0, 100) : new Range(0, 0);
        myValueAxisModel = new ClampedAxisComponentModel.Builder(myYRange, myFormatter).build();
        myTimeAxisModel = myAxis.createTimeAxisModel();
        myListener = listener;

        List<Updatable> updatables = new ArrayList<>();
        updatables.add(myLineModel);
        updatables.add(myValueAxisModel);
        Collections.addAll(updatables, frameUpdatables);
        mySubscription = new TimeAxisSubscription(myAxis, updatables);
    }

    public TimeAxisImpl getAxis() {
        return myAxis;
    }

    public StreamingTimeline getTimeline() {
        return myAxis.getTimeline();
    }

    public ChartUnit getUnit() {
        return myUnit;
    }

    public BaseAxisFormatter getFormatter() {
        return myFormatter;
    }

    public double getStorageScale() {
        return myStorageScale;
    }

    public Range getYRange() {
        return myYRange;
    }

    public LineChartModel getLineModel() {
        return myLineModel;
    }

    public ClampedAxisComponentModel getValueAxisModel() {
        return myValueAxisModel;
    }

    public ResizingAxisComponentModel getTimeAxisModel() {
        return myTimeAxisModel;
    }

    public List<TimeSeriesImpl> getSeries() {
        return Collections.unmodifiableList(mySeries);
    }

    public boolean isActive() {
        return mySubscription.isActive();
    }

    public void activate() {
        mySubscription.activate();
    }

    public void deactivate() {
        mySubscription.deactivate();
    }

    public TimeSeriesImpl addSeries(LocalizeValue name, TimeSeriesKind kind) {
        StreamingTimeline timeline = myAxis.getTimeline();
        TimeSeriesData data = new TimeSeriesData();
        RangedContinuousSeries ranged = new RangedContinuousSeries(name.get(),
            timeline.getViewRange(),
            myYRange,
            data.asLongDataSeries(myStorageScale),
            timeline.getDataRange());
        TimeSeriesImpl series = new TimeSeriesImpl(myListener, name, kind, ChartPalette.series(mySeries.size()), data, ranged);
        mySeries.add(series);
        myLineModel.add(ranged);
        return series;
    }

    public String formatLatest(TimeSeriesImpl series) {
        TimeSeriesData data = series.getData();
        if (data.size() == 0) {
            return "-";
        }
        return myFormatter.getFormattedString(myYRange.getLength(), data.getValue(data.size() - 1) * myStorageScale, true);
    }

    public double timeAt(double ratio) {
        Range view = myAxis.getTimeline().getViewRange();
        return view.getMin() + Math.max(0, Math.min(1, ratio)) * view.getLength();
    }

    public void select(double fromRatio, double toRatio) {
        double from = timeAt(fromRatio);
        double to = timeAt(toRatio);
        myAxis.getTimeline().getSelectionRange().set(Math.min(from, to), Math.max(from, to));
    }

    public void clearSelection() {
        myAxis.getTimeline().getSelectionRange().clear();
    }

    public void handleMouseWheel(double count, boolean zoom, double anchor) {
        StreamingTimeline timeline = myAxis.getTimeline();
        if (zoom) {
            timeline.handleMouseWheelZoom(count, Math.max(0, Math.min(1, anchor)));
        }
        else {
            timeline.handleMouseWheelPan(count);
        }
    }
}
