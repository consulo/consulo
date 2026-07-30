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
package consulo.desktop.awt.internal.versionControlSystem.patch;

import consulo.codeEditor.markup.EditorGutterArea;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * States how a patch hunk applied to these lines. The colour already encodes the outcome, since the
 * apply-patch viewer resolves status to a colour before building the highlighter.
 *
 * @param startLine   first logical line, inclusive
 * @param endLine     logical line past the last, exclusive; equal to {@code startLine} when the hunk
 *                    removed lines and so occupies a boundary
 * @param color       the outcome colour
 * @param borderColor gutter border colour, or {@code null} when the scheme defines none
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record PatchChangePresentation(
    int startLine,
    int endLine,
    ColorValue color,
    @Nullable ColorValue borderColor
) implements LineMarkerPresentation {

    @Override
    public EditorGutterArea area() {
        return EditorGutterArea.RIGHT_FREE_PAINTERS;
    }
}
