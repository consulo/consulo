/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.impl.internal.rename;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.TemplateState;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * User: anna
 * Date: 12/27/11
 */
@ExtensionImpl(order = "before hide-hints")
public class EscapeHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler myOriginalHandler;

    @Override
    public void execute(Editor editor, DataContext dataContext) {
        final SelectionModel selectionModel = editor.getSelectionModel();
        Project project;
        if (selectionModel.hasSelection() && (project = editor.getProject()) != null) {
            final TemplateState state = TemplateManager.getInstance(project).getTemplateState(editor);
            if (state != null && editor.getUserData(InplaceRefactoring.INPLACE_RENAMER) != null) {
                final LookupEx lookup = LookupManager.getActiveLookup(editor);
                if (lookup != null) {
                    selectionModel.removeSelection();
                    lookup.hide();
                    return;
                }
            }
        }

        myOriginalHandler.execute(editor, dataContext);
    }

    @Override
    public boolean isEnabled(Editor editor, DataContext dataContext) {
        Project project = editor.getProject();
        if (project != null) {
            final TemplateState templateState = TemplateManager.getInstance(project).getTemplateState(editor);

            if (templateState != null && !templateState.isFinished()) {
                return true;
            }
        }
        
        return myOriginalHandler.isEnabled(editor, dataContext);
    }

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_ESCAPE;
    }
}
