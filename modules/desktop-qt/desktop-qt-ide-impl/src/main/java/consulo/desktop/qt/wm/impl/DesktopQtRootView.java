/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.wm.impl;

import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposer;
import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.project.Project;
import consulo.ui.MenuBar;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtRootView {
    private final DesktopQtRootPaneImpl myRootPanel = new DesktopQtRootPaneImpl();
    private final DesktopQtIdeMenuBar myMenuBar;
    private final DesktopQtNavigationBar myNavigationBar;
    private final Project myProject;

    @RequiredUIAccess
    public DesktopQtRootView(Project project) {
        myProject = project;

        myRootPanel.getComponent().putUserData(UiDataProvider.KEY, sink -> {
            // isInitialized() also waits for the startup activities, and until they pass every action that needs a
            // project - Close Project above all - saw an empty context and disabled itself
            if (!myProject.isDisposed()) {
                sink.set(Project.KEY, myProject);
            }
        });

        myMenuBar = new DesktopQtIdeMenuBar(myRootPanel.getComponent());

        myNavigationBar = new DesktopQtNavigationBar(project, myRootPanel.getComponent());
        // the bar listens on the focus manager, which is one object for the whole application - nothing here is
        // taken down when the project closes unless it is said out loud, and a listener left behind holds the bar,
        // which holds the project. it then answers a focus change by asking a disposed project for a service
        Disposer.register(project, myNavigationBar);
        myRootPanel.setNavigationBar(myNavigationBar.getComponent());
    }

    public MenuBar getMenuBar() {
        return myMenuBar.getMenuBar();
    }

    /**
     * Runs when the frame comes on screen - the menu is expanded against a context which only exists once the
     * frame is there to provide it.
     */
    @RequiredUIAccess
    public void update() {
        myMenuBar.reset();
        myMenuBar.updateMenuActions();

        myNavigationBar.update();
    }

    @RequiredUIAccess
    public void setStatusBar(UnifiedStatusBarImpl statusBar) {
        myRootPanel.setStatusBar(statusBar);
    }

    public DesktopQtRootPaneImpl getRootPanel() {
        return myRootPanel;
    }
}
