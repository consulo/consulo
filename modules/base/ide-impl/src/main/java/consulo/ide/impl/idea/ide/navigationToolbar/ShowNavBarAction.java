/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ShowNavBarAction extends AnAction implements DumbAware, PopupAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e){
    final DataContext context = e.getDataContext();
    final Project project = context.getData(Project.KEY);
    if (project != null) {
      UISettings uiSettings = UISettings.getInstance();
      if (uiSettings.SHOW_NAVIGATION_BAR && !uiSettings.PRESENTATION_MODE){
        new SelectInNavBarTarget(project).select(null, false);
      } else {
        final Component component = context.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        if (!isInsideNavBar(component)) {
          final Editor editor = context.getData(Editor.KEY);
          final NavBarPanel toolbarPanel = new NavBarPanel(project, false);
          toolbarPanel.showHint(editor, context);
        }
      }
    }
  }

  private static boolean isInsideNavBar(Component c) {
    return c == null
           || c instanceof NavBarPanel
           || UIUtil.getParentOfType(NavBarListWrapper.class, c) != null;
  }


  @Override
  @RequiredUIAccess
  public void update(final AnActionEvent e){
    final boolean enabled = e.getData(Project.KEY) != null;
    e.getPresentation().setEnabled(enabled);
  }
}
