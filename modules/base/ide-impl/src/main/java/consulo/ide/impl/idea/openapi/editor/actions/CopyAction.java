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

import consulo.codeEditor.action.EditorActionUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.CaretAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.application.util.registry.Registry;

import jakarta.annotation.Nullable;

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 13, 2002
 * Time: 5:37:50 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
public class CopyAction extends TextComponentEditorAction {
    public static final String SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY = "editor.skip.copy.and.cut.for.empty.selection";

    public CopyAction() {
        super(new Handler());
    }

    private static class Handler extends EditorActionHandler {
        @Override
        public void doExecute(final Editor editor, @Nullable Caret caret, DataContext dataContext) {
            if (!editor.getSelectionModel().hasSelection(true)) {
                if (Registry.is(SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY)) {
                    return;
                }
                editor.getCaretModel().runForEachCaret(new CaretAction() {
                    @Override
                    public void perform(Caret caret) {
                        editor.getSelectionModel().selectLineAtCaret();
                        EditorActionUtil.moveCaretToLineStartIgnoringSoftWraps(editor);
                    }
                });
            }
            editor.getSelectionModel().copySelectionToClipboard();
        }
    }
}
