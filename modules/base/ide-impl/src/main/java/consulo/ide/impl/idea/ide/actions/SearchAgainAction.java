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
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.find.FindManager;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;

public class SearchAgainAction extends AnAction implements DumbAware {
    public SearchAgainAction() {
        setEnabledInModalContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        FileEditor editor = e.getData(FileEditor.KEY);
        if (editor == null || project == null) {
            return;
        }
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(IdeLocalize.commandFindNext())
            .run(() -> {
                PsiDocumentManager.getInstance(project).commitAllDocuments();
                IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
                if (FindManager.getInstance(project).findNextUsageInEditor(editor)) {
                    return;
                }

                FindUtil.searchAgain(project, editor, e.getDataContext());
            });
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        FileEditor editor = event.getData(FileEditor.KEY);
        presentation.setEnabled(editor instanceof TextEditor);
    }
}
