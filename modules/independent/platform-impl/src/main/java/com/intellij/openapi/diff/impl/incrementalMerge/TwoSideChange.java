package com.intellij.openapi.diff.impl.incrementalMerge;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diff.impl.highlighting.FragmentSide;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.util.TextRange;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TwoSideChange<T extends TwoSideChange.SideChange> extends ChangeSide implements DiffRangeMarker.RangeInvalidListener {
  @Nonnull
  protected final MergeList myMergeList;
  @Nonnull
  protected DiffRangeMarker myBaseRangeMarker;
  protected T myLeftChange;
  protected T myRightChange;
  @Nonnull
  protected final ChangeHighlighterHolder myCommonHighlighterHolder;

  protected TwoSideChange(@Nonnull TextRange baseRange,
                          @Nonnull MergeList mergeList,
                          @Nonnull ChangeHighlighterHolder highlighterHolder) {
    myBaseRangeMarker = new DiffRangeMarker((DocumentEx)mergeList.getBaseDocument(), baseRange, this);
    myMergeList = mergeList;
    myCommonHighlighterHolder = highlighterHolder;
  }

  @Nonnull
  public ChangeHighlighterHolder getHighlighterHolder() {
    return myCommonHighlighterHolder;
  }

  @Nonnull
  public DiffRangeMarker getRange() {
    return myBaseRangeMarker;
  }

  @Nullable
  public Change getLeftChange() {
    return myLeftChange;
  }

  @Nullable
  public Change getRightChange() {
    return myRightChange;
  }

  public void setRange(@Nonnull DiffRangeMarker range) {
    myBaseRangeMarker = range;
  }

  @Nullable
  T getOtherChange(@Nonnull T change) {
    if (change == myLeftChange) {
      return myRightChange;
    }
    else if (change == myRightChange) {
      return myLeftChange;
    }
    else {
      throw new IllegalStateException("Unexpected change: " + change);
    }
  }

  public void removeOtherChange(@Nonnull T change) {
    if (change == myLeftChange) {
      myRightChange = null;
    }
    else if (change == myRightChange) {
      myLeftChange = null;
    }
    else {
      throw new IllegalStateException("Unexpected change: " + change);
    }
  }

  public void conflictRemoved() {
    removeHighlighters(myLeftChange);
    removeHighlighters(myRightChange);
    myCommonHighlighterHolder.removeHighlighters();
    myMergeList.removeChanges(myLeftChange, myRightChange);
    myBaseRangeMarker.removeListener(this);
  }

  private static <T extends SideChange> void removeHighlighters(@Nullable T change) {
    if (change != null) {
      change.getOriginalSide().getHighlighterHolder().removeHighlighters();
    }
  }

  @Nonnull
  public Document getOriginalDocument(FragmentSide mergeSide) {
    return myMergeList.getChanges(mergeSide).getDocument(MergeList.BRANCH_SIDE);
  }

  public void onRangeInvalidated() {
    conflictRemoved();
  }

  @Nonnull
  public MergeList getMergeList() {
    return myMergeList;
  }

  protected static abstract class SideChange<V extends TwoSideChange> extends Change implements DiffRangeMarker.RangeInvalidListener {
    protected V myTwoSideChange;
    @Nonnull
    protected final ChangeList myChangeList;

    protected SimpleChangeSide myOriginalSide;
    @Nonnull
    protected ChangeType myType;

    protected SideChange(@Nonnull V twoSideChange,
                         @Nonnull ChangeList changeList,
                         @Nonnull ChangeType type,
                         @Nonnull FragmentSide mergeSide,
                         @Nonnull TextRange versionRange) {
      myTwoSideChange = twoSideChange;
      myChangeList = changeList;
      myOriginalSide =
              new SimpleChangeSide(mergeSide, new DiffRangeMarker((DocumentEx)twoSideChange.getOriginalDocument(mergeSide), versionRange, this));
      myType = type;
    }

    @Nonnull
    public ChangeType getType() {
      return myType;
    }

    public SimpleChangeSide getOriginalSide() {
      return myOriginalSide;
    }

    protected void markApplied() {
      myType = ChangeType.deriveApplied(myType);
      myChangeList.apply(this);

      myOriginalSide.getHighlighterHolder().updateHighlighter(myOriginalSide, myType);
      myOriginalSide.getHighlighterHolder().setActions(new AnAction[0]);

      // display, what one side of the conflict was resolved to
      myTwoSideChange.getHighlighterHolder().updateHighlighter(myTwoSideChange, myType);
    }

    public ChangeList getChangeList() {
      return myTwoSideChange.getMergeList().getChanges(myOriginalSide.getFragmentSide());
    }

    @Override
    protected void changeSide(ChangeSide sideToChange, DiffRangeMarker newRange) {
      myTwoSideChange.setRange(newRange);
    }

    @Nonnull
    @Override
    public ChangeSide getChangeSide(@Nonnull FragmentSide side) {
      return isBranch(side) ? myOriginalSide : myTwoSideChange;
    }

    protected static boolean isBranch(@Nonnull FragmentSide side) {
      return MergeList.BRANCH_SIDE == side;
    }

    protected void removeFromList() {
      myTwoSideChange.conflictRemoved();
      myTwoSideChange = null;
    }

    public boolean isValid() {
      return myTwoSideChange != null;
    }

    public void onRemovedFromList() {
      myOriginalSide.getRange().removeListener(this);
      myTwoSideChange = null;
      myOriginalSide = null;
    }

    public void onRangeInvalidated() {
      removeFromList();
    }
  }
}
