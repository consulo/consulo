package com.intellij.openapi.diff.impl.incrementalMerge;

import com.intellij.openapi.diff.impl.highlighting.FragmentSide;
import com.intellij.openapi.util.TextRange;
import javax.annotation.Nonnull;

public class MergeNoConflict extends TwoSideChange<NoConflictChange> {
  MergeNoConflict(@Nonnull TextRange baseRange,
                  @Nonnull TextRange leftRange,
                  @Nonnull TextRange rightRange,
                  @Nonnull MergeList mergeList) {
    super(baseRange, mergeList, new ChangeHighlighterHolder());
    myLeftChange = new NoConflictChange(this, FragmentSide.SIDE1, baseRange, leftRange, mergeList.getLeftChangeList());
    myRightChange = new NoConflictChange(this, FragmentSide.SIDE2, baseRange, rightRange, mergeList.getRightChangeList());
  }
}
