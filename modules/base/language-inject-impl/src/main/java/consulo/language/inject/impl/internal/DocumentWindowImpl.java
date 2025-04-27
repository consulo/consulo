// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.inject.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.LogicalPosition;
import consulo.disposer.Disposable;
import consulo.document.DocumentWindow;
import consulo.document.FileDocumentManager;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentListener;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.EditReadOnlyListener;
import consulo.document.internal.LineIterator;
import consulo.document.internal.RangeMarkerEx;
import consulo.document.util.ProperTextRange;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.ImmutableCharSequence;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

class DocumentWindowImpl extends UserDataHolderBase implements Disposable, DocumentWindow, DocumentEx {
    private static final Logger LOG = Logger.getInstance(DocumentWindowImpl.class);
    private final DocumentEx myDelegate;
    private final boolean myOneLine;
    private PlaceImpl myShreds; // guarded by myLock
    private final int myPrefixLineCount;
    private final int mySuffixLineCount;
    private final Object myLock = new Object();

    private CachedText myCachedText;

    DocumentWindowImpl(@Nonnull DocumentEx delegate, @Nonnull PlaceImpl shreds) {
        myDelegate = delegate;
        myOneLine = ContainerUtil.and(shreds, s -> ((ShredImpl)s).isOneLine());
        synchronized (myLock) {
            myShreds = shreds;
        }
        myPrefixLineCount = Math.max(1, 1 + StringUtil.countNewLines(shreds.get(0).getPrefix()));
        mySuffixLineCount = Math.max(1, 1 + StringUtil.countNewLines(shreds.get(shreds.size() - 1).getSuffix()));
    }

    /**
     * @param hPos
     * @return null means we were unable to calculate
     */
    @Nullable
    @RequiredReadAction
    LogicalPosition hostToInjectedInVirtualSpace(@Nonnull LogicalPosition hPos) {
        // beware the virtual space
        int hLineStartOffset =
            hPos.line >= myDelegate.getLineCount() ? myDelegate.getTextLength() : myDelegate.getLineStartOffset(hPos.line);
        int iLineStartOffset = hostToInjected(hLineStartOffset);
        int iLine = getLineNumber(iLineStartOffset);

        synchronized (myLock) {
            for (int i = myShreds.size() - 1; i >= 0; i--) {
                PsiLanguageInjectionHost.Shred shred = myShreds.get(i);
                if (!shred.isValid()) {
                    continue;
                }
                Segment hostRangeMarker = shred.getHostRangeMarker();
                if (hostRangeMarker == null) {
                    continue;
                }
                int hShredEndOffset = hostRangeMarker.getEndOffset();
                int hShredStartOffset = hostRangeMarker.getStartOffset();

                int hShredStartLine = myDelegate.getLineNumber(hShredStartOffset);
                int hShredEndLine = myDelegate.getLineNumber(hShredEndOffset);

                if (hShredStartLine <= hPos.line && hPos.line <= hShredEndLine) {
                    int hColumnOfShredEnd = hShredEndOffset - hLineStartOffset;
                    int iColumnOfShredEnd = hostToInjected(hShredEndOffset) - iLineStartOffset;
                    int iColumn = iColumnOfShredEnd + hPos.column - hColumnOfShredEnd;
                    return new LogicalPosition(iLine, iColumn);
                }
            }
        }

        return null;
    }

    private static class CachedText {
        private final String text;
        private final long modificationStamp;

        private CachedText(@Nonnull String text, long modificationStamp) {
            this.text = text;
            this.modificationStamp = modificationStamp;
        }

        @Nonnull
        private String getText() {
            return text;
        }

        private long getModificationStamp() {
            return modificationStamp;
        }
    }


    @Override
    @RequiredReadAction
    public int getLineCount() {
        return 1 + StringUtil.countNewLines(getText());
    }

    @Override
    public int getLineStartOffset(int line) {
        LOG.assertTrue(line >= 0, line);
        if (line == 0) {
            return 0;
        }
        String hostText = myDelegate.getText();

        int[] pos = new int[2]; // pos[0] = curLine; pos[1] == offset;
        synchronized (myLock) {
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                Segment hostRange = shred.getHostRangeMarker();
                if (hostRange == null) {
                    continue;
                }

                int found = countNewLinesIn(shred.getPrefix(), pos, line);
                if (found != -1) {
                    return found;
                }

                CharSequence text = hostText.subSequence(hostRange.getStartOffset(), hostRange.getEndOffset());
                found = countNewLinesIn(text, pos, line);
                if (found != -1) {
                    return found;
                }

                found = countNewLinesIn(shred.getSuffix(), pos, line);
                if (found != -1) {
                    return found;
                }
            }
        }

        return pos[1];
    }

    // returns startOffset found, or -1 if need to continue searching
    private static int countNewLinesIn(CharSequence text, int[] pos, int line) {
        int offsetInside = 0;
        for (int i = StringUtil.indexOf(text, '\n'); i != -1; i = StringUtil.indexOf(text, '\n', offsetInside)) {
            int curLine = ++pos[0];
            int lineLength = i + 1 - offsetInside;
            int offset = pos[1] += lineLength;
            offsetInside += lineLength;
            if (curLine == line) {
                return offset;
            }
        }
        pos[1] += text.length() - offsetInside;
        return -1;
    }

    @Override
    @RequiredReadAction
    public int getLineEndOffset(int line) {
        LOG.assertTrue(line >= 0, line);
        if (line == getLineCount() - 1) {
            return getTextLength(); // end >= start assertion fix for last line
        }
        int startOffsetOfNextLine = getLineStartOffset(line + 1);
        return startOffsetOfNextLine == 0 || getText().charAt(startOffsetOfNextLine - 1) != '\n' ? startOffsetOfNextLine : startOffsetOfNextLine - 1;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getText() {
        CachedText cachedText = myCachedText;

        if (cachedText == null || cachedText.getModificationStamp() != getModificationStamp()) {
            myCachedText = cachedText = new CachedText(calcText(), getModificationStamp());
        }

        return cachedText.getText();
    }

    @Nonnull
    private String calcText() {
        StringBuilder text = new StringBuilder();
        CharSequence hostText = myDelegate.getCharsSequence();
        synchronized (myLock) {
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                Segment hostRange = shred.getHostRangeMarker();
                if (hostRange != null) {
                    text.append(shred.getPrefix());
                    text.append(hostText, hostRange.getStartOffset(), hostRange.getEndOffset());
                    text.append(shred.getSuffix());
                }
            }
        }
        return text.toString();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public CharSequence getImmutableCharSequence() {
        return ImmutableCharSequence.asImmutable(getText());
    }

    @Override
    public int getTextLength() {
        int length = 0;
        synchronized (myLock) {
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                Segment hostRange = shred.getHostRangeMarker();
                if (hostRange == null) {
                    continue;
                }
                length += shred.getPrefix().length();
                length += hostRange.getEndOffset() - hostRange.getStartOffset();
                length += shred.getSuffix().length();
            }
        }
        return length;
    }

    @Override
    @RequiredReadAction
    public int getLineNumber(int offset) {
        int lineNumber = 0;
        String hostText = myDelegate.getText();
        synchronized (myLock) {
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                String prefix = shred.getPrefix();
                String suffix = shred.getSuffix();
                lineNumber += StringUtil.getLineBreakCount(prefix.substring(0, Math.min(offset, prefix.length())));
                if (offset < prefix.length()) {
                    return lineNumber;
                }
                offset -= prefix.length();

                Segment currentRange = shred.getHostRangeMarker();
                if (currentRange == null) {
                    continue;
                }
                int rangeLength = currentRange.getEndOffset() - currentRange.getStartOffset();
                CharSequence rangeText = hostText.subSequence(currentRange.getStartOffset(), currentRange.getEndOffset());

                lineNumber += StringUtil.getLineBreakCount(rangeText.subSequence(0, Math.min(offset, rangeLength)));
                if (offset < rangeLength) {
                    return lineNumber;
                }
                offset -= rangeLength;

                lineNumber += StringUtil.getLineBreakCount(suffix.substring(0, Math.min(offset, suffix.length())));
                if (offset < suffix.length()) {
                    return lineNumber;
                }

                offset -= suffix.length();
            }
        }
        lineNumber = getLineCount() - 1;
        return lineNumber < 0 ? 0 : lineNumber;
    }

    @Override
    public TextRange getHostRange(int hostOffset) {
        synchronized (myLock) {
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                Segment currentRange = shred.getHostRangeMarker();
                if (currentRange == null) {
                    continue;
                }
                TextRange textRange = ProperTextRange.create(currentRange);
                if (textRange.grown(1).contains(hostOffset)) {
                    return textRange;
                }
            }
        }
        return null;
    }

    @Override
    public void insertString(int offset, @Nonnull CharSequence s) {
        assert intersectWithEditable(new TextRange(offset, offset)) != null;
        if (isOneLine()) {
            s = StringUtil.replace(s.toString(), "\n", "");
        }
        myDelegate.insertString(injectedToHost(offset), s);
    }

    @Override
    public void deleteString(int startOffset, int endOffset) {
        assert intersectWithEditable(new TextRange(startOffset, startOffset)) != null;
        assert intersectWithEditable(new TextRange(endOffset, endOffset)) != null;

        List<TextRange> hostRangesToDelete;
        synchronized (myLock) {
            hostRangesToDelete = new ArrayList<>(myShreds.size());

            int offset = startOffset;
            int curRangeStart = 0;
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                curRangeStart += shred.getPrefix().length();
                if (offset < curRangeStart) {
                    offset = curRangeStart;
                }
                if (offset >= endOffset) {
                    break;
                }
                Segment hostRange = shred.getHostRangeMarker();
                if (hostRange == null) {
                    continue;
                }
                int hostRangeLength = hostRange.getEndOffset() - hostRange.getStartOffset();
                TextRange range = TextRange.from(curRangeStart, hostRangeLength);
                if (range.contains(offset)) {
                    TextRange rangeToDelete = new TextRange(offset, Math.min(range.getEndOffset(), endOffset));
                    hostRangesToDelete.add(rangeToDelete.shiftRight(hostRange.getStartOffset() - curRangeStart));
                    offset = rangeToDelete.getEndOffset();
                }
                curRangeStart += hostRangeLength;
                curRangeStart += shred.getSuffix().length();
            }
        }

        int delta = 0;
        for (TextRange hostRangeToDelete : hostRangesToDelete) {
            myDelegate.deleteString(hostRangeToDelete.getStartOffset() + delta, hostRangeToDelete.getEndOffset() + delta);
            delta -= hostRangeToDelete.getLength();
        }
    }

    @Override
    public void replaceString(int startOffset, int endOffset, @Nonnull CharSequence s) {
        if (isOneLine()) {
            s = StringUtil.replace(s.toString(), "\n", "");
        }

        CharSequence chars = getCharsSequence();
        CharSequence toDelete = chars.subSequence(startOffset, endOffset);

        int prefixLength = StringUtil.commonPrefixLength(s, toDelete);
        int suffixLength =
            StringUtil.commonSuffixLength(toDelete.subSequence(prefixLength, toDelete.length()), s.subSequence(prefixLength, s.length()));
        startOffset += prefixLength;
        endOffset -= suffixLength;
        s = s.subSequence(prefixLength, s.length() - suffixLength);

        doReplaceString(startOffset, endOffset, s);
    }

    private void doReplaceString(int startOffset, int endOffset, CharSequence s) {
        assert intersectWithEditable(new TextRange(startOffset, startOffset)) != null;
        assert intersectWithEditable(new TextRange(endOffset, endOffset)) != null;

        List<Pair<TextRange, CharSequence>> hostRangesToModify;
        synchronized (myLock) {
            hostRangesToModify = new ArrayList<>(myShreds.size());

            int offset = startOffset;
            int curRangeStart = 0;
            for (int i = 0; i < myShreds.size(); i++) {
                PsiLanguageInjectionHost.Shred shred = myShreds.get(i);
                curRangeStart += shred.getPrefix().length();
                if (offset < curRangeStart) {
                    offset = curRangeStart;
                }
                Segment hostRange = shred.getHostRangeMarker();
                if (hostRange == null) {
                    continue;
                }
                int hostRangeLength = hostRange.getEndOffset() - hostRange.getStartOffset();
                TextRange range = TextRange.from(curRangeStart, hostRangeLength);
                if (range.contains(offset) || range.getEndOffset() == offset/* in case of inserting at the end*/) {
                    TextRange rangeToModify = new TextRange(offset, Math.min(range.getEndOffset(), endOffset));
                    TextRange hostRangeToModify = rangeToModify.shiftRight(hostRange.getStartOffset() - curRangeStart);
                    CharSequence toReplace =
                        i == myShreds.size() - 1 || range.getEndOffset() + shred.getSuffix().length() >= endOffset ? s : s.subSequence(
                            0,
                            Math.min(hostRangeToModify.getLength(), s.length())
                        );
                    s = toReplace == s ? "" : s.subSequence(toReplace.length(), s.length());
                    hostRangesToModify.add(Pair.create(hostRangeToModify, toReplace));
                    offset = rangeToModify.getEndOffset();
                }
                curRangeStart += hostRangeLength;
                curRangeStart += shred.getSuffix().length();
                if (curRangeStart > endOffset) {
                    break;
                }
            }
        }

        int delta = 0;
        for (Pair<TextRange, CharSequence> pair : hostRangesToModify) {
            TextRange hostRange = pair.getFirst();
            CharSequence replace = pair.getSecond();

            myDelegate.replaceString(hostRange.getStartOffset() + delta, hostRange.getEndOffset() + delta, replace);
            delta -= hostRange.getLength() - replace.length();
        }
    }

    @Override
    public boolean isWritable() {
        return myDelegate.isWritable();
    }

    @Override
    @RequiredReadAction
    public long getModificationStamp() {
        return isValid() ? myDelegate.getModificationStamp() : -1;
    }

    @Override
    public int getModificationSequence() {
        return myDelegate.getModificationSequence();
    }

    @Override
    public void fireReadOnlyModificationAttempt() {
        myDelegate.fireReadOnlyModificationAttempt();
    }

    @Override
    public void addDocumentListener(@Nonnull DocumentListener listener) {
        myDelegate.addDocumentListener(listener);
    }

    @Override
    public void addDocumentListener(@Nonnull DocumentListener listener, @Nonnull Disposable parentDisposable) {
        myDelegate.addDocumentListener(listener, parentDisposable);
    }

    @Override
    public void removeDocumentListener(@Nonnull DocumentListener listener) {
        myDelegate.removeDocumentListener(listener);
    }

    @Override
    @Nonnull
    public RangeMarker createRangeMarker(int startOffset, int endOffset) {
        ProperTextRange hostRange = injectedToHost(new ProperTextRange(startOffset, endOffset));
        RangeMarker hostMarker = myDelegate.createRangeMarker(hostRange);
        int startShift = Math.max(0, hostToInjected(hostRange.getStartOffset()) - startOffset);
        int endShift = Math.max(0, endOffset - hostToInjected(hostRange.getEndOffset()) - startShift);
        return new RangeMarkerWindow(this, (RangeMarkerEx)hostMarker, startShift, endShift);
    }

    @Override
    @Nonnull
    public RangeMarker createRangeMarker(int startOffset, int endOffset, boolean surviveOnExternalChange) {
        if (!surviveOnExternalChange) {
            return createRangeMarker(startOffset, endOffset);
        }
        ProperTextRange hostRange = injectedToHost(new ProperTextRange(startOffset, endOffset));
        //todo persistent?
        RangeMarker hostMarker = myDelegate.createRangeMarker(hostRange.getStartOffset(), hostRange.getEndOffset(), true);
        int startShift = Math.max(0, hostToInjected(hostRange.getStartOffset()) - startOffset);
        int endShift = Math.max(0, endOffset - hostToInjected(hostRange.getEndOffset()) - startShift);
        return new RangeMarkerWindow(this, (RangeMarkerEx)hostMarker, startShift, endShift);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
        myDelegate.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
        myDelegate.removePropertyChangeListener(listener);
    }

    @Override
    public void setReadOnly(boolean isReadOnly) {
        myDelegate.setReadOnly(isReadOnly);
    }

    @Override
    @Nonnull
    public RangeMarker createGuardedBlock(int startOffset, int endOffset) {
        ProperTextRange hostRange = injectedToHost(new ProperTextRange(startOffset, endOffset));
        return myDelegate.createGuardedBlock(hostRange.getStartOffset(), hostRange.getEndOffset());
    }

    @Override
    public void removeGuardedBlock(@Nonnull RangeMarker block) {
        myDelegate.removeGuardedBlock(block);
    }

    @Override
    public RangeMarker getRangeGuard(int startOffset, int endOffset) {
        ProperTextRange injRange = new ProperTextRange(startOffset, endOffset);
        // include prefixes/suffixes in guarded ranges - they can't be edited
        TextRange editable = ObjectUtil.notNull(intersectWithEditable(injRange), TextRange.EMPTY_RANGE);
        if (!injRange.equals(editable)) {
            ProperTextRange guarded = injRange.cutOut(editable.shiftLeft(editable.getStartOffset() - injRange.getStartOffset()));
            return createRangeMarker(guarded);
        }

        ProperTextRange hostRange = injectedToHost(injRange);
        return myDelegate.getRangeGuard(hostRange.getStartOffset(), hostRange.getEndOffset());
    }

    @Override
    public void startGuardedBlockChecking() {
        myDelegate.startGuardedBlockChecking();
    }

    @Override
    public void stopGuardedBlockChecking() {
        myDelegate.stopGuardedBlockChecking();
    }

    @Override
    public void setCyclicBufferSize(int bufferSize) {
        myDelegate.setCyclicBufferSize(bufferSize);
    }

    @Override
    public void setText(@Nonnull CharSequence text) {
        synchronized (myLock) {
            LOG.assertTrue(text.toString().startsWith(myShreds.get(0).getPrefix()));
            LOG.assertTrue(text.toString().endsWith(myShreds.get(myShreds.size() - 1).getSuffix()));
            if (isOneLine()) {
                text = StringUtil.replace(text.toString(), "\n", "");
            }
            String[] changes = calculateMinEditSequence(text.toString());
            assert changes.length == myShreds.size();
            for (int i = 0; i < changes.length; i++) {
                String change = changes[i];
                if (change != null) {
                    Segment hostRange = myShreds.get(i).getHostRangeMarker();
                    if (hostRange == null) {
                        continue;
                    }
                    myDelegate.replaceString(hostRange.getStartOffset(), hostRange.getEndOffset(), change);
                }
            }
        }
    }

    @Override
    @RequiredReadAction
    public boolean isLineModified(int line) {
        return myDelegate.isLineModified(injectedToHostLine(line));
    }

    @Override
    @Nonnull
    public Segment[] getHostRanges() {
        synchronized (myLock) {
            List<Segment> markers = new ArrayList<>(myShreds.size());
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                Segment hostMarker = shred.getHostRangeMarker();
                if (hostMarker != null) {
                    markers.add(hostMarker);
                }
            }
            return markers.isEmpty() ? Segment.EMPTY_ARRAY : markers.toArray(Segment.EMPTY_ARRAY);
        }
    }

    @Override
    @Nonnull
    public RangeMarker createRangeMarker(@Nonnull TextRange textRange) {
        ProperTextRange properTextRange = new ProperTextRange(textRange);
        return createRangeMarker(properTextRange.getStartOffset(), properTextRange.getEndOffset());
    }

    @Override
    public void setStripTrailingSpacesEnabled(boolean isEnabled) {
        myDelegate.setStripTrailingSpacesEnabled(isEnabled);
    }

    @Override
    @RequiredReadAction
    public int getLineSeparatorLength(int line) {
        return myDelegate.getLineSeparatorLength(injectedToHostLine(line));
    }

    @Override
    @Nonnull
    public LineIterator createLineIterator() {
        return myDelegate.createLineIterator();
    }

    @Override
    public void setModificationStamp(long modificationStamp) {
        myDelegate.setModificationStamp(modificationStamp);
    }

    @Override
    public void addEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
        myDelegate.addEditReadOnlyListener(listener);
    }

    @Override
    public void removeEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
        myDelegate.removeEditReadOnlyListener(listener);
    }

    @Override
    public void replaceText(@Nonnull CharSequence chars, long newModificationStamp) {
        setText(chars);
        myDelegate.setModificationStamp(newModificationStamp);
    }

    @Override
    public void suppressGuardedExceptions() {
        myDelegate.suppressGuardedExceptions();
    }

    @Override
    public void unSuppressGuardedExceptions() {
        myDelegate.unSuppressGuardedExceptions();
    }

    @Override
    public boolean isInEventsHandling() {
        return myDelegate.isInEventsHandling();
    }

    @Override
    public boolean removeRangeMarker(@Nonnull RangeMarkerEx rangeMarker) {
        return myDelegate.removeRangeMarker(((RangeMarkerWindow)rangeMarker).getDelegate());
    }

    @Override
    public void registerRangeMarker(
        @Nonnull RangeMarkerEx rangeMarker,
        int start,
        int end,
        boolean greedyToLeft,
        boolean greedyToRight,
        int layer
    ) {
        throw new IllegalStateException();
    }

    @Override
    @Nonnull
    public DocumentEx getDelegate() {
        return myDelegate;
    }

    @Override
    public int hostToInjected(int hostOffset) {
        synchronized (myLock) {
            Segment hostRangeMarker = myShreds.get(0).getHostRangeMarker();
            if (hostRangeMarker == null || hostOffset < hostRangeMarker.getStartOffset()) {
                return myShreds.get(0).getPrefix().length();
            }
            int offset = 0;
            for (int i = 0; i < myShreds.size(); i++) {
                offset += myShreds.get(i).getPrefix().length();
                Segment currentRange = myShreds.get(i).getHostRangeMarker();
                if (currentRange == null) {
                    continue;
                }
                Segment nextRange = i == myShreds.size() - 1 ? null : myShreds.get(i + 1).getHostRangeMarker();
                if (nextRange == null || hostOffset < nextRange.getStartOffset()) {
                    if (hostOffset >= currentRange.getEndOffset()) {
                        hostOffset = currentRange.getEndOffset();
                    }
                    return offset + hostOffset - currentRange.getStartOffset();
                }
                offset += currentRange.getEndOffset() - currentRange.getStartOffset();
                offset += myShreds.get(i).getSuffix().length();
            }
            return getTextLength() - myShreds.get(myShreds.size() - 1).getSuffix().length();
        }
    }

    @Override
    public int injectedToHost(int offset) {
        int offsetInLeftFragment = injectedToHost(offset, true, false);
        int offsetInRightFragment = injectedToHost(offset, false, false);
        if (offsetInLeftFragment == offsetInRightFragment) {
            return offsetInLeftFragment;
        }

        // heuristics: return offset closest to the caret
        Editor[] editors = EditorFactory.getInstance().getEditors(getDelegate());
        Editor editor = editors.length == 0 ? null : editors[0];
        if (editor != null) {
            if (editor instanceof EditorWindow) {
                editor = ((EditorWindow)editor).getDelegate();
            }
            int caret = editor.getCaretModel().getOffset();
            return Math.abs(caret - offsetInLeftFragment) < Math.abs(caret - offsetInRightFragment) ? offsetInLeftFragment : offsetInRightFragment;
        }
        return offsetInLeftFragment;
    }

    @Override
    public int injectedToHost(int injectedOffset, boolean minHostOffset) {
        return injectedToHost(injectedOffset, minHostOffset, true);
    }

    private int injectedToHost(int offset, boolean preferLeftFragment, boolean skipEmptyShreds) {
        synchronized (myLock) {
            if (offset < myShreds.get(0).getPrefix().length()) {
                Segment hostRangeMarker = myShreds.get(0).getHostRangeMarker();
                return hostRangeMarker == null ? 0 : hostRangeMarker.getStartOffset();
            }
            int prevEnd = 0;
            for (int i = 0; i < myShreds.size(); i++) {
                PsiLanguageInjectionHost.Shred shred = myShreds.get(i);
                Segment currentRange = shred.getHostRangeMarker();
                if (currentRange == null) {
                    continue;
                }
                int currentStart = currentRange.getStartOffset();
                int currentEnd = currentRange.getEndOffset();
                if (skipEmptyShreds && !preferLeftFragment && currentStart == currentEnd
                    && shred.getPrefix().isEmpty() && shred.getSuffix().isEmpty()) {
                    continue;
                }
                offset -= shred.getPrefix().length();
                if (offset < 0) {
                    return preferLeftFragment ? prevEnd : currentStart - 1;
                }
                if (offset == 0) {
                    return preferLeftFragment && i != 0 ? prevEnd : currentStart;
                }
                int length = currentEnd - currentStart;
                if (offset < length || offset == length && preferLeftFragment) {
                    return currentStart + offset;
                }
                offset -= length;
                offset -= shred.getSuffix().length();
                prevEnd = currentEnd;
            }
            Segment hostRangeMarker = myShreds.get(myShreds.size() - 1).getHostRangeMarker();
            return hostRangeMarker == null ? 0 : hostRangeMarker.getEndOffset();
        }
    }

    @Override
    @Nonnull
    public ProperTextRange injectedToHost(@Nonnull TextRange injected) {
        int start = injectedToHost(injected.getStartOffset(), false, true);
        int end = injectedToHost(injected.getEndOffset(), true, true);
        if (end < start) {
            end = injectedToHost(injected.getEndOffset(), false, true);
        }
        return new ProperTextRange(start, end);
    }

    @Override
    @RequiredReadAction
    public int injectedToHostLine(int line) {
        if (line < myPrefixLineCount) {
            synchronized (myLock) {
                Segment hostRangeMarker = myShreds.get(0).getHostRangeMarker();
                return hostRangeMarker == null ? 0 : myDelegate.getLineNumber(hostRangeMarker.getStartOffset());
            }
        }
        int lineCount = getLineCount();
        if (line > lineCount - mySuffixLineCount) {
            return lineCount;
        }
        int offset = getLineStartOffset(line);
        int hostOffset = injectedToHost(offset);

        return myDelegate.getLineNumber(hostOffset);
    }

    @Override
    public boolean containsRange(int hostStart, int hostEnd) {
        synchronized (myLock) {
            ProperTextRange query = new ProperTextRange(hostStart, hostEnd);
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                Segment hostRange = shred.getHostRangeMarker();
                if (hostRange == null) {
                    continue;
                }
                TextRange textRange = ProperTextRange.create(hostRange);
                if (textRange.contains(query)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @deprecated Use {@link InjectedLanguageManager#intersectWithAllEditableFragments(PsiFile, TextRange)} instead
     */
    @Deprecated
    @Nullable
    private TextRange intersectWithEditable(@Nonnull TextRange rangeToEdit) {
        int startOffset = -1;
        int endOffset = -1;
        synchronized (myLock) {
            int offset = 0;
            for (PsiLanguageInjectionHost.Shred shred : myShreds) {
                Segment hostRange = shred.getHostRangeMarker();
                if (hostRange == null) {
                    continue;
                }
                offset += shred.getPrefix().length();
                int length = hostRange.getEndOffset() - hostRange.getStartOffset();
                TextRange intersection = new ProperTextRange(offset, offset + length).intersection(rangeToEdit);
                if (intersection != null) {
                    if (startOffset == -1) {
                        startOffset = intersection.getStartOffset();
                    }
                    endOffset = intersection.getEndOffset();
                }
                offset += length;
                offset += shred.getSuffix().length();
            }
        }
        if (startOffset == -1) {
            return null;
        }
        return new ProperTextRange(startOffset, endOffset);
    }

    // minimum sequence of text replacement operations for each host range
    // result[i] == null means no change
    // result[i] == "" means delete
    // result[i] == string means replace
    @Nonnull
    String[] calculateMinEditSequence(String newText) {
        synchronized (myLock) {
            String[] result = new String[myShreds.size()];
            String hostText = myDelegate.getText();
            calculateMinEditSequence(hostText, newText, result, 0, result.length - 1);
            for (int i = 0; i < result.length; i++) {
                String change = result[i];
                if (change == null) {
                    continue;
                }
                String prefix = myShreds.get(i).getPrefix();
                String suffix = myShreds.get(i).getSuffix();
                assert change.startsWith(prefix) : change + "/" + prefix;
                assert change.endsWith(suffix) : change + "/" + suffix;
                result[i] = StringUtil.trimEnd(StringUtil.trimStart(change, prefix), suffix);
            }
            return result;
        }
    }

    private String getRangeText(@Nonnull String hostText, int hostNum) {
        synchronized (myLock) {
            PsiLanguageInjectionHost.Shred shred = myShreds.get(hostNum);
            Segment hostRangeMarker = shred.getHostRangeMarker();
            return shred.getPrefix() + (
                hostRangeMarker == null
                    ? ""
                    : hostText.substring(hostRangeMarker.getStartOffset(), hostRangeMarker.getEndOffset())
            ) + shred.getSuffix();
        }
    }

    private void calculateMinEditSequence(String hostText, String newText, String[] result, int i, int j) {
        synchronized (myLock) {
            String rangeText1 = getRangeText(hostText, i);
            if (i == j) {
                result[i] = rangeText1.equals(newText) ? null : newText;
                return;
            }
            if (StringUtil.startsWith(newText, rangeText1)) {
                result[i] = null;  //no change
                calculateMinEditSequence(hostText, newText.substring(rangeText1.length()), result, i + 1, j);
                return;
            }
            String rangeText2 = getRangeText(hostText, j);
            if (StringUtil.endsWith(newText, rangeText2)) {
                result[j] = null;  //no change
                calculateMinEditSequence(hostText, newText.substring(0, newText.length() - rangeText2.length()), result, i, j - 1);
                return;
            }
            if (i + 1 == j) {
                String suffix = myShreds.get(i).getSuffix();
                String prefix = myShreds.get(j).getPrefix();
                String separator = suffix + prefix;
                if (!separator.isEmpty()) {
                    int sep = newText.indexOf(separator);
                    assert sep != -1;
                    result[i] = newText.substring(0, sep + suffix.length());
                    result[j] = newText.substring(sep + suffix.length() + prefix.length());
                    return;
                }
                String commonPrefix = StringUtil.commonPrefix(rangeText1, newText);
                result[i] = commonPrefix;
                result[j] = newText.substring(commonPrefix.length());
                return;
            }
            String middleText = getRangeText(hostText, i + 1);
            int m = newText.indexOf(middleText);
            if (m != -1) {
                result[i] = newText.substring(0, m);
                result[i + 1] = null;
                calculateMinEditSequence(hostText, newText.substring(m + middleText.length()), result, i + 2, j);
                return;
            }
            middleText = getRangeText(hostText, j - 1);
            m = newText.lastIndexOf(middleText);
            if (m != -1) {
                result[j] = newText.substring(m + middleText.length());
                result[j - 1] = null;
                calculateMinEditSequence(hostText, newText.substring(0, m), result, i, j - 2);
                return;
            }
            result[i] = "";
            result[j] = "";
            calculateMinEditSequence(hostText, newText, result, i + 1, j - 1);
        }
    }

    @Override
    public boolean areRangesEqual(@Nonnull DocumentWindow other) {
        DocumentWindowImpl window = (DocumentWindowImpl)other;
        PlaceImpl shreds = getShreds();
        PlaceImpl otherShreds = window.getShreds();
        if (shreds.size() != otherShreds.size()) {
            return false;
        }
        for (int i = 0; i < shreds.size(); i++) {
            PsiLanguageInjectionHost.Shred shred = shreds.get(i);
            PsiLanguageInjectionHost.Shred otherShred = otherShreds.get(i);
            if (!shred.getPrefix().equals(otherShred.getPrefix())) {
                return false;
            }
            if (!shred.getSuffix().equals(otherShred.getSuffix())) {
                return false;
            }

            Segment hostRange = shred.getHostRangeMarker();
            Segment otherRange = otherShred.getHostRangeMarker();
            if (hostRange == null || otherRange == null || !TextRange.areSegmentsEqual(hostRange, otherRange)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        PlaceImpl shreds;
        synchronized (myLock) {
            shreds = myShreds; // assumption: myShreds list is immutable
        }
        // can grab PsiLock in SmartPsiPointer.restore()
        // will check the 0th element manually (to avoid getting .getHost() twice)
        for (int i = 1; i < shreds.size(); i++) {
            PsiLanguageInjectionHost.Shred shred = shreds.get(i);
            if (!shred.isValid()) {
                return false;
            }
        }

        PsiLanguageInjectionHost.Shred firstShred = shreds.get(0);
        PsiLanguageInjectionHost host = firstShred.getHost();
        if (host == null || firstShred.getHostRangeMarker() == null) {
            return false;
        }
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(this);
        return virtualFile != null && ((PsiManagerEx)host.getManager()).getFileManager().findCachedViewProvider(virtualFile) != null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DocumentWindowImpl)) {
            return false;
        }
        DocumentWindowImpl window = (DocumentWindowImpl)o;
        return myDelegate.equals(window.getDelegate()) && areRangesEqual(window);
    }

    @Override
    public int hashCode() {
        synchronized (myLock) {
            Segment hostRangeMarker = myShreds.get(0).getHostRangeMarker();
            return hostRangeMarker == null ? -1 : hostRangeMarker.getStartOffset();
        }
    }

    @Override
    public boolean isOneLine() {
        return myOneLine;
    }

    @Override
    public void dispose() {
        synchronized (myLock) {
            myShreds.dispose();
        }
    }

    void setShreds(@Nonnull PlaceImpl shreds) {
        synchronized (myLock) {
            myShreds.dispose();
            myShreds = shreds;
        }
    }

    @Nonnull
    PlaceImpl getShreds() {
        synchronized (myLock) {
            return myShreds;
        }
    }

    //todo convert injected RMs to host
    @Override
    public boolean processRangeMarkers(@Nonnull Predicate<? super RangeMarker> processor) {
        return myDelegate.processRangeMarkers(processor);
    }

    @Override
    public boolean processRangeMarkersOverlappingWith(int start, int end, @Nonnull Predicate<? super RangeMarker> processor) {
        return myDelegate.processRangeMarkersOverlappingWith(start, end, processor);
    }
}
