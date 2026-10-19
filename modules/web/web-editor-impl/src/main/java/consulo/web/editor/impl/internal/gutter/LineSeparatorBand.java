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

import org.jspecify.annotations.Nullable;

/**
 * A {@link consulo.codeEditor.markup.LineSeparatorPresentation} as the browser receives it - the editor adds the line
 * and the side of it the separator is on.
 *
 * @param style      {@link GutterBand#STYLE_LINE} for a rule or {@link GutterBand#STYLE_WAVE} for a wave
 * @param color      the rule, or the band of the wave
 * @param doubleLine draw the rule two pixels thick
 * @param dotted     draw the rule dotted
 * @param topBorderColor    the top edge of the wave
 * @param bottomBorderColor the bottom edge of the wave
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public record LineSeparatorBand(
    String style,
    String color,
    boolean doubleLine,
    boolean dotted,
    @Nullable String topBorderColor,
    @Nullable String bottomBorderColor
) {
    public static LineSeparatorBand line(String color, boolean doubleLine, boolean dotted) {
        return new LineSeparatorBand(GutterBand.STYLE_LINE, color, doubleLine, dotted, null, null);
    }

    public static LineSeparatorBand wave(String background, @Nullable String topBorderColor, @Nullable String bottomBorderColor) {
        return new LineSeparatorBand(GutterBand.STYLE_WAVE, background, false, false, topBorderColor, bottomBorderColor);
    }
}
