// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.util.DocumentUtil;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUIUtil;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.CommandProcessor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "delete.in.column.mode")
public class DeleteInColumnModeHandler extends EditorWriteActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler myOriginalHandler;

    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (editor.isColumnMode() && caret == null && editor.getCaretModel().getCaretCount() > 1) {
            EditorUIUtil.hideCursorInEditor(editor);
            CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
            CopyPasteManager.getInstance().stopKillRings();

            editor.getCaretModel().runForEachCaret(c -> {
                int offset = c.getOffset();
                int lineEndOffset = DocumentUtil.getLineEndOffset(offset, editor.getDocument());
                if (offset < lineEndOffset) {
                    myOriginalHandler.execute(editor, c, dataContext);
                }
            });
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
