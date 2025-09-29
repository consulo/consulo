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

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import jakarta.annotation.Nonnull;

/**
 * Provides functionality similar to the emacs
 * <a href="http://www.gnu.org/software/emacs/manual/html_node/emacs/Setting-Mark.html">exchange-point-and-mark</a>.
 *
 * @author Denis Zhdanov
 * @since 2012-03-18
 */
@ActionImpl(id = "EditorSwapSelectionBoundaries")
public class SwapSelectionBoundariesAction extends EditorAction {
    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        public void execute(@Nonnull Editor editor, DataContext dataContext) {
            if (!(editor instanceof EditorEx editorEx)) {
                return;
            }
            SelectionModel selectionModel = editor.getSelectionModel();
            if (!selectionModel.hasSelection()) {
                return;
            }

            int start = selectionModel.getSelectionStart();
            int end = selectionModel.getSelectionEnd();
            CaretModel caretModel = editor.getCaretModel();
            boolean moveToEnd = caretModel.getOffset() == start;
            editorEx.setStickySelection(false);
            editorEx.setStickySelection(true);
            if (moveToEnd) {
                caretModel.moveToOffset(end);
            }
            else {
                caretModel.moveToOffset(start);
            }
        }
    }

    public SwapSelectionBoundariesAction() {
        super(CodeEditorLocalize.actionSwapSelectionBoundariesText(), new Handler());
    }
}
