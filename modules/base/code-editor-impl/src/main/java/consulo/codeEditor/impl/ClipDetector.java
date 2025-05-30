// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl;

import consulo.codeEditor.FoldingModel;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.util.EditorUtil;
import consulo.document.Document;

import java.awt.*;

/**
 * Allows performing clipping checks for painting in the editor.
 * Using this class will be faster than direct calculations if a lot of checks need to be performed in one painting sessions, and
 * requests are mostly grouped by visual lines, as caching of intermediate data is performed.
 */
public final class ClipDetector {
    private final RealEditor myEditor;
    private final Document myDocument;
    private final FoldingModel myFoldingModel;
    private final Rectangle myClipRectangle;
    private final boolean myDisabled;

    private int myVisualLineStartOffset = -1;
    private int myVisualLineEndOffset = -1;
    private int myVisualLineClipStartOffset;
    private int myVisualLineClipEndOffset;

    public ClipDetector(
        RealEditor editor,
        Document document,
        FoldingModel foldingModel,
        Rectangle clipRectangle,
        boolean isDisabled
    ) {
        myEditor = editor;
        myDocument = document;
        myFoldingModel = foldingModel;
        myClipRectangle = clipRectangle;
        myDisabled = isDisabled;
    }

    public boolean rangeCanBeVisible(int startOffset, int endOffset) {
        assert startOffset >= 0;
        assert startOffset <= endOffset;
        assert endOffset <= myDocument.getTextLength();
        if (myDisabled) return true;
        if (startOffset < myVisualLineStartOffset || startOffset > myVisualLineEndOffset) {
            myVisualLineStartOffset = EditorUtil.getNotFoldedLineStartOffset(myDocument, myFoldingModel, startOffset, false);
            myVisualLineEndOffset = EditorUtil.getNotFoldedLineEndOffset(myDocument, myFoldingModel, startOffset, false);
            int visualLine = myEditor.offsetToVisualLine(startOffset, false);
            int y = myEditor.visualLineToY(visualLine);
            myVisualLineClipStartOffset = myEditor.logicalPositionToOffset(myEditor.xyToLogicalPosition(new Point(myClipRectangle.x, y)));
            myVisualLineClipEndOffset = myEditor.logicalPositionToOffset(myEditor.xyToLogicalPosition(new Point(myClipRectangle.x + myClipRectangle.width, y)));
        }
        return endOffset > myVisualLineEndOffset || startOffset <= myVisualLineClipEndOffset && endOffset >= myVisualLineClipStartOffset;
    }
}
