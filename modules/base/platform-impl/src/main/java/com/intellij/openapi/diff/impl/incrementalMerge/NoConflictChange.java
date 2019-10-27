package com.intellij.openapi.diff.impl.incrementalMerge;

import com.intellij.openapi.diff.impl.highlighting.FragmentSide;
import com.intellij.openapi.util.TextRange;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

public class NoConflictChange extends TwoSideChange.SideChange<MergeNoConflict> {
  private static final Logger LOG = Logger.getInstance(NoConflictChange.class);

  private boolean myApplied;

  public NoConflictChange(@Nonnull MergeNoConflict twoSideChange,
                          @Nonnull FragmentSide mergeSide,
                          @Nonnull TextRange baseRange,
                          @Nonnull TextRange versionRange,
                          @Nonnull ChangeList changeList) {
    super(twoSideChange, changeList, ChangeType.fromRanges(baseRange, versionRange), mergeSide, versionRange);
  }

  @Override
  public void onApplied() {
    markApplied();

    NoConflictChange otherChange = myTwoSideChange.getOtherChange(this);
    LOG.assertTrue(otherChange != null, String.format("Other change is null. This change: %s Merge conflict: %s", this, myTwoSideChange));
    otherChange.markApplied();
  }

  @Override
  protected void markApplied() {
    if (!myApplied) {
      myApplied = true;
      super.markApplied();
    }
  }
}
