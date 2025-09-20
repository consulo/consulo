// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal.largeFileEditor.action;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.fileEditor.LargeFileEditor;
import consulo.fileEditor.internal.largeFileEditor.LargeEditorActionUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class LfeBaseEditorActionHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler originalHandler;

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        this.originalHandler = originalHandler;
    }

    @Override
    protected final void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        LargeFileEditor largeFileEditor = LargeEditorActionUtil.tryGetLargeFileEditorManagerFromEditor(editor);
        if (largeFileEditor != null) {
            doExecuteInLfe(largeFileEditor, editor, caret, dataContext);
        }
        else {
            if (originalHandler != null) {
                originalHandler.execute(editor, caret, dataContext);
            }
        }
    }

    @Override
    protected final boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
        LargeFileEditor largeFileEditor = LargeEditorActionUtil.tryGetLargeFileEditorManagerFromEditor(editor);
        if (largeFileEditor != null) {
            return isEnabledInLfe(largeFileEditor, editor, caret, dataContext);
        }
        else {
            return originalHandler != null
                ? originalHandler.isEnabled(editor, caret, dataContext)
                : false;
        }
    }

    protected final EditorActionHandler getOriginalHandler() {
        return originalHandler;
    }

    protected abstract void doExecuteInLfe(@Nonnull LargeFileEditor largeFileEditor,
                                           @Nonnull Editor editor,
                                           @Nullable Caret caret,
                                           DataContext dataContext);

    protected abstract boolean isEnabledInLfe(@Nonnull LargeFileEditor largeFileEditor,
                                              @Nonnull Editor editor,
                                              @Nonnull Caret caret,
                                              DataContext dataContext);
}
