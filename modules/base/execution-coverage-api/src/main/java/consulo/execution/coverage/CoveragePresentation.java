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
package consulo.execution.coverage;

import consulo.codeEditor.markup.EditorGutterArea;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * States the coverage status of a line.
 *
 * @param startLine first logical line, inclusive
 * @param endLine   logical line past the last, exclusive
 * @param color     the status colour, resolved from the colour scheme while building, already faded
 *                  when line numbers or annotations compete for the same space
 * @param icon      shown when the line is covered by exactly one test, otherwise {@code null}
 * @param payload   the line number this presentation came from, handed back on click
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record CoveragePresentation(
    int startLine,
    int endLine,
    @Nullable ColorValue color,
    @Nullable Image icon,
    @Nullable Object payload
) implements LineMarkerPresentation {

    @Override
    public EditorGutterArea area() {
        return EditorGutterArea.LEFT_FREE_PAINTERS;
    }
}
