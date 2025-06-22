/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import jakarta.annotation.Nullable;

/**
 * @author max
 * @since 2002-05-13
 */
public class CutAction extends EditorAction {
    public CutAction() {
        super(new Handler());
    }

    public static class Handler extends EditorWriteActionHandler {
        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            if (!editor.getSelectionModel().hasSelection(true)) {
                if (Registry.is(CopyAction.SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY)) {
                    return;
                }
                editor.getCaretModel().runForEachCaret(eachCaret -> editor.getSelectionModel().selectLineAtCaret());
            }
            editor.getSelectionModel().copySelectionToClipboard();
            EditorModificationUtil.deleteSelectedTextForAllCarets(editor);
        }
    }
}