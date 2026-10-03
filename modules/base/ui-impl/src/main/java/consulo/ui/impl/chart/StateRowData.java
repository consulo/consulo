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

import consulo.ui.impl.chart.model.DataSeries;
import consulo.ui.impl.chart.model.Range;
import consulo.ui.impl.chart.model.SeriesData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class StateRowData<S> {
    private final List<SeriesData<S>> myData = new ArrayList<>();

    public List<SeriesData<S>> getData() {
        return myData;
    }

    public boolean set(long timeUs, S state) {
        if (!myData.isEmpty()) {
            SeriesData<S> last = myData.get(myData.size() - 1);
            if (Objects.equals(last.value, state)) {
                return false;
            }
            if (last.x > timeUs) {
                return false;
            }
        }
        myData.add(new SeriesData<>(timeUs, state));
        return true;
    }

    public DataSeries<S> asDataSeries() {
        return this::dataForRange;
    }

    private List<SeriesData<S>> dataForRange(Range range) {
        if (myData.isEmpty() || range.isEmpty()) {
            return List.of();
        }
        int from = 0;
        while (from + 1 < myData.size() && myData.get(from + 1).x <= range.getMin()) {
            from++;
        }
        int to = from;
        while (to + 1 < myData.size() && myData.get(to).x < range.getMax()) {
            to++;
        }
        return List.copyOf(myData.subList(from, to + 1));
    }
}
