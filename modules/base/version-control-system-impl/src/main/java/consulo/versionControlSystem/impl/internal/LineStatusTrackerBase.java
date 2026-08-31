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
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base implementation of {@link LineStatusTrackerI}.
 *
 * <p>Uses {@link DocumentTracker} to manage the two-document diff (VCS vs. current)
 * and receive change notifications via {@link DocumentTracker.Handler}.
 *
 * <ul>
 *   <li>{@link DocumentTracker} owns document listening, dirty tracking, freeze/unfreeze,
 *       and diff computation.</li>
 *   <li>{@link VcsRange}s are derived from the tracker blocks on demand by
 *       {@link LineStatusTrackerBlockOperations} — there is no cached range list.</li>
 *   <li>This class fires {@link LineStatusTrackerListener} events. Highlighters are owned by the
 *       renderer, which reacts to those events on EDT.</li>
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

    protected final LineStatusTrackerBlockOperations<DocumentTracker.Block> blockOperations;

    private boolean myInitialized;
    private boolean myDuringRollback;
    private boolean myReleased;

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

        blockOperations = new MyBlockOperations(myDocumentTracker);
    }

    // -------------------------------------------------------------------------
    // Abstract API
    // -------------------------------------------------------------------------

    @RequiredUIAccess
    protected boolean isDetectWhitespaceChangedLines() {
        return false;
    }

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
            updateHighlighters();
        }

        if (isValid()) {
            fireBecomingValid();
        }
    }

    @RequiredUIAccess
    public void dropBaseRevision() {
        UIAccess.assertIsUIThread();
        if (myReleased || !myInitialized) return;

        myDocumentTracker.withWrite(() -> myInitialized = false);
        updateHighlighters();

        myDocumentTracker.doFrozen(() -> {
            myVcsDocument.setReadOnly(false);
            myVcsDocument.setText(myDocument.getImmutableCharSequence());
            myVcsDocument.setReadOnly(true);
            myDocumentTracker.setFrozenState(Collections.emptyList());
        });
    }

    public void release() {
        Runnable runnable = () -> {
            if (myReleased) return;
            LOG.assertTrue(!myDuringRollback);

            myDocumentTracker.withWrite(() -> {
                myReleased = true;
                myDocumentTracker.dispose();
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

    protected void updateHighlighters() {
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

    private class MyBlockOperations extends LineStatusTrackerBlockOperations<DocumentTracker.Block> {
        MyBlockOperations(DocumentTracker lock) {
            super(lock);
        }

        @Override
        protected @Nullable List<DocumentTracker.Block> getBlocks() {
            return isValid() ? myDocumentTracker.getBlocks() : null;
        }

        @Override
        protected VcsRange toRange(DocumentTracker.Block block) {
            return toVcsRange(block);
        }
    }

    // -------------------------------------------------------------------------
    // Bulk update (delegate to DocumentTracker freeze)
    // -------------------------------------------------------------------------

    @RequiredUIAccess
    public void startBulkUpdate() {
        if (myReleased) return;
        myDocumentTracker.freeze(Side.RIGHT);
    }

    @RequiredUIAccess
    public void finishBulkUpdate() {
        if (myReleased) return;
        myDocumentTracker.unfreeze(Side.RIGHT);
        // unfreeze triggers refreshDirty -> Handler.afterBulkRangeChange -> updateHighlighters
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
     * Returns the current ranges, or {@code null} if not valid.
     * Calling this twice without holding a read-lock can return different results.
     */
    @Override
    public @Nullable List<VcsRange> getRanges() {
        return blockOperations.getRanges();
    }

    @Override
    public @Nullable List<VcsRange> getRangesForLines(BitSet lines) {
        return blockOperations.getRangesForLines(lines);
    }

    @Override
    public @Nullable VcsRange getRangeForLine(int line) {
        return blockOperations.getRangeForLine(line);
    }

    @Override
    public @Nullable VcsRange findRange(VcsRange range) {
        return blockOperations.findRange(range);
    }

    @Override
    public @Nullable VcsRange getNextRange(VcsRange range) {
        return blockOperations.getNextRange(range);
    }

    @Override
    public @Nullable VcsRange getPrevRange(VcsRange range) {
        return blockOperations.getPrevRange(range);
    }

    @Override
    public @Nullable VcsRange getNextRange(int line) {
        return blockOperations.getNextRange(line);
    }

    @Override
    public @Nullable VcsRange getPrevRange(int line) {
        return blockOperations.getPrevRange(line);
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
        List<VcsRange> toRollback = blockOperations.getRangesForLines(lines);
        if (toRollback == null) return;
        rollbackChanges(toRollback);
    }

    @RequiredWriteAction
    private void rollbackChanges(List<VcsRange> ranges) {
        runBulkRollback(() -> {
            int shift = 0;
            for (VcsRange range : ranges) {
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
                updateHighlighters();
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
            return DiffImplUtil.getLinesRange(myDocument, range.getLine1(), range.getLine2());
        });
    }

    @Override
    public TextRange getVcsTextRange(VcsRange range) {
        return myDocumentTracker.withRead(() -> {
            assert isValid();
            return DiffImplUtil.getLinesRange(myVcsDocument, range.getVcsLine1(), range.getVcsLine2());
        });
    }

    @Override
    public boolean isLineModified(int line) {
        return blockOperations.isLineModified(line);
    }

    @Override
    public boolean isRangeModified(int line1, int line2) {
        return blockOperations.isRangeModified(line1, line2);
    }

    @Override
    public int transferLineToFromVcs(int line, boolean approximate) {
        return blockOperations.transferLineFromVcs(line, approximate);
    }

    @Override
    public int transferLineToVcs(int line, boolean approximate) {
        return blockOperations.transferLineToVcs(line, approximate);
    }

    private class MyDocumentTrackerHandler implements DocumentTracker.Handler {
        @Override
        public void onRangeShifted(DocumentTracker.Block before, DocumentTracker.Block after) {
            after.setData(before.getData());
        }

        @Override
        public void afterBulkRangeChange(boolean isDirty) {
            if (!isDirty) {
                updateMissingInnerRanges();
            }

            if (myDocumentTracker.getBlocks().isEmpty() && isOperational()) {
                fireFileUnchanged();
            }

            updateHighlighters();
        }

        @Override
        public void onUnfreeze(Side side) {
            updateMissingInnerRanges();

            updateHighlighters();

            if (isValid()) {
                fireBecomingValid();
            }
        }
    }
}
