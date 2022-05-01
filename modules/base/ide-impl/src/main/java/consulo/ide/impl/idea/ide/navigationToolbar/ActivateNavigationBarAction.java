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

import consulo.application.ui.UISettings;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.ide.impl.idea.openapi.wm.ex.WindowManagerEx;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;

/**
 * @author Anna Kozlova
 * @author Konstantin Bulenkov
 */
public class ActivateNavigationBarAction extends AnAction implements DumbAware {
  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    if (project != null && UISettings.getInstance().SHOW_NAVIGATION_BAR) {
      final IdeFrame frame = WindowManagerEx.getInstance().getIdeFrame(project);
      final IdeRootPaneNorthExtension navBarExt = frame.getNorthExtension(NavBarRootPaneExtension.NAV_BAR);
      if (navBarExt != null) {
        final JComponent c = navBarExt.getComponent();
        final NavBarPanel panel = (NavBarPanel)c.getClientProperty("NavBarPanel");
        panel.rebuildAndSelectTail(true);
      }
    }
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    UISettings settings = UISettings.getInstance();
    final boolean enabled = project != null && settings.SHOW_NAVIGATION_BAR && !settings.PRESENTATION_MODE;
    e.getPresentation().setEnabled(enabled);
  }
}
