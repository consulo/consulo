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

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.util.LineOffsetsUtil;
import consulo.diff.util.Side;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.document.util.TextRange;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.util.UndoConstants;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.LineStatusTrackerListener;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import static consulo.versionControlSystem.UpToDateLineNumberProvider.ABSENT_LINE_NUMBER;

/**
 * Base implementation of {@link LineStatusTrackerI}.
 *
 * <p>Uses {@link DocumentTracker} to manage the two-document diff (VCS vs. current)
 * and receive change notifications via {@link DocumentTracker.Handler}.
 *
 * <ul>
 *   <li>{@link DocumentTracker} owns document listening, dirty tracking, freeze/unfreeze,
 *       and diff computation.</li>
 *   <li>This class owns the {@link VcsRange} lifecycle and fires {@link LineStatusTrackerListener}
 *       events. Highlighters are owned by the renderer, which reacts to those events on EDT.</li>
 * </ul>
 */
@SuppressWarnings("MethodMayBeStatic")
public abstract class LineStatusTrackerBase implements LineStatusTrackerI {
    protected static final Logger LOG = Logger.getInstance(LineStatusTrackerBase.class);

    protected final @Nullable Project myProject;
    protected final Document myDocument;     // current / working document
    protected final Document myVcsDocument;  // VCS / base document

    protected final Application myApplication;

    /**
     * DocumentTracker manages: document listening, dirty-state, freeze, diff computation.
     * document1 = LEFT = VCS, document2 = RIGHT = current.
     */
    protected final DocumentTracker myDocumentTracker;

    private boolean myInitialized;
    private boolean myDuringRollback;
    private boolean myReleased;

    private List<VcsRange> myRanges = Collections.emptyList();

    /** Listeners notified when ranges are rebuilt or the tracker becomes valid. */
    private final List<LineStatusTrackerListener> myListeners = new CopyOnWriteArrayList<>();

    // -------------------------------------------------------------------------

    public LineStatusTrackerBase(@Nullable Project project, Document document) {
        myDocument = document;
        myProject = project;
        myApplication = ApplicationManager.getApplication();

        DocumentFactory documentFactory = DocumentFactory.getInstance();
        myVcsDocument = documentFactory.createDocument("", true);
        myVcsDocument.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);

        // LEFT = VCS/base, RIGHT = current working document
        myDocumentTracker = new DocumentTracker(myVcsDocument, myDocument);
        myDocumentTracker.addHandler(new MyDocumentTrackerHandler());
    }

    // -------------------------------------------------------------------------
    // Abstract API
    // -------------------------------------------------------------------------

    @RequiredUIAccess
    protected boolean isDetectWhitespaceChangedLines() {
        return false;
    }

    @RequiredUIAccess
    protected void fireFileUnchanged() {
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @RequiredUIAccess
    public void setBaseRevision(CharSequence vcsContent) {
        UIAccess.assertIsUIThread();
        if (myReleased) return;

        myDocumentTracker.doFrozen(Side.LEFT, () -> {
            myVcsDocument.setReadOnly(false);
            myVcsDocument.setText(vcsContent);
            myVcsDocument.setReadOnly(true);
        });

        if (!myInitialized) {
            myDocumentTracker.withWrite(() -> myInitialized = true);
            reinstallRanges();
        }

        if (isValid()) {
            fireBecomingValid();
        }
    }

    @RequiredUIAccess
    public void dropBaseRevision() {
        UIAccess.assertIsUIThread();
        if (myReleased || !myInitialized) return;

        myDocumentTracker.doFrozen(Side.LEFT, () -> {
            myVcsDocument.setReadOnly(false);
            myVcsDocument.setText(myDocument.getImmutableCharSequence());
            myVcsDocument.setReadOnly(true);
            myDocumentTracker.withWrite(() -> {
                myInitialized = false;
                destroyRanges();
            });
        });
    }

    public void release() {
        Runnable runnable = () -> {
            if (myReleased) return;
            LOG.assertTrue(!myDuringRollback);

            myDocumentTracker.withWrite(() -> {
                myReleased = true;
                myDocumentTracker.dispose();
                destroyRanges();
            });
            myListeners.clear();
        };

        if (myApplication.isDispatchThread() && !myDuringRollback) {
            runnable.run();
        }
        else {
            myApplication.invokeLater(runnable);
        }
    }

    // -------------------------------------------------------------------------
    // Range management
    // -------------------------------------------------------------------------

    @RequiredUIAccess
    protected void reinstallRanges() {
        if (!myInitialized || myReleased || myDocumentTracker.isFrozen()) return;

        boolean[] unchanged = new boolean[1];
        myDocumentTracker.withWrite(() -> {
            destroyRanges();

            List<DocumentTracker.Block> blocks = myDocumentTracker.getBlocks();
            myRanges = new ArrayList<>(blocks.size());
            for (DocumentTracker.Block block : blocks) {
                myRanges.add(toVcsRange(block));
            }
            unchanged[0] = myRanges.isEmpty();
        });

        if (unchanged[0]) {
            fireFileUnchanged();
        }

        // Notify listeners that the range set has changed (outside the lock).
        fireRangesChanged();
    }

    private VcsRange toVcsRange(DocumentTracker.Block block) {
        return new VcsRange(block.getStart(), block.getEnd(), block.getVcsStart(), block.getVcsEnd(), getInnerRanges(block));
    }

    @SuppressWarnings("unchecked")
    private static @Nullable List<VcsRange.InnerRange> getInnerRanges(DocumentTracker.Block block) {
        return (List<VcsRange.InnerRange>)block.getData();
    }

    private void updateMissingInnerRanges() {
        if (!isDetectWhitespaceChangedLines()) return;
        if (myDocumentTracker.isFrozen()) return;

        for (DocumentTracker.Block block : myDocumentTracker.getBlocks()) {
            if (block.getData() == null) {
                block.setData(calcInnerRanges(block));
            }
        }
    }

    @RequiredUIAccess
    protected void resetInnerRanges() {
        myDocumentTracker.withWrite(() -> {
            boolean detect = isDetectWhitespaceChangedLines();
            for (DocumentTracker.Block block : myDocumentTracker.getBlocks()) {
                block.setData(detect ? calcInnerRanges(block) : null);
            }
        });
    }

    private @Nullable List<VcsRange.InnerRange> calcInnerRanges(DocumentTracker.Block block) {
        if (block.getStart() == block.getEnd() || block.getVcsStart() == block.getVcsEnd()) return null;
        return RangesBuilder.createInnerRanges(block.getRange(),
            myVcsDocument.getImmutableCharSequence(), myDocument.getImmutableCharSequence(),
            LineOffsetsUtil.create(myVcsDocument), LineOffsetsUtil.create(myDocument));
    }

    private void destroyRanges() {
        for (VcsRange range : myRanges) {
            range.invalidate();
        }
        myRanges = Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Bulk update (delegate to DocumentTracker freeze)
    // -------------------------------------------------------------------------

    @RequiredUIAccess
    public void startBulkUpdate() {
        if (myReleased) return;
        myDocumentTracker.freeze(Side.RIGHT);
        myDocumentTracker.withWrite(this::destroyRanges);
    }

    @RequiredUIAccess
    public void finishBulkUpdate() {
        if (myReleased) return;
        myDocumentTracker.unfreeze(Side.RIGHT);
        // unfreeze triggers refreshDirty -> Handler.afterBulkRangeChange -> reinstallRanges
    }

    // -------------------------------------------------------------------------
    // LineStatusTrackerI — state
    // -------------------------------------------------------------------------

    @Override
    public boolean isOperational() {
        return myDocumentTracker.withRead(() -> myInitialized && !myReleased);
    }

    @Override
    public boolean isValid() {
        return myDocumentTracker.withRead(() ->
            !isSuppressed() && !myDocumentTracker.isFrozen());
    }

    @Override
    public boolean isReleased() {
        return myReleased;
    }

    private boolean isSuppressed() {
        return !myInitialized || myReleased || myDuringRollback;
    }

    // -------------------------------------------------------------------------
    // LineStatusTrackerI — documents / project
    // -------------------------------------------------------------------------

    @Override
    public @Nullable Project getProject() {
        return myProject;
    }

    @Override
    public Document getDocument() {
        return myDocument;
    }

    @Override
    public Document getVcsDocument() {
        return myVcsDocument;
    }

    /**
     * Subclasses that track a specific file should override this.
     * Returns {@code null} by default (e.g. for in-memory / diff-viewer trackers).
     */
    @Override
    public @Nullable VirtualFile getVirtualFile() {
        return null;
    }

    // -------------------------------------------------------------------------
    // LineStatusTrackerI — range access
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot copy of the current ranges, or {@code null} if not valid.
     * Calling this twice without holding a read-lock can return different results.
     */
    @Override
    public @Nullable List<VcsRange> getRanges() {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return null;
            List<VcsRange> result = new ArrayList<>(myRanges.size());
            for (VcsRange range : myRanges) {
                result.add(new VcsRange(range));
            }
            return result;
        });
    }

    @Override
    public @Nullable List<VcsRange> getRangesForLines(BitSet lines) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return null;
            List<VcsRange> result = new ArrayList<>();
            for (VcsRange range : myRanges) {
                if (DiffImplUtil.isSelectedByLine(lines, range.getLine1(), range.getLine2())) {
                    result.add(new VcsRange(range));
                }
            }
            return result;
        });
    }

    @Override
    public @Nullable VcsRange getRangeForLine(int line) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return null;
            for (VcsRange range : myRanges) {
                if (range.isSelectedByLine(line)) return range;
            }
            return null;
        });
    }

    @Override
    public @Nullable VcsRange findRange(VcsRange range) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return null;
            for (VcsRange r : myRanges) {
                if (r.getLine1() == range.getLine1() && r.getLine2() == range.getLine2()
                    && r.getVcsLine1() == range.getVcsLine1() && r.getVcsLine2() == range.getVcsLine2()) {
                    return r;
                }
            }
            return null;
        });
    }

    @TestOnly
    public List<VcsRange> getRangesInner() {
        return myRanges;
    }

    @Override
    public @Nullable VcsRange getNextRange(VcsRange range) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return null;
            int index = myRanges.indexOf(range);
            if (index == myRanges.size() - 1) return null;
            return myRanges.get(index + 1);
        });
    }

    @Override
    public @Nullable VcsRange getPrevRange(VcsRange range) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return null;
            int index = myRanges.indexOf(range);
            if (index <= 0) return null;
            return myRanges.get(index - 1);
        });
    }

    @Override
    public @Nullable VcsRange getNextRange(int line) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return null;
            for (VcsRange range : myRanges) {
                if (line < range.getLine2() && !range.isSelectedByLine(line)) return range;
            }
            return null;
        });
    }

    @Override
    public @Nullable VcsRange getPrevRange(int line) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return null;
            for (int i = myRanges.size() - 1; i >= 0; i--) {
                VcsRange range = myRanges.get(i);
                if (line > range.getLine1() && !range.isSelectedByLine(line)) return range;
            }
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // LineStatusTrackerI — freeze / lock
    // -------------------------------------------------------------------------

    @Override
    public void doFrozen(Runnable task) {
        myDocumentTracker.doFrozen(task);
    }

    @Override
    public <T> T readLock(Callable<T> task) {
        return myDocumentTracker.readLock(task);
    }

    // -------------------------------------------------------------------------
    // LineStatusTrackerI — listeners
    // -------------------------------------------------------------------------

    @Override
    public void addListener(LineStatusTrackerListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeListener(LineStatusTrackerListener listener) {
        myListeners.remove(listener);
    }

    private void fireRangesChanged() {
        for (LineStatusTrackerListener listener : myListeners) {
            listener.onRangesChanged();
        }
    }

    private void fireBecomingValid() {
        for (LineStatusTrackerListener listener : myListeners) {
            listener.onBecomingValid();
        }
    }

    // -------------------------------------------------------------------------
    // Rollback
    // -------------------------------------------------------------------------

    protected void doRollbackRange(VcsRange range) {
        DiffImplUtil.applyModification(myDocument, range.getLine1(), range.getLine2(),
            myVcsDocument, range.getVcsLine1(), range.getVcsLine2());
    }

    @RequiredWriteAction
    public void rollbackChanges(VcsRange range) {
        rollbackChanges(Collections.singletonList(range));
    }

    @RequiredWriteAction
    public void rollbackChanges(BitSet lines) {
        List<VcsRange> toRollback = new ArrayList<>();
        for (VcsRange range : myRanges) {
            if (DiffImplUtil.isSelectedByLine(lines, range.getLine1(), range.getLine2())) {
                toRollback.add(range);
            }
        }
        rollbackChanges(toRollback);
    }

    @RequiredWriteAction
    private void rollbackChanges(List<VcsRange> ranges) {
        runBulkRollback(() -> {
            int shift = 0;
            for (VcsRange range : ranges) {
                if (!range.isValid()) {
                    LOG.warn("Rollback of invalid range");
                    break;
                }
                VcsRange shiftedRange = new VcsRange(range);
                shiftedRange.shift(shift);
                doRollbackRange(shiftedRange);
                shift += (range.getVcsLine2() - range.getVcsLine1())
                    - (range.getLine2() - range.getLine1());
            }
        });
    }

    @RequiredWriteAction
    private void runBulkRollback(Runnable task) {
        myApplication.assertWriteAccessAllowed();
        if (!isValid()) return;

        myDocumentTracker.withWrite(() -> {
            try {
                myDuringRollback = true;
                task.run();
            }
            catch (Error | RuntimeException e) {
                reinstallRanges();
                throw e;
            }
            finally {
                myDuringRollback = false;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Content access
    // -------------------------------------------------------------------------

    public CharSequence getCurrentContent(VcsRange range) {
        TextRange textRange = getCurrentTextRange(range);
        return myDocument.getImmutableCharSequence()
            .subSequence(textRange.getStartOffset(), textRange.getEndOffset());
    }

    @Override
    public CharSequence getVcsContent(VcsRange range) {
        TextRange textRange = getVcsTextRange(range);
        return myVcsDocument.getImmutableCharSequence()
            .subSequence(textRange.getStartOffset(), textRange.getEndOffset());
    }

    @Override
    public TextRange getCurrentTextRange(VcsRange range) {
        return myDocumentTracker.withRead(() -> {
            assert isValid();
            if (!range.isValid()) LOG.warn("Current TextRange of invalid range");
            return DiffImplUtil.getLinesRange(myDocument, range.getLine1(), range.getLine2());
        });
    }

    @Override
    public TextRange getVcsTextRange(VcsRange range) {
        return myDocumentTracker.withRead(() -> {
            assert isValid();
            if (!range.isValid()) LOG.warn("Vcs TextRange of invalid range");
            return DiffImplUtil.getLinesRange(myVcsDocument, range.getVcsLine1(), range.getVcsLine2());
        });
    }

    @Override
    public boolean isLineModified(int line) {
        return isRangeModified(line, line + 1);
    }

    @Override
    public boolean isRangeModified(int line1, int line2) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return false;
            if (line1 == line2) return false;
            assert line1 < line2;
            for (VcsRange range : myRanges) {
                if (range.getLine1() >= line2) return false;
                if (range.getLine2() > line1) return true;
            }
            return false;
        });
    }

    @Override
    public int transferLineToFromVcs(int line, boolean approximate) {
        return transferLine(line, approximate, true);
    }

    @Override
    public int transferLineToVcs(int line, boolean approximate) {
        return transferLine(line, approximate, false);
    }

    private int transferLine(int line, boolean approximate, boolean fromVcs) {
        return myDocumentTracker.withRead(() -> {
            if (!isValid()) return approximate ? line : ABSENT_LINE_NUMBER;
            int result = line;
            for (VcsRange range : myRanges) {
                int startLine1 = fromVcs ? range.getVcsLine1() : range.getLine1();
                int endLine1   = fromVcs ? range.getVcsLine2() : range.getLine2();
                int startLine2 = fromVcs ? range.getLine1()    : range.getVcsLine1();

                if (startLine1 <= line && endLine1 > line) {
                    return approximate ? startLine2 : ABSENT_LINE_NUMBER;
                }
                if (endLine1 > line) return result;

                int length1 = endLine1 - startLine1;
                int length2 = (fromVcs ? range.getLine2() : range.getVcsLine2()) - startLine2;
                result += length2 - length1;
            }
            return result;
        });
    }

    private class MyDocumentTrackerHandler implements DocumentTracker.Handler {
        @Override
        public void onRangeShifted(DocumentTracker.Block before, DocumentTracker.Block after) {
            after.setData(before.getData());
        }

        @Override
        public void afterBulkRangeChange(boolean isDirty) {
            // Called after DocumentTracker finishes a diff refresh.
            // isDirty=false → blocks are up-to-date → re-install VcsRange highlighters.
            // isDirty=true  → blocks are still dirty (e.g. called during freeze) → skip.
            if (!isDirty) {
                updateMissingInnerRanges();
                reinstallRanges();
            }
        }

        @Override
        public void onUnfreeze(Side side) {
            updateMissingInnerRanges();

            // afterBulkRangeChange(false) will follow shortly via refreshDirty().
            // Fire onBecomingValid here if the tracker is now valid (both sides unfrozen).
            if (isValid()) {
                fireBecomingValid();
            }
        }
    }
}
