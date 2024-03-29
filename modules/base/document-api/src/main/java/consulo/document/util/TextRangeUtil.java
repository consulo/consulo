/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.document.util;

import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Miscellaneous utility methods to manipulate lists of text ranges.
 *
 * @see TextRange
 */
public class TextRangeUtil {

  public static final Comparator<TextRange> RANGE_COMPARATOR = (range1, range2) -> {
    int startOffsetDiff = range1.getStartOffset() - range2.getStartOffset();
    return startOffsetDiff != 0 ? startOffsetDiff : range1.getEndOffset() - range2.getEndOffset();
  };

  private TextRangeUtil() {
  }

  /**
   * Excludes ranges from the original range. For example, if the original range is [30..100] and ranges to exclude are
   * [20..50] and [60..90], resulting ranges will be [50..60] and [90..100]. The ranges may overlap and follow in any order. In the latter
   * case the original list of excluded ranges is sorted by start/end offset.
   *
   * @param original       The original range to exclude the ranges from.
   * @param excludedRanges The list of ranges to exclude.
   * @return A list of ranges after excluded ranges have been applied.
   */
  public static Iterable<TextRange> excludeRanges(@Nonnull TextRange original, @Nonnull List<? extends TextRange> excludedRanges) {
    if (!excludedRanges.isEmpty()) {
      if (excludedRanges.size() > 1) {
        excludedRanges.sort(RANGE_COMPARATOR);
      }
      int enabledRangeStart = original.getStartOffset();
      List<TextRange> enabledRanges = new ArrayList<>();
      for (TextRange excludedRange : excludedRanges) {
        if (excludedRange.getEndOffset() < enabledRangeStart) continue;
        int excludedRangeStart = excludedRange.getStartOffset();
        if (excludedRangeStart > original.getEndOffset()) break;
        if (excludedRangeStart > enabledRangeStart) {
          enabledRanges.add(new TextRange(enabledRangeStart, excludedRangeStart));
        }
        enabledRangeStart = excludedRange.getEndOffset();
      }
      if (enabledRangeStart < original.getEndOffset()) {
        enabledRanges.add(new TextRange(enabledRangeStart, original.getEndOffset()));
      }
      return enabledRanges;
    }
    return Collections.singletonList(original);
  }

  /**
   * Return least text range that contains all of passed text ranges.
   * For example for {[0, 3],[3, 7],[10, 17]} this method will return [0, 17]
   *
   * @param textRanges The list of ranges to process
   * @return least text range that contains all of passed text ranges
   */
  @Nonnull
  public static TextRange getEnclosingTextRange(@Nonnull List<? extends TextRange> textRanges) {
    if (textRanges.isEmpty()) return TextRange.EMPTY_RANGE;
    int lowerBound = textRanges.get(0).getStartOffset();
    int upperBound = textRanges.get(0).getEndOffset();
    for (int i = 1; i < textRanges.size(); ++i) {
      TextRange textRange = textRanges.get(i);
      lowerBound = Math.min(lowerBound, textRange.getStartOffset());
      upperBound = Math.max(upperBound, textRange.getEndOffset());
    }
    return new TextRange(lowerBound, upperBound);
  }

  /**
   * Checks that the given range intersects one of the ranges in the list by performing a binary search.
   *
   * @param range     The range to check.
   * @param rangeList The range list. <b>The list must be ordered by range start offset.</b>
   * @return True if the range intersects at least one range in the list.
   */
  public static boolean intersectsOneOf(TextRange range, List<? extends TextRange> rangeList) {
    return rangesContain(rangeList, range.getStartOffset()) || rangesContain(rangeList, range.getEndOffset());
  }

  /**
   * Checks that the given offset is contained in one of the ranges in the list by performing a binary search.
   *
   * @param range     The range to check.
   * @param rangeList The range list. <b>The list must be ordered by range start offset.</b>
   * @return True if the range intersects at least one range in the list.
   */
  public static boolean rangesContain(List<? extends TextRange> rangeList, int offset) {
    return rangesContain(rangeList, 0, rangeList.size() - 1, offset);
  }

  static boolean rangesContain(List<? extends TextRange> ranges, int startIndex, int endIndex, int offset) {
    if (endIndex < startIndex || ranges.size() <= startIndex || ranges.size() <= endIndex) return false;
    int startOffset = ranges.get(startIndex).getStartOffset();
    int endOffset = ranges.get(endIndex).getEndOffset();
    if (offset < startOffset || offset > endOffset) return false;
    if (startIndex == endIndex) return true;
    int midIndex = (endIndex + startIndex) / 2;
    return rangesContain(ranges, startIndex, midIndex, offset) || rangesContain(ranges, midIndex + 1, endIndex, offset);
  }

  public static int getDistance(@Nonnull Segment r2, @Nonnull Segment r1) {
    int s1 = r1.getStartOffset();
    int e1 = r1.getEndOffset();
    int s2 = r2.getStartOffset();
    int e2 = r2.getEndOffset();
    return Math.max(s1, s2) <= Math.min(e1, e2) ? 0 : Math.min(Math.abs(s1 - e2), Math.abs(s2 - e1));
  }

  @Nonnull
  @Contract(pure = true)
  public static List<TextRange> getWordIndicesIn(@Nonnull String text) {
    List<TextRange> result = new ArrayList<>();
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
}
