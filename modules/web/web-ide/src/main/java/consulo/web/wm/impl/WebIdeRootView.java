/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.wm.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.project.Project;
import consulo.actionSystem.impl.UnifiedActionUtil;
import consulo.ide.base.BaseDataManager;
import consulo.ui.MenuBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.web.internal.WebRootPaneImpl;
import consulo.wm.impl.UnifiedStatusBarImpl;

/**
 * @author VISTALL
 * @since 19-Oct-17
 */
public class WebIdeRootView {
  private final WebRootPaneImpl myRootPanel = new WebRootPaneImpl();
  private final MenuItemPresentationFactory myPresentationFactory;
  private Project myProject;

  private MenuBar myMenuBar;
  private UnifiedStatusBarImpl myStatusBar;

  @RequiredUIAccess
  public WebIdeRootView(Project project) {
    myProject = project;
    myRootPanel.setSizeFull();
    myPresentationFactory = new MenuItemPresentationFactory();

    myRootPanel.getComponent().putUserData(CommonDataKeys.PROJECT, myProject);

    myMenuBar = MenuBar.create();
    myRootPanel.setMenuBar(myMenuBar);
  }

  @RequiredUIAccess
  public void setStatusBar(UnifiedStatusBarImpl statusBar) {
    myStatusBar = statusBar;

    myRootPanel.setStatusBar(statusBar);
  }

  @RequiredUIAccess
  public void update() {
    DataContext dataContext = ((BaseDataManager)DataManager.getInstance()).getDataContextTest(myRootPanel.getComponent());

    AnAction action = ActionManager.getInstance().getAction(IdeActions.GROUP_MAIN_MENU);

    UnifiedActionUtil.expandActionGroup((ActionGroup)action, dataContext, ActionManager.getInstance(), myPresentationFactory, menuItem -> myMenuBar.add(menuItem));
  }

  public WebRootPaneImpl getRootPanel() {
    return myRootPanel;
  }
}
