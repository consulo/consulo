/*
 * Copyright (C) 2023 The Android Open Source Project
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

import consulo.ui.impl.chart.model.updater.Updater;

/**
 * Facilitates some zoom related operations that are used Timelines
 */
public class TimelineZoomHelper {
    /**
     * In order to prevent attempts to zoom larger than the current view range, this cap serves to limit the delta range to a fixed number
     * proportional to the current view range.
     */
    private static final double ZOOM_IN_DELTA_RANGE_US_MAX_RATIO = 0.90;

    /**
     * How many nanoseconds left in our zoom before we just clamp to our final value.
     */
    private static final double ZOOM_LERP_THRESHOLD_NS = 10.0;

    private final Range myDataRange;
    private final Range myViewRange;
    private final Range myZoomLeft;

    public TimelineZoomHelper(Range dataRange, Range viewRange, Range zoomLeft) {
        myDataRange = dataRange;
        myViewRange = viewRange;
        myZoomLeft = zoomLeft;
    }

    /**
     * Calculates a zoom within the current data bounds. If a zoom extends beyond data max the left over is applied to the view minimum.
     *
     * @param amountUs the amount of time request to change the view by.
     * @param ratio    a ratio between 0 and 1 that determines the focal point of the zoom. 1 applies the full delta to the min while 0
     *                 applies the full delta to the max.
     */
    public void zoom(double amountUs, double ratio) {
        double deltaUs = amountUs;
        if (deltaUs == 0.0) {
            return;
        }
        if (deltaUs < 0.0) {
            double zoomMax = -ZOOM_IN_DELTA_RANGE_US_MAX_RATIO * myViewRange.getLength();
            deltaUs = Math.max(zoomMax, deltaUs);
        }
        myZoomLeft.clear();
        double minUs = myViewRange.getMin() - deltaUs * ratio;
        double maxUs = myViewRange.getMax() + deltaUs * (1 - ratio);
        // When the view range is not fully covered, reset minUs to data range could change zoomLeft
        // from zero to a large number.
        boolean isDataRangeFullyCoveredByViewRange = myDataRange.getMin() <= myViewRange.getMin();
        if (isDataRangeFullyCoveredByViewRange && minUs < myDataRange.getMin()) {
            maxUs += myDataRange.getMin() - minUs;
            minUs = myDataRange.getMin();
        }
        // If our new view range is less than our data range then lock our max view, so we
        // don't expand it beyond the data range max.
        if (!isDataRangeFullyCoveredByViewRange && minUs < myDataRange.getMin()) {
            maxUs = myDataRange.getMax();
        }
        if (maxUs > myDataRange.getMax()) {
            minUs -= maxUs - myDataRange.getMax();
            maxUs = myDataRange.getMax();
        }
        // minUs could have gone past again.
        if (isDataRangeFullyCoveredByViewRange) {
            minUs = Math.max(minUs, myDataRange.getMin());
        }
        myZoomLeft.set(minUs - myViewRange.getMin(), maxUs - myViewRange.getMax());
    }

    /**
     * Updates {@code zoomLeft} after a timeline {@code frameViewToRange} call
     */
    public void updateZoomLeft(Range targetRange, double paddingRatio) {
        Range finalRange = new Range(targetRange.getMin() - targetRange.getLength() * paddingRatio,
            targetRange.getMax() + targetRange.getLength() * paddingRatio);

        // Cap requested view to max data.
        if (finalRange.getMax() > myDataRange.getMax()) {
            finalRange.setMax(myDataRange.getMax());
        }
        myZoomLeft.set(finalRange.getMin() - myViewRange.getMin(), finalRange.getMax() - myViewRange.getMax());
    }

    /**
     * Handles updating the view range by the delta stored in our {@code zoomLeft} value. If we have a delta stored in {@code zoomLeft}
     * we apply a percentage of that value to our current view, and reduce the delta currently stored. Eg: View = 10, 100 zoomLeft = 30,-30
     * After we call this function we end up with View = 20, 90 zoomLeft = 20, -20.
     */
    public void handleZoomView(long elapsedNs) {
        if (myZoomLeft.getMin() != 0.0 || myZoomLeft.getMax() != 0.0) {
            double min = Updater.lerp(0.0, myZoomLeft.getMin(), 0.99999f, elapsedNs, ZOOM_LERP_THRESHOLD_NS);
            double max = Updater.lerp(0.0, myZoomLeft.getMax(), 0.99999f, elapsedNs, ZOOM_LERP_THRESHOLD_NS);
            myZoomLeft.set(myZoomLeft.getMin() - min, myZoomLeft.getMax() - max);
            if ((myViewRange.getMax() + max) > myDataRange.getMax()) {
                max = myDataRange.getMax() - myViewRange.getMax();
            }
            myViewRange.set(myViewRange.getMin() + min, myViewRange.getMax() + max);
        }
    }
}
