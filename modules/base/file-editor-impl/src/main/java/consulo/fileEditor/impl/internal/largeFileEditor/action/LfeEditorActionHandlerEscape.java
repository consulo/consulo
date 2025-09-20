// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.fileEditor.LargeFileEditor;
import consulo.fileEditor.internal.SearchReplaceComponent;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public final class LfeEditorActionHandlerEscape extends LfeBaseEditorActionHandler {
    @Override
    protected void doExecuteInLfe(@Nonnull LargeFileEditor largeFileEditor,
                                  @Nonnull Editor editor,
                                  @Nullable Caret caret,
                                  DataContext dataContext) {
        if (largeFileEditor.getEditor().getHeaderComponent() instanceof SearchReplaceComponent) {
            largeFileEditor.getSearchManager().onEscapePressed();
        }
        else if (getOriginalHandler().isEnabled(editor, caret, dataContext)) {
            getOriginalHandler().execute(editor, caret, dataContext);
        }
    }

    @Override
    protected boolean isEnabledInLfe(@Nonnull LargeFileEditor largeFileEditor,
                                     @Nonnull Editor editor,
                                     @Nonnull Caret caret,
                                     DataContext dataContext) {
        return true;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_ESCAPE;
    }
}
