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
package consulo.desktop.qt.wm.impl.toolWindow;

import consulo.desktop.qt.ui.impl.layout.DesktopQtTabbedLayoutImpl;
import consulo.ide.impl.wm.impl.UnifiedToolWindowInternalDecorator;
import consulo.project.Project;
import consulo.project.ui.impl.internal.wm.UnifiedToolWindowImpl;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtToolWindowInternalDecorator extends UnifiedToolWindowInternalDecorator {
    private final int myHeaderHeight;

    @RequiredUIAccess
    public DesktopQtToolWindowInternalDecorator(
        Project project,
        WindowInfoImpl windowInfo,
        UnifiedToolWindowImpl toolWindow,
        boolean canWorkInDumbMode
    ) {
        super(project, windowInfo, toolWindow);

        myHeaderHeight = DesktopQtTabbedLayoutImpl.tabRowHeight();

        DockLayout header = getHeader().getComponent();
        header.setHeight(myHeaderHeight);
        header.paddingBuilder().leftSet(Space.MEDIUM).rightSet(Space.SMALL).apply();
    }

    @Override
    public int getHeaderHeight() {
        return myHeaderHeight;
    }
}
