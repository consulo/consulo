/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.EditorEx;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;

/**
 * Allows to toggle {@link EditorEx#isStickySelection() sticky selection} for editors.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2011-04-20
 */
@ActionImpl(id = IdeActions.ACTION_EDITOR_TOGGLE_STICKY_SELECTION)
public class ToggleStickySelectionModeAction extends EditorAction {
    static class Handler extends EditorActionHandler {
        @Override
        public void execute(@Nonnull Editor editor, DataContext dataContext) {
            if (editor instanceof EditorEx ex) {
                ex.setStickySelection(!ex.isStickySelection());
            }
        }
    }

    public ToggleStickySelectionModeAction() {
        super(ActionLocalize.actionEditortogglestickyselectionText(), new Handler());
    }
}
