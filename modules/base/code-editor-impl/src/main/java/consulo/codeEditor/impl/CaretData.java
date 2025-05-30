// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl;

import consulo.codeEditor.Caret;
import consulo.codeEditor.CaretModel;
import consulo.document.Document;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;

import java.util.List;

public final class CaretData {

    private static final CaretData NULL_CARET = new CaretData(-1, -1, ArrayUtil.EMPTY_INT_ARRAY, ArrayUtil.EMPTY_INT_ARRAY);

    public static @Nonnull CaretData createCaretData(@Nonnull Document document, @Nonnull CaretModel caretModel) {
        int caretRowStart = caretModel.getVisualLineStart();
        int caretRowEnd = caretModel.getVisualLineEnd();
        if (caretRowEnd == document.getTextLength() &&
            document.getLineCount() > 0 &&
            caretRowEnd > document.getLineStartOffset(document.getLineCount() - 1)) {
            caretRowEnd++;
        }
        List<Caret> carets = caretModel.getAllCarets();
        int caretCount = carets.size();
        int[] selectionStarts = new int[caretCount];
        int[] selectionEnds = new int[caretCount];
        for (int i = 0; i < caretCount; i++) {
            Caret caret = carets.get(i);
            selectionStarts[i] = caret.getSelectionStart();
            selectionEnds[i] = caret.getSelectionEnd();
        }
        return new CaretData(caretRowStart, caretRowEnd, selectionStarts, selectionEnds);
    }

    public static @Nonnull CaretData getNullCaret() {
        return NULL_CARET;
    }

    public static CaretData copyOf(CaretData original, boolean omitCaretRowData) {
        if (original == null || !omitCaretRowData) {
            return original;
        }
        else {
            return new CaretData(-1, -1, original.selectionStarts, original.selectionEnds);
        }
    }

    private final int caretRowStart;
    private final int caretRowEnd;
    private final int[] selectionStarts;
    private final int[] selectionEnds;

    CaretData(
        int caretRowStart,
        int caretRowEnd,
        int[] selectionStarts,
        int[] selectionEnds
    ) {
        this.caretRowStart = caretRowStart;
        this.caretRowEnd = caretRowEnd;
        this.selectionStarts = selectionStarts;
        this.selectionEnds = selectionEnds;
    }

    int caretRowStart() {
        return caretRowStart;
    }

    int caretRowEnd() {
        return caretRowEnd;
    }

    int selectionsSize() {
        return selectionStarts.length;
    }

    int selectionStart(int index, boolean reverse) {
        return selectionStarts[reverse ? selectionStarts.length - 1 - index : index];
    }

    int selectionEnd(int index, boolean reverse) {
        return selectionEnds[reverse ? selectionStarts.length - 1 - index : index];
    }
}
