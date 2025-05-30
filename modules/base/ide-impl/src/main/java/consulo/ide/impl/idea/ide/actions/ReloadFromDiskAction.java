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
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;

public class ReloadFromDiskAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        Editor editor = dataContext.getData(Editor.KEY);
        if (editor == null) {
            return;
        }
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            return;
        }

        int res = Messages.showOkCancelDialog(
            project,
            IdeLocalize.promptReloadFileFromDisk(psiFile.getVirtualFile().getPresentableUrl()).get(),
            IdeLocalize.titleReloadFile().get(),
            UIUtil.getWarningIcon()
        );
        if (res != 0) {
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(IdeLocalize.commandReloadFromDisk())
            .inWriteAction()
            .run(() -> PsiManager.getInstance(project).reloadFromDisk(psiFile));
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        Editor editor = dataContext.getData(Editor.KEY);
        if (editor == null) {
            presentation.setEnabled(false);
            return;
        }
        Document document = editor.getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null || psiFile.getVirtualFile() == null) {
            presentation.setEnabled(false);
        }
    }
}
