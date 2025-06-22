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

import consulo.dataContext.DataContext;
import consulo.ui.ex.action.IdeActions;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.action.EditorWriteActionHandler;
import jakarta.annotation.Nonnull;
import consulo.annotation.access.RequiredWriteAction;

/**
 * @author Denis Zhdanov
 * @since 2011-05-19
 */
public class StartNewLineBeforeAction extends EditorAction {
    public StartNewLineBeforeAction() {
        super(new Handler());
    }

    private static class Handler extends EditorWriteActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
            return getHandler(IdeActions.ACTION_EDITOR_ENTER).isEnabled(editor, caret, dataContext);
        }

        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            editor.getSelectionModel().removeSelection();
            LogicalPosition caretPosition = editor.getCaretModel().getLogicalPosition();
            int line = caretPosition.line;
            int lineStartOffset = editor.getDocument().getLineStartOffset(line);
            editor.getCaretModel().moveToOffset(lineStartOffset);
            getHandler(IdeActions.ACTION_EDITOR_ENTER).execute(editor, caret, dataContext);
            editor.getCaretModel().moveToOffset(editor.getDocument().getLineStartOffset(line));
            getHandler(IdeActions.ACTION_EDITOR_MOVE_LINE_END).execute(editor, caret, dataContext);
        }

        private static EditorActionHandler getHandler(@Nonnull String actionId) {
            return EditorActionManager.getInstance().getActionHandler(actionId);
        }
    }
}
