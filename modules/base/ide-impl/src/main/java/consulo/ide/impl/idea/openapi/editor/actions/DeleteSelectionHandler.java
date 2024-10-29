/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.CaretAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUIUtil;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.CommandProcessor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "delete.for.selection")
public class DeleteSelectionHandler extends EditorWriteActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler myOriginalHandler;

    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (caret == null ? editor.getSelectionModel().hasSelection(true) : caret.hasSelection()) {
            EditorUIUtil.hideCursorInEditor(editor);
            CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
            CopyPasteManager.getInstance().stopKillRings();
            CaretAction action = c -> EditorModificationUtil.deleteSelectedText(editor);
            if (caret == null) {
                editor.getCaretModel().runForEachCaret(action);
            }
            else {
                action.perform(caret);
            }
        }
        else {
            myOriginalHandler.execute(editor, caret, dataContext);
        }
    }

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_DELETE;
    }
}
