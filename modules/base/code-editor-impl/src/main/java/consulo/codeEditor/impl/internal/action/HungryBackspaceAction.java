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
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ui.ex.action.IdeActions;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

/**
 * Works like a usual backspace except the situation when the caret is located after white space - all white space symbols
 * (white spaces, tabulations, line feeds) are removed then.
 *
 * @author Denis Zhdanov
 * @since 2012-06-27
 */
@ActionImpl(id = "EditorHungryBackSpace")
public class HungryBackspaceAction extends TextComponentEditorAction {
    private static class Handler extends EditorWriteActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        @RequiredWriteAction
        public void executeWriteAction(@Nonnull Editor editor, Caret caret, DataContext dataContext) {
            Document document = editor.getDocument();
            int caretOffset = editor.getCaretModel().getOffset();
            if (caretOffset < 1) {
                return;
            }

            SelectionModel selectionModel = editor.getSelectionModel();
            CharSequence text = document.getCharsSequence();
            char c = text.charAt(caretOffset - 1);
            if (!selectionModel.hasSelection() && StringUtil.isWhiteSpace(c)) {
                int startOffset = CharArrayUtil.shiftBackward(text, caretOffset - 2, "\t \n") + 1;
                document.deleteString(startOffset, caretOffset);
            }
            else {
                EditorActionHandler handler = EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE);
                handler.execute(editor, caret, dataContext);
            }
        }
    }

    public HungryBackspaceAction() {
        super(CodeEditorLocalize.actionHungryBackspaceText(), CodeEditorLocalize.actionHungryBackspaceDescription(), new Handler());
    }
}
