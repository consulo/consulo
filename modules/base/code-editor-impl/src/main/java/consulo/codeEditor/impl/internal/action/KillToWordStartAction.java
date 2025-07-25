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
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.dataContext.DataContext;

/**
 * Stands for emacs <a href="http://www.gnu.org/software/emacs/manual/html_node/emacs/Words.html#Words">backward-kill-word</a> command.
 * <p/>
 * Generally, it removes text from the previous word start up to the current cursor position and puts
 * it to the {@link KillRingTransferable kill ring}.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2011-04-19
 */
@ActionImpl(id = "EditorKillToWordStart")
public class KillToWordStartAction extends TextComponentEditorAction {
    public KillToWordStartAction() {
        super(new Handler());
    }

    private static class Handler extends EditorWriteActionHandler {
        @RequiredWriteAction
        @Override
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            CaretModel caretModel = editor.getCaretModel();
            int caretOffset = caretModel.getOffset();
            if (caretOffset <= 0) {
                return;
            }

            boolean camel = editor.getSettings().isCamelWords();
            for (int i = caretOffset - 1; i >= 0; i--) {
                if (EditorActionUtil.isWordOrLexemeStart(editor, i, camel)) {
                    KillRingUtil.cut(editor, i, caretOffset);
                    return;
                }
            }

            KillRingUtil.cut(editor, 0, caretOffset);
        }
    }
}
