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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.fragment.LineFragment;
import consulo.diff.fragment.MergeLineFragment;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Class for the text comparison
 * CharSequences should to have '\n' as line separator
 * <p/>
 * It's good idea not to compare String due to expensive subSequence() implementation. Use CharSequenceSubSequence.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ComparisonManager {
  @Nonnull
  public static ComparisonManager getInstance() {
    return Application.get().getInstance(ComparisonManager.class);
  }

  /**
   * Compare two texts by-line
   */
  @Nonnull
  List<LineFragment> compareLines(@Nonnull CharSequence text1,
                                  @Nonnull CharSequence text2,
                                  @Nonnull ComparisonPolicy policy,
                                  @Nonnull ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Compare two texts by-line and then compare changed fragments by-word
   */
  @Nonnull
  List<LineFragment> compareLinesInner(@Nonnull CharSequence text1,
                                       @Nonnull CharSequence text2,
                                       @Nonnull ComparisonPolicy policy,
                                       @Nonnull ProgressIndicator indicator) throws DiffTooBigException;

  @Nonnull
  @Deprecated
  List<LineFragment> compareLinesInner(@Nonnull CharSequence text1,
                                       @Nonnull CharSequence text2,
                                       @Nonnull List<LineFragment> lineFragments,
                                       @Nonnull ComparisonPolicy policy,
                                       @Nonnull ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Compare three texts by-line (LEFT - BASE - RIGHT)
   */
  @Nonnull
  List<MergeLineFragment> compareLines(@Nonnull CharSequence text1,
                                       @Nonnull CharSequence text2,
                                       @Nonnull CharSequence text3,
                                       @Nonnull ComparisonPolicy policy,
                                       @Nonnull ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Compare two texts by-word
   */
  @Nonnull
  List<DiffFragment> compareWords(@Nonnull CharSequence text1,
                                  @Nonnull CharSequence text2,
                                  @Nonnull ComparisonPolicy policy,
                                  @Nonnull ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Compare two texts by-char
   */
  @Nonnull
  List<DiffFragment> compareChars(@Nonnull CharSequence text1,
                                  @Nonnull CharSequence text2,
                                  @Nonnull ComparisonPolicy policy,
                                  @Nonnull ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Check if two texts are equal using ComparisonPolicy
   */
  boolean isEquals(@Nonnull CharSequence text1, @Nonnull CharSequence text2, @Nonnull ComparisonPolicy policy);

  //
  // Post process line fragments
  //

  /**
   * compareLinesInner() comparison can produce adjustment line chunks. This method allows to squash shem.
   * <p>
   * ex: "A\nB" vs "A X\nB Y" will result to two LineFragments: [0, 1) - [0, 1) and [1, 2) - [1, 2)
   * squash will produce a single fragment: [0, 2) - [0, 2)
   */
  @Nonnull
  List<LineFragment> squash(@Nonnull List<LineFragment> oldFragments);

  /**
   * @param trim - if leading/trailing LineFragments with equal contents should be skipped
   * @see #squash
   */
  @Nonnull
  List<LineFragment> processBlocks(@Nonnull List<LineFragment> oldFragments,
                                   @Nonnull CharSequence text1, @Nonnull CharSequence text2,
                                   @Nonnull ComparisonPolicy policy,
                                   boolean squash, boolean trim);
}
