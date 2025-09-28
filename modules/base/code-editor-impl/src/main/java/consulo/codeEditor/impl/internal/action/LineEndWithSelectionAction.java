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
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;

/**
 * @author max
 * @since 2002-05-14
 */
@ActionImpl(id = IdeActions.ACTION_EDITOR_MOVE_LINE_END_WITH_SELECTION)
public class LineEndWithSelectionAction extends TextComponentEditorAction {
    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        public void execute(@Nonnull Editor editor, DataContext dataContext) {
            EditorActionUtil.moveCaretToLineEnd(editor, true);
        }
    }

    public LineEndWithSelectionAction() {
        super(CodeEditorLocalize.actionLineEndWithSelectionText(), new Handler());
    }
}
