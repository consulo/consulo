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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.action.EditorAction;
import consulo.project.Project;
import consulo.document.util.TextRange;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class SelectAllOccurrencesAction extends EditorAction {
    public SelectAllOccurrencesAction() {
        super(new Handler());
    }

    private static class Handler extends SelectOccurrencesActionHandler {
        @Override
        public boolean isEnabled(Editor editor, DataContext dataContext) {
            return super.isEnabled(editor, dataContext) && editor.getProject() != null && editor.getCaretModel().supportsMultipleCarets();
        }

        @Override
        public void doExecute(@Nonnull Editor editor, @Nullable Caret c, DataContext dataContext) {
            Caret caret = c == null ? editor.getCaretModel().getPrimaryCaret() : c;

            if (!caret.hasSelection()) {
                TextRange wordSelectionRange = getSelectionRange(editor, caret);
                if (wordSelectionRange != null) {
                    setSelection(editor, caret, wordSelectionRange);
                }
            }

            String selectedText = caret.getSelectedText();
            Project project = editor.getProject();
            if (project == null || selectedText == null) {
                return;
            }

            int caretShiftFromSelectionStart = caret.getOffset() - caret.getSelectionStart();
            FindManager findManager = FindManager.getInstance(project);

            FindModel model = new FindModel();
            model.setStringToFind(selectedText);
            model.setCaseSensitive(true);
            model.setWholeWordsOnly(true);

            int searchStartOffset = 0;
            FindResult findResult = findManager.findString(editor.getDocument().getCharsSequence(), searchStartOffset, model);
            while (findResult.isStringFound()) {
                int newCaretOffset = caretShiftFromSelectionStart + findResult.getStartOffset();
                EditorActionUtil.makePositionVisible(editor, newCaretOffset);
                Caret newCaret = editor.getCaretModel().addCaret(editor.offsetToVisualPosition(newCaretOffset));
                if (newCaret != null) {
                    setSelection(editor, newCaret, findResult);
                }
                findResult = findManager.findString(editor.getDocument().getCharsSequence(), findResult.getEndOffset(), model);
            }
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
    }
}
