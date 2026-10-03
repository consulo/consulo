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

/**
 * This class adds a name and an additional range to RangedSeries. This additional range represents the Y axis to the default range which
 * represents the x axis.
 */
public class RangedContinuousSeries extends RangedSeries<Long> {
    private final String myName;
    private final Range myYRange;

    public RangedContinuousSeries(String name, Range xRange, Range yRange, DataSeries<Long> series) {
        this(name, xRange, yRange, series, new Range(-Double.MAX_VALUE, Double.MAX_VALUE));
    }

    public RangedContinuousSeries(String name, Range xRange, Range yRange, DataSeries<Long> series, Range intersectRange) {
        super(xRange, series, intersectRange);
        myName = name;
        myYRange = yRange;
    }

    public String getName() {
        return myName;
    }

    public Range getYRange() {
        return myYRange;
    }
}
