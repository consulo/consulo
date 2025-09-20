// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.action;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.fileEditor.LargeFileEditor;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class LfeEditorActionHandlerDisabled extends LfeBaseEditorActionHandler {

    private static final Logger logger = Logger.getInstance(LfeEditorActionHandlerDisabled.class);

    @Override
    protected void doExecuteInLfe(@Nonnull LargeFileEditor largeFileEditor,
                                  @Nonnull Editor editor,
                                  @Nullable Caret caret,
                                  DataContext dataContext) {
        // never called
        logger.warn("Called code, that shouldn't be called. toString()=" + toString());
    }

    @Override
    protected boolean isEnabledInLfe(@Nonnull LargeFileEditor largeFileEditor,
                                     @Nonnull Editor editor,
                                     @Nonnull Caret caret,
                                     DataContext dataContext) {
        return false;
    }
}
