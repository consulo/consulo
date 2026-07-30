/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.event.ApplicationListener;
import consulo.component.ProcessCanceledException;
import consulo.diff.comparison.TrimUtil;
import consulo.diff.comparison.iterable.DiffIterableUtil;
import consulo.diff.comparison.iterable.FairDiffIterable;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.util.DiffRangeUtil;
import consulo.diff.util.LineOffsets;
import consulo.diff.util.LineOffsetsUtil;
import consulo.diff.util.Range;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.PeekableIteratorWrapper;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Any external calls (ex: Document modifications) must be avoided under LOCK,
 * to avoid deadlocks with application Read/Write action and ChangeListManager.
 * <p>
 * Tracker assumes that both documents are modified on EDT only.
 * <p>
 * Blocks are modified on EDT and under LOCK. As write actions do not run on EDT,
 * {@link #refreshDirty} additionally runs under a read action, so the document contents it reads
 * cannot change while the dirty blocks are re-diffed against them.
 */
public class DocumentTracker implements Disposable {
    private static final Logger LOG = Logger.getInstance(DocumentTracker.class);

    private final ReentrantLock myLock = new ReentrantLock();

    private final List<Handler> myHandlers = new ArrayList<>();

    private final Document myDocument1;
    private final Document myDocument2;

    private final LineTracker myTracker;
    private final FreezeHelper myFreezeHelper = new FreezeHelper();

    private boolean myIsDisposed = false;

    private final MyDocumentListener myDocumentListener1;
    private final MyDocumentListener myDocumentListener2;
    private final ApplicationListener myApplicationListener;

    public DocumentTracker(Document document1, Document document2) {
        assert document1 != document2;
        myDocument1 = document1;
        myDocument2 = document2;

        List<Range> changes;
        if (myDocument1.getImmutableCharSequence() == myDocument2.getImmutableCharSequence()) {
            changes = Collections.emptyList();
        }
        else {
            FairDiffIterable iterable = RangesBuilder.compareLines(myDocument1.getImmutableCharSequence(),
                myDocument2.getImmutableCharSequence(),
                LineOffsetsUtil.create(myDocument1),
                LineOffsetsUtil.create(myDocument2));
            changes = new ArrayList<>();
            for (Range range : iterable.iterateChanges()) {
                changes.add(range);
            }
        }
        myTracker = new LineTracker(myHandlers, changes);

        myDocumentListener1 = new MyDocumentListener(Side.LEFT);
        myDocumentListener2 = new MyDocumentListener(Side.RIGHT);
        myDocument1.addDocumentListener(myDocumentListener1);
        myDocument2.addDocumentListener(myDocumentListener2);

        myApplicationListener = new MyApplicationListener();
        ApplicationManager.getApplication().addApplicationListener(myApplicationListener);
    }

    @RequiredUIAccess
    @Override
    public void dispose() {
        if (myIsDisposed) return;
        myIsDisposed = true;

        myDocument1.removeDocumentListener(myDocumentListener1);
        myDocument2.removeDocumentListener(myDocumentListener2);
        ApplicationManager.getApplication().removeApplicationListener(myApplicationListener);

        myLock.lock();
        try {
            myTracker.destroy();
        }
        finally {
            myLock.unlock();
        }
    }

    public void addHandler(Handler handler) {
        myHandlers.add(handler);
    }

    public List<Block> getBlocks() {
        return myTracker.getBlocks();
    }

    public <T> T withRead(Callable<T> task) {
        myLock.lock();
        try {
            return task.call();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            myLock.unlock();
        }
    }

    public <T> T readLock(Callable<T> task) {
        return withRead(task);
    }

    public void withWrite(Runnable task) {
        myLock.lock();
        try {
            task.run();
        }
        finally {
            myLock.unlock();
        }
    }

    public boolean isLockHeldByCurrentThread() {
        return myLock.isHeldByCurrentThread();
    }

    public boolean isFrozen() {
        myLock.lock();
        try {
            return myFreezeHelper.isFrozen();
        }
        finally {
            myLock.unlock();
        }
    }

    public void freeze(Side side) {
        myLock.lock();
        try {
            myFreezeHelper.freeze(side);
        }
        finally {
            myLock.unlock();
        }
    }

    @RequiredUIAccess
    public void unfreeze(Side side) {
        myLock.lock();
        try {
            myFreezeHelper.unfreeze(side);
        }
        finally {
            myLock.unlock();
        }
    }

    @RequiredUIAccess
    public void doFrozen(Runnable task) {
        doFrozen(Side.LEFT, () -> doFrozen(Side.RIGHT, task));
    }

    @RequiredUIAccess
    public void doFrozen(Side side, Runnable task) {
        freeze(side);
        try {
            task.run();
        }
        finally {
            unfreeze(side);
        }
    }

    @RequiredUIAccess
    public boolean setFrozenState(List<Range> lineRanges) {
        if (myIsDisposed) return false;
        assert myFreezeHelper.isFrozen(Side.LEFT) && myFreezeHelper.isFrozen(Side.RIGHT);

        myLock.lock();
        try {
            CharSequence content1 = getContent(Side.LEFT);
            CharSequence content2 = getContent(Side.RIGHT);
            if (!RangesBuilder.isValidRanges(content1, content2,
                LineOffsetsUtil.create(content1), LineOffsetsUtil.create(content2), lineRanges)) {
                return false;
            }

            myTracker.setRanges(lineRanges, true);
            return true;
        }
        finally {
            myLock.unlock();
        }
    }

    public CharSequence getContent(Side side) {
        myLock.lock();
        try {
            CharSequence frozenContent = myFreezeHelper.getFrozenContent(side);
            if (frozenContent != null) return frozenContent;
            return getDocument(side).getImmutableCharSequence();
        }
        finally {
            myLock.unlock();
        }
    }

    public Document getDocument(Side side) {
        return side.select(myDocument1, myDocument2);
    }

    @RequiredUIAccess
    public void refreshDirty(boolean fastRefresh) {
        refreshDirty(fastRefresh, false);
    }

    public void refreshDirty(boolean fastRefresh, boolean forceInFrozen) {
        if (myIsDisposed) return;
        if (!forceInFrozen && myFreezeHelper.isFrozen()) return;

        myLock.lock();
        try {
            if (myTracker.isDirty() &&
                !getBlocks().isEmpty() &&
                StringUtil.equals(myDocument1.getImmutableCharSequence(), myDocument2.getImmutableCharSequence())) {
                myTracker.setRanges(Collections.emptyList(), false);
                return;
            }

            try {
                myTracker.refreshDirty(myDocument1.getImmutableCharSequence(),
                    myDocument2.getImmutableCharSequence(),
                    LineOffsetsUtil.create(myDocument1),
                    LineOffsetsUtil.create(myDocument2),
                    fastRefresh);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                LOG.error("document1: " + myDocument1 + ", document2: " + myDocument2 + ", " +
                    "isFrozen1: " + myFreezeHelper.isFrozen(Side.LEFT) + ", isFrozen2: " + myFreezeHelper.isFrozen(Side.RIGHT), e);

                myTracker.resetTrackerState(DiffImplUtil.getLineCount(myDocument1), DiffImplUtil.getLineCount(myDocument2));
            }
        }
        finally {
            myLock.unlock();
        }
    }

    private void unfreeze(Side side, CharSequence oldText) {
        assert myLock.isHeldByCurrentThread();
        if (myIsDisposed) return;

        Document newDocument = getDocument(side);
        FairDiffIterable iterable = RangesBuilder.compareLines(oldText, newDocument.getImmutableCharSequence(),
            LineOffsetsUtil.create(oldText), LineOffsetsUtil.create(newDocument));
        if (iterable.changes().hasNext()) {
            myTracker.rangesChanged(side, iterable);
        }
    }

    /**
     * All methods are invoked under LOCK.
     */
    public interface Handler {
        default void onRangeRefreshed(Block before, List<Block> after) {}

        default void onRangesChanged(List<Block> before, Block after) {}

        default void onRangeShifted(Block before, Block after) {}

        /**
         * In some cases, we might want to refresh multiple adjustent blocks together.
         * This method allows to veto such merging (ex: if blocks share conflicting sets of flags).
         *
         * @return true if blocks are allowed to be merged
         */
        default boolean mergeRanges(Block block1, Block block2, Block merged) {
            return true;
        }

        default void afterBulkRangeChange(boolean isDirty) {}

        default void onFreeze(Side side) {}

        default void onUnfreeze(Side side) {}

        default void onFreeze() {}

        default void onUnfreeze() {}
    }

    public static final class Block implements BlockI {
        private final Range myRange;
        private final boolean myIsDirty;
        private final boolean myIsTooBig;

        @Nullable
        private Object myData;

        Block(Range range, boolean isDirty, boolean isTooBig) {
            myRange = range;
            myIsDirty = isDirty;
            myIsTooBig = isTooBig;
        }

        public Range getRange() {
            return myRange;
        }

        @Override
        public int getStart() {
            return myRange.start2;
        }

        @Override
        public int getEnd() {
            return myRange.end2;
        }

        @Override
        public int getVcsStart() {
            return myRange.start1;
        }

        @Override
        public int getVcsEnd() {
            return myRange.end1;
        }

        @Nullable
        public Object getData() {
            return myData;
        }

        public void setData(@Nullable Object data) {
            myData = data;
        }

        boolean isDirty() {
            return myIsDirty;
        }

        boolean isTooBig() {
            return myIsTooBig;
        }

        @Override
        public String toString() {
            return "Block" + myRange + (myIsDirty ? " dirty" : "") + (myIsTooBig ? " tooBig" : "");
        }
    }

    private class FreezeHelper {
        @Nullable
        private FreezeData myData1; // LEFT = VCS/base
        @Nullable
        private FreezeData myData2; // RIGHT = current

        boolean isFrozen(Side side) {
            return getData(side) != null;
        }

        boolean isFrozen() {
            return isFrozen(Side.LEFT) || isFrozen(Side.RIGHT);
        }

        void freeze(Side side) {
            boolean wasFrozen = isFrozen();

            FreezeData data = getData(side);
            if (data == null) {
                data = new FreezeData(getDocument(side).getImmutableCharSequence());
                setData(side, data);
                data.counter++;

                if (wasFrozen) onFreeze();
                onFreeze(side);
            }
            else {
                data.counter++;
            }
        }

        void unfreeze(Side side) {
            FreezeData data = getData(side);
            if (data == null || data.counter == 0) {
                LOG.error("DocumentTracker is not freezed: " + side + ", " +
                    (myData1 != null ? myData1.counter : -1) + ", " + (myData2 != null ? myData2.counter : -1));
                return;
            }

            data.counter--;

            if (data.counter == 0) {
                DocumentTracker.this.unfreeze(side, data.textBeforeFreeze);

                setData(side, null);
                refreshDirty(false);
                onUnfreeze(side);
                if (!isFrozen()) onUnfreeze();
            }
        }

        @Nullable
        private FreezeData getData(Side side) {
            return side.select(myData1, myData2);
        }

        private void setData(Side side, @Nullable FreezeData data) {
            if (side.isLeft()) {
                myData1 = data;
            }
            else {
                myData2 = data;
            }
        }

        @Nullable
        CharSequence getFrozenContent(Side side) {
            FreezeData data = getData(side);
            return data != null ? data.textBeforeFreeze : null;
        }

        private void onFreeze(Side side) {
            for (Handler h : myHandlers) h.onFreeze(side);
        }

        private void onUnfreeze(Side side) {
            for (Handler h : myHandlers) h.onUnfreeze(side);
        }

        private void onFreeze() {
            for (Handler h : myHandlers) h.onFreeze();
        }

        private void onUnfreeze() {
            for (Handler h : myHandlers) h.onUnfreeze();
        }
    }

    private static class FreezeData {
        final CharSequence textBeforeFreeze;
        int counter;

        FreezeData(CharSequence text) {
            textBeforeFreeze = text;
            counter = 0;
        }
    }

    private class MyDocumentListener extends DocumentAdapter {
        private final Side mySide;
        private int myLine1 = 0;
        private int myLine2 = 0;

        MyDocumentListener(Side side) {
            mySide = side;
            if (getDocument(side).isInBulkUpdate()) freeze(side);
        }

        @Override
        public void beforeDocumentChange(DocumentEvent e) {
            if (myIsDisposed || myFreezeHelper.isFrozen(mySide)) return;

            Document document = getDocument(mySide);
            myLine1 = document.getLineNumber(e.getOffset());
            if (e.getOldLength() == 0) {
                myLine2 = myLine1 + 1;
            }
            else {
                myLine2 = document.getLineNumber(e.getOffset() + e.getOldLength()) + 1;
            }
        }

        @Override
        public void documentChanged(DocumentEvent e) {
            if (myIsDisposed || myFreezeHelper.isFrozen(mySide)) return;

            Document document = getDocument(mySide);
            int newLine2;
            if (e.getNewLength() == 0) {
                newLine2 = myLine1 + 1;
            }
            else {
                newLine2 = document.getLineNumber(e.getOffset() + e.getNewLength()) + 1;
            }

            int[] affected = getAffectedRange(myLine1, myLine2, newLine2, e);
            int startLine = affected[0];
            int afterLength = affected[1];
            int beforeLength = affected[2];

            myLock.lock();
            try {
                myTracker.rangeChanged(mySide, startLine, beforeLength, afterLength);
            }
            finally {
                myLock.unlock();
            }
        }

        @Override
        public void bulkUpdateStarting(Document document) {
            freeze(mySide);
        }

        @Override
        public void bulkUpdateFinished(Document document) {
            unfreeze(mySide);
        }

        private int[] getAffectedRange(int line1, int oldLine2, int newLine2, DocumentEvent e) {
            int afterLength = newLine2 - line1;
            int beforeLength = oldLine2 - line1;

            // Whole line insertion / deletion
            if (e.getOldLength() == 0 && e.getNewLength() != 0) {
                if (StringUtil.endsWithChar(e.getNewFragment(), '\n') && isNewlineBefore(e)) {
                    return new int[]{line1, afterLength - 1, beforeLength - 1};
                }
                if (StringUtil.startsWithChar(e.getNewFragment(), '\n') && isNewlineAfter(e)) {
                    return new int[]{line1 + 1, afterLength - 1, beforeLength - 1};
                }
            }
            if (e.getOldLength() != 0 && e.getNewLength() == 0) {
                if (StringUtil.endsWithChar(e.getOldFragment(), '\n') && isNewlineBefore(e)) {
                    return new int[]{line1, afterLength - 1, beforeLength - 1};
                }
                if (StringUtil.startsWithChar(e.getOldFragment(), '\n') && isNewlineAfter(e)) {
                    return new int[]{line1 + 1, afterLength - 1, beforeLength - 1};
                }
            }

            return new int[]{line1, afterLength, beforeLength};
        }

        private boolean isNewlineBefore(DocumentEvent e) {
            if (e.getOffset() == 0) return true;
            return e.getDocument().getImmutableCharSequence().charAt(e.getOffset() - 1) == '\n';
        }

        private boolean isNewlineAfter(DocumentEvent e) {
            CharSequence text = e.getDocument().getImmutableCharSequence();
            if (e.getOffset() + e.getNewLength() == text.length()) return true;
            return text.charAt(e.getOffset() + e.getNewLength()) == '\n';
        }
    }

    private class MyApplicationListener implements ApplicationListener {
        @Override
        public void afterWriteActionFinished(Object action) {
            Application application = ApplicationManager.getApplication();
            if (application.isDispatchThread()) {
                application.runReadAction(() -> refreshDirty(true));
            }
            else {
                application.invokeLater(() -> application.runReadAction(() -> refreshDirty(true)));
            }
        }
    }

    private static class LineTracker {
        private final List<Handler> myHandlers;
        private List<Block> myBlocks;
        private boolean myIsDirty = false;
        private boolean myForceMergeNearbyBlocks = false;

        LineTracker(List<Handler> handlers, List<Range> originalChanges) {
            myHandlers = handlers;
            List<Block> blocks = new ArrayList<>(originalChanges.size());
            for (Range range : originalChanges) {
                blocks.add(new Block(range, false, false));
            }
            myBlocks = blocks;
        }

        List<Block> getBlocks() {
            return myBlocks;
        }

        boolean isDirty() {
            return myIsDirty;
        }

        void setRanges(List<Range> ranges, boolean dirty) {
            List<Block> newBlocks = new ArrayList<>(ranges.size());
            for (Range range : ranges) {
                newBlocks.add(new Block(range, dirty, false));
            }
            for (Block block : newBlocks) {
                onRangesChanged(Collections.emptyList(), block);
            }

            myBlocks = newBlocks;
            myIsDirty = dirty;
            myForceMergeNearbyBlocks = false;

            afterBulkRangeChange(myIsDirty);
        }

        void destroy() {
            myBlocks = Collections.emptyList();
        }

        void refreshDirty(CharSequence text1,
                          CharSequence text2,
                          LineOffsets lineOffsets1,
                          LineOffsets lineOffsets2,
                          boolean fastRefresh) {
            if (!myIsDirty) return;

            BlocksRefresher.Result result =
                new BlocksRefresher(myHandlers, text1, text2, lineOffsets1, lineOffsets2, myForceMergeNearbyBlocks).refresh(myBlocks, fastRefresh);

            myBlocks = result.newBlocks;
            myIsDirty = false;
            myForceMergeNearbyBlocks = false;

            afterBulkRangeChange(myIsDirty);
        }

        /**
         * Reset to the simplest valid state. Hopefully, the next full refresh will be successful.
         */
        void resetTrackerState(int lineCount1, int lineCount2) {
            Range fullRange = new Range(0, lineCount1, 0, lineCount2);
            Block dirtyBlock = new Block(fullRange, true, false);
            onRangesChanged(Collections.emptyList(), dirtyBlock);

            myBlocks = Collections.singletonList(dirtyBlock);
            myIsDirty = true;
            myForceMergeNearbyBlocks = false;

            afterBulkRangeChange(myIsDirty);
        }

        void rangeChanged(Side side, int startLine, int beforeLength, int afterLength) {
            RangeChangeHandler.Result data = new RangeChangeHandler().run(myBlocks, side, startLine, beforeLength, afterLength);

            onRangesChanged(data.affectedBlocks, data.newAffectedBlock);
            for (int i = 0; i < data.afterBlocks.size(); i++) {
                onRangeShifted(data.afterBlocks.get(i), data.newAfterBlocks.get(i));
            }

            myBlocks = data.newBlocks;
            myIsDirty = !myBlocks.isEmpty();

            afterBulkRangeChange(myIsDirty);
        }

        void rangesChanged(Side side, FairDiffIterable iterable) {
            List<Block> newBlocks = new BulkRangeChangeHandler(myHandlers, myBlocks, side).run(iterable);

            myBlocks = newBlocks;
            myIsDirty = !newBlocks.isEmpty();
            myForceMergeNearbyBlocks = myIsDirty;

            afterBulkRangeChange(myIsDirty);
        }

        private void onRangesChanged(List<Block> before, Block after) {
            for (Handler h : myHandlers) h.onRangesChanged(before, after);
        }

        private void onRangeShifted(Block before, Block after) {
            for (Handler h : myHandlers) h.onRangeShifted(before, after);
        }

        private void afterBulkRangeChange(boolean isDirty) {
            for (Handler h : myHandlers) h.afterBulkRangeChange(isDirty);
        }
    }

    private static class RangeChangeHandler {
        Result run(List<Block> blocks,
                   Side side,
                   int startLine,
                   int beforeLength,
                   int afterLength) {
            int endLine = startLine + beforeLength;
            int rangeSizeDelta = afterLength - beforeLength;

            List<Block> beforeBlocks = new ArrayList<>();
            List<Block> affectedBlocks = new ArrayList<>();
            List<Block> afterBlocks = new ArrayList<>();
            sortRanges(blocks, side, startLine, endLine, beforeBlocks, affectedBlocks, afterBlocks);

            int ourToOtherShift = getOurToOtherShift(side, beforeBlocks);

            Block newAffectedBlock = getNewAffectedBlock(side, startLine, endLine, rangeSizeDelta, ourToOtherShift, affectedBlocks);
            List<Block> newAfterBlocks = new ArrayList<>(afterBlocks.size());
            for (Block block : afterBlocks) {
                newAfterBlocks.add(shiftBlock(block, side, rangeSizeDelta));
            }

            List<Block> newBlocks = new ArrayList<>(beforeBlocks.size() + newAfterBlocks.size() + 1);
            newBlocks.addAll(beforeBlocks);
            newBlocks.add(newAffectedBlock);
            newBlocks.addAll(newAfterBlocks);

            return new Result(beforeBlocks, newBlocks,
                affectedBlocks, afterBlocks,
                newAffectedBlock, newAfterBlocks);
        }

        private void sortRanges(List<Block> blocks,
                                Side side,
                                int line1,
                                int line2,
                                List<Block> beforeChange,
                                List<Block> affected,
                                List<Block> afterChange) {
            for (Block block : blocks) {
                if (rangeEnd(block.getRange(), side) < line1) {
                    beforeChange.add(block);
                }
                else if (rangeStart(block.getRange(), side) > line2) {
                    afterChange.add(block);
                }
                else {
                    affected.add(block);
                }
            }
        }

        private int getOurToOtherShift(Side side, List<Block> beforeBlocks) {
            if (beforeBlocks.isEmpty()) return 0;
            Range lastBefore = beforeBlocks.get(beforeBlocks.size() - 1).getRange();
            return rangeEnd(lastBefore, side.other()) - rangeEnd(lastBefore, side);
        }

        private Block getNewAffectedBlock(Side side,
                                          int startLine,
                                          int endLine,
                                          int rangeSizeDelta,
                                          int ourToOtherShift,
                                          List<Block> affectedBlocks) {
            int rangeStart;
            int rangeEnd;
            int rangeStartOther;
            int rangeEndOther;

            if (affectedBlocks.isEmpty()) {
                rangeStart = startLine;
                rangeEnd = endLine + rangeSizeDelta;
                rangeStartOther = startLine + ourToOtherShift;
                rangeEndOther = endLine + ourToOtherShift;
            }
            else {
                Range firstAffected = affectedBlocks.get(0).getRange();
                Range lastAffected = affectedBlocks.get(affectedBlocks.size() - 1).getRange();

                int affectedStart = rangeStart(firstAffected, side);
                int affectedStartOther = rangeStart(firstAffected, side.other());
                int affectedEnd = rangeEnd(lastAffected, side);
                int affectedEndOther = rangeEnd(lastAffected, side.other());

                if (affectedStart <= startLine) {
                    rangeStart = affectedStart;
                    rangeStartOther = affectedStartOther;
                }
                else {
                    rangeStart = startLine;
                    rangeStartOther = startLine + (affectedStartOther - affectedStart);
                }

                if (affectedEnd >= endLine) {
                    rangeEnd = affectedEnd + rangeSizeDelta;
                    rangeEndOther = affectedEndOther;
                }
                else {
                    rangeEnd = endLine + rangeSizeDelta;
                    rangeEndOther = endLine + (affectedEndOther - affectedEnd);
                }
            }

            boolean isTooBig = anyTooBig(affectedBlocks);
            Range range = createRange(side, rangeStart, rangeEnd, rangeStartOther, rangeEndOther);
            return new Block(range, true, isTooBig);
        }

        static class Result {
            final List<Block> beforeBlocks;
            final List<Block> newBlocks;
            final List<Block> affectedBlocks;
            final List<Block> afterBlocks;
            final Block newAffectedBlock;
            final List<Block> newAfterBlocks;

            Result(List<Block> beforeBlocks, List<Block> newBlocks,
                   List<Block> affectedBlocks, List<Block> afterBlocks,
                   Block newAffectedBlock, List<Block> newAfterBlocks) {
                this.beforeBlocks = beforeBlocks;
                this.newBlocks = newBlocks;
                this.affectedBlocks = affectedBlocks;
                this.afterBlocks = afterBlocks;
                this.newAffectedBlock = newAffectedBlock;
                this.newAfterBlocks = newAfterBlocks;
            }
        }
    }

    /**
     * We use line numbers in 3 documents:
     * A: Line number in unchanged document
     * B: Line number in changed document <before> the change
     * C: Line number in changed document <after> the change
     * <p>
     * Algorithm is similar to building ranges for a merge conflict.
     * ie: B is the "Base" and A/C are "Left"/"Right". Old blocks hold the differences "A -> B",
     * changes from iterable hold the differences "B -> C". We want to construct new blocks with differences "A -> C".
     * <p>
     * We iterate all differences in 'B' order, collecting interleaving groups of differences. Each group becomes a single newBlock.
     * blockShift/changeShift indicate how 'B' line is mapped to the 'A'/'C' lines at the start of current group.
     * dirtyBlockShift/dirtyChangeShift accumulate differences from the current group.
     * <p>
     * block(otherSide -> side): A -> B
     * newBlock(otherSide -> side): A -> C
     * iterable: B -> C
     * dirtyStart, dirtyEnd: B
     * blockShift: delta B -> A
     * changeShift: delta B -> C
     */
    private static class BulkRangeChangeHandler {
        private final List<Handler> myHandlers;
        private final List<Block> myBlocks;
        private final Side mySide;

        private final List<Block> myNewBlocks = new ArrayList<>();

        private int myDirtyStart = -1;
        private int myDirtyEnd = -1;
        private final List<Block> myDirtyBlocks = new ArrayList<>();
        private boolean myDirtyBlocksModified = false;

        private int myBlockShift = 0;
        private int myChangeShift = 0;
        private int myDirtyBlockShift = 0;
        private int myDirtyChangeShift = 0;

        BulkRangeChangeHandler(List<Handler> handlers, List<Block> blocks, Side side) {
            myHandlers = handlers;
            myBlocks = blocks;
            mySide = side;
        }

        List<Block> run(FairDiffIterable iterable) {
            PeekableIteratorWrapper<Block> it1 = new PeekableIteratorWrapper<>(myBlocks.iterator());
            PeekableIteratorWrapper<Range> it2 = new PeekableIteratorWrapper<>(iterable.changes());

            while (it1.hasNext() || it2.hasNext()) {
                if (!it2.hasNext()) {
                    handleBlock(it1.next());
                    continue;
                }
                if (!it1.hasNext()) {
                    handleChange(it2.next());
                    continue;
                }

                Block block = it1.peek();
                Range range1 = block.getRange();
                Range range2 = it2.peek();

                if (rangeStart(range1, mySide) <= range2.start1) {
                    handleBlock(it1.next());
                }
                else {
                    handleChange(it2.next());
                }
            }
            flush(Integer.MAX_VALUE);

            return myNewBlocks;
        }

        private void handleBlock(Block block) {
            Range range = block.getRange();
            flush(rangeStart(range, mySide));

            myDirtyBlockShift += getRangeDelta(range, mySide);

            markDirtyRange(rangeStart(range, mySide), rangeEnd(range, mySide));

            myDirtyBlocks.add(block);
        }

        private void handleChange(Range range) {
            flush(range.start1);

            myDirtyChangeShift += getRangeDelta(range, Side.LEFT);

            markDirtyRange(range.start1, range.end1);

            myDirtyBlocksModified = true;
        }

        private void markDirtyRange(int start, int end) {
            if (myDirtyEnd == -1) {
                myDirtyStart = start;
                myDirtyEnd = end;
            }
            else {
                myDirtyEnd = Math.max(myDirtyEnd, end);
            }
        }

        private void flush(int nextLine) {
            if (myDirtyEnd != -1 && myDirtyEnd < nextLine) {
                if (myDirtyBlocksModified) {
                    boolean isTooBig = anyTooBig(myDirtyBlocks);
                    Range range = createRange(mySide,
                        myDirtyStart + myChangeShift, myDirtyEnd + myChangeShift + myDirtyChangeShift,
                        myDirtyStart + myBlockShift, myDirtyEnd + myBlockShift + myDirtyBlockShift);
                    Block newBlock = new Block(range, true, isTooBig);
                    onRangesChanged(myDirtyBlocks, newBlock);
                    myNewBlocks.add(newBlock);
                }
                else {
                    assert myDirtyBlocks.size() == 1;
                    if (myChangeShift != 0) {
                        for (Block oldBlock : myDirtyBlocks) {
                            Block newBlock = shiftBlock(oldBlock, mySide, myChangeShift);
                            onRangeShifted(oldBlock, newBlock);
                            myNewBlocks.add(newBlock);
                        }
                    }
                    else {
                        myNewBlocks.addAll(myDirtyBlocks);
                    }
                }

                myDirtyStart = -1;
                myDirtyEnd = -1;
                myDirtyBlocks.clear();
                myDirtyBlocksModified = false;

                myBlockShift += myDirtyBlockShift;
                myChangeShift += myDirtyChangeShift;
                myDirtyBlockShift = 0;
                myDirtyChangeShift = 0;
            }
        }

        private void onRangesChanged(List<Block> before, Block after) {
            for (Handler h : myHandlers) h.onRangesChanged(before, after);
        }

        private void onRangeShifted(Block before, Block after) {
            for (Handler h : myHandlers) h.onRangeShifted(before, after);
        }
    }

    private static class BlocksRefresher {
        private static final int NEARBY_BLOCKS_LINES = 30;

        private final List<Handler> myHandlers;
        private final CharSequence myText1;
        private final CharSequence myText2;
        private final LineOffsets myLineOffsets1;
        private final LineOffsets myLineOffsets2;
        private final boolean myForceMergeNearbyBlocks;

        BlocksRefresher(List<Handler> handlers,
                        CharSequence text1,
                        CharSequence text2,
                        LineOffsets lineOffsets1,
                        LineOffsets lineOffsets2,
                        boolean forceMergeNearbyBlocks) {
            myHandlers = handlers;
            myText1 = text1;
            myText2 = text2;
            myLineOffsets1 = lineOffsets1;
            myLineOffsets2 = lineOffsets2;
            myForceMergeNearbyBlocks = forceMergeNearbyBlocks;
        }

        Result refresh(List<Block> blocks, boolean fastRefresh) {
            List<Block> newBlocks = new ArrayList<>();

            processMergeableGroups(blocks, group -> {
                if (anyDirty(group)) {
                    processMergedBlocks(group, mergedBlock -> {
                        List<Block> freshBlocks = refreshMergedBlock(mergedBlock, fastRefresh);

                        onRangeRefreshed(mergedBlock.merged, freshBlocks);

                        newBlocks.addAll(freshBlocks);
                    });
                }
                else {
                    newBlocks.addAll(group);
                }
            });
            return new Result(newBlocks);
        }

        private void processMergeableGroups(List<Block> blocks, Consumer<List<Block>> processGroup) {
            if (blocks.isEmpty()) return;

            int i = 0;
            int blockStart = 0;
            while (i < blocks.size() - 1) {
                if (!shouldMergeBlocks(blocks.get(i), blocks.get(i + 1))) {
                    processGroup.accept(blocks.subList(blockStart, i + 1));
                    blockStart = i + 1;
                }
                i += 1;
            }
            processGroup.accept(blocks.subList(blockStart, i + 1));
        }

        private boolean shouldMergeBlocks(Block block1, Block block2) {
            if (myForceMergeNearbyBlocks && block2.getRange().start2 - block1.getRange().end2 < NEARBY_BLOCKS_LINES) {
                return true;
            }
            if (isWhitespaceOnlySeparated(block1, block2)) return true;
            return false;
        }

        private boolean isWhitespaceOnlySeparated(Block block1, Block block2) {
            DiffRangeUtil.LinesRange range1 = DiffRangeUtil.getLinesRange(myLineOffsets1, block1.getRange().start1, block1.getRange().end1, false);
            DiffRangeUtil.LinesRange range2 = DiffRangeUtil.getLinesRange(myLineOffsets1, block2.getRange().start1, block2.getRange().end1, false);
            int start = range1.endOffset;
            int end = range2.startOffset;
            return TrimUtil.trimStart(myText1, start, end) == end;
        }

        private void processMergedBlocks(List<Block> group, Consumer<MergedBlock> processBlock) {
            assert !group.isEmpty();

            Block merged = null;
            List<Block> original = new ArrayList<>();

            for (Block block : group) {
                if (merged == null) {
                    merged = block;
                    original.add(block);
                }
                else {
                    Block newMerged = mergeBlocks(merged, block);
                    if (newMerged != null) {
                        merged = newMerged;
                        original.add(block);
                    }
                    else {
                        processBlock.accept(new MergedBlock(merged, new ArrayList<>(original)));
                        original.clear();
                        merged = block;
                        original.add(merged);
                    }
                }
            }

            processBlock.accept(new MergedBlock(merged, new ArrayList<>(original)));
        }

        @Nullable
        private Block mergeBlocks(Block block1, Block block2) {
            boolean isDirty = block1.isDirty() || block2.isDirty();
            boolean isTooBig = block1.isTooBig() || block2.isTooBig();
            Range range = new Range(block1.getRange().start1, block2.getRange().end1,
                block1.getRange().start2, block2.getRange().end2);
            Block merged = new Block(range, isDirty, isTooBig);

            for (Handler handler : myHandlers) {
                boolean success = handler.mergeRanges(block1, block2, merged);
                if (!success) return null; // merging vetoed
            }
            return merged;
        }

        private List<Block> refreshMergedBlock(MergedBlock mergedBlock, boolean fastRefresh) {
            List<Block> freshBlocks = refreshBlock(mergedBlock.merged, fastRefresh);
            if (mergedBlock.original.size() == 1) return freshBlocks;
            if (!myForceMergeNearbyBlocks) return freshBlocks;

            // try reuse original blocks to prevent occasional 'insertion' moves
            List<Block> nonMergedFreshBlocks = new ArrayList<>();
            for (Block block : mergedBlock.original) {
                if (block.isDirty()) {
                    nonMergedFreshBlocks.addAll(refreshBlock(block, fastRefresh));
                }
                else {
                    nonMergedFreshBlocks.add(block);
                }
            }

            int oldSize = calcNonWhitespaceSize(myText1, myText2, myLineOffsets1, myLineOffsets2, nonMergedFreshBlocks);
            int newSize = calcNonWhitespaceSize(myText1, myText2, myLineOffsets1, myLineOffsets2, freshBlocks);
            if (oldSize < newSize) return nonMergedFreshBlocks;
            if (oldSize > newSize) return freshBlocks;

            int oldTotalSize = calcSize(nonMergedFreshBlocks);
            int newTotalSize = calcSize(freshBlocks);
            if (oldTotalSize <= newTotalSize) return nonMergedFreshBlocks;
            return freshBlocks;
        }

        private List<Block> refreshBlock(Block block, boolean fastRefresh) {
            if (block.getRange().isEmpty()) return Collections.emptyList();

            FairDiffIterable iterable;
            boolean isTooBig;
            if (block.isTooBig() && fastRefresh) {
                iterable = RangesBuilder.fastCompareLines(block.getRange(), myText1, myText2, myLineOffsets1, myLineOffsets2);
                isTooBig = true;
            }
            else {
                FairDiffIterable realIterable = RangesBuilder.tryCompareLines(block.getRange(), myText1, myText2, myLineOffsets1, myLineOffsets2);
                if (realIterable != null) {
                    iterable = realIterable;
                    isTooBig = false;
                }
                else {
                    iterable = RangesBuilder.fastCompareLines(block.getRange(), myText1, myText2, myLineOffsets1, myLineOffsets2);
                    isTooBig = true;
                }
            }

            List<Block> result = new ArrayList<>();
            for (Range range : iterable.iterateChanges()) {
                result.add(new Block(shiftRange(range, block.getRange().start1, block.getRange().start2), false, isTooBig));
            }
            return result;
        }

        private int calcSize(List<Block> blocks) {
            int result = 0;
            for (Block block : blocks) {
                result += block.getRange().end1 - block.getRange().start1;
                result += block.getRange().end2 - block.getRange().start2;
            }
            return result;
        }

        private int calcNonWhitespaceSize(CharSequence text1,
                                          CharSequence text2,
                                          LineOffsets lineOffsets1,
                                          LineOffsets lineOffsets2,
                                          List<Block> blocks) {
            int result = 0;
            for (Block block : blocks) {
                for (int line = block.getRange().start1; line < block.getRange().end1; line++) {
                    if (!isWhitespaceLine(text1, lineOffsets1, line)) result++;
                }
                for (int line = block.getRange().start2; line < block.getRange().end2; line++) {
                    if (!isWhitespaceLine(text2, lineOffsets2, line)) result++;
                }
            }
            return result;
        }

        private boolean isWhitespaceLine(CharSequence text, LineOffsets lineOffsets, int line) {
            int start = lineOffsets.getLineStart(line);
            int end = lineOffsets.getLineEnd(line);
            return TrimUtil.trimStart(text, start, end) == end;
        }

        private void onRangeRefreshed(Block before, List<Block> after) {
            for (Handler h : myHandlers) h.onRangeRefreshed(before, after);
        }

        static class Result {
            final List<Block> newBlocks;

            Result(List<Block> newBlocks) {
                this.newBlocks = newBlocks;
            }
        }

        static class MergedBlock {
            final Block merged;
            final List<Block> original;

            MergedBlock(Block merged, List<Block> original) {
                this.merged = merged;
                this.original = original;
            }
        }
    }

    private static boolean anyDirty(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.isDirty()) return true;
        }
        return false;
    }

    private static boolean anyTooBig(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.isTooBig()) return true;
        }
        return false;
    }

    private static int getRangeDelta(Range range, Side side) {
        int delta = DiffIterableUtil.getRangeDelta(range);
        return side.isLeft() ? delta : -delta;
    }

    private static Block shiftBlock(Block block, Side side, int delta) {
        return new Block(shiftRange(block.getRange(), side, delta), block.isDirty(), block.isTooBig());
    }

    private static Range shiftRange(Range range, Side side, int shift) {
        return side.isLeft() ? shiftRange(range, shift, 0) : shiftRange(range, 0, shift);
    }

    private static Range shiftRange(Range range, int shift1, int shift2) {
        return new Range(range.start1 + shift1, range.end1 + shift1, range.start2 + shift2, range.end2 + shift2);
    }

    private static Range createRange(Side side, int start, int end, int otherStart, int otherEnd) {
        return side.isLeft() ? new Range(start, end, otherStart, otherEnd) : new Range(otherStart, otherEnd, start, end);
    }

    private static int rangeStart(Range range, Side side) {
        return side.isLeft() ? range.start1 : range.start2;
    }

    private static int rangeEnd(Range range, Side side) {
        return side.isLeft() ? range.end1 : range.end2;
    }
}
