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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.FlameGraph;
import consulo.ui.chart.FlameGraphModel;
import consulo.ui.chart.StateChart;
import consulo.ui.chart.StateRow;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeSeries;
import consulo.ui.chart.TimeSeriesChart;
import consulo.ui.chart.TimeSeriesKind;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TwoComponentSplitLayout;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "charts", order = "after imageViewer")
public class ChartsUITesterTab implements UITesterTab {
    private enum ThreadState {
        RUNNING,
        WAITING,
        SLEEPING,
        BLOCKED
    }

    private record Frame(String name, long weight, List<Frame> children) {
        static Frame of(String name, long weight, Frame... children) {
            return new Frame(name, weight, List.of(children));
        }
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Charts");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        TabbedLayout tabs = TabbedLayout.create();
        tabs.addTab("Live", live(uiDisposable));
        tabs.addTab("Flame Graph", flameGraph());
        return tabs;
    }

    @RequiredUIAccess
    private static Component live(Disposable uiDisposable) {
        TimeAxis axis = TimeAxis.create();
        axis.setFollowLatest(Duration.ofSeconds(30));

        TimeSeriesChart cpuChart = TimeSeriesChart.create(axis, ChartUnit.PERCENT);
        TimeSeries cpu = cpuChart.addSeries(LocalizeValue.of("CPU"), TimeSeriesKind.AREA);

        TimeSeriesChart memoryChart = TimeSeriesChart.create(axis, ChartUnit.BYTES);
        TimeSeries used = memoryChart.addSeries(LocalizeValue.of("Used"), TimeSeriesKind.AREA);
        TimeSeries committed = memoryChart.addSeries(LocalizeValue.of("Committed"), TimeSeriesKind.LINE);

        StateChart<ThreadState> threadChart = StateChart.create(axis);
        threadChart.setStatePresentation(ThreadState.RUNNING, LocalizeValue.of("Running"), RGBColor.fromRGBValue(0x5FB865));
        threadChart.setStatePresentation(ThreadState.WAITING, LocalizeValue.of("Waiting"), RGBColor.fromRGBValue(0xE5C07B));
        threadChart.setStatePresentation(ThreadState.SLEEPING, LocalizeValue.of("Sleeping"), RGBColor.fromRGBValue(0x7FA7D9));
        threadChart.setStatePresentation(ThreadState.BLOCKED, LocalizeValue.of("Blocked"), RGBColor.fromRGBValue(0xD9534F));

        List<StateRow<ThreadState>> rows = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            rows.add(threadChart.addRow(LocalizeValue.of("Thread-" + i)));
        }

        Random random = new Random();
        ThreadState[] states = ThreadState.values();
        long[] memory = {256L << 20};

        ScheduledFuture<?> future = UIAccess.current().getScheduler().scheduleWithFixedDelay(() -> {
            Instant now = Instant.now();
            cpu.add(now, 15 + random.nextInt(70));

            memory[0] = Math.max(64L << 20, memory[0] + (random.nextInt(65) - 32) * (1L << 20));
            used.add(now, memory[0]);
            committed.add(now, 1L << 30);

            for (StateRow<ThreadState> row : rows) {
                if (random.nextInt(3) == 0) {
                    row.set(now, states[random.nextInt(states.length)]);
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
        Disposer.register(uiDisposable, () -> future.cancel(false));

        TwoComponentSplitLayout charts = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        charts.setFirstComponent(cpuChart);
        charts.setSecondComponent(memoryChart);

        TwoComponentSplitLayout root = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        root.setFirstComponent(charts);
        root.setSecondComponent(threadChart);
        root.setProportion(60);
        return root;
    }

    @RequiredUIAccess
    private static Component flameGraph() {
        Frame root = Frame.of("main", 1000,
            Frame.of("Application.run", 700,
                Frame.of("Parser.parse", 300,
                    Frame.of("Lexer.next", 180),
                    Frame.of("Parser.reduce", 90)),
                Frame.of("Renderer.paint", 350,
                    Frame.of("Graphics.drawString", 200),
                    Frame.of("Layout.measure", 120,
                        Frame.of("Font.width", 80)))),
            Frame.of("GC", 200),
            Frame.of("Idle", 100));

        FlameGraph<Frame> graph = FlameGraph.create(new FlameGraphModel<>() {
            @Override
            public Frame getRoot() {
                return root;
            }

            @Override
            public List<Frame> getChildren(Frame node) {
                return node.children();
            }

            @Override
            public long getWeight(Frame node) {
                return node.weight();
            }

            @Override
            public String getName(Frame node) {
                return node.name();
            }
        });

        Label status = Label.create(LocalizeValue.of("Click a frame to select it, double-click to focus"));
        graph.addSelectListener(event -> {
            Frame value = event.getValue();
            status.setText(LocalizeValue.of(value == null ? "Nothing selected" : "Selected: " + value.name()));
        });
        graph.addDoubleClickListener(event -> graph.focus(event.getValue()));

        return DockLayout.create().center(graph).bottom(status);
    }
}
