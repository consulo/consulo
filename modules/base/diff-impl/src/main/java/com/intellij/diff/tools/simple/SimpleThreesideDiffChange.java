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

import com.intellij.diff.fragments.MergeLineFragment;
import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.MergeConflictType;
import com.intellij.diff.util.ThreeSide;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;

public class SimpleThreesideDiffChange extends ThreesideDiffChangeBase {
  @Nonnull
  private final List<? extends EditorEx> myEditors;
  @javax.annotation.Nullable
  private final MergeInnerDifferences myInnerFragments;

  private int[] myLineStarts = new int[3];
  private int[] myLineEnds = new int[3];

  public SimpleThreesideDiffChange(@Nonnull MergeLineFragment fragment,
                                   @Nonnull MergeConflictType conflictType,
                                   @Nullable MergeInnerDifferences innerFragments,
                                   @Nonnull SimpleThreesideDiffViewer viewer) {
    super(conflictType);
    myEditors = viewer.getEditors();
    myInnerFragments = innerFragments;

    for (ThreeSide side : ThreeSide.values()) {
      myLineStarts[side.getIndex()] = fragment.getStartLine(side);
      myLineEnds[side.getIndex()] = fragment.getEndLine(side);
    }

    reinstallHighlighters();
  }

  @RequiredUIAccess
  public void destroy() {
    destroyHighlighters();
    destroyInnerHighlighters();
  }

  @RequiredUIAccess
  public void reinstallHighlighters() {
    destroyHighlighters();
    installHighlighters();

    destroyInnerHighlighters();
    installInnerHighlighters();
  }

  //
  // Getters
  //

  @Override
  public int getStartLine(@Nonnull ThreeSide side) {
    return side.select(myLineStarts);
  }

  @Override
  public int getEndLine(@Nonnull ThreeSide side) {
    return side.select(myLineEnds);
  }

  @Override
  public boolean isResolved(@Nonnull ThreeSide side) {
    return false;
  }

  @Nonnull
  @Override
  protected Editor getEditor(@Nonnull ThreeSide side) {
    return side.select(myEditors);
  }

  @javax.annotation.Nullable
  @Override
  protected MergeInnerDifferences getInnerFragments() {
    return myInnerFragments;
  }

  //
  // Shift
  //

  public boolean processChange(int oldLine1, int oldLine2, int shift, @Nonnull ThreeSide side) {
    int line1 = getStartLine(side);
    int line2 = getEndLine(side);
    int sideIndex = side.getIndex();

    DiffUtil.UpdatedLineRange newRange = DiffUtil.updateRangeOnModification(line1, line2, oldLine1, oldLine2, shift);
    myLineStarts[sideIndex] = newRange.startLine;
    myLineEnds[sideIndex] = newRange.endLine;

    return newRange.damaged;
  }
}