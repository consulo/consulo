/*
 * Copyright 2013-2021 consulo.io
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
package consulo.actionSystem.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import consulo.ui.Menu;
import consulo.ui.MenuItem;
import consulo.ui.MenuSeparator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 17/08/2021
 */
public class UnifiedActionUtil {
  public static void expandActionGroup(ActionGroup group, DataContext context, ActionManager actionManager, PresentationFactory menuItemPresentationFactory, Consumer<MenuItem> actionAdded) {
    Map<AnAction, Presentation> actions = new LinkedHashMap<>();

    expandActionGroup0(group, context, actions, actionManager, menuItemPresentationFactory);

    for (Map.Entry<AnAction, Presentation> entry : actions.entrySet()) {
      AnAction action = entry.getKey();
      Presentation presentation = entry.getValue();

      if (action instanceof AnSeparator) {
        actionAdded.accept(MenuSeparator.create());
      }
      else if (action instanceof ActionGroup) {
        MenuItem menu = Menu.create(presentation.getText());
        menu.setIcon(presentation.getIcon());
        actionAdded.accept(menu);
        expandActionGroup((ActionGroup)action, context, actionManager, menuItemPresentationFactory, ((Menu)menu)::add);
      }
      else {
        MenuItem menu = MenuItem.create(presentation.getText());
        menu.addClickListener(event -> {
          DataContext dataContext = DataManager.getInstance().getDataContext();

          action.actionPerformed(AnActionEvent.createFromDataContext("Test", presentation, dataContext));
        });
        menu.setIcon(presentation.getIcon());
        actionAdded.accept(menu);
      }
    }
  }

  private static void expandActionGroup0(ActionGroup group,
                                         DataContext context,
                                         Map<AnAction, Presentation> newVisibleActions,
                                         ActionManager actionManager,
                                         PresentationFactory menuItemPresentationFactory) {
    if (group == null) return;
    final AnAction[] children = group.getChildren(null);
    for (final AnAction action : children) {
      final Presentation presentation = menuItemPresentationFactory.getPresentation(action);
      final AnActionEvent e = new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, actionManager, 0);
      e.setInjectedContext(action.isInInjectedContext());
      action.update(e);

      newVisibleActions.put(action, presentation);
    }
  }
}
