// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.document.impl;

import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.internal.*;
import consulo.document.util.TextRange;
import consulo.util.dataholder.Key;
import consulo.util.lang.ImmutableCharSequence;
import consulo.util.lang.ref.SoftReference;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @author peter
 */
public class FrozenDocument implements DocumentEx {
    private final ImmutableCharSequence myText;
    private volatile @Nullable SoftReference<LineSet> myLineSet;
    private final long myStamp;
    private volatile SoftReference<String> myTextString;

    FrozenDocument(ImmutableCharSequence text, @Nullable LineSet lineSet, long stamp, @Nullable String textString) {
        myText = text;
        myLineSet = lineSet == null ? null : new SoftReference<>(lineSet);
        myStamp = stamp;
        myTextString = textString == null ? null : new SoftReference<>(textString);
    }

    
    private LineSet getLineSet() {
        LineSet lineSet = SoftReference.dereference(myLineSet);
        if (lineSet == null) {
            myLineSet = new SoftReference<>(lineSet = LineSet.createLineSet(myText));
        }
        return lineSet;
    }

    public FrozenDocument applyEvent(DocumentEvent event, int newStamp) {
        int offset = event.getOffset();
        int oldEnd = offset + event.getOldLength();
        ImmutableCharSequence newText = myText.delete(offset, oldEnd).insert(offset, event.getNewFragment());
        LineSet newLineSet = getLineSet().update(myText, offset, oldEnd, event.getNewFragment(), event.isWholeTextReplaced());
        return new FrozenDocument(newText, newLineSet, newStamp, null);
    }

    
    @Override
    public LineIterator createLineIterator() {
        return getLineSet().createIterator();
    }

    @Override
    public void setModificationStamp(long modificationStamp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceText(CharSequence chars, long newModificationStamp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearLineModificationFlags() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeRangeMarker(RangeMarkerEx rangeMarker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerRangeMarker(
        RangeMarkerEx rangeMarker,
        int start,
        int end,
        boolean greedyToLeft,
        boolean greedyToRight,
        int layer
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean processRangeMarkers(Predicate<? super RangeMarker> processor) {
        return true;
    }

    @Override
    public boolean processRangeMarkersOverlappingWith(int start, int end, Predicate<? super RangeMarker> processor) {
        return true;
    }

    
    @Override
    public String getText() {
        String s = SoftReference.dereference(myTextString);
        if (s == null) {
            myTextString = new SoftReference<>(s = myText.toString());
        }
        return s;
    }

    
    @Override
    public String getText(TextRange range) {
        return myText.subSequence(range.getStartOffset(), range.getEndOffset()).toString();
    }

    
    @Override
    public CharSequence getCharsSequence() {
        return myText;
    }

    
    @Override
    public CharSequence getImmutableCharSequence() {
        return myText;
    }

    @Override
    public int getLineCount() {
        return getLineSet().getLineCount();
    }

    @Override
    public int getLineNumber(int offset) {
        return getLineSet().findLineIndex(offset);
    }

    @Override
    public int getLineStartOffset(int line) {
        if (line == 0) {
            return 0; // otherwise it crashed for zero-length document
        }
        return getLineSet().getLineStart(line);
    }

    @Override
    public int getLineEndOffset(int line) {
        if (getTextLength() == 0 && line == 0) {
            return 0;
        }
        int result = getLineSet().getLineEnd(line) - getLineSeparatorLength(line);
        assert result >= 0;
        return result;
    }

    @Override
    public void insertString(int offset, CharSequence s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteString(int startOffset, int endOffset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceString(int startOffset, int endOffset, CharSequence s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public long getModificationStamp() {
        return myStamp;
    }

    
    @Override
    public RangeMarker createRangeMarker(int startOffset, int endOffset, boolean surviveOnExternalChange) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly(boolean isReadOnly) {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public RangeMarker createGuardedBlock(int startOffset, int endOffset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeGuardedBlock(RangeMarker block) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable RangeMarker getOffsetGuard(int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable RangeMarker getRangeGuard(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startGuardedBlockChecking() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopGuardedBlockChecking() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setText(CharSequence text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLineSeparatorLength(int line) {
        return getLineSet().getSeparatorLength(line);
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStripTrailingSpacesEnabled(boolean isEnabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEditReadOnlyListener(EditReadOnlyListener listener) {
        throw new UnsupportedOperationException();
    }
}
