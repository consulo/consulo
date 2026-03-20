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

import java.util.List;

/**
 * Class for the text comparison
 * CharSequences should to have '\n' as line separator
 * <p/>
 * It's good idea not to compare String due to expensive subSequence() implementation. Use CharSequenceSubSequence.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ComparisonManager {
  
  public static ComparisonManager getInstance() {
    return Application.get().getInstance(ComparisonManager.class);
  }

  /**
   * Compare two texts by-line
   */
  List<LineFragment> compareLines(CharSequence text1,
                                  CharSequence text2,
                                  ComparisonPolicy policy,
                                  ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Compare two texts by-line and then compare changed fragments by-word
   */
  List<LineFragment> compareLinesInner(CharSequence text1,
                                       CharSequence text2,
                                       ComparisonPolicy policy,
                                       ProgressIndicator indicator) throws DiffTooBigException;

  
  @Deprecated
  List<LineFragment> compareLinesInner(CharSequence text1,
                                       CharSequence text2,
                                       List<LineFragment> lineFragments,
                                       ComparisonPolicy policy,
                                       ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Compare three texts by-line (LEFT - BASE - RIGHT)
   */
  List<MergeLineFragment> compareLines(CharSequence text1,
                                       CharSequence text2,
                                       CharSequence text3,
                                       ComparisonPolicy policy,
                                       ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Compare two texts by-word
   */
  List<DiffFragment> compareWords(CharSequence text1,
                                  CharSequence text2,
                                  ComparisonPolicy policy,
                                  ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Compare two texts by-char
   */
  List<DiffFragment> compareChars(CharSequence text1,
                                  CharSequence text2,
                                  ComparisonPolicy policy,
                                  ProgressIndicator indicator) throws DiffTooBigException;

  /**
   * Check if two texts are equal using ComparisonPolicy
   */
  boolean isEquals(CharSequence text1, CharSequence text2, ComparisonPolicy policy);

  //
  // Post process line fragments
  //

  /**
   * compareLinesInner() comparison can produce adjustment line chunks. This method allows to squash shem.
   * <p>
   * ex: "A\nB" vs "A X\nB Y" will result to two LineFragments: [0, 1) - [0, 1) and [1, 2) - [1, 2)
   * squash will produce a single fragment: [0, 2) - [0, 2)
   */
  List<LineFragment> squash(List<LineFragment> oldFragments);

  /**
   * @param trim - if leading/trailing LineFragments with equal contents should be skipped
   * @see #squash
   */
  List<LineFragment> processBlocks(List<LineFragment> oldFragments,
                                   CharSequence text1, CharSequence text2,
                                   ComparisonPolicy policy,
                                   boolean squash, boolean trim);
}
