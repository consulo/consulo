/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.diff.util;

import java.util.ArrayList;
import java.util.List;

public final class DiffRangeUtil {
    public static CharSequence getLinesContent(CharSequence sequence, LineOffsets lineOffsets, int line1, int line2) {
        return getLinesContent(sequence, lineOffsets, line1, line2, false);
    }

    public static CharSequence getLinesContent(CharSequence sequence,
                                               LineOffsets lineOffsets,
                                               int line1,
                                               int line2,
                                               boolean includeNewline) {
        if (sequence.length() != lineOffsets.getTextLength()) {
            throw new IllegalStateException();
        }
        LinesRange linesRange = getLinesRange(lineOffsets, line1, line2, includeNewline);
        return sequence.subSequence(linesRange.startOffset, linesRange.endOffset);
    }

    public static LinesRange getLinesRange(LineOffsets lineOffsets, int line1, int line2, boolean includeNewline) {
        if (line1 == line2) {
            int lineStartOffset = line1 < lineOffsets.getLineCount() ? lineOffsets.getLineStart(line1) : lineOffsets.getTextLength();
            return new LinesRange(lineStartOffset, lineStartOffset);
        }
        else {
            int startOffset = lineOffsets.getLineStart(line1);
            int endOffset = lineOffsets.getLineEnd(line2 - 1);
            if (includeNewline && endOffset < lineOffsets.getTextLength()) endOffset++;
            return new LinesRange(startOffset, endOffset);
        }
    }

    public static List<String> getLines(CharSequence text, LineOffsets lineOffsets) {
        return getLines(text, lineOffsets, 0, lineOffsets.getLineCount());
    }

    public static List<String> getLines(CharSequence text, LineOffsets lineOffsets, int startLine, int endLine) {
        if (startLine < 0 || startLine > endLine || endLine > lineOffsets.getLineCount()) {
            throw new IndexOutOfBoundsException("Wrong line range: [" + startLine + ", " + endLine + "); lineCount: '" + lineOffsets.getLineCount() + "'");
        }

        List<String> result = new ArrayList<>();
        for (int i = startLine; i < endLine; i++) {
            int start = lineOffsets.getLineStart(i);
            int end = lineOffsets.getLineEnd(i);
            result.add(text.subSequence(start, end).toString());
        }
        return result;
    }

    public static final class LinesRange {
        public final int startOffset;
        public final int endOffset;

        public LinesRange(int startOffset, int endOffset) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
}
