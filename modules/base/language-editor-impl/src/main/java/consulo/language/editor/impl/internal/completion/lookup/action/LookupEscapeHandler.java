/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.completion.lookup.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
@ExtensionImpl(id = "hide-lookup", order = "after hide-hints")
public class LookupEscapeHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler myOriginalHandler;

    @Override
    @RequiredUIAccess
    public void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        LookupEx lookup = LookupManager.getActiveLookup(editor);
        if (lookup != null) {
            lookup.hideLookup(true);
            return;
        }

        myOriginalHandler.execute(editor, caret, dataContext);
    }

    @Override
    public boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
        return LookupManager.getActiveLookup(editor) != null || myOriginalHandler.isEnabled(editor, caret, dataContext);
    }

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_ESCAPE;
    }
}
