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
package consulo.util.lang;

import org.jetbrains.annotations.Contract;

/**
 * @author UNV
 * @since 2026-03-01
 */
public class StringEscapeUtil {
    @Contract(pure = true)
    public static String quote(CharSequence value, char quote) {
        return escape(value, quote, new StringBuilder(value.length() + 2).append(quote)).append(quote).toString();
    }

    @Contract(mutates = "param3")
    public static StringBuilder quote(CharSequence value, char quote, StringBuilder builder) {
        builder.ensureCapacity(builder.length() + value.length() + 2);
        builder.append(quote);
        escape(value, quote, builder);
        return builder.append(quote);
    }

    @Contract(pure = true)
    public static String escape(CharSequence value, char quote) {
        return escape(value, 0, value.length(), quote);
    }

    @Contract(pure = true)
    public static String escape(CharSequence value, int fromIndex, int toIndex, char quote) {
        return escape(value, fromIndex, toIndex, quote, new StringBuilder(toIndex - fromIndex)).toString();
    }

    @Contract(mutates = "param3")
    public static StringBuilder escape(CharSequence value, char quote, StringBuilder builder) {
        return escape(value, 0, value.length(), quote, builder);
    }

    @Contract(mutates = "param5")
    public static StringBuilder escape(
        CharSequence value,
        int fromIndex,
        int toIndex,
        char quote,
        StringBuilder builder
    ) {
        for (int i = fromIndex; i < toIndex; i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");

                default -> {
                    if (ch == quote) {
                        builder.append("\\").append(ch);
                    }
                    else if (' ' <= ch && ch <= '~' || StringUtil.isPrintableUnicode(ch)) {
                        builder.append(ch);
                    }
                    else {
                        builder.append("\\u");
                        builder.append(toHexDigit((ch & 0xF000) >> 12));
                        builder.append(toHexDigit((ch & 0x0F00) >> 8));
                        builder.append(toHexDigit((ch & 0x00F0) >> 4));
                        builder.append(toHexDigit(ch & 0x000F));
                    }
                }
            }
        }
        return builder;
    }

    @Contract(pure = true)
    public static boolean isQuoted(CharSequence value, char quote) {
        return isQuoted(value, 0, value.length(), quote);
    }

    @Contract(pure = true)
    public static boolean isQuoted(CharSequence value, int fromIndex, int toIndex, char quote) {
        return toIndex - fromIndex >= 2 && value.charAt(fromIndex) == quote && value.charAt(toIndex - 1) == quote;
    }

    @Contract(pure = true)
    public static String unquote(CharSequence value, char quote) {
        return unquote(value, 0, value.length(), quote);
    }

    @Contract(pure = true)
    public static String unquote(CharSequence value, int fromIndex, int toIndex, char quote) {
        if (isQuoted(value, fromIndex, toIndex, quote)) {
            return unescape(value, fromIndex + 1, toIndex - 1);
        }
        return unescape(value, fromIndex, toIndex);
    }

    @Contract(pure = true)
    public static StringBuilder unquote(CharSequence value, char quote, StringBuilder builder) {
        return unquote(value, 0, value.length(), quote, builder);
    }

    @Contract(pure = true)
    public static StringBuilder unquote(
        CharSequence value,
        int fromIndex,
        int toIndex,
        char quote,
        StringBuilder builder
    ) {
        if (isQuoted(value, fromIndex, toIndex, quote)) {
            return unescape(value, fromIndex + 1, toIndex - 1, builder);
        }
        return unescape(value, fromIndex, toIndex, builder);
    }

    @Contract(pure = true)
    public static String unescape(CharSequence value) {
        return unescape(value, 0, value.length());
    }

    @Contract(pure = true)
    public static String unescape(CharSequence value, int fromIndex, int toIndex) {
        if (!hasEscapes(value, fromIndex, toIndex)) {
            return value.subSequence(fromIndex, toIndex).toString();
        }
        return unescape(value, fromIndex, toIndex, new StringBuilder(toIndex - fromIndex)).toString();
    }

    @Contract(pure = true)
    public static StringBuilder unescape(CharSequence s, StringBuilder builder) {
        return unescape(s, 0, s.length(), builder);
    }

    @Contract(mutates = "param3")
    public static StringBuilder unescape(CharSequence value, int fromIndex, int toIndex, StringBuilder builder) {
        boolean escaped = false;
        for (int i = fromIndex; i < toIndex; i++) {
            char ch = value.charAt(i);
            if (!escaped) {
                if (ch == '\\') {
                    escaped = true;
                }
                else {
                    builder.append(ch);
                }
            }
            else {
                switch (ch) {
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 's' -> builder.append(' ');
                    case 't' -> builder.append('\t');

                    case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                        int code = ch - '0', j = i + 1;
                        if (j < toIndex) {
                            char d = value.charAt(j);
                            if ('0' <= d && d <= '7') {
                                code = (code << 3) + d - '0';
                                i++;
                                if (ch <= '3' && ++j < toIndex) {
                                    d = value.charAt(j);
                                    if ('0' <= d && d <= '7') {
                                        code = (code << 3) + d - '0';
                                        i++;
                                    }
                                }
                            }
                        }
                        builder.append((char) code);
                    }

                    case 'u' -> {
                        int start = i - 1;
                        do {
                            i++;
                        } while (i < toIndex && value.charAt(i) == 'u');
                        if (i + 3 < toIndex) {
                            try {
                                int code = (fromHexDigit(value.charAt(i)) << 12) +
                                    (fromHexDigit(value.charAt(i + 1)) << 8) +
                                    (fromHexDigit(value.charAt(i + 2)) << 4) +
                                    fromHexDigit(value.charAt(i + 3));
                                i += 3;
                                builder.append((char) code);
                            }
                            catch (NumberFormatException e) {
                                builder.append(value, start, i);
                                i--;
                            }
                        }
                        else {
                            builder.append(value, start, i);
                            i--;
                        }
                    }

                    default -> builder.append(ch);
                }
                escaped = false;
            }
        }

        if (escaped) {
            builder.append('\\');
        }

        return builder;
    }

    @Contract(pure = true)
    private static boolean hasEscapes(CharSequence value, int fromIndex, int toIndex) {
        if (value instanceof String strValue) {
            return strValue.indexOf('\\', fromIndex, toIndex) >= 0;
        }
        for (int i = fromIndex; i < toIndex; i++) {
            if (value.charAt(i) == '\\') {
                return true;
            }
        }
        return false;
    }

    private static char toHexDigit(int value) {
        assert 0 <= value && value < 16;
        if (value <= 9) {
            return (char) ('0' + value);
        }
        return (char) ('A' - 10 + value);
    }

    private static int fromHexDigit(char digit) {
        if ('0' <= digit && digit <= '9') {
            return digit - '0';
        }
        if ('A' <= digit && digit <= 'F') {
            return digit - 'A' + 10;
        }
        if ('a' <= digit && digit <= 'f') {
            return digit - 'a' + 10;
        }
        throw new NumberFormatException("Invalid hex digit: " + digit);
    }

    private StringEscapeUtil() {
    }
}
