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

  private static final String VOWELS = "aeiouy";

  public static final Function<String, String> QUOTER = s -> "\"" + s + "\"";

  public static final Function<String, String> SINGLE_QUOTER = s -> "'" + s + "'";

  public static boolean isAscii(@Nonnull String str) {
    return isAscii((CharSequence)str);
  }

  @Nonnull
  @Contract(pure = true)
  public static String htmlEmphasize(@Nonnull String text) {
    return "<b><code>" + escapeXml(text) + "</code></b>";
  }

  public static boolean isAscii(@Nonnull CharSequence str) {
    for (int i = 0, length = str.length(); i < length; ++i) {
      if (str.charAt(i) >= 128) return false;
    }
    return true;
  }

  public static boolean isAscii(char c) {
    return c < 128;
  }

  @Nonnull
  @Contract(pure = true)
  public static String replaceChar(@Nonnull String buffer, char oldChar, char newChar) {
    StringBuilder newBuffer = null;
    for (int i = 0; i < buffer.length(); i++) {
      char c = buffer.charAt(i);
      if (c == oldChar) {
        if (newBuffer == null) {
          newBuffer = new StringBuilder(buffer.length());
          newBuffer.append(buffer, 0, i);
        }

        newBuffer.append(newChar);
      }
      else if (newBuffer != null) {
        newBuffer.append(c);
      }
    }
    return newBuffer == null ? buffer : newBuffer.toString();
  }

  @Contract(pure = true)
  public static String defaultIfEmpty(@Nullable String value, String defaultValue) {
    return isEmpty(value) ? defaultValue : value;
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapePattern(@Nonnull final String text) {
    return replace(replace(text, "'", "''"), "{", "'{'");
  }

  @Nonnull
  @Contract(pure = true)
  public static String capitalizeWords(@Nonnull String text, boolean allWords) {
    return capitalizeWords(text, " \t\n\r\f", allWords, false);
  }

  @Nonnull
  @Contract(pure = true)
  public static String capitalizeWords(@Nonnull String text, @Nonnull String tokenizerDelim, boolean allWords, boolean leaveOriginalDelims) {
    final StringTokenizer tokenizer = new StringTokenizer(text, tokenizerDelim, leaveOriginalDelims);
    final StringBuilder out = new StringBuilder(text.length());
    boolean toCapitalize = true;
    while (tokenizer.hasMoreTokens()) {
      final String word = tokenizer.nextToken();
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

  @Nonnull
  @Contract(pure = true)
  public static String firstLast(@Nonnull String text, int length) {
    return text.length() > length ? text.subSequence(0, length / 2) + "\u2026" + text.subSequence(text.length() - length / 2 - 1, text.length()) : text;
  }

  @Contract(pure = true)
  public static boolean containsAnyChar(@Nonnull final String value, @Nonnull final String chars) {
    if (chars.length() > value.length()) {
      return containsAnyChar(value, chars, 0, value.length());
    }
    else {
      return containsAnyChar(chars, value, 0, chars.length());
    }
  }

  @Contract(pure = true)
  public static boolean containsAnyChar(@Nonnull final String value, @Nonnull final String chars, final int start, final int end) {
    for (int i = start; i < end; i++) {
      if (chars.indexOf(value.charAt(i)) >= 0) {
        return true;
      }
    }

    return false;
  }

  @Contract(pure = true)
  public static int indexOfIgnoreCase(@Nonnull String where, char what, int fromIndex) {
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

  @Nonnull
  @Contract(pure = true)
  public static String pluralize(@Nonnull String base, int n) {
    if (n == 1) return base;
    return pluralize(base);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @Nonnull
  @Contract(pure = true)
  public static String pluralize(@Nonnull String suggestion) {
    if (suggestion.endsWith("Child") || suggestion.endsWith("child")) {
      return suggestion + "ren";
    }

    if (suggestion.equals("this")) {
      return "these";
    }
    if (suggestion.equals("This")) {
      return "These";
    }

    if (endsWithIgnoreCase(suggestion, "es")) {
      return suggestion;
    }

    if (endsWithIgnoreCase(suggestion, "s") || endsWithIgnoreCase(suggestion, "x") || endsWithIgnoreCase(suggestion, "ch")) {
      return suggestion + "es";
    }

    int len = suggestion.length();
    if (endsWithIgnoreCase(suggestion, "y") && len > 1 && !isVowel(toLowerCase(suggestion.charAt(len - 2)))) {
      return suggestion.substring(0, len - 1) + "ies";
    }

    return suggestion + "s";
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
  public static String unpluralize(@Nonnull final String name) {
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
  private static String stripEnding(@Nonnull String name, @Nonnull String ending) {
    if (name.endsWith(ending)) {
      if (name.equals(ending)) return name; // do not return empty string
      return name.substring(0, name.length() - 1);
    }
    return null;
  }

  @Contract(pure = true)
  public static boolean isVowel(char c) {
    return VOWELS.indexOf(c) >= 0;
  }

  @Nonnull
  @Contract(pure = true)
  public static String getPackageName(@Nonnull String fqName) {
    return getPackageName(fqName, '.');
  }

  /**
   * Given a fqName returns the package name for the type or the containing type.
   * <p/>
   * <ul>
   * <li><code>java.lang.String</code> -> <code>java.lang</code></li>
   * <li><code>java.util.Map.Entry</code> -> <code>java.util.Map</code></li>
   * </ul>
   *
   * @param fqName    a fully qualified type name. Not supposed to contain any type arguments
   * @param separator the separator to use. Typically '.'
   * @return the package name of the type or the declarator of the type. The empty string if the given fqName is unqualified
   */
  @Nonnull
  @Contract(pure = true)
  public static String getPackageName(@Nonnull String fqName, char separator) {
    int lastPointIdx = fqName.lastIndexOf(separator);
    if (lastPointIdx >= 0) {
      return fqName.substring(0, lastPointIdx);
    }
    return "";
  }

  @Contract(pure = true)
  public static boolean equalsIgnoreCase(@Nullable CharSequence s1, @Nullable CharSequence s2) {
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

  @Nonnull
  @Contract(pure = true)
  public static String replaceSubstring(@Nonnull String original, int startOffset, int endOffset, @Nonnull String replacement) {
    try {
      String beginning = original.substring(0, startOffset);
      String ending = original.substring(endOffset, original.length());
      return beginning + replacement + ending;
    }
    catch (StringIndexOutOfBoundsException e) {
      throw new StringIndexOutOfBoundsException("Can't replace " + startOffset + ":" + endOffset + " range from '" + original + "' with '" + replacement + "'");
    }
  }

  @Contract(pure = true)
  public static boolean isChar(CharSequence seq, int index, char c) {
    return index >= 0 && index < seq.length() && seq.charAt(index) == c;
  }

  @Contract(pure = true)
  public static int hashCode(@Nonnull CharSequence s) {
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
  public static boolean contains(@Nonnull CharSequence s, int start, int end, char c) {
    return indexOf(s, c, start, end) >= 0;
  }

  @Contract(pure = true)
  public static boolean contains(@Nonnull CharSequence sequence, @Nonnull CharSequence infix) {
    return indexOf(sequence, infix) >= 0;
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull CharSequence sequence, @Nonnull CharSequence infix) {
    return indexOf(sequence, infix, 0);
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
  public static double parseDouble(final String string, final double defaultValue) {
    try {
      return Double.parseDouble(string);
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

  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String trim(@Nullable String s) {
    return s == null ? null : s.trim();
  }

  @Nonnull
  @Contract(pure = true)
  public static String wrapWithDoubleQuote(@Nonnull String str) {
    return '\"' + str + "\"";
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimEnd(@Nonnull String s, @NonNls @Nonnull String suffix) {
    return trimEnd(s, suffix, false);
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimEnd(@Nonnull String s, char suffix) {
    if (endsWithChar(s, suffix)) {
      return s.substring(0, s.length() - 1);
    }
    return s;
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

  @Contract(value = "null -> false", pure = true)
  public static boolean isCapitalized(@Nullable String s) {
    return s != null && !s.isEmpty() && Character.isUpperCase(s.charAt(0));
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
  public static String toUpperCase(@Nonnull String s) {
    return toUpperCase((CharSequence)s).toString();
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
  public static int indexOf(@Nonnull CharSequence s, char c, int start, int end, boolean caseSensitive) {
    for (int i = start; i < end; i++) {
      if (charsMatch(s.charAt(i), c, !caseSensitive)) return i;
    }
    return -1;
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull char[] s, char c, int start, int end, boolean caseSensitive) {
    for (int i = start; i < end; i++) {
      if (charsMatch(s[i], c, !caseSensitive)) return i;
    }
    return -1;
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

  @Nonnull
  @Contract(pure = true)
  public static String formatDuration(long duration) {
    return formatDuration(duration, null);
  }

  @Nonnull
  @Contract(pure = true)
  public static String formatDuration(long duration, final String spaceBeforeUnits) {
    return formatValue(duration, " ", new String[]{"ms", "s", "m", "h", "d", "w", "mo", "yr", "c", "ml", "ep"}, new long[]{1000, 60, 60, 24, 7, 4, 12, 100, 10, 10000}, spaceBeforeUnits);
  }

  @Nonnull
  private static String formatValue(long value, String partSeparator, String[] units, long[] multipliers, final String spaceBeforeUnits) {
    StringBuilder sb = new StringBuilder();
    long count = value;
    long remainder = 0;
    int i = 0;
    for (; i < units.length; i++) {
      long multiplier = i < multipliers.length ? multipliers[i] : -1;
      if (multiplier == -1 || count < multiplier) break;
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
      sb.append(String.format(Locale.US, "%.2f", count + (double)remainder / multipliers[i - 1]));
      if (spaceBeforeUnits != null) {
        sb.append(spaceBeforeUnits);
      }
      sb.append(units[i]);
    }
    return sb.toString();
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

  /**
   * Consider using {@link StringUtil#unquoteString(String)} instead.
   * Note: this method has an odd behavior:
   * Quotes are removed even if leading and trailing quotes are different or
   * if there is only one quote (leading or trailing).
   */
  @Nonnull
  @Contract(pure = true)
  public static String stripQuotesAroundValue(@Nonnull String text) {
    final int len = text.length();
    if (len > 0) {
      final int from = isQuoteAt(text, 0) ? 1 : 0;
      final int to = len > 1 && isQuoteAt(text, len - 1) ? len - 1 : len;
      if (from > 0 || to < len) {
        return text.substring(from, to);
      }
    }
    return text;
  }

  @Nonnull
  @Contract(pure = true)
  public static String[] filterEmptyStrings(@Nonnull String[] strings) {
    int emptyCount = 0;
    for (String string : strings) {
      if (string == null || string.isEmpty()) emptyCount++;
    }
    if (emptyCount == 0) return strings;

    String[] result = new String[strings.length - emptyCount];
    int count = 0;
    for (String string : strings) {
      if (string == null || string.isEmpty()) continue;
      result[count++] = string;
    }

    return result;
  }

  @Contract(pure = true)
  public static boolean isLineBreak(char c) {
    return c == '\n' || c == '\r';
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapeLineBreak(@Nonnull String text) {
    StringBuilder buffer = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '\n':
          buffer.append("\\n");
          break;
        case '\r':
          buffer.append("\\r");
          break;
        default:
          buffer.append(c);
      }
    }
    return buffer.toString();
  }


  /**
   * Formats the specified file size as a string.
   *
   * @param fileSize the size to format.
   * @return the size formatted as a string.
   * @since 5.0.1
   */
  @Nonnull
  @Contract(pure = true)
  public static String formatFileSize(long fileSize) {
    return formatFileSize(fileSize, null);
  }

  /**
   * Formats the specified file size as a string.
   *
   * @param fileSize         the size to format.
   * @param spaceBeforeUnits space to be used between counts and measurement units
   * @return the size formatted as a string.
   * @since 5.0.1
   */
  @Nonnull
  @Contract(pure = true)
  public static String formatFileSize(long fileSize, final String spaceBeforeUnits) {
    return formatValue(fileSize, null, new String[]{"B", "K", "M", "G", "T", "P", "E"}, new long[]{1000, 1000, 1000, 1000, 1000, 1000}, spaceBeforeUnits);
  }

  /**
   * Find position of the first character accepted by given filter.
   *
   * @param s      the string to search
   * @param filter search filter
   * @return position of the first character accepted or -1 if not found
   */
  @Contract(pure = true)
  public static int findFirst(@Nonnull final CharSequence s, @Nonnull CharFilter filter) {
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if (filter.accept(ch)) {
        return i;
      }
    }
    return -1;
  }

  @NonNls
  private static final String[] ourPrepositions =
          {"a", "an", "and", "as", "at", "but", "by", "down", "for", "from", "if", "in", "into", "not", "of", "on", "onto", "or", "out", "over", "per", "nor", "the", "to", "up", "upon", "via",
                  "with"};

  @Contract(pure = true)
  public static boolean isPreposition(@Nonnull String s, int firstChar, int lastChar) {
    return isPreposition(s, firstChar, lastChar, ourPrepositions);
  }

  @Contract(pure = true)
  public static boolean isPreposition(@Nonnull String s, int firstChar, int lastChar, @Nonnull String[] prepositions) {
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

  @Nonnull
  @Contract(pure = true)
  public static String wordsToBeginFromUpperCase(@Nonnull String s) {
    return fixCapitalization(s, ourPrepositions, true);
  }

  @Nonnull
  @Contract(pure = true)
  public static String wordsToBeginFromLowerCase(@Nonnull String s) {
    return fixCapitalization(s, ourPrepositions, false);
  }

  @Nonnull
  @Contract(pure = true)
  public static String toTitleCase(@Nonnull String s) {
    return fixCapitalization(s, new String[0], true);
  }

  @Nonnull
  private static String fixCapitalization(@Nonnull String s, @Nonnull String[] prepositions, boolean title) {
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
  public static String substringAfter(@Nonnull String text, @Nonnull String subString) {
    int i = text.indexOf(subString);
    if (i == -1) return null;
    return text.substring(i + subString.length());
  }

  @Contract(pure = true)
  @Nullable
  public static String substringAfterLast(@Nonnull String text, @Nonnull String subString) {
    int i = text.lastIndexOf(subString);
    if (i == -1) return null;
    return text.substring(i + subString.length());
  }

  /**
   * @return list containing all words in {@code text}, or {@link ContainerUtil#emptyList()} if there are none.
   * The <b>word</b> here means the maximum sub-string consisting entirely of characters which are <code>Character.isJavaIdentifierPart(c)</code>.
   */
  @Nonnull
  @Contract(pure = true)
  public static List<String> getWordsIn(@Nonnull String text) {
    List<String> result = null;
    int start = -1;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      boolean isIdentifierPart = Character.isJavaIdentifierPart(c);
      if (isIdentifierPart && start == -1) {
        start = i;
      }
      if (isIdentifierPart && i == text.length() - 1 && start != -1) {
        if (result == null) {
          result = new ArrayList<>();
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
}
