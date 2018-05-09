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
import java.util.function.Consumer;

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

    expandActionGroup((ActionGroup)CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_MENU), dataContext, ActionManager.getInstance(), myPresentationFactory,
                      menuItem -> myMenuBar.add(menuItem));
  }

  private static void expandActionGroup(ActionGroup group, DataContext context, ActionManager actionManager, MenuItemPresentationFactory menuItemPresentationFactory, Consumer<MenuItem> actionAdded) {
    ArrayList<AnAction> actions = new ArrayList<>();

    expandActionGroup0(group, context, actions, actionManager, menuItemPresentationFactory);

    for (AnAction action : actions) {
      Presentation presentation = menuItemPresentationFactory.getPresentation(action);

      MenuItem menu = action instanceof ActionGroup ? Menu.create(presentation.getText()) : MenuItem.create(presentation.getText());

      actionAdded.accept(menu);

      if (action instanceof ActionGroup) {
        expandActionGroup((ActionGroup)action, context, actionManager, menuItemPresentationFactory, menuItem -> ((Menu)menu).add(menuItem));
      }
    }
  }

  private static void expandActionGroup0(ActionGroup group,
                                         DataContext context,
                                         List<AnAction> newVisibleActions,
                                         ActionManager actionManager,
                                         MenuItemPresentationFactory menuItemPresentationFactory) {
    if (group == null) return;
    final AnAction[] children = group.getChildren(null);
    for (final AnAction action : children) {
      if (!(action instanceof ActionGroup)) {
        continue;
      }
      final Presentation presentation = menuItemPresentationFactory.getPresentation(action);
      final AnActionEvent e = new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, actionManager, 0);
      e.setInjectedContext(action.isInInjectedContext());
      action.update(e);

      newVisibleActions.add(action);
    }
  }

  public Component getComponent() {
    return myRootPanel;
  }
}
