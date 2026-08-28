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
package consulo.web.ui.impl.internal;

import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.color.HSLColor;
import consulo.ui.color.RGBColor;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A colour of the platform as css. Everything the browser is told about a colour goes through here - a
 * {@link ColorValue} is not necessarily rgb, an hsb one among them, and only {@link ColorValue#toRGB()} knows
 * how to resolve whichever it is.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public final class WebColors {
    private static final Logger LOG = Logger.getInstance(WebColors.class);

    private static final Pattern CSS_FUNCTION = Pattern.compile("([a-z]+)\\s*\\(\\s*(.*?)\\s*\\)");
    private static final Pattern CSS_ARGUMENTS = Pattern.compile("[\\s,/]+");

    private static final int MAX_COMPONENT = 255;

    private WebColors() {
    }

    public static @Nullable String toCssColor(@Nullable ColorValue colorValue) {
        if (colorValue == null) {
            return null;
        }

        RGBColor color = colorValue.toRGB();

        // an alpha of its own is kept - a highlight of the editor is translucent over the text it covers
        int alpha = color.getAlpha();
        if (alpha != 255) {
            return String.format("rgba(%d,%d,%d,%.3f)", color.getRed(), color.getGreen(), color.getBlue(), alpha / 255f);
        }

        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static @Nullable ColorValue fromCssColor(@Nullable String cssColor) {
        if (cssColor == null) {
            return null;
        }

        String value = cssColor.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }

        ColorValue color = parseCssColor(value);
        if (color == null) {
            LOG.warn("Cannot read color: " + cssColor);
        }
        return color;
    }

    private static @Nullable ColorValue parseCssColor(String value) {
        if (value.charAt(0) == '#') {
            return parseHexColor(value.substring(1));
        }

        Matcher matcher = CSS_FUNCTION.matcher(value);
        if (!matcher.matches()) {
            return null;
        }

        String function = matcher.group(1);
        String[] arguments = CSS_ARGUMENTS.split(matcher.group(2));

        if (arguments.length != 3 && arguments.length != 4) {
            return null;
        }

        try {
            if ("rgb".equals(function) || "rgba".equals(function)) {
                return new RGBColor(
                    parseComponent(arguments[0]),
                    parseComponent(arguments[1]),
                    parseComponent(arguments[2]),
                    arguments.length == 4 ? parseAlpha(arguments[3]) : MAX_COMPONENT
                );
            }

            if ("hsl".equals(function) || "hsla".equals(function)) {
                return new HSLColor(
                    parseHue(arguments[0]),
                    parseRatio(arguments[1]),
                    parseRatio(arguments[2]),
                    arguments.length == 4 ? parseAlpha(arguments[3]) : MAX_COMPONENT
                );
            }
        }
        catch (NumberFormatException e) {
            return null;
        }

        return null;
    }

    private static @Nullable ColorValue parseHexColor(String hexDigits) {
        String digits = hexDigits;

        if (digits.length() == 3 || digits.length() == 4) {
            StringBuilder builder = new StringBuilder(digits.length() * 2);
            for (int i = 0; i < digits.length(); i++) {
                builder.append(digits.charAt(i)).append(digits.charAt(i));
            }
            digits = builder.toString();
        }

        if (digits.length() != 6 && digits.length() != 8) {
            return null;
        }

        try {
            return new RGBColor(
                Integer.parseInt(digits.substring(0, 2), 16),
                Integer.parseInt(digits.substring(2, 4), 16),
                Integer.parseInt(digits.substring(4, 6), 16),
                digits.length() == 8 ? Integer.parseInt(digits.substring(6, 8), 16) : MAX_COMPONENT
            );
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseComponent(String argument) {
        if (argument.endsWith("%")) {
            return clampComponent(Math.round(parseNumber(argument, 1) * MAX_COMPONENT / 100f));
        }
        return clampComponent(Math.round(Float.parseFloat(argument)));
    }

    private static int parseAlpha(String argument) {
        if (argument.endsWith("%")) {
            return clampComponent(Math.round(parseNumber(argument, 1) * MAX_COMPONENT / 100f));
        }
        return clampComponent(Math.round(Float.parseFloat(argument) * MAX_COMPONENT));
    }

    private static float parseRatio(String argument) {
        if (argument.endsWith("%")) {
            return clampRatio(parseNumber(argument, 1) / 100f);
        }
        return clampRatio(Float.parseFloat(argument));
    }

    private static float parseHue(String argument) {
        if (argument.endsWith("deg")) {
            return parseNumber(argument, 3);
        }
        if (argument.endsWith("turn")) {
            return parseNumber(argument, 4) * 360f;
        }
        if (argument.endsWith("grad")) {
            return parseNumber(argument, 4) * 0.9f;
        }
        if (argument.endsWith("rad")) {
            return (float) Math.toDegrees(parseNumber(argument, 3));
        }
        return Float.parseFloat(argument);
    }

    private static float parseNumber(String argument, int suffixLength) {
        return Float.parseFloat(argument.substring(0, argument.length() - suffixLength));
    }

    private static int clampComponent(int value) {
        return Math.max(0, Math.min(MAX_COMPONENT, value));
    }

    private static float clampRatio(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
