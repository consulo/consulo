/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.impl.chart.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the default implementation of a ranged series. It provides access to the DataSeries scoped by a given Range or the
 * intersection of two given ranges.
 *
 * @param <E> This should be the type of data this RangedSeries represents.
 */
public class RangedSeries<E> {
    /**
     * The range of the view
     */
    private final Range myXRange;
    /**
     * Store for the data we will provide scoped access to
     */
    private final DataSeries<E> mySeries;
    /**
     * The range of the data
     */
    private final Range myIntersectRange;

    private Range myLastQueriedRange = new Range();
    private List<SeriesData<E>> myLastQueriedSeries = List.of();

    public RangedSeries(Range xRange, DataSeries<E> series) {
        this(xRange, series, new Range(-Double.MAX_VALUE, Double.MAX_VALUE));
    }

    public RangedSeries(Range xRange, DataSeries<E> series, Range intersectRange) {
        myXRange = xRange;
        mySeries = series;
        myIntersectRange = intersectRange;
    }

    public Range getXRange() {
        return myXRange;
    }

    /**
     * A new range object that represents the intersection between the default and intersect ranges.
     */
    public Range getIntersection() {
        return myXRange.getIntersection(myIntersectRange);
    }

    /**
     * A new, immutable {@code List<SeriesData>} consisting of items in the DataStore scoped to the range(s) that the RangedSeries was
     * initialized with.
     * <p>
     * Note - this call is frequently made by UI components on the main thread, so the last queried results are cached and returned if the
     * query range is determined to not have changed to avoid hitting the Datastore redundantly. If the query range's max value is
     * Long.MAX_VALUE or Double.MAX_VALUE, however, then the cache is bypassed since there might be new data that are still streaming in.
     */
    public List<SeriesData<E>> getSeries() {
        return getValuesInRange();
    }

    // See comments on getSeries() for more details on how this function works.
    private List<SeriesData<E>> getValuesInRange() {
        Range queryRange = myXRange.getIntersection(myIntersectRange);
        if (queryRange.getMax() == (double) Long.MAX_VALUE || queryRange.getMax() == Double.MAX_VALUE) {
            return mySeries.getDataForRange(queryRange);
        }
        if (!myLastQueriedRange.isSameAs(queryRange)) {
            List<SeriesData<E>> queriedSeries = mySeries.getDataForRange(queryRange);
            myLastQueriedRange = queryRange;
            // Make a copy to allow the underlying series to change freely
            myLastQueriedSeries = List.copyOf(new ArrayList<>(queriedSeries));
        }
        return myLastQueriedSeries;
    }

    /**
     * @param range The range to which the data will be scoped.
     * @return A new, immutable {@link SeriesData} list that allows the caller to get items in the DataStore scoped to the given range.
     */
    public List<SeriesData<E>> getSeriesForRange(Range range) {
        return mySeries.getDataForRange(range);
    }

    /**
     * Invalidate cached query
     */
    public void invalidate() {
        myLastQueriedRange = new Range();
        myLastQueriedSeries = List.of();
    }
}
