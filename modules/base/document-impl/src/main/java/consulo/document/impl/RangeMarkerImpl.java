// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.document.impl;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.impl.event.DocumentEventImpl;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.RangeMarkerEx;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.document.util.UnfairTextRange;
import consulo.logging.Logger;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.BinaryFileDecompiler;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

public class RangeMarkerImpl extends UserDataHolderBase implements RangeMarkerEx {
    protected static final Logger LOG = Logger.getInstance(RangeMarkerImpl.class);

    @Nonnull
    private final Object myDocumentOrFile; // either VirtualFile (if any) or DocumentEx if no file associated
    protected RangeMarkerTree.RMNode<RangeMarkerEx> myNode;

    private final long myId;
    private static final StripedIDGenerator counter = new StripedIDGenerator();

    public RangeMarkerImpl(@Nonnull DocumentEx document, int start, int end, boolean register, boolean forceDocumentStrongReference) {
        this(forceDocumentStrongReference ? document : ObjectUtil.notNull(FileDocumentManager.getInstance().getFile(document), document),
            document,
            document.getTextLength(), start, end, register, false, false);
    }

    // The constructor which creates a marker without a document and saves it in the virtual file directly. Can be cheaper than loading the entire document.
    public RangeMarkerImpl(@Nonnull VirtualFile virtualFile, int start, int end, int estimatedDocumentLength, boolean register) {
        // unfortunately, we don't know the exact document size until we load it
        this(virtualFile, null, estimatedDocumentLength, start, end, register, false, false);
    }

    public static int estimateDocumentLength(@Nonnull VirtualFile virtualFile) {
        Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
        return document == null ? Integer.MAX_VALUE : document.getTextLength();
    }

    private RangeMarkerImpl(@Nonnull Object documentOrFile,
                            @Nullable DocumentEx document,
                            int documentTextLength,
                            int start,
                            int end,
                            boolean register,
                            boolean greedyToLeft,
                            boolean greedyToRight) {
        if (start < 0) {
            throw new IllegalArgumentException("Wrong start: " + start + "; end=" + end);
        }
        if (end > documentTextLength) {
            throw new IllegalArgumentException("Wrong end: " + end + "; document length=" + documentTextLength + "; start=" + start);
        }
        if (start > end) {
            throw new IllegalArgumentException("start > end: start=" + start + "; end=" + end);
        }

        myDocumentOrFile = documentOrFile;
        myId = counter.next();

        if (register) {
            DocumentEx d = document == null ? getDocument() : document;
            registerInTree(d, start, end, greedyToLeft, greedyToRight, 0);
        }
    }

    protected void registerInTree(
        @Nonnull DocumentEx document,
        int start,
        int end,
        boolean greedyToLeft,
        boolean greedyToRight,
        int layer
    ) {
        document.registerRangeMarker(this, start, end, greedyToLeft, greedyToRight, layer);
    }

    protected boolean unregisterInTree() {
        if (!isValid()) {
            return false;
        }
        IntervalTreeImpl<?> tree = myNode.getTree();
        tree.checkMax(true);
        boolean b = getDocument().removeRangeMarker(this);
        tree.checkMax(true);
        return b;
    }

    @Override
    public long getId() {
        return myId;
    }

    @Override
    public void dispose() {
        unregisterInTree();
    }

    @Override
    public int getStartOffset() {
        RangeMarkerTree.RMNode<?> node = myNode;
        return node == null ? -1 : node.intervalStart() + node.computeDeltaUpToRoot();
    }

    @Override
    public int getEndOffset() {
        RangeMarkerTree.RMNode<?> node = myNode;
        return node == null ? -1 : node.intervalEnd() + node.computeDeltaUpToRoot();
    }

    public void invalidate(@Nonnull final Object reason) {
        setValid(false);
        RangeMarkerTree.RMNode<?> node = myNode;

        if (node != null) {
            node.processAliveKeys(markerEx -> {
                myNode.getTree().beforeRemove(markerEx, reason);
                return true;
            });
        }
    }

    @Override
    @Nonnull
    public final DocumentEx getDocument() {
        Object file = myDocumentOrFile;
        DocumentEx document =
            file instanceof VirtualFile ? (DocumentEx) FileDocumentManager.getInstance().getDocument((VirtualFile) file) : (DocumentEx) file;
        if (document == null) {
            LOG.error("document is null; isValid=" + isValid() + "; file=" + file);
        }
        return document;
    }

    // fake method to simplify setGreedyToLeft/right methods. overridden in RangeHighlighter
    public int getLayer() {
        return 0;
    }

    @Override
    public void setGreedyToLeft(final boolean greedy) {
        if (!isValid() || greedy == isGreedyToLeft()) {
            return;
        }

        myNode.getTree().changeData(this, getStartOffset(), getEndOffset(), greedy, isGreedyToRight(), isStickingToRight(), getLayer());
    }

    @Override
    public void setGreedyToRight(final boolean greedy) {
        if (!isValid() || greedy == isGreedyToRight()) {
            return;
        }
        myNode.getTree().changeData(this, getStartOffset(), getEndOffset(), isGreedyToLeft(), greedy, isStickingToRight(), getLayer());
    }

    public void setStickingToRight(boolean value) {
        if (!isValid() || value == isStickingToRight()) {
            return;
        }
        myNode.getTree().changeData(this, getStartOffset(), getEndOffset(), isGreedyToLeft(), isGreedyToRight(), value, getLayer());
    }

    @Override
    public boolean isGreedyToLeft() {
        RangeMarkerTree.RMNode<?> node = myNode;
        return node != null && node.isGreedyToLeft();
    }

    @Override
    public boolean isGreedyToRight() {
        RangeMarkerTree.RMNode<?> node = myNode;
        return node != null && node.isGreedyToRight();
    }

    boolean isStickingToRight() {
        RangeMarkerTree.RMNode<?> node = myNode;
        return node != null && node.isStickingToRight();
    }

    @Override
    public final void documentChanged(@Nonnull DocumentEvent e) {
        int oldStart = intervalStart();
        int oldEnd = intervalEnd();
        int docLength = getDocument().getTextLength();
        if (!isValid()) {
            LOG.error("Invalid range marker " +
                (isGreedyToLeft() ? "[" : "(") +
                oldStart +
                ", " +
                oldEnd +
                (isGreedyToRight() ? "]" : ")") +
                ". Event = " +
                e +
                ". Doc length=" +
                docLength +
                "; " +
                getClass());
            return;
        }
        if (intervalStart() > intervalEnd() || intervalStart() < 0 || intervalEnd() > docLength - e.getNewLength() + e.getOldLength()) {
            LOG.error("RangeMarker" +
                (isGreedyToLeft() ? "[" : "(") +
                oldStart +
                ", " +
                oldEnd +
                (isGreedyToRight() ? "]" : ")") +
                " is invalid before update. Event = " +
                e +
                ". Doc length=" +
                docLength +
                "; " +
                getClass());
            invalidate(e);
            return;
        }
        changedUpdateImpl(e);
        if (isValid() && (intervalStart() > intervalEnd() || intervalStart() < 0 || intervalEnd() > docLength)) {
            LOG.error("Update failed. Event = " +
                e +
                ". " +
                "old doc length=" +
                docLength +
                "; real doc length = " +
                getDocument().getTextLength() +
                "; " +
                getClass() +
                "." +
                " After update: '" +
                this +
                "'");
            invalidate(e);
        }
    }

    protected void changedUpdateImpl(@Nonnull DocumentEvent e) {
        if (!isValid()) {
            return;
        }

        TextRange newRange = applyChange(e, intervalStart(), intervalEnd(), isGreedyToLeft(), isGreedyToRight(), isStickingToRight());
        if (newRange == null) {
            invalidate(e);
            return;
        }

        setIntervalStart(newRange.getStartOffset());
        setIntervalEnd(newRange.getEndOffset());
    }

    protected void onReTarget(int startOffset, int endOffset, int destOffset) {
    }

    @Nullable
    static TextRange applyChange(@Nonnull DocumentEvent e, int intervalStart, int intervalEnd, boolean isGreedyToLeft, boolean isGreedyToRight, boolean isStickingToRight) {
        if (intervalStart == intervalEnd) {
            return processIfOnePoint(e, intervalStart, isGreedyToRight, isStickingToRight);
        }

        final int offset = e.getOffset();
        final int oldLength = e.getOldLength();
        final int newLength = e.getNewLength();

        // changes after the end.
        if (intervalEnd < offset) {
            return new UnfairTextRange(intervalStart, intervalEnd);
        }
        if (!isGreedyToRight && intervalEnd == offset) {
            // handle replaceString that was minimized and resulted in insertString at the range end
            if (e instanceof DocumentEventImpl && oldLength == 0 && ((DocumentEventImpl) e).getInitialStartOffset() < offset) {
                return new UnfairTextRange(intervalStart, intervalEnd + newLength);
            }
            return new UnfairTextRange(intervalStart, intervalEnd);
        }

        // changes before start
        if (intervalStart > offset + oldLength) {
            return new UnfairTextRange(intervalStart + newLength - oldLength, intervalEnd + newLength - oldLength);
        }
        if (!isGreedyToLeft && intervalStart == offset + oldLength) {
            // handle replaceString that was minimized and resulted in insertString at the range start
            if (e instanceof DocumentEventImpl && oldLength == 0 && ((DocumentEventImpl) e).getInitialStartOffset() + ((DocumentEventImpl) e).getInitialOldLength() > offset) {
                return new UnfairTextRange(intervalStart, intervalEnd + newLength);
            }
            return new UnfairTextRange(intervalStart + newLength - oldLength, intervalEnd + newLength - oldLength);
        }

        // Changes inside marker's area. Expand/collapse.
        if (intervalStart <= offset && intervalEnd >= offset + oldLength) {
            return new ProperTextRange(intervalStart, intervalEnd + newLength - oldLength);
        }

        // At this point we either have (myStart xor myEnd inside changed area) or whole area changed.

        // Replacing prefix or suffix...
        if (intervalStart >= offset && intervalStart <= offset + oldLength && intervalEnd > offset + oldLength) {
            return new ProperTextRange(offset + newLength, intervalEnd + newLength - oldLength);
        }

        if (intervalEnd >= offset && intervalEnd <= offset + oldLength && intervalStart < offset) {
            return new UnfairTextRange(intervalStart, offset);
        }

        return null;
    }

    @Nullable
    private static TextRange processIfOnePoint(@Nonnull DocumentEvent e, int intervalStart, boolean greedyRight, boolean stickyRight) {
        int offset = e.getOffset();
        int oldLength = e.getOldLength();
        int oldEnd = offset + oldLength;
        if (offset < intervalStart && intervalStart < oldEnd) {
            return null;
        }

        if (offset == intervalStart && oldLength == 0) {
            if (greedyRight) {
                return new UnfairTextRange(intervalStart, intervalStart + e.getNewLength());
            }
            else if (stickyRight) {
                return new UnfairTextRange(intervalStart + e.getNewLength(), intervalStart + e.getNewLength());
            }
        }

        if (intervalStart > oldEnd || intervalStart == oldEnd && oldLength > 0) {
            return new UnfairTextRange(intervalStart + e.getNewLength() - oldLength, intervalStart + e.getNewLength() - oldLength);
        }

        return new UnfairTextRange(intervalStart, intervalStart);
    }

    @Override
    @NonNls
    public String toString() {
        return "RangeMarker" + (isGreedyToLeft() ? "[" : "(") + (isValid() ? "" : "invalid:") + getStartOffset() + "," + getEndOffset() + (isGreedyToRight() ? "]" : ")") + " " + getId();
    }

    public int setIntervalStart(int start) {
        if (start < 0) {
            LOG.error("Negative start: " + start);
        }
        return myNode.setIntervalStart(start);
    }

    public int setIntervalEnd(int end) {
        if (end < 0) {
            LOG.error("Negative end: " + end);
        }
        return myNode.setIntervalEnd(end);
    }

    @Override
    public boolean isValid() {
        RangeMarkerTree.RMNode<?> node = myNode;
        if (node == null || !node.isValid()) {
            return false;
        }
        Object file = myDocumentOrFile;
        return file instanceof Document || canHaveDocument((VirtualFile) file);
    }

    private static boolean canHaveDocument(@Nonnull VirtualFile file) {
        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (document != null) {
            return true;
        }
        if (!file.isValid() || file.isDirectory() || isBinaryWithoutDecompiler(file)) {
            return false;
        }

        return !file.getFileType().isBinary() || !RawFileLoader.getInstance().isTooLarge(file.getLength());
    }

    private static boolean isBinaryWithoutDecompiler(@Nonnull VirtualFile file) {
        final FileType fileType = file.getFileType();
        return fileType.isBinary() && BinaryFileDecompiler.forFileType(fileType) == null;
    }

    public boolean setValid(boolean value) {
        RangeMarkerTree.RMNode<?> node = myNode;
        return node == null || node.setValid(value);
    }

    public int intervalStart() {
        RangeMarkerTree.RMNode<?> node = myNode;
        if (node == null) {
            return -1;
        }
        return node.intervalStart();
    }

    public int intervalEnd() {
        RangeMarkerTree.RMNode<?> node = myNode;
        if (node == null) {
            return -1;
        }
        return node.intervalEnd();
    }

    public RangeMarker findRangeMarkerAfter() {
        return myNode.getTree().findRangeMarkerAfter(this);
    }

    public RangeMarker findRangeMarkerBefore() {
        return myNode.getTree().findRangeMarkerBefore(this);
    }
}
