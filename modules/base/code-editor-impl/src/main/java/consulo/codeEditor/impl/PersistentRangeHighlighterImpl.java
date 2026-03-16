// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl;

import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.event.DocumentEvent;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of the markup element for the editor and document.
 */
public final class PersistentRangeHighlighterImpl extends RangeHighlighterImpl {
    // temporary fields, to investigate exception
    short prevStartOffset;
    short prevEndOffset;
    byte modificationStamp;

    static PersistentRangeHighlighterImpl create(MarkupModelImpl model,
                                                          int offset,
                                                          int layer,
                                                          HighlighterTargetArea target,
                                                          @Nullable TextAttributesKey textAttributesKey,
                                                          boolean normalizeStartOffset) {
        int line = model.getDocument().getLineNumber(offset);
        int startOffset = normalizeStartOffset ? model.getDocument().getLineStartOffset(line) : offset;
        int endOffset = model.getDocument().getLineEndOffset(line);
        return new PersistentRangeHighlighterImpl(model, startOffset, endOffset, layer, target, textAttributesKey);
    }

    public short getPrevStartOffset() {
        return prevStartOffset;
    }

    public short getPrevEndOffset() {
        return prevEndOffset;
    }

    public byte getModificationStamp() {
        return modificationStamp;
    }

    private PersistentRangeHighlighterImpl(MarkupModelImpl model,
                                           int startOffset,
                                           int endOffset,
                                           int layer,
                                           HighlighterTargetArea target,
                                           @Nullable TextAttributesKey textAttributesKey) {
        super(model, startOffset, endOffset, layer, target, textAttributesKey, false, false);
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    protected void changedUpdateImpl(DocumentEvent e) {
        prevStartOffset = (short) intervalStart();
        prevEndOffset = (short) intervalEnd();
        modificationStamp = (byte) e.getDocument().getModificationStamp();
        persistentHighlighterUpdate(e, getTargetArea() == HighlighterTargetArea.LINES_IN_RANGE);
    }

    @Override
    public String toString() {
        return "PersistentRangeHighlighter" +
            (isGreedyToLeft() ? "[" : "(") +
            (isValid() ? "valid" : "invalid") + "," +
            (getTargetArea() == HighlighterTargetArea.LINES_IN_RANGE ? "whole-line" : "exact") + "," +
            getStartOffset() + "," + getEndOffset() +
            (isGreedyToRight() ? "]" : ")");
    }
}
