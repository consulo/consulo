/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.util.EditorUtil;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.CommandProcessor;

import java.util.Collections;
import java.util.List;

@ActionImpl(id = IdeActions.ACTION_EDITOR_DELETE_LINE)
public class DeleteLineAction extends TextComponentEditorAction {
    private static class Handler extends EditorWriteActionHandler {
        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
            CopyPasteManager.getInstance().stopKillRings();
            Document document = editor.getDocument();

            List<Caret> carets = caret == null ? editor.getCaretModel().getAllCarets() : Collections.singletonList(caret);

            editor.getCaretModel().runBatchCaretOperation(() -> {
                int[] caretColumns = new int[carets.size()];
                int caretIndex = carets.size() - 1;
                TextRange range = getRangeToDelete(editor, carets.get(caretIndex));

                while (caretIndex >= 0) {
                    int currentCaretIndex = caretIndex;
                    TextRange currentRange = range;
                    // find carets with overlapping line ranges
                    while (--caretIndex >= 0) {
                        range = getRangeToDelete(editor, carets.get(caretIndex));
                        if (range.getEndOffset() < currentRange.getStartOffset()) {
                            break;
                        }
                        currentRange = new TextRange(range.getStartOffset(), currentRange.getEndOffset());
                    }

                    for (int i = caretIndex + 1; i <= currentCaretIndex; i++) {
                        caretColumns[i] = carets.get(i).getVisualPosition().column;
                    }
                    int targetLine = editor.offsetToVisualPosition(currentRange.getStartOffset()).line;

                    document.deleteString(currentRange.getStartOffset(), currentRange.getEndOffset());

                    for (int i = caretIndex + 1; i <= currentCaretIndex; i++) {
                        carets.get(i).moveToVisualPosition(new VisualPosition(targetLine, caretColumns[i]));
                    }
                }
            });
        }
    }

    public DeleteLineAction() {
        super(ActionLocalize.actionEditordeletelineText(), new Handler());
    }

    private static TextRange getRangeToDelete(Editor editor, Caret caret) {
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        int startOffset = EditorUtil.getNotFoldedLineStartOffset(editor, selectionStart);
        // There is a possible case that selection ends at the line start, i.e. something like below ([...] denotes selected text,
        // '|' is a line start):
        //   |line 1
        //   |[line 2
        //   |]line 3
        // We don't want to delete line 3 here. However, the situation below is different:
        //   |line 1
        //   |[line 2
        //   |line] 3
        // Line 3 must be removed here.
        int endOffset = EditorUtil.getNotFoldedLineEndOffset(
            editor,
            selectionEnd > 0 && selectionEnd != selectionStart ? selectionEnd - 1 : selectionEnd
        );
        if (endOffset < editor.getDocument().getTextLength()) {
            endOffset++;
        }
        else if (startOffset > 0) {
            startOffset--;
        }
        return new TextRange(startOffset, endOffset);
    }
}
