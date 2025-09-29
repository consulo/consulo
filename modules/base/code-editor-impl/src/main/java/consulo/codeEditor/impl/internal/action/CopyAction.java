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
package consulo.codeEditor.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nullable;

/**
 * @author max
 * @since 2002-05-13
 */
@ActionImpl(id = IdeActions.ACTION_EDITOR_COPY, shortcutFrom = @ActionRef(id = IdeActions.ACTION_COPY))
public class CopyAction extends TextComponentEditorAction {
    private static class Handler extends EditorActionHandler {
        @Override
        public void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            if (!editor.getSelectionModel().hasSelection(true)) {
                if (Registry.is(SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY)) {
                    return;
                }
                editor.getCaretModel().runForEachCaret(eachCaret -> {
                    editor.getSelectionModel().selectLineAtCaret();
                    EditorActionUtil.moveCaretToLineStartIgnoringSoftWraps(editor);
                });
            }
            editor.getSelectionModel().copySelectionToClipboard();
        }
    }

    public static final String SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY = "editor.skip.copy.and.cut.for.empty.selection";

    public CopyAction() {
        super(CodeEditorLocalize.actionCopyText(), new Handler());
    }
}
