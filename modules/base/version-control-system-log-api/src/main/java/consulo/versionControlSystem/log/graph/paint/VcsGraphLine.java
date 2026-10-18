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
package consulo.versionControlSystem.log.graph.paint;

import consulo.ui.color.ColorValue;

/**
 * A line of one row of the graph, from the lane of the row's element outward. x in pixels, y as an anchor plus a pixel
 * offset. An arrow, when present, sits at the end point.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public record VcsGraphLine(
    int startX,
    VcsGraphAnchor startAnchor,
    int startOffset,
    int endX,
    VcsGraphAnchor endAnchor,
    int endOffset,
    ColorValue color,
    boolean dashed,
    boolean arrow,
    boolean selected
) {
}
