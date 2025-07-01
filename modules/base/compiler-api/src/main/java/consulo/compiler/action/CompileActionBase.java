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
package consulo.compiler.action;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.compiler.CompilerManager;
import consulo.dataContext.DataContext;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class CompileActionBase extends AnAction implements DumbAware {
    protected CompileActionBase() {
    }

    protected CompileActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        Editor editor = e.getData(Editor.KEY);
        PsiFile file = e.getData(PsiFile.KEY);
        if (file != null && editor != null && !DumbService.getInstance(project).isDumb()) {
            DaemonCodeAnalyzer.getInstance(project).autoImportReferenceAtCursor(editor, file); //let autoimport complete
        }
        doAction(dataContext, project);
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @RequiredUIAccess
    protected abstract void doAction(DataContext dataContext, Project project);

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        Project project = e.getData(Project.KEY);
        if (project == null || !project.isInitialized()) {
            e.getPresentation().setEnabled(false);
        }
        else {
            e.getPresentation().setEnabled(!CompilerManager.getInstance(project).isCompilationActive());
        }
    }
}
