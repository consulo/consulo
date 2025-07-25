/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.codeEditor.impl.internal.action;

import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.util.lang.CharArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class CutLineActionHandler extends EditorWriteActionHandler {
    private final boolean myToLineStart;
    private final boolean myIgnoreSelection;
    private final boolean myCopyToClipboard;

    CutLineActionHandler(boolean toLineStart, boolean ignoreSelection, boolean copyToClipboard) {
        super(!copyToClipboard); // as CutLineEndAction interacts with clipboard, multi-caret support for it needs to be implemented explicitly (todo)
        myToLineStart = toLineStart;
        myIgnoreSelection = ignoreSelection;
        myCopyToClipboard = copyToClipboard;
    }

    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (caret == null) {
            caret = editor.getCaretModel().getCurrentCaret();
        }
        if (!myIgnoreSelection && caret.hasSelection()) {
            delete(editor, caret, caret.getSelectionStart(), caret.getSelectionEnd());
            return;
        }

        Document doc = editor.getDocument();
        int caretOffset = caret.getOffset();
        if ((myToLineStart && (caretOffset == 0)) || (!myToLineStart && (caretOffset >= doc.getTextLength()))) {
            return;
        }
        int lineNumber = doc.getLineNumber(caretOffset);
        int lineEndOffset = doc.getLineEndOffset(lineNumber);
        int lineStartOffset = doc.getLineStartOffset(lineNumber);

        if (editor.isColumnMode() && editor.getCaretModel().supportsMultipleCarets()
            && caretOffset == (myToLineStart ? lineStartOffset : lineEndOffset)) {
            return;
        }

        int start;
        int end;
        if (myToLineStart) {
            start = lineStartOffset;
            end = caretOffset;
        }
        else {
            if (caretOffset >= lineEndOffset) {
                start = lineEndOffset;
                end = lineEndOffset + 1;
            }
            else {
                start = caretOffset;
                end = lineEndOffset;
                if (lineEndOffset < doc.getTextLength() && CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), caretOffset, lineEndOffset)) {
                    end++;
                }
            }
        }

        delete(editor, caret, start, end);
    }

    private void delete(@Nonnull Editor editor, @Nonnull Caret caret, int start, int end) {
        if (myCopyToClipboard) {
            KillRingUtil.copyToKillRing(editor, start, end, true);
        }
        else {
            CopyPasteManager.getInstance().stopKillRings();
        }
        editor.getDocument().deleteString(start, end);

        // in case the caret was in the virtual space, we force it to go back to the real offset
        caret.moveToOffset(start);
    }
}
