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
package consulo.execution.profiler.live;

import consulo.localize.LocalizeValue;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.TimeSeriesKind;

/**
 * A live value a profiler reports, such as heap used or process CPU load. Metrics with the same group are drawn on one
 * chart, so they share a unit.
 *
 * @param id    stable id of the metric, unique within its profiler
 * @param name  the series name shown in the chart legend
 * @param unit  the unit of the reported values
 * @param group the id of the chart the metric is drawn on, such as {@code "cpu"} or {@code "memory"}
 * @param kind  how the series is drawn
 * @author VISTALL
 * @since 2026-10-03
 */
public record ProfilerMetric(String id, LocalizeValue name, ChartUnit unit, String group, TimeSeriesKind kind) {
}
