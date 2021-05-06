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
package consulo.ui.taskbar;

import com.intellij.ide.DataManager;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import consulo.wm.impl.DesktopSystemDockImpl;

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
