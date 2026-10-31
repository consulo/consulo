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
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BasePasteHandler extends EditorWriteActionHandler {
    protected DataTransfer myTransfer;

    @Override
    public boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
        return !editor.isViewer();
    }

    /**
     * The clipboard is read before anything is opened, because a frontend which keeps it in a browser answers
     * with a round trip. Only once it has answered are the command and the write action opened, so the edit
     * itself stays synchronous and remains a single undoable step.
     */
    @Override
    @RequiredUIAccess
    public void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        EditorImplUtil.whenContentsToPaste(getContentsToPaste(editor, dataContext), transfer -> {
            myTransfer = transfer;
            try {
                CommandProcessor.getInstance()
                    .newCommand()
                    .project(editor.getProject())
                    .document(editor.getDocument())
                    .name(CodeEditorLocalize.actionPasteText())
                    .groupId(getCommandGroupId(editor))
                    .run(() -> super.doExecute(editor, caret, dataContext));
            }
            finally {
                myTransfer = null;
            }
        });
    }

    /**
     * The command is opened above, once the clipboard has answered. Letting the action open one first would put
     * the read inside it and leave the command empty while the browser is asked.
     */
    @Override
    public boolean executeInCommand(Editor editor, DataContext dataContext) {
        return false;
    }

    @Override
    @RequiredWriteAction
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (myTransfer != null) {
            EditorCopyPasteHelper.getInstance().pasteDataTransfer(editor, myTransfer);
        }
    }

    protected CompletableFuture<@Nullable DataTransfer> getContentsToPaste(Editor editor, DataContext dataContext) {
        Supplier<DataTransfer> producer = dataContext.getData(PasteAction.DATA_TRANSFER_PROVIDER);
        return EditorImplUtil.getContentsToPasteToEditor(producer);
    }
}
