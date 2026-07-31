/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.internal.ui;

import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.web.internal.ui.base.ToVaadinComponentWrapper;

/**
 * @author VISTALL
 * @since 2023-05-27
 */
public class WebRootPaneImpl {
    private final DockLayout myDockLayout = DockLayout.create();

    // the dock has a single top slot, the menu bar and the navigation bar are stacked inside it
    private final VerticalLayout myTopLayout = VerticalLayout.create();

    public WebRootPaneImpl() {
        myDockLayout.top(myTopLayout);
    }

    @RequiredUIAccess
    public void setCenterComponent(Component content) {
        myDockLayout.center(content);
    }

    @RequiredUIAccess
    public void setMenuBar(MenuBar menuBar) {
        myTopLayout.add(menuBar);
    }

    @RequiredUIAccess
    public void setNavigationBar(Component navigationBar) {
        myTopLayout.add(navigationBar);
    }

    @RequiredUIAccess
    public void setStatusBar(UnifiedStatusBarImpl statusBar) {
        Component component = statusBar.getUIComponent();

        // the bar collapses to the border while every widget is still empty, and an ide frame without a status
        // bar at all looks broken - the strip keeps its height like the awt one does
        if (component instanceof ToVaadinComponentWrapper wrapper) {
            wrapper.toVaadinComponent().addClassName("web-status-bar");
        }

        myDockLayout.bottom(component);
    }

    public Component getComponent() {
        return myDockLayout;
    }
}
