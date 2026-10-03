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

package consulo.desktop.awt.ui.impl.adtui.chart.statechart;

import consulo.desktop.awt.ui.impl.adtui.common.AdtUiUtils;
import java.awt.Color;

/**
 * This class helps map between values specified in {@link StateChart} components to the element color.
 */
public abstract class StateChartColorProvider<T> {

    /**
     * Get a color for the {@link StateChart} rectangle associated with this value.
     */
    public abstract Color getColor(boolean isMouseOver, T value);

    /**
     * Get a color for the {@link StateChart} text associated with this value.
     */
    public Color getFontColor(boolean isMouseOver, T value) {
        return AdtUiUtils.DEFAULT_FONT_COLOR;
    }
}
