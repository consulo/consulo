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
package consulo.web.internal.ui.editor.gutter;

import consulo.codeEditor.markup.EditorGutterArea;
import org.jspecify.annotations.Nullable;

/**
 * A {@link consulo.codeEditor.markup.LineMarkerPresentation} as the browser receives it: which strip, which
 * lines, and what it looks like. The css does the drawing, so a painter produces one of these instead of
 * putting pixels anywhere.
 *
 * @param area      name of the {@link EditorGutterArea} the band occupies
 * @param startLine first document line, inclusive
 * @param endLine   line past the last, exclusive - equal to {@code startLine} for a marker that sits on the
 *                  boundary between two lines, which is how a deletion is expressed
 * @param color     fill, or null for an outline only
 * @param borderColor outline, or null for a fill only
 * @param dotted    draw the outline dotted
 * @param clickId   index of the band in the update it belongs to, or -1 when nothing answers a click on it
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public record GutterBand(
    String area,
    int startLine,
    int endLine,
    @Nullable String color,
    @Nullable String borderColor,
    boolean dotted,
    int clickId
) {
    public GutterBand(String area, int startLine, int endLine, @Nullable String color, @Nullable String borderColor, boolean dotted) {
        this(area, startLine, endLine, color, borderColor, dotted, -1);
    }

    public static GutterBand fill(EditorGutterArea area, int startLine, int endLine, String color) {
        return new GutterBand(area.name(), startLine, endLine, color, null, false);
    }

    public GutterBand withClickId(int clickId) {
        return new GutterBand(area, startLine, endLine, color, borderColor, dotted, clickId);
    }
}
