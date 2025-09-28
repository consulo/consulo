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
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
@ActionImpl(id = IdeActions.ACTION_EDITOR_ESCAPE)
public class EscapeAction extends EditorAction {
    private static class Handler extends EditorActionHandler {
        @Override
        public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            if (editor instanceof EditorEx) {
                EditorEx editorEx = (EditorEx) editor;
                if (editorEx.isStickySelection()) {
                    editorEx.setStickySelection(false);
                }
            }
            boolean scrollNeeded = editor.getCaretModel().getCaretCount() > 1;
            retainOldestCaret(editor.getCaretModel());
            editor.getSelectionModel().removeSelection();
            if (scrollNeeded) {
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            }
        }

        private static void retainOldestCaret(CaretModel caretModel) {
            while (caretModel.getCaretCount() > 1) {
                caretModel.removeCaret(caretModel.getPrimaryCaret());
            }
        }

        @Override
        public boolean isEnabled(Editor editor, DataContext dataContext) {
            SelectionModel selectionModel = editor.getSelectionModel();
            CaretModel caretModel = editor.getCaretModel();
            return selectionModel.hasSelection() || caretModel.getCaretCount() > 1;
        }
    }

    public EscapeAction() {
        super(CodeEditorLocalize.actionEscapeText(), new Handler());
        setInjectedContext(true);
    }

    @Override
    public int getExecuteWeight() {
        return Integer.MIN_VALUE;
    }
}
