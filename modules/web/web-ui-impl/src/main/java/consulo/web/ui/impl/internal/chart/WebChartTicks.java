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
package consulo.web.ui.impl.internal.chart;

import consulo.ui.impl.chart.AxisMarkers;
import consulo.ui.impl.chart.model.axis.AxisComponentModel;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class WebChartTicks {
    private WebChartTicks() {
    }

    static void put(ObjectNode root, String field, AxisComponentModel model, double origin, double span) {
        ArrayNode ticks = root.putArray(field);
        for (AxisMarkers.Marker marker : AxisMarkers.compute(model, true, false)) {
            String label = marker.label();
            if (label != null) {
                ArrayNode tick = ticks.addArray();
                tick.add(origin + marker.offset() * span);
                tick.add(label);
            }
        }
    }
}
