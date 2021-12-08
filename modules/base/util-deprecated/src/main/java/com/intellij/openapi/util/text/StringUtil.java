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
package com.intellij.openapi.util.text;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.CharSequenceSubSequence;
import com.intellij.util.text.MergingCharSequence;
import com.intellij.util.text.StringFactory;
import consulo.logging.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TeamCity inherits StringUtil: do not add private constructors!!!
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "MethodOverridesStaticMethodOfSuperclass"})
public class StringUtil extends StringUtilRt {
  private static final Logger LOG = Logger.getInstance(StringUtil.class);

  @NonNls
  private static final String VOWELS = "aeiouy";
  @NonNls
  private static final Pattern EOL_SPLIT_KEEP_SEPARATORS = Pattern.compile("(?<=(\r\n|\n))|(?<=\r)(?=[^\n])");
  @NonNls
  private static final Pattern EOL_SPLIT_PATTERN = Pattern.compile(" *(\r|\n|\r\n)+ *");
  @NonNls
  private static final Pattern EOL_SPLIT_PATTERN_WITH_EMPTY = Pattern.compile(" *(\r|\n|\r\n) *");
  @NonNls
  private static final Pattern EOL_SPLIT_DONT_TRIM_PATTERN = Pattern.compile("(\r|\n|\r\n)+");

  public static final NotNullFunction<String, String> QUOTER = new NotNullFunction<String, String>() {
    @Override
    @Nonnull
    public String fun(String s) {
      return "\"" + s + "\"";
    }
  };

  public static final NotNullFunction<String, String> SINGLE_QUOTER = new NotNullFunction<String, String>() {
    @Override
    @Nonnull
    public String fun(String s) {
      return "'" + s + "'";
    }
  };

  /**
   * @return a lightweight CharSequence which results from replacing {@code [start, end)} range in the {@code charSeq} with {@code replacement}.
   * Works in O(1), but retains references to the passed char sequences, so please use something else if you want them to be garbage-collected.
   */
  @Nonnull
  public static MergingCharSequence replaceSubSequence(@Nonnull CharSequence charSeq, int start, int end, @Nonnull CharSequence replacement) {
    return new MergingCharSequence(new MergingCharSequence(new CharSequenceSubSequence(charSeq, 0, start), replacement), new CharSequenceSubSequence(charSeq, end, charSeq.length()));
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> getWordsInStringLongestFirst(@Nonnull String find) {
    List<String> words = getWordsIn(find);
    // hope long words are rare
    Collections.sort(words, new Comparator<String>() {
      @Override
      public int compare(@Nonnull final String o1, @Nonnull final String o2) {
        return o2.length() - o1.length();
      }
    });
    return words;
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapePattern(@Nonnull final String text) {
    return replace(replace(text, "'", "''"), "{", "'{'");
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Function<T, String> createToStringFunction(@Nonnull Class<T> cls) {
    return new Function<T, String>() {
      @Override
      public String fun(@Nonnull T o) {
        return o.toString();
      }
    };
  }

  @Nonnull
  public static Function<String, String> TRIMMER = new Function<String, String>() {
    @Nullable
    @Override
    public String fun(@Nullable String s) {
      return trim(s);
    }
  };

  @Contract(pure = true)
  public static boolean isEscapedBackslash(@Nonnull char[] chars, int startOffset, int backslashOffset) {
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

  @Contract(pure = true)
  public static boolean isEscapedBackslash(@Nonnull CharSequence text, int startOffset, int backslashOffset) {
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

  @Nonnull
  @Contract(pure = true)
  public static String replace(@NonNls @Nonnull String text, @NonNls @Nonnull String oldS, @NonNls @Nonnull String newS) {
    return replace(text, oldS, newS, false);
  }

  @Nonnull
  @Contract(pure = true)
  public static String replaceIgnoreCase(@NonNls @Nonnull String text, @NonNls @Nonnull String oldS, @NonNls @Nonnull String newS) {
    return replace(text, oldS, newS, true);
  }

  public static void replaceChar(@Nonnull char[] buffer, char oldChar, char newChar, int start, int end) {
    for (int i = start; i < end; i++) {
      char c = buffer[i];
      if (c == oldChar) {
        buffer[i] = newChar;
      }
    }
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
  public static String replace(@NonNls @Nonnull final String text, @NonNls @Nonnull final String oldS, @NonNls @Nonnull final String newS, final boolean ignoreCase) {
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

  @Contract(pure = true)
  public static boolean containsIgnoreCase(@Nonnull String where, @Nonnull String what) {
    return indexOfIgnoreCase(where, what, 0) >= 0;
  }

  @Contract(pure = true)
  public static boolean endsWithIgnoreCase(@NonNls @Nonnull String str, @NonNls @Nonnull String suffix) {
    return StringUtilRt.endsWithIgnoreCase(str, suffix);
  }

  @Contract(pure = true)
  public static boolean startsWithIgnoreCase(@NonNls @Nonnull String str, @NonNls @Nonnull String prefix) {
    return StringUtilRt.startsWithIgnoreCase(str, prefix);
  }

  @Contract(pure = true)
  @Nonnull
  public static String stripHtml(@Nonnull String html, boolean convertBreaks) {
    if (convertBreaks) {
      html = html.replaceAll("<br/?>", "\n\n");
    }

    return html.replaceAll("<(.|\n)*?>", "");
  }

  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String toLowerCase(@Nullable final String str) {
    //noinspection ConstantConditions
    return str == null ? null : str.toLowerCase(Locale.US);
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

  @Contract(pure = true)
  public static boolean containsLineBreak(@Nonnull CharSequence text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (isLineBreak(c)) return true;
    }
    return false;
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

  @Contract(pure = true)
  public static boolean endsWithLineBreak(@Nonnull CharSequence text) {
    int len = text.length();
    return len > 0 && isLineBreak(text.charAt(len - 1));
  }

  @Contract(pure = true)
  public static int lineColToOffset(@Nonnull CharSequence text, int line, int col) {
    int curLine = 0;
    int offset = 0;
    while (line != curLine) {
      if (offset == text.length()) return -1;
      char c = text.charAt(offset);
      if (c == '\n') {
        curLine++;
      }
      else if (c == '\r') {
        curLine++;
        if (offset < text.length() - 1 && text.charAt(offset + 1) == '\n') {
          offset++;
        }
      }
      offset++;
    }
    return offset + col;
  }

  @Contract(pure = true)
  public static int offsetToLineNumber(@Nonnull CharSequence text, int offset) {
    int curLine = 0;
    int curOffset = 0;
    while (curOffset < offset) {
      if (curOffset == text.length()) return -1;
      char c = text.charAt(curOffset);
      if (c == '\n') {
        curLine++;
      }
      else if (c == '\r') {
        curLine++;
        if (curOffset < text.length() - 1 && text.charAt(curOffset + 1) == '\n') {
          curOffset++;
        }
      }
      curOffset++;
    }
    return curLine;
  }

  /**
   * Classic dynamic programming algorithm for string differences.
   */
  @Contract(pure = true)
  public static int difference(@Nonnull String s1, @Nonnull String s2) {
    int[][] a = new int[s1.length()][s2.length()];

    for (int i = 0; i < s1.length(); i++) {
      a[i][0] = i;
    }

    for (int j = 0; j < s2.length(); j++) {
      a[0][j] = j;
    }

    for (int i = 1; i < s1.length(); i++) {
      for (int j = 1; j < s2.length(); j++) {

        a[i][j] = Math.min(Math.min(a[i - 1][j - 1] + (s1.charAt(i) == s2.charAt(j) ? 0 : 1), a[i - 1][j] + 1), a[i][j - 1] + 1);
      }
    }

    return a[s1.length() - 1][s2.length() - 1];
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
    return fixCapitalization(s, ArrayUtil.EMPTY_STRING_ARRAY, true);
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
  public static NotNullFunction<String, String> escaper(final boolean escapeSlash, @Nullable final String additionalChars) {
    return new NotNullFunction<String, String>() {
      @Nonnull
      @Override
      public String fun(@Nonnull String dom) {
        final StringBuilder builder = new StringBuilder(dom.length());
        escapeStringCharacters(dom.length(), dom, additionalChars, escapeSlash, builder);
        return builder.toString();
      }
    };
  }


  public static void escapeStringCharacters(int length, @Nonnull String str, @Nonnull @NonNls StringBuilder buffer) {
    escapeStringCharacters(length, str, "\"", buffer);
  }

  @Nonnull
  public static StringBuilder escapeStringCharacters(int length, @Nonnull String str, @Nullable String additionalChars, @Nonnull @NonNls StringBuilder buffer) {
    return escapeStringCharacters(length, str, additionalChars, true, buffer);
  }

  @Nonnull
  public static StringBuilder escapeStringCharacters(int length, @Nonnull String str, @Nullable String additionalChars, boolean escapeSlash, @Nonnull @NonNls StringBuilder buffer) {
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
            CharSequence hexCode = StringUtilRt.toUpperCase(Integer.toHexString(ch));
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

  @Nonnull
  @Contract(pure = true)
  public static String escapeStringCharacters(@Nonnull String s) {
    StringBuilder buffer = new StringBuilder(s.length());
    escapeStringCharacters(s.length(), s, "\"", buffer);
    return buffer.toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapeCharCharacters(@Nonnull String s) {
    StringBuilder buffer = new StringBuilder(s.length());
    escapeStringCharacters(s.length(), s, "\'", buffer);
    return buffer.toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static String unescapeStringCharacters(@Nonnull String s) {
    StringBuilder buffer = new StringBuilder(s.length());
    unescapeStringCharacters(s.length(), s, buffer);
    return buffer.toString();
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

  /**
   * This is just an optimized version of Matcher.quoteReplacement
   */
  @Nonnull
  @Contract(pure = true)
  public static String quoteReplacement(@Nonnull String s) {
    boolean needReplacements = false;

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\' || c == '$') {
        needReplacements = true;
        break;
      }
    }

    if (!needReplacements) return s;

    StringBuilder sb = new StringBuilder(s.length() * 6 / 5);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\') {
        sb.append('\\');
        sb.append('\\');
      }
      else if (c == '$') {
        sb.append('\\');
        sb.append('$');
      }
      else {
        sb.append(c);
      }
    }
    return sb.toString();
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
  public static boolean isVowel(char c) {
    return VOWELS.indexOf(c) >= 0;
  }

  @Nonnull
  @Contract(pure = true)
  public static String capitalize(@Nonnull String s) {
    if (s.isEmpty()) return s;
    if (s.length() == 1) return StringUtilRt.toUpperCase(s).toString();

    // Optimization
    if (Character.isUpperCase(s.charAt(0))) return s;
    return toUpperCase(s.charAt(0)) + s.substring(1);
  }

  @Contract(value = "null -> false", pure = true)
  public static boolean isCapitalized(@Nullable String s) {
    return s != null && !s.isEmpty() && Character.isUpperCase(s.charAt(0));
  }

  @Nonnull
  @Contract(pure = true)
  public static String capitalizeWithJavaBeanConvention(@Nonnull String s) {
    if (s.length() > 1 && Character.isUpperCase(s.charAt(1))) {
      return s;
    }
    return capitalize(s);
  }

  @Contract(pure = true)
  public static int stringHashCode(@Nonnull CharSequence chars) {
    if (chars instanceof String || chars instanceof CharSequenceWithStringHash) {
      // we know for sure these classes have conformant (and maybe faster) hashCode()
      return chars.hashCode();
    }

    return stringHashCode(chars, 0, chars.length());
  }

  @Contract(pure = true)
  public static int stringHashCode(@Nonnull CharSequence chars, int from, int to) {
    return stringHashCode(chars, from, to, 0);
  }

  @Contract(pure = true)
  public static int stringHashCode(@Nonnull CharSequence chars, int from, int to, int prefixHash) {
    int h = prefixHash;
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
  public static int stringHashCodeInsensitive(@Nonnull CharSequence chars, int from, int to, int prefixHash) {
    int h = prefixHash;
    for (int off = from; off < to; off++) {
      h = 31 * h + toLowerCase(chars.charAt(off));
    }
    return h;
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

  @Contract(pure = true)
  public static int stringHashCodeIgnoreWhitespaces(@Nonnull CharSequence chars) {
    return stringHashCodeIgnoreWhitespaces(chars, 0, chars.length());
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

  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String trim(@Nullable String s) {
    return s == null ? null : s.trim();
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
  public static String trimLog(@Nonnull final String text, final int limit) {
    if (limit > 5 && text.length() > limit) {
      return text.substring(0, limit - 5) + " ...\n";
    }
    return text;
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

  @Contract(pure = true)
  public static boolean startsWithChar(@Nullable CharSequence s, char prefix) {
    return StringUtilRt.startsWithChar(s, prefix);
  }

  @Contract(pure = true)
  public static boolean endsWithChar(@Nullable CharSequence s, char suffix) {
    return StringUtilRt.endsWithChar(s, suffix);
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimStart(@Nonnull String s, @NonNls @Nonnull String prefix) {
    return StringUtilRt.trimStart(s, prefix);
  }

  @Nonnull
  @Contract(pure = true)
  public static String trimExtension(@Nonnull String name) {
    int index = name.lastIndexOf('.');
    return index < 0 ? name : name.substring(0, index);
  }

  @Nonnull
  @Contract(pure = true)
  public static String pluralize(@Nonnull String base, int n) {
    if (n == 1) return base;
    return pluralize(base);
  }

  public static void repeatSymbol(@Nonnull Appendable buffer, char symbol, int times) {
    assert times >= 0 : times;
    try {
      for (int i = 0; i < times; i++) {
        buffer.append(symbol);
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Contract(pure = true)
  public static String defaultIfEmpty(@Nullable String value, String defaultValue) {
    return isEmpty(value) ? defaultValue : value;
  }

  @Contract(value = "null -> false", pure = true)
  public static boolean isNotEmpty(@Nullable String s) {
    return s != null && !s.isEmpty();
  }

  @Contract(pure = true)
  public static int length(@Nullable CharSequence cs) {
    return cs == null ? 0 : cs.length();
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

  @Nonnull
  @Contract(pure = true)
  public static String getThrowableText(@Nonnull Throwable aThrowable) {
    return ExceptionUtil.getThrowableText(aThrowable);
  }

  @Nonnull
  @Contract(pure = true)
  public static String getThrowableText(@Nonnull Throwable aThrowable, @NonNls @Nonnull final String stackFrameSkipPattern) {
    return ExceptionUtil.getThrowableText(aThrowable, stackFrameSkipPattern);
  }

  @Nullable
  @Contract(pure = true)
  public static String getMessage(@Nonnull Throwable e) {
    return ExceptionUtil.getMessage(e);
  }

  @Nonnull
  @Contract(pure = true)
  public static String repeatSymbol(final char aChar, final int count) {
    char[] buffer = new char[count];
    Arrays.fill(buffer, aChar);
    return StringFactory.createShared(buffer);
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
      return Collections.singletonList(s);
    }
    List<CharSequence> result = new ArrayList<CharSequence>();
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

  @Nonnull
  @Contract(pure = true)
  public static Iterable<String> tokenize(@Nonnull String s, @Nonnull String separators) {
    final com.intellij.util.text.StringTokenizer tokenizer = new com.intellij.util.text.StringTokenizer(s, separators);
    return new Iterable<String>() {
      @Nonnull
      @Override
      public Iterator<String> iterator() {
        return new Iterator<String>() {
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
  public static Iterable<String> tokenize(@Nonnull final StringTokenizer tokenizer) {
    return new Iterable<String>() {
      @Nonnull
      @Override
      public Iterator<String> iterator() {
        return new Iterator<String>() {
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
          result = new SmartList<String>();
        }
        result.add(text.substring(start, i + 1));
      }
      else if (!isIdentifierPart && start != -1) {
        if (result == null) {
          result = new SmartList<String>();
        }
        result.add(text.substring(start, i));
        start = -1;
      }
    }
    if (result == null) {
      return ContainerUtil.emptyList();
    }
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static List<TextRange> getWordIndicesIn(@Nonnull String text) {
    List<TextRange> result = new SmartList<TextRange>();
    int start = -1;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      boolean isIdentifierPart = Character.isJavaIdentifierPart(c);
      if (isIdentifierPart && start == -1) {
        start = i;
      }
      if (isIdentifierPart && i == text.length() - 1 && start != -1) {
        result.add(new TextRange(start, i + 1));
      }
      else if (!isIdentifierPart && start != -1) {
        result.add(new TextRange(start, i));
        start = -1;
      }
    }
    return result;
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
  public static String[] zip(@Nonnull String[] strings1, @Nonnull String[] strings2, String separator) {
    if (strings1.length != strings2.length) throw new IllegalArgumentException();

    String[] result = ArrayUtil.newStringArray(strings1.length);
    for (int i = 0; i < result.length; i++) {
      result[i] = strings1[i] + separator + strings2[i];
    }

    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static String[] surround(@Nonnull String[] strings1, String prefix, String suffix) {
    String[] result = ArrayUtil.newStringArray(strings1.length);
    for (int i = 0; i < result.length; i++) {
      result[i] = prefix + strings1[i] + suffix;
    }

    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> String join(@Nonnull T[] items, @Nonnull Function<T, String> f, @Nonnull @NonNls String separator) {
    return join(Arrays.asList(items), f, separator);
  }

  public static <T> void join(@Nonnull Iterable<? extends T> items, @Nonnull Function<? super T, String> f, @Nonnull String separator, @Nonnull StringBuilder result) {
    boolean isFirst = true;
    for (T item : items) {
      String string = f.fun(item);
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
  public static <T> String join(@Nonnull Collection<? extends T> items, @Nonnull Function<? super T, String> f, @Nonnull @NonNls String separator) {
    if (items.isEmpty()) return "";
    return join((Iterable<? extends T>)items, f, separator);
  }

  @Contract(pure = true)
  public static String join(@Nonnull Iterable<?> items, @Nonnull @NonNls String separator) {
    StringBuilder result = new StringBuilder();
    for (Object item : items) {
      result.append(item).append(separator);
    }
    if (result.length() > 0) {
      result.setLength(result.length() - separator.length());
    }
    return result.toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> String join(@Nonnull Iterable<? extends T> items, @Nonnull Function<? super T, String> f, @Nonnull @NonNls String separator) {
    final StringBuilder result = new StringBuilder();
    for (T item : items) {
      String string = f.fun(item);
      if (string != null && !string.isEmpty()) {
        if (result.length() != 0) result.append(separator);
        result.append(string);
      }
    }
    return result.toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static String join(@Nonnull Collection<String> strings, @Nonnull String separator) {
    if (strings.size() <= 1) {
      return notNullize(ContainerUtil.getFirstItem(strings));
    }
    StringBuilder result = new StringBuilder();
    join(strings, separator, result);
    return result.toString();
  }

  public static void join(@Nonnull Collection<String> strings, @Nonnull String separator, @Nonnull StringBuilder result) {
    boolean isFirst = true;
    for (String string : strings) {
      if (string != null) {
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
  public static String join(@Nonnull final int[] strings, @Nonnull final String separator) {
    final StringBuilder result = new StringBuilder();
    for (int i = 0; i < strings.length; i++) {
      if (i > 0) result.append(separator);
      result.append(strings[i]);
    }
    return result.toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static String join(@Nullable final String... strings) {
    if (strings == null || strings.length == 0) return "";

    final StringBuilder builder = new StringBuilder();
    for (final String string : strings) {
      builder.append(string);
    }
    return builder.toString();
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

  @Nonnull
  @Contract(pure = true)
  public static String formatDurationApproximate(long duration) {
    return formatDuration(duration, null);
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
  public static boolean containsAlphaCharacters(@Nonnull String value) {
    for (int i = 0; i < value.length(); i++) {
      if (Character.isLetter(value.charAt(i))) return true;
    }
    return false;
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
  public static boolean containsChar(@Nonnull final String value, final char ch) {
    return value.indexOf(ch) >= 0;
  }

  /**
   * @deprecated use #capitalize(String)
   */
  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String firstLetterToUpperCase(@Nullable final String displayString) {
    if (displayString == null || displayString.isEmpty()) return displayString;
    char firstChar = displayString.charAt(0);
    char uppedFirstChar = toUpperCase(firstChar);

    if (uppedFirstChar == firstChar) return displayString;

    char[] buffer = displayString.toCharArray();
    buffer[0] = uppedFirstChar;
    return StringFactory.createShared(buffer);
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

  @Nonnull
  @Contract(pure = true)
  public static List<String> findMatches(@Nonnull String s, @Nonnull Pattern pattern) {
    return findMatches(s, pattern, 1);
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> findMatches(@Nonnull String s, @Nonnull Pattern pattern, int groupIndex) {
    List<String> result = new SmartList<String>();
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

  @Nonnull
  @Contract(pure = true)
  public static String replaceSubstring(@Nonnull String string, @Nonnull TextRange range, @Nonnull String replacement) {
    return range.replace(string, replacement);
  }

  @Contract(pure = true)
  public static boolean startsWithWhitespace(@Nonnull String text) {
    return !text.isEmpty() && Character.isWhitespace(text.charAt(0));
  }

  @Contract(pure = true)
  public static boolean isChar(CharSequence seq, int index, char c) {
    return index >= 0 && index < seq.length() && seq.charAt(index) == c;
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

  @Contract(pure = true)
  public static boolean endsWith(@Nonnull CharSequence text, @Nonnull CharSequence suffix) {
    return StringUtilRt.endsWith(text, suffix);
  }

  @Contract(pure = true)
  public static boolean endsWith(@Nonnull CharSequence text, int start, int end, @Nonnull CharSequence suffix) {
    int suffixLen = suffix.length();
    if (end < suffixLen) return false;

    for (int i = end - 1; i >= end - suffixLen && i >= start; i--) {
      if (text.charAt(i) != suffix.charAt(i + suffixLen - end)) return false;
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
  public static boolean containsWhitespaces(@Nullable CharSequence s) {
    if (s == null) return false;

    for (int i = 0; i < s.length(); i++) {
      if (Character.isWhitespace(s.charAt(i))) return true;
    }
    return false;
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
  public static boolean contains(@Nonnull CharSequence sequence, @Nonnull CharSequence infix) {
    return indexOf(sequence, infix) >= 0;
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull CharSequence sequence, @Nonnull CharSequence infix) {
    return indexOf(sequence, infix, 0);
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
  public static int indexOfSubstringEnd(@Nonnull String text, @Nonnull String subString) {
    int i = text.indexOf(subString);
    if (i == -1) return -1;
    return i + subString.length();
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
  public static int lastIndexOfAny(@Nonnull CharSequence s, @Nonnull final String chars) {
    for (int i = s.length() - 1; i >= 0; i--) {
      if (containsChar(chars, s.charAt(i))) return i;
    }
    return -1;
  }

  @Contract(pure = true)
  @Nullable
  public static String substringBefore(@Nonnull String text, @Nonnull String subString) {
    int i = text.indexOf(subString);
    if (i == -1) return null;
    return text.substring(0, i);
  }

  @Contract(pure = true)
  @Nonnull
  public static String substringBeforeLast(@Nonnull String text, @Nonnull String subString) {
    int i = text.lastIndexOf(subString);
    if (i == -1) return text;
    return text.substring(0, i);
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
    return StringUtilRt.lastIndexOf(s, c, start, end);
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
  public static String firstLast(@Nonnull String text, int length) {
    return text.length() > length ? text.subSequence(0, length / 2) + "\u2026" + text.subSequence(text.length() - length / 2 - 1, text.length()) : text;
  }

  @Nonnull
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
  public static String escapeSlashes(@Nonnull final String str) {
    return escapeChar(str, '/');
  }

  @Nonnull
  @Contract(pure = true)
  public static String escapeBackSlashes(@Nonnull final String str) {
    return escapeChar(str, '\\');
  }

  public static void escapeSlashes(@Nonnull final StringBuilder buf) {
    escapeChar(buf, '/');
  }

  @Nonnull
  @Contract(pure = true)
  public static String unescapeSlashes(@Nonnull final String str) {
    final StringBuilder buf = new StringBuilder(str.length());
    unescapeChar(buf, str, '/');
    return buf.toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static String unescapeBackSlashes(@Nonnull final String str) {
    final StringBuilder buf = new StringBuilder(str.length());
    unescapeChar(buf, str, '\\');
    return buf.toString();
  }

  @Nonnull
  @Contract(pure = true)
  public static String unescapeChar(@Nonnull final String str, char unescapeChar) {
    final StringBuilder buf = new StringBuilder(str.length());
    unescapeChar(buf, str, unescapeChar);
    return buf.toString();
  }

  private static void unescapeChar(@Nonnull StringBuilder buf, @Nonnull String str, char unescapeChar) {
    final int length = str.length();
    final int last = length - 1;
    for (int i = 0; i < length; i++) {
      char ch = str.charAt(i);
      if (ch == '\\' && i != last) {
        i++;
        ch = str.charAt(i);
        if (ch != unescapeChar) buf.append('\\');
      }

      buf.append(ch);
    }
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
  public static String wrapWithDoubleQuote(@Nonnull String str) {
    return '\"' + str + "\"";
  }

  @NonNls
  private static final String[] REPLACES_REFS = {"&lt;", "&gt;", "&amp;", "&#39;", "&quot;"};
  @NonNls
  private static final String[] REPLACES_DISP = {"<", ">", "&", "'", "\""};

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

  @NonNls
  private static final String[] MN_QUOTED = {"&&", "__"};
  @NonNls
  private static final String[] MN_CHARS = {"&", "_"};

  @Contract(value = "null -> null; !null -> !null", pure = true)
  public static String escapeMnemonics(@Nullable String text) {
    if (text == null) return null;
    return replace(text, MN_CHARS, MN_QUOTED);
  }

  @Nonnull
  @Contract(pure = true)
  public static String htmlEmphasize(@Nonnull String text) {
    return "<b><code>" + escapeXml(text) + "</code></b>";
  }


  @Nonnull
  @Contract(pure = true)
  public static String escapeToRegexp(@Nonnull String text) {
    final StringBuilder result = new StringBuilder(text.length());
    return escapeToRegexp(text, result).toString();
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

  @Contract(pure = true)
  public static boolean isNotEscapedBackslash(@Nonnull char[] chars, int startOffset, int backslashOffset) {
    if (chars[backslashOffset] != '\\') {
      return false;
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
    return !escaped;
  }

  @Contract(pure = true)
  public static boolean isNotEscapedBackslash(@Nonnull CharSequence text, int startOffset, int backslashOffset) {
    if (text.charAt(backslashOffset) != '\\') {
      return false;
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
    return !escaped;
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

  @Nonnull
  @Contract(pure = true)
  public static String[] filterEmptyStrings(@Nonnull String[] strings) {
    int emptyCount = 0;
    for (String string : strings) {
      if (string == null || string.isEmpty()) emptyCount++;
    }
    if (emptyCount == 0) return strings;

    String[] result = ArrayUtil.newStringArray(strings.length - emptyCount);
    int count = 0;
    for (String string : strings) {
      if (string == null || string.isEmpty()) continue;
      result[count++] = string;
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

  @Nonnull
  @Contract(pure = true)
  public static String capitalsOnly(@Nonnull String s) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      if (Character.isUpperCase(s.charAt(i))) {
        b.append(s.charAt(i));
      }
    }

    return b.toString();
  }

  /**
   * @param args Strings to join.
   * @return {@code null} if any of given Strings is {@code null}.
   */
  @Nullable
  @Contract(pure = true)
  public static String joinOrNull(@Nonnull String... args) {
    StringBuilder r = new StringBuilder();
    for (String arg : args) {
      if (arg == null) return null;
      r.append(arg);
    }
    return r.toString();
  }

  @Nullable
  @Contract(pure = true)
  public static String getPropertyName(@NonNls @Nonnull String methodName) {
    if (methodName.startsWith("get")) {
      return decapitalize(methodName.substring(3));
    }
    else if (methodName.startsWith("is")) {
      return decapitalize(methodName.substring(2));
    }
    else if (methodName.startsWith("set")) {
      return decapitalize(methodName.substring(3));
    }
    else {
      return null;
    }
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

  /**
   * Escape property name or key in property file. Unicode characters are escaped as well.
   *
   * @param input an input to escape
   * @param isKey if true, the rules for key escaping are applied. The leading space is escaped in that case.
   * @return an escaped string
   */
  @Nonnull
  @Contract(pure = true)
  public static String escapeProperty(@Nonnull String input, final boolean isKey) {
    final StringBuilder escaped = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      final char ch = input.charAt(i);
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

  @Contract(pure = true)
  public static String getQualifiedName(@Nullable String packageName, String className) {
    if (packageName == null || packageName.isEmpty()) {
      return className;
    }
    return packageName + '.' + className;
  }

  @Contract(pure = true)
  public static int compareVersionNumbers(@Nullable String v1, @Nullable String v2) {
    return consulo.util.lang.StringUtil.compareVersionNumbers(v1, v2);
  }

  @Contract(pure = true)
  public static int getOccurrenceCount(@Nonnull String text, final char c) {
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
  public static int getOccurrenceCount(@Nonnull String text, @Nonnull String s) {
    int res = 0;
    int i = 0;
    while (i < text.length()) {
      i = text.indexOf(s, i);
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

  @Nonnull
  @Contract(pure = true)
  public static String fixVariableNameDerivedFromPropertyName(@Nonnull String name) {
    if (isEmptyOrSpaces(name)) return name;
    char c = name.charAt(0);
    if (isVowel(c)) {
      return "an" + Character.toUpperCase(c) + name.substring(1);
    }
    return "a" + Character.toUpperCase(c) + name.substring(1);
  }

  @Nonnull
  @Contract(pure = true)
  public static String sanitizeJavaIdentifier(@Nonnull String name) {
    final StringBuilder result = new StringBuilder(name.length());

    for (int i = 0; i < name.length(); i++) {
      final char ch = name.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
        if (result.length() == 0 && !Character.isJavaIdentifierStart(ch)) {
          result.append("_");
        }
        result.append(ch);
      }
    }

    return result.toString();
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
  public static String tail(@Nonnull String s, final int idx) {
    return idx >= s.length() ? "" : s.substring(idx, s.length());
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

  @Nonnull
  @Contract(pure = true)
  public static String[] splitByLinesDontTrim(@Nonnull String string) {
    return EOL_SPLIT_DONT_TRIM_PATTERN.split(string);
  }

  /**
   * Splits string by lines, keeping all line separators at the line ends and in the empty lines.
   * <br> E.g. splitting text
   * <blockquote>
   * foo\r\n<br>
   * \n<br>
   * bar\n<br>
   * \r\n<br>
   * baz\r<br>
   * \r<br>
   * </blockquote>
   * will return the following array: foo\r\n, \n, bar\n, \r\n, baz\r, \r
   */
  @Nonnull
  @Contract(pure = true)
  public static String[] splitByLinesKeepSeparators(@Nonnull String string) {
    return EOL_SPLIT_KEEP_SEPARATORS.split(string);
  }

  @Nonnull
  @Contract(pure = true)
  public static List<Pair<String, Integer>> getWordsWithOffset(@Nonnull String s) {
    List<Pair<String, Integer>> res = ContainerUtil.newArrayList();
    s += " ";
    StringBuilder name = new StringBuilder();
    int startInd = -1;
    for (int i = 0; i < s.length(); i++) {
      if (Character.isWhitespace(s.charAt(i))) {
        if (name.length() > 0) {
          res.add(Pair.create(name.toString(), startInd));
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
    return res;
  }

  /**
   * Implementation of "Sorting for Humans: Natural Sort Order":
   * http://www.codinghorror.com/blog/2007/12/sorting-for-humans-natural-sort-order.html
   */
  @Contract(pure = true)
  public static int naturalCompare(@Nullable String string1, @Nullable String string2) {
    return NaturalComparator.INSTANCE.compare(string1, string2);
  }

  @Contract(pure = true)
  public static boolean isDecimalDigit(char c) {
    return c >= '0' && c <= '9';
  }

  @Contract("null -> false")
  public static boolean isNotNegativeNumber(@Nullable CharSequence s) {
    if (s == null) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (!isDecimalDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
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

  @Contract(pure = true)
  public static int comparePairs(@Nullable String s1, @Nullable String t1, @Nullable String s2, @Nullable String t2, boolean ignoreCase) {
    final int compare = compare(s1, s2, ignoreCase);
    return compare != 0 ? compare : compare(t1, t2, ignoreCase);
  }

  @Contract(pure = true)
  public static int hashCode(@Nonnull CharSequence s) {
    return stringHashCode(s);
  }

  @Contract(pure = true)
  public static boolean equals(@Nullable CharSequence s1, @Nullable CharSequence s2) {
    return consulo.util.lang.StringUtil.equals(s1, s2);
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
  public static boolean equalsIgnoreWhitespaces(@Nullable CharSequence s1, @Nullable CharSequence s2) {
    if (s1 == null ^ s2 == null) {
      return false;
    }

    if (s1 == null) {
      return true;
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

      if (!skipped) return false;
    }

    for (; index1 != len1; index1++) {
      if (!isWhiteSpace(s1.charAt(index1))) return false;
    }
    for (; index2 != len2; index2++) {
      if (!isWhiteSpace(s2.charAt(index2))) return false;
    }

    return true;
  }

  @Contract(pure = true)
  public static boolean equalsTrimWhitespaces(@Nonnull CharSequence s1, @Nonnull CharSequence s2) {
    int start1 = 0;
    int end1 = s1.length();
    int start2 = 0;
    int end2 = s2.length();

    while (start1 < end1) {
      char c = s1.charAt(start1);
      if (!isWhiteSpace(c)) break;
      start1++;
    }

    while (start1 < end1) {
      char c = s1.charAt(end1 - 1);
      if (!isWhiteSpace(c)) break;
      end1--;
    }

    while (start2 < end2) {
      char c = s2.charAt(start2);
      if (!isWhiteSpace(c)) break;
      start2++;
    }

    while (start2 < end2) {
      char c = s2.charAt(end2 - 1);
      if (!isWhiteSpace(c)) break;
      end2--;
    }

    CharSequence ts1 = new CharSequenceSubSequence(s1, start1, end1);
    CharSequence ts2 = new CharSequenceSubSequence(s2, start2, end2);

    return equals(ts1, ts2);
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
    char u1 = StringUtilRt.toUpperCase(c1);
    char u2 = StringUtilRt.toUpperCase(c2);
    d = u1 - u2;
    if (d != 0) {
      // Unfortunately, conversion to uppercase does not work properly
      // for the Georgian alphabet, which has strange rules about case
      // conversion.  So we need to make one last check before
      // exiting.
      d = StringUtilRt.toLowerCase(u1) - StringUtilRt.toLowerCase(u2);
    }
    return d;
  }

  @Contract(pure = true)
  public static boolean charsMatch(char c1, char c2, boolean ignoreCase) {
    return compare(c1, c2, ignoreCase) == 0;
  }

  @Nonnull
  @Contract(pure = true)
  public static String formatLinks(@Nonnull String message) {
    Pattern linkPattern = Pattern.compile("http://[a-zA-Z0-9\\./\\-\\+]+");
    StringBuffer result = new StringBuffer();
    Matcher m = linkPattern.matcher(message);
    while (m.find()) {
      m.appendReplacement(result, "<a href=\"" + m.group() + "\">" + m.group() + "</a>");
    }
    m.appendTail(result);
    return result.toString();
  }

  @Contract(pure = true)
  public static boolean isHexDigit(char c) {
    return '0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F';
  }

  @Contract(pure = true)
  public static boolean isOctalDigit(char c) {
    return '0' <= c && c <= '7';
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

  @Contract(pure = true)
  public static boolean charsEqual(char a, char b, boolean ignoreCase) {
    return ignoreCase ? charsEqualIgnoreCase(a, b) : a == b;
  }

  @Contract(pure = true)
  public static boolean charsEqualIgnoreCase(char a, char b) {
    return StringUtilRt.charsEqualIgnoreCase(a, b);
  }

  @Contract(pure = true)
  public static char toUpperCase(char a) {
    return StringUtilRt.toUpperCase(a);
  }

  @Nonnull
  @Contract(pure = true)
  public static String toUpperCase(@Nonnull String a) {
    return StringUtilRt.toUpperCase(a).toString();
  }

  @Contract(pure = true)
  public static char toLowerCase(final char a) {
    return StringUtilRt.toLowerCase(a);
  }

  @Nullable
  public static LineSeparator detectSeparators(@Nonnull CharSequence text) {
    int index = indexOfAny(text, "\n\r");
    if (index == -1) return null;
    if (startsWith(text, index, "\r\n")) return LineSeparator.CRLF;
    if (text.charAt(index) == '\r') return LineSeparator.CR;
    if (text.charAt(index) == '\n') return LineSeparator.LF;
    throw new IllegalStateException();
  }

  @Nullable
  public static LineSeparator getLineSeparatorAt(@Nonnull CharSequence text, int index) {
    if (index < 0 || index >= text.length()) {
      return null;
    }
    char ch = text.charAt(index);
    if (ch == '\r') {
      return index + 1 < text.length() && text.charAt(index + 1) == '\n' ? LineSeparator.CRLF : LineSeparator.CR;
    }
    return ch == '\n' ? LineSeparator.LF : null;
  }

  @Nonnull
  @Contract(pure = true)
  public static String convertLineSeparators(@Nonnull String text) {
    return StringUtilRt.convertLineSeparators(text);
  }

  @Nonnull
  @Contract(pure = true)
  public static String convertLineSeparators(@Nonnull String text, boolean keepCarriageReturn) {
    return StringUtilRt.convertLineSeparators(text, keepCarriageReturn);
  }

  @Nonnull
  @Contract(pure = true)
  public static String convertLineSeparators(@Nonnull String text, @Nonnull String newSeparator) {
    return StringUtilRt.convertLineSeparators(text, newSeparator);
  }

  @Nonnull
  public static String convertLineSeparators(@Nonnull String text, @Nonnull String newSeparator, @Nullable int[] offsetsToKeep) {
    return StringUtilRt.convertLineSeparators(text, newSeparator, offsetsToKeep);
  }

  @Nonnull
  public static String convertLineSeparators(@Nonnull String text, @Nonnull String newSeparator, @Nullable int[] offsetsToKeep, boolean keepCarriageReturn) {
    return StringUtilRt.convertLineSeparators(text, newSeparator, offsetsToKeep, keepCarriageReturn);
  }

  @Contract(pure = true)
  public static int parseInt(final String string, final int defaultValue) {
    return StringUtilRt.parseInt(string, defaultValue);
  }

  @Contract(pure = true)
  public static double parseDouble(final String string, final double defaultValue) {
    return StringUtilRt.parseDouble(string, defaultValue);
  }

  @Contract(pure = true)
  public static boolean parseBoolean(String string, final boolean defaultValue) {
    return StringUtilRt.parseBoolean(string, defaultValue);
  }

  @Contract(pure = true)
  public static <E extends Enum<E>> E parseEnum(String string, E defaultValue, Class<E> clazz) {
    return StringUtilRt.parseEnum(string, defaultValue, clazz);
  }

  @Nonnull
  @Contract(pure = true)
  public static String getShortName(@Nonnull Class aClass) {
    return StringUtilRt.getShortName(aClass);
  }

  @Nonnull
  @Contract(pure = true)
  public static String getShortName(@Nonnull String fqName) {
    return StringUtilRt.getShortName(fqName);
  }

  @Nonnull
  @Contract(pure = true)
  public static String getShortName(@Nonnull String fqName, char separator) {
    return StringUtilRt.getShortName(fqName, separator);
  }

  @Nonnull
  @Contract(pure = true)
  public static CharSequence newBombedCharSequence(@Nonnull CharSequence sequence, long delay) {
    final long myTime = System.currentTimeMillis() + delay;
    return new BombedCharSequence(sequence) {
      @Override
      protected void checkCanceled() {
        long l = System.currentTimeMillis();
        if (l >= myTime) {
          throw new ProcessCanceledException();
        }
      }
    };
  }

  public static boolean trimEnd(@Nonnull StringBuilder buffer, @Nonnull CharSequence end) {
    if (endsWith(buffer, end)) {
      buffer.delete(buffer.length() - end.length(), buffer.length());
      return true;
    }
    return false;
  }

  /**
   * Say smallPart = "op" and bigPart="open". Method returns true for "Ope" and false for "ops"
   */
  @Contract(pure = true)
  public static boolean isBetween(@Nonnull String string, @Nonnull String smallPart, @Nonnull String bigPart) {
    final String s = string.toLowerCase();
    return s.startsWith(smallPart.toLowerCase()) && bigPart.toLowerCase().startsWith(s);
  }

  @Contract(pure = true)
  public static String getShortened(@Nonnull String s, int maxWidth) {
    int length = s.length();
    if (isEmpty(s) || length <= maxWidth) return s;
    ArrayList<String> words = new ArrayList<String>();

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length; i++) {
      char ch = s.charAt(i);

      if (i == length - 1) {
        builder.append(ch);
        words.add(builder.toString());
        builder.delete(0, builder.length());
        continue;
      }

      if (i > 0 && (ch == '/' || ch == '\\' || ch == '.' || ch == '-' || Character.isUpperCase(ch))) {
        words.add(builder.toString());
        builder.delete(0, builder.length());
      }
      builder.append(ch);
    }
    for (int i = 0; i < words.size(); i++) {
      String word = words.get(i);
      if (i < words.size() - 1 && word.length() == 1) {
        words.remove(i);
        words.set(i, word + words.get(i));
      }
    }

    int removedLength = 0;

    String toPaste = "...";
    int index;
    while (true) {
      index = Math.max(0, (words.size() - 1) / 2);
      String aWord = words.get(index);
      words.remove(index);
      int toCut = length - removedLength - maxWidth + 3;
      if (words.size() < 2 || (toCut < aWord.length() - 2 && removedLength == 0)) {
        int pos = (aWord.length() - toCut) / 2;
        toPaste = aWord.substring(0, pos) + "..." + aWord.substring(pos + toCut);
        break;
      }
      removedLength += aWord.length();
      if (length - removedLength <= maxWidth - 3) {
        break;
      }
    }
    for (int i = 0; i < Math.max(1, words.size()); i++) {
      String word = words.isEmpty() ? "" : words.get(i);
      if (i == index || words.size() == 1) builder.append(toPaste);
      builder.append(word);
    }
    return builder.toString().replaceAll("\\.{4,}", "...");
  }

  /**
   * Does the string have an uppercase character?
   *
   * @param s the string to test.
   * @return true if the string has an uppercase character, false if not.
   */
  public static boolean hasUpperCaseChar(String s) {
    char[] chars = s.toCharArray();
    for (char c : chars) {
      if (Character.isUpperCase(c)) {
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
  public static boolean hasLowerCaseChar(String s) {
    char[] chars = s.toCharArray();
    for (char c : chars) {
      if (Character.isLowerCase(c)) {
        return true;
      }
    }
    return false;
  }


  private static final Pattern UNICODE_CHAR = Pattern.compile("\\\\u[0-9a-eA-E]{4}");

  public static String replaceUnicodeEscapeSequences(String text) {
    if (text == null) return null;

    final Matcher matcher = UNICODE_CHAR.matcher(text);
    if (!matcher.find()) return text; // fast path

    matcher.reset();
    int lastEnd = 0;
    final StringBuilder sb = new StringBuilder(text.length());
    while (matcher.find()) {
      sb.append(text.substring(lastEnd, matcher.start()));
      final char c = (char)Integer.parseInt(matcher.group().substring(2), 16);
      sb.append(c);
      lastEnd = matcher.end();
    }
    sb.append(text.substring(lastEnd, text.length()));
    return sb.toString();
  }

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


  /**
   * @return {@code text} with some characters replaced with standard XML entities, e.g. '<' replaced with '{@code &lt;}'
   */
  @Nonnull
  @Contract(pure = true)
  public static String escapeXmlEntities(@Nonnull String text) {
    return replace(text, REPLACES_DISP, REPLACES_REFS);
  }

  /**
   * @deprecated use {@link #startsWithConcatenation(String, String...)} (to remove in IDEA 15)
   */
  @SuppressWarnings("unused")
  public static boolean startsWithConcatenationOf(@Nonnull String string, @Nonnull String firstPrefix, @Nonnull String secondPrefix) {
    return startsWithConcatenation(string, firstPrefix, secondPrefix);
  }

  @Contract(pure = true)
  public static int lastIndexOfIgnoreCase(@Nonnull String where, char what, int fromIndex) {
    for (int i = Math.min(fromIndex, where.length() - 1); i >= 0; i--) {
      if (charsEqualIgnoreCase(where.charAt(i), what)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * @return {@code text} with some standard XML entities replaced with corresponding characters, e.g. '{@code &lt;}' replaced with '<'
   */
  @Nonnull
  @Contract(pure = true)
  public static String unescapeXmlEntities(@Nonnull String text) {
    return replace(text, REPLACES_REFS, REPLACES_DISP);
  }

  /**
   * Finds the next position in the supplied CharSequence which is neither a space nor a tab.
   *
   * @param text text
   * @param pos  starting position
   * @return position of the first non-whitespace character after or equal to pos; or the length of the CharSequence
   * if no non-whitespace character found
   */
  public static int skipWhitespaceForward(@Nonnull CharSequence text, int pos) {
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
  public static int skipWhitespaceBackward(@Nonnull CharSequence text, int pos) {
    while (pos > 0 && isWhitespaceOrTab(text.charAt(pos - 1))) {
      pos--;
    }
    return pos;
  }

  @Contract(value = "null -> null; !null->!null", pure = true)
  public static String internEmptyString(String s) {
    return s == null ? null : s.isEmpty() ? "" : s;
  }

  private static boolean isWhitespaceOrTab(char c) {
    return c == ' ' || c == '\t';
  }
}
