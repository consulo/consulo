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

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;

/**
 * Based on IDEA code
 */
public class StringUtil {
  @Nonnull
  @Contract(pure = true)
  public static <T> String join(@Nonnull Collection<? extends T> items, @Nonnull Function<? super T, String> f, @Nonnull String separator) {
    if (items.isEmpty()) return "";
    return join((Iterable<? extends T>)items, f, separator);
  }

  @Contract(pure = true)
  public static String join(@Nonnull Iterable<?> items, @Nonnull String separator) {
    StringBuilder result = new StringBuilder();
    for (Object item : items) {
      result.append(item).append(separator);
    }
    if (result.length() > 0) {
      result.setLength(result.length() - separator.length());
    }
    return result.toString();
  }

  /**
   * Equivalent to string.startsWith(prefixes[0] + prefixes[1] + ...) but avoids creating an object for concatenation.
   */
  @Contract(pure = true)
  public static boolean startsWithConcatenation(@Nonnull String string, @Nonnull String... prefixes) {
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

  @Contract(value = "null -> null", pure = true)
  public static String decapitalize(@Nullable String name) {
    if (isEmpty(name)) {
      return name;
    }
    if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
      return name;
    }
    char chars[] = name.toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    return new String(chars);
  }

  @Contract(pure = true)
  public static boolean startsWithChar(@Nullable CharSequence s, char prefix) {
    return s != null && s.length() != 0 && s.charAt(0) == prefix;
  }

  public static int stringHashCode(@Nonnull CharSequence chars) {
    if (chars instanceof String || chars instanceof CharSequenceWithStringHash) {
      // we know for sure these classes have conformant (and maybe faster) hashCode()
      return chars.hashCode();
    }

    return stringHashCode(chars, 0, chars.length());
  }

  @Contract(pure = true)
  public static int stringHashCode(@Nonnull CharSequence chars, int from, int to) {
    int h = 0;
    for (int off = from; off < to; off++) {
      h = 31 * h + chars.charAt(off);
    }
    return h;
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
  public static int stringHashCodeInsensitive(@Nonnull char[] chars, int from, int to) {
    int h = 0;
    for (int off = from; off < to; off++) {
      h = 31 * h + toLowerCase(chars[off]);
    }
    return h;
  }

  @Contract(pure = true)
  public static int stringHashCodeInsensitive(@Nonnull CharSequence chars, int from, int to) {
    int h = 0;
    for (int off = from; off < to; off++) {
      h = 31 * h + toLowerCase(chars.charAt(off));
    }
    return h;
  }

  @Contract(pure = true)
  public static int stringHashCodeInsensitive(@Nonnull CharSequence chars) {
    return stringHashCodeInsensitive(chars, 0, chars.length());
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

  @Contract(pure = true)
  public static int stringHashCodeIgnoreWhitespaces(@Nonnull CharSequence chars, int from, int to) {
    int h = 0;
    for (int off = from; off < to; off++) {
      char c = chars.charAt(off);
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
    return c == '\n' || c == '\t' || c == ' ';
  }

  @Contract(pure = true)
  public static int stringHashCodeIgnoreWhitespaces(@Nonnull CharSequence chars) {
    return stringHashCodeIgnoreWhitespaces(chars, 0, chars.length());
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> String join(@Nonnull Iterable<? extends T> items, @Nonnull Function<? super T, String> f, @Nonnull String separator) {
    final StringBuilder result = new StringBuilder();
    for (T item : items) {
      String string = f.apply(item);
      if (string != null && !string.isEmpty()) {
        if (result.length() != 0) result.append(separator);
        result.append(string);
      }
    }
    return result.toString();
  }

  @Contract("null,!null,_ -> false; !null,null,_ -> false; null,null,_ -> true")
  public static boolean equal(@Nullable CharSequence s1, @Nullable CharSequence s2, boolean caseSensitive) {
    if (s1 == s2) return true;
    if (s1 == null || s2 == null) return false;

    if (s1.length() != s2.length()) return false;

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
  @Contract(pure = true)
  public static boolean equals(@Nullable CharSequence s1, @Nullable CharSequence s2) {
    if (s1 == null ^ s2 == null) {
      return false;
    }

    if (s1 == null) {
      return true;
    }

    if (s1.length() != s2.length()) {
      return false;
    }
    for (int i = 0; i < s1.length(); i++) {
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

  @Nonnull
  @Contract(pure = true)
  public static CharSequence toUpperCase(@Nonnull CharSequence s) {
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

  @Contract(pure = true)
  public static char toUpperCase(char a) {
    if (a < 'a') {
      return a;
    }
    if (a <= 'z') {
      return (char)(a + ('A' - 'a'));
    }
    return Character.toUpperCase(a);
  }

  @Contract(pure = true)
  public static char toLowerCase(char a) {
    if (a < 'A' || a >= 'a' && a <= 'z') {
      return a;
    }

    if (a <= 'Z') {
      return (char)(a + ('a' - 'A'));
    }

    return Character.toLowerCase(a);
  }

  @Contract(pure = true)
  public static boolean isJavaIdentifierStart(char c) {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || Character.isJavaIdentifierStart(c);
  }

  @Contract(pure = true)
  public static boolean isJavaIdentifierPart(char c) {
    return c >= '0' && c <= '9' || isJavaIdentifierStart(c);
  }

  @Contract(pure = true)
  public static boolean isJavaIdentifier(@Nonnull String text) {
    int len = text.length();
    if (len == 0) return false;

    if (!isJavaIdentifierStart(text.charAt(0))) return false;

    for (int i = 1; i < len; i++) {
      if (!isJavaIdentifierPart(text.charAt(i))) return false;
    }

    return true;
  }

  @Contract(pure = true)
  public static boolean endsWithChar(@Nullable CharSequence s, char suffix) {
    return s != null && s.length() != 0 && s.charAt(s.length() - 1) == suffix;
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimStart(@Nonnull String s, @Nonnull String prefix) {
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

  @Nonnull
  @Contract(pure = true)
  public static String notNullize(@Nullable final String s) {
    return notNullize(s, "");
  }

  @Nonnull
  @Contract(pure = true)
  public static String notNullize(@Nullable final String s, @Nonnull String defaultValue) {
    return s == null ? defaultValue : s;
  }

  @Nonnull
  @Contract(pure = true)
  public static String notNullizeIfEmpty(@Nullable final String s, @Nonnull String defaultValue) {
    return isEmpty(s) ? defaultValue : s;
  }

  @Nullable
  @Contract(pure = true)
  public static String nullize(@Nullable final String s) {
    return nullize(s, false);
  }

  @Nullable
  @Contract(pure = true)
  public static String nullize(@Nullable final String s, boolean nullizeSpaces) {
    if (nullizeSpaces) {
      if (isEmptyOrSpaces(s)) return null;
    }
    else {
      if (isEmpty(s)) return null;
    }
    return s;
  }

  @Contract(value = "null -> true", pure = true)
  // we need to keep this method to preserve backward compatibility
  public static boolean isEmptyOrSpaces(@Nullable String s) {
    return isEmptyOrSpaces(((CharSequence)s));
  }

  @Contract(value = "null -> true", pure = true)
  public static boolean isEmptyOrSpaces(@Nullable CharSequence s) {
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

  @Contract(pure = true)
  public static int compareVersionNumbers(@Nullable String v1, @Nullable String v2) {
    // todo duplicates com.intellij.util.text.VersionComparatorUtil.compare
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

    String[] part1 = v1.split("[\\.\\_\\-]");
    String[] part2 = v2.split("[\\.\\_\\-]");

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
      if (cmp != 0) return cmp;
    }

    if (part1.length == part2.length) {
      return 0;
    }
    else {
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
        if (cmp != 0) return left ? cmp : -cmp;
      }
      return 0;
    }
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull CharSequence s, char c) {
    return indexOf(s, c, 0, s.length());
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull CharSequence s, char c, int start) {
    return indexOf(s, c, start, s.length());
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull CharSequence s, char c, int start, int end) {
    for (int i = start; i < end; i++) {
      if (s.charAt(i) == c) return i;
    }
    return -1;
  }

  @Contract(pure = true)
  public static String escapeChar(@Nonnull final String str, final char character) {
    return escapeChars(str, character);
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapeChars(@Nonnull final String str, final char... character) {
    final StringBuilder buf = new StringBuilder(str);
    for (char c : character) {
      escapeChar(buf, c);
    }
    return buf.toString();
  }

  private static void escapeChar(@Nonnull final StringBuilder buf, final char character) {
    int idx = 0;
    while ((idx = indexOf(buf, character, idx)) >= 0) {
      buf.insert(idx, "\\");
      idx += 2;
    }
  }

  public static void escapeStringCharacters(int length, @Nonnull String str, @Nonnull StringBuilder buffer) {
    escapeStringCharacters(length, str, "\"", buffer);
  }

  @Nonnull
  public static StringBuilder escapeStringCharacters(int length, @Nonnull String str, @Nullable String additionalChars, @Nonnull StringBuilder buffer) {
    return escapeStringCharacters(length, str, additionalChars, true, buffer);
  }

  @Nonnull
  public static StringBuilder escapeStringCharacters(int length, @Nonnull String str, @Nullable String additionalChars, boolean escapeSlash, @Nonnull StringBuilder buffer) {
    char prev = 0;
    for (int idx = 0; idx < length; idx++) {
      char ch = str.charAt(idx);
      switch (ch) {
        case '\b':
          buffer.append("\\b");
          break;

        case '\t':
          buffer.append("\\t");
          break;

        case '\n':
          buffer.append("\\n");
          break;

        case '\f':
          buffer.append("\\f");
          break;

        case '\r':
          buffer.append("\\r");
          break;

        default:
          if (escapeSlash && ch == '\\') {
            buffer.append("\\\\");
          }
          else if (additionalChars != null && additionalChars.indexOf(ch) > -1 && (escapeSlash || prev != '\\')) {
            buffer.append("\\").append(ch);
          }
          else if (!isPrintableUnicode(ch)) {
            CharSequence hexCode = toUpperCase(Integer.toHexString(ch));
            buffer.append("\\u");
            int paddingCount = 4 - hexCode.length();
            while (paddingCount-- > 0) {
              buffer.append(0);
            }
            buffer.append(hexCode);
          }
          else {
            buffer.append(ch);
          }
      }
      prev = ch;
    }
    return buffer;
  }

  @Contract(pure = true)
  public static boolean isPrintableUnicode(char c) {
    int t = Character.getType(c);
    return t != Character.UNASSIGNED &&
           t != Character.LINE_SEPARATOR &&
           t != Character.PARAGRAPH_SEPARATOR &&
           t != Character.CONTROL &&
           t != Character.FORMAT &&
           t != Character.PRIVATE_USE &&
           t != Character.SURROGATE;
  }
}
