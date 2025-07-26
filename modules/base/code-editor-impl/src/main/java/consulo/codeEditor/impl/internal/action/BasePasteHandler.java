/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.codeEditor.impl.internal.action;

import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorCopyPasteHelper;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.datatransfer.Transferable;
import java.util.function.Supplier;

public class BasePasteHandler extends EditorWriteActionHandler {
    protected Transferable myTransferable;

    @Override
    public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
        return !editor.isViewer();
    }

    @RequiredUIAccess
    @Override
    public void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        // We capture the contents to paste here, so it that it won't be affected by possible clipboard operations later (e.g. during unlocking
        // of current file for writing)
        myTransferable = getContentsToPaste(editor, dataContext);
        try {
            super.doExecute(editor, caret, dataContext);
        }
        finally {
            myTransferable = null;
        }
    }

    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (myTransferable != null) {
            EditorCopyPasteHelper.getInstance().pasteTransferable(editor, myTransferable);
        }
    }

    protected Transferable getContentsToPaste(Editor editor, DataContext dataContext) {
        Supplier<Transferable> producer = dataContext.getData(PasteAction.TRANSFERABLE_PROVIDER);
        return EditorImplUtil.getContentsToPasteToEditor(producer);
    }
}
