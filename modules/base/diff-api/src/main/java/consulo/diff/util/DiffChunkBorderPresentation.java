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
 * States that a diff chunk has an edge at a line boundary, without describing a chunk body.
 * <p>
 * Distinct from {@link DiffChunkPresentation} because the edge weight is chosen by the caller
 * rather than derived from how much room the chunk turned out to have.
 *
 * @param line        the boundary this edge sits on; the top of that line
 * @param area        horizontal band of the gutter the edge spans
 * @param color       edge colour
 * @param doubleLine  draw the edge two pixels thick
 * @param dotted      draw the edge dotted, used for resolved conflicts
 * @param payload     domain object handed back on click
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record DiffChunkBorderPresentation(
    int line,
    EditorGutterArea area,
    ColorValue color,
    boolean doubleLine,
    boolean dotted,
    @Nullable Object payload
) implements LineMarkerPresentation {

    @Override
    public int startLine() {
        return line;
    }

    @Override
    public int endLine() {
        return line;
    }
}
