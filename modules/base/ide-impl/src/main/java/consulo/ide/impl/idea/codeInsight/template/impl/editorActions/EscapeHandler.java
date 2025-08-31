/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.template.impl.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.impl.internal.template.TemplateManagerImpl;
import consulo.language.editor.impl.internal.template.TemplateStateImpl;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "templateEscape", order = "before hide-hints")
public class EscapeHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler myOriginalHandler;

    @Override
    @RequiredUIAccess
    public void execute(@Nonnull Editor editor, DataContext dataContext) {
        TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
        if (templateState != null && !templateState.isFinished()) {
            SelectionModel selectionModel = editor.getSelectionModel();
            LookupEx lookup = LookupManager.getActiveLookup(editor);

            // the idea behind lookup checking is that if there is a preselected value in lookup
            // then user might want just to close lookup but not finish a template.
            // E.g. user wants to move to the next template segment by Tab without completion invocation.
            // If there is no selected value in completion that user definitely wants to finish template
            boolean lookupIsEmpty = lookup == null || lookup.getCurrentItem() == null;
            if (!selectionModel.hasSelection() && lookupIsEmpty) {
                CommandProcessor.getInstance().setCurrentCommandName(CodeInsightLocalize.finishTemplateCommand());
                templateState.gotoEnd(true);
                return;
            }
        }

        if (myOriginalHandler.isEnabled(editor, dataContext)) {
            myOriginalHandler.execute(editor, dataContext);
        }
    }

    @Override
    public boolean isEnabled(Editor editor, DataContext dataContext) {
        TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
        return templateState != null && !templateState.isFinished() || myOriginalHandler.isEnabled(editor, dataContext);
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
