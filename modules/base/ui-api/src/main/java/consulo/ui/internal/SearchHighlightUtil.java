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
package consulo.ui.internal;

import consulo.ui.HasSearchHighlight;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The matching rule of {@link HasSearchHighlight}, shared by every frontend so that all of them mark the same runs of
 * a text.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public final class SearchHighlightUtil {
    private static final Set<String> SKIPPED_HTML_ELEMENTS = Set.of("head", "script", "style", "title");

    private static final char UNMATCHABLE = '\uFFFF';

    private SearchHighlightUtil() {
    }

    public static @Nullable String normalizeQuery(@Nullable String query) {
        return query == null || words(query).isEmpty() ? null : query;
    }

    public static List<String> words(String query) {
        List<String> words = new ArrayList<>();
        int start = -1;
        for (int i = 0; i <= query.length(); i++) {
            boolean separator = i == query.length() || Character.isWhitespace(query.charAt(i));
            if (separator) {
                if (start != -1) {
                    String word = query.substring(start, i);
                    if (!words.contains(word)) {
                        words.add(word);
                    }
                    start = -1;
                }
            }
            else if (start == -1) {
                start = i;
            }
        }
        return words;
    }

    public static boolean matches(String text, @Nullable String query) {
        if (query == null) {
            return false;
        }

        for (boolean matched : mask(text, words(query))) {
            if (matched) {
                return true;
            }
        }
        return false;
    }

    public static void forEachFragment(String text, @Nullable String query, BiConsumer<String, Boolean> consumer) {
        if (text.isEmpty()) {
            return;
        }

        boolean[] mask = query == null ? new boolean[text.length()] : mask(text, words(query));

        int start = 0;
        for (int i = 1; i <= text.length(); i++) {
            if (i == text.length() || mask[i] != mask[start]) {
                consumer.accept(text.substring(start, i), mask[start]);
                start = i;
            }
        }
    }

    public static String markupText(String text, @Nullable String query, String openTag, String closeTag) {
        StringBuilder builder = new StringBuilder(text.length() + 32);
        forEachFragment(text, query, (fragment, matched) -> {
            if (matched) {
                builder.append(openTag);
            }

            XmlStringUtil.escapeText(fragment, builder);

            if (matched) {
                builder.append(closeTag);
            }
        });
        return builder.toString();
    }

    public static String markupHtml(String html, @Nullable String query, String openTag, String closeTag) {
        if (query == null) {
            return html;
        }

        List<String> words = words(query);
        if (words.isEmpty()) {
            return html;
        }

        int length = html.length();
        StringBuilder visible = new StringBuilder(length);
        int[] starts = new int[length];
        int[] ends = new int[length];
        int[] runs = new int[length];

        int count = 0;
        int run = 0;
        int index = 0;
        while (index < length) {
            char c = html.charAt(index);

            if (c == '<' && isTagStart(html, index + 1)) {
                int close = html.indexOf('>', index);
                int tagEnd = close == -1 ? length : close + 1;

                String name = openingTagName(html, index + 1, tagEnd);
                index = tagEnd;

                if (name != null && SKIPPED_HTML_ELEMENTS.contains(name)) {
                    int closingTag = StringUtil.indexOfIgnoreCase(html, "</" + name, index);
                    index = closingTag == -1 ? length : closingTag;
                }

                run++;
                continue;
            }

            int entityEnd = c == '&' ? entityEnd(html, index) : -1;
            if (entityEnd != -1) {
                visible.append(decodeEntity(html.substring(index + 1, entityEnd - 1)));
                starts[count] = index;
                ends[count] = entityEnd;
            }
            else {
                visible.append(c);
                starts[count] = index;
                ends[count] = index + 1;
            }

            runs[count] = run;
            index = ends[count];
            count++;
        }

        boolean[] mask = mask(visible.toString(), words);

        StringBuilder builder = new StringBuilder(length + 64);
        int position = 0;
        for (int i = 0; i < count; i++) {
            builder.append(html, position, starts[i]);

            boolean opens = mask[i] && (i == 0 || !mask[i - 1] || runs[i - 1] != runs[i]);
            boolean closes = mask[i] && (i == count - 1 || !mask[i + 1] || runs[i + 1] != runs[i]);

            if (opens) {
                builder.append(openTag);
            }

            builder.append(html, starts[i], ends[i]);

            if (closes) {
                builder.append(closeTag);
            }

            position = ends[i];
        }
        builder.append(html, position, length);
        return builder.toString();
    }

    private static boolean[] mask(String text, List<String> words) {
        boolean[] mask = new boolean[text.length()];
        for (String word : words) {
            int wordLength = word.length();
            for (int i = 0; i + wordLength <= text.length(); i++) {
                if (text.regionMatches(true, i, word, 0, wordLength)) {
                    Arrays.fill(mask, i, i + wordLength, true);
                }
            }
        }
        return mask;
    }

    private static boolean isTagStart(String html, int index) {
        if (index >= html.length()) {
            return false;
        }

        char c = html.charAt(index);
        return Character.isLetter(c) || c == '/' || c == '!' || c == '?';
    }

    private static @Nullable String openingTagName(String html, int from, int to) {
        if (from >= to || !Character.isLetter(html.charAt(from))) {
            return null;
        }

        int end = from;
        while (end < to && Character.isLetterOrDigit(html.charAt(end))) {
            end++;
        }
        return html.substring(from, end).toLowerCase(Locale.ROOT);
    }

    private static int entityEnd(String html, int ampersand) {
        int limit = Math.min(html.length(), ampersand + 12);
        for (int i = ampersand + 1; i < limit; i++) {
            char c = html.charAt(i);
            if (c == ';') {
                return i > ampersand + 1 ? i + 1 : -1;
            }

            if (!Character.isLetterOrDigit(c) && !(c == '#' && i == ampersand + 1)) {
                return -1;
            }
        }
        return -1;
    }

    private static char decodeEntity(String name) {
        if (name.startsWith("#")) {
            try {
                int codePoint = name.length() > 1 && (name.charAt(1) == 'x' || name.charAt(1) == 'X')
                    ? Integer.parseInt(name.substring(2), 16)
                    : Integer.parseInt(name.substring(1));
                return Character.isBmpCodePoint(codePoint) ? (char) codePoint : UNMATCHABLE;
            }
            catch (NumberFormatException e) {
                return UNMATCHABLE;
            }
        }

        return switch (name) {
            case "amp" -> '&';
            case "lt" -> '<';
            case "gt" -> '>';
            case "quot" -> '"';
            case "apos" -> '\'';
            case "nbsp" -> '\u00A0';
            default -> UNMATCHABLE;
        };
    }
}
