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
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.application.dumb.DumbAware;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class MoveProjectToGroupActionGroup extends DefaultActionGroup implements DumbAware {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    removeAll();
    final List<ProjectGroup> groups = new ArrayList<>(RecentProjectsManager.getInstance().getGroups());
    Collections.sort(groups, (o1, o2) -> StringUtil.naturalCompare(o1.getName(), o2.getName()));
    for (ProjectGroup group : groups) {
      if (!group.isTutorials()) add(new MoveProjectToGroupAction(group));
    }
    if (groups.size() > 0) {
      add(AnSeparator.getInstance());
      add(new RemoveSelectedProjectsFromGroupsAction());
    }
  }
}
