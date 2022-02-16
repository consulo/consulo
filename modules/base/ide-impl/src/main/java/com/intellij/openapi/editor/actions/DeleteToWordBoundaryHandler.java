// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.actions;

import consulo.dataContext.DataContext;
import consulo.undoRedo.CommandProcessor;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.ide.CopyPasteManager;
import consulo.document.util.TextRange;
import javax.annotation.Nonnull;

/**
 * @author eldar
 */
class DeleteToWordBoundaryHandler extends EditorWriteActionHandler.ForEachCaret {
  private final boolean myIsUntilStart;
  private final boolean myNegateCamelMode;

  DeleteToWordBoundaryHandler(boolean isUntilStart, boolean negateCamelMode) {
    myIsUntilStart = isUntilStart;
    myNegateCamelMode = negateCamelMode;
  }

  @Override
  public void executeWriteAction(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
    CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
    CopyPasteManager.getInstance().stopKillRings();

    boolean camelMode = editor.getSettings().isCamelWords();
    if (myNegateCamelMode) {
      camelMode = !camelMode;
    }

    if (editor.getSelectionModel().hasSelection()) {
      EditorModificationUtil.deleteSelectedText(editor);
      return;
    }

    final TextRange range = myIsUntilStart ? EditorActionUtil.getRangeToWordStart(editor, camelMode, true) : EditorActionUtil.getRangeToWordEnd(editor, camelMode, true);
    if (!range.isEmpty()) {
      editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());
    }
  }
}
