/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.action;

import consulo.desktop.awt.welcomeScreen.NewRecentProjectPanel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.RecentProjectsManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class ManageRecentProjectsAction extends DumbAwareAction {
  public ManageRecentProjectsAction() {
    super(ActionLocalize.actionManagerecentprojectsText());
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Disposable disposable = Disposable.newDisposable();
    NewRecentProjectPanel panel = new NewRecentProjectPanel(disposable, false);
    JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel.getRootPanel(), panel.getList())
      .setTitle("Recent Projects")
      .setFocusable(true)
      .setRequestFocus(true)
      .setMayBeParent(true)
      .setMovable(true)
      .createPopup();
    Disposer.register(popup, disposable);
    Project project = e.getRequiredData(Project.KEY);
    popup.showCenteredInCurrentWindow(project);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    boolean enable = false;
    if (project != null) {
      enable = RecentProjectsManager.getInstance().getRecentProjectsActions(false).length > 0;
    }

    e.getPresentation().setEnabledAndVisible(enable);
  }
}
