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

import consulo.project.ui.wm.StatusBar;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtRootPaneImpl {
    private final DockLayout myDockLayout = DockLayout.create();

    @RequiredUIAccess
    public void setCenterComponent(@Nullable Component centerComponent) {
        myDockLayout.center(centerComponent);
    }

    @RequiredUIAccess
    public void setMenuBar(@Nullable MenuBar menuBar) {
        myDockLayout.top(menuBar);
    }

    @RequiredUIAccess
    public void setNavigationBar(Component navigationBar) {
        // the frame hands the menu bar to the qt window itself, so the top slot of the dock is the bar's own
        myDockLayout.top(navigationBar);
    }

    public Component getComponent() {
        return myDockLayout;
    }

    @RequiredUIAccess
    public void setStatusBar(StatusBar statusBar) {
        myDockLayout.bottom(statusBar.getUIComponent());
    }
}
