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
package consulo.ide.impl.idea.openapi.util.text;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Stripped-down version of {@code consulo.ide.impl.idea.openapi.util.text.StringUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers so it should not contain any library dependencies.
 *
 * @since 12.0
 */
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor"})
public class StringUtilRt {
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
  public static boolean charsEqualIgnoreCase(char a, char b) {
    return a == b || toUpperCase(a) == toUpperCase(b) || toLowerCase(a) == toLowerCase(b);
  }

  @Nonnull
  @Contract(pure = true)
  public static String replace(@NonNls @Nonnull String text, @NonNls @Nonnull String oldS, @NonNls @Nonnull String newS) {
    return replace(text, oldS, newS, false);
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
  public static String notNullize(@Nullable String s) {
    return notNullize(s, "");
  }

  @Nonnull
  @Contract(pure = true)
  public static String notNullize(@Nullable String s, @Nonnull String defaultValue) {
    return s == null ? defaultValue : s;
  }

  @Nonnull
  @Contract(pure = true)
  public static String notNullizeIfEmpty(@Nullable String s, @Nonnull String defaultValue) {
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
  public static String replace(@NonNls @Nonnull String text, @NonNls @Nonnull String oldS, @NonNls @Nonnull String newS, boolean ignoreCase) {
    if (text.length() < oldS.length()) return text;

    StringBuilder newText = null;
    int i = 0;

    while (i < text.length()) {
      int index = ignoreCase ? indexOfIgnoreCase(text, oldS, i) : text.indexOf(oldS, i);
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
  @Contract(pure = true)
  public static int indexOfIgnoreCase(@Nonnull String where, @Nonnull String what, int fromIndex) {
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

  /**
   * Converts line separators to <code>"\n"</code>
   */
  @Nonnull
  @Contract(pure = true)
  public static String convertLineSeparators(@Nonnull String text) {
    return convertLineSeparators(text, false);
  }

  @Nonnull
  @Contract(pure = true)
  public static String convertLineSeparators(@Nonnull String text, boolean keepCarriageReturn) {
    return convertLineSeparators(text, "\n", null, keepCarriageReturn);
  }

  @Nonnull
  @Contract(pure = true)
  public static String convertLineSeparators(@Nonnull String text, @Nonnull String newSeparator) {
    return convertLineSeparators(text, newSeparator, null);
  }

  @Nonnull
  @Contract(pure = true)
  public static CharSequence convertLineSeparators(@Nonnull CharSequence text, @Nonnull String newSeparator) {
    return unifyLineSeparators(text, newSeparator, null, false);
  }

  @Nonnull
  public static String convertLineSeparators(@Nonnull String text, @Nonnull String newSeparator, @Nullable int[] offsetsToKeep) {
    return convertLineSeparators(text, newSeparator, offsetsToKeep, false);
  }

  @Nonnull
  public static String convertLineSeparators(@Nonnull String text, @Nonnull String newSeparator, @Nullable int[] offsetsToKeep, boolean keepCarriageReturn) {
    return unifyLineSeparators(text, newSeparator, offsetsToKeep, keepCarriageReturn).toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static CharSequence unifyLineSeparators(@Nonnull CharSequence text) {
    return unifyLineSeparators(text, "\n", null, false);
  }

  @Nonnull
  public static CharSequence unifyLineSeparators(@Nonnull CharSequence text, @Nonnull String newSeparator, @Nullable int[] offsetsToKeep, boolean keepCarriageReturn) {
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
    if (offsets == null) return;
    int shift = newLength - oldLength;
    if (shift == 0) return;
    for (int i = 0; i < offsets.length; i++) {
      int offset = offsets[i];
      if (offset >= changeOffset + oldLength) {
        offsets[i] += shift;
      }
    }
  }

  @Contract(pure = true)
  public static int parseInt(@Nullable String string, int defaultValue) {
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

  @Contract(pure = true)
  public static double parseDouble(String string, double defaultValue) {
    try {
      return Double.parseDouble(string);
    }
    catch (Exception e) {
      return defaultValue;
    }
  }

  @Contract(pure = true)
  public static boolean parseBoolean(String string, boolean defaultValue) {
    try {
      return Boolean.parseBoolean(string);
    }
    catch (Exception e) {
      return defaultValue;
    }
  }

  @Contract(pure = true)
  public static <E extends Enum<E>> E parseEnum(String string, E defaultValue, Class<E> clazz) {
    try {
      return Enum.valueOf(clazz, string);
    }
    catch (Exception e) {
      return defaultValue;
    }
  }

  @Nonnull
  @Contract(pure = true)
  public static String getShortName(@Nonnull Class aClass) {
    return getShortName(aClass.getName());
  }

  @Nonnull
  @Contract(pure = true)
  public static String getShortName(@Nonnull String fqName) {
    return getShortName(fqName, '.');
  }

  @Nonnull
  @Contract(pure = true)
  public static String getShortName(@Nonnull String fqName, char separator) {
    int lastPointIdx = fqName.lastIndexOf(separator);
    if (lastPointIdx >= 0) {
      return fqName.substring(lastPointIdx + 1);
    }
    return fqName;
  }

  @Contract(pure = true)
  public static boolean startsWithChar(@Nullable CharSequence s, char prefix) {
    return s != null && s.length() != 0 && s.charAt(0) == prefix;
  }

  @Contract(pure = true)
  public static boolean endsWithChar(@Nullable CharSequence s, char suffix) {
    return s != null && s.length() != 0 && s.charAt(s.length() - 1) == suffix;
  }

  @Contract(pure = true)
  public static boolean endsWith(@Nonnull CharSequence text, @Nonnull CharSequence suffix) {
    int l1 = text.length();
    int l2 = suffix.length();
    if (l1 < l2) return false;

    for (int i = l1 - 1; i >= l1 - l2; i--) {
      if (text.charAt(i) != suffix.charAt(i + l2 - l1)) return false;
    }

    return true;
  }

  @Contract(pure = true)
  public static boolean startsWithIgnoreCase(@NonNls @Nonnull String str, @NonNls @Nonnull String prefix) {
    int stringLength = str.length();
    int prefixLength = prefix.length();
    return stringLength >= prefixLength && str.regionMatches(true, 0, prefix, 0, prefixLength);
  }

  @Contract(pure = true)
  public static boolean endsWithIgnoreCase(@NonNls @Nonnull CharSequence text, @NonNls @Nonnull CharSequence suffix) {
    int l1 = text.length();
    int l2 = suffix.length();
    if (l1 < l2) return false;

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
  @Contract(pure = true)
  public static int lastIndexOf(@Nonnull CharSequence s, char c, int start, int end) {
    for (int i = end - 1; i >= start; i--) {
      if (s.charAt(i) == c) return i;
    }
    return -1;
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimStart(@Nonnull String s, @NonNls @Nonnull String prefix) {
    if (s.startsWith(prefix)) {
      return s.substring(prefix.length());
    }
    return s;
  }
}
