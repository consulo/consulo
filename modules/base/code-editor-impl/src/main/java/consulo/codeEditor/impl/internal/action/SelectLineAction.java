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
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;

/**
 * @author max
 * @since 2002-05-22
 */
@ActionImpl(id = "EditorSelectLine")
public class SelectLineAction extends TextComponentEditorAction {
    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        public void execute(Editor editor, DataContext dataContext) {
            editor.getSelectionModel().selectLineAtCaret();
            EditorActionUtil.moveCaretToLineStartIgnoringSoftWraps(editor);
        }
    }

    public SelectLineAction() {
        super(CodeEditorLocalize.actionSelectLineText(), new Handler());
    }
}
