/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.codeEditor.imaginary;

import consulo.codeEditor.*;
import consulo.document.util.TextRange;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2025-03-07
 */
public class ImaginaryCaret extends UserDataHolderBase implements Caret {
    private final ImaginaryCaretModel myCaretModel;
    private int myStart;
    private int myPos;
    private int myEnd;

    public ImaginaryCaret(ImaginaryCaretModel caretModel) {
        myCaretModel = caretModel;
    }

    private RuntimeException notImplemented() {
        return myCaretModel.getEditor().notImplemented();
    }

    @Override
    public Editor getEditor() {
        return myCaretModel.getEditor();
    }

    @Override
    public CaretModel getCaretModel() {
        return myCaretModel;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public int getOffset() {
        return myPos;
    }

    @Override
    public void moveToOffset(int offset) {
        moveToOffset(offset, false);
    }

    @Override
    public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
        if (offset < 0) {
            offset = 0;
        }
        myStart = myPos = myEnd = offset;
    }

    @Override
    public void moveToLogicalPosition(LogicalPosition pos) {
        moveToOffset(myCaretModel.getEditor().logicalPositionToOffset(pos));
    }

    @Override
    public void moveToVisualPosition(VisualPosition pos) {
        moveToOffset(myCaretModel.getEditor().visualPositionToOffset(pos));
    }

    @Override
    public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean scrollToCaret) {
        if (lineShift == 0) {
            myEnd += columnShift;
            if (!withSelection) {
                myStart = myPos = myEnd;
            }
        }
        else {
            int oldPos = myPos;
            LogicalPosition currentPosition = getLogicalPosition();
            moveToLogicalPosition(new LogicalPosition(currentPosition.line + lineShift, currentPosition.column + columnShift));
            if (withSelection) {
                int newPos = myPos;
                myStart = Math.min(oldPos, newPos);
                myEnd = Math.max(oldPos, newPos);
            }
        }
    }

    @Override
    public boolean isUpToDate() {
        return true;
    }

    @Override
    public LogicalPosition getLogicalPosition() {
        return myCaretModel.getEditor().offsetToLogicalPosition(myPos);
    }

    @Override
    public VisualPosition getVisualPosition() {
        return myCaretModel.getEditor().offsetToVisualPosition(myPos);
    }

    @Override
    public int getVisualLineStart() {
        throw notImplemented();
    }

    @Override
    public int getVisualLineEnd() {
        throw notImplemented();
    }

    @Override
    public int getSelectionStart() {
        return myStart;
    }

    @Override
    public VisualPosition getSelectionStartPosition() {
        return myCaretModel.getEditor().offsetToVisualPosition(myStart);
    }

    @Override
    public int getSelectionEnd() {
        return myEnd;
    }

    @Override
    public VisualPosition getSelectionEndPosition() {
        return myCaretModel.getEditor().offsetToVisualPosition(myEnd);
    }

    @Override
    public @Nullable String getSelectedText() {
        if (!hasSelection()) {
            return null;
        }
        return myCaretModel.getEditor().getDocument().getText(new TextRange(myStart, myEnd));
    }

    @Override
    public int getLeadSelectionOffset() {
        return getOffset();
    }

    @Override
    public VisualPosition getLeadSelectionPosition() {
        throw notImplemented();
    }

    @Override
    public boolean hasSelection() {
        return myEnd > myStart;
    }

    @Override
    public void setSelection(int startOffset, int endOffset) {
        if (startOffset < 0) {
            startOffset = 0;
        }
        if (endOffset < 0) {
            endOffset = 0;
        }
        // mimicking CaretImpl's doSetSelection: removing selection if startOffset == endOffset
        if (startOffset == endOffset) {
            myStart = myPos;
            myEnd = myPos;
            return;
        }
        if (startOffset > endOffset) {
            myStart = endOffset;
            myEnd = startOffset;
        }
        else {
            myStart = startOffset;
            myEnd = endOffset;
        }
        if (myPos < myStart) {
            myPos = myStart;
        }
        else if (myPos > myEnd) {
            myPos = myEnd;
        }
    }

    @Override
    public void setSelection(int startOffset, int endOffset, boolean updateSystemSelection) {
        setSelection(startOffset, endOffset);
    }

    @Override
    public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
        throw notImplemented();
    }

    @Override
    public void setSelection(@Nullable VisualPosition startPosition, int startOffset,
                             @Nullable VisualPosition endPosition, int endOffset) {
        throw notImplemented();
    }

    @Override
    public void setSelection(@Nullable VisualPosition startPosition, int startOffset,
                             @Nullable VisualPosition endPosition, int endOffset,
                             boolean updateSystemSelection) {
        throw notImplemented();
    }

    @Override
    public void removeSelection() {
        myStart = myPos;
        myEnd = myPos;
    }

    @Override
    public void selectLineAtCaret() {
        throw notImplemented();
    }

    @Override
    public void selectWordAtCaret(boolean honorCamelWordsSettings) {
        throw notImplemented();
    }

    @Override
    public @Nullable Caret clone(boolean above) {
        throw notImplemented();
    }

    @Override
    public boolean isAtRtlLocation() {
        throw notImplemented();
    }

    @Override
    public boolean isAtBidiRunBoundary() {
        throw notImplemented();
    }

    @Override
    public CaretVisualAttributes getVisualAttributes() {
        throw notImplemented();
    }

    @Override
    public void setVisualAttributes(CaretVisualAttributes attributes) {
        throw notImplemented();
    }

    @Override
    public void dispose() {
    }
}
