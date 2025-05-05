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
package consulo.web.internal.wm;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.actionSystem.impl.UnifiedActionUtil;
import consulo.ide.impl.dataContext.BaseDataManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.project.Project;
import consulo.ui.MenuBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.web.internal.ui.WebRootPaneImpl;

/**
 * @author VISTALL
 * @since 19-Oct-17
 */
public class WebIdeRootView {
    private final WebRootPaneImpl myRootPanel = new WebRootPaneImpl();
    private final MenuItemPresentationFactory myPresentationFactory;
    private Project myProject;

    private MenuBar myMenuBar;

    @RequiredUIAccess
    public WebIdeRootView(Project project) {
        myProject = project;
        myPresentationFactory = new MenuItemPresentationFactory();

        myRootPanel.getComponent().putUserData(Project.KEY, myProject);

        myMenuBar = MenuBar.create();
        myRootPanel.setMenuBar(myMenuBar);
    }

    @RequiredUIAccess
    public void setStatusBar(UnifiedStatusBarImpl statusBar) {
        myRootPanel.setStatusBar(statusBar);
    }

    @RequiredUIAccess
    public void update() {
        DataContext dataContext = ((BaseDataManager) DataManager.getInstance()).getDataContextTest(myRootPanel.getComponent());

        AnAction action = ActionManager.getInstance().getAction(IdeActions.GROUP_MAIN_MENU);

        // TODO explicit read action - remove in future
        myProject.getApplication().runReadAction(() -> {
            UnifiedActionUtil.expandActionGroup((ActionGroup) action,
                dataContext,
                ActionManager.getInstance(),
                myPresentationFactory,
                menuItem -> myMenuBar.add(menuItem));
        });
    }

    public WebRootPaneImpl getRootPanel() {
        return myRootPanel;
    }
}
