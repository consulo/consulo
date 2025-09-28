/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.document.Document;

/**
 * Stands for emacs <a href="http://www.gnu.org/software/emacs/manual/html_node/emacs/Words.html#Words">kill-word</a> command.
 * <p/>
 * Generally, it removes text from the current cursor position up to the end of the current word and puts
 * it to the {@link KillRingTransferable kill ring}.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2011-04-19
 */
@ActionImpl(id = "EditorKillToWordEnd")
public class KillToWordEndAction extends TextComponentEditorAction {
    private static class Handler extends EditorWriteActionHandler {
        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            CaretModel caretModel = editor.getCaretModel();
            int caretOffset = caretModel.getOffset();
            Document document = editor.getDocument();
            if (caretOffset >= document.getTextLength()) {
                return;
            }

            int caretLine = caretModel.getLogicalPosition().line;
            int lineEndOffset = document.getLineEndOffset(caretLine);
            CharSequence text = document.getCharsSequence();
            boolean camel = editor.getSettings().isCamelWords();
            for (int i = caretOffset + 1; i < lineEndOffset; i++) {
                if (EditorActionUtil.isWordEnd(text, i, camel)) {
                    KillRingUtil.cut(editor, caretOffset, i);
                    return;
                }
            }

            int end = lineEndOffset;
            if (caretLine < document.getLineCount() - 1) {
                // No word end found between the current position and line end, hence, remove line feed sign if possible.
                end++;
            }

            if (end > caretOffset) {
                KillRingUtil.cut(editor, caretOffset, end);
            }
        }
    }

    public KillToWordEndAction() {
        super(CodeEditorLocalize.actionKillToWordEndText(), new Handler());
    }
}
