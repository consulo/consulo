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
package consulo.desktop.awt.internal.diff.simple;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.desktop.awt.internal.diff.ThreesideDiffChangeBase;
import consulo.diff.fragment.MergeLineFragment;
import consulo.diff.impl.internal.merge.MergeInnerDifferences;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.util.MergeConflictType;
import consulo.diff.util.ThreeSide;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class SimpleThreesideDiffChange extends ThreesideDiffChangeBase {
    @Nonnull
    private final List<? extends EditorEx> myEditors;
    @Nullable
    private final MergeInnerDifferences myInnerFragments;

    private int[] myLineStarts = new int[3];
    private int[] myLineEnds = new int[3];

    public SimpleThreesideDiffChange(
        @Nonnull MergeLineFragment fragment,
        @Nonnull MergeConflictType conflictType,
        @Nullable MergeInnerDifferences innerFragments,
        @Nonnull SimpleThreesideDiffViewer viewer
    ) {
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

    @Nullable
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

        DiffImplUtil.UpdatedLineRange newRange = DiffImplUtil.updateRangeOnModification(line1, line2, oldLine1, oldLine2, shift);
        myLineStarts[sideIndex] = newRange.startLine;
        myLineEnds[sideIndex] = newRange.endLine;

        return newRange.damaged;
    }
}