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
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.find.FindManager;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

public class SearchBackAction extends AnAction implements DumbAware {
    public SearchBackAction() {
        setEnabledInModalContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        FileEditor editor = e.getRequiredData(FileEditor.KEY);
        CommandProcessor.getInstance().newCommand()
            .project(e.getRequiredData(Project.KEY))
            .name(IdeLocalize.commandFindPrevious())
            .run(() -> {
                PsiDocumentManager.getInstance(project).commitAllDocuments();
                if (FindManager.getInstance(project).findPreviousUsageInEditor(editor)) {
                    return;
                }
                FindUtil.searchBack(project, editor, e.getDataContext());
            });
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY) && e.getData(FileEditor.KEY) instanceof TextEditor);
    }
}
