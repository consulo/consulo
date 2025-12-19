// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ProjectGroup;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "WelcomeScreen.NewGroup")
public class CreateNewProjectGroupAction extends RecentProjectsWelcomeScreenActionBase {
    public CreateNewProjectGroupAction() {
        super(ProjectUILocalize.actionRecentProjectsNewGroupText(), LocalizeValue.absent(), PlatformIconGroup.nodesFolder());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        InputValidator validator = new InputValidator() {
            @Override
            @RequiredUIAccess
            public boolean checkInput(String inputString) {
                inputString = inputString.trim();
                return getGroup(inputString) == null;
            }

            @Override
            @RequiredUIAccess
            public boolean canClose(String inputString) {
                return true;
            }
        };
        String newGroup = Messages.showInputDialog(
            (Project) null,
            ProjectUILocalize.dialogMessageRecentProjectsProjectGroupName().get(),
            ProjectUILocalize.dialogTitleRecentProjectsCreateNewProjectGroup().get(),
            null,
            null,
            validator
        );
        if (newGroup != null) {
            RecentProjectsManager.getInstance().addGroup(new ProjectGroup(newGroup));
            rebuildRecentProjectsList(e);
        }
    }

    private static ProjectGroup getGroup(String name) {
        for (ProjectGroup group : RecentProjectsManager.getInstance().getGroups()) {
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }
}
