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
package consulo.ide.impl.actionSystem.impl;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionRunnerAsync;
import consulo.ui.Menu;
import consulo.ui.MenuItem;
import consulo.ui.MenuSeparator;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 17/08/2021
 */
public class UnifiedActionUtil {
    @RequiredUIAccess
    public static CompletableFuture<Void> expandActionGroup(ActionGroup group,
                                                            DataContext context,
                                                            ActionManager actionManager,
                                                            PresentationFactory menuItemPresentationFactory,
                                                            Consumer<MenuItem> actionAdded) {
        if (group == null) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        AnAction[] children = group.getChildren(null);
        List<AnAction> actions = new ArrayList<>();
        List<Presentation> presentations = new ArrayList<>();
        List<CompletableFuture<?>> updates = new ArrayList<>();
        for (AnAction action : children) {
            Presentation presentation = menuItemPresentationFactory.getPresentation(action);
            AnActionEvent e = new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, actionManager, 0);
            e.setInjectedContext(action.isInInjectedContext());
            actions.add(action);
            presentations.add(presentation);
            updates.add(ActionRunnerAsync.performDumbAwareUpdateAsync(action, e));
        }

        return CompletableFuture.allOf(updates.toArray(new CompletableFuture[0])).whenCompleteAsync((r, throwable) -> {
            for (int i = 0; i < actions.size(); i++) {
                AnAction action = actions.get(i);
                Presentation presentation = presentations.get(i);

                if (action instanceof AnSeparator) {
                    actionAdded.accept(MenuSeparator.create());
                }
                else if (action instanceof ActionGroup actionGroup) {
                    MenuItem menu = Menu.create(presentation.getTextValue());
                    menu.setIcon(presentation.getIcon());
                    actionAdded.accept(menu);
                    expandActionGroup(actionGroup, context, actionManager, menuItemPresentationFactory, ((Menu) menu)::add);
                }
                else {
                    MenuItem menu = MenuItem.create(presentation.getTextValue());
                    menu.addClickListener(event -> {
                        DataContext dataContext = DataManager.getInstance().getDataContext();

                        action.actionPerformed(AnActionEvent.createFromDataContext("Test", presentation, dataContext));
                    });
                    menu.setIcon(presentation.getIcon());
                    actionAdded.accept(menu);
                }
            }
        }, uiAccess);
    }
}
