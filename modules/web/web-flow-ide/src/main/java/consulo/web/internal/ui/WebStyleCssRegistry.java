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
package consulo.web.internal.ui;

import consulo.ui.color.RGBColor;
import consulo.ui.impl.style.StyleColorKeys;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.Style;
import consulo.ui.style.StyleColorValue;
import consulo.ui.style.StyleManager;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public class WebStyleCssRegistry {
    public static final String STYLE_ATTRIBUTE = "consulo-style";

    private static final String VAR_PREFIX = "--consulo-";

    public static String buildStyleCss() {
        StringBuilder builder = new StringBuilder();
        for (Style style : StyleManager.get().getStyles()) {
            builder.append("html[").append(STYLE_ATTRIBUTE).append("=\"").append(style.getId()).append("\"] {\n");

            for (Map.Entry<StyleColorValue, String> entry : StyleColorKeys.getKeys().entrySet()) {
                builder.append("  ")
                    .append(toVariableName(entry.getValue()))
                    .append(": ")
                    .append(toCssColor(style.getColorValue(entry.getKey()).toRGB()))
                    .append(";\n");
            }

            // aura derives every surface and contrast shade from its own base color, picked out of the
            // light/dark pair by color-scheme. both sides get the theme value, so styles sharing a color
            // scheme - dark and dark grey - still look different
            appendVariable(builder, "--aura-background-color-light", style, ComponentColors.LAYOUT);
            appendVariable(builder, "--aura-background-color-dark", style, ComponentColors.LAYOUT);
            appendVariable(builder, "--vaadin-background-color", style, ComponentColors.LAYOUT);
            appendVariable(builder, "--vaadin-body-text-color", style, ComponentColors.TEXT_FOREGROUND);
            appendVariable(builder, "--vaadin-border-color", style, ComponentColors.BORDER);

            // aura does not define the contrast series the utility classes read, their static fallbacks never
            // follow the theme
            RGBColor contrast = style.getColorValue(ComponentColors.TEXT_FOREGROUND).toRGB();
            builder.append("  --vaadin-contrast-color: ").append(toCssColor(contrast)).append(";\n");
            for (int percent = 5; percent <= 90; percent += percent == 5 ? 5 : 10) {
                builder.append("  --vaadin-contrast-").append(percent).append("pct: rgba(")
                    .append(contrast.getRed()).append(", ")
                    .append(contrast.getGreen()).append(", ")
                    .append(contrast.getBlue()).append(", ")
                    .append(percent / 100f)
                    .append(");\n");
            }

            builder.append("}\n");
        }
        return builder.toString();
    }

    private static void appendVariable(StringBuilder builder, String name, Style style, StyleColorValue colorValue) {
        builder.append("  ").append(name).append(": ").append(toCssColor(style.getColorValue(colorValue).toRGB())).append(";\n");
    }

    public static String toVariableName(String key) {
        StringBuilder builder = new StringBuilder(VAR_PREFIX);
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '.') {
                builder.append('-');
            }
            else if (Character.isUpperCase(c)) {
                if (i > 0 && key.charAt(i - 1) != '.') {
                    builder.append('-');
                }
                builder.append(Character.toLowerCase(c));
            }
            else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String toCssColor(RGBColor color) {
        int alpha = color.getAlpha();
        if (alpha == 255) {
            return "rgb(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")";
        }
        return "rgba(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", " + (alpha / 255f) + ")";
    }
}
