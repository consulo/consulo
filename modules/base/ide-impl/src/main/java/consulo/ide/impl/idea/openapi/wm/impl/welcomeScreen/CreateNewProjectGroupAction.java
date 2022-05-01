// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen;

import consulo.ide.impl.idea.ide.ProjectGroup;
import consulo.ide.impl.idea.ide.RecentProjectsManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class CreateNewProjectGroupAction extends RecentProjectsWelcomeScreenActionBase {
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final InputValidator validator = new InputValidator() {
      @Override
      public boolean checkInput(String inputString) {
        inputString = inputString.trim();
        return getGroup(inputString) == null;
      }

      @Override
      public boolean canClose(String inputString) {
        return true;
      }
    };
    final String newGroup = Messages.showInputDialog((Project)null, "Project group name", "Create New Project Group", null, null, validator);
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
