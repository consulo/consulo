/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal;

import consulo.application.progress.DumbProgressIndicator;
import consulo.diff.comparison.ByLine;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.comparison.ComparisonUtil;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.comparison.TrimUtil;
import consulo.diff.comparison.iterable.DiffIterableUtil;
import consulo.diff.comparison.iterable.FairDiffIterable;
import consulo.diff.util.DiffRangeUtil;
import consulo.diff.util.LineOffsets;
import consulo.diff.util.LineOffsetsUtil;
import consulo.diff.util.Range;
import consulo.document.Document;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.internal.VcsRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RangesBuilder {
  public static List<VcsRange> createRanges(List<String> current,
                                            List<String> vcs,
                                            int currentShift,
                                            int vcsShift,
                                            boolean innerWhitespaceChanges) {
    FairDiffIterable iterable = compareLines(vcs, current);

    List<VcsRange> result = new ArrayList<>();
    for (Range range : iterable.iterateChanges()) {
      List<VcsRange.InnerRange> inner = innerWhitespaceChanges
        ? createInnerRanges(vcs.subList(range.start1, range.end1), current.subList(range.start2, range.end2))
        : null;
      result.add(new VcsRange(range.start2 + currentShift, range.end2 + currentShift,
                              range.start1 + vcsShift, range.end1 + vcsShift, inner));
    }
    return result;
  }

  public static List<VcsRange> createRanges(Document current, Document vcs) {
    return createRanges(current.getImmutableCharSequence(), vcs.getImmutableCharSequence(),
                        LineOffsetsUtil.create(current), LineOffsetsUtil.create(vcs));
  }

  public static List<VcsRange> createRanges(CharSequence current, CharSequence vcs) {
    return createRanges(current, vcs, LineOffsetsUtil.create(current), LineOffsetsUtil.create(vcs));
  }

  private static List<VcsRange> createRanges(CharSequence current,
                                             CharSequence vcs,
                                             LineOffsets currentLineOffsets,
                                             LineOffsets vcsLineOffsets) {
    FairDiffIterable iterable = compareLines(vcs, current, vcsLineOffsets, currentLineOffsets);
    return createRanges(iterable);
  }

  public static List<VcsRange> createRanges(FairDiffIterable iterable) {
    List<VcsRange> result = new ArrayList<>();
    for (Range range : iterable.iterateChanges()) {
      result.add(new VcsRange(range.start2, range.end2, range.start1, range.end1));
    }
    return result;
  }

  public static FairDiffIterable compareLines(CharSequence text1,
                                              CharSequence text2,
                                              LineOffsets lineOffsets1,
                                              LineOffsets lineOffsets2) {
    Range range = TrimUtil.expand(text1, text2, 0, 0, text1.length(), text2.length());
    if (range.isEmpty()) {
      return DiffIterableUtil.fair(DiffIterableUtil.create(Collections.emptyList(), lineOffsets1.getLineCount(), lineOffsets2.getLineCount()));
    }

    int contextLines = 5;
    int start = Math.max(lineOffsets1.getLineNumber(range.start1) - contextLines, 0);
    int tail = Math.max(lineOffsets1.getLineCount() - lineOffsets1.getLineNumber(range.end1) - 1 - contextLines, 0);
    Range lineRange = new Range(start, lineOffsets1.getLineCount() - tail, start, lineOffsets2.getLineCount() - tail);

    FairDiffIterable iterable = compareLines(lineRange, text1, text2, lineOffsets1, lineOffsets2);
    return DiffIterableUtil.fair(DiffIterableUtil.expandedIterable(iterable, start, start, lineOffsets1.getLineCount(), lineOffsets2.getLineCount()));
  }

  public static FairDiffIterable compareLines(Range lineRange,
                                              CharSequence text1,
                                              CharSequence text2,
                                              LineOffsets lineOffsets1,
                                              LineOffsets lineOffsets2) {
    List<String> lines1 = DiffRangeUtil.getLines(text1, lineOffsets1, lineRange.start1, lineRange.end1);
    List<String> lines2 = DiffRangeUtil.getLines(text2, lineOffsets2, lineRange.start2, lineRange.end2);
    return compareLines(lines1, lines2);
  }

  public static List<VcsRange.InnerRange> createInnerRanges(Range lineRange,
                                                            CharSequence text1,
                                                            CharSequence text2,
                                                            LineOffsets lineOffsets1,
                                                            LineOffsets lineOffsets2) {
    List<String> lines1 = DiffRangeUtil.getLines(text1, lineOffsets1, lineRange.start1, lineRange.end1);
    List<String> lines2 = DiffRangeUtil.getLines(text2, lineOffsets2, lineRange.start2, lineRange.end2);
    return createInnerRanges(lines1, lines2);
  }

  private static List<VcsRange.InnerRange> createInnerRanges(List<String> lines1, List<String> lines2) {
    FairDiffIterable iwIterable = safeCompareLines(lines1, lines2, ComparisonPolicy.IGNORE_WHITESPACES);

    List<VcsRange.InnerRange> result = new ArrayList<>();
    for (Pair<Range, Boolean> pair : DiffIterableUtil.iterateAll(iwIterable)) {
      Range range = pair.first;
      boolean equals = pair.second;
      result.add(new VcsRange.InnerRange(range.start2, range.end2, getChangeType(range, equals)));
    }
    return result;
  }

  private static byte getChangeType(Range range, boolean equals) {
    if (equals) return VcsRange.EQUAL;
    int deleted = range.end1 - range.start1;
    int inserted = range.end2 - range.start2;
    if (deleted > 0 && inserted > 0) return VcsRange.MODIFIED;
    if (deleted > 0) return VcsRange.DELETED;
    if (inserted > 0) return VcsRange.INSERTED;
    return VcsRange.EQUAL;
  }

  public static FairDiffIterable tryCompareLines(Range lineRange,
                                                 CharSequence text1,
                                                 CharSequence text2,
                                                 LineOffsets lineOffsets1,
                                                 LineOffsets lineOffsets2) {
    List<String> lines1 = DiffRangeUtil.getLines(text1, lineOffsets1, lineRange.start1, lineRange.end1);
    List<String> lines2 = DiffRangeUtil.getLines(text2, lineOffsets2, lineRange.start2, lineRange.end2);
    return tryCompareLines(lines1, lines2);
  }

  public static FairDiffIterable fastCompareLines(Range lineRange,
                                                  CharSequence text1,
                                                  CharSequence text2,
                                                  LineOffsets lineOffsets1,
                                                  LineOffsets lineOffsets2) {
    List<String> lines1 = DiffRangeUtil.getLines(text1, lineOffsets1, lineRange.start1, lineRange.end1);
    List<String> lines2 = DiffRangeUtil.getLines(text2, lineOffsets2, lineRange.start2, lineRange.end2);
    return fastCompareLines(lines1, lines2);
  }

  private static FairDiffIterable compareLines(List<String> lines1, List<String> lines2) {
    FairDiffIterable iwIterable = safeCompareLines(lines1, lines2, ComparisonPolicy.IGNORE_WHITESPACES);
    return processLines(lines1, lines2, iwIterable);
  }

  private static FairDiffIterable tryCompareLines(List<String> lines1, List<String> lines2) {
    FairDiffIterable iwIterable = tryCompareLines(lines1, lines2, ComparisonPolicy.IGNORE_WHITESPACES);
    if (iwIterable == null) return null;
    return processLines(lines1, lines2, iwIterable);
  }

  private static FairDiffIterable fastCompareLines(List<String> lines1, List<String> lines2) {
    FairDiffIterable iwIterable = fastCompareLines(lines1, lines2, ComparisonPolicy.IGNORE_WHITESPACES);
    return processLines(lines1, lines2, iwIterable);
  }

  /**
   * Compare lines, preferring non-optimal but less confusing results for whitespace-only changed lines
   * Ex: "X\n\nY\nZ" vs " X\n Y\n\n Z" should be a single big change, rather than 2 changes separated by "matched" empty line.
   */
  private static FairDiffIterable processLines(List<String> lines1, List<String> lines2, FairDiffIterable iwIterable) {
    DiffIterableUtil.ExpandChangeBuilder builder = new DiffIterableUtil.ExpandChangeBuilder(lines1, lines2);
    for (Range range : iwIterable.iterateUnchanged()) {
      int count = range.end1 - range.start1;
      for (int i = 0; i < count; i++) {
        int index1 = range.start1 + i;
        int index2 = range.start2 + i;
        if (lines1.get(index1).equals(lines2.get(index2))) {
          builder.markEqual(index1, index2);
        }
      }
    }

    return DiffIterableUtil.fair(builder.finish());
  }

  private static FairDiffIterable safeCompareLines(List<String> lines1, List<String> lines2, ComparisonPolicy comparisonPolicy) {
    FairDiffIterable iterable = tryCompareLines(lines1, lines2, comparisonPolicy);
    return iterable != null ? iterable : fastCompareLines(lines1, lines2, comparisonPolicy);
  }

  private static FairDiffIterable tryCompareLines(List<String> lines1, List<String> lines2, ComparisonPolicy comparisonPolicy) {
    try {
      return ByLine.compare(lines1, lines2, comparisonPolicy, DumbProgressIndicator.INSTANCE);
    }
    catch (DiffTooBigException e) {
      return null;
    }
  }

  private static FairDiffIterable fastCompareLines(List<String> lines1, List<String> lines2, ComparisonPolicy comparisonPolicy) {
    Range range = TrimUtil.expand(lines1, lines2, 0, 0, lines1.size(), lines2.size(),
                                  (line1, line2) -> ComparisonUtil.isEquals(line1, line2, comparisonPolicy));
    List<Range> ranges = range.isEmpty() ? Collections.emptyList() : Collections.singletonList(range);
    return DiffIterableUtil.fair(DiffIterableUtil.create(ranges, lines1.size(), lines2.size()));
  }

  public static boolean isValidRanges(CharSequence content1,
                                      CharSequence content2,
                                      LineOffsets lineOffsets1,
                                      LineOffsets lineOffsets2,
                                      List<Range> lineRanges) {
    boolean allRangesValid = lineRanges.stream().allMatch(it ->
      isValidLineRange(lineOffsets1, it.start1, it.end1) &&
        isValidLineRange(lineOffsets2, it.start2, it.end2));
    if (!allRangesValid) return false;

    var iterable = DiffIterableUtil.create(lineRanges, lineOffsets1.getLineCount(), lineOffsets2.getLineCount());
    for (Range range : iterable.iterateUnchanged()) {
      List<String> lines1 = DiffRangeUtil.getLines(content1, lineOffsets1, range.start1, range.end1);
      List<String> lines2 = DiffRangeUtil.getLines(content2, lineOffsets2, range.start2, range.end2);
      if (!lines1.equals(lines2)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isValidLineRange(LineOffsets lineOffsets, int start, int end) {
    return start >= 0 && start <= end && end <= lineOffsets.getLineCount();
  }

}
