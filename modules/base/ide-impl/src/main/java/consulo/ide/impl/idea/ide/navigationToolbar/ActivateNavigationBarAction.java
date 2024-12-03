/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.navigationToolbar;

import consulo.application.dumb.DumbAware;
import consulo.application.ui.UISettings;
import consulo.project.Project;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.NavBarRootPaneExtension;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author Anna Kozlova
 * @author Konstantin Bulenkov
 */
public class ActivateNavigationBarAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getDataContext().getData(Project.KEY);
        if (project != null && UISettings.getInstance().SHOW_NAVIGATION_BAR) {
            final IdeFrame frame = WindowManagerEx.getInstance().getIdeFrame(project);
            final NavBarRootPaneExtension navBarExt = frame.getNorthExtension(NavBarRootPaneExtension.class);
            if (navBarExt != null) {
                navBarExt.rebuildAndSelectTail();
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        final Project project = e.getDataContext().getData(Project.KEY);
        UISettings settings = UISettings.getInstance();
        final boolean enabled = project != null && settings.SHOW_NAVIGATION_BAR && !settings.PRESENTATION_MODE;
        e.getPresentation().setEnabled(enabled);
    }
}
