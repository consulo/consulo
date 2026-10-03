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
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class TimeSeriesData {
    private long[] myTimes = new long[64];
    private double[] myValues = new double[64];
    private int mySize;

    public int size() {
        return mySize;
    }

    public long getTime(int index) {
        return myTimes[index];
    }

    public double getValue(int index) {
        return myValues[index];
    }

    public void add(long timeUs, double value) {
        if (mySize == myTimes.length) {
            myTimes = Arrays.copyOf(myTimes, mySize * 2);
            myValues = Arrays.copyOf(myValues, mySize * 2);
        }
        int index = mySize;
        if (mySize > 0 && myTimes[mySize - 1] > timeUs) {
            index = insertionIndex(timeUs);
            System.arraycopy(myTimes, index, myTimes, index + 1, mySize - index);
            System.arraycopy(myValues, index, myValues, index + 1, mySize - index);
        }
        myTimes[index] = timeUs;
        myValues[index] = value;
        mySize++;
    }

    public void clear() {
        mySize = 0;
    }

    public double maxValue() {
        double max = 0;
        for (int i = 0; i < mySize; i++) {
            max = Math.max(max, myValues[i]);
        }
        return max;
    }

    public int firstIndexAtOrBefore(double timeUs) {
        int index = insertionIndex((long) Math.ceil(timeUs));
        return Math.max(0, index - 1);
    }

    public int lastIndexAtOrAfter(double timeUs) {
        int index = insertionIndex((long) Math.floor(timeUs));
        return Math.min(mySize - 1, index);
    }

    private int insertionIndex(long timeUs) {
        int low = 0;
        int high = mySize - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (myTimes[mid] <= timeUs) {
                low = mid + 1;
            }
            else {
                high = mid - 1;
            }
        }
        return low;
    }

    public DataSeries<Long> asLongDataSeries(double scale) {
        return range -> longDataForRange(range, scale);
    }

    private List<SeriesData<Long>> longDataForRange(Range range, double scale) {
        if (mySize == 0 || range.isEmpty()) {
            return List.of();
        }
        int from = firstIndexAtOrBefore(range.getMin());
        int to = lastIndexAtOrAfter(range.getMax());
        List<SeriesData<Long>> result = new ArrayList<>(Math.max(0, to - from + 1));
        for (int i = from; i <= to; i++) {
            result.add(new SeriesData<>(myTimes[i], Math.round(myValues[i] * scale)));
        }
        return result;
    }
}
