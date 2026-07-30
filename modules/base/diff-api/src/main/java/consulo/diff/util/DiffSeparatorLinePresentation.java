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
 * A plain rule across the editor content marking a diff chunk edge.
 *
 * @param color      the edge colour, or {@code null} to use the theme's component border colour
 * @param doubleLine draw the edge two pixels thick
 * @param dotted     draw the edge dotted, used for resolved conflicts
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record DiffSeparatorLinePresentation(
    @Nullable ColorValue color,
    boolean doubleLine,
    boolean dotted
) implements LineSeparatorPresentation {
}
