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

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Contract;

/**
 * @author UNV
 * @since 2026-03-01
 */
public class StringEscapeUtil {
    @Contract(pure = true)
    @Nonnull
    public static String quote(@Nonnull CharSequence value, char quote) {
        return escape(value, quote, new StringBuilder(value.length() + 2).append(quote)).append(quote).toString();
    }

    @Contract(mutates = "param3")
    @Nonnull
    public static StringBuilder quote(@Nonnull CharSequence value, char quote, @Nonnull StringBuilder builder) {
        builder.ensureCapacity(builder.length() + value.length() + 2);
        builder.append(quote);
        escape(value, quote, builder);
        return builder.append(quote);
    }

    @Contract(pure = true)
    @Nonnull
    public static String escape(@Nonnull CharSequence value, char quote) {
        return escape(value, 0, value.length(), quote);
    }

    @Contract(pure = true)
    @Nonnull
    public static String escape(@Nonnull CharSequence value, int fromIndex, int toIndex, char quote) {
        return escape(value, fromIndex, toIndex, quote, new StringBuilder(toIndex - fromIndex)).toString();
    }

    @Contract(mutates = "param3")
    public static @Nonnull StringBuilder escape(@Nonnull CharSequence value, char quote, @Nonnull StringBuilder builder) {
        return escape(value, 0, value.length(), quote, builder);
    }

    @Contract(mutates = "param5")
    public static @Nonnull StringBuilder escape(
        @Nonnull CharSequence value,
        int fromIndex,
        int toIndex,
        char quote,
        @Nonnull StringBuilder builder
    ) {
        for (int i = fromIndex; i < toIndex; i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case 0 -> builder.append("\\0");
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
    @Nonnull
    public static String unquote(@Nonnull CharSequence value, char quote) {
        return unquote(value, 0, value.length(), quote);
    }

    @Contract(pure = true)
    @Nonnull
    public static String unquote(@Nonnull CharSequence value, int fromIndex, int toIndex, char quote) {
        if (toIndex - fromIndex >= 2 && value.charAt(fromIndex) == quote && value.charAt(toIndex - 1) == quote) {
            return unescape(value, fromIndex + 1, toIndex - 1);
        }
        return unescape(value, fromIndex, toIndex);
    }

    @Contract(pure = true)
    @Nonnull
    public static StringBuilder unquote(@Nonnull CharSequence value, char quote, @Nonnull StringBuilder builder) {
        return unquote(value, 0, value.length(), quote, builder);
    }

    @Contract(pure = true)
    @Nonnull
    public static StringBuilder unquote(
        @Nonnull CharSequence value,
        int fromIndex,
        int toIndex,
        char quote,
        @Nonnull StringBuilder builder
    ) {
        if (toIndex - fromIndex >= 2 && value.charAt(fromIndex) == quote && value.charAt(toIndex - 1) == quote) {
            return unescape(value, fromIndex + 1, toIndex - 1, builder);
        }
        return unescape(value, fromIndex, toIndex, builder);
    }

    @Contract(pure = true)
    @Nonnull
    public static String unescape(@Nonnull CharSequence value) {
        return unescape(value, 0, value.length());
    }

    @Contract(pure = true)
    @Nonnull
    public static String unescape(@Nonnull CharSequence value, int fromIndex, int toIndex) {
        return unescape(value, fromIndex, toIndex, new StringBuilder(toIndex - fromIndex)).toString();
    }

    @Contract(pure = true)
    @Nonnull
    public static StringBuilder unescape(@Nonnull CharSequence s, @Nonnull StringBuilder builder) {
        return unescape(s, 0, s.length(), builder);
    }

    @Contract(mutates = "param3")
    public static StringBuilder unescape(@Nonnull CharSequence value, int fromIndex, int toIndex, @Nonnull StringBuilder builder) {
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
                    case '\'' -> builder.append('\'');
                    case '\"' -> builder.append('\"');
                    case '\\' -> builder.append('\\');
                    case '0' -> builder.append('\0');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');

                    case 'u' -> {
                        if (i + 4 < toIndex) {
                            try {
                                int code = (fromHexDigit(value.charAt(i + 1)) << 12) +
                                    (fromHexDigit(value.charAt(i + 2)) << 8) +
                                    (fromHexDigit(value.charAt(i + 3)) << 4) +
                                    fromHexDigit(value.charAt(i + 4));
                                i += 4;
                                builder.append((char) code);
                            }
                            catch (NumberFormatException e) {
                                builder.append("\\u");
                            }
                        }
                        else {
                            builder.append("\\u");
                        }
                    }

                    default -> builder.append('\\').append(ch);
                }
                escaped = false;
            }
        }

        if (escaped) {
            builder.append('\\');
        }

        return builder;
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
}
