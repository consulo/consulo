// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.document.impl;

import consulo.document.event.DocumentEvent;
import consulo.document.impl.event.DocumentEventImpl;
import consulo.document.util.DocumentEventUtil;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.document.util.TextRangeScalarUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A range marker that has to be manually updated with {@link #getUpdatedRange(DocumentEvent, FrozenDocument)}.
 * Can hold PSI-based range and be updated when the document is committed.
 */
public class ManualRangeMarker implements Segment {
    private final boolean myGreedyLeft;
    private final boolean myGreedyRight;
    private final boolean mySurviveOnExternalChange;
    private final PersistentRangeMarker.LinesCols myLinesCols;
    private final long myRange;

    public ManualRangeMarker(int start, int end,
                             boolean greedyLeft,
                             boolean greedyRight,
                             boolean surviveOnExternalChange,
                             @Nullable PersistentRangeMarker.LinesCols linesCols) {
        myRange = TextRangeScalarUtil.toScalarRange(start, end);
        myGreedyLeft = greedyLeft;
        myGreedyRight = greedyRight;
        mySurviveOnExternalChange = surviveOnExternalChange;
        myLinesCols = linesCols;
    }

    public @Nullable ManualRangeMarker getUpdatedRange(@Nonnull DocumentEvent event, @Nonnull FrozenDocument documentBefore) {
        if (mySurviveOnExternalChange && PersistentRangeMarkerUtil.shouldTranslateViaDiff(event, myRange)) {
            PersistentRangeMarker.LinesCols linesCols = myLinesCols != null ? myLinesCols
                : PersistentRangeMarker.storeLinesAndCols(documentBefore, myRange);
            Pair<TextRange, PersistentRangeMarker.LinesCols> pair =
                linesCols == null ? null : PersistentRangeMarker.translateViaDiff((DocumentEventImpl) event, linesCols);
            if (pair != null) {
                return new ManualRangeMarker(pair.first.getStartOffset(), pair.first.getEndOffset(), myGreedyLeft, myGreedyRight, true, pair.second);
            }
        }

        long newRange = RangeMarkerImpl.applyChange(event, myRange, myGreedyLeft, myGreedyRight, false);
        if (newRange == -1) return null;

        int delta = 0;
        if (DocumentEventUtil.isMoveInsertion(event)) {
            int srcOffset = event.getMoveOffset();
            if (srcOffset <= TextRangeScalarUtil.startOffset(newRange) && TextRangeScalarUtil.endOffset(newRange) <= srcOffset + event.getNewLength()) {
                delta = event.getOffset() - srcOffset;
            }
        }
        return new ManualRangeMarker(TextRangeScalarUtil.startOffset(newRange) + delta, TextRangeScalarUtil.endOffset(newRange) + delta, myGreedyLeft, myGreedyRight,
            mySurviveOnExternalChange, null);
    }

    @Override
    public int getStartOffset() {
        return TextRangeScalarUtil.startOffset(myRange);
    }

    @Override
    public int getEndOffset() {
        return TextRangeScalarUtil.endOffset(myRange);
    }

    @Override
    public String toString() {
        return "ManualRangeMarker " + TextRange.create(this);
    }

}
