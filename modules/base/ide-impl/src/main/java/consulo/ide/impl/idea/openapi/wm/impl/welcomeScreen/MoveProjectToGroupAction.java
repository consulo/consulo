/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen;

import consulo.project.ProjectGroup;
import consulo.project.internal.RecentProjectsManager;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class MoveProjectToGroupAction extends RecentProjectsWelcomeScreenActionBase {
  private final ProjectGroup myGroup;

  public MoveProjectToGroupAction(ProjectGroup group) {
    myGroup = group;
    getTemplatePresentation().setText(group.getName());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final List<AnAction> elements = getSelectedElements(e);
    for (AnAction element : elements) {
      if (element instanceof ReopenProjectAction) {
        final String path = ((ReopenProjectAction)element).getProjectPath();
        for (ProjectGroup group : RecentProjectsManager.getInstance().getGroups()) {
          group.removeProject(path);
          myGroup.addProject(path);
        }
      }
    }
    rebuildRecentProjectsList(e);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(!hasGroupSelected(e));
  }
}
