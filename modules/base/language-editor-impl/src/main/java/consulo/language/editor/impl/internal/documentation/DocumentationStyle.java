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
package consulo.language.editor.impl.internal.documentation;

import consulo.colorScheme.FontSize;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.Style;
import consulo.ui.util.ColorValueUtil;
import consulo.util.lang.StringUtil;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class DocumentationStyle {
    private static final int OUTER_PADDING = 10;
    private static final int INNER_PADDING = 2;
    private static final int PARAGRAPH_SPACING = 4;

    private DocumentationStyle() {
    }

    public static String build(Style style, ColorValue background, FontSize fontSize, String editorFontName) {
        RGBColor backgroundRgb = background.toRGB();
        String backgroundColor = "#" + ColorValueUtil.toHex(backgroundRgb);
        String foreground = color(style, ComponentColors.TEXT_FOREGROUND, backgroundRgb);
        String grayed = color(style, ComponentColors.DISABLED_TEXT, backgroundRgb);
        String link = color(style, ComponentColors.LINK_FOREGROUND, backgroundRgb);
        String editorFont = "\"" + StringUtil.escapeQuotes(editorFontName) + "\", monospace";

        return "html { background-color: " + backgroundColor + "; }\n"
            + "body { margin: 0; padding: 0 " + OUTER_PADDING + "px " + OUTER_PADDING + "px " + OUTER_PADDING + "px; "
            + "font-size: " + fontSize.getSize() + "px; color: " + foreground + "; background-color: " + backgroundColor + "; }\n"
            + "code, pre, .pre { font-family: " + editorFont + "; }\n"
            + "pre { white-space: pre-wrap; }\n"
            + "a { color: " + link + "; text-decoration: none; }\n"
            + "img { vertical-align: middle; }\n"
            + "p { margin: 0; padding: " + PARAGRAPH_SPACING + "px 0 " + PARAGRAPH_SPACING + "px 0; }\n"
            + "h1, h2, h3, h4, h5, h6 { margin-top: 0; padding-top: 1px; }\n"
            + "ol, ul { margin-top: 0; margin-bottom: 0; }\n"
            + ".definition, .definition-only { padding: " + PARAGRAPH_SPACING + "px " + INNER_PADDING + "px " + PARAGRAPH_SPACING + "px "
            + INNER_PADDING + "px; }\n"
            + ".definition-separator { margin: 0 0 " + PARAGRAPH_SPACING + "px 0; }\n"
            + ".definition pre, .definition-only pre { margin: 0; padding: 0; }\n"
            + ".content, .content-only { padding: 0 " + INNER_PADDING + "px 0 " + INNER_PADDING + "px; }\n"
            + ".bottom, .bottom-no-content { padding: " + PARAGRAPH_SPACING + "px " + INNER_PADDING + "px " + PARAGRAPH_SPACING + "px "
            + INNER_PADDING + "px; }\n"
            + ".grayed { color: " + grayed + "; }\n"
            + ".centered { text-align: center; }\n"
            + ".sections { border-spacing: 0; }\n"
            + "table p { padding-bottom: 0; }\n"
            + "td { margin: 0; padding: 0; }\n"
            + "th { text-align: left; }\n"
            + ".section { color: " + grayed + "; padding-right: 4px; white-space: nowrap; }\n";
    }

    private static String color(Style style, ComponentColors key, RGBColor background) {
        RGBColor color = style.getColorValue(key).toRGB();
        int alpha = color.getAlpha();
        if (alpha == 255) {
            return "#" + ColorValueUtil.toHex(color);
        }

        RGBColor opaque = new RGBColor(
            blend(color.getRed(), background.getRed(), alpha),
            blend(color.getGreen(), background.getGreen(), alpha),
            blend(color.getBlue(), background.getBlue(), alpha)
        );
        return "#" + ColorValueUtil.toHex(opaque);
    }

    private static int blend(int value, int background, int alpha) {
        return (value * alpha + background * (255 - alpha)) / 255;
    }
}
