// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.codeEditor.action.EditorActionUtil;
import consulo.dataContext.DataContext;
import consulo.undoRedo.CommandProcessor;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.ide.impl.idea.openapi.editor.actionSystem.EditorWriteActionHandler;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUIUtil;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.document.util.DocumentUtil;
import consulo.annotation.access.RequiredWriteAction;
import jakarta.inject.Inject;

import javax.annotation.Nullable;

public class DeleteInColumnModeHandler extends EditorWriteActionHandler {
  private final EditorActionHandler myOriginalHandler;

  @Inject
  public DeleteInColumnModeHandler(EditorActionHandler handler) {
    myOriginalHandler = handler;
  }

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
        if (offset < lineEndOffset) myOriginalHandler.execute(editor, c, dataContext);
      });
    }
    else {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }
}
