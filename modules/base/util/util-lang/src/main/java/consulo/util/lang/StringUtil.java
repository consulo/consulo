/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.util.lang.internal.NaturalComparator;
import consulo.util.lang.xml.XmlStringUtil;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Based on IDEA code
 */
public final class StringUtil {
    /**
     * Expirable CharSequence. Very useful to control external library execution time,
     * i.e. when java.util.regex.Pattern match goes out of control.
     */
    public abstract static class BombedCharSequence implements CharSequence {
        private final CharSequence delegate;
        private int i;
        private boolean myDefused;

        public BombedCharSequence(CharSequence sequence) {
            delegate = sequence;
        }

        @Override
        public int length() {
            check();
            return delegate.length();
        }

        @Override
        public char charAt(int i) {
            check();
            return delegate.charAt(i);
        }

        protected void check() {
            if (myDefused) {
                return;
            }
            if ((++i & 1023) == 0) {
                checkCanceled();
            }
        }

        public final void defuse() {
            myDefused = true;
        }

        @Override
        public String toString() {
            check();
            return delegate.toString();
        }

        protected abstract void checkCanceled();

        @Override
        public CharSequence subSequence(int i, int i1) {
            check();
            return delegate.subSequence(i, i1);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(StringUtil.class);

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String[] MN_QUOTED = {"&&", "__"};
    private static final String[] MN_CHARS = {"&", "_"};

    private static final String[] REPLACES_REFS = {"&lt;", "&gt;", "&amp;", "&#39;", "&quot;"};
    private static final String[] REPLACES_DISP = {"<", ">", "&", "'", "\""};

    private static final Pattern EOL_SPLIT_PATTERN = Pattern.compile(" *(\r|\n|\r\n)+ *");
    private static final Pattern EOL_SPLIT_PATTERN_WITH_EMPTY = Pattern.compile(" *(\r|\n|\r\n) *");

    private static final String VOWELS = "aeiouy";

    public static final Function<String, String> QUOTER = s -> "\"" + s + "\"";

    public static final Function<String, String> SINGLE_QUOTER = s -> "'" + s + "'";

    @Contract(pure = true)
    public static List<String> getWordsInStringLongestFirst(String find) {
        List<String> words = getWordsIn(find);
        if (words.isEmpty()) {
            return words;
        }

        // hope long words are rare
        words.sort((o1, o2) -> o2.length() - o1.length());
        return words;
    }

    @Contract(pure = true)
    public static String escapePattern(String text) {
        return replace(replace(text, "'", "''"), "{", "'{'");
    }

    @Contract(pure = true)
    public static String replace(String text, String oldS, String newS) {
        return replace(text, oldS, newS, false);
    }

    @Contract(pure = true)
    public static String replaceIgnoreCase(String text, String oldS, String newS) {
        return replace(text, oldS, newS, true);
    }

    @Contract(pure = true)
    public static String replace(String text, String oldS, String newS, boolean ignoreCase) {
        if (text.length() < oldS.length()) {
            return text;
        }

        StringBuilder newText = null;
        int i = 0;

        while (i < text.length()) {
            int index = ignoreCase ? indexOfIgnoreCase(text, oldS, i) : text.indexOf(oldS, i);
            if (index < 0) {
                if (i == 0) {
                    return text;
                }
                assert newText != null;
                newText.append(text, i, text.length());
                break;
            }
            else {
                if (newText == null) {
                    if (text.length() == oldS.length()) {
                        return newS;
                    }
                    newText = new StringBuilder(text.length() - i);
                }

                newText.append(text, i, index);
                newText.append(newS);
                i = index + oldS.length();
            }
        }
        return newText != null ? newText.toString() : "";
    }

    @Contract(pure = true)
    @Deprecated
    public static String replaceChar(String buffer, char oldChar, char newChar) {
        return buffer.replace(oldChar, newChar);
    }

    @Contract(pure = true)
    public static String replace(String text, String[] from, String[] to) {
        return replace(text, Arrays.asList(from), Arrays.asList(to));
    }

    @Contract(pure = true)
    public static String replace(String text, List<String> from, List<String> to) {
        assert from.size() == to.size();
        StringBuilder result = new StringBuilder(text.length());
        replace:
        for (int i = 0, n = text.length(); i < n; i++) {
            for (int j = 0, m = from.size(); j < m; j++) {
                String toReplace = from.get(j);
                String replaceWith = to.get(j);

                int len = toReplace.length();
                if (text.regionMatches(i, toReplace, 0, len)) {
                    result.append(replaceWith);
                    i += len - 1;
                    continue replace;
                }
            }
            result.append(text.charAt(i));
        }
        return result.toString();
    }

    @Contract(pure = true)
    public static int indexOfIgnoreCase(String where, String what, int fromIndex) {
        return indexOfIgnoreCase((CharSequence) where, what, fromIndex);
    }

    /**
     * Implementation copied from {@link String#indexOf(String, int)} except character comparisons made case-insensitive
     */
    @Contract(pure = true)
    public static int indexOfIgnoreCase(CharSequence where, CharSequence what, int fromIndex) {
        int targetCount = what.length();
        int sourceCount = where.length();

        if (fromIndex >= sourceCount) {
            return targetCount == 0 ? sourceCount : -1;
        }

        if (fromIndex < 0) {
            fromIndex = 0;
        }

        if (targetCount == 0) {
            return fromIndex;
        }

        char first = what.charAt(0);
        int max = sourceCount - targetCount;

        for (int i = fromIndex; i <= max; i++) {
            /* Look for the first character. */
            if (!charsEqualIgnoreCase(where.charAt(i), first)) {
                //noinspection StatementWithEmptyBody,AssignmentToForLoopParameter
                while (++i <= max && !charsEqualIgnoreCase(where.charAt(i), first)) {
                }
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                //noinspection StatementWithEmptyBody
                for (int k = 1; j < end && charsEqualIgnoreCase(where.charAt(j), what.charAt(k)); j++, k++) {
                }

                if (j == end) {
                    /* Found whole string. */
                    return i;
                }
            }
        }

        return -1;
    }

    @Contract(pure = true)
    public static int indexOfIgnoreCase(String where, char what, int fromIndex) {
        int sourceCount = where.length();

        if (fromIndex >= sourceCount) {
            return -1;
        }

        if (fromIndex < 0) {
            fromIndex = 0;
        }

        for (int i = fromIndex; i < sourceCount; i++) {
            if (charsEqualIgnoreCase(where.charAt(i), what)) {
                return i;
            }
        }

        return -1;
    }

    @Contract(pure = true)
    public static int lastIndexOfIgnoreCase(String where, char c, int fromIndex) {
        for (int i = Math.min(fromIndex, where.length() - 1); i >= 0; i--) {
            if (charsEqualIgnoreCase(where.charAt(i), c)) {
                return i;
            }
        }

        return -1;
    }

    @Contract(pure = true)
    public static boolean containsIgnoreCase(String where, String what) {
        return indexOfIgnoreCase(where, what, 0) >= 0;
    }

    @Contract(pure = true)
    public static String stripHtml(String html, boolean convertBreaks) {
        if (convertBreaks) {
            html = html.replaceAll("<br/?>", "\n\n");
        }

        return html.replaceAll("<(.|\n)*?>", "");
    }


    public static boolean isAscii(String str) {
        return isAscii((CharSequence) str);
    }

    @Contract(pure = true)
    public static String htmlEmphasize(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 20);
        builder.append("<b><code>");
        XmlStringUtil.escapeText(text, builder);
        builder.append("</code></b>");
        return builder.toString();
    }

    public static boolean isAscii(CharSequence str) {
        for (int i = 0, length = str.length(); i < length; ++i) {
            if (str.charAt(i) >= 128) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAscii(char c) {
        return c < 128;
    }

    @Contract(pure = true)
    public static String defaultIfEmpty(@Nullable String value, String defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }

    @Contract(pure = true)
    public static String firstLast(String text, int length) {
        return text.length() > length ? text.subSequence(0, length / 2) + "\u2026" + text.subSequence(
            text.length() - length / 2 - 1,
            text.length()
        ) : text;
    }

    @Contract(pure = true)
    public static boolean containsAnyChar(String value, String chars) {
        if (chars.length() > value.length()) {
            return containsAnyChar(value, chars, 0, value.length());
        }
        else {
            return containsAnyChar(chars, value, 0, chars.length());
        }
    }

    @Contract(pure = true)
    public static boolean containsAnyChar(String value, String chars, int start, int end) {
        for (int i = start; i < end; i++) {
            if (chars.indexOf(value.charAt(i)) >= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns unpluralized variant using English based heuristics like properties -> property, names -> name, children -> child.
     * Returns <code>null</code> if failed to match appropriate heuristic.
     *
     * @param name english word in plural form
     * @return name in singular form or <code>null</code> if failed to find one.
     */
    @SuppressWarnings({"HardCodedStringLiteral"})
    @Nullable
    @Contract(pure = true)
    public static String unpluralize(String name) {
        if (name.endsWith("sses") || name.endsWith("shes") || name.endsWith("ches") || name.endsWith("xes")) { //?
            return name.substring(0, name.length() - 2);
        }

        if (name.endsWith("ses")) {
            return name.substring(0, name.length() - 1);
        }

        if (name.endsWith("ies")) {
            if (name.endsWith("cookies") || name.endsWith("Cookies")) {
                return name.substring(0, name.length() - "ookies".length()) + "ookie";
            }

            return name.substring(0, name.length() - 3) + "y";
        }

        if (name.endsWith("leaves") || name.endsWith("Leaves")) {
            return name.substring(0, name.length() - "eaves".length()) + "eaf";
        }

        String result = stripEnding(name, "s");
        if (result != null) {
            return result;
        }

        if (name.endsWith("children")) {
            return name.substring(0, name.length() - "children".length()) + "child";
        }

        if (name.endsWith("Children") && name.length() > "Children".length()) {
            return name.substring(0, name.length() - "Children".length()) + "Child";
        }

        return null;
    }

    @Nullable
    @Contract(pure = true)
    private static String stripEnding(String name, String ending) {
        if (name.endsWith(ending)) {
            if (name.equals(ending)) {
                return name; // do not return empty string
            }
            return name.substring(0, name.length() - 1);
        }
        return null;
    }

    @Contract(pure = true)
    public static boolean isVowel(char c) {
        return VOWELS.indexOf(c) >= 0;
    }

    /**
     * @see #getPackageName(String, char)
     */
    @Contract(pure = true)
    public static String getPackageName(String fqName) {
        return getPackageName(fqName, '.');
    }

    /**
     * Given a fqName returns the package name for the type <i>or the containing type</i>.
     * <p/>
     * <ul>
     * <li>{@code java.lang.String} -> {@code java.lang}</li>
     * <li>{@code java.util.Map.Entry} -> {@code java.util.Map}</li>
     * </ul>
     *
     * @param fqName    a fully qualified type name. Not supposed to contain any type arguments
     * @param separator the separator to use. Typically, '.'
     * @return the package name of the type or the declarator of the type. The empty string if the given {@code fqName} is unqualified
     */
    @Contract(pure = true)
    public static String getPackageName(String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(0, lastPointIdx);
        }
        return "";
    }

    @Contract(pure = true)
    public static boolean equalsIgnoreCase(@Nullable CharSequence s1, @Nullable CharSequence s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }

        int n = s1.length();
        if (n != s2.length()) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (!charsMatch(s1.charAt(i), s2.charAt(i), true)) {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public static boolean charsMatch(char c1, char c2, boolean ignoreCase) {
        return compare(c1, c2, ignoreCase) == 0;
    }

    @Contract(pure = true)
    public static String replaceSubstring(String original, int startOffset, int endOffset, String replacement) {
        try {
            String beginning = original.substring(0, startOffset);
            String ending = original.substring(endOffset, original.length());
            return beginning + replacement + ending;
        }
        catch (StringIndexOutOfBoundsException e) {
            throw new StringIndexOutOfBoundsException(
                "Can't replace " + startOffset + ":" + endOffset + " range from '" + original + "' with '" + replacement + "'"
            );
        }
    }

    @Contract(pure = true)
    public static boolean isChar(CharSequence seq, int index, char c) {
        return index >= 0 && index < seq.length() && seq.charAt(index) == c;
    }

    @Contract(pure = true)
    public static int hashCode(CharSequence s) {
        return stringHashCode(s);
    }

    /**
     * Allows to answer if target symbol is contained at given char sequence at <code>[start; end)</code> interval.
     *
     * @param s     target char sequence to check
     * @param start start offset to use within the given char sequence (inclusive)
     * @param end   end offset to use within the given char sequence (exclusive)
     * @param c     target symbol to check
     * @return <code>true</code> if given symbol is contained at the target range of the given char sequence;
     * <code>false</code> otherwise
     */
    @Contract(pure = true)
    public static boolean contains(CharSequence s, int start, int end, char c) {
        return indexOf(s, c, start, end) >= 0;
    }

    @Contract(pure = true)
    public static boolean contains(CharSequence sequence, CharSequence infix) {
        return indexOf(sequence, infix) >= 0;
    }

    @Contract(pure = true)
    public static int indexOf(CharSequence sequence, CharSequence infix) {
        return indexOf(sequence, infix, 0);
    }

    @Contract(pure = true)
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#escape")
    public static String escapeStringCharacters(CharSequence s) {
        return StringEscapeUtil.escape(s, '"');
    }

    @Contract(mutates = "param2")
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#escape")
    public static StringBuilder escapeStringCharacters(CharSequence s, StringBuilder buffer) {
        return StringEscapeUtil.escape(s, '"', buffer);
    }

    @Contract(mutates = "param3")
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#escape")
    @SuppressWarnings("deprecation")
    public static void escapeStringCharacters(int length, String s, StringBuilder buffer) {
        StringEscapeUtil.escape(s, 0, length, '"', buffer);
    }

    @Contract(mutates = "param4")
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#escape")
    @SuppressWarnings("deprecation")
    public static StringBuilder escapeStringCharacters(
        int length,
        String str,
        @Nullable String additionalChars,
        StringBuilder buffer
    ) {
        return escapeStringCharacters(length, str, additionalChars, true, buffer);
    }

    @Contract(mutates = "param5")
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#escape")
    @SuppressWarnings("deprecation")
    public static StringBuilder escapeStringCharacters(
        int length,
        String str,
        @Nullable String additionalChars,
        boolean escapeSlash,
        StringBuilder buffer
    ) {
        return escapeStringCharacters(length, str, additionalChars, escapeSlash, true, buffer);
    }

    @Contract(mutates = "param6")
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#escape")
    public static StringBuilder escapeStringCharacters(
        int length,
        String str,
        @Nullable String additionalChars,
        boolean escapeSlash,
        boolean escapeUnicode,
        StringBuilder buffer
    ) {
        char prev = 0;
        for (int idx = 0; idx < length; idx++) {
            char ch = str.charAt(idx);
            switch (ch) {
                case '\b' -> buffer.append("\\b");
                case '\f' -> buffer.append("\\f");
                case '\n' -> buffer.append("\\n");
                case '\r' -> buffer.append("\\r");
                case '\t' -> buffer.append("\\t");

                case '\\' -> {
                    if (escapeSlash) {
                        buffer.append("\\\\");
                    }
                    else {
                        buffer.append(ch);
                    }
                }

                default -> {
                    if (additionalChars != null && additionalChars.indexOf(ch) > -1 && (escapeSlash || prev != '\\')) {
                        buffer.append("\\").append(ch);
                    }
                    else if (escapeUnicode && !isPrintableUnicode(ch)) {
                        buffer.append("\\u");
                        CharSequence hexCode = toUpperCase(Integer.toHexString(ch));
                        for (int paddingCount = 4 - hexCode.length(); --paddingCount >= 0; ) {
                            buffer.append('0');
                        }
                        buffer.append(hexCode);
                    }
                    else {
                        buffer.append(ch);
                    }
                }
            }
            prev = ch;
        }
        return buffer;
    }

    @Contract(pure = true)
    public static boolean isPrintableUnicode(char c) {
        int t = Character.getType(c);
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return t != Character.UNASSIGNED
            && t != Character.LINE_SEPARATOR
            && t != Character.PARAGRAPH_SEPARATOR
            && t != Character.CONTROL
            && t != Character.FORMAT
            && t != Character.PRIVATE_USE
            && t != Character.SURROGATE
            && block != Character.UnicodeBlock.VARIATION_SELECTORS
            && block != Character.UnicodeBlock.VARIATION_SELECTORS_SUPPLEMENT;
    }

    @Contract(pure = true)
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#escape")
    public static String escapeCharCharacters(CharSequence s) {
        return StringEscapeUtil.escape(s, '\'');
    }

    private static boolean isQuoteAt(String s, int ind) {
        char ch = s.charAt(ind);
        return ch == '\'' || ch == '\"';
    }

    @Contract(pure = true)
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#isQuoted with specific quote sign")
    public static boolean isQuotedString(String s) {
        return s.length() > 1 && isQuoteAt(s, 0) && s.charAt(0) == s.charAt(s.length() - 1);
    }

    @Contract(pure = true)
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#unquote (it unescapes as well)")
    public static String unquoteString(String s) {
        if (isQuotedString(s)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    @Contract(pure = true)
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#unquote (it unescapes as well)")
    public static String unquoteString(String s, char quotationChar) {
        if (s.length() > 1 && quotationChar == s.charAt(0) && quotationChar == s.charAt(s.length() - 1)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    @Contract(pure = true)
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#unescape")
    public static String unescapeStringCharacters(CharSequence s) {
        return StringEscapeUtil.unescape(s);
    }

    @Contract(mutates = "param3")
    @Deprecated
    @DeprecationInfo("Use StringEscapeUtil#unescape")
    public static void unescapeStringCharacters(int length, String s, StringBuilder buffer) {
        StringEscapeUtil.unescape(s, 0, length, buffer);
    }

    @Contract(pure = true)
    @SuppressWarnings({"HardCodedStringLiteral"})
    public static String pluralize(String word) {
        if (word.endsWith("Child") || word.endsWith("child")) {
            return word + "ren";
        }

        if (word.equals("this")) {
            return "these";
        }
        if (word.equals("This")) {
            return "These";
        }

        if (endsWithIgnoreCase(word, "es")) {
            return word;
        }

        if (endsWithIgnoreCase(word, "s") || endsWithIgnoreCase(word, "x") || endsWithIgnoreCase(word, "ch")) {
            return word + "es";
        }

        int len = word.length();
        if (endsWithIgnoreCase(word, "y") && len > 1 && !isVowel(toLowerCase(word.charAt(len - 2)))) {
            return word.substring(0, len - 1) + "ies";
        }

        return word + "s";
    }

    @Contract(pure = true)
    public static String capitalizeWords(String text, boolean allWords) {
        return capitalizeWords(text, " \t\n\r\f", allWords, false);
    }

    @Contract(pure = true)
    public static String capitalizeWords(String text, String tokenizerDelim, boolean allWords, boolean leaveOriginalDelims) {
        StringTokenizer tokenizer = new StringTokenizer(text, tokenizerDelim, leaveOriginalDelims);
        StringBuilder out = new StringBuilder(text.length());
        boolean toCapitalize = true;
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            if (!leaveOriginalDelims && out.length() > 0) {
                out.append(' ');
            }
            out.append(toCapitalize ? capitalize(word) : word);
            if (!allWords) {
                toCapitalize = false;
            }
        }
        return out.toString();
    }

    @Contract(value = "null -> null", pure = true)
    @Nullable
    public static String decapitalize(@Nullable String s) {
        if (isEmpty(s)) {
            return s;
        }
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1)) && Character.isUpperCase(s.charAt(0))) {
            return s;
        }
        char chars[] = s.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    @Contract(pure = true)
    public static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        if (s.length() == 1) {
            return toUpperCase(s);
        }

        // Optimization
        if (Character.isUpperCase(s.charAt(0))) {
            return s;
        }
        return toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Contract(value = "null -> false", pure = true)
    public static boolean isCapitalized(@Nullable String s) {
        return s != null && !s.isEmpty() && Character.isUpperCase(s.charAt(0));
    }

    @Contract(pure = true)
    public static String escapeToRegexp(String text) {
        StringBuilder result = new StringBuilder(text.length());
        return escapeToRegexp(text, result).toString();
    }

    @Contract(pure = true)
    public static List<String> findMatches(String s, Pattern pattern) {
        return findMatches(s, pattern, 1);
    }

    @Contract(pure = true)
    public static List<String> findMatches(String s, Pattern pattern, int groupIndex) {
        List<String> result = new ArrayList<>();
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            String group = m.group(groupIndex);
            if (group != null) {
                result.add(group);
            }
        }
        return result;
    }

    /**
     * Strip out all characters not accepted by given filter
     *
     * @param s      e.g. "/n    my string "
     * @param filter e.g. {@link CharFilter#NOT_WHITESPACE_FILTER}
     * @return stripped string e.g. "mystring"
     */
    @Contract(pure = true)
    public static String strip(String s, CharFilter filter) {
        StringBuilder result = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (filter.accept(ch)) {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * Splits string by lines.
     *
     * @param string String to split
     * @return array of strings
     */
    @Contract(pure = true)
    public static String[] splitByLines(String string) {
        return splitByLines(string, true);
    }

    /**
     * Splits string by lines. If several line separators are in a row corresponding empty lines
     * are also added to result if {@code excludeEmptyStrings} is {@code false}.
     *
     * @param string String to split
     * @return array of strings
     */
    @Contract(pure = true)
    public static String[] splitByLines(String string, boolean excludeEmptyStrings) {
        return (excludeEmptyStrings ? EOL_SPLIT_PATTERN : EOL_SPLIT_PATTERN_WITH_EMPTY).split(string);
    }

    @Contract(pure = true)
    public static List<Pair<String, Integer>> getWordsWithOffset(String s) {
        List<Pair<String, Integer>> result = new ArrayList<>();
        StringBuilder name = new StringBuilder();
        int startInd = -1;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                if (!name.isEmpty()) {
                    result.add(Pair.create(name.toString(), startInd));
                    name.setLength(0);
                    startInd = -1;
                }
            }
            else {
                if (startInd == -1) {
                    startInd = i;
                }
                name.append(s.charAt(i));
            }
        }
        if (!name.isEmpty()) {
            result.add(Pair.create(name.toString(), startInd));
        }
        return result;
    }

    @Contract(pure = true)
    public static int parseInt(@Nullable String string, int defaultValue) {
        if (string != null) {
            try {
                return Integer.parseInt(string);
            }
            catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    @Contract(pure = true)
    public static long parseLong(@Nullable String string, long defaultValue) {
        if (string != null) {
            try {
                return Long.parseLong(string);
            }
            catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    @Contract(pure = true)
    public static double parseDouble(@Nullable String string, double defaultValue) {
        if (string != null) {
            try {
                return Double.parseDouble(string);
            }
            catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    @Deprecated
    @Contract(pure = true)
    public static boolean parseBoolean(String string, boolean defaultValue) {
        return Boolean.parseBoolean(string);
    }

    @Contract(pure = true)
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        int stringLength = str.length();
        int prefixLength = prefix.length();
        return stringLength >= prefixLength && str.regionMatches(true, 0, prefix, 0, prefixLength);
    }

    public static StringBuilder escapeToRegexp(CharSequence text, StringBuilder builder) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                builder.append("\\n");
            }
            else if (c == '\r') {
                builder.append("\\r");
            }
            else if (".$|()[]{}^?*+\\".indexOf(c) >= 0) {
                // escaping ']' and '}' is not required for most regex dialects,
                // but we do it for maximum compatibility
                builder.append('\\').append(c);
            }
            else {
                builder.append(c);
            }
        }

        return builder;
    }

    @Contract(pure = true)
    public static String shortenTextWithEllipsis(String text, int maxLength, int suffixLength) {
        return shortenTextWithEllipsis(text, maxLength, suffixLength, false);
    }

    @Contract(pure = true)
    public static String trimMiddle(String text, int maxLength, boolean useEllipsisSymbol) {
        return shortenTextWithEllipsis(text, maxLength, useEllipsisSymbol ? maxLength >> 1 : (maxLength >> 1) - 1, useEllipsisSymbol);
    }

    @Contract(pure = true)
    public static String trimMiddle(String text, int maxLength) {
        return shortenTextWithEllipsis(text, maxLength, maxLength >> 1, true);
    }

    /**
     * Converts line separators to <code>"\n"</code>
     */
    @Contract(pure = true)
    public static String convertLineSeparators(String text) {
        return convertLineSeparators(text, false);
    }

    @Contract(pure = true)
    public static String trimLog(String text, int limit) {
        if (limit > 5 && text.length() > limit) {
            return text.substring(0, limit - 5) + " ...\n";
        }
        return text;
    }

    public static void quote(StringBuilder builder) {
        quote(builder, '\"');
    }

    public static void quote(StringBuilder builder, char quotingChar) {
        builder.insert(0, quotingChar);
        builder.append(quotingChar);
    }

    @Contract(pure = true)
    public static String escapeQuotes(String str) {
        return escapeChar(str, '"');
    }

    public static void escapeQuotes(StringBuilder buf) {
        escapeChar(buf, '"');
    }

    @Contract(pure = true)
    public static String convertLineSeparators(String text, boolean keepCarriageReturn) {
        return convertLineSeparators(text, "\n", null, keepCarriageReturn);
    }

    @Contract(pure = true)
    public static String convertLineSeparators(String text, String newSeparator) {
        return convertLineSeparators(text, newSeparator, null);
    }

    @Contract(pure = true)
    public static CharSequence convertLineSeparators(CharSequence text, String newSeparator) {
        return unifyLineSeparators(text, newSeparator, null, false);
    }

    public static String convertLineSeparators(String text, String newSeparator, int @Nullable [] offsetsToKeep) {
        return convertLineSeparators(text, newSeparator, offsetsToKeep, false);
    }

    public static String convertLineSeparators(
        String text,
        String newSeparator,
        int @Nullable [] offsetsToKeep,
        boolean keepCarriageReturn
    ) {
        return unifyLineSeparators(text, newSeparator, offsetsToKeep, keepCarriageReturn).toString();
    }

    @Contract(pure = true)
    public static CharSequence unifyLineSeparators(CharSequence text) {
        return unifyLineSeparators(text, "\n", null, false);
    }

    @Contract(pure = true)
    public static String shortenTextWithEllipsis(String text, int maxLength, int suffixLength, String symbol) {
        int textLength = text.length();
        if (textLength > maxLength) {
            int prefixLength = maxLength - suffixLength - symbol.length();
            if (prefixLength <= 0) {
                throw new IllegalArgumentException(
                    "prefixLength = " + prefixLength +
                        " for given textLength = " + textLength +
                        ", maxLength = " + maxLength +
                        " and suffixLength = " + suffixLength
                );
            }
            return text.substring(0, prefixLength) + symbol + text.substring(textLength - suffixLength);
        }
        else {
            return text;
        }
    }

    @Contract(pure = true)
    public static String shortenTextWithEllipsis(String text, int maxLength, int suffixLength, boolean useEllipsisSymbol) {
        String symbol = useEllipsisSymbol ? "\u2026" : "...";
        return shortenTextWithEllipsis(text, maxLength, suffixLength, symbol);
    }

    @Contract(pure = true)
    public static String shortenPathWithEllipsis(String path, int maxLength, boolean useEllipsisSymbol) {
        return shortenTextWithEllipsis(path, maxLength, (int) (maxLength * 0.7), useEllipsisSymbol);
    }

    @Contract(pure = true)
    public static String shortenPathWithEllipsis(String path, int maxLength) {
        return shortenPathWithEllipsis(path, maxLength, false);
    }

    public static CharSequence unifyLineSeparators(
        CharSequence text,
        String newSeparator,
        int @Nullable [] offsetsToKeep,
        boolean keepCarriageReturn
    ) {
        StringBuilder buffer = null;
        int intactLength = 0;
        boolean newSeparatorIsSlashN = "\n".equals(newSeparator);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                if (!newSeparatorIsSlashN) {
                    if (buffer == null) {
                        buffer = new StringBuilder(text.length());
                        buffer.append(text, 0, intactLength);
                    }
                    buffer.append(newSeparator);
                    shiftOffsets(offsetsToKeep, buffer.length(), 1, newSeparator.length());
                }
                else if (buffer == null) {
                    intactLength++;
                }
                else {
                    buffer.append(c);
                }
            }
            else if (c == '\r') {
                boolean followedByLineFeed = i < text.length() - 1 && text.charAt(i + 1) == '\n';
                if (!followedByLineFeed && keepCarriageReturn) {
                    if (buffer == null) {
                        intactLength++;
                    }
                    else {
                        buffer.append(c);
                    }
                    continue;
                }
                if (buffer == null) {
                    buffer = new StringBuilder(text.length());
                    buffer.append(text, 0, intactLength);
                }
                buffer.append(newSeparator);
                if (followedByLineFeed) {
                    //noinspection AssignmentToForLoopParameter
                    i++;
                    shiftOffsets(offsetsToKeep, buffer.length(), 2, newSeparator.length());
                }
                else {
                    shiftOffsets(offsetsToKeep, buffer.length(), 1, newSeparator.length());
                }
            }
            else if (buffer == null) {
                intactLength++;
            }
            else {
                buffer.append(c);
            }
        }
        return buffer == null ? text : buffer;
    }

    private static void shiftOffsets(int @Nullable [] offsets, int changeOffset, int oldLength, int newLength) {
        if (offsets == null) {
            return;
        }
        int shift = newLength - oldLength;
        if (shift == 0) {
            return;
        }
        for (int i = 0; i < offsets.length; i++) {
            int offset = offsets[i];
            if (offset >= changeOffset + oldLength) {
                offsets[i] += shift;
            }
        }
    }

    @Contract(pure = true)
    public static List<String> splitHonorQuotes(String s, char separator) {
        List<String> result = new ArrayList<>();
        StringBuilder builder = new StringBuilder(s.length());
        char quote = 0;
        boolean isEscaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean isSeparator = c == separator;
            boolean isQuote = c == '"' || c == '\'';
            boolean isQuoted = quote != 0;
            boolean isEscape = c == '\\';

            if (!isQuoted && isSeparator) {
                if (builder.length() > 0) {
                    result.add(builder.toString());
                    builder.setLength(0);
                }
                continue;
            }

            if (!isEscaped && isQuote && (quote == 0 || quote == c)) {
                quote = isQuoted ? 0 : c;
            }

            isEscaped = isEscape && !isEscaped;

            builder.append(c);
        }
        if (builder.length() > 0) {
            result.add(builder.toString());
        }
        return result;
    }

    @Contract(pure = true)
    public static int countNewLines(CharSequence text) {
        return countChars(text, '\n');
    }

    @Contract(pure = true)
    public static int countChars(CharSequence text, char c) {
        return countChars(text, c, 0, false);
    }

    @Contract(pure = true)
    public static int countChars(CharSequence text, char c, int offset, boolean stopAtOtherChar) {
        return countChars(text, c, offset, text.length(), stopAtOtherChar);
    }

    @Contract(pure = true)
    public static int countChars(CharSequence text, char c, int start, int end, boolean stopAtOtherChar) {
        boolean forward = start <= end;
        start = forward ? Math.max(0, start) : Math.min(text.length(), start);
        end = forward ? Math.min(text.length(), end) : Math.max(0, end);
        int count = 0;
        for (int i = forward ? start : start - 1; forward == i < end; i += forward ? 1 : -1) {
            if (text.charAt(i) == c) {
                count++;
            }
            else if (stopAtOtherChar) {
                break;
            }
        }
        return count;
    }

    @Contract(pure = true)
    public static String pluralize(String base, int count) {
        if (count == 1) {
            return base;
        }
        return pluralize(base);
    }

    @Contract(mutates = "param1")
    public static void repeatSymbol(Appendable buffer, char symbol, int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Negative count: " + times);
        }
        try {
            for (int i = 0; i < times; i++) {
                buffer.append(symbol);
            }
        }
        catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Contract(pure = true)
    public static int indexOfAny(String s, String chars) {
        return indexOfAny(s, chars, 0, s.length());
    }

    @Contract(pure = true)
    public static int indexOfAny(CharSequence s, String chars) {
        return indexOfAny(s, chars, 0, s.length());
    }

    @Contract(pure = true)
    public static int indexOfAny(String s, String chars, int start, int end) {
        return indexOfAny((CharSequence) s, chars, start, end);
    }

    @Contract(pure = true)
    public static int indexOfAny(CharSequence s, String chars, int start, int end) {
        if (chars.isEmpty()) {
            return -1;
        }

        end = Math.min(end, s.length());
        for (int i = Math.max(start, 0); i < end; i++) {
            if (containsChar(chars, s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static boolean containsChar(String value, char ch) {
        return value.indexOf(ch) >= 0;
    }

    @Contract(pure = true)
    public static String repeatSymbol(char aChar, int count) {
        char[] buffer = new char[count];
        Arrays.fill(buffer, aChar);
        return new String(buffer);
    }

    @Contract(pure = true)
    public static String repeat(String s, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative count: " + count);
        }
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Contract(pure = true)
    public static boolean isDecimalDigit(char c) {
        return '0' <= c && c <= '9';
    }

    @Contract("null -> false")
    public static boolean isNotNegativeNumber(@Nullable CharSequence s) {
        if (s == null) {
            return false;
        }
        for (int i = 0, n = s.length(); i < n; i++) {
            if (!isDecimalDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public static int compare(@Nullable String s1, @Nullable String s2, boolean ignoreCase) {
        //noinspection StringEquality
        if (s1 == s2) {
            return 0;
        }
        if (s1 == null) {
            return -1;
        }
        if (s2 == null) {
            return 1;
        }
        return ignoreCase ? s1.compareToIgnoreCase(s2) : s1.compareTo(s2);
    }

    @Contract(pure = true)
    public static int compare(char c1, char c2, boolean ignoreCase) {
        // duplicating String.equalsIgnoreCase logic
        int d = c1 - c2;
        if (d == 0 || !ignoreCase) {
            return d;
        }
        // If characters don't match but case may be ignored,
        // try converting both characters to uppercase.
        // If the results match, then the comparison scan should
        // continue.
        char u1 = toUpperCase(c1);
        char u2 = toUpperCase(c2);
        d = u1 - u2;
        if (d != 0) {
            // Unfortunately, conversion to uppercase does not work properly
            // for the Georgian alphabet, which has strange rules about case
            // conversion.  So we need to make one last check before
            // exiting.
            d = toLowerCase(u1) - toLowerCase(u2);
        }
        return d;
    }

    @Contract(pure = true)
    public static int compare(@Nullable CharSequence s1, @Nullable CharSequence s2, boolean ignoreCase) {
        if (s1 == s2) {
            return 0;
        }
        if (s1 == null) {
            return -1;
        }
        if (s2 == null) {
            return 1;
        }

        int length1 = s1.length();
        int length2 = s2.length();
        int i = 0;
        for (; i < length1 && i < length2; i++) {
            int diff = compare(s1.charAt(i), s2.charAt(i), ignoreCase);
            if (diff != 0) {
                return diff;
            }
        }
        return length1 - length2;
    }

    /**
     * Implementation of "Sorting for Humans: Natural Sort Order":
     * http://www.codinghorror.com/blog/2007/12/sorting-for-humans-natural-sort-order.html
     */
    @Contract(pure = true)
    public static int naturalCompare(@Nullable String string1, @Nullable String string2) {
        return NaturalComparator.INSTANCE.compare(string1, string2);
    }

    @Contract(value = "null -> false", pure = true)
    public static boolean isNotEmpty(@Nullable String s) {
        return s != null && !s.isEmpty();
    }

    @Contract(pure = true)
    public static int length(@Nullable CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * @return {@code text} with some characters replaced with standard XML entities, e.g. '<' replaced with '{@code &lt;}'
     */
    @Contract(pure = true)
    @Deprecated
    @DeprecationInfo("Use XmlStringUtil#escapeText or XmlStringUtil#escapeAttr")
    public static String escapeXmlEntities(String text) {
        return replace(text, REPLACES_DISP, REPLACES_REFS);
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    @Nullable
    public static String escapeMnemonics(@Nullable String text) {
        if (text == null) {
            return null;
        }
        return replace(text, MN_CHARS, MN_QUOTED);
    }

    @Contract(pure = true)
    public static String first(String text, int maxLength, boolean appendEllipsis) {
        if (text.length() <= maxLength) {
            return text;
        }
        String cropped = text.substring(0, maxLength);
        return appendEllipsis ? cropped + "..." : cropped;
    }

    @Contract(pure = true)
    public static CharSequence first(CharSequence text, int maxLength, boolean appendEllipsis) {
        if (text.length() <= maxLength) {
            return text;
        }
        CharSequence cropped = text.subSequence(0, maxLength);
        return appendEllipsis ? cropped + "..." : cropped;
    }

    @Contract(pure = true)
    public static CharSequence last(CharSequence text, int maxLength, boolean prependEllipsis) {
        int length = text.length();
        if (length <= maxLength) {
            return text;
        }
        CharSequence cropped = text.subSequence(length - maxLength, length);
        return prependEllipsis ? "..." + cropped : cropped;
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    @Nullable
    public static String trim(@Nullable String s) {
        return s == null ? null : s.trim();
    }

    /**
     * Trim all characters not accepted by given filter
     *
     * @param s      e.g. "/n    my string "
     * @param filter e.g. {@link CharFilter#NOT_WHITESPACE_FILTER}
     * @return trimmed string e.g. "my string"
     */
    @Contract(pure = true)
    public static String trim(String s, CharFilter filter) {
        int start = 0;
        int end = s.length();

        for (; start < end; start++) {
            char ch = s.charAt(start);
            if (filter.accept(ch)) {
                break;
            }
        }

        for (; start < end; end--) {
            char ch = s.charAt(end - 1);
            if (filter.accept(ch)) {
                break;
            }
        }

        return s.substring(start, end);
    }

    @Contract(value = "null -> null", pure = true)
    @Nullable
    public static String trimToNull(@Nullable String s) {
        if (s == null) {
            return null;
        }

        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Contract(pure = true)
    public static String wrapWithDoubleQuote(String str) {
        return '\"' + str + "\"";
    }

    @Contract(pure = true)
    public static String trimEnd(String s, String suffix) {
        return trimEnd(s, suffix, false);
    }

    @Contract(pure = true)
    public static String trimEnd(String s, char suffix) {
        if (endsWithChar(s, suffix)) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    @Contract(pure = true)
    public static String trimEnd(String s, String suffix, boolean ignoreCase) {
        boolean endsWith = ignoreCase ? endsWithIgnoreCase(s, suffix) : s.endsWith(suffix);
        if (endsWith) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }

    @Contract(pure = true)
    public static boolean endsWithIgnoreCase(CharSequence text, CharSequence suffix) {
        int l1 = text.length();
        int l2 = suffix.length();
        if (l1 < l2) {
            return false;
        }

        for (int i = l1 - 1; i >= l1 - l2; i--) {
            if (!charsEqualIgnoreCase(text.charAt(i), suffix.charAt(i + l2 - l1))) {
                return false;
            }
        }

        return true;
    }

    @Contract(pure = true)
    public static boolean endsWith(CharSequence text, CharSequence suffix) {
        int l1 = text.length();
        int l2 = suffix.length();
        if (l1 < l2) {
            return false;
        }

        for (int i = l1 - 1; i >= l1 - l2; i--) {
            if (text.charAt(i) != suffix.charAt(i + l2 - l1)) {
                return false;
            }
        }

        return true;
    }

    @Contract(pure = true)
    public static Iterable<String> tokenize(String s, String separators) {
        consulo.util.lang.text.StringTokenizer tokenizer = new consulo.util.lang.text.StringTokenizer(s, separators);
        return () -> tokenizer;
    }

    @Contract(pure = true)
    public static Iterable<String> tokenize(StringTokenizer tokenizer) {
        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return tokenizer.hasMoreTokens();
            }

            @Override
            public String next() {
                return tokenizer.nextToken();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Contract(pure = true)
    public static String join(String @Nullable ... strings) {
        int length = strings == null ? 0 : strings.length;
        if (length == 0) {
            return "";
        }
        else if (length == 1) {
            assert strings != null;
            return String.valueOf(strings[0]);
        }

        return join(new StringBuilder(), strings).toString();
    }

    @Contract(mutates = "param1")
    public static StringBuilder join(StringBuilder result, String @Nullable ... strings) {
        if (strings != null) {
            for (String string : strings) {
                result.append(string);
            }
        }
        return result;
    }

    @Contract(pure = true)
    public static String join(String[] strings, String separator) {
        return join(strings, 0, strings.length, separator);
    }

    @Contract(mutates = "param3")
    public static StringBuilder join(String[] strings, String separator, StringBuilder result) {
        return join(strings, 0, strings.length, separator, result);
    }

    @Contract(pure = true)
    public static String join(String[] strings, int startIndex, int endIndex, String separator) {
        if (endIndex == startIndex) {
            return "";
        }
        else if (endIndex == startIndex + 1) {
            return String.valueOf(strings[startIndex]);
        }
        return join(strings, startIndex, endIndex, separator, new StringBuilder()).toString();
    }

    @Contract(mutates = "param5")
    public static StringBuilder join(String[] strings, int startIndex, int endIndex, String separator, StringBuilder result) {
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                result.append(separator);
            }
            result.append(strings[i]);
        }
        return result;
    }

    @Contract(pure = true)
    public static <T> String join(T[] items, Function<T, String> f, String separator) {
        int length = items.length;
        if (length == 0) {
            return "";
        }
        else if (length == 1) {
            return notNullize(f.apply(items[0]));
        }
        return join(items, f, separator, new StringBuilder()).toString();
    }

    @Contract(mutates = "param4")
    public static <T> StringBuilder join(
        T[] items,
        Function<T, String> f,
        String separator,
        StringBuilder result
    ) {
        return join(Arrays.asList(items), f, separator, result);
    }

    @Contract(pure = true)
    public static String join(Collection<String> strings, String separator) {
        if (strings.isEmpty()) {
            return "";
        }
        else if (strings.size() == 1) {
            String item = strings instanceof List<String> list ? list.get(0) : strings.iterator().next();
            return notNullize(item);
        }
        return join(strings, separator, new StringBuilder()).toString();
    }

    @Contract(mutates = "param3")
    public static StringBuilder join(Collection<String> strings, String separator, StringBuilder result) {
        boolean isFirst = true;
        for (String string : strings) {
            if (string == null) {
                continue;
            }

            if (isFirst) {
                isFirst = false;
            }
            else {
                result.append(separator);
            }
            result.append(string);
        }
        return result;
    }

    @Contract(pure = true)
    public static <T> String join(Collection<? extends T> items, Function<? super T, String> f, String separator) {
        if (items.isEmpty()) {
            return "";
        }
        else if (items.size() == 1) {
            T item = items instanceof List<? extends T> list ? list.get(0) : items.iterator().next();
            return notNullize(f.apply(item));
        }
        return join(items, f, separator, new StringBuilder()).toString();
    }

    @Contract(mutates = "param4")
    public static <T> StringBuilder join(
        Collection<? extends T> items,
        Function<? super T, String> f,
        String separator,
        StringBuilder result
    ) {
        return join((Iterable<? extends T>) items, f, separator, result);
    }

    @Contract(pure = true)
    public static String join(Iterable<?> items, String separator) {
        return join(items, separator, new StringBuilder()).toString();
    }

    @Contract(mutates = "param3")
    public static StringBuilder join(Iterable<?> items, String separator, StringBuilder result) {
        for (Object item : items) {
            result.append(item).append(separator);
        }
        if (result.length() > 0) {
            result.setLength(result.length() - separator.length());
        }
        return result;
    }

    @Contract(pure = true)
    public static <T> String join(Iterable<? extends T> items, Function<? super T, String> f, String separator) {
        return join(items, f, separator, new StringBuilder()).toString();
    }

    @Contract(mutates = "param4")
    public static <T> StringBuilder join(
        Iterable<? extends T> items,
        Function<? super T, String> f,
        String separator,
        StringBuilder result
    ) {
        boolean isFirst = true;
        for (T item : items) {
            String string = f.apply(item);
            if (isEmpty(string)) {
                continue;
            }

            if (isFirst) {
                isFirst = false;
            }
            else {
                result.append(separator);
            }
            result.append(string);
        }
        return result;
    }

    /**
     * Equivalent to string.startsWith(prefixes[0] + prefixes[1] + ...) but avoids creating an object for concatenation.
     */
    @Contract(pure = true)
    public static boolean startsWithConcatenation(String string, String... prefixes) {
        int offset = 0;
        for (String prefix : prefixes) {
            int prefixLen = prefix.length();
            if (!string.regionMatches(offset, prefix, 0, prefixLen)) {
                return false;
            }
            offset += prefixLen;
        }
        return true;
    }

    @Contract(pure = true)
    public static boolean isHexDigit(char c) {
        return '0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F';
    }

    @Contract(pure = true)
    public static boolean startsWithChar(@Nullable CharSequence s, char prefix) {
        return s != null && s.length() != 0 && s.charAt(0) == prefix;
    }

    @Contract(pure = true)
    public static boolean startsWithWhitespace(String text) {
        return !text.isEmpty() && Character.isWhitespace(text.charAt(0));
    }

    public static int stringHashCode(CharSequence chars) {
        if (chars instanceof String || chars instanceof CharSequenceWithStringHash) {
            // we know for sure these classes have conformant (and maybe faster) hashCode()
            return chars.hashCode();
        }

        return stringHashCode(chars, 0, chars.length());
    }

    @Contract(pure = true)
    public static int stringHashCode(CharSequence chars, int from, int to) {
        return stringHashCode(chars, from, to, 0);
    }

    @Contract(pure = true)
    public static int stringHashCode(CharSequence chars, int from, int to, int prefixHash) {
        int h = prefixHash;
        for (int off = from; off < to; off++) {
            h = 31 * h + chars.charAt(off);
        }
        return h;
    }

    @Contract(pure = true)
    public static int stringHashCode(char[] chars) {
        return stringHashCode(chars, 0, chars.length);
    }

    @Contract(pure = true)
    public static int stringHashCode(char[] chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            h = 31 * h + chars[off];
        }
        return h;
    }

    @Contract(pure = true)
    public static int stringHashCodeInsensitive(CharSequence chars) {
        return stringHashCodeInsensitive(chars, 0, chars.length());
    }

    @Contract(pure = true)
    public static int stringHashCodeInsensitive(CharSequence chars, int from, int to) {
        return stringHashCodeInsensitive(chars, from, to, 0);
    }

    @Contract(pure = true)
    public static int stringHashCodeInsensitive(CharSequence chars, int from, int to, int prefixHash) {
        int h = prefixHash;
        for (int off = from; off < to; off++) {
            h = 31 * h + toLowerCase(chars.charAt(off));
        }
        return h;
    }

    @Contract(pure = true)
    public static int stringHashCodeInsensitive(char[] chars) {
        return stringHashCodeInsensitive(chars, 0, chars.length);
    }

    @Contract(pure = true)
    public static int stringHashCodeInsensitive(char[] chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            h = 31 * h + toLowerCase(chars[off]);
        }
        return h;
    }

    @Contract(pure = true)
    public static int stringHashCodeIgnoreWhitespaces(CharSequence chars) {
        return stringHashCodeIgnoreWhitespaces(chars, 0, chars.length());
    }

    @Contract(pure = true)
    public static int stringHashCodeIgnoreWhitespaces(CharSequence chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            char c = chars.charAt(off);
            if (!isWhiteSpace(c)) {
                h = 31 * h + c;
            }
        }
        return h;
    }

    @Contract(pure = true)
    public static int stringHashCodeIgnoreWhitespaces(char[] chars) {
        return stringHashCodeIgnoreWhitespaces(chars, 0, chars.length);
    }

    @Contract(pure = true)
    public static int stringHashCodeIgnoreWhitespaces(char[] chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            char c = chars[off];
            if (!isWhiteSpace(c)) {
                h = 31 * h + c;
            }
        }
        return h;
    }

    /**
     * Allows to answer if given symbol is white space, tabulation or line feed.
     *
     * @param c symbol to check
     * @return <code>true</code> if given symbol is white space, tabulation or line feed; <code>false</code> otherwise
     */
    @Contract(pure = true)
    public static boolean isWhiteSpace(char c) {
        return c == '\t' || c == '\r' || c == '\n' || c == ' ';
    }

    @Contract("null,!null,_ -> false; !null,null,_ -> false; null,null,_ -> true")
    public static boolean equal(@Nullable CharSequence s1, @Nullable CharSequence s2, boolean caseSensitive) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }

        int n = s1.length();
        if (s2.length() != n) {
            return false;
        }

        if (caseSensitive) {
            for (int i = 0; i < n; i++) {
                if (s1.charAt(i) != s2.charAt(i)) {
                    return false;
                }
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                if (!charsEqualIgnoreCase(s1.charAt(i), s2.charAt(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Contract(pure = true)
    public static boolean equals(@Nullable CharSequence s1, @Nullable CharSequence s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }

        int n = s1.length();
        if (n != s2.length()) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public static boolean charsEqualIgnoreCase(char a, char b) {
        return a == b || toUpperCase(a) == toUpperCase(b) || toLowerCase(a) == toLowerCase(b);
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    @Nullable
    public static String toUpperCase(@Nullable String s) {
        return s == null ? null : s.toUpperCase(Locale.US);
    }

    @Contract(pure = true)
    public static CharSequence toUpperCase(CharSequence s) {
        StringBuilder answer = null;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char upcased = toUpperCase(c);
            if (answer == null && upcased != c) {
                answer = new StringBuilder(s.length());
                answer.append(s, 0, i);
            }

            if (answer != null) {
                answer.append(upcased);
            }
        }

        return answer == null ? s : answer;
    }

    @Contract(pure = true)
    public static char toUpperCase(char a) {
        if (a < 'a') {
            return a;
        }
        if (a <= 'z') {
            return (char) (a + ('A' - 'a'));
        }
        return Character.toUpperCase(a);
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    @Nullable
    public static String toLowerCase(@Nullable String str) {
        return str == null ? null : str.toLowerCase(Locale.US);
    }

    @Contract(pure = true)
    public static char toLowerCase(char a) {
        if (a < 'A' || a >= 'a' && a <= 'z') {
            return a;
        }

        if (a <= 'Z') {
            return (char) (a + ('a' - 'A'));
        }

        return Character.toLowerCase(a);
    }

    @Contract(pure = true)
    @Nullable
    public static String getPropertyName(String methodName) {
        if (methodName.startsWith("get")) {
            return decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is")) {
            return decapitalize(methodName.substring(2));
        }
        if (methodName.startsWith("set")) {
            return decapitalize(methodName.substring(3));
        }
        return null;
    }

    @Contract(pure = true)
    public static boolean isJavaIdentifierStart(char c) {
        return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z' || Character.isJavaIdentifierStart(c);
    }

    @Contract(pure = true)
    public static boolean isJavaIdentifierPart(char c) {
        return '0' <= c && c <= '9' || 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z' || Character.isJavaIdentifierPart(c);
    }

    @Contract(pure = true)
    private static boolean isJavaIdentifierStart(int cp) {
        return 'a' <= cp && cp <= 'z' || 'A' <= cp && cp <= 'Z' || Character.isJavaIdentifierStart(cp);
    }

    @Contract(pure = true)
    private static boolean isJavaIdentifierPart(int cp) {
        return cp >= '0' && cp <= '9' || cp >= 'a' && cp <= 'z' || cp >= 'A' && cp <= 'Z' || Character.isJavaIdentifierPart(cp);
    }

    /**
     * @return true iff the string is a valid java identifier (according to JLS 3.8)
     */
    @Contract(pure = true)
    public static boolean isJavaIdentifier(String text) {
        int len = text.length();
        if (len == 0) {
            return false;
        }
        int point = text.codePointAt(0);
        if (!isJavaIdentifierStart(point)) {
            return false;
        }
        int i = Character.charCount(point);

        while (i < len) {
            point = text.codePointAt(i);
            if (!isJavaIdentifierPart(point)) {
                return false;
            }
            i += Character.charCount(point);
        }
        return true;
    }

    @Contract(pure = true)
    public static boolean endsWithChar(@Nullable CharSequence s, char suffix) {
        return s != null && s.length() != 0 && s.charAt(s.length() - 1) == suffix;
    }

    @Contract(pure = true)
    public static String trimStart(String s, String prefix) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    @Contract(value = "null -> true", pure = true)
    public static boolean isEmpty(@Nullable String s) {
        return s == null || s.isEmpty();
    }

    @Contract(value = "null -> true", pure = true)
    public static boolean isEmpty(@Nullable CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    @Contract(pure = true)
    public static String notNullize(@Nullable String s) {
        return notNullize(s, "");
    }

    @Contract(pure = true)
    public static String notNullize(@Nullable String s, String defaultValue) {
        return s == null ? defaultValue : s;
    }

    @Contract(pure = true)
    public static String notNullizeIfEmpty(@Nullable String s, String defaultValue) {
        return isEmpty(s) ? defaultValue : s;
    }

    @Nullable
    @Contract(pure = true)
    public static String nullize(@Nullable String s) {
        return nullize(s, false);
    }

    @Nullable
    @Contract(pure = true)
    public static String nullize(@Nullable String s, boolean nullizeSpaces) {
        if (nullizeSpaces) {
            return isEmptyOrSpaces(s) ? null : s;
        }
        else {
            return isEmpty(s) ? null : s;
        }
    }

    @Contract(value = "null -> true", pure = true)
    // we need to keep this method to preserve backward compatibility
    public static boolean isEmptyOrSpaces(@Nullable String s) {
        return isEmptyOrSpaces(((CharSequence) s));
    }

    @Contract(value = "null -> true", pure = true)
    public static boolean isEmptyOrSpaces(@Nullable CharSequence s) {
        if (isEmpty(s)) {
            return true;
        }
        for (int i = 0, n = s.length(); i < n; i++) {
            if (s.charAt(i) > ' ') {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public static int compareVersionNumbers(@Nullable String v1, @Nullable String v2) {
        // todo duplicates consulo.ide.impl.idea.util.text.VersionComparatorUtil.compare
        // todo please refactor next time you make changes here
        if (v1 == null && v2 == null) {
            return 0;
        }
        if (v1 == null) {
            return -1;
        }
        if (v2 == null) {
            return 1;
        }

        String[] part1 = v1.split("[._\\-]");
        String[] part2 = v2.split("[._\\-]");

        int idx = 0;
        for (; idx < part1.length && idx < part2.length; idx++) {
            String p1 = part1[idx];
            String p2 = part2[idx];

            int cmp;
            if (p1.matches("\\d+") && p2.matches("\\d+")) {
                cmp = Integer.valueOf(p1).compareTo(Integer.valueOf(p2));
            }
            else {
                cmp = part1[idx].compareTo(part2[idx]);
            }
            if (cmp != 0) {
                return cmp;
            }
        }

        if (part1.length != part2.length) {
            boolean left = part1.length > idx;
            String[] parts = left ? part1 : part2;

            for (; idx < parts.length; idx++) {
                String p = parts[idx];
                int cmp;
                if (p.matches("\\d+")) {
                    cmp = Integer.valueOf(p).compareTo(0);
                }
                else {
                    cmp = 1;
                }
                if (cmp != 0) {
                    return left ? cmp : -cmp;
                }
            }
        }
        return 0;
    }

    @Contract(pure = true)
    public static int getOccurrenceCount(String text, char c) {
        int res = 0;
        int i = 0;
        while (i < text.length()) {
            i = text.indexOf(c, i);
            if (i >= 0) {
                res++;
                i++;
            }
            else {
                break;
            }
        }
        return res;
    }

    @Contract(pure = true)
    public static int getOccurrenceCount(String text, String s) {
        int res = 0;
        for (int i = 0, n = text.length(); i < n; i++, res++) {
            i = text.indexOf(s, i);
            if (i < 0) {
                break;
            }
        }
        return res;
    }

    @Contract(pure = true)
    public static String fixVariableNameDerivedFromPropertyName(String name) {
        if (isEmptyOrSpaces(name)) {
            return name;
        }
        char c = name.charAt(0);
        if (isVowel(c)) {
            return "an" + Character.toUpperCase(c) + name.substring(1);
        }
        return "a" + Character.toUpperCase(c) + name.substring(1);
    }

    @Contract(pure = true)
    public static String sanitizeJavaIdentifier(String name) {
        int n = name.length();
        StringBuilder result = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            char ch = name.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) {
                if (result.isEmpty() && !Character.isJavaIdentifierStart(ch)) {
                    result.append("_");
                }
                result.append(ch);
            }
        }

        return result.toString();
    }

    @Contract(pure = true)
    public static void assertValidSeparators(CharSequence s) {
        char[] chars = CharArrayUtil.fromSequenceWithoutCopying(s);
        int slashRIndex = -1;

        if (chars != null) {
            for (int i = 0, len = s.length(); i < len; ++i) {
                if (chars[i] == '\r') {
                    slashRIndex = i;
                    break;
                }
            }
        }
        else {
            for (int i = 0, len = s.length(); i < len; i++) {
                if (s.charAt(i) == '\r') {
                    slashRIndex = i;
                    break;
                }
            }
        }

        if (slashRIndex != -1) {
            String context =
                String.valueOf(last(s.subSequence(0, slashRIndex), 10, true)) + first(s.subSequence(slashRIndex, s.length()), 10, true);
            LOG.error("Wrong line separators: " + StringEscapeUtil.quote(context, '"') + " at offset " + slashRIndex);
        }
    }

    @Contract(pure = true)
    public static int indexOf(CharSequence s, char c, int start, int end, boolean caseSensitive) {
        end = Math.min(end, s.length());
        for (int i = Math.max(start, 0); i < end; i++) {
            if (charsMatch(s.charAt(i), c, !caseSensitive)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static int indexOf(char[] s, char c, int start, int end, boolean caseSensitive) {
        end = Math.min(end, s.length);
        for (int i = Math.max(start, 0); i < end; i++) {
            boolean ignoreCase = !caseSensitive;
            if (charsMatch(s[i], c, ignoreCase)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static int indexOf(CharSequence s, char c) {
        return indexOf(s, c, 0, s.length());
    }

    @Contract(pure = true)
    public static int indexOf(CharSequence s, char c, int start) {
        return indexOf(s, c, start, s.length());
    }

    @Contract(pure = true)
    public static int indexOf(CharSequence s, char c, int start, int end) {
        end = Math.min(end, s.length());
        for (int i = Math.max(start, 0); i < end; i++) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static String escapeChar(String str, char character) {
        return escapeChars(str, character);
    }

    @Contract(pure = true)
    public static String escapeChars(String str, char... character) {
        StringBuilder buf = new StringBuilder(str);
        for (char c : character) {
            escapeChar(buf, c);
        }
        return buf.toString();
    }

    public static void escapeChar(StringBuilder buf, char character) {
        int idx = 0;
        while ((idx = indexOf(buf, character, idx)) >= 0) {
            buf.insert(idx, "\\");
            idx += 2;
        }
    }

    @Contract(pure = true)
    public static String trimExtension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? name : name.substring(0, index);
    }

    @Contract(value = "null -> null; !null->!null", pure = true)
    @Nullable
    public static String internEmptyString(@Nullable String s) {
        return s == null ? null : s.isEmpty() ? "" : s;
    }

    @Contract(pure = true)
    public static String trimLeading(String string) {
        return trimLeading((CharSequence) string).toString();
    }

    @Contract(pure = true)
    public static CharSequence trimLeading(CharSequence string) {
        int index = 0;
        while (index < string.length() && Character.isWhitespace(string.charAt(index))) index++;
        return string.subSequence(index, string.length());
    }

    @Contract(pure = true)
    public static String trimLeading(String string, char symbol) {
        int index = 0;
        while (index < string.length() && string.charAt(index) == symbol) index++;
        return string.substring(index);
    }

    @Contract(pure = true)
    public static String trimTrailing(String string) {
        return trimTrailing((CharSequence) string).toString();
    }

    @Contract(pure = true)
    public static CharSequence trimTrailing(CharSequence string) {
        int index = string.length() - 1;
        while (index >= 0 && Character.isWhitespace(string.charAt(index))) index--;
        return string.subSequence(0, index + 1);
    }

    @Contract(pure = true)
    public static String trimTrailing(String string, char symbol) {
        int index = string.length() - 1;
        while (index >= 0 && string.charAt(index) == symbol) index--;
        return string.substring(0, index + 1);
    }

    @Contract(pure = true)
    public static CharSequence trimTrailing(CharSequence string, char symbol) {
        int index = string.length() - 1;
        while (index >= 0 && string.charAt(index) == symbol) index--;
        return string.subSequence(0, index + 1);
    }

    @Contract(pure = true)
    public static String getShortName(Class aClass) {
        return getShortName(aClass.getName());
    }

    @Contract(pure = true)
    public static String getShortName(String fqName) {
        return getShortName(fqName, '.');
    }

    @Contract(pure = true)
    public static String getShortName(String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(lastPointIdx + 1);
        }
        return fqName;
    }

    /**
     * Allows to retrieve index of last occurrence of the given symbols at <code>[start; end)</code> sub-sequence of the given text.
     *
     * @param s     target text
     * @param c     target symbol which last occurrence we want to check
     * @param start start offset of the target text (inclusive)
     * @param end   end offset of the target text (exclusive)
     * @return index of the last occurrence of the given symbol at the target sub-sequence of the given text if any;
     * <code>-1</code> otherwise
     */
    @Contract(pure = true)
    public static int lastIndexOf(CharSequence s, char c, int start, int end) {
        start = Math.max(start, 0);
        for (int i = Math.min(end, s.length()) - 1; i >= start; i--) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }


    @Contract(pure = true)
    public static List<String> split(String s, String separator) {
        return split(s, separator, true);
    }

    @Contract(pure = true)
    public static List<CharSequence> split(CharSequence s, CharSequence separator) {
        return split(s, separator, true, true);
    }

    @Contract(pure = true)
    public static List<String> split(String s, String separator, boolean excludeSeparator) {
        return split(s, separator, excludeSeparator, true);
    }

    @Contract(pure = true)
    public static List<String> split(String s, String separator, boolean excludeSeparator, boolean excludeEmptyStrings) {
        return (List) split((CharSequence) s, separator, excludeSeparator, excludeEmptyStrings);
    }

    @Contract(pure = true)
    public static List<CharSequence> split(
        CharSequence s,
        CharSequence separator,
        boolean excludeSeparator,
        boolean excludeEmptyStrings
    ) {
        if (separator.length() == 0) {
            return List.of();
        }
        List<CharSequence> result = new ArrayList<>();
        int pos = 0;
        while (true) {
            int index = indexOf(s, separator, pos);
            if (index == -1) {
                break;
            }
            int nextPos = index + separator.length();
            CharSequence token = s.subSequence(pos, excludeSeparator ? index : nextPos);
            if (token.length() != 0 || !excludeEmptyStrings) {
                result.add(token);
            }
            pos = nextPos;
        }
        if (pos < s.length() || !excludeEmptyStrings && pos == s.length()) {
            result.add(s.subSequence(pos, s.length()));
        }
        return result;
    }

    @Contract(pure = true)
    public static int indexOf(CharSequence sequence, CharSequence infix, int start) {
        for (int i = start; i <= sequence.length() - infix.length(); i++) {
            if (startsWith(sequence, i, infix)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static boolean startsWith(CharSequence text, int startIndex, CharSequence prefix) {
        int tl = text.length();
        if (startIndex < 0 || startIndex > tl) {
            throw new IllegalArgumentException("Index is out of bounds: " + startIndex + ", length: " + tl);
        }
        int l1 = tl - startIndex;
        int l2 = prefix.length();
        if (l1 < l2) {
            return false;
        }

        for (int i = 0; i < l2; i++) {
            if (text.charAt(i + startIndex) != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return a lightweight CharSequence which results from replacing {@code [start, end)} range in the {@code charSeq} with {@code replacement}.
     * Works in O(1), but retains references to the passed char sequences, so please use something else if you want them to be garbage-collected.
     */
    public static MergingCharSequence replaceSubSequence(
        CharSequence charSeq,
        int start,
        int end,
        CharSequence replacement
    ) {
        return new MergingCharSequence(
            new MergingCharSequence(new CharSequenceSubSequence(charSeq, 0, start), replacement),
            new CharSequenceSubSequence(charSeq, end, charSeq.length())
        );
    }

    @Contract(pure = true)
    @SuppressWarnings("SimplifiableIfStatement")
    public static boolean isEscapedBackslash(CharSequence text, int startOffset, int backslashOffset) {
        if (text.charAt(backslashOffset) != '\\') {
            return true;
        }
        boolean escaped = false;
        for (int i = startOffset; i < backslashOffset; i++) {
            if (text.charAt(i) == '\\') {
                escaped = !escaped;
            }
            else {
                escaped = false;
            }
        }
        return escaped;
    }

    @Contract(pure = true)
    @SuppressWarnings("SimplifiableIfStatement")
    public static boolean isEscapedBackslash(char[] chars, int startOffset, int backslashOffset) {
        if (chars[backslashOffset] != '\\') {
            return true;
        }
        boolean escaped = false;
        for (int i = startOffset; i < backslashOffset; i++) {
            if (chars[i] == '\\') {
                escaped = !escaped;
            }
            else {
                escaped = false;
            }
        }
        return escaped;
    }

    // TODO: process all escapes
    @Contract(value = "null -> null; !null -> !null", pure = true)
    @Nullable
    public static String unescapeXml(@Nullable String text) {
        if (text == null) {
            return null;
        }
        return replace(text, REPLACES_REFS, REPLACES_DISP);
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    @Deprecated
    @DeprecationInfo("Use XmlStringUtil#escapeText or XmlStringUtil#escapeAttr")
    @Nullable
    public static String escapeXml(@Nullable String text) {
        if (text == null) {
            return null;
        }
        return replace(text, REPLACES_DISP, REPLACES_REFS);
    }

    @Contract(pure = true)
    public static int getLineBreakCount(CharSequence text) {
        int count = 0;
        for (int i = 0, n = text.length(); i < n; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                count++;
            }
            else if (c == '\r') {
                count++;
                if (i + 1 < n && text.charAt(i + 1) == '\n') {
                    i++;
                }
            }
        }
        return count;
    }

    @Contract(pure = true)
    public static String commonSuffix(String s1, String s2) {
        return s1.substring(s1.length() - commonSuffixLength(s1, s2));
    }

    @Contract(pure = true)
    public static int commonSuffixLength(CharSequence s1, CharSequence s2) {
        int i, l1 = s1.length(), l2 = s2.length();
        int minLength = Math.min(l1, l2);
        for (i = 0; i < minLength; i++) {
            if (s1.charAt(l1 - i - 1) != s2.charAt(l2 - i - 1)) {
                break;
            }
        }
        return i;
    }

    @Contract(pure = true)
    public static boolean startsWith(CharSequence text, CharSequence prefix) {
        int l1 = text.length();
        int l2 = prefix.length();
        if (l1 < l2) {
            return false;
        }

        for (int i = 0; i < l2; i++) {
            if (text.charAt(i) != prefix.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    @Contract(pure = true)
    public static String commonPrefix(String s1, String s2) {
        return s1.substring(0, commonPrefixLength(s1, s2));
    }

    @Contract(pure = true)
    public static String formatDuration(long duration) {
        return formatDuration(duration, " ");
    }

    @Contract(pure = true)
    public static String formatDuration(long duration, String spaceBeforeUnits) {
        return formatValue(
            duration,
            " ",
            new String[]{"ms", "s", "m", "h", "d"},
            new long[]{1000, 60, 60, 24},
            spaceBeforeUnits
        );
    }

    private static String formatValue(long value, String partSeparator, String[] units, long[] multipliers, String spaceBeforeUnits) {
        DecimalFormat decimalFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        StringBuilder sb = new StringBuilder();
        long count = value;
        long remainder = 0;
        int i = 0;
        for (; i < units.length; i++) {
            long multiplier = i < multipliers.length ? multipliers[i] : -1;
            if (multiplier == -1 || count < multiplier) {
                break;
            }
            remainder = count % multiplier;
            count /= multiplier;
            if (partSeparator != null && (remainder != 0 || sb.length() > 0)) {
                sb.insert(0, units[i]);
                if (spaceBeforeUnits != null) {
                    sb.insert(0, spaceBeforeUnits);
                }
                sb.insert(0, remainder).insert(0, partSeparator);
            }
        }
        if (partSeparator != null || remainder == 0) {
            sb.insert(0, units[i]);
            if (spaceBeforeUnits != null) {
                sb.insert(0, spaceBeforeUnits);
            }
            sb.insert(0, count);
        }
        else if (remainder > 0) {
            sb.append(decimalFormat.format(count + (double) remainder / multipliers[i - 1]));
            if (spaceBeforeUnits != null) {
                sb.append(spaceBeforeUnits);
            }
            sb.append(units[i]);
        }
        return sb.toString();
    }

    @Contract(pure = true)
    public static int commonPrefixLength(CharSequence s1, CharSequence s2) {
        int i;
        int minLength = Math.min(s1.length(), s2.length());
        for (i = 0; i < minLength; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                break;
            }
        }
        return i;
    }

    /**
     * Consider using {@link StringUtil#unquoteString(String)} instead.
     * Note: this method has an odd behavior:
     * Quotes are removed even if leading and trailing quotes are different or
     * if there is only one quote (leading or trailing).
     */
    @Contract(pure = true)
    public static String stripQuotesAroundValue(String text) {
        int len = text.length();
        if (len > 0) {
            int from = isQuoteAt(text, 0) ? 1 : 0;
            int to = len > 1 && isQuoteAt(text, len - 1) ? len - 1 : len;
            if (from > 0 || to < len) {
                return text.substring(from, to);
            }
        }
        return text;
    }

    @Contract(pure = true)
    public static String[] filterEmptyStrings(String[] strings) {
        int emptyCount = 0;
        for (String string : strings) {
            if (string == null || string.isEmpty()) {
                emptyCount++;
            }
        }
        if (emptyCount == 0) {
            return strings;
        }

        String[] result = new String[strings.length - emptyCount];
        int count = 0;
        for (String string : strings) {
            if (string == null || string.isEmpty()) {
                continue;
            }
            result[count++] = string;
        }

        return result;
    }

    @Contract(pure = true)
    public static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r';
    }

    @Contract(pure = true)
    public static String escapeLineBreak(String text) {
        int n = text.length();
        StringBuilder buffer = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n' -> buffer.append("\\n");
                case '\r' -> buffer.append("\\r");
                default -> buffer.append(c);
            }
        }
        return buffer.toString();
    }

    @Contract(pure = true)
    public static boolean endsWithLineBreak(CharSequence text) {
        int len = text.length();
        return len > 0 && isLineBreak(text.charAt(len - 1));
    }

    @Contract(pure = true)
    public static int lineColToOffset(CharSequence text, int line, int col) {
        int curLine = 0, offset = 0, length = text.length();
        while (line != curLine) {
            if (offset == length) {
                return -1;
            }
            char c = text.charAt(offset);
            if (c == '\n') {
                curLine++;
            }
            else if (c == '\r') {
                curLine++;
                if (offset + 1 < length && text.charAt(offset + 1) == '\n') {
                    offset++;
                }
            }
            offset++;
        }
        return offset + col;
    }

    @Contract(pure = true)
    public static int offsetToLineNumber(CharSequence text, int offset) {
        int curLine = 0, curOffset = 0, length = text.length();
        while (curOffset < offset) {
            if (curOffset == length) {
                return -1;
            }
            char c = text.charAt(curOffset);
            if (c == '\n') {
                curLine++;
            }
            else if (c == '\r') {
                curLine++;
                if (curOffset + 1 < length && text.charAt(curOffset + 1) == '\n') {
                    curOffset++;
                }
            }
            curOffset++;
        }
        return curLine;
    }

    /**
     * Formats the specified file size as a string.
     *
     * @param fileSize the size to format.
     * @return the size formatted as a string.
     * @since 5.0.1
     */
    @Contract(pure = true)
    public static String formatFileSize(long fileSize) {
        return formatFileSize(fileSize, " ", -1);
    }

    /**
     * Formats the specified file size as a string.
     *
     * @param fileSize      the size to format.
     * @param unitSeparator space to be used between counts and measurement units
     * @return the size formatted as a string.
     * @since 5.0.1
     */
    @Contract(pure = true)
    public static String formatFileSize(long fileSize, String unitSeparator) {
        return formatFileSize(fileSize, unitSeparator, -1);
    }

    /**
     * Formats the specified file size as a string.
     *
     * @param fileSize      the size to format.
     * @param unitSeparator space to be used between counts and measurement units
     * @param rank          preferred rank. 0 - bytes, 1 - kilobytes, ..., 6 - exabytes. If less than 0 then picked automatically
     * @return the size formatted as a string.
     * @since 5.0.1
     */
    @Contract(pure = true)
    public static String formatFileSize(long fileSize, String unitSeparator, int rank) {
        return formatFileSize(fileSize, unitSeparator, rank, false);
    }

    /**
     * @param fileSize               - size of the file in bytes
     * @param unitSeparator          - separator inserted between value and unit
     * @param rank                   - preferred rank. 0 - bytes, 1 - kilobytes, ..., 6 - exabytes. If less than 0 then picked automatically
     * @param fixedFractionPrecision - keep the fraction precision. if true, a number like 5.50 will be kept as it is, otherwise it will be
     *                               rounded to 5.5
     * @return string with formatted file size
     */
    @Contract(pure = true)
    public static String formatFileSize(long fileSize, String unitSeparator, int rank, boolean fixedFractionPrecision) {
        if (fileSize < 0) {
            throw new IllegalArgumentException("Invalid value: " + fileSize);
        }
        if (fileSize == 0) {
            return '0' + unitSeparator + 'B';
        }
        if (rank < 0) {
            rank = rankForFileSize(fileSize);
        }
        double value = fileSize / Math.pow(1000, rank);
        String[] units = {"B", "kB", "MB", "GB", "TB", "PB", "EB"};
        DecimalFormat decimalFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        if (fixedFractionPrecision) {
            decimalFormat.setMinimumFractionDigits(2);
        }
        return decimalFormat.format(value) + unitSeparator + units[rank];
    }

    @Contract(pure = true)
    public static int rankForFileSize(long fileSize) {
        if (fileSize < 0) {
            throw new IllegalArgumentException("Invalid value: " + fileSize);
        }
        return (int) ((Math.log10(fileSize) + 0.0000021714778384307465) / 3);  // (3 - Math.log10(999.995))
    }

    /**
     * Find position of the first character accepted by given filter.
     *
     * @param s      the string to search
     * @param filter search filter
     * @return position of the first character accepted or -1 if not found
     */
    @Contract(pure = true)
    public static int findFirst(CharSequence s, CharFilter filter) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (filter.accept(ch)) {
                return i;
            }
        }
        return -1;
    }

    private static final String[] PREPOSITIONS = {
        "a", "an", "and", "as", "at", "but", "by", "down",
        "for", "from", "if", "in", "into", "not", "of", "on",
        "onto", "or", "out", "over", "per", "nor", "the", "to",
        "up", "upon", "via", "with"
    };

    @Contract(pure = true)
    public static boolean isPreposition(String s, int firstChar, int lastChar) {
        return isPreposition(s, firstChar, lastChar, PREPOSITIONS);
    }

    @Contract(pure = true)
    public static boolean isPreposition(String s, int firstChar, int lastChar, String[] prepositions) {
        for (String preposition : prepositions) {
            boolean found = false;
            if (lastChar - firstChar + 1 == preposition.length()) {
                found = true;
                for (int j = 0; j < preposition.length(); j++) {
                    if (!(toLowerCase(s.charAt(firstChar + j)) == preposition.charAt(j))) {
                        found = false;
                    }
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static Function<String, String> escaper(boolean escapeSlash, @Nullable String additionalChars) {
        return (String dom) -> {
            StringBuilder builder = new StringBuilder(dom.length());
            escapeStringCharacters(dom.length(), dom, additionalChars, escapeSlash, builder);
            return builder.toString();
        };
    }

    @Contract(pure = true)
    public static String wordsToBeginFromUpperCase(String s) {
        return fixCapitalization(s, PREPOSITIONS, true);
    }

    @Contract(pure = true)
    public static String wordsToBeginFromLowerCase(String s) {
        return fixCapitalization(s, PREPOSITIONS, false);
    }

    @Contract(pure = true)
    public static String toTitleCase(String s) {
        return fixCapitalization(s, new String[0], true);
    }

    private static String fixCapitalization(String s, String[] prepositions, boolean title) {
        StringBuilder buffer = null;
        for (int i = 0; i < s.length(); i++) {
            char prevChar = i == 0 ? ' ' : s.charAt(i - 1);
            char currChar = s.charAt(i);
            if (!Character.isLetterOrDigit(prevChar) && prevChar != '\'') {
                if (Character.isLetterOrDigit(currChar)) {
                    if (title || Character.isUpperCase(currChar)) {
                        int j = i;
                        for (; j < s.length(); j++) {
                            if (!Character.isLetterOrDigit(s.charAt(j))) {
                                break;
                            }
                        }
                        if (!isPreposition(s, i, j - 1, prepositions)) {
                            if (buffer == null) {
                                buffer = new StringBuilder(s);
                            }
                            buffer.setCharAt(i, title ? toUpperCase(currChar) : toLowerCase(currChar));
                        }
                    }
                }
            }
        }
        if (buffer == null) {
            return s;
        }
        else {
            return buffer.toString();
        }
    }

    @Contract(pure = true)
    @Nullable
    public static String substringAfter(String text, String subString) {
        int i = text.indexOf(subString);
        if (i == -1) {
            return null;
        }
        return text.substring(i + subString.length());
    }

    @Contract(pure = true)
    @Nullable
    public static String substringAfterLast(String text, String subString) {
        int i = text.lastIndexOf(subString);
        if (i == -1) {
            return null;
        }
        return text.substring(i + subString.length());
    }

    /**
     * @return list containing all words in {@code text}, or {@link List#of()} if there are none.
     * The <b>word</b> here means the maximum sub-string consisting entirely of characters which are <code>Character.isJavaIdentifierPart(c)</code>.
     */
    @Contract(pure = true)
    public static List<String> getWordsIn(String text) {
        List<String> result = null;
        int start = -1;
        for (int i = 0, n = text.length(); i < n; i++) {
            char c = text.charAt(i);
            boolean isIdentifierPart = Character.isJavaIdentifierPart(c);
            if (isIdentifierPart && start == -1) {
                start = i;
            }
            if (isIdentifierPart && i == n - 1 && start != -1) {
                if (result == null) {
                    result = new ArrayList<>(1);
                }
                result.add(text.substring(start, i + 1));
            }
            else if (!isIdentifierPart && start != -1) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(text.substring(start, i));
                start = -1;
            }
        }
        if (result == null) {
            return List.of();
        }
        return result;
    }

    @Contract(pure = true)
    public static String escapeSlashes(String str) {
        return escapeChar(str, '/');
    }

    @Contract(pure = true)
    public static String escapeBackSlashes(String str) {
        return escapeChar(str, '\\');
    }

    public static void escapeSlashes(StringBuilder buf) {
        escapeChar(buf, '/');
    }

    @Contract(pure = true)
    public static String capitalizeWithJavaBeanConvention(String s) {
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1))) {
            return s;
        }
        return capitalize(s);
    }

    @Contract(pure = true)
    public static String getQualifiedName(@Nullable String packageName, String className) {
        if (packageName == null || packageName.isEmpty()) {
            return className;
        }
        return packageName + '.' + className;
    }

    @Contract(pure = true)
    public static boolean containsLineBreak(CharSequence text) {
        for (int i = 0, n = text.length(); i < n; i++) {
            if (isLineBreak(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static String unescapeSlashes(String str) {
        StringBuilder buf = new StringBuilder(str.length());
        unescapeChar(buf, str, '/');
        return buf.toString();
    }

    @Contract(pure = true)
    public static String unescapeBackSlashes(String str) {
        StringBuilder buf = new StringBuilder(str.length());
        unescapeChar(buf, str, '\\');
        return buf.toString();
    }

    private static void unescapeChar(StringBuilder buf, String str, char unescapeChar) {
        int length = str.length();
        int last = length - 1;
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            if (ch == '\\' && i != last) {
                i++;
                ch = str.charAt(i);
                if (ch != unescapeChar) {
                    buf.append('\\');
                }
            }

            buf.append(ch);
        }
    }

    /**
     * Does the string have an uppercase character?
     *
     * @param s the string to test.
     * @return true if the string has an uppercase character, false if not.
     */
    @Contract(pure = true)
    public static boolean hasUpperCaseChar(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            if (Character.isUpperCase(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does the string have a lowercase character?
     *
     * @param s the string to test.
     * @return true if the string has a lowercase character, false if not.
     */
    @Contract(pure = true)
    public static boolean hasLowerCaseChar(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            if (Character.isLowerCase(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static boolean containsWhitespaces(@Nullable CharSequence s) {
        if (s == null) {
            return false;
        }

        for (int i = 0, n = s.length(); i < n; i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    @Nullable
    public static String substringBefore(String text, String subString) {
        int i = text.indexOf(subString);
        if (i == -1) {
            return null;
        }
        return text.substring(0, i);
    }

    @Contract(pure = true)
    public static String substringBeforeLast(String text, String subString) {
        int i = text.lastIndexOf(subString);
        if (i == -1) {
            return text;
        }
        return text.substring(0, i);
    }

    @Nullable
    public static String replaceUnicodeEscapeSequences(@Nullable String text) {
        if (text == null) {
            return null;
        }
        int length = text.length();
        StringBuilder sb = new StringBuilder(text.length());
        outer:
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                int j = i + 1;
                boolean escape = true;
                while (j < length && (c = text.charAt(j)) == '\\') {
                    escape = !escape;
                    j++;
                }
                if (!escape || c != 'u') {
                    sb.append(text, i, j);
                    i = j - 1;
                    continue;
                }
                while (j < length && text.charAt(j) == 'u') j++;
                if (j > length - 4) {
                    sb.append(text, i, j);
                    i = j - 1;
                    continue;
                }
                for (int k = 0; k < 4; k++) {
                    if (!isHexDigit(text.charAt(j + k))) {
                        sb.append(text, i, j + k);
                        i = j + k - 1;
                        continue outer;
                    }
                }
                char d = (char) Integer.parseInt(text.substring(j, j + 4), 16);
                sb.append(d);
                i = j + 3;
            }
            else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    @Contract(pure = true)
    public static int indexOfSubstringEnd(String text, String subString) {
        int i = text.indexOf(subString);
        if (i == -1) {
            return -1;
        }
        return i + subString.length();
    }

    @Contract(pure = true)
    public static boolean containsAlphaCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static String[] surround(String[] strings1, String prefix, String suffix) {
        String[] result = strings1.length == 0 ? EMPTY_STRING_ARRAY : new String[strings1.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = prefix + strings1[i] + suffix;
        }

        return result;
    }

    /**
     * Escape property name or key in property file. Unicode characters are escaped as well.
     *
     * @param input an input to escape
     * @param isKey if true, the rules for key escaping are applied. The leading space is escaped in that case.
     * @return an escaped string
     */
    @Contract(pure = true)
    public static String escapeProperty(String input, boolean isKey) {
        StringBuilder escaped = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case ' ':
                    if (isKey && i == 0) {
                        // only the leading space has to be escaped
                        escaped.append('\\');
                    }
                    escaped.append(' ');
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\\':
                case '#':
                case '!':
                case ':':
                case '=':
                    escaped.append('\\');
                    escaped.append(ch);
                    break;
                default:
                    if (20 < ch && ch < 0x7F) {
                        escaped.append(ch);
                    }
                    else {
                        escaped.append("\\u");
                        escaped.append(Character.forDigit((ch >> 12) & 0xF, 16));
                        escaped.append(Character.forDigit((ch >> 8) & 0xF, 16));
                        escaped.append(Character.forDigit((ch >> 4) & 0xF, 16));
                        escaped.append(Character.forDigit((ch) & 0xF, 16));
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    public static boolean trimEnd(StringBuilder buffer, CharSequence end) {
        if (endsWith(buffer, end)) {
            buffer.delete(buffer.length() - end.length(), buffer.length());
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if {@code s1} and {@code s2} refer to the same instance of {@link String}.
     * There are only few cases when you really need to use this method instead of {@link String#equals} or {@link Objects#equals}:
     * <ul>
     * <li>for small performance improvement if you're sure that there will be no different instances of the same string;</li>
     * <li>to implement "Sentinel" pattern; in that case use {@link String#String(String)} constructor to create the sentinel instance.</li>
     * </ul>
     */
    @SuppressWarnings({"StringEquality", "StringEqualitySSR"})
    public static boolean areSameInstance(@Nullable String s1, @Nullable String s2) {
        return s1 == s2;
    }

    @Contract(pure = true)
    public static boolean equalsIgnoreWhitespaces(@Nullable CharSequence s1, @Nullable CharSequence s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        int index1 = 0;
        int index2 = 0;
        while (index1 < len1 && index2 < len2) {
            if (s1.charAt(index1) == s2.charAt(index2)) {
                index1++;
                index2++;
                continue;
            }

            boolean skipped = false;
            while (index1 != len1 && isWhiteSpace(s1.charAt(index1))) {
                skipped = true;
                index1++;
            }
            while (index2 != len2 && isWhiteSpace(s2.charAt(index2))) {
                skipped = true;
                index2++;
            }

            if (!skipped) {
                return false;
            }
        }

        for (; index1 != len1; index1++) {
            if (!isWhiteSpace(s1.charAt(index1))) {
                return false;
            }
        }
        for (; index2 != len2; index2++) {
            if (!isWhiteSpace(s2.charAt(index2))) {
                return false;
            }
        }

        return true;
    }

    @Contract(pure = true)
    public static boolean equalsTrimWhitespaces(CharSequence s1, CharSequence s2) {
        int start1 = 0;
        int end1 = s1.length();
        int start2 = 0;
        int end2 = s2.length();

        while (start1 < end1) {
            char c = s1.charAt(start1);
            if (!isWhiteSpace(c)) {
                break;
            }
            start1++;
        }

        while (start1 < end1) {
            char c = s1.charAt(end1 - 1);
            if (!isWhiteSpace(c)) {
                break;
            }
            end1--;
        }

        while (start2 < end2) {
            char c = s2.charAt(start2);
            if (!isWhiteSpace(c)) {
                break;
            }
            start2++;
        }

        while (start2 < end2) {
            char c = s2.charAt(end2 - 1);
            if (!isWhiteSpace(c)) {
                break;
            }
            end2--;
        }

        CharSequence ts1 = new CharSequenceSubSequence(s1, start1, end1);
        CharSequence ts2 = new CharSequenceSubSequence(s2, start2, end2);

        return equals(ts1, ts2);
    }

    @Contract(pure = true)
    public static int lastIndexOfAny(CharSequence s, String chars) {
        for (int i = s.length() - 1; i >= 0; i--) {
            if (containsChar(chars, s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the next position in the supplied CharSequence which is neither a space nor a tab.
     *
     * @param text text
     * @param pos  starting position
     * @return position of the first non-whitespace character after or equal to pos; or the length of the CharSequence
     * if no non-whitespace character found
     */
    @Contract(pure = true)
    public static int skipWhitespaceForward(CharSequence text, int pos) {
        int length = text.length();
        while (pos < length && isWhitespaceOrTab(text.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    /**
     * Finds the previous position in the supplied CharSequence which is neither a space nor a tab.
     *
     * @param text text
     * @param pos  starting position
     * @return position of the character before or equal to pos which has no space or tab before;
     * or zero if no non-whitespace character found
     */
    @Contract(pure = true)
    public static int skipWhitespaceBackward(CharSequence text, int pos) {
        while (pos > 0 && isWhitespaceOrTab(text.charAt(pos - 1))) {
            pos--;
        }
        return pos;
    }

    private static boolean isWhitespaceOrTab(char c) {
        return c == ' ' || c == '\t';
    }

    @Contract(pure = true)
    public static boolean endsWith(CharSequence text, int start, int end, CharSequence suffix) {
        if (start < 0 || end > text.length()) {
            throw new IllegalArgumentException("Invalid offsets: start=" + start + "; end=" + end + "; text.length()=" + text.length());
        }

        int suffixLen = suffix.length();
        int delta = end - suffixLen;
        if (delta < start) {
            return false;
        }

        for (int i = 0; i < suffixLen; i++) {
            if (text.charAt(delta + i) != suffix.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Say smallPart = "op" and bigPart="open". Method returns true for "Ope" and false for "ops"
     */
    @Contract(pure = true)
    public static boolean isBetween(String string, String smallPart, String bigPart) {
        String s = string.toLowerCase();
        return s.startsWith(smallPart.toLowerCase()) && bigPart.toLowerCase().startsWith(s);
    }
}
