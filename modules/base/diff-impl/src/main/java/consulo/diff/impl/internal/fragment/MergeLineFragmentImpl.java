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
package consulo.diff.impl.internal.fragment;

import consulo.diff.fragment.MergeLineFragment;
import consulo.diff.fragment.MergeWordFragment;
import consulo.diff.util.MergeRange;
import consulo.diff.util.ThreeSide;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class MergeLineFragmentImpl implements MergeLineFragment {
  private final int myStartLine1;
  private final int myEndLine1;
  private final int myStartLine2;
  private final int myEndLine2;
  private final int myStartLine3;
  private final int myEndLine3;

  @jakarta.annotation.Nullable
  private final List<MergeWordFragment> myInnerFragments;

  public MergeLineFragmentImpl(int startLine1,
                               int endLine1,
                               int startLine2,
                               int endLine2,
                               int startLine3,
                               int endLine3) {
    this(startLine1, endLine1, startLine2, endLine2, startLine3, endLine3, null);
  }

  public MergeLineFragmentImpl(int startLine1,
                               int endLine1,
                               int startLine2,
                               int endLine2,
                               int startLine3,
                               int endLine3,
                               @jakarta.annotation.Nullable List<MergeWordFragment> innerFragments) {
    myStartLine1 = startLine1;
    myEndLine1 = endLine1;
    myStartLine2 = startLine2;
    myEndLine2 = endLine2;
    myStartLine3 = startLine3;
    myEndLine3 = endLine3;
    myInnerFragments = innerFragments;
  }

  public MergeLineFragmentImpl(@Nonnull MergeRange range) {
    this(range, null);
  }

  public MergeLineFragmentImpl(@Nonnull MergeRange range, @jakarta.annotation.Nullable List<MergeWordFragment> innerFragments) {
    this(range.start1, range.end1, range.start2, range.end2, range.start3, range.end3, innerFragments);
  }

  public MergeLineFragmentImpl(@Nonnull MergeLineFragment fragment, @Nullable List<MergeWordFragment> fragments) {
    this(fragment.getStartLine(ThreeSide.LEFT), fragment.getEndLine(ThreeSide.LEFT),
         fragment.getStartLine(ThreeSide.BASE), fragment.getEndLine(ThreeSide.BASE),
         fragment.getStartLine(ThreeSide.RIGHT), fragment.getEndLine(ThreeSide.RIGHT),
         fragments);
  }

  @Override
  public int getStartLine(@Nonnull ThreeSide side) {
    return side.select(myStartLine1, myStartLine2, myStartLine3);
  }

  @Override
  public int getEndLine(@Nonnull ThreeSide side) {
    return side.select(myEndLine1, myEndLine2, myEndLine3);
  }

  @jakarta.annotation.Nullable
  @Override
  public List<MergeWordFragment> getInnerFragments() {
    return myInnerFragments;
  }
}
