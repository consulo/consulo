// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.action;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.fileEditor.LargeFileEditor;
import consulo.fileEditor.history.IdeDocumentHistory;
import org.jspecify.annotations.Nullable;

public abstract class LfeEditorActionTextStartEndHandler extends LfeBaseEditorActionHandler {

    private final boolean isStart;

    public LfeEditorActionTextStartEndHandler(boolean start) {
        isStart = start;
    }

    @Override
    protected void doExecuteInLfe(LargeFileEditor largeFileEditor,
                                  Editor editor,
                                  @Nullable Caret caret,
                                  DataContext dataContext) {
        if (isStart) {
            largeFileEditor.getEditorModel().setCaretToFileStartAndShow();
        }
        else {
            largeFileEditor.getEditorModel().setCaretToFileEndAndShow();
        }

        IdeDocumentHistory docHistory = IdeDocumentHistory.getInstance(largeFileEditor.getProject());
        if (docHistory != null) {
            docHistory.includeCurrentCommandAsNavigation();
            docHistory.setCurrentCommandHasMoves();
        }
    }

    @Override
    protected boolean isEnabledInLfe(LargeFileEditor largeFileEditor,
                                     Editor editor,
                                     Caret caret,
                                     DataContext dataContext) {
        return true;
    }
}
