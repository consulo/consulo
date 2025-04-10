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

import consulo.dataContext.DataManager;
import consulo.ide.IdeView;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolWindow.ToolWindow;

/**
 * @author yole
 */
public class NewElementToolbarAction extends NewElementAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        if (e.getData(IdeView.KEY) == null) {
            Project project = e.getData(Project.KEY);
            PsiFileSystemItem psiFile = e.getData(PsiFile.KEY).getParent();
            ProjectView.getInstance(project).selectCB(psiFile, psiFile.getVirtualFile(), true)
                .doWhenDone(() -> showPopup(DataManager.getInstance().getDataContext()));
        }
        else {
            super.actionPerformed(e);
        }
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
        super.update(event);
        if (event.getData(IdeView.KEY) == null) {
            Project project = event.getData(Project.KEY);
            PsiFile psiFile = event.getData(PsiFile.KEY);
            if (project != null && psiFile != null) {
                ToolWindow projectViewWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
                if (projectViewWindow.isVisible()) {
                    event.getPresentation().setEnabled(true);
                }
            }
        }
    }
}
