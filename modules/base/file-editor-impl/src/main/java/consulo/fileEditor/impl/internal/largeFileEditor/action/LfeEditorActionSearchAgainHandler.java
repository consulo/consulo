// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal.largeFileEditor.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.fileEditor.LargeFileEditor;
import consulo.fileEditor.impl.internal.largeFileEditor.search.CloseSearchTask;
import consulo.fileEditor.internal.largeFileEditor.LfeSearchManager;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class LfeEditorActionSearchAgainHandler extends LfeBaseEditorActionHandler {

    private final boolean isForwardDirection = isForwardDirection();

    @Override
    protected void doExecuteInLfe(LargeFileEditor largeFileEditor,
                                  Editor editor,
                                  @Nullable Caret caret,
                                  DataContext dataContext) {
        LfeSearchManager searchManager = largeFileEditor.getSearchManager();
        searchManager.gotoNextOccurrence(isForwardDirection);
    }

    @Override
    protected boolean isEnabledInLfe(LargeFileEditor largeFileEditor,
                                     Editor editor,
                                     Caret caret,
                                     DataContext dataContext) {
        LfeSearchManager searchManager = largeFileEditor.getSearchManager();
        CloseSearchTask task = (CloseSearchTask) searchManager.getLastExecutedCloseSearchTask();
        return task == null || task.isFinished();
    }

    protected boolean isForwardDirection() {
        return true;
    }

    
    @Override
    public String getActionId() {
        return IdeActions.ACTION_FIND_NEXT;
    }
}
