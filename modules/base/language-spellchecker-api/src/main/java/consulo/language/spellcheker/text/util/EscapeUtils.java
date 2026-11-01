package consulo.language.spellcheker.text.util;

import consulo.document.util.TextRange;
import consulo.util.lang.StringCharactersParser;
import consulo.language.spellcheker.text.TextContent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class EscapeUtils {
    private EscapeUtils() {
    }

    /**
     * Replaces backslash-escaped characters in the given {@link TextContent} object with their corresponding characters
     * and marks problematic ranges or characters that cannot be resolved properly as {@link TextContent.ExclusionKind#unknown}.
     * <p>
     * This includes handling sequences like {@code \n} and {@code \t} by translating them into their whitespace character equivalents.
     * It also tracks offsets to identify and exclude invalid or unresolvable ranges in the text.
     */
    public static TextContent replaceBackslashEscapes(TextContent content) {
        TextContent text = replaceBackslashEscapedWhitespace(content);
        int[] offsets = new int[text.length() + 1];
        StringCharactersParser.parseStringCharacters(text.toString(), offsets);
        List<TextContent.Exclusion> exclusions = new ArrayList<>();
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i] != 0 && offsets[i] - offsets[i - 1] != 1) {
                exclusions.add(TextContent.Exclusion.markUnknown(new TextRange(offsets[i - 1], offsets[i])));
            }
        }
        return text.excludeRanges(exclusions);
    }

    /**
     * Same as {@link #replaceBackslashEscapedWhitespace(TextContent, char)}, but escapes only {@code \n} and {@code \t}.
     */
    public static TextContent replaceBackslashEscapedWhitespace(TextContent content) {
        return replaceBackslashEscapedWhitespace(replaceBackslashEscapedWhitespace(content, 'n'), 't');
    }

    /**
     * Same as {@link #replaceBackslashEscapedWhitespace(TextContent, char)}, but escapes only vertical tab ({@code \v}).
     */
    public static TextContent replaceEscapedVerticalTab(TextContent content) {
        return replaceBackslashEscapedWhitespace(content, 'v');
    }

    /**
     * Replaces occurrences of backslash-escaped whitespace-like characters in the given {@link TextContent} object
     * with their respective normalized whitespace representations.
     */
    public static TextContent replaceBackslashEscapedWhitespace(TextContent content, char separator) {
        List<TextRange> excluded = getBackslashExcludeRanges(content, separator);
        if (excluded.isEmpty()) {
            return content;
        }

        List<TextContent> components = new ArrayList<>();
        for (TextRange range : invertExcludedToContentRanges(excluded, content.length())) {
            TextContent sub = content.subText(range);
            if (sub != null) {
                components.add(sub);
            }
        }
        return switch (components.size()) {
            case 0 -> content;
            case 1 -> components.get(0);
            default -> Objects.requireNonNull(TextContent.joinWithWhitespace(mapSeparator(separator), components));
        };
    }

    private static List<TextRange> getBackslashExcludeRanges(TextContent content, char symbol) {
        if (content.length() < 2) {
            return List.of();
        }
        List<TextRange> ranges = new ArrayList<>();
        int i = 0;
        while (i < content.length() - 1) {
            if (content.charAt(i) == '\\' && content.charAt(i + 1) == '\\') {
                i += 2;
            }
            else if (isSeparator(content, i, symbol)) {
                ranges.add(new TextRange(i, i + 2));
                i += 2;
            }
            else {
                i++;
            }
        }
        return ranges;
    }

    /**
     * Inverts a list of excluded ranges to get the remaining content ranges.
     * Input array must be sorted by start offset.
     * <p>
     * Example:
     * For ranges [[2, 3], [5, 7]] and length 12 this method will return [[0, 2], [3, 5], [7, 12]]
     */
    private static List<TextRange> invertExcludedToContentRanges(List<TextRange> excluded, int length) {
        if (excluded.isEmpty()) {
            return List.of(new TextRange(0, length));
        }
        List<TextRange> result = new ArrayList<>(excluded.size() + 1);

        TextRange first = excluded.get(0);
        TextRange last = excluded.get(excluded.size() - 1);
        if (first.getStartOffset() > 0) {
            result.add(new TextRange(0, first.getStartOffset()));
        }
        for (int idx = 0; idx < excluded.size() - 1; idx++) {
            TextRange current = excluded.get(idx);
            TextRange next = excluded.get(idx + 1);
            if (next.getStartOffset() > current.getEndOffset()) {
                result.add(new TextRange(current.getEndOffset(), next.getStartOffset()));
            }
        }
        if (length > last.getEndOffset()) {
            result.add(new TextRange(last.getEndOffset(), length));
        }

        result.sort(Comparator.comparingInt(TextRange::getStartOffset));
        return result;
    }

    private static boolean isSeparator(CharSequence text, int index, char symbol) {
        return text.charAt(index) == '\\' && text.charAt(index + 1) == symbol;
    }

    private static char mapSeparator(char symbol) {
        return switch (symbol) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'b' -> ' ';
            case 'r' -> '\r';
            case 'f' -> '\u000C';
            case 'v' -> (char) 0x0b;
            default -> ' ';
        };
    }
}
