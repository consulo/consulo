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

import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;

/**
 * @author max
 * @since 2002-05-13
 */
public class MoveCaretRightWithSelectionAction extends EditorAction {
    public MoveCaretRightWithSelectionAction() {
        super(new Handler());
    }

    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        public void doExecute(Editor editor, Caret caret, DataContext dataContext) {
            editor.getCaretModel()
                .moveCaretRelatively(1, 0, true, editor.isColumnMode(), caret == editor.getCaretModel().getPrimaryCaret());
        }
    }
}
