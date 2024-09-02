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
package consulo.util.nodep.text;

/**
 * Stripped-down version of {@code consulo.ide.impl.idea.openapi.util.text.StringUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers so it should not contain any library dependencies.
 *
 * @since 12.0
 */
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor"})
public class StringUtilRt {
    public static boolean equal(CharSequence s1, CharSequence s2, boolean caseSensitive) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        if (s1.length() != s2.length()) {
            return false;
        }
        if (caseSensitive) {
            for (int i = 0; i < s1.length(); i++) {
                if (s1.charAt(i) != s2.charAt(i)) {
                    return false;
                }
            }
        }
        else {
            for (int i = 0; i < s1.length(); i++) {
                if (!charsEqualIgnoreCase(s1.charAt(i), s2.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static long parseLong(String string, long defaultValue) {
        if (string != null) {
            try {
                return Long.parseLong(string);
            }
            catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public static boolean charsEqualIgnoreCase(char a, char b) {
        return a == b || toUpperCase(a) == toUpperCase(b) || toLowerCase(a) == toLowerCase(b);
    }

    public static String replace(String text, String oldS, String newS) {
        return replace(text, oldS, newS, false);
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static String notNullize(final String s) {
        return notNullize(s, "");
    }

    public static String notNullize(final String s, String defaultValue) {
        return s == null ? defaultValue : s;
    }

    public static String notNullizeIfEmpty(final String s, String defaultValue) {
        return isEmpty(s) ? defaultValue : s;
    }

    public static String nullize(final String s) {
        return nullize(s, false);
    }

    public static String nullize(final String s, boolean nullizeSpaces) {
        if (nullizeSpaces) {
            if (isEmptyOrSpaces(s)) {
                return null;
            }
        }
        else {
            if (isEmpty(s)) {
                return null;
            }
        }
        return s;
    }

    // we need to keep this method to preserve backward compatibility
    public static boolean isEmptyOrSpaces(String s) {
        return isEmptyOrSpaces(((CharSequence) s));
    }

    public static boolean isEmptyOrSpaces(CharSequence s) {
        if (isEmpty(s)) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > ' ') {
                return false;
            }
        }
        return true;
    }

    public static String replace(final String text, final String oldS, final String newS, final boolean ignoreCase) {
        if (text.length() < oldS.length()) {
            return text;
        }
        StringBuilder newText = null;
        int i = 0;
        while (i < text.length()) {
            final int index = ignoreCase ? indexOfIgnoreCase(text, oldS, i) : text.indexOf(oldS, i);
            if (index < 0) {
                if (i == 0) {
                    return text;
                }
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

    /**
     * Implementation copied from {@link String#indexOf(String, int)} except character comparisons made case insensitive
     */
    public static int indexOfIgnoreCase(String where, String what, int fromIndex) {
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
            /* Look for first character. */
            if (!charsEqualIgnoreCase(where.charAt(i), first)) {
                while (++i <= max && !charsEqualIgnoreCase(where.charAt(i), first)) ;
            }
            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = 1; j < end && charsEqualIgnoreCase(where.charAt(j), what.charAt(k)); j++, k++) ;
                if (j == end) {
                    /* Found whole string. */
                    return i;
                }
            }
        }
        return -1;
    }

    public static CharSequence toUpperCase(CharSequence s) {
        StringBuilder answer = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char upcased = toUpperCase(c);
            if (answer == null && upcased != c) {
                answer = new StringBuilder(s.length());
                answer.append(s.subSequence(0, i));
            }
            if (answer != null) {
                answer.append(upcased);
            }
        }
        return answer == null ? s : answer;
    }

    public static char toUpperCase(char a) {
        if (a < 'a') {
            return a;
        }
        if (a <= 'z') {
            return (char) (a + ('A' - 'a'));
        }
        return Character.toUpperCase(a);
    }

    public static char toLowerCase(char a) {
        if (a < 'A' || a >= 'a' && a <= 'z') {
            return a;
        }
        if (a <= 'Z') {
            return (char) (a + ('a' - 'A'));
        }
        return Character.toLowerCase(a);
    }

    /**
     * Converts line separators to <code>"\n"</code>
     */
    public static String convertLineSeparators(String text) {
        return convertLineSeparators(text, false);
    }

    public static String convertLineSeparators(String text, boolean keepCarriageReturn) {
        return convertLineSeparators(text, "\n", null, keepCarriageReturn);
    }

    public static String convertLineSeparators(String text, String newSeparator) {
        return convertLineSeparators(text, newSeparator, null);
    }

    public static CharSequence convertLineSeparators(CharSequence text, String newSeparator) {
        return unifyLineSeparators(text, newSeparator, null, false);
    }

    public static String convertLineSeparators(String text, String newSeparator, int[] offsetsToKeep) {
        return convertLineSeparators(text, newSeparator, offsetsToKeep, false);
    }

    public static String convertLineSeparators(String text, String newSeparator, int[] offsetsToKeep, boolean keepCarriageReturn) {
        return unifyLineSeparators(text, newSeparator, offsetsToKeep, keepCarriageReturn).toString();
    }

    public static CharSequence unifyLineSeparators(CharSequence text) {
        return unifyLineSeparators(text, "\n", null, false);
    }

    public static CharSequence unifyLineSeparators(CharSequence text, String newSeparator, int[] offsetsToKeep, boolean keepCarriageReturn) {
        StringBuilder buffer = null;
        int intactLength = 0;
        final boolean newSeparatorIsSlashN = "\n".equals(newSeparator);
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
            else {
                if (buffer == null) {
                    intactLength++;
                }
                else {
                    buffer.append(c);
                }
            }
        }
        return buffer == null ? text : buffer;
    }

    private static void shiftOffsets(int[] offsets, int changeOffset, int oldLength, int newLength) {
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

    public static int parseInt(String string, final int defaultValue) {
        if (string == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(string);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    public static double parseDouble(final String string, final double defaultValue) {
        try {
            return Double.parseDouble(string);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean parseBoolean(final String string, final boolean defaultValue) {
        try {
            return Boolean.parseBoolean(string);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    public static <E extends Enum<E>> E parseEnum(String string, E defaultValue, Class<E> clazz) {
        try {
            return Enum.valueOf(clazz, string);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getShortName(Class aClass) {
        return getShortName(aClass.getName());
    }

    public static String getShortName(String fqName) {
        return getShortName(fqName, '.');
    }

    public static String getShortName(String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(lastPointIdx + 1);
        }
        return fqName;
    }

    public static String getPackageName(String fqName) {
        return getPackageName(fqName, '.');
    }

    public static String getPackageName(String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(0, lastPointIdx);
        }
        return "";
    }

    public static boolean startsWithChar(CharSequence s, char prefix) {
        return s != null && s.length() != 0 && s.charAt(0) == prefix;
    }

    public static boolean endsWithChar(CharSequence s, char suffix) {
        return s != null && s.length() != 0 && s.charAt(s.length() - 1) == suffix;
    }

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

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        final int stringLength = str.length();
        final int prefixLength = prefix.length();
        return stringLength >= prefixLength && str.regionMatches(true, 0, prefix, 0, prefixLength);
    }

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
    public static int lastIndexOf(CharSequence s, char c, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    public static String trimStart(String s, String prefix) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }
}
