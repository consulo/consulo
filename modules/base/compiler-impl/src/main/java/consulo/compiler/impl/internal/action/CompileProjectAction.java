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
package consulo.compiler.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.compiler.CompilerManager;
import consulo.compiler.action.CompileActionBase;
import consulo.compiler.localize.CompilerLocalize;
import consulo.dataContext.DataContext;
import consulo.localHistory.LocalHistory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "CompileProject")
public class CompileProjectAction extends CompileActionBase {
    @Override
    @RequiredUIAccess
    protected void doAction(DataContext dataContext, Project project) {
        CompilerManager.getInstance(project).rebuild((aborted, errors, warnings, compileContext) -> {
            if (aborted) {
                return;
            }

            String text = getTemplatePresentation().getText();
            LocalHistory.getInstance().putSystemLabel(
                project,
                errors == 0
                    ? CompilerLocalize.rebuildLvcsLabelNoErrors(text)
                    : CompilerLocalize.rebuildLvcsLabelWithErrors(text)
            );
        });
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);
        Presentation presentation = event.getPresentation();
        if (!presentation.isEnabled()) {
            return;
        }
        Project project = event.getData(Project.KEY);
        presentation.setEnabled(project != null);
    }
}