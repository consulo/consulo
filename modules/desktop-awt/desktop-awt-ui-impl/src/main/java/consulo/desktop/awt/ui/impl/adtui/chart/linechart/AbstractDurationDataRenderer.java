/*
 * Copyright (C) 2020 The Android Open Source Project
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
package consulo.desktop.awt.ui.impl.adtui.chart.linechart;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * An interface for custom rendering of duration data
 */
public interface AbstractDurationDataRenderer extends LineChartCustomRenderer {
    /**
     * Render overlay on top of given component
     */
    void renderOverlay(Component host, Graphics2D g2d);

    /**
     * Handle mouse event
     *
     * @return whether other handlers should also handle this event
     */
    boolean handleMouseEvent(Component overlayComponent, Component selectionComponent, MouseEvent event);
}
