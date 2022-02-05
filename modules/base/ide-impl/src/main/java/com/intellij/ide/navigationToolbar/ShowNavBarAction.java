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

package com.intellij.ide.navigationToolbar;

import consulo.application.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import consulo.editor.Editor;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.application.ui.awt.UIUtil;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ShowNavBarAction extends AnAction implements DumbAware, PopupAction {
  @Override
  public void actionPerformed(AnActionEvent e){
    final DataContext context = e.getDataContext();
    final Project project = context.getData(CommonDataKeys.PROJECT);
    if (project != null) {
      UISettings uiSettings = UISettings.getInstance();
      if (uiSettings.SHOW_NAVIGATION_BAR && !uiSettings.PRESENTATION_MODE){
        new SelectInNavBarTarget(project).select(null, false);
      } else {
        final Component component = context.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        if (!isInsideNavBar(component)) {
          final Editor editor = context.getData(PlatformDataKeys.EDITOR);
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
  public void update(final AnActionEvent e){
    final boolean enabled = e.getData(CommonDataKeys.PROJECT) != null;
    e.getPresentation().setEnabled(enabled);
  }
}
