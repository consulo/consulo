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
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import jakarta.annotation.Nullable;

/**
 * @author max
 * @since 2002-05-13
 */
@ActionImpl(id = "EditorPageDownWithSelection")
public class PageDownWithSelectionAction extends EditorAction {
    private static class Handler extends EditorActionHandler {
        @Override
        public void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            if (!editor.getCaretModel().supportsMultipleCarets()) {
                EditorActionUtil.moveCaretPageDown(editor, true);
                return;
            }
            if (editor.isColumnMode()) {
                int lines = editor.getScrollingModel().getVisibleArea().height / editor.getLineHeight();
                Caret currentCaret = caret == null ? editor.getCaretModel().getPrimaryCaret() : caret;
                for (int i = 0; i < lines; i++) {
                    if (!EditorActionUtil.cloneOrRemoveCaret(editor, currentCaret, false)) {
                        break;
                    }
                    currentCaret = editor.getCaretModel().getPrimaryCaret();
                }
            }
            else if (caret == null) {
                editor.getCaretModel().runForEachCaret(eachCaret -> EditorActionUtil.moveCaretPageDown(editor, true));
            }
            else {
                // assuming caret is equal to CaretModel.getCurrentCaret()
                EditorActionUtil.moveCaretPageDown(editor, true);
            }
        }
    }

    public PageDownWithSelectionAction() {
        super(CodeEditorLocalize.actionPageDownWithSelectionText(), new Handler());
    }
}
