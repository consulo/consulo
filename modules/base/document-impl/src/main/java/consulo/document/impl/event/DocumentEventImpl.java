// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.document.impl.event;

import consulo.application.util.diff.Diff;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.impl.LineSet;
import consulo.document.internal.LineIterator;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.MergingCharSequence;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class DocumentEventImpl extends DocumentEvent {
    private final int myOffset;
    private final @Nonnull CharSequence myOldString;
    private final int myOldLength;
    private final @Nonnull CharSequence myNewString;
    private final int myNewLength;

    private final long myOldTimeStamp;
    private final boolean myIsWholeDocReplaced;
    private Diff.Change myChange;
    private static final Diff.Change TOO_BIG_FILE = new Diff.Change(0, 0, 0, 0, null);

    private final int myInitialStartOffset;
    private final int myInitialOldLength;
    private final int myMoveOffset;

    private LineSet myOldFragmentLineSet;
    private int myOldFragmentLineSetStart;

    public DocumentEventImpl(@Nonnull Document document,
                             int offset,
                             @Nonnull CharSequence oldString,
                             @Nonnull CharSequence newString,
                             long oldTimeStamp,
                             boolean wholeTextReplaced,
                             int initialStartOffset,
                             int initialOldLength,
                             int moveOffset) {
        super(document);
        myOffset = offset;

        myOldString = oldString;
        myOldLength = oldString.length();

        myNewString = newString;
        myNewLength = newString.length();

        myInitialStartOffset = initialStartOffset;
        myInitialOldLength = initialOldLength;
        myMoveOffset = moveOffset;

        myOldTimeStamp = oldTimeStamp;

        myIsWholeDocReplaced = getDocument().getTextLength() != 0 && wholeTextReplaced;
        assert initialStartOffset >= 0 : initialStartOffset;
        assert initialOldLength >= 0 : initialOldLength;
        assert moveOffset == offset || myOldLength == 0 || myNewLength == 0 : this;
        assert getOldFragment().length() == getOldLength() : "event.getOldFragment().length() = " + getOldFragment().length() + "; event.getOldLength() = " + getOldLength();
        assert getNewFragment().length() == getNewLength() : "event.getNewFragment().length() = " + getNewFragment().length() + "; event.getNewLength() = " + getNewLength();
    }

    @Override
    public int getOffset() {
        return myOffset;
    }

    @Override
    public int getOldLength() {
        return myOldLength;
    }

    @Override
    public int getNewLength() {
        return myNewLength;
    }

    @Override
    public @Nonnull CharSequence getOldFragment() {
        return myOldString;
    }

    @Override
    public @Nonnull CharSequence getNewFragment() {
        return myNewString;
    }

    /**
     * @return initial start offset as requested in {@link Document#replaceString(int, int, CharSequence)} call, before common prefix and
     * suffix were removed from the changed range.
     */
    public int getInitialStartOffset() {
        return myInitialStartOffset;
    }

    /**
     * @return initial "old fragment" length (endOffset - startOffset) as requested in {@link Document#replaceString(int, int, CharSequence)} call, before common prefix and
     * suffix were removed from the changed range.
     */
    public int getInitialOldLength() {
        return myInitialOldLength;
    }

    @Override
    public int getMoveOffset() {
        return myMoveOffset;
    }

    @Override
    public long getOldTimeStamp() {
        return myOldTimeStamp;
    }

    @Override
    public String toString() {
        return "DocumentEventImpl[myOffset=" + myOffset + ", myOldLength=" + myOldLength + ", myNewLength=" + myNewLength +
            "]" + (isWholeTextReplaced() ? " Whole." : ".");
    }

    @Override
    public boolean isWholeTextReplaced() {
        return myIsWholeDocReplaced;
    }

    public int translateLineViaDiff(int line) throws FilesTooBigForDiffException {
        Diff.Change change = reBuildDiffIfNeeded();
        if (change == null) {
            return line;
        }

        int startLine = getDocument().getLineNumber(getOffset());
        line -= startLine;
        int newLine = line;

        while (change != null) {
            if (line < change.line0) {
                break;
            }
            if (line >= change.line0 + change.deleted) {
                newLine += change.inserted - change.deleted;
            }
            else {
                int delta = Math.min(change.inserted, line - change.line0);
                newLine = change.line1 + delta;
                break;
            }

            change = change.link;
        }

        return newLine + startLine;
    }

    public int translateLineViaDiffStrict(int line) throws FilesTooBigForDiffException {
        Diff.Change change = reBuildDiffIfNeeded();
        if (change == null) {
            return line;
        }
        int startLine = getDocument().getLineNumber(getOffset());
        if (line < startLine) {
            return line;
        }
        int translatedRelative = Diff.translateLine(change, line - startLine);
        return translatedRelative < 0 ? -1 : translatedRelative + startLine;
    }

    // line numbers in Diff.Change are relative to change start
    private Diff.Change reBuildDiffIfNeeded() throws FilesTooBigForDiffException {
        if (myChange == TOO_BIG_FILE) {
            throw new FilesTooBigForDiffException();
        }
        if (myChange == null) {
            String[] oldLines = getOldLines();
            String[] newLines = Diff.splitLines(myNewString);
            try {
                myChange = Diff.buildChanges(oldLines, newLines);
            }
            catch (FilesTooBigForDiffException e) {
                myChange = TOO_BIG_FILE;
                throw e;
            }
        }
        return myChange;
    }

    @Nonnull
    private String[] getOldLines() {
        createOldFragmentLineSetIfNeeded();
        int offsetDiff = myOffset - myOldFragmentLineSetStart;
        LineIterator lineIterator = myOldFragmentLineSet.createIterator();
        List<String> lines = new ArrayList<>(myOldFragmentLineSet.getLineCount());
        while (!lineIterator.atEnd()) {
            int start = lineIterator.getStart() - offsetDiff;
            int end = lineIterator.getEnd() - lineIterator.getSeparatorLength() - offsetDiff;
            if (start >= 0 && end <= myOldString.length()) {
                lines.add(myOldString.subSequence(start, end).toString());
            }
            lineIterator.advance();
        }
        return lines.isEmpty() ? new String[]{""} : ArrayUtil.toStringArray(lines);
    }


    /**
     * This method is supposed to be called right after the document change, represented by this event instance (e.g. from
     * {@link DocumentListener#documentChanged(DocumentEvent)} callback).
     * Given an offset ({@code offsetBeforeUpdate}), it calculates the line number that would be returned by
     * {@link Document#getLineNumber(int)}, if that call would be performed before the document change.
     */
    public int getLineNumberBeforeUpdate(int offsetBeforeUpdate) {
        createOldFragmentLineSetIfNeeded();
        Document document = getDocument();
        if (offsetBeforeUpdate <= myOldFragmentLineSetStart) {
            return document.getLineNumber(offsetBeforeUpdate);
        }
        int oldFragmentLineSetEnd = myOldFragmentLineSetStart + myOldFragmentLineSet.getLength();
        if (offsetBeforeUpdate <= oldFragmentLineSetEnd) {
            return document.getLineNumber(myOldFragmentLineSetStart) +
                myOldFragmentLineSet.findLineIndex(offsetBeforeUpdate - myOldFragmentLineSetStart);
        }
        int shift = getNewLength() - getOldLength();
        return document.getLineNumber(myOldFragmentLineSetStart) +
            (myOldFragmentLineSetStart == oldFragmentLineSetEnd ? 0 : myOldFragmentLineSet.getLineCount() - 1) +
            document.getLineNumber(offsetBeforeUpdate + shift) - document.getLineNumber(oldFragmentLineSetEnd + shift);
    }

    private void createOldFragmentLineSetIfNeeded() {
        if (myOldFragmentLineSet != null) {
            return;
        }
        CharSequence newText = getDocument().getImmutableCharSequence();
        CharSequence oldFragment = getOldFragment();
        myOldFragmentLineSetStart = getOffset();
        if (myOldFragmentLineSetStart > 0 && newText.charAt(myOldFragmentLineSetStart - 1) == '\r') {
            myOldFragmentLineSetStart--;
            oldFragment = new MergingCharSequence("\r", oldFragment);
        }
        int newChangeEnd = getOffset() + getNewLength();
        if (newChangeEnd < newText.length() && newText.charAt(newChangeEnd) == '\n') {
            oldFragment = new MergingCharSequence(oldFragment, "\n");
        }
        myOldFragmentLineSet = LineSet.createLineSet(oldFragment);
    }
}
