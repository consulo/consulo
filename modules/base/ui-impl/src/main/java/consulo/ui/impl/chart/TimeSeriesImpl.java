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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.TimeSeries;
import consulo.ui.chart.TimeSeriesKind;
import consulo.ui.color.ColorValue;
import consulo.ui.impl.chart.model.RangedContinuousSeries;

import java.time.Instant;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class TimeSeriesImpl implements TimeSeries {
    private final TimeSeriesListener myListener;
    private final LocalizeValue myName;
    private final TimeSeriesKind myKind;
    private final TimeSeriesData myData;
    private final RangedContinuousSeries myRanged;

    private ColorValue myColor;

    TimeSeriesImpl(TimeSeriesListener listener,
                   LocalizeValue name,
                   TimeSeriesKind kind,
                   ColorValue color,
                   TimeSeriesData data,
                   RangedContinuousSeries ranged) {
        myListener = listener;
        myName = name;
        myKind = kind;
        myColor = color;
        myData = data;
        myRanged = ranged;
    }

    public TimeSeriesData getData() {
        return myData;
    }

    public RangedContinuousSeries getRanged() {
        return myRanged;
    }

    public ColorValue getColor() {
        return myColor;
    }

    @Override
    public LocalizeValue getName() {
        return myName;
    }

    @Override
    public TimeSeriesKind getKind() {
        return myKind;
    }

    @RequiredUIAccess
    @Override
    public void add(Instant time, double value) {
        myData.add(ChartTime.toMicros(time), value);
        myRanged.invalidate();
        myListener.dataChanged(this);
    }

    @RequiredUIAccess
    @Override
    public void setColor(ColorValue color) {
        myColor = color;
        myListener.colorChanged(this);
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        myData.clear();
        myRanged.invalidate();
        myListener.dataChanged(this);
    }
}
