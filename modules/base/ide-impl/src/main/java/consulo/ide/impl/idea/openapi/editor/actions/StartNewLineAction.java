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
import consulo.ui.ex.awt.CopyPasteManager;
import jakarta.annotation.Nonnull;
import consulo.annotation.access.RequiredWriteAction;

/**
 * @author max
 */
public class StartNewLineAction extends EditorAction {
    public StartNewLineAction() {
        super(new Handler());
    }

    private static class Handler extends EditorWriteActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
            return getEnterHandler().isEnabled(editor, caret, dataContext);
        }

        @RequiredWriteAction
        @Override
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            CopyPasteManager.getInstance().stopKillRings();
            if (editor.getDocument().getLineCount() != 0) {
                editor.getSelectionModel().removeSelection();
                LogicalPosition caretPosition = editor.getCaretModel().getLogicalPosition();
                int lineEndOffset = editor.getDocument().getLineEndOffset(caretPosition.line);
                editor.getCaretModel().moveToOffset(lineEndOffset);
            }

            getEnterHandler().execute(editor, caret, dataContext);
        }

        private static EditorActionHandler getEnterHandler() {
            return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER);
        }
    }
}
