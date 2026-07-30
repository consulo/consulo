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

import consulo.codeEditor.markup.EditorGutterArea;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * States that a range of lines belongs to a diff chunk.
 * <p>
 * A chunk spanning too few pixels to show an interior collapses to a single doubled edge, but that
 * is a rendering decision: under folding a multi-line range can resolve to zero height, so it can
 * only be taken once the line range has been mapped to pixels.
 *
 * @param startLine   first logical line, inclusive
 * @param endLine     logical line past the last, exclusive; equal to {@code startLine} for an
 *                    insertion or deletion point
 * @param area        horizontal band of the gutter this piece of the chunk covers
 * @param borderColor colour of the chunk edges
 * @param fillColor   interior colour, or {@code null} to leave the interior unfilled
 * @param dotted      draw edges dotted, used for resolved conflicts
 * @param payload     domain object handed back on click
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record DiffChunkPresentation(
    int startLine,
    int endLine,
    EditorGutterArea area,
    ColorValue borderColor,
    @Nullable ColorValue fillColor,
    boolean dotted,
    @Nullable Object payload
) implements LineMarkerPresentation {
}
