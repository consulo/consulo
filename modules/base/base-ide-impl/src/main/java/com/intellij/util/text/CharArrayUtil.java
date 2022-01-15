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
package com.intellij.util.text;

import com.intellij.openapi.util.TextRange;
import consulo.annotation.internal.MigratedExtensionsTo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("MigratedExtensionsTo")
@MigratedExtensionsTo(consulo.util.lang.CharArrayUtil.class)
public class CharArrayUtil {
  private CharArrayUtil() {
  }

  /**
   * Copies all symbols from the given char sequence to the given array
   *
   * @param src         source data holder
   * @param dst         output data buffer
   * @param dstOffset   start offset to use within the given output data buffer
   */
  public static void getChars(@Nonnull CharSequence src, @Nonnull char[] dst, int dstOffset) {
    consulo.util.lang.CharArrayUtil.getChars(src, dst, dstOffset);
  }

  /**
   * Copies necessary number of symbols from the given char sequence start to the given array.
   *
   * @param src         source data holder
   * @param dst         output data buffer
   * @param dstOffset   start offset to use within the given output data buffer
   * @param len         number of source data symbols to copy to the given buffer
   */
  public static void getChars(@Nonnull CharSequence src, @Nonnull char[] dst, int dstOffset, int len) {
    consulo.util.lang.CharArrayUtil.getChars(src, dst, dstOffset, len);
  }

  /**
   * Copies necessary number of symbols from the given char sequence to the given array.
   *
   * @param src         source data holder
   * @param dst         output data buffer
   * @param srcOffset   source text offset
   * @param dstOffset   start offset to use within the given output data buffer
   * @param len         number of source data symbols to copy to the given buffer
   */
  public static void getChars(@Nonnull CharSequence src, @Nonnull char[] dst, int srcOffset, int dstOffset, int len) {
    consulo.util.lang.CharArrayUtil.getChars(src, dst, srcOffset, dstOffset, len);
  }

  /**
   * @deprecated use {@link #fromSequence(CharSequence)}
   */
  @Nonnull
  public static char[] fromSequenceStrict(@Nonnull CharSequence seq) {
    return consulo.util.lang.CharArrayUtil.fromSequenceStrict(seq);
  }

  @Nullable
  public static char[] fromSequenceWithoutCopying(@Nullable CharSequence seq) {
    return consulo.util.lang.CharArrayUtil.fromSequenceWithoutCopying(seq);
  }

  /**
   * @return the underlying char[] array if any, or the new chara array if not
   */
  @Nonnull
  public static char[] fromSequence(@Nonnull CharSequence seq) {
    return consulo.util.lang.CharArrayUtil.fromSequence(seq);
  }

  /**
   * @return a new char array containing the sub-sequence's chars
   */
  @Nonnull
  public static char[] fromSequence(@Nonnull CharSequence seq, int start, int end) {
    return consulo.util.lang.CharArrayUtil.fromSequence(seq, start, end);
  }

  public static int shiftForward(@Nonnull CharSequence buffer, int offset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftForward(buffer, offset, chars);
  }

  /**
   * Tries to find an offset from the <code>[startOffset; endOffset)</code> interval such that a char from the given buffer is
   * not contained at the given 'chars' string.
   * <p/>
   * Example:
   * {@code buffer="abc", startOffset=0, endOffset = 3, chars="ab". Result: 2}
   *
   * @param buffer       target buffer which symbols should be checked
   * @param startOffset  start offset to use within the given buffer (inclusive)
   * @param endOffset    end offset to use within the given buffer (exclusive)
   * @param chars        pass-through symbols
   * @return             offset from the <code>[startOffset; endOffset)</code> which points to a symbol at the given buffer such
   *                     as that that symbol is not contained at the given 'chars';
   *                     <code>endOffset</code> otherwise
   */
  public static int shiftForward(@Nonnull CharSequence buffer, final int startOffset, final int endOffset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftForward(buffer, startOffset, endOffset, chars);
  }

  public static int shiftForwardCarefully(@Nonnull CharSequence buffer, int offset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftForwardCarefully(buffer, offset, chars);
  }

  public static int shiftForward(@Nonnull char[] buffer, int offset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftForward(buffer, offset, chars);
  }

  public static int shiftBackward(@Nonnull CharSequence buffer, int offset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftBackward(buffer, offset, chars);
  }

  public static int shiftBackward(@Nonnull CharSequence buffer, int minOffset, int maxOffset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftBackward(buffer, minOffset, maxOffset, chars);
  }

  public static int shiftBackward(@Nonnull char[] buffer, int offset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftBackward(buffer, offset, chars);
  }

  public static int shiftForwardUntil(@Nonnull CharSequence buffer, int offset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftForwardUntil(buffer, offset, chars);
  }

  //Commented in order to apply to the green code policy as the method is unused.
  //
  //public static int shiftBackwardUntil(char[] buffer, int offset, String chars) {
  //  return shiftBackwardUntil(new CharArrayCharSequence(buffer), offset, chars);
  //}

  /**
   * Calculates offset that points to the given buffer and has the following characteristics:
   * <p/>
   * <ul>
   *   <li>is less than or equal to the given offset;</li>
   *   <li>
   *      it's guaranteed that all symbols of the given buffer that are located at <code>(returned offset; given offset]</code>
   *      interval differ from the given symbols;
   *    </li>
   * </ul>
   * <p/>
   * Example: suppose that this method is called with buffer that holds <code>'test data'</code> symbols, offset that points
   * to the last symbols and <code>'sf'</code> as a chars to exclude. Offset that points to <code>'s'</code> symbol
   * is returned then, i.e. all symbols of the given buffer that are located after it and not after given offset
   * (<code>'t data'</code>) are guaranteed to not contain given chars (<code>'sf'</code>).
   *
   * @param buffer      symbols buffer to check
   * @param offset      initial symbols buffer offset to use
   * @param chars       chars to exclude
   * @return            offset of the given buffer that guarantees that all symbols at <code>(returned offset; given offset]</code>
   *                    interval of the given buffer differ from symbols of given <code>'chars'</code> arguments;
   *                    given offset is returned if it is outside of given buffer bounds;
   *                    <code>'-1'</code> is returned if all document symbols that precede given offset differ from symbols
   *                    of the given <code>'chars to exclude'</code>
   */
  public static int shiftBackwardUntil(@Nonnull CharSequence buffer, int offset, @Nonnull String chars) {
    return consulo.util.lang.CharArrayUtil.shiftBackwardUntil(buffer, offset, chars);
  }

  public static boolean regionMatches(@Nonnull char[] buffer, int start, int end, @Nonnull CharSequence s) {
    return consulo.util.lang.CharArrayUtil.regionMatches(buffer, start, end, s);
  }

  public static boolean regionMatches(@Nonnull CharSequence buffer, int start, int end, @Nonnull CharSequence s) {
    return consulo.util.lang.CharArrayUtil.regionMatches(buffer, start, end, s);
  }

  public static boolean regionMatches(@Nonnull CharSequence s1, int start1, int end1, @Nonnull CharSequence s2, int start2, int end2) {
    return consulo.util.lang.CharArrayUtil.regionMatches(s1, start1, end1, s2, start2, end2);
  }

  public static boolean regionMatches(@Nonnull CharSequence buffer, int offset, @Nonnull CharSequence s) {
    return consulo.util.lang.CharArrayUtil.regionMatches(buffer, offset, s);
  }

  public static boolean equals(@Nonnull char[] buffer1, int start1, int end1, @Nonnull char[] buffer2, int start2, int end2) {
    return consulo.util.lang.CharArrayUtil.equals(buffer1, start1, end1, buffer2, start2, end2);
  }

  public static int indexOf(@Nonnull char[] buffer, @Nonnull String pattern, int fromIndex) {
    return consulo.util.lang.CharArrayUtil.indexOf(buffer, pattern, fromIndex);
  }

  public static int indexOf(@Nonnull CharSequence buffer, @Nonnull CharSequence pattern, int fromIndex) {
    return consulo.util.lang.CharArrayUtil.indexOf(buffer, pattern, fromIndex);
  }

  /**
   * Tries to find index of given pattern at the given buffer.
   *
   * @param buffer       characters buffer which contents should be checked for the given pattern
   * @param pattern      target characters sequence to find at the given buffer
   * @param fromIndex    start index (inclusive). Zero is used if given index is negative
   * @param toIndex      end index (exclusive)
   * @return             index of the given pattern at the given buffer if the match is found; <code>-1</code> otherwise
   */
  public static int indexOf(@Nonnull CharSequence buffer, @Nonnull CharSequence pattern, int fromIndex, final int toIndex) {
    return consulo.util.lang.CharArrayUtil.indexOf(buffer, pattern, fromIndex, toIndex);
  }

  /**
   * Tries to find index that points to the first location of the given symbol at the given char array at range <code>[from; to)</code>.
   *
   * @param buffer      target symbols holder to check
   * @param symbol      target symbol which offset should be found
   * @param fromIndex   start index to search (inclusive)
   * @param toIndex     end index to search (exclusive)
   * @return            index that points to the first location of the given symbol at the given char array at range
   *                    <code>[from; to)</code> if target symbol is found;
   *                    <code>-1</code> otherwise
   */
  public static int indexOf(@Nonnull char[] buffer, final char symbol, int fromIndex, final int toIndex) {
    return consulo.util.lang.CharArrayUtil.indexOf(buffer, symbol, fromIndex, toIndex);
  }

  /**
   * Tries to find index that points to the last location of the given symbol at the given char array at range <code>[from; to)</code>.
   *
   * @param buffer      target symbols holder to check
   * @param symbol      target symbol which offset should be found
   * @param fromIndex   start index to search (inclusive)
   * @param toIndex     end index to search (exclusive)
   * @return            index that points to the last location of the given symbol at the given char array at range
   *                    <code>[from; to)</code> if target symbol is found;
   *                    <code>-1</code> otherwise
   */
  public static int lastIndexOf(@Nonnull char[] buffer, final char symbol, int fromIndex, final int toIndex) {
    return consulo.util.lang.CharArrayUtil.lastIndexOf(buffer, symbol, fromIndex, toIndex);
  }

  public static int lastIndexOf(@Nonnull CharSequence buffer, @Nonnull String pattern, int maxIndex) {
    return consulo.util.lang.CharArrayUtil.lastIndexOf(buffer, pattern, maxIndex);
  }

  public static int lastIndexOf(@Nonnull char[] buffer, @Nonnull String pattern, int maxIndex) {
    return consulo.util.lang.CharArrayUtil.lastIndexOf(buffer, pattern, maxIndex);
  }

  @Nonnull
  public static byte[] toByteArray(@Nonnull char[] chars) throws IOException {
    return consulo.util.lang.CharArrayUtil.toByteArray(chars);
  }

  @Nonnull
  public static byte[] toByteArray(@Nonnull char[] chars, int size) throws IOException {
    return consulo.util.lang.CharArrayUtil.toByteArray(chars, size);
  }

  public static boolean containsOnlyWhiteSpaces(@Nullable CharSequence chars) {
    return consulo.util.lang.CharArrayUtil.containsOnlyWhiteSpaces(chars);
  }

  //Commented in order to apply to green code policy as the method is unused.
  //
  //public static boolean subArraysEqual(char[] ca1, int startOffset1, int endOffset1,char[] ca2, int startOffset2, int endOffset2) {
  //  if (endOffset1 - startOffset1 != endOffset2 - startOffset2) return false;
  //  for (int i = startOffset1; i < endOffset1; i++) {
  //    char c1 = ca1[i];
  //    char c2 = ca2[i - startOffset1 + startOffset2];
  //    if (c1 != c2) return false;
  //  }
  //  return true;
  //}

  @Nonnull
  public static TextRange[] getIndents(@Nonnull CharSequence charsSequence, int shift) {
    List<TextRange> result = new ArrayList<TextRange>();
    int whitespaceEnd = -1;
    int lastTextFound = 0;
    for(int i = charsSequence.length() - 1; i >= 0; i--){
      final char charAt = charsSequence.charAt(i);
      final boolean isWhitespace = Character.isWhitespace(charAt);
      if(charAt == '\n'){
        result.add(new TextRange(i, (whitespaceEnd >= 0 ? whitespaceEnd : i) + 1).shiftRight(shift));
        whitespaceEnd = -1;
      }
      else if(whitespaceEnd >= 0 ){
        if(isWhitespace){
          continue;
        }
        lastTextFound = result.size();
        whitespaceEnd = -1;
      }
      else if(isWhitespace){
        whitespaceEnd = i;
      } else {
        lastTextFound = result.size();
      }
    }
    if(whitespaceEnd > 0) result.add(new TextRange(0, whitespaceEnd + 1).shiftRight(shift));
    if (lastTextFound < result.size()) {
      result = result.subList(0, lastTextFound);
    }
    return result.toArray(new TextRange[result.size()]);
  }

  public static boolean containLineBreaks(@Nonnull CharSequence seq) {
    return consulo.util.lang.CharArrayUtil.containLineBreaks(seq);
  }

  public static boolean containLineBreaks(@Nullable CharSequence seq, int fromOffset, int endOffset) {
    return consulo.util.lang.CharArrayUtil.containLineBreaks(seq, fromOffset, endOffset);
  }

  /**
   * Allows to answer if target region of the given text contains only white space symbols (tabulations, white spaces and line feeds).
   *
   * @param text      text to check
   * @param start     start offset within the given text to check (inclusive)
   * @param end       end offset within the given text to check (exclusive)
   * @return          <code>true</code> if target region of the given text contains white space symbols only; <code>false</code> otherwise
   */
  public static boolean isEmptyOrSpaces(@Nonnull CharSequence text, int start, int end) {
    return consulo.util.lang.CharArrayUtil.isEmptyOrSpaces(text, start, end);
  }

  @Nonnull
  public static Reader readerFromCharSequence(@Nonnull CharSequence text) {
    char[] chars = fromSequenceWithoutCopying(text);
    //noinspection IOResourceOpenedButNotSafelyClosed
    return chars == null ? new CharSequenceReader(text.toString()) : new UnsyncCharArrayReader(chars, 0, text.length());
  }

  @Nonnull
  public static ImmutableCharSequence createImmutableCharSequence(@Nonnull CharSequence sequence) {
    return ImmutableText.valueOf(sequence);
  }
}
