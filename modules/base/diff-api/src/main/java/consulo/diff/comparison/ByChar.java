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

import consulo.diff.comparison.TrimUtil;
import consulo.diff.comparison.iterable.DiffIterable;
import consulo.diff.comparison.iterable.FairDiffIterable;
import consulo.diff.util.Range;
import consulo.application.progress.ProgressIndicator;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static consulo.diff.comparison.TrimUtil.isPunctuation;
import static consulo.diff.comparison.iterable.DiffIterableUtil.*;
import static consulo.util.lang.StringUtil.isWhiteSpace;

public class ByChar {
  @Nonnull
  public static FairDiffIterable compare(@Nonnull CharSequence text1,
                                         @Nonnull CharSequence text2,
                                         @Nonnull ProgressIndicator indicator) {
    indicator.checkCanceled();

    int[] chars1 = getAllChars(text1);
    int[] chars2 = getAllChars(text2);

    return diff(chars1, chars2, indicator);
  }

  @Nonnull
  public static FairDiffIterable compareTwoStep(@Nonnull CharSequence text1,
                                                @Nonnull CharSequence text2,
                                                @Nonnull ProgressIndicator indicator) {
    indicator.checkCanceled();

    CharOffsets chars1 = getNonSpaceChars(text1);
    CharOffsets chars2 = getNonSpaceChars(text2);

    FairDiffIterable nonSpaceChanges = diff(chars1.characters, chars2.characters, indicator);
    return matchAdjustmentSpaces(chars1, chars2, text1, text2, nonSpaceChanges, indicator);
  }

  @Nonnull
  public static DiffIterable compareIgnoreWhitespaces(@Nonnull CharSequence text1,
                                                      @Nonnull CharSequence text2,
                                                      @Nonnull ProgressIndicator indicator) {
    indicator.checkCanceled();

    CharOffsets chars1 = getNonSpaceChars(text1);
    CharOffsets chars2 = getNonSpaceChars(text2);

    FairDiffIterable changes = diff(chars1.characters, chars2.characters, indicator);
    return matchAdjustmentSpacesIW(chars1, chars2, text1, text2, changes);
  }

  /*
   * Compare punctuation chars only, all other characters are left unmatched
   */
  @Nonnull
  public static FairDiffIterable comparePunctuation(@Nonnull CharSequence text1,
                                                    @Nonnull CharSequence text2,
                                                    @Nonnull ProgressIndicator indicator) {
    indicator.checkCanceled();

    CharOffsets chars1 = getPunctuationChars(text1);
    CharOffsets chars2 = getPunctuationChars(text2);

    FairDiffIterable nonSpaceChanges = diff(chars1.characters, chars2.characters, indicator);
    return transfer(chars1, chars2, text1, text2, nonSpaceChanges, indicator);
  }

  //
  // Impl
  //

  @Nonnull
  private static FairDiffIterable transfer(@Nonnull final CharOffsets chars1,
                                           @Nonnull final CharOffsets chars2,
                                           @Nonnull final CharSequence text1,
                                           @Nonnull final CharSequence text2,
                                           @Nonnull final FairDiffIterable changes,
                                           @Nonnull final ProgressIndicator indicator) {
    ChangeBuilder builder = new ChangeBuilder(text1.length(), text2.length());

    for (Range range : changes.iterateUnchanged()) {
      int count = range.end1 - range.start1;
      for (int i = 0; i < count; i++) {
        int offset1 = chars1.offsets[range.start1 + i];
        int offset2 = chars2.offsets[range.start2 + i];
        builder.markEqual(offset1, offset2);
      }
    }

    return fair(builder.finish());
  }

  /*
   * Given DiffIterable on non-space characters, convert it into DiffIterable on original texts.
   *
   * Idea: run fair diff on all gaps between matched characters
   * (inside these pairs could met non-space characters, but they will be unique and can't be matched)
   */
  @Nonnull
  private static FairDiffIterable matchAdjustmentSpaces(@Nonnull final CharOffsets chars1,
                                                        @Nonnull final CharOffsets chars2,
                                                        @Nonnull final CharSequence text1,
                                                        @Nonnull final CharSequence text2,
                                                        @Nonnull final FairDiffIterable changes,
                                                        @Nonnull final ProgressIndicator indicator) {
    return new ChangeCorrector.DefaultCharChangeCorrector(chars1, chars2, text1, text2, changes, indicator).build();
  }

  /*
   * Given DiffIterable on non-whitespace characters, convert it into DiffIterable on original texts.
   *
   * matched characters: matched non-space characters + all adjustment whitespaces
   */
  @Nonnull
  private static DiffIterable matchAdjustmentSpacesIW(@Nonnull CharOffsets chars1,
                                                      @Nonnull CharOffsets chars2,
                                                      @Nonnull CharSequence text1,
                                                      @Nonnull CharSequence text2,
                                                      @Nonnull FairDiffIterable changes) {
    final List<Range> ranges = new ArrayList<Range>();

    for (Range ch : changes.iterateChanges()) {
      int startOffset1;
      int endOffset1;
      if (ch.start1 == ch.end1) {
        startOffset1 = endOffset1 = expandForwardW(chars1, chars2, text1, text2, ch, true);
      }
      else {
        startOffset1 = chars1.offsets[ch.start1];
        endOffset1 = chars1.offsets[ch.end1 - 1] + 1;
      }

      int startOffset2;
      int endOffset2;
      if (ch.start2 == ch.end2) {
        startOffset2 = endOffset2 = expandForwardW(chars1, chars2, text1, text2, ch, false);
      }
      else {
        startOffset2 = chars2.offsets[ch.start2];
        endOffset2 = chars2.offsets[ch.end2 - 1] + 1;
      }

      ranges.add(new Range(startOffset1, endOffset1, startOffset2, endOffset2));
    }
    return create(ranges, text1.length(), text2.length());
  }

  /*
   * we need it to correct place of insertion/deletion: we want to match whitespaces, if we can to
   *
   * sample: "x y" -> "x zy", space should be matched instead of being ignored.
   */
  private static int expandForwardW(@Nonnull CharOffsets chars1,
                                    @Nonnull CharOffsets chars2,
                                    @Nonnull CharSequence text1,
                                    @Nonnull CharSequence text2,
                                    @Nonnull Range ch,
                                    boolean left) {
    int offset1 = ch.start1 == 0 ? 0 : chars1.offsets[ch.start1 - 1] + 1;
    int offset2 = ch.start2 == 0 ? 0 : chars2.offsets[ch.start2 - 1] + 1;

    int start = left ? offset1 : offset2;

    return start + TrimUtil.expandForwardW(text1, text2, offset1, offset2, text1.length(), text2.length());
  }

  //
  // Misc
  //

  @Nonnull
  private static int[] getAllChars(@Nonnull CharSequence text) {
    int[] chars = new int[text.length()];
    for (int i = 0; i < text.length(); i++) {
      chars[i] = text.charAt(i);
    }
    return chars;
  }

  @Nonnull
  private static CharOffsets getNonSpaceChars(@Nonnull CharSequence text) {
    IntList chars = IntLists.newArrayList(text.length());
    IntList offsets = IntLists.newArrayList(text.length());

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (!isWhiteSpace(c)) {
        chars.add(c);
        offsets.add(i);
      }
    }

    return new CharOffsets(chars.toArray(), offsets.toArray());
  }

  @Nonnull
  private static CharOffsets getPunctuationChars(@Nonnull CharSequence text) {
    IntList chars = IntLists.newArrayList(text.length());
    IntList offsets = IntLists.newArrayList(text.length());

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (isPunctuation(c)) {
        chars.add(c);
        offsets.add(i);
      }
    }

    return new CharOffsets(chars.toArray(), offsets.toArray());
  }

  static class CharOffsets {
    public final int[] characters;
    public final int[] offsets;

    public CharOffsets(int[] characters, int[] offsets) {
      this.characters = characters;
      this.offsets = offsets;
    }
  }
}
