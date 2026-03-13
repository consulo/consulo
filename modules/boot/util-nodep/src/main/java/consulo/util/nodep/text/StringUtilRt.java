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

import consulo.util.nodep.annotation.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Stripped-down version of {@code consulo.ide.impl.idea.openapi.util.text.StringUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers so it should not contain any library dependencies.
 *
 * @since 12.0
 */
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor"})
public class StringUtilRt {
    public static boolean charsEqualIgnoreCase(char a, char b) {
        return a == b || toUpperCase(a) == toUpperCase(b) || toLowerCase(a) == toLowerCase(b);
    }

    @Contract("null -> true")
    public static boolean isEmpty(@Nullable String s) {
        return s == null || s.isEmpty();
    }

    @Contract("null -> true")
    public static boolean isEmpty(@Nullable CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static String notNullize(@Nullable String s) {
        return notNullize(s, "");
    }

    public static String notNullize(@Nullable String s, String defaultValue) {
        return s == null ? defaultValue : s;
    }

    @Nullable
    public static String nullize(@Nullable String s) {
        return nullize(s, false);
    }

    @Nullable
    public static String nullize(@Nullable String s, boolean nullizeSpaces) {
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
    @Contract("null -> true")
    public static boolean isEmptyOrSpaces(@Nullable String s) {
        return isEmptyOrSpaces(((CharSequence) s));
    }

    @Contract("null -> true")
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

    public static String getPackageName(String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(0, lastPointIdx);
        }
        return "";
    }

    public static boolean startsWithChar(@Nullable CharSequence s, char prefix) {
        return s != null && s.length() != 0 && s.charAt(0) == prefix;
    }

    public static boolean endsWithChar(@Nullable CharSequence s, char suffix) {
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
        start = Math.max(start, 0);
        for (int i = Math.min(end, s.length()) - 1; i >= start; i--) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }
}
