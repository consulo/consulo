/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.codeEditor.impl.internal.action.TextComponentEditorAction;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

public class SelectAllAction extends TextComponentEditorAction implements DumbAware {
    public SelectAllAction() {
        super(new Handler());
    }

    private static class Handler extends EditorActionHandler {
        @Override
        @RequiredUIAccess
        public void execute(@Nonnull Editor editor, DataContext dataContext) {
            CommandProcessor.getInstance().newCommand()
                .project(dataContext.getData(Project.KEY))
                .name(IdeLocalize.commandSelectAll())
                .run(() -> editor.getSelectionModel().setSelection(0, editor.getDocument().getTextLength()));
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Editor editor = TextComponentEditorAction.getEditorFromContext(event.getDataContext());
        event.getPresentation().setEnabled(editor != null);
    }
}
