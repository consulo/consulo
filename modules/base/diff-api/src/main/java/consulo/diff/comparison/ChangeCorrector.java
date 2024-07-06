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

import consulo.diff.comparison.ByLine.Line;
import consulo.diff.comparison.iterable.DiffIterableUtil;
import consulo.diff.comparison.iterable.FairDiffIterable;
import consulo.diff.util.Range;
import consulo.application.progress.ProgressIndicator;
import consulo.util.collection.primitive.ints.IntList;

import jakarta.annotation.Nonnull;
import java.util.List;

import static consulo.diff.comparison.TrimUtil.expand;
import static consulo.diff.comparison.iterable.DiffIterableUtil.diff;
import static consulo.diff.comparison.iterable.DiffIterableUtil.fair;

/*
 * Base class for two-step diff algorithms.
 * Given matching between some sub-sequences of base sequences - build matching on whole base sequences
 */
abstract class ChangeCorrector {
  private final int myLength1;
  private final int myLength2;
  @Nonnull
  private final FairDiffIterable myChanges;

  @Nonnull
  protected final ProgressIndicator myIndicator;

  @Nonnull
  protected final DiffIterableUtil.ChangeBuilder myBuilder;

  public ChangeCorrector(int length1,
                         int length2,
                         @Nonnull FairDiffIterable changes,
                         @Nonnull ProgressIndicator indicator) {
    myLength1 = length1;
    myLength2 = length2;
    myChanges = changes;
    myIndicator = indicator;

    myBuilder = new DiffIterableUtil.ChangeBuilder(length1, length2);
  }

  @Nonnull
  public FairDiffIterable build() {
    execute();
    return fair(myBuilder.finish());
  }

  protected void execute() {
    int last1 = 0;
    int last2 = 0;

    for (Range ch : myChanges.iterateUnchanged()) {
      int count = ch.end1 - ch.start1;
      for (int i = 0; i < count; i++) {
        int index1 = getOriginalIndex1(ch.start1 + i);
        int index2 = getOriginalIndex2(ch.start2 + i);

        matchGap(last1, index1, last2, index2);
        myBuilder.markEqual(index1, index2);

        last1 = index1 + 1;
        last2 = index2 + 1;
      }
    }
    matchGap(last1, myLength1, last2, myLength2);
  }

  // match elements in range [start1 - end1) -> [start2 - end2)
  protected abstract void matchGap(int start1, int end1, int start2, int end2);

  protected abstract int getOriginalIndex1(int index);

  protected abstract int getOriginalIndex2(int index);

  //
  // Implementations
  //

  public static class DefaultCharChangeCorrector extends ChangeCorrector {
    @Nonnull
    private final ByChar.CharOffsets myChars1;
    @Nonnull
    private final ByChar.CharOffsets myChars2;
    @Nonnull
    private final CharSequence myText1;
    @Nonnull
    private final CharSequence myText2;

    public DefaultCharChangeCorrector(@Nonnull ByChar.CharOffsets chars1,
                                      @Nonnull ByChar.CharOffsets chars2,
                                      @Nonnull CharSequence text1,
                                      @Nonnull CharSequence text2,
                                      @Nonnull FairDiffIterable changes,
                                      @Nonnull ProgressIndicator indicator) {
      super(text1.length(), text2.length(), changes, indicator);
      myChars1 = chars1;
      myChars2 = chars2;
      myText1 = text1;
      myText2 = text2;
    }

    @Override
    protected void matchGap(int start1, int end1, int start2, int end2) {
      Range expand = expand(myText1, myText2, start1, start2, end1, end2);

      CharSequence inner1 = myText1.subSequence(expand.start1, expand.end1);
      CharSequence inner2 = myText2.subSequence(expand.start2, expand.end2);
      FairDiffIterable innerChanges = ByChar.compare(inner1, inner2, myIndicator);

      myBuilder.markEqual(start1, start2, expand.start1, expand.start2);

      for (Range chunk : innerChanges.iterateUnchanged()) {
        myBuilder.markEqual(expand.start1 + chunk.start1, expand.start2 + chunk.start2, chunk.end1 - chunk.start1);
      }

      myBuilder.markEqual(expand.end1, expand.end2, end1, end2);
    }

    @Override
    protected int getOriginalIndex1(int index) {
      return myChars1.offsets[index];
    }

    @Override
    protected int getOriginalIndex2(int index) {
      return myChars2.offsets[index];
    }
  }

  public static class SmartLineChangeCorrector extends ChangeCorrector {
    @Nonnull
    private final IntList myIndexes1;
    @Nonnull
    private final IntList myIndexes2;
    @Nonnull
    private final List<Line> myLines1;
    @Nonnull
    private final List<Line> myLines2;

    public SmartLineChangeCorrector(@Nonnull IntList indexes1,
                                    @Nonnull IntList indexes2,
                                    @Nonnull List<Line> lines1,
                                    @Nonnull List<Line> lines2,
                                    @Nonnull FairDiffIterable changes,
                                    @Nonnull ProgressIndicator indicator) {
      super(lines1.size(), lines2.size(), changes, indicator);
      myIndexes1 = indexes1;
      myIndexes2 = indexes2;
      myLines1 = lines1;
      myLines2 = lines2;
    }

    @Override
    protected void matchGap(int start1, int end1, int start2, int end2) {
      Range expand = expand(myLines1, myLines2, start1, start2, end1, end2);

      List<Line> inner1 = myLines1.subList(expand.start1, expand.end1);
      List<Line> inner2 = myLines2.subList(expand.start2, expand.end2);
      FairDiffIterable innerChanges = diff(inner1, inner2, myIndicator);

      myBuilder.markEqual(start1, start2, expand.start1, expand.start2);

      for (Range chunk : innerChanges.iterateUnchanged()) {
        myBuilder.markEqual(expand.start1 + chunk.start1, expand.start2 + chunk.start2, chunk.end1 - chunk.start1);
      }

      myBuilder.markEqual(expand.end1, expand.end2, end1, end2);
    }

    @Override
    protected int getOriginalIndex1(int index) {
      return myIndexes1.get(index);
    }

    @Override
    protected int getOriginalIndex2(int index) {
      return myIndexes2.get(index);
    }
  }
}
