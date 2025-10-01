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
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.CommandProcessor;

/**
 * @author max
 * @since 2002-05-14
 */
@ActionImpl(id = IdeActions.ACTION_EDITOR_DELETE_TO_WORD_END)
public class DeleteToWordEndAction extends TextComponentEditorAction {
    static class Handler extends EditorWriteActionHandler {
        private final boolean myNegateCamelMode;

        Handler(boolean negateCamelMode) {
            super(true);
            myNegateCamelMode = negateCamelMode;
        }

        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
            CopyPasteManager.getInstance().stopKillRings();

            int lineNumber = editor.getCaretModel().getLogicalPosition().line;
            if (editor.isColumnMode() && editor.getCaretModel().supportsMultipleCarets()
                && editor.getCaretModel().getOffset() == editor.getDocument().getLineEndOffset(lineNumber)) {
                return;
            }

            boolean camelMode = editor.getSettings().isCamelWords();
            if (myNegateCamelMode) {
                camelMode = !camelMode;
            }
            deleteToWordEnd(editor, camelMode);
        }
    }

    public DeleteToWordEndAction() {
        super(CodeEditorLocalize.actionDeleteToWordEndText(), new Handler(false));
    }

    private static void deleteToWordEnd(Editor editor, boolean camelMode) {
        int startOffset = editor.getCaretModel().getOffset();
        int endOffset = getWordEndOffset(editor, startOffset, camelMode);
        if (endOffset > startOffset) {
            Document document = editor.getDocument();
            document.deleteString(startOffset, endOffset);
        }
    }

    private static int getWordEndOffset(Editor editor, int offset, boolean camelMode) {
        Document document = editor.getDocument();
        CharSequence text = document.getCharsSequence();
        if (offset >= document.getTextLength() - 1) {
            return offset;
        }
        int newOffset = offset + 1;
        int lineNumber = editor.getCaretModel().getLogicalPosition().line;
        int maxOffset = document.getLineEndOffset(lineNumber);
        if (newOffset > maxOffset) {
            if (lineNumber + 1 >= document.getLineCount()) {
                return offset;
            }
            maxOffset = document.getLineEndOffset(lineNumber + 1);
        }
        for (; newOffset < maxOffset; newOffset++) {
            if (EditorActionUtil.isWordEnd(text, newOffset, camelMode)
                || EditorActionUtil.isWordStart(text, newOffset, camelMode)) {
                break;
            }
        }
        return newOffset;
    }
}
