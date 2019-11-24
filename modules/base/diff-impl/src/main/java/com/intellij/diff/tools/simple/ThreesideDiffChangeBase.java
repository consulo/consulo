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
package com.intellij.diff.tools.simple;

import com.intellij.diff.util.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.TextRange;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.ArrayList;
import java.util.List;

public abstract class ThreesideDiffChangeBase {
  @Nonnull
  private final MergeConflictType myType;

  @Nonnull
  protected final List<RangeHighlighter> myHighlighters = new ArrayList<>();
  @Nonnull
  protected final List<RangeHighlighter> myInnerHighlighters = new ArrayList<>();

  public ThreesideDiffChangeBase(@Nonnull MergeConflictType type) {
    myType = type;
  }

  @RequiredUIAccess
  protected void installHighlighters() {
    assert myHighlighters.isEmpty();

    createHighlighter(ThreeSide.BASE);
    if (isChange(Side.LEFT)) createHighlighter(ThreeSide.LEFT);
    if (isChange(Side.RIGHT)) createHighlighter(ThreeSide.RIGHT);
  }

  @RequiredUIAccess
  protected void installInnerHighlighters() {
    assert myInnerHighlighters.isEmpty();

    createInnerHighlighter(ThreeSide.BASE);
    if (isChange(Side.LEFT)) createInnerHighlighter(ThreeSide.LEFT);
    if (isChange(Side.RIGHT)) createInnerHighlighter(ThreeSide.RIGHT);
  }

  @RequiredUIAccess
  protected void destroyHighlighters() {
    for (RangeHighlighter highlighter : myHighlighters) {
      highlighter.dispose();
    }
    myHighlighters.clear();
  }

  @RequiredUIAccess
  protected void destroyInnerHighlighters() {
    for (RangeHighlighter highlighter : myInnerHighlighters) {
      highlighter.dispose();
    }
    myInnerHighlighters.clear();
  }

  //
  // Getters
  //

  public abstract int getStartLine(@Nonnull ThreeSide side);

  public abstract int getEndLine(@Nonnull ThreeSide side);

  public abstract boolean isResolved(@Nonnull ThreeSide side);

  @Nonnull
  protected abstract Editor getEditor(@Nonnull ThreeSide side);

  @Nullable
  protected abstract MergeInnerDifferences getInnerFragments();

  @Nonnull
  public TextDiffType getDiffType() {
    return myType.getDiffType();
  }

  @Nonnull
  public MergeConflictType getType() {
    return myType;
  }

  public boolean isConflict() {
    return getDiffType() == TextDiffType.CONFLICT;
  }

  public boolean isChange(@Nonnull Side side) {
    return myType.isChange(side);
  }

  public boolean isChange(@Nonnull ThreeSide side) {
    return myType.isChange(side);
  }

  //
  // Highlighters
  //

  protected void createHighlighter(@Nonnull ThreeSide side) {
    Editor editor = getEditor(side);

    TextDiffType type = getDiffType();
    int startLine = getStartLine(side);
    int endLine = getEndLine(side);

    boolean resolved = isResolved(side);
    boolean ignored = !resolved && getInnerFragments() != null;
    boolean shouldHideWithoutLineNumbers = side == ThreeSide.BASE && !isChange(Side.LEFT) && isChange(Side.RIGHT);
    myHighlighters.addAll(DiffDrawUtil.createHighlighter(editor, startLine, endLine, type, ignored, resolved, shouldHideWithoutLineNumbers));
  }

  protected void createInnerHighlighter(@Nonnull ThreeSide side) {
    if (isResolved(side)) return;
    MergeInnerDifferences innerFragments = getInnerFragments();
    if (innerFragments == null) return;

    List<TextRange> ranges = innerFragments.get(side);
    if (ranges == null) return;

    Editor editor = getEditor(side);
    int start = DiffUtil.getLinesRange(editor.getDocument(), getStartLine(side), getEndLine(side)).getStartOffset();
    for (TextRange fragment : ranges) {
      int innerStart = start + fragment.getStartOffset();
      int innerEnd = start + fragment.getEndOffset();
      myInnerHighlighters.addAll(DiffDrawUtil.createInlineHighlighter(editor, innerStart, innerEnd, getDiffType()));
    }
  }
}