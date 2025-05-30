// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.document.impl;

import consulo.application.ReadAction;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.impl.event.DocumentEventImpl;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.RangeMarkerEx;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.document.util.TextRangeScalarUtil;
import consulo.document.util.UnfairTextRange;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * This class is an extension to range marker that tries to restore its range even in situations when target text referenced by it
 * is replaced.
 * <p/>
 * Example: consider that the user selects all text at editor (Ctrl+A), copies it to the buffer (Ctrl+C) and performs paste (Ctrl+V).
 * All document text is replaced then but in essence it's the same, hence, we may want particular range markers to be still valid.
 */
class PersistentRangeMarker extends RangeMarkerImpl {
    @Nonnull
    private LinesCols myLinesCols;
    private volatile boolean documentLoaded;

    PersistentRangeMarker(@Nonnull DocumentEx document, int startOffset, int endOffset, boolean register) {
        super(document, startOffset, endOffset, register, false);
        myLinesCols = Objects.requireNonNull(storeLinesAndCols(document, TextRangeScalarUtil.toScalarRange(startOffset, endOffset)));
        documentLoaded = true;
    }

    // The constructor which creates a marker without a document and saves it in the virtual file directly. Can be cheaper than loading the entire document.
    PersistentRangeMarker(@Nonnull VirtualFile virtualFile,
                          int startOffset,
                          int endOffset,
                          int startLine,
                          int startCol,
                          int endLine,
                          int endCol,
                          int estimatedDocumentLength,
                          boolean register) {
        super(virtualFile, startOffset, endOffset, estimatedDocumentLength, register);
        myLinesCols = new LinesCols(startLine, startCol, endLine, endCol);
        documentLoaded = FileDocumentManager.getInstance().getCachedDocument(virtualFile) != null;
    }

    static @Nullable LinesCols storeLinesAndCols(@Nonnull Document document, long range) {
        LineCol start = calcLineCol(document, TextRangeScalarUtil.startOffset(range));
        LineCol end = calcLineCol(document, TextRangeScalarUtil.endOffset(range));

        if (start == null || end == null) {
            return null;
        }
        return new LinesCols(start.line, start.col, end.line, end.col);
    }

    private static LineCol calcLineCol(@Nonnull Document document, int offset) {
        // document might have been changed already
        if (offset <= document.getTextLength()) {
            int line = document.getLineNumber(offset);
            int col = offset - document.getLineStartOffset(line);
            if (col < 0) {
                return null;
            }
            return new LineCol(line, col);
        }
        return null;
    }

    private static class LineCol {
        private final int line;
        private final int col;

        LineCol(int line, int col) {
            this.line = line;
            this.col = col;
        }
    }

    static @Nullable Pair.NonNull<TextRange, LinesCols> translateViaDiff(@Nonnull DocumentEventImpl event, @Nonnull LinesCols linesCols) {
        try {
            int myStartLine = event.translateLineViaDiffStrict(linesCols.myStartLine);
            Document document = event.getDocument();
            if (myStartLine < 0 || myStartLine >= document.getLineCount()) {
                return null;
            }

            int start = document.getLineStartOffset(myStartLine) + linesCols.myStartColumn;
            if (start >= document.getTextLength()) {
                return null;
            }

            int myEndLine = event.translateLineViaDiffStrict(linesCols.myEndLine);
            if (myEndLine < 0 || myEndLine >= document.getLineCount()) {
                return null;
            }

            int end = document.getLineStartOffset(myEndLine) + linesCols.myEndColumn;
            if (end > document.getTextLength() || end < start) {
                return null;
            }

            if (end > event.getDocument().getTextLength() ||
                myEndLine < myStartLine ||
                myStartLine == myEndLine && linesCols.myEndColumn < linesCols.myStartColumn ||
                event.getDocument().getLineCount() < myEndLine) {
                return null;
            }

            return Pair.createNonNull(new TextRange(start, end), new LinesCols(myStartLine, linesCols.myStartColumn, myEndLine, linesCols.myEndColumn));
        }
        catch (FilesTooBigForDiffException e) {
            return null;
        }
    }

    @Override
    protected void changedUpdateImpl(@Nonnull DocumentEvent event) {
        if (!isValid()) {
            return;
        }

        long translatedRange = -1;
        LinesCols translatedLineCols = null;
        if (PersistentRangeMarkerUtil.shouldTranslateViaDiff(event, toScalarRange())) {
            Pair.NonNull<TextRange, LinesCols> translated = translateViaDiff((DocumentEventImpl) event, myLinesCols);
            translatedRange = translated == null ? -1 : TextRangeScalarUtil.toScalarRange(translated.first);
            translatedLineCols = Pair.getSecond(translated);
        }
        if (translatedRange == -1) {
            RangeMarkerTree.RMNode<RangeMarkerEx> node = myNode;
            translatedRange = node == null ? -1 : applyChange(event, node.toScalarRange(), isGreedyToLeft(), isGreedyToRight(), isStickingToRight());
            if (translatedRange != -1) {
                translatedLineCols = storeLinesAndCols(event.getDocument(), translatedRange);
            }
        }
        if (translatedRange == -1 || translatedLineCols == null) {
            invalidate();
        }
        else {
            setRange(translatedRange);
            myLinesCols = translatedLineCols;
        }
    }

    @Override
    public String toString() {
        return "PersistentRangeMarker" +
            (isGreedyToLeft() ? "[" : "(") +
            (isValid() ? "valid" : "invalid") + "," + getStartOffset() + "," + getEndOffset() +
            " " + myLinesCols +
            (isGreedyToRight() ? "]" : ")");
    }

    static final class LinesCols {
        private final int myStartLine;
        private final int myStartColumn;
        private final int myEndLine;
        private final int myEndColumn;

        private LinesCols(int startLine, int startColumn, int endLine, int endColumn) {
            myStartLine = startLine;
            myStartColumn = startColumn;
            myEndLine = endLine;
            myEndColumn = endColumn;
        }

        @Override
        public String toString() {
            return myStartLine + ":" + myStartColumn + "-" + myEndLine + ":" + myEndColumn;
        }
    }

    @Override
    public int getStartOffset() {
        // load document in case this is a lazy persistent marker and the document wasn't loaded yet, because we need to convert (line;col) to offset first in this case
        if (!isDocumentLoaded()) {
            ReadAction.run(() -> getDocument());
        }
        return super.getStartOffset();
    }

    @Override
    public int getEndOffset() {
        // load document in case this is a lazy persistent marker and the document wasn't loaded yet, because we need to convert (line;col) to offset first in this case
        if (!isDocumentLoaded()) {
            ReadAction.run(() -> getDocument());
        }
        return super.getEndOffset();
    }

    @Override
    @Nonnull
    TextRange reCalcTextRangeAfterReload(@Nonnull DocumentImpl document, int tabSize) {
        // have to convert line/col back to offset if the persistent range marker was created with line/col only
        LinesCols linesCols = myLinesCols;
        int startOffset = DocumentUtil.calculateOffset(document, linesCols.myStartLine, linesCols.myStartColumn, tabSize);
        int endOffset = DocumentUtil.calculateOffset(document, linesCols.myEndLine, linesCols.myEndColumn, tabSize);
        documentLoaded = true;
        return new UnfairTextRange(startOffset, endOffset);
    }

    private boolean isDocumentLoaded() {
        return documentLoaded;
    }
}
