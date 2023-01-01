/*
 * Copyright 2013-2018 consulo.io
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
package consulo.desktop.awt.wm.impl.dock;

import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.RecentProjectsManager;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.Application;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-09-01
 */
public class Java9DockDelegateImpl implements DesktopSystemDockImpl.Delegate {
  private final Menu recentProjectsMenu = new Menu("Recent projects");

  public Java9DockDelegateImpl() {
    PopupMenu dockMenu = new PopupMenu("DockMenu");
    Taskbar.getTaskbar().setMenu(dockMenu);
    dockMenu.add(recentProjectsMenu);
  }

  @Override
  public void updateRecentProjectsMenu() {
    Application.get().getLastUIAccess().give(() -> {
      RecentProjectsManager projectsManager = RecentProjectsManager.getInstance();

      final AnAction[] recentProjectActions = projectsManager.getRecentProjectsActions(false);
      recentProjectsMenu.removeAll();

      for (final AnAction action : recentProjectActions) {
        MenuItem menuItem = new MenuItem(((ReopenProjectAction)action).getProjectName());
        menuItem.addActionListener(e -> action.actionPerformed(AnActionEvent.createFromAnAction(action, null, ActionPlaces.DOCK_MENU, DataManager.getInstance().getDataContext((Component)null))));
        recentProjectsMenu.add(menuItem);
      }
    });
  }
}
