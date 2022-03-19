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

import consulo.util.lang.internal.NaturalComparator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Based on IDEA code
 */
public class StringUtil {
  /**
   * Expirable CharSequence. Very useful to control external library execution time,
   * i.e. when java.util.regex.Pattern match goes out of control.
   */
  public abstract static class BombedCharSequence implements CharSequence {
    private final CharSequence delegate;
    private int i;
    private boolean myDefused;

    public BombedCharSequence(@Nonnull CharSequence sequence) {
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

    @Nonnull
    @Override
    public String toString() {
      check();
      return delegate.toString();
    }

    protected abstract void checkCanceled();

    @Nonnull
    @Override
    public CharSequence subSequence(int i, int i1) {
      check();
      return delegate.subSequence(i, i1);
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(StringUtil.class);

  private static final String[] MN_QUOTED = {"&&", "__"};
  private static final String[] MN_CHARS = {"&", "_"};

  private static final String[] REPLACES_REFS = {"&lt;", "&gt;", "&amp;", "&#39;", "&quot;"};
  private static final String[] REPLACES_DISP = {"<", ">", "&", "'", "\""};

  private static final Pattern EOL_SPLIT_KEEP_SEPARATORS = Pattern.compile("(?<=(\r\n|\n))|(?<=\r)(?=[^\n])");
  private static final Pattern EOL_SPLIT_PATTERN = Pattern.compile(" *(\r|\n|\r\n)+ *");
  private static final Pattern EOL_SPLIT_PATTERN_WITH_EMPTY = Pattern.compile(" *(\r|\n|\r\n) *");
  private static final Pattern EOL_SPLIT_DONT_TRIM_PATTERN = Pattern.compile("(\r|\n|\r\n)+");


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
  public static boolean contains(@Nonnull CharSequence s, int start, int end, char c) {
    return indexOf(s, c, start, end) >= 0;
  }

  @Nonnull
  @Contract(pure = true)
  public static String unescapeStringCharacters(@Nonnull String s) {
    StringBuilder buffer = new StringBuilder(s.length());
    unescapeStringCharacters(s.length(), s, buffer);
    return buffer.toString();
  }

  private static void unescapeStringCharacters(int length, @Nonnull String s, @Nonnull StringBuilder buffer) {
    boolean escaped = false;
    for (int idx = 0; idx < length; idx++) {
      char ch = s.charAt(idx);
      if (!escaped) {
        if (ch == '\\') {
          escaped = true;
        }
        else {
          buffer.append(ch);
        }
      }
      else {
        switch (ch) {
          case 'n':
            buffer.append('\n');
            break;

          case 'r':
            buffer.append('\r');
            break;

          case 'b':
            buffer.append('\b');
            break;

          case 't':
            buffer.append('\t');
            break;

          case 'f':
            buffer.append('\f');
            break;

          case '\'':
            buffer.append('\'');
            break;

          case '\"':
            buffer.append('\"');
            break;

          case '\\':
            buffer.append('\\');
            break;

          case 'u':
            if (idx + 4 < length) {
              try {
                int code = Integer.parseInt(s.substring(idx + 1, idx + 5), 16);
                idx += 4;
                buffer.append((char)code);
              }
              catch (NumberFormatException e) {
                buffer.append("\\u");
              }
            }
            else {
              buffer.append("\\u");
            }
            break;

          default:
            buffer.append(ch);
            break;
        }
        escaped = false;
      }
    }

    if (escaped) buffer.append('\\');
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapeToRegexp(@Nonnull String text) {
    final StringBuilder result = new StringBuilder(text.length());
    return escapeToRegexp(text, result).toString();
  }

  /**
   * Strip out all characters not accepted by given filter
   *
   * @param s      e.g. "/n    my string "
   * @param filter e.g. {@link CharFilter#NOT_WHITESPACE_FILTER}
   * @return stripped string e.g. "mystring"
   */
  @Nonnull
  @Contract(pure = true)
  public static String strip(@Nonnull final String s, @Nonnull final CharFilter filter) {
    final StringBuilder result = new StringBuilder(s.length());
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
  @Nonnull
  @Contract(pure = true)
  public static String[] splitByLines(@Nonnull String string) {
    return splitByLines(string, true);
  }

  /**
   * Splits string by lines. If several line separators are in a row corresponding empty lines
   * are also added to result if {@code excludeEmptyStrings} is {@code false}.
   *
   * @param string String to split
   * @return array of strings
   */
  @Nonnull
  @Contract(pure = true)
  public static String[] splitByLines(@Nonnull String string, boolean excludeEmptyStrings) {
    return (excludeEmptyStrings ? EOL_SPLIT_PATTERN : EOL_SPLIT_PATTERN_WITH_EMPTY).split(string);
  }

  @Contract(pure = true)
  public static int parseInt(@Nullable String string, final int defaultValue) {
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
  public static boolean startsWithIgnoreCase(@NonNls @Nonnull String str, @NonNls @Nonnull String prefix) {
    final int stringLength = str.length();
    final int prefixLength = prefix.length();
    return stringLength >= prefixLength && str.regionMatches(true, 0, prefix, 0, prefixLength);
  }

  @Nonnull
  public static StringBuilder escapeToRegexp(@Nonnull CharSequence text, @Nonnull StringBuilder builder) {
    for (int i = 0; i < text.length(); i++) {
      final char c = text.charAt(i);
      if (c == ' ' || Character.isLetter(c) || Character.isDigit(c) || c == '_') {
        builder.append(c);
      }
      else if (c == '\n') {
        builder.append("\\n");
      }
      else {
        builder.append('\\').append(c);
      }
    }

    return builder;
  }

  @Nonnull
  @Contract(pure = true)
  public static String shortenTextWithEllipsis(@Nonnull final String text, final int maxLength, final int suffixLength) {
    return shortenTextWithEllipsis(text, maxLength, suffixLength, false);
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimMiddle(@Nonnull String text, int maxLength, boolean useEllipsisSymbol) {
    return shortenTextWithEllipsis(text, maxLength, maxLength >> 1, useEllipsisSymbol);
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimMiddle(@Nonnull String text, int maxLength) {
    return shortenTextWithEllipsis(text, maxLength, maxLength >> 1, true);
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
  public static String join(@Nonnull final String[] strings, @Nonnull final String separator) {
    return join(strings, 0, strings.length, separator);
  }

  @Nonnull
  @Contract(pure = true)
  public static String join(@Nonnull final String[] strings, int startIndex, int endIndex, @Nonnull final String separator) {
    final StringBuilder result = new StringBuilder();
    for (int i = startIndex; i < endIndex; i++) {
      if (i > startIndex) result.append(separator);
      result.append(strings[i]);
    }
    return result.toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimLog(@Nonnull final String text, final int limit) {
    if (limit > 5 && text.length() > limit) {
      return text.substring(0, limit - 5) + " ...\n";
    }
    return text;
  }

  public static void quote(@Nonnull final StringBuilder builder) {
    quote(builder, '\"');
  }

  public static void quote(@Nonnull final StringBuilder builder, final char quotingChar) {
    builder.insert(0, quotingChar);
    builder.append(quotingChar);
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapeQuotes(@Nonnull final String str) {
    return escapeChar(str, '"');
  }

  public static void escapeQuotes(@Nonnull final StringBuilder buf) {
    escapeChar(buf, '"');
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
  @Contract(pure = true)
  public static String shortenTextWithEllipsis(@Nonnull final String text, final int maxLength, final int suffixLength, @Nonnull String symbol) {
    final int textLength = text.length();
    if (textLength > maxLength) {
      final int prefixLength = maxLength - suffixLength - symbol.length();
      assert prefixLength > 0;
      return text.substring(0, prefixLength) + symbol + text.substring(textLength - suffixLength);
    }
    else {
      return text;
    }
  }

  @Nonnull
  @Contract(pure = true)
  public static String shortenTextWithEllipsis(@Nonnull final String text, final int maxLength, final int suffixLength, boolean useEllipsisSymbol) {
    String symbol = useEllipsisSymbol ? "\u2026" : "...";
    return shortenTextWithEllipsis(text, maxLength, suffixLength, symbol);
  }

  @Nonnull
  @Contract(pure = true)
  public static String shortenPathWithEllipsis(@Nonnull final String path, final int maxLength, boolean useEllipsisSymbol) {
    return shortenTextWithEllipsis(path, maxLength, (int)(maxLength * 0.7), useEllipsisSymbol);
  }

  @Nonnull
  @Contract(pure = true)
  public static String shortenPathWithEllipsis(@Nonnull final String path, final int maxLength) {
    return shortenPathWithEllipsis(path, maxLength, false);
  }

  @Nonnull
  public static CharSequence unifyLineSeparators(@Nonnull CharSequence text, @Nonnull String newSeparator, @Nullable int[] offsetsToKeep, boolean keepCarriageReturn) {
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

  @Nonnull
  @Contract(pure = true)
  public static List<String> splitHonorQuotes(@Nonnull String s, char separator) {
    final List<String> result = new ArrayList<String>();
    final StringBuilder builder = new StringBuilder(s.length());
    boolean inQuotes = false;
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c == separator && !inQuotes) {
        if (builder.length() > 0) {
          result.add(builder.toString());
          builder.setLength(0);
        }
        continue;
      }

      if ((c == '"' || c == '\'') && !(i > 0 && s.charAt(i - 1) == '\\')) {
        inQuotes = !inQuotes;
      }
      builder.append(c);
    }

    if (builder.length() > 0) {
      result.add(builder.toString());
    }
    return result;
  }

  @Contract(pure = true)
  public static int countNewLines(@Nonnull CharSequence text) {
    return countChars(text, '\n');
  }

  @Contract(pure = true)
  public static int countChars(@Nonnull CharSequence text, char c) {
    return countChars(text, c, 0, false);
  }

  @Contract(pure = true)
  public static int countChars(@Nonnull CharSequence text, char c, int offset, boolean stopAtOtherChar) {
    return countChars(text, c, offset, text.length(), stopAtOtherChar);
  }

  @Contract(pure = true)
  public static int countChars(@Nonnull CharSequence text, char c, int start, int end, boolean stopAtOtherChar) {
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

  public static void repeatSymbol(@Nonnull Appendable buffer, char symbol, int times) {
    assert times >= 0 : times;
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
  public static int indexOfAny(@Nonnull final String s, @Nonnull final String chars) {
    return indexOfAny(s, chars, 0, s.length());
  }

  @Contract(pure = true)
  public static int indexOfAny(@Nonnull final CharSequence s, @Nonnull final String chars) {
    return indexOfAny(s, chars, 0, s.length());
  }

  @Contract(pure = true)
  public static int indexOfAny(@Nonnull final String s, @Nonnull final String chars, final int start, final int end) {
    return indexOfAny((CharSequence)s, chars, start, end);
  }

  @Contract(pure = true)
  public static int indexOfAny(@Nonnull final CharSequence s, @Nonnull final String chars, final int start, final int end) {
    for (int i = start; i < end; i++) {
      if (containsChar(chars, s.charAt(i))) return i;
    }
    return -1;
  }

  @Contract(pure = true)
  public static boolean containsChar(@Nonnull final String value, final char ch) {
    return value.indexOf(ch) >= 0;
  }

  @Nonnull
  @Contract(pure = true)
  public static String repeatSymbol(final char aChar, final int count) {
    char[] buffer = new char[count];
    Arrays.fill(buffer, aChar);
    return new String(buffer);
  }

  @Nonnull
  @Contract(pure = true)
  public static String repeat(@Nonnull String s, int count) {
    assert count >= 0 : count;
    StringBuilder sb = new StringBuilder(s.length() * count);
    for (int i = 0; i < count; i++) {
      sb.append(s);
    }
    return sb.toString();
  }

  @Contract(pure = true)
  public static boolean isDecimalDigit(char c) {
    return c >= '0' && c <= '9';
  }

  @Contract(pure = true)
  public static int compare(@Nullable String s1, @Nullable String s2, boolean ignoreCase) {
    //noinspection StringEquality
    if (s1 == s2) return 0;
    if (s1 == null) return -1;
    if (s2 == null) return 1;
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
    if (s1 == s2) return 0;
    if (s1 == null) return -1;
    if (s2 == null) return 1;

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

  /**
   * @return {@code text} with some characters replaced with standard XML entities, e.g. '<' replaced with '{@code &lt;}'
   */
  @Nonnull
  @Contract(pure = true)
  public static String escapeXmlEntities(@Nonnull String text) {
    return replace(text, REPLACES_DISP, REPLACES_REFS);
  }

  private static boolean isQuoteAt(@Nonnull String s, int ind) {
    char ch = s.charAt(ind);
    return ch == '\'' || ch == '\"';
  }

  @Contract(pure = true)
  public static boolean isQuotedString(@Nonnull String s) {
    return s.length() > 1 && isQuoteAt(s, 0) && s.charAt(0) == s.charAt(s.length() - 1);
  }

  @Nonnull
  @Contract(pure = true)
  public static String unquoteString(@Nonnull String s) {
    if (isQuotedString(s)) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  @Nonnull
  @Contract(pure = true)
  public static String unquoteString(@Nonnull String s, char quotationChar) {
    if (s.length() > 1 && quotationChar == s.charAt(0) && quotationChar == s.charAt(s.length() - 1)) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String escapeMnemonics(@Nullable String text) {
    if (text == null) return null;
    return replace(text, MN_CHARS, MN_QUOTED);
  }

  @Contract(pure = true)
  public static boolean containsIgnoreCase(@Nonnull String where, @Nonnull String what) {
    return indexOfIgnoreCase(where, what, 0) >= 0;
  }

  @Nonnull
  @Contract(pure = true)
  public static String first(@Nonnull String text, final int maxLength, final boolean appendEllipsis) {
    return text.length() > maxLength ? text.substring(0, maxLength) + (appendEllipsis ? "..." : "") : text;
  }

  @Nonnull
  @Contract(pure = true)
  public static CharSequence first(@Nonnull CharSequence text, final int length, final boolean appendEllipsis) {
    return text.length() > length ? text.subSequence(0, length) + (appendEllipsis ? "..." : "") : text;
  }

  @Nonnull
  @Contract(pure = true)
  public static CharSequence last(@Nonnull CharSequence text, final int length, boolean prependEllipsis) {
    return text.length() > length ? (prependEllipsis ? "..." : "") + text.subSequence(text.length() - length, text.length()) : text;
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimEnd(@Nonnull String s, @NonNls @Nonnull String suffix) {
    return trimEnd(s, suffix, false);
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimEnd(@Nonnull String s, @NonNls @Nonnull String suffix, boolean ignoreCase) {
    boolean endsWith = ignoreCase ? endsWithIgnoreCase(s, suffix) : s.endsWith(suffix);
    if (endsWith) {
      return s.substring(0, s.length() - suffix.length());
    }
    return s;
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

  @Nonnull
  @Contract(pure = true)
  public static Iterable<String> tokenize(@Nonnull String s, @Nonnull String separators) {
    final consulo.util.lang.text.StringTokenizer tokenizer = new consulo.util.lang.text.StringTokenizer(s, separators);
    return () -> tokenizer;
  }

  @Nonnull
  @Contract(pure = true)
  public static Iterable<String> tokenize(@Nonnull final StringTokenizer tokenizer) {
    return new Iterable<>() {
      @Nonnull
      @Override
      public Iterator<String> iterator() {
        return new Iterator<>() {
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
    };
  }

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

  @Contract(pure = true)
  public static boolean isHexDigit(char c) {
    return '0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F';
  }

  @Nonnull
  @Contract(pure = true)
  public static String capitalize(@Nonnull String s) {
    if (s.isEmpty()) return s;
    if (s.length() == 1) return toUpperCase(s).toString();

    // Optimization
    if (Character.isUpperCase(s.charAt(0))) return s;
    return toUpperCase(s.charAt(0)) + s.substring(1);
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
  public static <T> String join(@Nonnull T[] items, @Nonnull java.util.function.Function<T, String> f, @Nonnull @NonNls String separator) {
    return join(Arrays.asList(items), f, separator);
  }

  public static <T> void join(@Nonnull Iterable<? extends T> items, @Nonnull java.util.function.Function<? super T, String> f, @Nonnull String separator, @Nonnull StringBuilder result) {
    boolean isFirst = true;
    for (T item : items) {
      String string = f.apply(item);
      if (!isEmpty(string)) {
        if (isFirst) {
          isFirst = false;
        }
        else {
          result.append(separator);
        }
        result.append(string);
      }
    }
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

  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String toLowerCase(@Nullable final String str) {
    //noinspection ConstantConditions
    return str == null ? null : str.toLowerCase(Locale.US);
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

  @Nonnull
  @Contract(pure = true)
  public static String trimExtension(@Nonnull String name) {
    int index = name.lastIndexOf('.');
    return index < 0 ? name : name.substring(0, index);
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

  @Nonnull
  @Contract(pure = true)
  public static String replace(@Nonnull String text, @Nonnull String oldS, @Nonnull String newS) {
    return replace(text, oldS, newS, false);
  }

  @Nonnull
  @Contract(pure = true)
  public static String replace(@Nonnull String text, @Nonnull String[] from, @Nonnull String[] to) {
    return replace(text, Arrays.asList(from), Arrays.asList(to));
  }

  @Nonnull
  @Contract(pure = true)
  public static String replace(@Nonnull String text, @Nonnull List<String> from, @Nonnull List<String> to) {
    assert from.size() == to.size();
    final StringBuilder result = new StringBuilder(text.length());
    replace:
    for (int i = 0; i < text.length(); i++) {
      for (int j = 0; j < from.size(); j += 1) {
        String toReplace = from.get(j);
        String replaceWith = to.get(j);

        final int len = toReplace.length();
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

  @Contract(value = "null -> null; !null->!null", pure = true)
  public static String internEmptyString(String s) {
    return s == null ? null : s.isEmpty() ? "" : s;
  }

  @Nonnull
  @Contract(pure = true)
  public static String replaceIgnoreCase(@Nonnull String text, @Nonnull String oldS, @Nonnull String newS) {
    return replace(text, oldS, newS, true);
  }

  @Contract(pure = true)
  public static String replace(@Nonnull final String text, @Nonnull final String oldS, @Nonnull final String newS, final boolean ignoreCase) {
    if (text.length() < oldS.length()) return text;

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

  @Contract(pure = true)
  public static int indexOfIgnoreCase(@Nonnull String where, @Nonnull String what, int fromIndex) {
    return indexOfIgnoreCase((CharSequence)where, what, fromIndex);
  }

  /**
   * Implementation copied from {@link String#indexOf(String, int)} except character comparisons made case insensitive
   */
  @Contract(pure = true)
  public static int indexOfIgnoreCase(@Nonnull CharSequence where, @Nonnull CharSequence what, int fromIndex) {
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
        //noinspection StatementWithEmptyBody,AssignmentToForLoopParameter
        while (++i <= max && !charsEqualIgnoreCase(where.charAt(i), first)) ;
      }

      /* Found first character, now look at the rest of v2 */
      if (i <= max) {
        int j = i + 1;
        int end = j + targetCount - 1;
        //noinspection StatementWithEmptyBody
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
  public static String trimLeading(@Nonnull String string) {
    return trimLeading((CharSequence)string).toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static CharSequence trimLeading(@Nonnull CharSequence string) {
    int index = 0;
    while (index < string.length() && Character.isWhitespace(string.charAt(index))) index++;
    return string.subSequence(index, string.length());
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimLeading(@Nonnull String string, char symbol) {
    int index = 0;
    while (index < string.length() && string.charAt(index) == symbol) index++;
    return string.substring(index);
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimTrailing(@Nonnull String string) {
    return trimTrailing((CharSequence)string).toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static CharSequence trimTrailing(@Nonnull CharSequence string) {
    int index = string.length() - 1;
    while (index >= 0 && Character.isWhitespace(string.charAt(index))) index--;
    return string.subSequence(0, index + 1);
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimTrailing(@Nonnull String string, char symbol) {
    int index = string.length() - 1;
    while (index >= 0 && string.charAt(index) == symbol) index--;
    return string.substring(0, index + 1);
  }

  @Nonnull
  @Contract(pure = true)
  public static CharSequence trimTrailing(@Nonnull CharSequence string, char symbol) {
    int index = string.length() - 1;
    while (index >= 0 && string.charAt(index) == symbol) index--;
    return string.subSequence(0, index + 1);
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
  public static List<String> split(@Nonnull String s, @Nonnull String separator) {
    return split(s, separator, true);
  }

  @Nonnull
  @Contract(pure = true)
  public static List<CharSequence> split(@Nonnull CharSequence s, @Nonnull CharSequence separator) {
    return split(s, separator, true, true);
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> split(@Nonnull String s, @Nonnull String separator, boolean excludeSeparator) {
    return split(s, separator, excludeSeparator, true);
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> split(@Nonnull String s, @Nonnull String separator, boolean excludeSeparator, boolean excludeEmptyStrings) {
    return (List)split((CharSequence)s, separator, excludeSeparator, excludeEmptyStrings);
  }

  @Nonnull
  @Contract(pure = true)
  public static List<CharSequence> split(@Nonnull CharSequence s, @Nonnull CharSequence separator, boolean excludeSeparator, boolean excludeEmptyStrings) {
    if (separator.length() == 0) {
      return List.of();
    }
    List<CharSequence> result = new ArrayList<>();
    int pos = 0;
    while (true) {
      int index = indexOf(s, separator, pos);
      if (index == -1) break;
      final int nextPos = index + separator.length();
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
  public static int indexOf(@Nonnull CharSequence sequence, @Nonnull CharSequence infix, int start) {
    for (int i = start; i <= sequence.length() - infix.length(); i++) {
      if (startsWith(sequence, i, infix)) {
        return i;
      }
    }
    return -1;
  }

  @Contract(pure = true)
  public static boolean startsWith(@Nonnull CharSequence text, int startIndex, @Nonnull CharSequence prefix) {
    int l1 = text.length() - startIndex;
    int l2 = prefix.length();
    if (l1 < l2) return false;

    for (int i = 0; i < l2; i++) {
      if (text.charAt(i + startIndex) != prefix.charAt(i)) return false;
    }

    return true;
  }

  /**
   * @return a lightweight CharSequence which results from replacing {@code [start, end)} range in the {@code charSeq} with {@code replacement}.
   * Works in O(1), but retains references to the passed char sequences, so please use something else if you want them to be garbage-collected.
   */
  @Nonnull
  public static MergingCharSequence replaceSubSequence(@Nonnull CharSequence charSeq, int start, int end, @Nonnull CharSequence replacement) {
    return new MergingCharSequence(new MergingCharSequence(new CharSequenceSubSequence(charSeq, 0, start), replacement), new CharSequenceSubSequence(charSeq, end, charSeq.length()));
  }

  public static void assertValidSeparators(@Nonnull CharSequence s) {
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
      String context = String.valueOf(last(s.subSequence(0, slashRIndex), 10, true)) + first(s.subSequence(slashRIndex, s.length()), 10, true);
      context = escapeStringCharacters(context);
      LOG.error("Wrong line separators: '" + context + "' at offset " + slashRIndex);
    }
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapeStringCharacters(@Nonnull String s) {
    StringBuilder buffer = new StringBuilder(s.length());
    escapeStringCharacters(s.length(), s, "\"", buffer);
    return buffer.toString();
  }

  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String unescapeXml(@Nullable final String text) {
    if (text == null) return null;
    return replace(text, REPLACES_REFS, REPLACES_DISP);
  }

  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String escapeXml(@Nullable final String text) {
    if (text == null) return null;
    return replace(text, REPLACES_DISP, REPLACES_REFS);
  }

  @Contract(pure = true)
  public static int getLineBreakCount(@Nonnull CharSequence text) {
    int count = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\n') {
        count++;
      }
      else if (c == '\r') {
        if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
          //noinspection AssignmentToForLoopParameter
          i++;
          count++;
        }
        else {
          count++;
        }
      }
    }
    return count;
  }

  @Nonnull
  @Contract(pure = true)
  public static String commonSuffix(@Nonnull String s1, @Nonnull String s2) {
    return s1.substring(s1.length() - commonSuffixLength(s1, s2));
  }

  @Contract(pure = true)
  public static int commonSuffixLength(@Nonnull CharSequence s1, @Nonnull CharSequence s2) {
    int s1Length = s1.length();
    int s2Length = s2.length();
    if (s1Length == 0 || s2Length == 0) return 0;
    int i;
    for (i = 0; i < s1Length && i < s2Length; i++) {
      if (s1.charAt(s1Length - i - 1) != s2.charAt(s2Length - i - 1)) {
        break;
      }
    }
    return i;
  }

  @Contract(pure = true)
  public static boolean startsWith(@Nonnull CharSequence text, @Nonnull CharSequence prefix) {
    int l1 = text.length();
    int l2 = prefix.length();
    if (l1 < l2) return false;

    for (int i = 0; i < l2; i++) {
      if (text.charAt(i) != prefix.charAt(i)) return false;
    }

    return true;
  }

  @Nonnull
  @Contract(pure = true)
  public static String commonPrefix(@Nonnull String s1, @Nonnull String s2) {
    return s1.substring(0, commonPrefixLength(s1, s2));
  }

  @Contract(pure = true)
  public static int commonPrefixLength(@Nonnull CharSequence s1, @Nonnull CharSequence s2) {
    int i;
    int minLength = Math.min(s1.length(), s2.length());
    for (i = 0; i < minLength; i++) {
      if (s1.charAt(i) != s2.charAt(i)) {
        break;
      }
    }
    return i;
  }
}
