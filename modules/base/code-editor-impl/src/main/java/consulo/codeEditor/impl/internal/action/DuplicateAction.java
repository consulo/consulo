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
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.codeEditor.util.EditorUtil;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.Presentation;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 * @since 2002-05-14
 */
@ActionImpl(id = "EditorDuplicate")
public class DuplicateAction extends EditorAction {
    public DuplicateAction() {
        super(ActionLocalize.actionEditorduplicateText(), new Handler());
    }

    @Override
    public int getExecuteWeight() {
        return 2_000_000;
    }
    
    private static class Handler extends EditorWriteActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            duplicateLineOrSelectedBlockAtCaret(editor);
        }

        @Override
        public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
            return !editor.isOneLineMode() || editor.getSelectionModel().hasSelection();
        }
    }

    private static void duplicateLineOrSelectedBlockAtCaret(Editor editor) {
        Document document = editor.getDocument();
        CaretModel caretModel = editor.getCaretModel();
        ScrollingModel scrollingModel = editor.getScrollingModel();
        if (editor.getSelectionModel().hasSelection()) {
            int start = editor.getSelectionModel().getSelectionStart();
            int end = editor.getSelectionModel().getSelectionEnd();
            String s = document.getCharsSequence().subSequence(start, end).toString();
            document.insertString(end, s);
            caretModel.moveToOffset(end + s.length());
            scrollingModel.scrollToCaret(ScrollType.RELATIVE);
            editor.getSelectionModel().removeSelection();
            editor.getSelectionModel().setSelection(end, end + s.length());
        }
        else {
            duplicateLinesRange(editor, document, caretModel.getVisualPosition(), caretModel.getVisualPosition());
        }
    }

    @Nullable
    static Couple<Integer> duplicateLinesRange(
        Editor editor,
        Document document,
        VisualPosition rangeStart,
        VisualPosition rangeEnd
    ) {
        Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, rangeStart, rangeEnd);
        int offset = editor.getCaretModel().getOffset();

        LogicalPosition lineStart = lines.first;
        LogicalPosition nextLineStart = lines.second;
        int start = editor.logicalPositionToOffset(lineStart);
        int end = editor.logicalPositionToOffset(nextLineStart);
        if (end <= start) {
            return null;
        }
        String s = document.getCharsSequence().subSequence(start, end).toString();
        int lineToCheck = nextLineStart.line - 1;

        int newOffset = end + offset - start;
        if (lineToCheck == document.getLineCount() /* empty document */
            || lineStart.line == document.getLineCount() - 1 /* last line*/
            || document.getLineSeparatorLength(lineToCheck) == 0) {
            s = "\n" + s;
            newOffset++;
        }
        document.insertString(end, s);

        editor.getCaretModel().moveToOffset(newOffset);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        return Couple.of(end, end + s.length() - 1);   // don't include separator of last line in range to select
    }

    @Override
    public void update(Editor editor, Presentation presentation, DataContext dataContext) {
        super.update(editor, presentation, dataContext);
        presentation.setTextValue(
            editor.getSelectionModel().hasSelection()
                ? CodeEditorLocalize.actionDuplicateSelection()
                : CodeEditorLocalize.actionDuplicateLine()
        );
    }
}
