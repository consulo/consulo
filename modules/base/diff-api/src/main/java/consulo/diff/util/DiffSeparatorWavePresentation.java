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
package consulo.diff.util;

import consulo.codeEditor.markup.LineSeparatorPresentation;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * The zigzag band marking a collapsed region between diff chunks.
 * <p>
 * Carries only the three base colours. The band is a vertical stack of identical polylines whose
 * colour is interpolated per pixel row, and the number of rows follows the editor's line height, so
 * the interpolation itself can only be done once the line height is known.
 *
 * @param background   colour of the band's body
 * @param topBorder    colour fading in at the top edge, or {@code null} when the scheme defines none
 * @param bottomBorder colour fading in at the bottom edge, or {@code null} when the scheme defines none
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record DiffSeparatorWavePresentation(
    ColorValue background,
    @Nullable ColorValue topBorder,
    @Nullable ColorValue bottomBorder
) implements LineSeparatorPresentation {
}
