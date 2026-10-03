/*
 * Copyright (C) 2017 The Android Open Source Project
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

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A renderer is a function performing arbitrary drawing on the graphics. Other parameters such as boundary and defaultFontMetrics meant to
 * be read-only.
 */
@FunctionalInterface
public interface Renderer<T> {
    void render(Graphics2D g, Rectangle2D.Float boundary, FontMetrics defaultFontMetrics, boolean hovered, T value);
}
