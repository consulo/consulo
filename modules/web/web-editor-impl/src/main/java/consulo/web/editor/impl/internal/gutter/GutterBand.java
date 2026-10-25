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
package consulo.web.editor.impl.internal.gutter;

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
 * @param color     fill, or null for an outline only; the band of a {@link #STYLE_WAVE} wave
 * @param borderColor outline, or null for a fill only; the top edge of a {@link #STYLE_WAVE} wave
 * @param dotted    draw the outline dotted
 * @param clickId   index of the band in the update it belongs to, or -1 when nothing answers a click on it
 * @param style     how the band is drawn: {@link #STYLE_BAND}, {@link #STYLE_CHUNK}, {@link #STYLE_LINE}, {@link #STYLE_WAVE} or
 *                  {@link #STYLE_PIXELS}
 * @param doubleLine draw the line of a {@link #STYLE_LINE} band two pixels thick
 * @param bottomBorderColor the bottom edge of a {@link #STYLE_WAVE} wave
 * @param pixelOffset the distance of a {@link #STYLE_PIXELS} strip from the top of {@code startLine}
 * @param pixelHeight the height of a {@link #STYLE_PIXELS} strip
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
    int clickId,
    String style,
    boolean doubleLine,
    @Nullable String bottomBorderColor,
    int pixelOffset,
    int pixelHeight
) {
    /**
     * A filled strip with an outline all around, a short bar across the boundary when its lines are empty.
     */
    public static final String STYLE_BAND = "BAND";

    /**
     * A filled strip with a rule above and below it, a double rule on the boundary when its lines are empty.
     */
    public static final String STYLE_CHUNK = "CHUNK";

    /**
     * A rule on the boundary above {@code startLine}.
     */
    public static final String STYLE_LINE = "LINE";

    /**
     * The wave of a collapsed run of lines, hanging from the top of {@code startLine}.
     */
    public static final String STYLE_WAVE = "WAVE";
    /**
     * A filled strip of {@code pixelHeight} pixels, {@code pixelOffset} pixels from the top of {@code startLine}.
     */
    public static final String STYLE_PIXELS = "PIXELS";

    public GutterBand(String area, int startLine, int endLine, @Nullable String color, @Nullable String borderColor, boolean dotted) {
        this(area, startLine, endLine, color, borderColor, dotted, -1, STYLE_BAND, false, null, 0, 0);
    }

    public static GutterBand fill(EditorGutterArea area, int startLine, int endLine, String color) {
        return new GutterBand(area.name(), startLine, endLine, color, null, false);
    }

    public static GutterBand chunk(
        EditorGutterArea area,
        int startLine,
        int endLine,
        @Nullable String color,
        @Nullable String borderColor,
        boolean dotted
    ) {
        return new GutterBand(area.name(), startLine, endLine, color, borderColor, dotted, -1, STYLE_CHUNK, false, null, 0, 0);
    }

    public static GutterBand line(EditorGutterArea area, int line, String color, boolean doubleLine, boolean dotted) {
        return new GutterBand(area.name(), line, line, null, color, dotted, -1, STYLE_LINE, doubleLine, null, 0, 0);
    }

    public static GutterBand wave(
        EditorGutterArea area,
        int line,
        String background,
        @Nullable String topBorderColor,
        @Nullable String bottomBorderColor
    ) {
        return new GutterBand(area.name(), line, line + 1, background, topBorderColor, false, -1, STYLE_WAVE, false, bottomBorderColor, 0, 0);
    }

    public static GutterBand pixels(EditorGutterArea area, int line, int pixelOffset, int pixelHeight, String color) {
        return new GutterBand(area.name(), line, line, color, null, false, -1, STYLE_PIXELS, false, null, pixelOffset, pixelHeight);
    }

    public GutterBand withClickId(int clickId) {
        return new GutterBand(
            area,
            startLine,
            endLine,
            color,
            borderColor,
            dotted,
            clickId,
            style,
            doubleLine,
            bottomBorderColor,
            pixelOffset,
            pixelHeight
        );
    }
}
