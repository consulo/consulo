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
package consulo.diff.comparison;

import consulo.application.progress.ProgressIndicator;
import consulo.diff.comparison.iterable.DiffIterable;
import consulo.diff.comparison.iterable.FairDiffIterable;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.fragment.MergeWordFragment;
import consulo.diff.internal.ComparisonManagerEx;
import consulo.diff.util.MergeRange;
import consulo.diff.util.Range;
import consulo.util.lang.Couple;
import consulo.util.lang.MergingCharSequence;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import static consulo.diff.comparison.TrimUtil.trim;
import static consulo.diff.comparison.TrimUtil.*;
import static consulo.diff.comparison.iterable.DiffIterableUtil.trim;
import static consulo.diff.comparison.iterable.DiffIterableUtil.*;
import static consulo.util.lang.StringUtil.isWhiteSpace;

public class ByWord {
  @Nonnull
  public static List<DiffFragment> compare(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text2,
    @Nonnull ComparisonPolicy policy,
    @Nonnull ProgressIndicator indicator
  ) {
    indicator.checkCanceled();

    List<InlineChunk> words1 = getInlineChunks(text1);
    List<InlineChunk> words2 = getInlineChunks(text2);

    return compare(text1, words1, text2, words2, policy, indicator);
  }

  @Nonnull
  public static List<DiffFragment> compare(
    @Nonnull CharSequence text1,
    @Nonnull List<InlineChunk> words1,
    @Nonnull CharSequence text2,
    @Nonnull List<InlineChunk> words2,
    @Nonnull ComparisonPolicy policy,
    @Nonnull ProgressIndicator indicator
  ) {
    FairDiffIterable wordChanges = diff(words1, words2, indicator);
    wordChanges = optimizeWordChunks(text1, text2, words1, words2, wordChanges, indicator);

    FairDiffIterable delimitersIterable = matchAdjustmentDelimiters(text1, text2, words1, words2, wordChanges, indicator);
    DiffIterable iterable = matchAdjustmentWhitespaces(text1, text2, delimitersIterable, policy, indicator);

    ComparisonManagerEx comparisonManager = (ComparisonManagerEx)ComparisonManager.getInstance();
    return comparisonManager.convertIntoDiffFragments(iterable);
  }

  @Nonnull
  public static List<MergeWordFragment> compare(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text2,
    @Nonnull CharSequence text3,
    @Nonnull ComparisonPolicy policy,
    @Nonnull ProgressIndicator indicator
  ) {
    indicator.checkCanceled();

    List<InlineChunk> words1 = getInlineChunks(text1);
    List<InlineChunk> words2 = getInlineChunks(text2);
    List<InlineChunk> words3 = getInlineChunks(text3);

    FairDiffIterable wordChanges1 = diff(words2, words1, indicator);
    wordChanges1 = optimizeWordChunks(text2, text1, words2, words1, wordChanges1, indicator);
    FairDiffIterable iterable1 = matchAdjustmentDelimiters(text2, text1, words2, words1, wordChanges1, indicator);

    FairDiffIterable wordChanges2 = diff(words2, words3, indicator);
    wordChanges2 = optimizeWordChunks(text2, text3, words2, words3, wordChanges2, indicator);
    FairDiffIterable iterable2 = matchAdjustmentDelimiters(text2, text3, words2, words3, wordChanges2, indicator);

    List<MergeRange> wordConflicts = ComparisonMergeUtil.buildFair(iterable1, iterable2, indicator);
    List<MergeRange> result = matchAdjustmentWhitespaces(text1, text2, text3, wordConflicts, policy, indicator);

    ComparisonManagerEx comparisonManager = (ComparisonManagerEx)ComparisonManager.getInstance();
    return comparisonManager.convertIntoMergeWordFragments(result);
  }

  @Nonnull
  public static List<LineBlock> compareAndSplit(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text2,
    @Nonnull ComparisonPolicy policy,
    @Nonnull ProgressIndicator indicator
  ) {
    indicator.checkCanceled();

    // TODO: figure out, what do we exactly want from 'Split' logic
    // -- it is used for trimming of ignored blocks. So we want whitespace-only leading/trailing lines to be separate block.
    // -- old approach: split by matched '\n's

    // TODO: other approach could lead to better results:
    // * Compare words-only
    // * prefer big chunks
    // -- here we can try to minimize number of matched pairs 'pair[i]' and 'pair[i+1]' such that
    //    containsNewline(pair[i].left .. pair[i+1].left) XOR containsNewline(pair[i].right .. pair[i+1].right) == true
    //    ex: "A X C" - "A Y C \n M C" - do not match with last 'C'
    //    ex: "A \n" - "A B \n \n" - do not match with last '\n'
    //    Try some greedy approach ?
    // * split into blocks
    // -- squash blocks with too small unchanged words count (1 matched word out of 40 - is a bad reason to create new block)
    // * match adjustment punctuation
    // * match adjustment whitespaces ('\n' are matched here)

    List<InlineChunk> words1 = getInlineChunks(text1);
    List<InlineChunk> words2 = getInlineChunks(text2);

    FairDiffIterable wordChanges = diff(words1, words2, indicator);
    wordChanges = optimizeWordChunks(text1, text2, words1, words2, wordChanges, indicator);

    List<LineFragmentSplitter.WordBlock> wordBlocks = new LineFragmentSplitter(text1, text2, words1, words2, wordChanges, indicator).run();

    List<LineBlock> lineBlocks = new ArrayList<>(wordBlocks.size());
    for (LineFragmentSplitter.WordBlock block : wordBlocks) {
      Range offsets = block.offsets;
      Range words = block.words;

      CharSequence subtext1 = text1.subSequence(offsets.start1, offsets.end1);
      CharSequence subtext2 = text2.subSequence(offsets.start2, offsets.end2);

      List<InlineChunk> subwords1 = words1.subList(words.start1, words.end1);
      List<InlineChunk> subwords2 = words2.subList(words.start2, words.end2);

      FairDiffIterable subiterable = fair(trim(wordChanges, words.start1, words.end1, words.start2, words.end2));

      FairDiffIterable delimitersIterable = matchAdjustmentDelimiters(
        subtext1,
        subtext2,
        subwords1,
        subwords2,
        subiterable,
        offsets.start1,
        offsets.start2,
        indicator
      );
      DiffIterable iterable = matchAdjustmentWhitespaces(subtext1, subtext2, delimitersIterable, policy, indicator);

      ComparisonManagerEx comparisonManager = (ComparisonManagerEx)ComparisonManager.getInstance();
      List<DiffFragment> fragments = comparisonManager.convertIntoDiffFragments(iterable);

      int newlines1 = countNewlines(subwords1);
      int newlines2 = countNewlines(subwords2);

      lineBlocks.add(new LineBlock(fragments, offsets, newlines1, newlines2));
    }

    return lineBlocks;
  }

  //
  // Impl
  //

  @Nonnull
  private static FairDiffIterable optimizeWordChunks(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text2,
    @Nonnull List<InlineChunk> words1,
    @Nonnull List<InlineChunk> words2,
    @Nonnull FairDiffIterable iterable,
    @Nonnull ProgressIndicator indicator
  ) {
    return new ChunkOptimizer.WordChunkOptimizer(words1, words2, text1, text2, iterable, indicator).build();
  }

  @Nonnull
  private static FairDiffIterable matchAdjustmentDelimiters(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text2,
    @Nonnull List<InlineChunk> words1,
    @Nonnull List<InlineChunk> words2,
    @Nonnull FairDiffIterable changes,
    @Nonnull ProgressIndicator indicator
  ) {
    return matchAdjustmentDelimiters(text1, text2, words1, words2, changes, 0, 0, indicator);
  }

  @Nonnull
  private static FairDiffIterable matchAdjustmentDelimiters(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text2,
    @Nonnull List<InlineChunk> words1,
    @Nonnull List<InlineChunk> words2,
    @Nonnull FairDiffIterable changes,
    int startShift1,
    int startShift2,
    @Nonnull ProgressIndicator indicator
  ) {
    return new AdjustmentPunctuationMatcher(text1, text2, words1, words2, startShift1, startShift2, changes, indicator).build();
  }

  @Nonnull
  private static DiffIterable matchAdjustmentWhitespaces(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text2,
    @Nonnull FairDiffIterable iterable,
    @Nonnull ComparisonPolicy policy,
    @Nonnull ProgressIndicator indicator
  ) {
    switch (policy) {
      case DEFAULT:
        return new DefaultCorrector(iterable, text1, text2, indicator).build();
      case TRIM_WHITESPACES:
        DiffIterable defaultIterable = new DefaultCorrector(iterable, text1, text2, indicator).build();
        return new TrimSpacesCorrector(defaultIterable, text1, text2, indicator).build();
      case IGNORE_WHITESPACES:
        return new IgnoreSpacesCorrector(iterable, text1, text2, indicator).build();
      default:
        throw new IllegalArgumentException(policy.name());
    }
  }

  @Nonnull
  private static List<MergeRange> matchAdjustmentWhitespaces(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text2,
    @Nonnull CharSequence text3,
    @Nonnull List<MergeRange> conflicts,
    @Nonnull ComparisonPolicy policy,
    @Nonnull ProgressIndicator indicator
  ) {
    switch (policy) {
      case DEFAULT:
        return new MergeDefaultCorrector(conflicts, text1, text2, text3, indicator).build();
      case TRIM_WHITESPACES:
        List<MergeRange> defaultConflicts = new MergeDefaultCorrector(conflicts, text1, text2, text3, indicator).build();
        return new MergeTrimSpacesCorrector(defaultConflicts, text1, text2, text3, indicator).build();
      case IGNORE_WHITESPACES:
        return new MergeIgnoreSpacesCorrector(conflicts, text1, text2, text3, indicator).build();
      default:
        throw new IllegalArgumentException(policy.name());
    }
  }

  //
  // Punctuation matching
  //

  /*
   * sample: "[ X { A ! B } Y ]" "( X ... Y )" will lead to comparison of 3 groups of separators
   *      "["  vs "(",
   *      "{" + "}" vs "..."
   *      "]"  vs ")"
   */
  private static class AdjustmentPunctuationMatcher {
    @Nonnull
    private final CharSequence myText1;
    @Nonnull
    private final CharSequence myText2;
    @Nonnull
    private final List<InlineChunk> myWords1;
    @Nonnull
    private final List<InlineChunk> myWords2;
    @Nonnull
    private final FairDiffIterable myChanges;
    @Nonnull
    private final ProgressIndicator myIndicator;

    private final int myStartShift1;
    private final int myStartShift2;

    private final int myLen1;
    private final int myLen2;

    private final ChangeBuilder myBuilder;

    public AdjustmentPunctuationMatcher(
      @Nonnull CharSequence text1,
      @Nonnull CharSequence text2,
      @Nonnull List<InlineChunk> words1,
      @Nonnull List<InlineChunk> words2,
      int startShift1,
      int startShift2,
      @Nonnull FairDiffIterable changes,
      @Nonnull ProgressIndicator indicator
    ) {
      myText1 = text1;
      myText2 = text2;
      myWords1 = words1;
      myWords2 = words2;
      myStartShift1 = startShift1;
      myStartShift2 = startShift2;

      myChanges = changes;
      myIndicator = indicator;

      myLen1 = text1.length();
      myLen2 = text2.length();

      myBuilder = new ChangeBuilder(myLen1, myLen2);
    }

    @Nonnull
    public FairDiffIterable build() {
      execute();
      return fair(myBuilder.finish());
    }

    int lastStart1;
    int lastStart2;
    int lastEnd1;
    int lastEnd2;

    private void execute() {
      clearLastRange();

      matchForward(-1, -1);

      for (Range ch : myChanges.iterateUnchanged()) {
        int count = ch.end1 - ch.start1;
        for (int i = 0; i < count; i++) {
          int index1 = ch.start1 + i;
          int index2 = ch.start2 + i;

          int start1 = getStartOffset1(index1);
          int start2 = getStartOffset2(index2);
          int end1 = getEndOffset1(index1);
          int end2 = getEndOffset2(index2);

          matchBackward(index1, index2);

          myBuilder.markEqual(start1, start2, end1, end2);

          matchForward(index1, index2);
        }
      }

      matchBackward(myWords1.size(), myWords2.size());
    }

    private void clearLastRange() {
      lastStart1 = -1;
      lastStart2 = -1;
      lastEnd1 = -1;
      lastEnd2 = -1;
    }

    private void matchBackward(int index1, int index2) {
      int start1 = index1 == 0 ? 0 : getEndOffset1(index1 - 1);
      int start2 = index2 == 0 ? 0 : getEndOffset2(index2 - 1);
      int end1 = index1 == myWords1.size() ? myLen1 : getStartOffset1(index1);
      int end2 = index2 == myWords2.size() ? myLen2 : getStartOffset2(index2);

      matchBackward(start1, start2, end1, end2);
      clearLastRange();
    }

    private void matchForward(int index1, int index2) {
      int start1 = index1 == -1 ? 0 : getEndOffset1(index1);
      int start2 = index2 == -1 ? 0 : getEndOffset2(index2);
      int end1 = index1 + 1 == myWords1.size() ? myLen1 : getStartOffset1(index1 + 1);
      int end2 = index2 + 1 == myWords2.size() ? myLen2 : getStartOffset2(index2 + 1);

      matchForward(start1, start2, end1, end2);
    }

    private void matchForward(int start1, int start2, int end1, int end2) {
      assert lastStart1 == -1 && lastStart2 == -1 && lastEnd1 == -1 && lastEnd2 == -1;

      lastStart1 = start1;
      lastStart2 = start2;
      lastEnd1 = end1;
      lastEnd2 = end2;
    }

    private void matchBackward(int start1, int start2, int end1, int end2) {
      assert lastStart1 != -1 && lastStart2 != -1 && lastEnd1 != -1 && lastEnd2 != -1;

      if (lastStart1 == start1 && lastStart2 == start2) { // pair of adjustment matched words, match gap between ("A B" - "A B")
        assert lastEnd1 == end1 && lastEnd2 == end2;

        matchRange(start1, start2, end1, end2);
        return;
      }
      if (lastStart1 < start1 && lastStart2 < start2) { // pair of matched words, with few unmatched ones between ("A X B" - "A Y B")
        assert lastEnd1 <= start1 && lastEnd2 <= start2;

        matchRange(lastStart1, lastStart2, lastEnd1, lastEnd2);
        matchRange(start1, start2, end1, end2);
        return;
      }

      // one side adjustment, and other has non-matched words between ("A B" - "A Y B")
      matchComplexRange(lastStart1, lastStart2, lastEnd1, lastEnd2, start1, start2, end1, end2);
    }

    private void matchRange(int start1, int start2, int end1, int end2) {
      if (start1 == end1 && start2 == end2) return;

      CharSequence sequence1 = myText1.subSequence(start1, end1);
      CharSequence sequence2 = myText2.subSequence(start2, end2);

      DiffIterable changes = ByChar.comparePunctuation(sequence1, sequence2, myIndicator);

      for (Range ch : changes.iterateUnchanged()) {
        myBuilder.markEqual(start1 + ch.start1, start2 + ch.start2, start1 + ch.end1, start2 + ch.end2);
      }
    }

    private void matchComplexRange(int start11, int start12, int end11, int end12, int start21, int start22, int end21, int end22) {
      if (start11 == start21 && end11 == end21) {
        matchComplexRangeLeft(start11, end11, start12, end12, start22, end22);
      }
      else if (start12 == start22 && end12 == end22) {
        matchComplexRangeRight(start12, end12, start11, end11, start21, end21);
      }
      else {
        throw new IllegalStateException();
      }
    }

    private void matchComplexRangeLeft(int start1, int end1, int start12, int end12, int start22, int end22) {
      CharSequence sequence1 = myText1.subSequence(start1, end1);
      CharSequence sequence21 = myText2.subSequence(start12, end12);
      CharSequence sequence22 = myText2.subSequence(start22, end22);

      Couple<FairDiffIterable> changes = comparePunctuation2Side(sequence1, sequence21, sequence22, myIndicator);

      for (Range ch : changes.first.iterateUnchanged()) {
        myBuilder.markEqual(start1 + ch.start1, start12 + ch.start2, start1 + ch.end1, start12 + ch.end2);
      }
      for (Range ch : changes.second.iterateUnchanged()) {
        myBuilder.markEqual(start1 + ch.start1, start22 + ch.start2, start1 + ch.end1, start22 + ch.end2);
      }
    }

    private void matchComplexRangeRight(int start2, int end2, int start11, int end11, int start21, int end21) {
      CharSequence sequence11 = myText1.subSequence(start11, end11);
      CharSequence sequence12 = myText1.subSequence(start21, end21);
      CharSequence sequence2 = myText2.subSequence(start2, end2);

      Couple<FairDiffIterable> changes = comparePunctuation2Side(sequence2, sequence11, sequence12, myIndicator);

      // Mirrored ch.*1 and ch.*2 as we use "compare2Side" that works with 2 right side, while we have 2 left here
      for (Range ch : changes.first.iterateUnchanged()) {
        myBuilder.markEqual(start11 + ch.start2, start2 + ch.start1, start11 + ch.end2, start2 + ch.end1);
      }
      for (Range ch : changes.second.iterateUnchanged()) {
        myBuilder.markEqual(start21 + ch.start2, start2 + ch.start1, start21 + ch.end2, start2 + ch.end1);
      }
    }

    private int getStartOffset1(int index) {
      return myWords1.get(index).getOffset1() - myStartShift1;
    }

    private int getStartOffset2(int index) {
      return myWords2.get(index).getOffset1() - myStartShift2;
    }

    private int getEndOffset1(int index) {
      return myWords1.get(index).getOffset2() - myStartShift1;
    }

    private int getEndOffset2(int index) {
      return myWords2.get(index).getOffset2() - myStartShift2;
    }
  }

  /*
   * Compare one char sequence with two others (as if they were single sequence)
   *
   * Return two DiffIterable: (0, len1) - (0, len21) and (0, len1) - (0, len22)
   */
  @Nonnull
  private static Couple<FairDiffIterable> comparePunctuation2Side(
    @Nonnull CharSequence text1,
    @Nonnull CharSequence text21,
    @Nonnull CharSequence text22,
    @Nonnull ProgressIndicator indicator
  ) {
    CharSequence text2 = new MergingCharSequence(text21, text22);
    FairDiffIterable changes = ByChar.comparePunctuation(text1, text2, indicator);

    Couple<List<Range>> ranges = splitIterable2Side(changes, text21.length());

    FairDiffIterable iterable1 = fair(createUnchanged(ranges.first, text1.length(), text21.length()));
    FairDiffIterable iterable2 = fair(createUnchanged(ranges.second, text1.length(), text22.length()));
    return Couple.of(iterable1, iterable2);
  }

  @Nonnull
  private static Couple<List<Range>> splitIterable2Side(@Nonnull FairDiffIterable changes, int offset) {
    final List<Range> ranges1 = new ArrayList<>();
    final List<Range> ranges2 = new ArrayList<>();
    for (Range ch : changes.iterateUnchanged()) {
      if (ch.end2 <= offset) {
        ranges1.add(new Range(ch.start1, ch.end1, ch.start2, ch.end2));
      }
      else if (ch.start2 >= offset) {
        ranges2.add(new Range(ch.start1, ch.end1, ch.start2 - offset, ch.end2 - offset));
      }
      else {
        int len2 = offset - ch.start2;
        ranges1.add(new Range(ch.start1, ch.start1 + len2, ch.start2, offset));
        ranges2.add(new Range(ch.start1 + len2, ch.end1, 0, ch.end2 - offset));
      }
    }
    return Couple.of(ranges1, ranges2);
  }

  //
  // Whitespaces matching
  //

  private static class DefaultCorrector {
    @Nonnull
    private final DiffIterable myIterable;
    @Nonnull
    private final CharSequence myText1;
    @Nonnull
    private final CharSequence myText2;
    @Nonnull
    private final ProgressIndicator myIndicator;

    @Nonnull
    private final List<Range> myChanges;

    public DefaultCorrector(
      @Nonnull DiffIterable iterable,
      @Nonnull CharSequence text1,
      @Nonnull CharSequence text2,
      @Nonnull ProgressIndicator indicator
    ) {
      myIterable = iterable;
      myText1 = text1;
      myText2 = text2;
      myIndicator = indicator;

      myChanges = new ArrayList<>();
    }

    @Nonnull
    public DiffIterable build() {
      for (Range range : myIterable.iterateChanges()) {
        int endCut = expandBackwardW(myText1, myText2, range.start1, range.start2, range.end1, range.end2);
        int startCut = expandForwardW(myText1, myText2, range.start1, range.start2, range.end1 - endCut, range.end2 - endCut);

        Range expand =
          new Range(range.start1 + startCut, range.end1 - endCut, range.start2 + startCut, range.end2 - endCut);

        if (!expand.isEmpty()) {
          myChanges.add(expand);
        }
      }

      return create(myChanges, myText1.length(), myText2.length());
    }
  }

  private static class MergeDefaultCorrector {
    @Nonnull
    private final List<MergeRange> myIterable;
    @Nonnull
    private final CharSequence myText1;
    @Nonnull
    private final CharSequence myText2;
    @Nonnull
    private final CharSequence myText3;
    @Nonnull
    private final ProgressIndicator myIndicator;

    @Nonnull
    private final List<MergeRange> myChanges;

    public MergeDefaultCorrector(
      @Nonnull List<MergeRange> iterable,
      @Nonnull CharSequence text1,
      @Nonnull CharSequence text2,
      @Nonnull CharSequence text3,
      @Nonnull ProgressIndicator indicator
    ) {
      myIterable = iterable;
      myText1 = text1;
      myText2 = text2;
      myText3 = text3;
      myIndicator = indicator;

      myChanges = new ArrayList<>();
    }

    @Nonnull
    public List<MergeRange> build() {
      for (MergeRange range : myIterable) {
        int endCut = expandBackwardW(myText1, myText2, myText3,
                                     range.start1, range.start2, range.start3,
                                     range.end1, range.end2, range.end3);
        int startCut = expandForwardW(myText1, myText2, myText3,
                                      range.start1, range.start2, range.start3,
                                      range.end1 - endCut, range.end2 - endCut, range.end3 - endCut);

        MergeRange expand = new MergeRange(
          range.start1 + startCut, range.end1 - endCut,
          range.start2 + startCut, range.end2 - endCut,
          range.start3 + startCut, range.end3 - endCut
        );

        if (!expand.isEmpty()) {
          myChanges.add(expand);
        }
      }

      return myChanges;
    }
  }

  private static class IgnoreSpacesCorrector {
    @Nonnull
    private final DiffIterable myIterable;
    @Nonnull
    private final CharSequence myText1;
    @Nonnull
    private final CharSequence myText2;
    @Nonnull
    private final ProgressIndicator myIndicator;

    @Nonnull
    private final List<Range> myChanges;

    public IgnoreSpacesCorrector(
      @Nonnull DiffIterable iterable,
      @Nonnull CharSequence text1,
      @Nonnull CharSequence text2,
      @Nonnull ProgressIndicator indicator
    ) {
      myIterable = iterable;
      myText1 = text1;
      myText2 = text2;
      myIndicator = indicator;

      myChanges = new ArrayList<>();
    }

    @Nonnull
    public DiffIterable build() {
      for (Range range : myIterable.iterateChanges()) {
        // match spaces if we can, ignore them if we can't
        Range expanded = expandW(myText1, myText2, range);
        Range trimmed = trim(myText1, myText2, expanded);

        if (!trimmed.isEmpty()) {
          myChanges.add(trimmed);
        }
      }

      return create(myChanges, myText1.length(), myText2.length());
    }
  }

  private static class MergeIgnoreSpacesCorrector {
    @Nonnull
    private final List<MergeRange> myIterable;
    @Nonnull
    private final CharSequence myText1;
    @Nonnull
    private final CharSequence myText2;
    @Nonnull
    private final CharSequence myText3;
    @Nonnull
    private final ProgressIndicator myIndicator;

    @Nonnull
    private final List<MergeRange> myChanges;

    public MergeIgnoreSpacesCorrector(
      @Nonnull List<MergeRange> iterable,
      @Nonnull CharSequence text1,
      @Nonnull CharSequence text2,
      @Nonnull CharSequence text3,
      @Nonnull ProgressIndicator indicator
    ) {
      myIterable = iterable;
      myText1 = text1;
      myText2 = text2;
      myText3 = text3;
      myIndicator = indicator;

      myChanges = new ArrayList<>();
    }

    @Nonnull
    public List<MergeRange> build() {
      for (MergeRange range : myIterable) {
        MergeRange expanded = expandW(myText1, myText2, myText3, range);
        MergeRange trimmed = trim(myText1, myText2, myText3, expanded);

        if (!trimmed.isEmpty()) {
          myChanges.add(trimmed);
        }
      }

      return myChanges;
    }
  }

  private static class TrimSpacesCorrector {
    @Nonnull
    private final DiffIterable myIterable;
    @Nonnull
    private final CharSequence myText1;
    @Nonnull
    private final CharSequence myText2;
    @Nonnull
    private final ProgressIndicator myIndicator;

    @Nonnull
    private final List<Range> myChanges;

    public TrimSpacesCorrector(
      @Nonnull DiffIterable iterable,
      @Nonnull CharSequence text1,
      @Nonnull CharSequence text2,
      @Nonnull ProgressIndicator indicator
    ) {
      myIterable = iterable;
      myText1 = text1;
      myText2 = text2;
      myIndicator = indicator;

      myChanges = new ArrayList<>();
    }

    @Nonnull
    public DiffIterable build() {
      for (Range range : myIterable.iterateChanges()) {
        int start1 = range.start1;
        int start2 = range.start2;
        int end1 = range.end1;
        int end2 = range.end2;

        if (isLeadingTrailingSpace(myText1, start1)) {
          start1 = trimStart(myText1, start1, end1);
        }
        if (isLeadingTrailingSpace(myText1, end1 - 1)) {
          end1 = trimEnd(myText1, start1, end1);
        }
        if (isLeadingTrailingSpace(myText2, start2)) {
          start2 = trimStart(myText2, start2, end2);
        }
        if (isLeadingTrailingSpace(myText2, end2 - 1)) {
          end2 = trimEnd(myText2, start2, end2);
        }

        Range trimmed = new Range(start1, end1, start2, end2);

        if (!trimmed.isEmpty()) {
          myChanges.add(trimmed);
        }
      }

      return create(myChanges, myText1.length(), myText2.length());
    }
  }

  private static class MergeTrimSpacesCorrector {
    @Nonnull
    private final List<MergeRange> myIterable;
    @Nonnull
    private final CharSequence myText1;
    @Nonnull
    private final CharSequence myText2;
    @Nonnull
    private final CharSequence myText3;
    @Nonnull
    private final ProgressIndicator myIndicator;

    @Nonnull
    private final List<MergeRange> myChanges;

    public MergeTrimSpacesCorrector(
      @Nonnull List<MergeRange> iterable,
      @Nonnull CharSequence text1,
      @Nonnull CharSequence text2,
      @Nonnull CharSequence text3,
      @Nonnull ProgressIndicator indicator
    ) {
      myIterable = iterable;
      myText1 = text1;
      myText2 = text2;
      myText3 = text3;
      myIndicator = indicator;

      myChanges = new ArrayList<>();
    }

    @Nonnull
    public List<MergeRange> build() {
      for (MergeRange range : myIterable) {
        int start1 = range.start1;
        int start2 = range.start2;
        int start3 = range.start3;
        int end1 = range.end1;
        int end2 = range.end2;
        int end3 = range.end3;

        if (isLeadingTrailingSpace(myText1, start1)) {
          start1 = trimStart(myText1, start1, end1);
        }
        if (isLeadingTrailingSpace(myText1, end1 - 1)) {
          end1 = trimEnd(myText1, start1, end1);
        }
        if (isLeadingTrailingSpace(myText2, start2)) {
          start2 = trimStart(myText2, start2, end2);
        }
        if (isLeadingTrailingSpace(myText2, end2 - 1)) {
          end2 = trimEnd(myText2, start2, end2);
        }
        if (isLeadingTrailingSpace(myText3, start3)) {
          start3 = trimStart(myText3, start3, end3);
        }
        if (isLeadingTrailingSpace(myText3, end3 - 1)) {
          end3 = trimEnd(myText3, start3, end3);
        }

        MergeRange trimmed = new MergeRange(start1, end1, start2, end2, start3, end3);

        if (!trimmed.isEmpty()) {
          myChanges.add(trimmed);
        }
      }

      return myChanges;
    }
  }

  private static boolean isLeadingTrailingSpace(@Nonnull CharSequence text, int start) {
    return isLeadingSpace(text, start) || isTrailingSpace(text, start);
  }

  private static boolean isLeadingSpace(@Nonnull CharSequence text, int start) {
    if (start < 0) return false;
    if (start == text.length()) return false;
    if (!isWhiteSpace(text.charAt(start))) return false;

    start--;
    while (start >= 0) {
      char c = text.charAt(start);
      if (c == '\n') return true;
      if (!isWhiteSpace(c)) return false;
      start--;
    }
    return true;
  }

  private static boolean isTrailingSpace(@Nonnull CharSequence text, int end) {
    if (end < 0) return false;
    if (end == text.length()) return false;
    if (!isWhiteSpace(text.charAt(end))) return false;

    while (end < text.length()) {
      char c = text.charAt(end);
      if (c == '\n') return true;
      if (!isWhiteSpace(c)) return false;
      end++;
    }
    return true;
  }

  //
  // Misc
  //

  private static int countNewlines(@Nonnull List<InlineChunk> words) {
    int count = 0;
    for (InlineChunk word : words) {
      if (word instanceof NewlineChunk) count++;
    }
    return count;
  }

  @Nonnull
  public static List<InlineChunk> getInlineChunks(@Nonnull final CharSequence text) {
    final List<InlineChunk> chunks = new ArrayList<>();

    final int len = text.length();

    int offset = 0;
    while (offset < len) {
      char ch = text.charAt(offset);

      if (isAlpha(ch)) {
        int startOffset = offset;

        int h = 0;
        while (offset < len) {
          char c = text.charAt(offset);
          if (!isAlpha(c)) break;
          h = 31 * h + c;
          offset++;
        }

        chunks.add(new WordChunk(text, startOffset, offset, h));
      }
      else {
        while (offset < len) {
          char c = text.charAt(offset);
          if (isAlpha(c)) break;
          if (c == '\n') chunks.add(new NewlineChunk(offset));
          offset++;
        }
      }
    }

    return chunks;
  }

  //
  // Helpers
  //

  public interface InlineChunk {
    int getOffset1();

    int getOffset2();
  }

  static class WordChunk implements InlineChunk {
    @Nonnull
    private final CharSequence myText;
    private final int myOffset1;
    private final int myOffset2;
    private final int myHash;

    public WordChunk(@Nonnull CharSequence text, int offset1, int offset2, int hash) {
      myText = text;
      myOffset1 = offset1;
      myOffset2 = offset2;
      myHash = hash;
    }

    @Nonnull
    public CharSequence getContent() {
      return myText.subSequence(myOffset1, myOffset2);
    }

    public int getOffset1() {
      return myOffset1;
    }

    public int getOffset2() {
      return myOffset2;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      WordChunk word = (WordChunk)o;

      if (myHash != word.myHash) return false;

      return StringUtil.equals(getContent(), word.getContent());
    }

    @Override
    public int hashCode() {
      return myHash;
    }
  }

  static class NewlineChunk implements InlineChunk {
    private final int myOffset;

    public NewlineChunk(int offset) {
      myOffset = offset;
    }

    @Override
    public int getOffset1() {
      return myOffset;
    }

    @Override
    public int getOffset2() {
      return myOffset + 1;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }
  }

  public static class LineBlock {
    @Nonnull
    public final List<DiffFragment> fragments;

    @Nonnull
    public final Range offsets;

    public final int newlines1;
    public final int newlines2;

    public LineBlock(@Nonnull List<DiffFragment> fragments, @Nonnull Range offsets, int newlines1, int newlines2) {
      this.fragments = fragments;
      this.offsets = offsets;
      this.newlines1 = newlines1;
      this.newlines2 = newlines2;
    }
  }
}
