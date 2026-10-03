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

import consulo.ui.impl.chart.model.axis.AxisComponentModel;
import consulo.ui.impl.chart.model.formatter.BaseAxisFormatter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class AxisMarkers {
    public record Marker(float offset, @Nullable String label) {
        public boolean isMajor() {
            return label != null;
        }
    }

    private AxisMarkers() {
    }

    public static List<Marker> compute(AxisComponentModel model, boolean hideNegativeValues, boolean onlyShowUnitAtMax) {
        double currentMinValueRelative = model.getRange().getMin();
        double currentMaxValueRelative = model.getRange().getMax();
        if (hideNegativeValues) {
            currentMinValueRelative -= model.getZero();
            currentMaxValueRelative -= model.getZero();
        }

        double range = model.getRange().getLength();
        double labelRange = model.getDataRange();
        List<Marker> markers = new ArrayList<>();
        if (range <= 0 || model.getRange().isEmpty()) {
            return markers;
        }

        BaseAxisFormatter formatter = model.getFormatter();
        float majorInterval = formatter.getMajorInterval(range);
        float minorInterval = formatter.getMinorInterval(majorInterval);
        if (majorInterval <= 0 || minorInterval <= 0) {
            return markers;
        }
        float minorScale = (float) (minorInterval / range);

        double firstMarkerValue = Math.floor(currentMinValueRelative / majorInterval) * majorInterval;
        float firstMarkerOffset = (float) (minorScale * (firstMarkerValue - currentMinValueRelative) / minorInterval);

        int numMarkers = (int) Math.floor((currentMaxValueRelative - firstMarkerValue) / minorInterval) + 1;
        int numMinorPerMajor = Math.max(1, (int) (majorInterval / minorInterval));

        for (int i = 0; i < numMarkers; i++) {
            double markerValue = firstMarkerValue + i * minorInterval;
            if (!model.getMarkerRange().contains(markerValue)) {
                continue;
            }

            float markerOffset = firstMarkerOffset + i * minorScale;
            if ((markerOffset < 0 && i > 0) || markerOffset > 1f) {
                continue;
            }

            if (i % numMinorPerMajor == 0) {
                markers.add(new Marker(markerOffset, formatter.getFormattedString(labelRange, markerValue, !onlyShowUnitAtMax)));
            }
            else {
                markers.add(new Marker(markerOffset, null));
            }
        }
        return markers;
    }

    public static String formatMax(AxisComponentModel model, boolean hideNegativeValues) {
        double max = model.getRange().getMax();
        if (hideNegativeValues) {
            max -= model.getZero();
        }
        return model.getFormatter().getFormattedString(model.getDataRange(), max, true);
    }
}
