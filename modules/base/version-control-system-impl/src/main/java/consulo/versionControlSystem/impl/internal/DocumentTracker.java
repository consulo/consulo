/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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

import consulo.application.ApplicationManager;
import consulo.application.event.ApplicationAdapter;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.component.ProcessCanceledException;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.internal.VcsRange;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks changes between two documents:
 * <ul>
 *   <li>Document1 (LEFT / VCS) — the base/VCS version</li>
 *   <li>Document2 (RIGHT / current) — the working document</li>
 * </ul>
 *
 * <p>Any external calls (e.g. Document modifications) must NOT be made while holding
 * the internal lock, to avoid deadlocks with the application Read/Write action.</p>
 *
 * <p>Both documents must be modified on EDT only.</p>
 */
public class DocumentTracker implements Disposable {
    private static final Logger LOG = Logger.getInstance(DocumentTracker.class);

    // ReentrantLock allows re-entrant acquisition (e.g. unfreeze -> refreshDirty).
    private final ReentrantLock myLock = new ReentrantLock();

    private final List<Handler> myHandlers = new ArrayList<>();

    // Document1 = LEFT = VCS/base, Document2 = RIGHT = current
    private final Document myDocument1;
    private final Document myDocument2;

    private final FreezeHelper myFreezeHelper = new FreezeHelper();

    private List<Block> myBlocks = Collections.emptyList();
    private boolean myIsDirty = false;
    private boolean myIsDisposed = false;

    private final MyDocumentListener myDocumentListener1;
    private final MyDocumentListener myDocumentListener2;
    private final ApplicationAdapter myApplicationListener;

    public DocumentTracker(Document document1, Document document2) {
        assert document1 != document2;
        myDocument1 = document1;
        myDocument2 = document2;

        myDocumentListener1 = new MyDocumentListener(Side.LEFT);
        myDocumentListener2 = new MyDocumentListener(Side.RIGHT);
        myDocument1.addDocumentListener(myDocumentListener1);
        myDocument2.addDocumentListener(myDocumentListener2);

        myApplicationListener = new MyApplicationListener();
        ApplicationManager.getApplication().addApplicationListener(myApplicationListener);

        // Compute initial blocks
        myIsDirty = true;
        refreshDirty(false);
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
            myBlocks = Collections.emptyList();
        }
        finally {
            myLock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Handler management
    // -------------------------------------------------------------------------

    public void addHandler(Handler handler) {
        myHandlers.add(handler);
    }

    // -------------------------------------------------------------------------
    // Block access
    // -------------------------------------------------------------------------

    /** Returns an unmodifiable snapshot of the current blocks. Must be called on EDT or under lock. */
    public List<Block> getBlocks() {
        return Collections.unmodifiableList(myBlocks);
    }

    // -------------------------------------------------------------------------
    // Locking
    // -------------------------------------------------------------------------

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

    /** Alias for {@link #withRead(Callable)} — matches JetBrains naming convention. */
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

    // -------------------------------------------------------------------------
    // Freeze / Unfreeze
    // -------------------------------------------------------------------------

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
        // Trigger a refresh after the freeze is lifted (re-entrant safe).
        if (!isFrozen() && myIsDirty) {
            refreshDirty(false);
        }
    }

    /**
     * Freezes both sides, executes {@code task}, then unfreezes both sides.
     * Any document changes that occurred during the freeze are reconciled on unfreeze.
     */
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

    // -------------------------------------------------------------------------
    // Content access (freeze-aware)
    // -------------------------------------------------------------------------

    /**
     * Returns the content of the given side, using the frozen snapshot if the side is frozen.
     */
    public CharSequence getContent(Side side) {
        myLock.lock();
        try {
            CharSequence frozen = myFreezeHelper.getFrozenContent(side);
            if (frozen != null) return frozen;
            return getDocument(side).getImmutableCharSequence();
        }
        finally {
            myLock.unlock();
        }
    }

    /** Returns the document for the given side (LEFT = VCS/base, RIGHT = current). */
    public Document getDocument(Side side) {
        return side.select(myDocument1, myDocument2);
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    /**
     * Re-computes blocks by diffing the two documents. Safe to call re-entrantly
     * (uses {@link ReentrantLock}).
     */
    @RequiredUIAccess
    public void refreshDirty(boolean fastRefresh) {
        if (myIsDisposed) return;
        if (myFreezeHelper.isFrozen()) return;

        myLock.lock();
        try {
            if (!myIsDirty) return;

            try {
                if (StringUtil.equals(myDocument1.getImmutableCharSequence(),
                    myDocument2.getImmutableCharSequence())) {
                    updateBlocks(Collections.emptyList());
                    return;
                }

                // document2 = current, document1 = VCS/base
                List<VcsRange> ranges =
                    RangesBuilder.createRanges(myDocument2, myDocument1, false);

                List<Block> newBlocks = new ArrayList<>(ranges.size());
                for (VcsRange r : ranges) {
                    newBlocks.add(new Block(r.getLine1(), r.getLine2(),
                        r.getVcsLine1(), r.getVcsLine2()));
                }
                updateBlocks(newBlocks);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (FilesTooBigForDiffException e) {
                LOG.warn("DocumentTracker: file too big for diff");
                updateBlocks(Collections.emptyList());
            }
            catch (Throwable e) {
                LOG.error("DocumentTracker: error refreshing blocks " +
                    "(doc1=" + myDocument1 + ", doc2=" + myDocument2 + ")", e);
                updateBlocks(Collections.emptyList());
            }
            finally {
                myIsDirty = false;
            }
        }
        finally {
            myLock.unlock();
        }
    }

    private void updateBlocks(List<Block> newBlocks) {
        assert myLock.isHeldByCurrentThread();
        myBlocks = newBlocks;
        // Blocks are freshly computed here (myIsDirty is reset to false in the
        // finally block of refreshDirty(), but that happens AFTER this call).
        // Pass false ("not dirty") so handlers know the blocks are up-to-date.
        for (Handler h : myHandlers) {
            h.afterBulkRangeChange(false);
        }
    }

    private void markDirty() {
        myLock.lock();
        try {
            myIsDirty = true;
        }
        finally {
            myLock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Public API types
    // -------------------------------------------------------------------------

    /**
     * Handler for tracking changes to the block list.
     * All methods are invoked while holding the internal lock (except freeze/unfreeze callbacks).
     */
    public interface Handler {
        /** Called after a bulk change to the block list. */
        default void afterBulkRangeChange(boolean isDirty) {}

        /** Called when one side starts being frozen (bulk-update or document swap). */
        default void onFreeze(Side side) {}

        /** Called when one side stops being frozen. */
        default void onUnfreeze(Side side) {}

        /** Called when the tracker transitions from unfrozen to frozen (either side). */
        default void onFreeze() {}

        /** Called when the tracker transitions from frozen to completely unfrozen. */
        default void onUnfreeze() {}
    }

    /**
     * Represents a changed region between the two documents.
     * <ul>
     *   <li>{@link #getStart()} / {@link #getEnd()} — line range in the current (RIGHT) document</li>
     *   <li>{@link #getVcsStart()} / {@link #getVcsEnd()} — line range in the VCS/base (LEFT) document</li>
     * </ul>
     */
    public static final class Block {
        private final int myLine1;    // current doc (RIGHT)
        private final int myLine2;    // current doc (RIGHT)
        private final int myVcsLine1; // VCS/base doc (LEFT)
        private final int myVcsLine2; // VCS/base doc (LEFT)

        @Nullable
        private Object myData;

        public Block(int line1, int line2, int vcsLine1, int vcsLine2) {
            myLine1 = line1;
            myLine2 = line2;
            myVcsLine1 = vcsLine1;
            myVcsLine2 = vcsLine2;
        }

        /** First changed line in the current document (inclusive). */
        public int getStart() { return myLine1; }

        /** First line after the changed region in the current document (exclusive). */
        public int getEnd() { return myLine2; }

        /** First changed line in the VCS/base document (inclusive). */
        public int getVcsStart() { return myVcsLine1; }

        /** First line after the changed region in the VCS/base document (exclusive). */
        public int getVcsEnd() { return myVcsLine2; }

        @Nullable
        public Object getData() { return myData; }

        public void setData(@Nullable Object data) { myData = data; }

        /**
         * Derives a {@link VcsRange} from this block.
         * Callers should cache this if used frequently.
         */
        public VcsRange toVcsRange() {
            return new VcsRange(myLine1, myLine2, myVcsLine1, myVcsLine2);
        }

        @Override
        public String toString() {
            return "Block[" + myLine1 + "-" + myLine2 + " / vcs:" + myVcsLine1 + "-" + myVcsLine2 + "]";
        }
    }

    // -------------------------------------------------------------------------
    // Private: FreezeHelper
    // -------------------------------------------------------------------------

    private class FreezeHelper {
        @Nullable FreezeData myData1; // LEFT = VCS/base
        @Nullable FreezeData myData2; // RIGHT = current

        boolean isFrozen(Side side) { return getData(side) != null; }
        boolean isFrozen() { return isFrozen(Side.LEFT) || isFrozen(Side.RIGHT); }

        @Nullable
        CharSequence getFrozenContent(Side side) {
            FreezeData data = getData(side);
            return data != null ? data.textBeforeFreeze : null;
        }

        void freeze(Side side) {
            boolean wasFrozen = isFrozen();
            FreezeData data = getData(side);
            if (data == null) {
                data = new FreezeData(getDocument(side).getImmutableCharSequence());
                setData(side, data);
                if (!wasFrozen) notifyGlobalFreeze();
                notifyFreeze(side);
            }
            data.counter++;
        }

        void unfreeze(Side side) {
            FreezeData data = getData(side);
            if (data == null || data.counter == 0) {
                LOG.error("DocumentTracker is not frozen: " + side +
                    " data1=" + (myData1 != null ? myData1.counter : -1) +
                    " data2=" + (myData2 != null ? myData2.counter : -1));
                return;
            }
            data.counter--;
            if (data.counter == 0) {
                setData(side, null);
                myIsDirty = true;
                notifyUnfreeze(side);
                if (!isFrozen()) notifyGlobalUnfreeze();
            }
        }

        @Nullable FreezeData getData(Side side) { return side.select(myData1, myData2); }
        void setData(Side side, @Nullable FreezeData data) {
            if (side.isLeft()) myData1 = data; else myData2 = data;
        }

        private void notifyFreeze(Side side)       { for (Handler h : myHandlers) h.onFreeze(side); }
        private void notifyUnfreeze(Side side)     { for (Handler h : myHandlers) h.onUnfreeze(side); }
        private void notifyGlobalFreeze()          { for (Handler h : myHandlers) h.onFreeze(); }
        private void notifyGlobalUnfreeze()        { for (Handler h : myHandlers) h.onUnfreeze(); }
    }

    private static class FreezeData {
        final CharSequence textBeforeFreeze;
        int counter;

        FreezeData(CharSequence text) {
            textBeforeFreeze = text;
            counter = 0;
        }
    }

    // -------------------------------------------------------------------------
    // Private: Document listeners
    // -------------------------------------------------------------------------

    private class MyDocumentListener extends DocumentAdapter {
        private final Side mySide;

        MyDocumentListener(Side side) {
            mySide = side;
        }

        @Override
        public void documentChanged(DocumentEvent e) {
            if (myIsDisposed || myFreezeHelper.isFrozen(mySide)) return;
            markDirty();
        }

        @Override
        public void bulkUpdateStarting(Document document) {
            freeze(mySide);
        }

        @Override
        public void bulkUpdateFinished(Document document) {
            unfreeze(mySide);
        }
    }

    private class MyApplicationListener extends ApplicationAdapter {
        @Override
        public void afterWriteActionFinished(Object action) {
            refreshDirty(true);
        }
    }
}
