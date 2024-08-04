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

import consulo.ide.impl.idea.ide.ProjectGroupActionGroup;
import consulo.project.internal.RecentProjectsManager;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.ScrollingUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public class RemoveSelectedProjectsAction extends RecentProjectsWelcomeScreenActionBase {
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    RecentProjectsManager mgr = RecentProjectsManager.getInstance();
    for (AnAction action : getSelectedElements(e)) {
      if (action instanceof ReopenProjectAction) {
        String path = ((ReopenProjectAction)action).getProjectPath();
        mgr.removePath(path);
      }
      else if (action instanceof ProjectGroupActionGroup) {
        ProjectGroupActionGroup group = (ProjectGroupActionGroup)action;
        for (String path : group.getGroup().getProjects()) {
          mgr.removePath(path);
        }
        mgr.removeGroup(group.getGroup());
      }

      rebuildRecentProjectsList(e);

      JList list = getList(e);
      if (list != null) {
        ScrollingUtil.ensureSelectionExists(list);
      }
    }

  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(!getSelectedElements(e).isEmpty());
  }
}
