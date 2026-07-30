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
 * The gutter half of a diff separator: the same zigzag band as
 * {@link DiffSeparatorWavePresentation}, continued across the gutter so the two join seamlessly,
 * plus a backdrop that clears the annotations area behind it.
 *
 * @param startLine             the boundary the band sits on
 * @param background            colour of the band's body
 * @param topBorder             colour fading in at the top edge, or {@code null}
 * @param bottomBorder          colour fading in at the bottom edge, or {@code null}
 * @param annotationsBackground backdrop for the annotations area, or {@code null} to leave it alone
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record DiffSeparatorGutterPresentation(
    int startLine,
    ColorValue background,
    @Nullable ColorValue topBorder,
    @Nullable ColorValue bottomBorder,
    @Nullable ColorValue annotationsBackground
) implements LineMarkerPresentation {

    @Override
    public int endLine() {
        return startLine + 1;
    }

    @Override
    public EditorGutterArea area() {
        return EditorGutterArea.WHOLE_GUTTER;
    }
}
