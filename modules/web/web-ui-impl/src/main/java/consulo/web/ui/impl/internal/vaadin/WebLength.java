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
package consulo.web.ui.impl.internal.vaadin;

import consulo.ui.Length;

import java.util.List;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The browser is the only frontend which can keep a length relative to the text without measuring it, so a length in
 * fonts becomes a calc the browser re-evaluates, rather than a pixel count taken once.
 * <p/>
 * {@code em} is the font size of the element itself, so one text line is {@code 1em} times the line height - {@code em}
 * alone would leave out the leading and come up short of a rendered line. The line height carries a literal fallback
 * because an undefined variable makes the whole {@code calc} invalid, which drops the height instead of missing it.
 * <p/>
 * A window is sized in the font height the desktop frontends use instead, so a dialog sized in fonts keeps the
 * proportions it has there rather than growing with the roomier line of the web theme.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public final class WebLength {
    private static final String LINE_UNIT = "em * var(--vaadin-line-height-m, 1.625)";
    private static final String WINDOW_UNIT = "em * 1.3";

    public static String toCss(Length length) {
        return "calc(" + toExpression(length, LINE_UNIT) + ")";
    }

    public static String toWindowCss(Length length) {
        return "calc(" + toExpression(length, WINDOW_UNIT) + ")";
    }

    private static String toExpression(Length length, String fontUnit) {
        return length.accept(new Length.Visitor<String>() {
            @Override
            public String visitPixel(int pixels) {
                return pixels + "px";
            }

            @Override
            public String visitFont(float fonts) {
                return fonts + fontUnit;
            }

            @Override
            public String visitComposite(List<Length> parts) {
                return parts.stream().map(part -> toExpression(part, fontUnit)).collect(Collectors.joining(" + "));
            }
        });
    }

    private WebLength() {
    }
}
