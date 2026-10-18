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
import org.jspecify.annotations.Nullable;

/**
 * A straight stroke in pixels of a row of a known height, ready to be drawn. {@code dash} is null for a solid line.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public record VcsGraphSegment(float x1, float y1, float x2, float y2, ColorValue color, float width, float @Nullable [] dash) {
}
