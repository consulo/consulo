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
package consulo.codeEditor.impl.internal.floating;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbar;
import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
@ExtensionImpl(id = "codeFloatingToolbarEscape", order = "before hide-hints")
public class CodeFloatingToolbarEscapeHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private @Nullable EditorActionHandler myOriginalHandler;

    @Override
    @RequiredUIAccess
    public void execute(Editor editor, DataContext dataContext) {
        CodeFloatingToolbar toolbar = CodeFloatingToolbar.getToolbar(editor);
        if (toolbar != null && toolbar.isShown()) {
            toolbar.scheduleHide();
            return;
        }

        EditorActionHandler originalHandler = myOriginalHandler;
        if (originalHandler != null && originalHandler.isEnabled(editor, dataContext)) {
            originalHandler.execute(editor, dataContext);
        }
    }

    @Override
    @RequiredUIAccess
    public boolean isEnabled(Editor editor, DataContext dataContext) {
        CodeFloatingToolbar toolbar = CodeFloatingToolbar.getToolbar(editor);
        if (toolbar != null && toolbar.isShown()) {
            return true;
        }

        EditorActionHandler originalHandler = myOriginalHandler;
        return originalHandler != null && originalHandler.isEnabled(editor, dataContext);
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
