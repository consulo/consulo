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
package consulo.ide.impl.idea.codeInsight.template.impl.actions;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.editor.impl.internal.template.TemplateManagerImpl;
import consulo.language.editor.impl.internal.template.TemplateStateImpl;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 * @since 2002-05-16
 */
public class PreviousVariableAction extends EditorAction {
    public PreviousVariableAction() {
        super(new Handler());
        setInjectedContext(true);
    }

    private static class Handler extends EditorActionHandler {
        @Override
        @RequiredUIAccess
        protected void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
            assert templateState != null;
            CommandProcessor.getInstance().setCurrentCommandName(CodeInsightLocalize.templatePreviousVariableCommand());
            templateState.previousTab();
        }

        @Override
        protected boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
            TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
            return templateState != null && !templateState.isFinished();
        }
    }
}