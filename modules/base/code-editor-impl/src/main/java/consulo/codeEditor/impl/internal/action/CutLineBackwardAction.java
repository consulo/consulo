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
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.Document;

/**
 * Stands for emacs 'reverse-kill-line' action, i.e.
 * <a href="http://www.gnu.org/software/emacs/manual/html_node/emacs/Killing-by-Lines.html">'kill-line' action</a>
 * with negative argument.
 *
 * @author Denis Zhdanov
 * @since 2011-04-18
 */
@ActionImpl(id = "EditorCutLineBackward")
public class CutLineBackwardAction extends EditorAction {

    public CutLineBackwardAction() {
        super(new Handler());
    }

    static class Handler extends EditorWriteActionHandler {
        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, DataContext dataContext) {
            Document document = editor.getDocument();
            int caretOffset = editor.getCaretModel().getOffset();
            if (caretOffset <= 0) {
                return;
            }

            // The main idea is to kill everything between the current line start and caret and the whole previous line.

            int caretLine = document.getLineNumber(caretOffset);
            int start;

            if (caretLine <= 0) {
                start = 0;
            }
            else {
                start = document.getLineStartOffset(caretLine - 1);
            }
            KillRingUtil.cut(editor, start, caretOffset);
        }
    }
}
