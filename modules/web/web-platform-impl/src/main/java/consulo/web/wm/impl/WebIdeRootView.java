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
package consulo.web.wm.impl;

import com.intellij.ide.DataManager;
import com.intellij.ide.impl.DataManagerImpl;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.project.Project;
import consulo.ui.*;
import consulo.ui.internal.WGwtRootPanelImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 19-Oct-17
 */
public class WebIdeRootView {
  private final WGwtRootPanelImpl myRootPanel = new WGwtRootPanelImpl();
  private final MenuItemPresentationFactory myPresentationFactory;
  private Project myProject;

  private MenuBar myMenuBar;

  public WebIdeRootView(Project project) {
    myProject = project;
    myRootPanel.setSizeFull();
    myPresentationFactory = new MenuItemPresentationFactory();

    myRootPanel.putUserData(CommonDataKeys.PROJECT, myProject);
    myMenuBar = MenuBar.create();
    myRootPanel.setMenuBar(myMenuBar);
  }

  @RequiredUIAccess
  public void update() {
    DataContext dataContext = ((DataManagerImpl)DataManager.getInstance()).getDataContextTest(myRootPanel);
    ArrayList<AnAction> newVisibleActions = new ArrayList<>();
    expandActionGroup(dataContext, newVisibleActions, ActionManager.getInstance());

    for (AnAction newVisibleAction : newVisibleActions) {
      Presentation presentation = myPresentationFactory.getPresentation(newVisibleAction);

      if (presentation.isVisible()) {
        Menu menu = Menu.create(presentation.getText());

        myMenuBar.add(menu);
      }
    }
  }

  private void expandActionGroup(final DataContext context, final List<AnAction> newVisibleActions, ActionManager actionManager) {
    final ActionGroup mainActionGroup = (ActionGroup)CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_MENU);
    if (mainActionGroup == null) return;
    final AnAction[] children = mainActionGroup.getChildren(null);
    for (final AnAction action : children) {
      if (!(action instanceof ActionGroup)) {
        continue;
      }
      final Presentation presentation = myPresentationFactory.getPresentation(action);
      final AnActionEvent e = new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, actionManager, 0);
      e.setInjectedContext(action.isInInjectedContext());
      action.update(e);
      if (presentation.isVisible()) { // add only visible items
        newVisibleActions.add(action);
      }
    }
  }

  public Component getComponent() {
    return myRootPanel;
  }
}
