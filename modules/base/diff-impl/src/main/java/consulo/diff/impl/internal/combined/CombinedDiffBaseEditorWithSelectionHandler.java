// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

public abstract class CombinedDiffBaseEditorWithSelectionHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private @Nullable EditorActionHandler myOriginal;

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginal = originalHandler;
    }

    @Override
    protected final boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
        EditorActionHandler original = myOriginal;
        return original != null && original.isEnabled(editor, caret, dataContext);
    }

    @RequiredUIAccess
    @Override
    protected final void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        CombinedDiffViewer combined = editor.getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY);
        if (combined != null) {
            doExecute(combined, editor, caret, dataContext);
            return;
        }
        executeOriginal(editor, caret, dataContext);
    }

    @RequiredUIAccess
    protected void executeOriginal(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        EditorActionHandler original = myOriginal;
        if (original != null) {
            original.execute(editor, caret, dataContext);
        }
    }

    @RequiredUIAccess
    protected abstract void doExecute(CombinedDiffViewer combined, Editor editor, @Nullable Caret caret, DataContext dc);
}
