/*
 * Copyright (C) 2018 The Android Open Source Project
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

package consulo.ui.impl.chart.model.axis;

import consulo.ui.impl.chart.model.AspectModel;
import consulo.ui.impl.chart.model.Range;
import consulo.ui.impl.chart.model.formatter.BaseAxisFormatter;

import org.jspecify.annotations.Nullable;

public abstract class AxisComponentModel extends AspectModel<AxisComponentModel.Aspect> {

    public enum Aspect {
        AXIS
    }

    protected final Range myRange;
    protected final Range myMarkerRange;
    protected final BaseAxisFormatter myFormatter;
    protected String myLabel;

    /**
     * During the first update, skip the y range interpolation and snap to the initial max value.
     */
    protected boolean myFirstUpdate = true;

    protected AxisComponentModel(BaseBuilder<? extends AxisComponentModel> builder) {
        myRange = builder.myRange;
        myMarkerRange = builder.myMarkerRange;
        myFormatter = builder.myFormatter;
        myLabel = builder.myLabel;
        myRange.addDependency(this).onChange(Range.Aspect.RANGE, this::updateImmediately);
    }

    public abstract void updateImmediately();

    public void reset() {
        myFirstUpdate = true;
        updateImmediately();
    }

    public String getLabel() {
        return myLabel;
    }

    public Range getRange() {
        return myRange;
    }

    public Range getMarkerRange() {
        return myMarkerRange;
    }

    public double getDataRange() {
        return myRange.getLength();
    }

    public BaseAxisFormatter getFormatter() {
        return myFormatter;
    }

    public double getZero() {
        return myRange.getMin();
    }

    public static abstract class BaseBuilder<T extends AxisComponentModel> {
        protected final Range myRange;
        protected final BaseAxisFormatter myFormatter;

        protected Range myMarkerRange = new Range(0, Double.MAX_VALUE);
        @Nullable protected Range myGlobalRange;
        protected String myLabel = "";

        /**
         * @param range     A Range object this AxisComponent listens to for the min/max values.
         * @param formatter Formatter used for determining the tick marker and labels that need to be rendered.
         */
        public BaseBuilder(Range range, BaseAxisFormatter formatter) {
            myRange = range;
            myFormatter = formatter;
        }

        /**
         * Sets the content of the axis's label.
         */
        public BaseBuilder<T> setLabel(String label) {
            myLabel = label;
            return this;
        }

        /**
         * Sets the range of the axis's markers, there could be an range visible on the axis but
         * no markers are shown.
         */
        public AxisComponentModel.BaseBuilder<T> setMarkerRange(Range range) {
            myMarkerRange = range;
            return this;
        }

        public abstract T build();
    }
}
