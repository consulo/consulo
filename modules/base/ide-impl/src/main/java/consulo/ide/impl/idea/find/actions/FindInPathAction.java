/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.find.findInProject.FindInProjectManager;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@ActionImpl(id = "FindInPath")
public class FindInPathAction extends FindReplaceInPathActionBase {
    @Inject
    public FindInPathAction(NotificationService notificationService) {
        super(ActionLocalize.actionFindinpathText(), ActionLocalize.actionFindinpathDescription(), notificationService);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);

        FindInProjectManager findManager = FindInProjectManager.getInstance(project);
        if (!findManager.isEnabled()) {
            showNotAvailableMessage(e, project);
            return;
        }

        findManager.findInProject(e.getDataContext(), null);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        doUpdate(e);
    }

    static void doUpdate(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = e.getData(Project.KEY);
        presentation.setEnabled(project != null);
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            presentation.setVisible(isValidSearchScope(e));
        }
    }

    private static boolean isValidSearchScope(@Nonnull AnActionEvent e) {
        PsiElement[] elements = e.getData(PsiElement.KEY_OF_ARRAY);
        if (elements != null && elements.length == 1 && elements[0] instanceof PsiDirectoryContainer) {
            return true;
        }
        VirtualFile[] virtualFiles = e.getData(VirtualFile.KEY_OF_ARRAY);
        return virtualFiles != null && virtualFiles.length == 1 && virtualFiles[0].isDirectory();
    }
}
