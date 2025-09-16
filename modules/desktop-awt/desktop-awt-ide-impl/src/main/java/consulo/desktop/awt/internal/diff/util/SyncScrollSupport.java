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
package consulo.desktop.awt.internal.diff.util;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.impl.CodeEditorFoldingModelBase;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class SyncScrollSupport {
    public interface SyncScrollable {
        @RequiredUIAccess
        boolean isSyncScrollEnabled();

        @RequiredUIAccess
        int transfer(@Nonnull Side baseSide, int line);
    }

    public interface Support {
        void enterDisableScrollSection();

        void exitDisableScrollSection();
    }

    public static class TwosideSyncScrollSupport extends SyncScrollSupportBase {
        @Nonnull
        private final List<? extends Editor> myEditors;
        @Nonnull
        private final SyncScrollable myScrollable;

        @Nonnull
        private final ScrollHelper myHelper1;
        @Nonnull
        private final ScrollHelper myHelper2;

        public TwosideSyncScrollSupport(@Nonnull List<? extends Editor> editors, @Nonnull SyncScrollable scrollable) {
            myEditors = editors;
            myScrollable = scrollable;

            myHelper1 = create(Side.LEFT);
            myHelper2 = create(Side.RIGHT);
        }

        @Override
        @Nonnull
        protected List<? extends Editor> getEditors() {
            return myEditors;
        }

        @Override
        @Nonnull
        protected List<? extends ScrollHelper> getScrollHelpers() {
            return Arrays.asList(myHelper1, myHelper2);
        }

        @Nonnull
        public SyncScrollable getScrollable() {
            return myScrollable;
        }

        @RequiredUIAccess
        public void visibleAreaChanged(VisibleAreaEvent e) {
            if (!myScrollable.isSyncScrollEnabled() || isDuringSyncScroll()) {
                return;
            }

            enterDisableScrollSection();
            try {
                if (e.getEditor() == Side.LEFT.select(myEditors)) {
                    myHelper1.visibleAreaChanged(e);
                }
                else if (e.getEditor() == Side.RIGHT.select(myEditors)) {
                    myHelper2.visibleAreaChanged(e);
                }
            }
            finally {
                exitDisableScrollSection();
            }
        }

        public void makeVisible(
            @Nonnull Side masterSide,
            int startLine1, int endLine1, int startLine2, int endLine2,
            boolean animate
        ) {
            doMakeVisible(masterSide.getIndex(), new int[]{startLine1, startLine2}, new int[]{endLine1, endLine2}, animate);
        }

        @Nonnull
        private ScrollHelper create(@Nonnull Side side) {
            return ScrollHelper.create(myEditors, side.getIndex(), side.other().getIndex(), myScrollable, side);
        }
    }

    public static class ThreesideSyncScrollSupport extends SyncScrollSupportBase {
        @Nonnull
        private final List<? extends Editor> myEditors;
        @Nonnull
        private final SyncScrollable myScrollable12;
        @Nonnull
        private final SyncScrollable myScrollable23;

        @Nonnull
        private final ScrollHelper myHelper12;
        @Nonnull
        private final ScrollHelper myHelper21;
        @Nonnull
        private final ScrollHelper myHelper23;
        @Nonnull
        private final ScrollHelper myHelper32;

        public ThreesideSyncScrollSupport(
            @Nonnull List<? extends Editor> editors,
            @Nonnull SyncScrollable scrollable12,
            @Nonnull SyncScrollable scrollable23
        ) {
            assert editors.size() == 3;

            myEditors = editors;
            myScrollable12 = scrollable12;
            myScrollable23 = scrollable23;

            myHelper12 = create(ThreeSide.LEFT, ThreeSide.BASE);
            myHelper21 = create(ThreeSide.BASE, ThreeSide.LEFT);

            myHelper23 = create(ThreeSide.BASE, ThreeSide.RIGHT);
            myHelper32 = create(ThreeSide.RIGHT, ThreeSide.BASE);
        }

        @Nonnull
        @Override
        protected List<? extends Editor> getEditors() {
            return myEditors;
        }

        @Nonnull
        @Override
        protected List<? extends ScrollHelper> getScrollHelpers() {
            return Arrays.asList(myHelper12, myHelper21, myHelper23, myHelper32);
        }

        @Nonnull
        public SyncScrollable getScrollable12() {
            return myScrollable12;
        }

        @Nonnull
        public SyncScrollable getScrollable23() {
            return myScrollable23;
        }

        @RequiredUIAccess
        public void visibleAreaChanged(VisibleAreaEvent e) {
            if (isDuringSyncScroll()) {
                return;
            }

            enterDisableScrollSection();
            try {
                if (e.getEditor() == ThreeSide.LEFT.select(myEditors)) {
                    if (myScrollable12.isSyncScrollEnabled()) {
                        myHelper12.visibleAreaChanged(e);
                        if (myScrollable23.isSyncScrollEnabled()) {
                            myHelper23.visibleAreaChanged(e);
                        }
                    }
                }
                else if (e.getEditor() == ThreeSide.BASE.select(myEditors)) {
                    if (myScrollable12.isSyncScrollEnabled()) {
                        myHelper21.visibleAreaChanged(e);
                    }
                    if (myScrollable23.isSyncScrollEnabled()) {
                        myHelper23.visibleAreaChanged(e);
                    }
                }
                else if (e.getEditor() == ThreeSide.RIGHT.select(myEditors)) {
                    if (myScrollable23.isSyncScrollEnabled()) {
                        myHelper32.visibleAreaChanged(e);
                        if (myScrollable12.isSyncScrollEnabled()) {
                            myHelper21.visibleAreaChanged(e);
                        }
                    }
                }
            }
            finally {
                exitDisableScrollSection();
            }
        }

        public void makeVisible(@Nonnull ThreeSide masterSide, int[] startLines, int[] endLines, boolean animate) {
            doMakeVisible(masterSide.getIndex(), startLines, endLines, animate);
        }

        @Nonnull
        private ScrollHelper create(@Nonnull ThreeSide master, @Nonnull ThreeSide slave) {
            assert master != slave;
            assert master == ThreeSide.BASE || slave == ThreeSide.BASE;

            boolean leftSide = master == ThreeSide.LEFT || slave == ThreeSide.LEFT;
            SyncScrollable scrollable = leftSide ? myScrollable12 : myScrollable23;

            Side side;
            if (leftSide) {
                // LEFT - BASE -> LEFT
                // BASE - LEFT -> RIGHT
                side = Side.fromLeft(master == ThreeSide.LEFT);
            }
            else {
                // BASE - RIGHT -> LEFT
                // RIGHT - BASE -> RIGHT
                side = Side.fromLeft(master == ThreeSide.BASE);
            }

            return ScrollHelper.create(myEditors, master.getIndex(), slave.getIndex(), scrollable, side);
        }
    }

    //
    // Impl
    //

    private abstract static class SyncScrollSupportBase implements Support {
        private int myDuringSyncScrollDepth = 0;

        public boolean isDuringSyncScroll() {
            return myDuringSyncScrollDepth > 0;
        }

        @Override
        public void enterDisableScrollSection() {
            myDuringSyncScrollDepth++;
        }

        @Override
        public void exitDisableScrollSection() {
            myDuringSyncScrollDepth--;
            assert myDuringSyncScrollDepth >= 0;
        }

        @Nonnull
        protected abstract List<? extends Editor> getEditors();

        @Nonnull
        protected abstract List<? extends ScrollHelper> getScrollHelpers();

        protected void doMakeVisible(int masterIndex, int[] startLines, int[] endLines, boolean animate) {
            List<? extends Editor> editors = getEditors();
            List<? extends ScrollHelper> helpers = getScrollHelpers();

            int count = editors.size();
            assert startLines.length == count;
            assert endLines.length == count;

            int[] offsets = getTargetOffsets(editors.toArray(new Editor[count]), startLines, endLines, -1);

            int[] startOffsets = new int[count];
            for (int i = 0; i < count; i++) {
                startOffsets[i] = editors.get(i).getScrollingModel().getVisibleArea().y;
            }

            Editor masterEditor = editors.get(masterIndex);
            int masterOffset = offsets[masterIndex];
            int masterStartOffset = startOffsets[masterIndex];

            for (ScrollHelper helper : helpers) {
                helper.setAnchor(startOffsets[helper.getMasterIndex()], offsets[helper.getMasterIndex()],
                    startOffsets[helper.getSlaveIndex()], offsets[helper.getSlaveIndex()]
                );
            }

            doScrollHorizontally(masterEditor, 0, false); // animation will be canceled by "scroll vertically" anyway
            doScrollVertically(masterEditor, masterOffset, animate);

            masterEditor.getScrollingModel().runActionOnScrollingFinished(() -> {
                for (ScrollHelper helper : helpers) {
                    helper.removeAnchor();
                }

                int masterFinalOffset = masterEditor.getScrollingModel().getVisibleArea().y;
                boolean animateSlaves = animate && masterFinalOffset == masterStartOffset;
                for (int i = 0; i < count; i++) {
                    if (i == masterIndex) {
                        continue;
                    }
                    Editor editor = editors.get(i);

                    int finalOffset = editor.getScrollingModel().getVisibleArea().y;
                    if (finalOffset != offsets[i]) {
                        enterDisableScrollSection();

                        doScrollVertically(editor, offsets[i], animateSlaves);

                        editor.getScrollingModel().runActionOnScrollingFinished(this::exitDisableScrollSection);
                    }
                }
            });
        }
    }

    private static abstract class ScrollHelper implements VisibleAreaListener {
        @Nonnull
        private final List<? extends Editor> myEditors;
        private final int myMasterIndex;
        private final int mySlaveIndex;

        @Nullable
        private Anchor myAnchor;

        public ScrollHelper(@Nonnull List<? extends Editor> editors, int masterIndex, int slaveIndex) {
            myEditors = editors;
            myMasterIndex = masterIndex;
            mySlaveIndex = slaveIndex;
        }

        @Nonnull
        public static ScrollHelper create(
            @Nonnull List<? extends Editor> editors,
            int masterIndex,
            int slaveIndex,
            @Nonnull final SyncScrollable scrollable,
            @Nonnull final Side side
        ) {
            return new ScrollHelper(editors, masterIndex, slaveIndex) {
                @Override
                @RequiredUIAccess
                protected int convertLine(int value) {
                    return scrollable.transfer(side, value);
                }
            };
        }

        protected abstract int convertLine(int value);

        public void setAnchor(int masterStartOffset, int masterEndOffset, int slaveStartOffset, int slaveEndOffset) {
            myAnchor = new Anchor(masterStartOffset, masterEndOffset, slaveStartOffset, slaveEndOffset);
        }

        public void removeAnchor() {
            myAnchor = null;
        }

        @Override
        public void visibleAreaChanged(VisibleAreaEvent e) {
            if (((CodeEditorFoldingModelBase) getSlave().getFoldingModel()).isInBatchFoldingOperation()) {
                return;
            }

            Rectangle newRectangle = e.getNewRectangle();
            Rectangle oldRectangle = e.getOldRectangle();
            if (oldRectangle == null) {
                return;
            }

            if (newRectangle.x != oldRectangle.x) {
                syncHorizontalScroll(false);
            }
            if (newRectangle.y != oldRectangle.y) {
                syncVerticalScroll(false);
            }
        }

        public int getMasterIndex() {
            return myMasterIndex;
        }

        public int getSlaveIndex() {
            return mySlaveIndex;
        }

        @Nonnull
        public Editor getMaster() {
            return myEditors.get(myMasterIndex);
        }

        @Nonnull
        public Editor getSlave() {
            return myEditors.get(mySlaveIndex);
        }

        private void syncVerticalScroll(boolean animated) {
            if (getMaster().getDocument().getTextLength() == 0) {
                return;
            }

            Rectangle viewRect = getMaster().getScrollingModel().getVisibleArea();
            int middleY = viewRect.height / 3;

            int offset;
            if (myAnchor == null) {
                LogicalPosition masterPos = getMaster().xyToLogicalPosition(new Point(viewRect.x, viewRect.y + middleY));
                int masterCenterLine = masterPos.line;
                int convertedCenterLine = convertLine(masterCenterLine);

                Point point = getSlave().logicalPositionToXY(new LogicalPosition(convertedCenterLine, masterPos.column));
                int correction = (viewRect.y + middleY) % getMaster().getLineHeight();
                offset = point.y - middleY + correction;
            }
            else {
                double progress = myAnchor.masterStartOffset == myAnchor.masterEndOffset || viewRect.y == myAnchor.masterEndOffset ? 1 :
                    ((double) (viewRect.y - myAnchor.masterStartOffset)) / (myAnchor.masterEndOffset - myAnchor.masterStartOffset);

                offset = myAnchor.slaveStartOffset + (int) ((myAnchor.slaveEndOffset - myAnchor.slaveStartOffset) * progress);
            }

            int deltaHeaderOffset = getHeaderOffset(getSlave()) - getHeaderOffset(getMaster());
            doScrollVertically(getSlave(), offset + deltaHeaderOffset, animated);
        }

        private void syncHorizontalScroll(boolean animated) {
            int offset = getMaster().getScrollingModel().getVisibleArea().x;
            doScrollHorizontally(getSlave(), offset, animated);
        }
    }

    private static void doScrollVertically(@Nonnull Editor editor, int offset, boolean animated) {
        ScrollingModel model = editor.getScrollingModel();
        if (!animated) {
            model.disableAnimation();
        }
        model.scrollVertically(offset);
        if (!animated) {
            model.enableAnimation();
        }
    }

    private static void doScrollHorizontally(@Nonnull Editor editor, int offset, boolean animated) {
        ScrollingModel model = editor.getScrollingModel();
        if (!animated) {
            model.disableAnimation();
        }
        model.scrollHorizontally(offset);
        if (!animated) {
            model.enableAnimation();
        }
    }

    private static int getHeaderOffset(@Nonnull Editor editor) {
        JComponent header = editor.getHeaderComponent();
        return header == null ? 0 : header.getHeight();
    }

    @Nonnull
    public static int[] getTargetOffsets(
        @Nonnull Editor editor1, @Nonnull Editor editor2,
        int startLine1, int endLine1, int startLine2, int endLine2,
        int preferredTopShift
    ) {
        return getTargetOffsets(
            new Editor[]{editor1, editor2},
            new int[]{startLine1, startLine2},
            new int[]{endLine1, endLine2},
            preferredTopShift
        );
    }

    @Nonnull
    private static int[] getTargetOffsets(@Nonnull Editor[] editors, int[] startLines, int[] endLines, int preferredTopShift) {
        int count = editors.length;
        assert startLines.length == count;
        assert endLines.length == count;

        int[] topOffsets = new int[count];
        int[] bottomOffsets = new int[count];
        int[] rangeHeights = new int[count];
        int[] gapLines = new int[count];
        int[] editorHeights = new int[count];
        int[] maximumOffsets = new int[count];
        int[] topShifts = new int[count];

        for (int i = 0; i < count; i++) {
            topOffsets[i] = editors[i].logicalPositionToXY(new LogicalPosition(startLines[i], 0)).y;
            bottomOffsets[i] = editors[i].logicalPositionToXY(new LogicalPosition(endLines[i] + 1, 0)).y;
            rangeHeights[i] = bottomOffsets[i] - topOffsets[i];

            gapLines[i] = 2 * editors[i].getLineHeight();
            editorHeights[i] = editors[i].getScrollingModel().getVisibleArea().height;

            maximumOffsets[i] = ((EditorEx) editors[i]).getScrollPane().getVerticalScrollBar().getMaximum() - editorHeights[i];

            // 'shift' here - distance between editor's top and first line of range

            // make whole range visible. If possible, locate it at 'center' (1/3 of height) (or at 'preferredTopShift' if it was specified)
            // If can't show whole range - show as much as we can
            boolean canShow = 2 * gapLines[i] + rangeHeights[i] <= editorHeights[i];

            int shift = preferredTopShift != -1 ? preferredTopShift : editorHeights[i] / 3;
            topShifts[i] = canShow ? Math.min(editorHeights[i] - gapLines[i] - rangeHeights[i], shift) : gapLines[i];
        }

        int topShift = ArrayUtil.min(topShifts);

        // check if we're at the top of file
        topShift = Math.min(topShift, ArrayUtil.min(topOffsets));

        int[] offsets = new int[count];
        boolean haveEnoughSpace = true;
        for (int i = 0; i < count; i++) {
            offsets[i] = topOffsets[i] - topShift;
            haveEnoughSpace &= maximumOffsets[i] > offsets[i];
        }

        if (haveEnoughSpace) {
            return offsets;
        }

        // One of the ranges is at end of file - we can't scroll where we want to.
        topShift = 0;
        for (int i = 0; i < count; i++) {
            topShift = Math.max(topOffsets[i] - maximumOffsets[i], topShift);
        }

        for (int i = 0; i < count; i++) {
            // Try to show as much of range as we can (even if it breaks alignment)
            offsets[i] = topOffsets[i] - topShift + Math.max(topShift + rangeHeights[i] + gapLines[i] - editorHeights[i], 0);

            // always show top of the range
            offsets[i] = Math.min(offsets[i], topOffsets[i] - gapLines[i]);
        }

        return offsets;
    }

    private static class Anchor {
        public final int masterStartOffset;
        public final int masterEndOffset;
        public final int slaveStartOffset;
        public final int slaveEndOffset;

        public Anchor(int masterStartOffset, int masterEndOffset, int slaveStartOffset, int slaveEndOffset) {
            this.masterStartOffset = masterStartOffset;
            this.masterEndOffset = masterEndOffset;
            this.slaveStartOffset = slaveStartOffset;
            this.slaveEndOffset = slaveEndOffset;
        }
    }
}
