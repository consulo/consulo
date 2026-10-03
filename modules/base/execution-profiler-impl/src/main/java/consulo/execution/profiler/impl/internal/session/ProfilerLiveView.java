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
package consulo.execution.profiler.impl.internal.session;

import consulo.disposer.Disposable;
import consulo.execution.profiler.live.ProfilerMetric;
import consulo.execution.profiler.live.ProfilerMonitorSink;
import consulo.execution.profiler.live.ProfilerThreadState;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Separator;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.StateChart;
import consulo.ui.chart.StateRow;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeRange;
import consulo.ui.chart.TimeSeries;
import consulo.ui.chart.TimeSeriesChart;
import consulo.ui.color.RGBColor;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerLiveView implements ProfilerMonitorSink, Disposable {
    public static final String CPU_GROUP = "cpu";
    public static final String MEMORY_GROUP = "memory";

    private static final Logger LOG = Logger.getInstance(ProfilerLiveView.class);

    private static final Duration VISIBLE_WINDOW = Duration.ofMinutes(1);

    private static final int MAIN_CHARTS_PROPORTION = 50;

    private final Project myProject;
    private final ProfilerLiveHistory myHistory;
    private final TimeAxis myAxis;
    private final Map<String, TimeSeriesChart> myCharts = new LinkedHashMap<>();
    private final Map<String, TimeSeries> mySeries = new HashMap<>();
    private final StateChart<ProfilerThreadState> myThreadChart;
    private final Map<Long, StateRow<ProfilerThreadState>> myThreadRows = new HashMap<>();
    private final @Nullable VerticalLayout myCompactChartsLayout;
    private final DockLayout myRoot;
    private final Queue<Runnable> myPendingUpdates = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean myFlushScheduled = new AtomicBoolean();

    private volatile boolean myDisposed;

    @RequiredUIAccess
    public ProfilerLiveView(Project project, ProfilerSessionLayout layout, ProfilerLiveHistory history) {
        myProject = project;
        myHistory = history;

        Instant dataStart = history.getFirstTime();
        myAxis = dataStart == null ? TimeAxis.create() : TimeAxis.create(dataStart);
        myAxis.setFollowLatest(VISIBLE_WINDOW);

        TimeSeriesChart cpuChart = TimeSeriesChart.create(myAxis, ChartUnit.PERCENT);
        TimeSeriesChart memoryChart = TimeSeriesChart.create(myAxis, ChartUnit.BYTES);
        myCharts.put(CPU_GROUP, cpuChart);
        myCharts.put(MEMORY_GROUP, memoryChart);

        myThreadChart = StateChart.create(myAxis);
        setStatePresentation(ProfilerThreadState.RUNNING, LocalizeValue.localizeTODO("Running"), 0x5FB865);
        setStatePresentation(ProfilerThreadState.RUNNING_NATIVE, LocalizeValue.localizeTODO("Running native"), 0x3F8F8A);
        setStatePresentation(ProfilerThreadState.SLEEPING, LocalizeValue.localizeTODO("Sleeping"), 0x7FA7D9);
        setStatePresentation(ProfilerThreadState.WAITING, LocalizeValue.localizeTODO("Waiting"), 0xE5C07B);
        setStatePresentation(ProfilerThreadState.BLOCKED, LocalizeValue.localizeTODO("Blocked"), 0xD9534F);
        setStatePresentation(ProfilerThreadState.TERMINATED, LocalizeValue.localizeTODO("Terminated"), 0x9E9E9E);

        myRoot = DockLayout.create(Space.NONE);

        if (layout == ProfilerSessionLayout.COMPACT) {
            myCompactChartsLayout = VerticalLayout.create(Space.NONE);

            TwoComponentSplitLayout mainCharts = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
            mainCharts.setFirstComponent(cpuChart);
            mainCharts.setSecondComponent(memoryChart);
            mainCharts.setProportion(MAIN_CHARTS_PROPORTION);

            VerticalLayout compact = VerticalLayout.create(Space.NONE);
            compact.add(mainCharts);
            compact.add(Separator.horizontal());
            compact.add(myCompactChartsLayout);
            compact.add(FoldoutLayout.create(LocalizeValue.localizeTODO("Threads"), myThreadChart, true));

            myRoot.center(ScrollableLayout.create(
                compact,
                ScrollableLayoutOptions.builder()
                    .horizontalScrollPolicy(ScrollableLayoutOptions.ScrollPolicy.NEVER)
                    .build()
            ));
        }
        else {
            myCompactChartsLayout = null;
            relayoutFull();
        }
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void stopFollowingLatest() {
        if (myDisposed || !myAxis.isFollowingLatest()) {
            return;
        }

        Instant lastTime = myHistory.getLastTime();
        if (lastTime != null) {
            myAxis.setVisible(new TimeRange(lastTime.minus(VISIBLE_WINDOW), lastTime));
        }
    }

    @Override
    public void metric(ProfilerMetric metric, Instant time, double value) {
        enqueue(() -> applyMetric(metric, time, value));
    }

    @Override
    public void threadState(long threadId, String threadName, Instant time, ProfilerThreadState state) {
        enqueue(() -> applyThreadState(threadId, threadName, time, state));
    }

    @Override
    public void dispose() {
        myDisposed = true;
        myPendingUpdates.clear();
    }

    private void enqueue(Runnable update) {
        if (myDisposed) {
            return;
        }

        myPendingUpdates.add(update);
        if (myFlushScheduled.compareAndSet(false, true)) {
            myProject.getUIAccess().give(this::flush);
        }
    }

    @RequiredUIAccess
    private void flush() {
        myFlushScheduled.set(false);
        if (myDisposed || myProject.isDisposed()) {
            myPendingUpdates.clear();
            return;
        }

        Runnable update;
        while ((update = myPendingUpdates.poll()) != null) {
            try {
                update.run();
            }
            catch (Throwable e) {
                LOG.error("Failed to apply a live profiler value", e);
            }
        }
    }

    @RequiredUIAccess
    private void applyMetric(ProfilerMetric metric, Instant time, double value) {
        TimeSeries series = mySeries.get(metric.id());
        if (series == null) {
            TimeSeriesChart chart = myCharts.get(metric.group());
            if (chart == null) {
                chart = TimeSeriesChart.create(myAxis, metric.unit());
                myCharts.put(metric.group(), chart);
                addExtraChart(chart);
            }

            series = chart.addSeries(metric.name(), metric.kind());
            mySeries.put(metric.id(), series);
        }

        series.add(time, value);
    }

    @RequiredUIAccess
    private void applyThreadState(long threadId, String threadName, Instant time, ProfilerThreadState state) {
        StateRow<ProfilerThreadState> row = myThreadRows.get(threadId);
        if (row == null) {
            row = myThreadChart.addRow(LocalizeValue.of(threadName));
            myThreadRows.put(threadId, row);
        }

        row.set(time, state);
    }

    @RequiredUIAccess
    private void addExtraChart(TimeSeriesChart chart) {
        VerticalLayout compactCharts = myCompactChartsLayout;
        if (compactCharts == null) {
            relayoutFull();
            return;
        }

        compactCharts.add(chart);
        compactCharts.add(Separator.horizontal());
    }

    @RequiredUIAccess
    private void relayoutFull() {
        List<Component> rows = new ArrayList<>(myCharts.values());
        rows.add(myThreadChart);

        myRoot.removeAll();
        myRoot.center(split(rows, 0));
    }

    @RequiredUIAccess
    private static Component split(List<Component> rows, int from) {
        int count = rows.size() - from;
        if (count == 1) {
            return rows.get(from);
        }

        TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        layout.setFirstComponent(rows.get(from));
        layout.setSecondComponent(split(rows, from + 1));
        layout.setProportion(100 / count);
        return layout;
    }

    @RequiredUIAccess
    private void setStatePresentation(ProfilerThreadState state, LocalizeValue label, int rgb) {
        myThreadChart.setStatePresentation(state, label, RGBColor.fromRGBValue(rgb));
    }
}
