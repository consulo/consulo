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
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.diff.util.LineRange;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.GenericPatchApplier;
import consulo.util.lang.BeforeAfter;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static consulo.ide.impl.idea.openapi.vcs.changes.patch.AppliedTextPatch.HunkStatus.NOT_APPLIED;


public class AppliedTextPatch {
  @Nonnull
  private final List<AppliedSplitPatchHunk> mySplitPatchHunkList;

  public enum HunkStatus {ALREADY_APPLIED, EXACTLY_APPLIED, NOT_APPLIED}

  public static AppliedTextPatch create(@Nonnull List<AppliedSplitPatchHunk> splitPatchHunkList) {
    List<AppliedSplitPatchHunk> hunks = new ArrayList<>(splitPatchHunkList);

    // ensure, that `appliedTo` ranges do not overlap
    BitSet appliedLines = new BitSet();
    for (int i = 0; i < hunks.size(); i++) {
      AppliedSplitPatchHunk hunk = hunks.get(i);
      LineRange appliedTo = hunk.getAppliedTo();
      if (appliedTo == null) continue;

      int nextAppliedLine = appliedLines.nextSetBit(appliedTo.start);
      if (nextAppliedLine != -1 && nextAppliedLine < appliedTo.end) {
        hunks.set(i, new AppliedSplitPatchHunk(hunk, -1, -1, NOT_APPLIED));
      }
      else {
        appliedLines.set(appliedTo.start, appliedTo.end, true);
      }
    }

    ContainerUtil.sort(hunks, (o1, o2) -> Integer.compare(o1.getLineRangeBefore().start, o2.getLineRangeBefore().start));

    return new AppliedTextPatch(hunks);
  }

  private AppliedTextPatch(@Nonnull List<AppliedSplitPatchHunk> hunks) {
    mySplitPatchHunkList = hunks;
  }

  @Nonnull
  public List<AppliedSplitPatchHunk> getHunks() {
    return mySplitPatchHunkList;
  }

  public static class AppliedSplitPatchHunk {
    @Nonnull
    private final HunkStatus myStatus;

    @Nonnull
    private final List<String> myContextBefore;
    @Nonnull
    private final List<String> myContextAfter;

    @Nonnull
    private final List<String> myDeletedLines;
    @Nonnull
    private final List<String> myInsertedLines;

    private final int myAppliedToLinesStart;
    private final int myAppliedToLinesEnd;

    private final int myStartLineBefore;
    private final int myStartLineAfter;

    public AppliedSplitPatchHunk(@Nonnull GenericPatchApplier.SplitHunk splitHunk,
                                 int startLineApplied,
                                 int endLineApplied,
                                 @Nonnull HunkStatus status) {
      myStatus = status;
      myAppliedToLinesStart = startLineApplied;
      myAppliedToLinesEnd = endLineApplied;

      myStartLineBefore = splitHunk.getStartLineBefore();
      myStartLineAfter = splitHunk.getStartLineAfter();

      myContextBefore = splitHunk.getContextBefore();
      myContextAfter = splitHunk.getContextAfter();

      myDeletedLines = new ArrayList<>();
      myInsertedLines = new ArrayList<>();
      for (BeforeAfter<List<String>> step : splitHunk.getPatchSteps()) {
        myDeletedLines.addAll(step.getBefore());
        myInsertedLines.addAll(step.getAfter());
      }
    }

    private AppliedSplitPatchHunk(@Nonnull AppliedSplitPatchHunk hunk,
                                  int appliedToLinesStart,
                                  int appliedToLinesEnd,
                                  @Nonnull HunkStatus status) {
      myStatus = status;
      myAppliedToLinesStart = appliedToLinesStart;
      myAppliedToLinesEnd = appliedToLinesEnd;

      myContextBefore = hunk.myContextBefore;
      myContextAfter = hunk.myContextAfter;
      myDeletedLines = hunk.myDeletedLines;
      myInsertedLines = hunk.myInsertedLines;
      myStartLineBefore = hunk.myStartLineBefore;
      myStartLineAfter = hunk.myStartLineAfter;
    }

    /*
     * Lines that hunk can be applied to
     */
    public LineRange getAppliedTo() {
      if (myStatus == NOT_APPLIED) return null;
      return new LineRange(myAppliedToLinesStart, myAppliedToLinesEnd);
    }

    /*
     * Hunk lines (including context) in base document
     */
    @Nonnull
    public LineRange getLineRangeBefore() {
      int start = myStartLineBefore;
      return new LineRange(start, start + myContextBefore.size() + myDeletedLines.size() + myContextAfter.size());
    }

    /*
     * Hunk lines (including context) in originally patched document
     */
    @Nonnull
    public LineRange getLineRangeAfter() {
      int start = myStartLineAfter;
      return new LineRange(start, start + myContextBefore.size() + myInsertedLines.size() + myContextAfter.size());
    }

    @Nonnull
    public HunkStatus getStatus() {
      return myStatus;
    }

    @Nonnull
    public List<String> getContextBefore() {
      return myContextBefore;
    }

    @Nonnull
    public List<String> getContextAfter() {
      return myContextAfter;
    }

    @Nonnull
    public List<String> getDeletedLines() {
      return myDeletedLines;
    }

    @Nonnull
    public List<String> getInsertedLines() {
      return myInsertedLines;
    }
  }
}
