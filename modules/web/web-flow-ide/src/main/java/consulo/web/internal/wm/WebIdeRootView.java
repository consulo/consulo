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

import consulo.disposer.Disposer;
import consulo.dataContext.UiDataProvider;
import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.web.internal.ui.action.WebActionMenuExpander;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.internal.ui.WebRootPaneImpl;
import consulo.web.internal.ui.base.WebFocusTracker;
import consulo.web.internal.ui.base.WebKeyTracker;
import consulo.web.internal.ui.base.WebShortcutDispatcher;

/**
 * @author VISTALL
 * @since 19-Oct-17
 */
public class WebIdeRootView {
    private final WebRootPaneImpl myRootPanel = new WebRootPaneImpl();
    private final WebIdeMenuBar myMenuBar;
    private final WebNavigationBar myNavigationBar;
    private final Project myProject;

    @RequiredUIAccess
    public WebIdeRootView(Project project) {
        myProject = project;

        // the frame provider is reached by walking up from whatever scope is focused, it must not become a scope
        // itself - otherwise clicking the menu bar or the navigation bar would drop the editor context
        WebFocusTracker.exclude(myRootPanel.getComponent());

        myRootPanel.getComponent().putUserData(UiDataProvider.KEY, sink -> {
            // isInitialized() also waits for the startup activities, and until they pass every action that needs a
            // project - Close Project above all - saw an empty context and disabled itself
            if (myProject != null && !myProject.isDisposed()) {
                sink.set(Project.KEY, myProject);
            }
        });

        WebFocusTracker.installRoot(myRootPanel.getComponent());
        WebKeyTracker.installRoot(myRootPanel.getComponent());
        WebShortcutDispatcher.installRoot(myRootPanel.getComponent());

        myMenuBar = new WebIdeMenuBar(myRootPanel.getComponent());
        myRootPanel.setMenuBar(myMenuBar.getMenuBar());
        myRootPanel.setMenuBarRightComponent(createCloseProjectButton());

        myNavigationBar = new WebNavigationBar(project, myRootPanel.getComponent());
        // the bar listens on the focus manager, which is one object for the whole application - nothing here is
        // taken down when the project closes unless it is said out loud, and a listener left behind holds the bar,
        // which holds the project. it then answers a focus change by asking a disposed project for a service
        Disposer.register(project, myNavigationBar);
        myRootPanel.setNavigationBar(myNavigationBar.getComponent());
    }

    /**
     * Closing a project is the frame's own business - a browser has no window controls to put it among, so it
     * sits at the end of the menu row. The platform action is what runs, the same one the File menu holds.
     */
    @RequiredUIAccess
    private Component createCloseProjectButton() {
        // the icon alone, drawn the way a toolbar draws its buttons - a label here wraps over two lines and
        // takes the width the menu needs, which pushes the menu into an overflow of its own
        Button button = Button.create(LocalizeValue.empty());
        button.setIcon(PlatformIconGroup.actionsCancel());
        button.setToolTipText(ActionLocalize.actionCloseprojectText());
        button.addStyle(ButtonStyle.BORDERLESS);
        button.addClickListener(event -> {
            AnAction action = ActionManager.getInstance().getAction("CloseProject");
            if (action != null) {
                WebActionMenuExpander.performAction(
                    action,
                    WebFocusTracker.createDataContext(myRootPanel.getComponent()),
                    ActionPlaces.MAIN_MENU,
                    new MenuItemPresentationFactory(),
                    event.getInputDetails()
                );
            }
        });
        return button;
    }

    @RequiredUIAccess
    public void setStatusBar(UnifiedStatusBarImpl statusBar) {
        myRootPanel.setStatusBar(statusBar);
    }

    @RequiredUIAccess
    public void update() {
        myMenuBar.updateMenuActions();

        myNavigationBar.update();
    }

    public WebRootPaneImpl getRootPanel() {
        return myRootPanel;
    }
}
