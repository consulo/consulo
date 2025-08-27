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

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

@ActionImpl(
    id = "ReloadFromDisk"/*,
    parents = @ActionParentRef(
        value = @ActionRef(id = IdeActions.GROUP_FILE),
        anchor = ActionRefAnchor.AFTER,
        relatedToAction = @ActionRef(id = IdeActions.ACTION_SYNCHRONIZE)
    )*/
)
public class ReloadFromDiskAction extends AnAction implements DumbAware {
    public ReloadFromDiskAction() {
        super(ActionLocalize.actionReloadfromdiskText(), ActionLocalize.actionReloadfromdiskDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        Editor editor = e.getRequiredData(Editor.KEY);
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
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);
        if (project == null || editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        Document document = editor.getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null || psiFile.getVirtualFile() == null) {
            e.getPresentation().setEnabled(false);
        }
    }
}
