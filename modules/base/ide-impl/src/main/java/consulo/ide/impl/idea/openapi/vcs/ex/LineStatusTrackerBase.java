/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.ex;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.event.ApplicationAdapter;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.component.ProcessCanceledException;
import consulo.diff.internal.DiffImplUtil;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.impl.DocumentImpl;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.vcs.CalledInAwt;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.util.UndoConstants;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.VcsRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

import static consulo.diff.internal.DiffImplUtil.getLineCount;
import static consulo.versionControlSystem.UpToDateLineNumberProvider.ABSENT_LINE_NUMBER;

@SuppressWarnings({"MethodMayBeStatic", "FieldAccessedSynchronizedAndUnsynchronized"})
public abstract class LineStatusTrackerBase implements LineStatusTrackerI {
    protected static final Logger LOG = Logger.getInstance(LineStatusTrackerBase.class);

    // all variables should be modified in EDT and under LOCK
    // read access allowed from EDT or while holding LOCK
    private final Object LOCK = new Object();

    @Nullable
    protected final Project myProject;
    @Nonnull
    protected final Document myDocument;
    @Nonnull
    protected final Document myVcsDocument;

    @Nonnull
    protected final Application myApplication;

    @Nonnull
    protected final MyDocumentListener myDocumentListener;
    @Nonnull
    protected final ApplicationAdapter myApplicationListener;

    private boolean myInitialized;
    private boolean myDuringRollback;
    private boolean myBulkUpdate;
    private boolean myAnathemaThrown;
    private boolean myReleased;

    @Nonnull
    private List<VcsRange> myRanges = Collections.emptyList();

    // operation delayed till the end of write action
    @Nonnull
    private final Set<VcsRange> myToBeDestroyedRanges = ContainerUtil.newIdentityTroveSet();
    @Nonnull
    private final Set<VcsRange> myToBeInstalledRanges = ContainerUtil.newIdentityTroveSet();

    @Nullable
    private DirtyRange myDirtyRange;

    public LineStatusTrackerBase(@Nullable Project project,
                                 @Nonnull Document document) {
        myDocument = document;
        myProject = project;

        myApplication = ApplicationManager.getApplication();

        myDocumentListener = new MyDocumentListener();
        myDocument.addDocumentListener(myDocumentListener);

        myApplicationListener = new MyApplicationListener();
        myApplication.addApplicationListener(myApplicationListener);

        myVcsDocument = new DocumentImpl("", true);
        myVcsDocument.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
    }

    //
    // Abstract
    //

    @CalledInAwt
    protected abstract void createHighlighter(@Nonnull VcsRange range);

    @CalledInAwt
    protected boolean isDetectWhitespaceChangedLines() {
        return false;
    }

    @CalledInAwt
    protected void installNotification(@Nonnull String text) {
    }

    @CalledInAwt
    protected void destroyNotification() {
    }

    @CalledInAwt
    protected void fireFileUnchanged() {
    }

    //
    // Impl
    //

    @RequiredUIAccess
    public void setBaseRevision(@Nonnull CharSequence vcsContent) {
        UIAccess.assertIsUIThread();
        if (myReleased) {
            return;
        }

        synchronized (LOCK) {
            try {
                myVcsDocument.setReadOnly(false);
                myVcsDocument.setText(vcsContent);
                myVcsDocument.setReadOnly(true);
            }
            finally {
                myInitialized = true;
            }

            reinstallRanges();
        }
    }

    @CalledInAwt
    protected void reinstallRanges() {
        if (!myInitialized || myReleased || myBulkUpdate) {
            return;
        }

        synchronized (LOCK) {
            destroyRanges();
            try {
                myRanges = RangesBuilder.createRanges(myDocument, myVcsDocument, isDetectWhitespaceChangedLines());
                for (VcsRange range : myRanges) {
                    createHighlighter(range);
                }

                if (myRanges.isEmpty()) {
                    fireFileUnchanged();
                }
            }
            catch (FilesTooBigForDiffException e) {
                installAnathema();
            }
        }
    }

    @CalledInAwt
    private void destroyRanges() {
        removeAnathema();
        for (VcsRange range : myRanges) {
            range.invalidate();
            disposeHighlighter(range);
        }
        for (VcsRange range : myToBeDestroyedRanges) {
            disposeHighlighter(range);
        }
        myRanges = Collections.emptyList();
        myToBeDestroyedRanges.clear();
        myToBeInstalledRanges.clear();
        myDirtyRange = null;
    }

    @CalledInAwt
    private void installAnathema() {
        myAnathemaThrown = true;
        installNotification("Can not highlight changed lines. File is too big and there are too many changes.");
    }

    @CalledInAwt
    private void removeAnathema() {
        if (!myAnathemaThrown) {
            return;
        }
        myAnathemaThrown = false;
        destroyNotification();
    }

    @CalledInAwt
    private void disposeHighlighter(@Nonnull VcsRange range) {
        try {
            RangeHighlighter highlighter = range.getHighlighter();
            if (highlighter != null) {
                range.setHighlighter(null);
                highlighter.dispose();
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    private boolean tryValidate() {
        if (myApplication.isDispatchThread()) {
            updateRanges();
        }
        return isValid();
    }

    @Override
    public boolean isOperational() {
        synchronized (LOCK) {
            return myInitialized && !myReleased;
        }
    }

    @Override
    public boolean isValid() {
        synchronized (LOCK) {
            return !isSuppressed() && myDirtyRange == null;
        }
    }

    private boolean isSuppressed() {
        return !myInitialized || myReleased || myAnathemaThrown || myBulkUpdate || myDuringRollback;
    }

    public void release() {
        Runnable runnable = () -> {
            if (myReleased) {
                return;
            }
            LOG.assertTrue(!myDuringRollback);

            synchronized (LOCK) {
                myReleased = true;
                myDocument.removeDocumentListener(myDocumentListener);
                myApplication.removeApplicationListener(myApplicationListener);

                destroyRanges();
            }
        };

        if (myApplication.isDispatchThread() && !myDuringRollback) {
            runnable.run();
        }
        else {
            myApplication.invokeLater(runnable);
        }
    }

    @Nullable
    public Project getProject() {
        return myProject;
    }

    @Nonnull
    public Document getDocument() {
        return myDocument;
    }

    @Override
    @Nonnull
    public Document getVcsDocument() {
        return myVcsDocument;
    }

    /**
     * Ranges can be modified without taking the write lock, so calling this method twice not from EDT can produce different results.
     */
    @Override
    @Nullable
    public List<VcsRange> getRanges() {
        synchronized (LOCK) {
            if (!tryValidate()) {
                return null;
            }
            myApplication.assertReadAccessAllowed();

            List<VcsRange> result = new ArrayList<>(myRanges.size());
            for (VcsRange range : myRanges) {
                result.add(new VcsRange(range));
            }
            return result;
        }
    }

    @Nonnull
    @TestOnly
    public List<VcsRange> getRangesInner() {
        return myRanges;
    }

    @CalledInAwt
    public void startBulkUpdate() {
        if (myReleased) {
            return;
        }
        synchronized (LOCK) {
            myBulkUpdate = true;
            destroyRanges();
        }
    }

    @CalledInAwt
    public void finishBulkUpdate() {
        if (myReleased) {
            return;
        }
        synchronized (LOCK) {
            myBulkUpdate = false;
            reinstallRanges();
        }
    }

    @CalledInAwt
    private void updateRanges() {
        if (isSuppressed()) {
            return;
        }
        if (myDirtyRange != null) {
            synchronized (LOCK) {
                try {
                    doUpdateRanges(myDirtyRange.line1, myDirtyRange.line2, myDirtyRange.lineShift, myDirtyRange.beforeTotalLines);
                    myDirtyRange = null;
                }
                catch (Exception e) {
                    LOG.error(e);
                    reinstallRanges();
                }
            }
        }
    }

    @CalledInAwt
    private void updateRangeHighlighters() {
        if (myToBeInstalledRanges.isEmpty() && myToBeDestroyedRanges.isEmpty()) {
            return;
        }

        synchronized (LOCK) {
            myToBeInstalledRanges.removeAll(myToBeDestroyedRanges);

            for (VcsRange range : myToBeDestroyedRanges) {
                disposeHighlighter(range);
            }
            for (VcsRange range : myToBeInstalledRanges) {
                createHighlighter(range);
            }
            myToBeDestroyedRanges.clear();
            myToBeInstalledRanges.clear();
        }
    }

    private class MyApplicationListener extends ApplicationAdapter {
        @Override
        public void afterWriteActionFinished(@Nonnull Object action) {
            updateRanges();
            updateRangeHighlighters();
        }
    }

    private class MyDocumentListener extends DocumentAdapter {
        /*
         *   beforeWriteLock   beforeChange     Current
         *              |            |             |
         *              |            | line1       |
         * updatedLine1 +============+-------------+ newLine1
         *              |            |             |
         *      r.line1 +------------+ oldLine1    |
         *              |            |             |
         *              |     old    |             |
         *              |    dirty   |             |
         *              |            | oldLine2    |
         *      r.line2 +------------+         ----+ newLine2
         *              |            |        /    |
         * updatedLine2 +============+--------     |
         *                            line2
         */

        private int myLine1;
        private int myLine2;
        private int myBeforeTotalLines;

        @Override
        public void beforeDocumentChange(DocumentEvent e) {
            if (isSuppressed()) {
                return;
            }
            assert myDocument == e.getDocument();

            myLine1 = myDocument.getLineNumber(e.getOffset());
            if (e.getOldLength() == 0) {
                myLine2 = myLine1 + 1;
            }
            else {
                myLine2 = myDocument.getLineNumber(e.getOffset() + e.getOldLength()) + 1;
            }

            myBeforeTotalLines = getLineCount(myDocument);
        }

        @Override
        @RequiredUIAccess
        public void documentChanged(DocumentEvent e) {
            UIAccess.assertIsUIThread();

            if (isSuppressed()) {
                return;
            }
            assert myDocument == e.getDocument();

            synchronized (LOCK) {
                int newLine1 = myLine1;
                int newLine2;
                if (e.getNewLength() == 0) {
                    newLine2 = newLine1 + 1;
                }
                else {
                    newLine2 = myDocument.getLineNumber(e.getOffset() + e.getNewLength()) + 1;
                }

                int linesShift = (newLine2 - newLine1) - (myLine2 - myLine1);

                int[] fixed = fixRanges(e, myLine1, myLine2);
                int line1 = fixed[0];
                int line2 = fixed[1];

                if (myDirtyRange == null) {
                    myDirtyRange = new DirtyRange(line1, line2, linesShift, myBeforeTotalLines);
                }
                else {
                    int oldLine1 = myDirtyRange.line1;
                    int oldLine2 = myDirtyRange.line2 + myDirtyRange.lineShift;

                    int updatedLine1 = myDirtyRange.line1 - Math.max(oldLine1 - line1, 0);
                    int updatedLine2 = myDirtyRange.line2 + Math.max(line2 - oldLine2, 0);

                    myDirtyRange = new DirtyRange(updatedLine1, updatedLine2, linesShift + myDirtyRange.lineShift, myDirtyRange.beforeTotalLines);
                }
            }
        }
    }

    @Nonnull
    private int[] fixRanges(@Nonnull DocumentEvent e, int line1, int line2) {
        CharSequence document = myDocument.getCharsSequence();
        int offset = e.getOffset();

        if (e.getOldLength() == 0 && e.getNewLength() != 0) {
            if (StringUtil.endsWithChar(e.getNewFragment(), '\n') && isNewline(offset - 1, document)) {
                return new int[]{line1, line2 - 1};
            }
            if (StringUtil.startsWithChar(e.getNewFragment(), '\n') && isNewline(offset + e.getNewLength(), document)) {
                return new int[]{line1 + 1, line2};
            }
        }
        if (e.getOldLength() != 0 && e.getNewLength() == 0) {
            if (StringUtil.endsWithChar(e.getOldFragment(), '\n') && isNewline(offset - 1, document)) {
                return new int[]{line1, line2 - 1};
            }
            if (StringUtil.startsWithChar(e.getOldFragment(), '\n') && isNewline(offset + e.getNewLength(), document)) {
                return new int[]{line1 + 1, line2};
            }
        }

        return new int[]{line1, line2};
    }

    private static boolean isNewline(int offset, @Nonnull CharSequence sequence) {
        if (offset < 0) {
            return false;
        }
        if (offset >= sequence.length()) {
            return false;
        }
        return sequence.charAt(offset) == '\n';
    }

    private void doUpdateRanges(int beforeChangedLine1,
                                int beforeChangedLine2,
                                int linesShift,
                                int beforeTotalLines) {
        LOG.assertTrue(!myReleased);

        List<VcsRange> rangesBeforeChange = new ArrayList<>();
        List<VcsRange> rangesAfterChange = new ArrayList<>();
        List<VcsRange> changedRanges = new ArrayList<>();

        sortRanges(beforeChangedLine1, beforeChangedLine2, linesShift, rangesBeforeChange, changedRanges, rangesAfterChange);

        VcsRange firstChangedRange = ContainerUtil.getFirstItem(changedRanges);
        VcsRange lastChangedRange = ContainerUtil.getLastItem(changedRanges);

        if (firstChangedRange != null && firstChangedRange.getLine1() < beforeChangedLine1) {
            beforeChangedLine1 = firstChangedRange.getLine1();
        }
        if (lastChangedRange != null && lastChangedRange.getLine2() > beforeChangedLine2) {
            beforeChangedLine2 = lastChangedRange.getLine2();
        }

        doUpdateRanges(beforeChangedLine1, beforeChangedLine2, linesShift, beforeTotalLines,
            rangesBeforeChange, changedRanges, rangesAfterChange);
    }

    private void doUpdateRanges(int beforeChangedLine1,
                                int beforeChangedLine2,
                                int linesShift, // before -> after
                                int beforeTotalLines,
                                @Nonnull List<VcsRange> rangesBefore,
                                @Nonnull List<VcsRange> changedRanges,
                                @Nonnull List<VcsRange> rangesAfter) {
        try {
            int vcsTotalLines = getLineCount(myVcsDocument);

            VcsRange lastRangeBefore = ContainerUtil.getLastItem(rangesBefore);
            VcsRange firstRangeAfter = ContainerUtil.getFirstItem(rangesAfter);

            //noinspection UnnecessaryLocalVariable
            int afterChangedLine1 = beforeChangedLine1;
            int afterChangedLine2 = beforeChangedLine2 + linesShift;

            int vcsLine1 = getVcsLine1(lastRangeBefore, beforeChangedLine1);
            int vcsLine2 = getVcsLine2(firstRangeAfter, beforeChangedLine2, beforeTotalLines, vcsTotalLines);

            List<VcsRange> newChangedRanges = getNewChangedRanges(afterChangedLine1, afterChangedLine2, vcsLine1, vcsLine2);

            shiftRanges(rangesAfter, linesShift);

            if (!changedRanges.equals(newChangedRanges)) {
                myRanges = new ArrayList<>(rangesBefore.size() + newChangedRanges.size() + rangesAfter.size());

                myRanges.addAll(rangesBefore);
                myRanges.addAll(newChangedRanges);
                myRanges.addAll(rangesAfter);

                for (VcsRange range : changedRanges) {
                    range.invalidate();
                }
                myToBeDestroyedRanges.addAll(changedRanges);
                myToBeInstalledRanges.addAll(newChangedRanges);

                if (myRanges.isEmpty()) {
                    fireFileUnchanged();
                }
            }
        }
        catch (ProcessCanceledException ignore) {
        }
        catch (FilesTooBigForDiffException e1) {
            destroyRanges();
            installAnathema();
        }
    }

    private static int getVcsLine1(@Nullable VcsRange range, int line) {
        return range == null ? line : line + range.getVcsLine2() - range.getLine2();
    }

    private static int getVcsLine2(@Nullable VcsRange range, int line, int totalLinesBefore, int totalLinesAfter) {
        return range == null ? totalLinesAfter - totalLinesBefore + line : line + range.getVcsLine1() - range.getLine1();
    }

    private List<VcsRange> getNewChangedRanges(int changedLine1, int changedLine2, int vcsLine1, int vcsLine2)
        throws FilesTooBigForDiffException {

        if (changedLine1 == changedLine2 && vcsLine1 == vcsLine2) {
            return Collections.emptyList();
        }
        if (changedLine1 == changedLine2) {
            return Collections.singletonList(new VcsRange(changedLine1, changedLine2, vcsLine1, vcsLine2));
        }
        if (vcsLine1 == vcsLine2) {
            return Collections.singletonList(new VcsRange(changedLine1, changedLine2, vcsLine1, vcsLine2));
        }

        List<String> lines = DiffImplUtil.getLines(myDocument, changedLine1, changedLine2);
        List<String> vcsLines = DiffImplUtil.getLines(myVcsDocument, vcsLine1, vcsLine2);

        return RangesBuilder.createRanges(lines, vcsLines, changedLine1, vcsLine1, isDetectWhitespaceChangedLines());
    }

    private static void shiftRanges(@Nonnull List<VcsRange> rangesAfterChange, int shift) {
        for (VcsRange range : rangesAfterChange) {
            range.shift(shift);
        }
    }

    private void sortRanges(int beforeChangedLine1,
                            int beforeChangedLine2,
                            int linesShift,
                            @Nonnull List<VcsRange> rangesBeforeChange,
                            @Nonnull List<VcsRange> changedRanges,
                            @Nonnull List<VcsRange> rangesAfterChange) {
        int lastBefore = -1;
        int firstAfter = myRanges.size();
        for (int i = 0; i < myRanges.size(); i++) {
            VcsRange range = myRanges.get(i);

            if (range.getLine2() < beforeChangedLine1) {
                lastBefore = i;
            }
            else if (range.getLine1() > beforeChangedLine2) {
                firstAfter = i;
                break;
            }
        }

        // Expand on ranges, that are separated from changed lines only by whitespaces

        while (lastBefore != -1) {
            int firstChangedLine = beforeChangedLine1;
            if (lastBefore + 1 < myRanges.size()) {
                VcsRange firstChanged = myRanges.get(lastBefore + 1);
                firstChangedLine = Math.min(firstChangedLine, firstChanged.getLine1());
            }

            if (!isLineRangeEmpty(myDocument, myRanges.get(lastBefore).getLine2(), firstChangedLine)) {
                break;
            }

            lastBefore--;
        }

        while (firstAfter != myRanges.size()) {
            int firstUnchangedLineAfter = beforeChangedLine2 + linesShift;
            if (firstAfter > 0) {
                VcsRange lastChanged = myRanges.get(firstAfter - 1);
                firstUnchangedLineAfter = Math.max(firstUnchangedLineAfter, lastChanged.getLine2() + linesShift);
            }

            if (!isLineRangeEmpty(myDocument, firstUnchangedLineAfter, myRanges.get(firstAfter).getLine1() + linesShift)) {
                break;
            }

            firstAfter++;
        }

        for (int i = 0; i < myRanges.size(); i++) {
            VcsRange range = myRanges.get(i);
            if (i <= lastBefore) {
                rangesBeforeChange.add(range);
            }
            else if (i >= firstAfter) {
                rangesAfterChange.add(range);
            }
            else {
                changedRanges.add(range);
            }
        }
    }

    private static boolean isLineRangeEmpty(@Nonnull Document document, int line1, int line2) {
        int lineCount = getLineCount(document);
        int startOffset = line1 == lineCount ? document.getTextLength() : document.getLineStartOffset(line1);
        int endOffset = line2 == lineCount ? document.getTextLength() : document.getLineStartOffset(line2);

        CharSequence interval = document.getImmutableCharSequence().subSequence(startOffset, endOffset);
        return StringUtil.isEmptyOrSpaces(interval);
    }

    @Override
    @Nullable
    public VcsRange getNextRange(VcsRange range) {
        synchronized (LOCK) {
            if (!tryValidate()) {
                return null;
            }
            int index = myRanges.indexOf(range);
            if (index == myRanges.size() - 1) {
                return null;
            }
            return myRanges.get(index + 1);
        }
    }

    @Override
    @Nullable
    public VcsRange getPrevRange(VcsRange range) {
        synchronized (LOCK) {
            if (!tryValidate()) {
                return null;
            }
            int index = myRanges.indexOf(range);
            if (index <= 0) {
                return null;
            }
            return myRanges.get(index - 1);
        }
    }

    @Nullable
    public VcsRange getNextRange(int line) {
        synchronized (LOCK) {
            if (!tryValidate()) {
                return null;
            }
            for (VcsRange range : myRanges) {
                if (line < range.getLine2() && !range.isSelectedByLine(line)) {
                    return range;
                }
            }
            return null;
        }
    }

    @Nullable
    public VcsRange getPrevRange(int line) {
        synchronized (LOCK) {
            if (!tryValidate()) {
                return null;
            }
            for (int i = myRanges.size() - 1; i >= 0; i--) {
                VcsRange range = myRanges.get(i);
                if (line > range.getLine1() && !range.isSelectedByLine(line)) {
                    return range;
                }
            }
            return null;
        }
    }

    @Nullable
    public VcsRange getRangeForLine(int line) {
        synchronized (LOCK) {
            if (!tryValidate()) {
                return null;
            }
            for (VcsRange range : myRanges) {
                if (range.isSelectedByLine(line)) {
                    return range;
                }
            }
            return null;
        }
    }

    protected void doRollbackRange(@Nonnull VcsRange range) {
        DiffImplUtil.applyModification(myDocument, range.getLine1(), range.getLine2(), myVcsDocument, range.getVcsLine1(), range.getVcsLine2());
    }

    @RequiredWriteAction
    public void rollbackChanges(@Nonnull VcsRange range) {
        rollbackChanges(Collections.singletonList(range));
    }

    @RequiredWriteAction
    public void rollbackChanges(@Nonnull BitSet lines) {
        List<VcsRange> toRollback = new ArrayList<>();
        for (VcsRange range : myRanges) {
            boolean check = DiffImplUtil.isSelectedByLine(lines, range.getLine1(), range.getLine2());
            if (check) {
                toRollback.add(range);
            }
        }

        rollbackChanges(toRollback);
    }

    /**
     * @param ranges - sorted list of ranges to rollback
     */
    @RequiredWriteAction
    private void rollbackChanges(@Nonnull List<VcsRange> ranges) {
        runBulkRollback(() -> {
            VcsRange first = null;
            VcsRange last = null;

            int shift = 0;
            for (VcsRange range : ranges) {
                if (!range.isValid()) {
                    LOG.warn("Rollback of invalid range");
                    break;
                }

                if (first == null) {
                    first = range;
                }
                last = range;

                VcsRange shiftedRange = new VcsRange(range);
                shiftedRange.shift(shift);

                doRollbackRange(shiftedRange);

                shift += (range.getVcsLine2() - range.getVcsLine1()) - (range.getLine2() - range.getLine1());
            }

            if (first != null) {
                int beforeChangedLine1 = first.getLine1();
                int beforeChangedLine2 = last.getLine2();

                int beforeTotalLines = getLineCount(myDocument) - shift;

                doUpdateRanges(beforeChangedLine1, beforeChangedLine2, shift, beforeTotalLines);
                updateRangeHighlighters();
            }
        });
    }

    @RequiredWriteAction
    private void runBulkRollback(@Nonnull Runnable task) {
        myApplication.assertWriteAccessAllowed();
        if (!tryValidate()) {
            return;
        }

        synchronized (LOCK) {
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
        }
    }

    @Nonnull
    public CharSequence getCurrentContent(@Nonnull VcsRange range) {
        TextRange textRange = getCurrentTextRange(range);
        int startOffset = textRange.getStartOffset();
        int endOffset = textRange.getEndOffset();
        return myDocument.getImmutableCharSequence().subSequence(startOffset, endOffset);
    }

    @Nonnull
    public CharSequence getVcsContent(@Nonnull VcsRange range) {
        TextRange textRange = getVcsTextRange(range);
        int startOffset = textRange.getStartOffset();
        int endOffset = textRange.getEndOffset();
        return myVcsDocument.getImmutableCharSequence().subSequence(startOffset, endOffset);
    }

    @Nonnull
    public TextRange getCurrentTextRange(@Nonnull VcsRange range) {
        synchronized (LOCK) {
            assert isValid();
            if (!range.isValid()) {
                LOG.warn("Current TextRange of invalid range");
            }
            return DiffImplUtil.getLinesRange(myDocument, range.getLine1(), range.getLine2());
        }
    }

    @Nonnull
    public TextRange getVcsTextRange(@Nonnull VcsRange range) {
        synchronized (LOCK) {
            assert isValid();
            if (!range.isValid()) {
                LOG.warn("Vcs TextRange of invalid range");
            }
            return DiffImplUtil.getLinesRange(myVcsDocument, range.getVcsLine1(), range.getVcsLine2());
        }
    }

    @Override
    public boolean isLineModified(int line) {
        return isRangeModified(line, line + 1);
    }

    @Override
    public boolean isRangeModified(int line1, int line2) {
        synchronized (LOCK) {
            if (!tryValidate()) {
                return false;
            }
            if (line1 == line2) {
                return false;
            }
            assert line1 < line2;

            for (VcsRange range : myRanges) {
                if (range.getLine1() >= line2) {
                    return false;
                }
                if (range.getLine2() > line1) {
                    return true;
                }
            }
            return false;
        }
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
        synchronized (LOCK) {
            if (!tryValidate()) {
                return approximate ? line : ABSENT_LINE_NUMBER;
            }

            int result = line;

            for (VcsRange range : myRanges) {
                int startLine1 = fromVcs ? range.getVcsLine1() : range.getLine1();
                int endLine1 = fromVcs ? range.getVcsLine2() : range.getLine2();
                int startLine2 = fromVcs ? range.getLine1() : range.getVcsLine1();
                int endLine2 = fromVcs ? range.getLine2() : range.getVcsLine2();

                if (startLine1 <= line && endLine1 > line) {
                    return approximate ? startLine2 : ABSENT_LINE_NUMBER;
                }

                if (endLine1 > line) {
                    return result;
                }

                int length1 = endLine1 - startLine1;
                int length2 = endLine2 - startLine2;
                result += length2 - length1;
            }
            return result;
        }
    }

    private static class DirtyRange {
        public final int line1;
        public final int line2;
        public final int lineShift;
        public final int beforeTotalLines;

        public DirtyRange(int line1, int line2, int lineShift, int beforeTotalLines) {
            this.line1 = line1;
            this.line2 = line2;
            this.lineShift = lineShift;
            this.beforeTotalLines = beforeTotalLines;
        }
    }
}
