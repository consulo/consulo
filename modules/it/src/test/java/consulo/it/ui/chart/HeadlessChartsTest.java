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
package consulo.it.ui.chart;

import consulo.it.FlameGraphTester;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.HeadlessUIThread;
import consulo.it.TimeAxisTester;
import consulo.it.internal.ui.HeadlessTimeSeriesChart;
import consulo.localize.LocalizeValue;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.FlameGraph;
import consulo.ui.chart.FlameGraphModel;
import consulo.ui.chart.StateChart;
import consulo.ui.chart.StateRow;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeSeries;
import consulo.ui.chart.TimeSeriesChart;
import consulo.ui.chart.TimeSeriesKind;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class HeadlessChartsTest {
    private static final class MapFlameGraphModel implements FlameGraphModel<String> {
        private final Map<String, List<String>> myChildren = new ConcurrentHashMap<>();
        private final Map<String, Long> myWeights = new ConcurrentHashMap<>();

        MapFlameGraphModel put(String frame, long weight, String... children) {
            myWeights.put(frame, weight);
            myChildren.put(frame, List.of(children));
            return this;
        }

        @Override
        public String getRoot() {
            return "main";
        }

        @Override
        public List<String> getChildren(String node) {
            return myChildren.getOrDefault(node, List.of());
        }

        @Override
        public long getWeight(String node) {
            return myWeights.getOrDefault(node, 0L);
        }

        @Override
        public String getName(String node) {
            return node;
        }
    }

    @Test
    public void advancingTheTimerMovesTheLatestTimeOfTheAxis() {
        TimeAxisTester tester = TimeAxisTester.create();
        HeadlessUIThread.compute(() -> TimeSeriesChart.create(tester.getAxis(), ChartUnit.PERCENT));
        Instant start = tester.latest();

        tester.advance(Duration.ofSeconds(2));
        assertThat(Duration.between(start, tester.latest())).isEqualTo(Duration.ofSeconds(2));

        tester.advance(Duration.ofMillis(500));
        assertThat(Duration.between(start, tester.latest())).isEqualTo(Duration.ofMillis(2500));
    }

    @Test
    public void anAxisNobodyShowsStandsStillAndCatchesUpOnceAChartShowsIt() {
        TimeAxisTester tester = TimeAxisTester.create();
        Instant start = tester.latest();
        assertThat(tester.isRunning()).isFalse();

        tester.advance(Duration.ofSeconds(3));
        assertThat(tester.latest()).isEqualTo(start);

        HeadlessUIThread.compute(() -> TimeSeriesChart.create(tester.getAxis(), ChartUnit.COUNT));

        assertThat(tester.isRunning()).isTrue();
        assertThat(Duration.between(start, tester.latest())).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    public void theLastChartLeavingStopsTheAxisUntilOneComesBack() {
        TimeAxisTester tester = TimeAxisTester.create();
        HeadlessTimeSeriesChart first = (HeadlessTimeSeriesChart) HeadlessUIThread.compute(
            () -> TimeSeriesChart.create(tester.getAxis(), ChartUnit.COUNT)
        );
        HeadlessTimeSeriesChart second = (HeadlessTimeSeriesChart) HeadlessUIThread.compute(
            () -> TimeSeriesChart.create(tester.getAxis(), ChartUnit.BYTES)
        );
        Instant start = tester.latest();

        HeadlessUIThread.run(() -> first.setAttached(false));
        assertThat(tester.isRunning()).isTrue();

        HeadlessUIThread.run(() -> second.setAttached(false));
        assertThat(tester.isRunning()).isFalse();

        tester.advance(Duration.ofSeconds(4));
        assertThat(tester.latest()).isEqualTo(start);

        HeadlessUIThread.run(() -> second.setAttached(true));
        assertThat(tester.isRunning()).isTrue();
        assertThat(Duration.between(start, tester.latest())).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    public void theLatestValueOfASeriesIsFormattedInTheUnitOfItsChart() {
        TimeAxisTester tester = TimeAxisTester.create();
        TimeAxis axis = tester.getAxis();
        TimeSeriesChart cpuChart = HeadlessUIThread.compute(() -> TimeSeriesChart.create(axis, ChartUnit.PERCENT));
        TimeSeriesChart memoryChart = HeadlessUIThread.compute(() -> TimeSeriesChart.create(axis, ChartUnit.BYTES));
        TimeSeriesChart eventChart = HeadlessUIThread.compute(() -> TimeSeriesChart.create(axis, ChartUnit.COUNT));
        Instant now = tester.latest();

        HeadlessUIThread.run(() -> {
            TimeSeries cpu = cpuChart.addSeries(LocalizeValue.of("CPU"), TimeSeriesKind.AREA);
            cpuChart.addSeries(LocalizeValue.of("Idle"), TimeSeriesKind.LINE);
            cpu.add(now, 15);
            cpu.add(now.plusMillis(500), 42);

            TimeSeries used = memoryChart.addSeries(LocalizeValue.of("Used"), TimeSeriesKind.AREA);
            used.add(now, 512L << 20);

            TimeSeries events = eventChart.addSeries(LocalizeValue.of("Events"), TimeSeriesKind.LINE);
            events.add(now, 7);
        });

        assertThat(TimeAxisTester.dump(cpuChart)).isEqualTo("""
            CPU AREA 2 42 %
            Idle LINE 0 -
            """);
        assertThat(TimeAxisTester.dump(memoryChart)).isEqualTo("Used AREA 1 512 MB\n");
        assertThat(TimeAxisTester.dump(eventChart)).isEqualTo("Events LINE 1 7\n");
        assertThat(cpuChart.getSeries()).extracting(TimeSeries::getName)
            .containsExactly(LocalizeValue.of("CPU"), LocalizeValue.of("Idle"));
    }

    @Test
    public void theValueRangeOfAChartFollowsItsDataOnTheNextTick() {
        TimeAxisTester tester = TimeAxisTester.create();
        TimeSeriesChart memoryChart = HeadlessUIThread.compute(() -> TimeSeriesChart.create(tester.getAxis(), ChartUnit.BYTES));
        TimeSeriesChart cpuChart = HeadlessUIThread.compute(() -> TimeSeriesChart.create(tester.getAxis(), ChartUnit.PERCENT));
        Instant now = tester.latest();
        long used = 300L << 20;

        HeadlessUIThread.run(() -> memoryChart.addSeries(LocalizeValue.of("Used"), TimeSeriesKind.AREA).add(now, used));
        HeadlessUIThread.run(() -> cpuChart.addSeries(LocalizeValue.of("CPU"), TimeSeriesKind.AREA).add(now, 30));
        assertThat(TimeAxisTester.valueMax(memoryChart)).isZero();
        assertThat(TimeAxisTester.valueMax(cpuChart)).isEqualTo(100);

        tester.advance(Duration.ofSeconds(1));

        assertThat(TimeAxisTester.valueMax(memoryChart)).isGreaterThanOrEqualTo(used);
        assertThat(TimeAxisTester.valueMax(cpuChart)).isEqualTo(100);
    }

    @Test
    public void aStateRowAnswersTheStateInForceAtATime() {
        TimeAxisTester tester = TimeAxisTester.create();
        StateChart<String> chart = HeadlessUIThread.compute(() -> StateChart.create(tester.getAxis()));
        Instant start = tester.latest();

        StateRow<String> main = HeadlessUIThread.compute(() -> chart.addRow(LocalizeValue.of("main")));
        StateRow<String> worker = HeadlessUIThread.compute(() -> chart.addRow(LocalizeValue.of("worker")));
        HeadlessUIThread.run(() -> {
            main.set(start, "RUNNING");
            main.set(start.plusSeconds(1), "RUNNING");
            main.set(start.plusSeconds(2), "WAITING");
            main.set(start.plusSeconds(1), "BLOCKED");
            worker.set(start.plusSeconds(1), "SLEEPING");
        });

        assertThat(TimeAxisTester.stateAt(main, start.minusMillis(1))).isNull();
        assertThat(TimeAxisTester.stateAt(main, start)).isEqualTo("RUNNING");
        assertThat(TimeAxisTester.stateAt(main, start.plusMillis(1500))).isEqualTo("RUNNING");
        assertThat(TimeAxisTester.stateAt(main, start.plusSeconds(5))).isEqualTo("WAITING");
        assertThat(TimeAxisTester.stateAt(worker, start)).isNull();
        assertThat(TimeAxisTester.stateAt(worker, start.plusSeconds(1))).isEqualTo("SLEEPING");

        HeadlessUIThread.run(() -> chart.removeRow(main));
        assertThat(chart.getRows()).extracting(StateRow::getName).containsExactly(LocalizeValue.of("worker"));
    }

    @Test
    public void aFlameGraphLaysOutTheModelAndReportsWhatTheUserPicks() {
        MapFlameGraphModel model = new MapFlameGraphModel()
            .put("main", 100, "run", "gc")
            .put("run", 70, "parse", "paint")
            .put("parse", 30)
            .put("paint", 40)
            .put("gc", 30);
        FlameGraph<String> graph = HeadlessUIThread.compute(() -> FlameGraph.create(model));
        FlameGraphTester<String> tester = FlameGraphTester.of(graph);
        List<@Nullable String> selected = new CopyOnWriteArrayList<>();
        List<String> doubleClicked = new CopyOnWriteArrayList<>();
        graph.addSelectListener(event -> selected.add(event.getValue()));
        graph.addDoubleClickListener(event -> doubleClicked.add(event.getValue()));

        tester.assertStructure("""
            main 100
             run 70
              parse 30
              paint 40
             gc 30
            """);
        assertThat(tester.getMaxDepth()).isEqualTo(2);

        tester.userSelect("parse");
        tester.userSelect("unknown");
        tester.userSelect("paint");
        tester.userDoubleClick("run");
        tester.userDoubleClick("unknown");

        assertThat(selected).containsExactly("parse", null, "paint");
        assertThat(doubleClicked).containsExactly("run");
        tester.assertStructure("""
            main 100
             run 70
              parse 30
              [paint] 40
             gc 30
            """);
    }

    @Test
    public void focusAndHighlightOnlyKeepFramesOfTheGraph() {
        MapFlameGraphModel model = new MapFlameGraphModel()
            .put("main", 100, "run", "gc")
            .put("run", 70, "parse", "paint")
            .put("parse", 30)
            .put("paint", 40)
            .put("gc", 30);
        FlameGraph<String> graph = HeadlessUIThread.compute(() -> FlameGraph.create(model));
        FlameGraphTester<String> tester = FlameGraphTester.of(graph);

        HeadlessUIThread.run(() -> graph.focus("run"));
        HeadlessUIThread.run(() -> graph.setHighlight(frame -> frame.startsWith("p")));
        tester.userSelect("parse");

        tester.assertStructure("""
            ~main 100
             ~run 70 *
              [parse] 30
              paint 40
             ~gc 30
            """);

        HeadlessUIThread.run(() -> graph.focus("unknown"));
        HeadlessUIThread.run(() -> graph.setHighlight(null));
        assertThat(graph.getFocused()).isNull();
        tester.assertStructure("""
            main 100
             run 70
              [parse] 30
              paint 40
             gc 30
            """);
    }

    @Test
    public void refreshRebuildsTheGraphAndDropsFramesWhichAreGone() {
        MapFlameGraphModel model = new MapFlameGraphModel()
            .put("main", 100, "run", "gc")
            .put("run", 70, "parse", "paint")
            .put("parse", 30)
            .put("paint", 40)
            .put("gc", 30);
        FlameGraph<String> graph = HeadlessUIThread.compute(() -> FlameGraph.create(model));
        FlameGraphTester<String> tester = FlameGraphTester.of(graph);
        HeadlessUIThread.run(() -> graph.focus("run"));
        tester.userSelect("parse");

        model.put("run", 50, "paint").put("paint", 50).put("main", 120, "run", "gc", "io").put("io", 40);
        HeadlessUIThread.run(graph::refresh);

        assertThat(graph.getSelectedValue()).isNull();
        assertThat(graph.getFocused()).isEqualTo("run");
        tester.assertStructure("""
            main 120
             run 50 *
              paint 50
             gc 30
             io 40
            """);

        model.put("main", 120, "gc", "io");
        HeadlessUIThread.run(graph::refresh);

        assertThat(graph.getFocused()).isNull();
        assertThat(tester.getMaxDepth()).isEqualTo(1);
    }
}
