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
import consulo.diff.comparison.ByWord.InlineChunk;
import consulo.diff.comparison.ByWord.NewlineChunk;
import consulo.diff.comparison.iterable.FairDiffIterable;
import consulo.diff.util.Range;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/*
 * Given matchings on words, split initial line block into 'logically different' line blocks
 */
class LineFragmentSplitter {
  @Nonnull
  private final CharSequence myText1;
  @Nonnull
  private final CharSequence myText2;

  @Nonnull
  private final List<InlineChunk> myWords1;
  @Nonnull
  private final List<InlineChunk> myWords2;
  @Nonnull
  private final FairDiffIterable myIterable;
  @Nonnull
  private final ProgressIndicator myIndicator;

  @Nonnull
  private final List<WordBlock> myResult = new ArrayList<>();

  public LineFragmentSplitter(@Nonnull CharSequence text1,
                              @Nonnull CharSequence text2,
                              @Nonnull List<InlineChunk> words1,
                              @Nonnull List<InlineChunk> words2,
                              @Nonnull FairDiffIterable iterable,
                              @Nonnull ProgressIndicator indicator) {
    myText1 = text1;
    myText2 = text2;
    myWords1 = words1;
    myWords2 = words2;
    myIterable = iterable;
    myIndicator = indicator;
  }

  private int last1 = -1;
  private int last2 = -1;

  private boolean lastHasEqualWords = false;
  private boolean hasEqualWords = false;

  // indexes here are a bit tricky
  // -1 - the beginning of file, words.size() - end of file, everything in between - InlineChunks (words or newlines)

  @Nonnull
  public List<WordBlock> run() {
    for (Range range : myIterable.iterateUnchanged()) {
      int count = range.end1 - range.start1;
      for (int i = 0; i < count; i++) {
        int index1 = range.start1 + i;
        int index2 = range.start2 + i;

        if (isNewline(myWords1, index1) && isNewline(myWords2, index2)) { // split by matched newlines
          addLineChunk(index1, index2);
        }
        else {
          if (isFirstInLine(myWords1, index1) && isFirstInLine(myWords2, index2)) { // split by matched first word in line
            addLineChunk(index1 - 1, index2 - 1);
          }
          // TODO: split by 'last word in line' + 'last word in whole sequence' ?
          hasEqualWords = true;
        }
      }
    }
    addLineChunk(myWords1.size(), myWords2.size());

    return myResult;
  }

  private void addLineChunk(int end1, int end2) {
    if (last1 > end1 || last2 > end2) return;

    WordBlock block = createBlock(last1, last2, end1, end2);
    if (block.offsets.isEmpty()) return;

    WordBlock lastBlock = ContainerUtil.getLastItem(myResult);

    if (lastBlock != null && shouldMergeBlocks(lastBlock, block)) {
      myResult.remove(myResult.size() - 1);
      myResult.add(mergeBlocks(lastBlock, block));
      lastHasEqualWords = hasEqualWords || lastHasEqualWords;
    }
    else {
      myResult.add(block);
      lastHasEqualWords = hasEqualWords;
    }

    hasEqualWords = false;
    last1 = end1;
    last2 = end2;
  }

  @Nonnull
  private WordBlock createBlock(int start1, int start2, int end1, int end2) {
    int startOffset1 = getOffset(myWords1, myText1, start1);
    int startOffset2 = getOffset(myWords2, myText2, start2);
    int endOffset1 = getOffset(myWords1, myText1, end1);
    int endOffset2 = getOffset(myWords2, myText2, end2);

    start1 = Math.max(0, start1 + 1);
    start2 = Math.max(0, start2 + 1);
    end1 = Math.min(end1 + 1, myWords1.size());
    end2 = Math.min(end2 + 1, myWords2.size());

    return new WordBlock(new Range(start1, end1, start2, end2), new Range(startOffset1, endOffset1, startOffset2, endOffset2));
  }

  private boolean shouldMergeBlocks(@Nonnull WordBlock lastBlock, @Nonnull WordBlock newBlock) {
    if (!lastHasEqualWords && !hasEqualWords) return true; // combine lines, that matched only by '\n'
    if (isEqualsIgnoreWhitespace(newBlock) && isEqualsIgnoreWhitespace(lastBlock)) return true; // combine whitespace-only changed lines
    if (noWordsInside(lastBlock) || noWordsInside(newBlock)) return true; // squash block without words in it
    return false;
  }

  private boolean isEqualsIgnoreWhitespace(@Nonnull WordBlock block) {
    CharSequence sequence1 = myText1.subSequence(block.offsets.start1, block.offsets.end1);
    CharSequence sequence2 = myText2.subSequence(block.offsets.start2, block.offsets.end2);

    return StringUtil.equalsIgnoreWhitespaces(sequence1, sequence2);
  }

  @Nonnull
  private static WordBlock mergeBlocks(@Nonnull WordBlock start, @Nonnull WordBlock end) {
    return new WordBlock(new Range(start.words.start1, end.words.end1, start.words.start2, end.words.end2),
                         new Range(start.offsets.start1, end.offsets.end1, start.offsets.start2, end.offsets.end2));
  }

  private static int getOffset(@Nonnull List<InlineChunk> words, @Nonnull CharSequence text, int index) {
    if (index == -1) return 0;
    if (index == words.size()) return text.length();
    InlineChunk chunk = words.get(index);
    assert chunk instanceof NewlineChunk;
    return chunk.getOffset2();
  }

  private static boolean isNewline(@Nonnull List<InlineChunk> words1, int index) {
    return words1.get(index) instanceof NewlineChunk;
  }

  private static boolean isFirstInLine(@Nonnull List<InlineChunk> words1, int index) {
    if (index == 0) return true;
    return words1.get(index - 1) instanceof NewlineChunk;
  }

  private boolean noWordsInside(@Nonnull WordBlock block) {
    for (int i = block.words.start1; i < block.words.end1; i++) {
      if (!(myWords1.get(i) instanceof NewlineChunk)) return false;
    }
    for (int i = block.words.start2; i < block.words.end2; i++) {
      if (!(myWords2.get(i) instanceof NewlineChunk)) return false;
    }
    return true;
  }

  //
  // Helpers
  //

  public static class WordBlock {
    @Nonnull
    public final Range words;
    @Nonnull
    public final Range offsets;

    public WordBlock(@Nonnull Range words, @Nonnull Range offsets) {
      this.words = words;
      this.offsets = offsets;
    }
  }
}
