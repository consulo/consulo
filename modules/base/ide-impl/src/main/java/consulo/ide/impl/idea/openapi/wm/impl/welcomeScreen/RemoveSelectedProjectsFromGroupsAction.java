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

import consulo.ide.impl.idea.ide.ProjectGroup;
import consulo.ide.impl.idea.ide.RecentProjectsManager;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class RemoveSelectedProjectsFromGroupsAction extends RecentProjectsWelcomeScreenActionBase {
  public RemoveSelectedProjectsFromGroupsAction() {
    getTemplatePresentation().setText("Remove from Groups");
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final List<AnAction> elements = getSelectedElements(e);
    for (AnAction element : elements) {
      if (element instanceof ReopenProjectAction) {
        for (ProjectGroup group : RecentProjectsManager.getInstance().getGroups()) {
          group.removeProject(((ReopenProjectAction)element).getProjectPath());
        }
      }

    }

    rebuildRecentProjectsList(e);
  }
}
