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
package consulo.ide.impl.idea.ide;

import consulo.project.ProjectGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.RecentProjectsWelcomeScreenActionBase;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class ProjectGroupActionGroup extends DefaultActionGroup implements DumbAware {
  private final ProjectGroup myGroup;

  public ProjectGroupActionGroup(ProjectGroup group, List<AnAction> children) {
    super(group.getName(), children);
    myGroup = group;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    new RecentProjectsWelcomeScreenActionBase() {
      @RequiredUIAccess
      @Override
      public void actionPerformed(AnActionEvent e) {
        myGroup.setExpanded(!myGroup.isExpanded());
        final JList list = getList(e);
        if (list != null) {
          final int index = list.getSelectedIndex();
          rebuildRecentProjectsList(e);
          list.setSelectedIndex(index);
        }
      }
    }.actionPerformed(e);
  }

  @Override
  public boolean isPopup() {
    return !myGroup.isExpanded();
  }

  public ProjectGroup getGroup() {
    return myGroup;
  }
}
