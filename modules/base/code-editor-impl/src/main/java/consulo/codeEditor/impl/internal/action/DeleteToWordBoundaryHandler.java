// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl.internal.action;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

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

        TextRange range = myIsUntilStart
            ? EditorActionUtil.getRangeToWordStart(editor, camelMode, true)
            : EditorActionUtil.getRangeToWordEnd(editor, camelMode, true);
        if (!range.isEmpty()) {
            editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());
        }
    }
}
