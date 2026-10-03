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
package consulo.it;

import consulo.it.internal.ui.HeadlessTimeSeriesChart;
import consulo.ui.chart.StateRow;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeSeriesChart;
import consulo.ui.impl.chart.ChartTime;
import consulo.ui.impl.chart.StateRowImpl;
import consulo.ui.impl.chart.TimeAxisImpl;
import consulo.ui.impl.chart.TimeSeriesChartModel;
import consulo.ui.impl.chart.TimeSeriesImpl;
import consulo.ui.impl.chart.model.FakeTimer;
import consulo.ui.impl.chart.model.SeriesData;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class TimeAxisTester {
    private final TimeAxisImpl myAxis;
    private final FakeTimer myTimer;

    private TimeAxisTester(TimeAxisImpl axis, FakeTimer timer) {
        myAxis = axis;
        myTimer = timer;
    }

    public static TimeAxisTester of(TimeAxis axis) {
        if (axis instanceof TimeAxisImpl impl && impl.getUpdater().getTimer() instanceof FakeTimer timer) {
            return new TimeAxisTester(impl, timer);
        }
        throw new IllegalArgumentException("Not a headless time axis: " + axis);
    }

    public static TimeAxisTester create() {
        return of(TimeAxis.create());
    }

    public TimeAxis getAxis() {
        return myAxis;
    }

    public TimeAxisTester advance(Duration elapsed) {
        HeadlessUIThread.run(() -> myTimer.advance(elapsed.toNanos()));
        return this;
    }

    public TimeAxisTester step() {
        HeadlessUIThread.run(myTimer::step);
        return this;
    }

    public boolean isRunning() {
        return HeadlessUIThread.compute(myTimer::isRunning);
    }

    public Instant latest() {
        return HeadlessUIThread.compute(() -> ChartTime.fromMicros(myAxis.getTimeline().getDataRange().getMax()));
    }

    public static String dump(TimeSeriesChart chart) {
        TimeSeriesChartModel model = modelOf(chart);
        return HeadlessUIThread.compute(() -> {
            StringBuilder builder = new StringBuilder();
            for (TimeSeriesImpl series : model.getSeries()) {
                builder.append(series.getName().get())
                    .append(' ')
                    .append(series.getKind())
                    .append(' ')
                    .append(series.getData().size())
                    .append(' ')
                    .append(model.formatLatest(series))
                    .append('\n');
            }
            return builder.toString();
        });
    }

    public static double valueMax(TimeSeriesChart chart) {
        TimeSeriesChartModel model = modelOf(chart);
        return HeadlessUIThread.compute(() -> model.getYRange().getMax());
    }

    public static <S> @Nullable S stateAt(StateRow<S> row, Instant time) {
        if (!(row instanceof StateRowImpl<S> impl)) {
            throw new IllegalArgumentException("Not a headless state row: " + row);
        }

        long timeUs = ChartTime.toMicros(time);
        return HeadlessUIThread.compute(() -> {
            @Nullable S state = null;
            for (SeriesData<S> item : impl.getData().getData()) {
                if (item.x > timeUs) {
                    break;
                }
                state = item.value;
            }
            return state;
        });
    }

    private static TimeSeriesChartModel modelOf(TimeSeriesChart chart) {
        if (!(chart instanceof HeadlessTimeSeriesChart headless)) {
            throw new IllegalArgumentException("Not a headless time series chart: " + chart);
        }
        return headless.getModel();
    }
}
