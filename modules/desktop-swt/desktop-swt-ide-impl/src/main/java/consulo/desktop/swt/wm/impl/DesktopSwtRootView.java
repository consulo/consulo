/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.wm.impl;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.project.Project;
import consulo.ui.MenuBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.wm.impl.UnifiedStatusBarImpl;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
public class DesktopSwtRootView {
  private final DesktopSwtRootPaneImpl myRootPanel = new DesktopSwtRootPaneImpl();
  private final MenuItemPresentationFactory myPresentationFactory;
  private Project myProject;

  private MenuBar myMenuBar;
  private UnifiedStatusBarImpl myStatusBar;

  @RequiredUIAccess
  public DesktopSwtRootView(Project project) {
    myProject = project;
    myRootPanel.setSizeFull();
    myPresentationFactory = new MenuItemPresentationFactory();

    myRootPanel.getComponent().putUserData(CommonDataKeys.PROJECT, myProject);

    //myMenuBar = MenuBar.create();
    //myRootPanel.setMenuBar(myMenuBar);
  }

  @RequiredUIAccess
  public void setStatusBar(UnifiedStatusBarImpl statusBar) {
    myStatusBar = statusBar;

    myRootPanel.setStatusBar(statusBar);
  }

  @RequiredUIAccess
  public void update() {
    //DataContext dataContext = ((BaseDataManager)DataManager.getInstance()).getDataContextTest(myRootPanel.getComponent());
    //
    //AnAction action = ActionManager.getInstance().getAction(IdeActions.GROUP_MAIN_MENU);
    //
    //UnifiedActionUtil.expandActionGroup((ActionGroup)action, dataContext, ActionManager.getInstance(), myPresentationFactory, menuItem -> myMenuBar.add(menuItem));
  }

  public DesktopSwtRootPaneImpl getRootPanel() {
    return myRootPanel;
  }
}
