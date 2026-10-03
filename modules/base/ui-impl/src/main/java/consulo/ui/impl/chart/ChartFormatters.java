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

import consulo.ui.chart.ChartUnit;
import consulo.ui.impl.chart.model.formatter.BaseAxisFormatter;
import consulo.ui.impl.chart.model.formatter.MemoryAxisFormatter;
import consulo.ui.impl.chart.model.formatter.SingleUnitAxisFormatter;
import consulo.ui.impl.chart.model.formatter.TimeAxisFormatter;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ChartFormatters {
    private static final BaseAxisFormatter PERCENT = new SingleUnitAxisFormatter(1, 5, 10, "%");
    private static final BaseAxisFormatter BYTES = new MemoryAxisFormatter(1, 2, 5);
    private static final BaseAxisFormatter COUNT = new SingleUnitAxisFormatter(1, 5, 1, "");

    private ChartFormatters() {
    }

    public static BaseAxisFormatter forUnit(ChartUnit unit) {
        return switch (unit) {
            case PERCENT -> PERCENT;
            case BYTES -> BYTES;
            case COUNT -> COUNT;
            case DURATION -> TimeAxisFormatter.DEFAULT;
        };
    }

    public static double storageScale(ChartUnit unit) {
        return unit == ChartUnit.DURATION ? 0.001 : 1;
    }
}
