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
package consulo.welcomeScreen.impl.internal;

import consulo.application.Application;
import consulo.application.platform.PlatformTargetManager;
import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.ide.impl.idea.ide.PopupProjectGroupActionGroup;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.RecentProjectsWelcomeScreenActionBase;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.RecentProjectsManager;
import consulo.project.internal.RecentProjectsChecker;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.ListBoxStyle;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.KeyPressedEvent;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-11-07
 */
public class RecentProjectsList {
    private static final String WELCOME_SCREEN_RECENT_PROJECT_ACTION_GROUP = "WelcomeScreenRecentProjectActionGroup";

    private final RecentProjectsManager myRecentProjectsManager;
    private final DataManager myDataManager;
    private final ActionManager myActionManager;

    private @Nullable ActionPopupMenu myRecentProjectMenu;

    public RecentProjectsList(RecentProjectsManager recentProjectsManager, DataManager dataManager, ActionManager actionManager) {
        myRecentProjectsManager = recentProjectsManager;
        myDataManager = dataManager;
        myActionManager = actionManager;
    }

    @RequiredUIAccess
    public Component create(AnAction[] recentProjectsActions) {
        List<AnAction> allActions = Arrays.asList(recentProjectsActions);
        MutableFlatDataModel<AnAction> model = FlatDataModel.of(allActions);

        ListBox<AnAction> listSelect = ListBox.create(model);
        listSelect.setRenderer(new RecentProjectItemRenderer(
            myRecentProjectsManager,
            RecentProjectsChecker.getInstance(),
            Application.get().getInstance(PlatformTargetManager.class),
            (action, event) -> performAction(listSelect, action, event),
            (action, event) -> showRecentProjectMenu(listSelect, action, event)
        ));
        // the menu is built against the list, so what the actions of the menu work on is published by it
        listSelect.putUserData(
            UiDataProvider.KEY,
            sink -> sink.set(RecentProjectsWelcomeScreenActionBase.RECENT_PROJECTS_LIST, listSelect)
        );

        listSelect.setSelectOnHover(true);
        listSelect.setPlaceholder(ProjectLocalize.recentProjectsNoProjectOpenYet());
        listSelect.setItemHeightGetter(action -> RecentProjectItemRenderer.ROW_HEIGHT);
        listSelect.addStyle(ListBoxStyle.TRANSPARENT_BACKGROUND);

        ScrollableLayout scroll = ScrollableLayout.create(
            listSelect,
            ScrollableLayoutOptions.builder()
                .backgroundPaint(false)
                .horizontalScrollPolicy(ScrollableLayoutOptions.ScrollPolicy.NEVER)
                .build()
        );

        if (allActions.isEmpty()) {
            return scroll;
        }

        TextBox search = TextBox.create();
        search.setPlaceholder(ProjectLocalize.recentProjectsSearchPlaceholder());
        search.addStyle(TextBoxStyle.HAS_CLEAR_BUTTON);
        Label searchIcon = Label.create();
        searchIcon.setImage(PlatformIconGroup.actionsSearch());
        search.setPrefixComponent(searchIcon);
        search.addValueListener(event -> filter(listSelect, model, allActions, StringUtil.notNullize(event.getValue())));
        search.addKeyPressedListener(event -> onSearchKeyPressed(search, listSelect, event));

        DockLayout searchPanel = DockLayout.create(Space.NONE);
        searchPanel.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        searchPanel.center(search);
        searchPanel.paddingBuilder().allSet(Space.MEDIUM).apply();

        DockLayout layout = DockLayout.create(Space.NONE);
        layout.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        layout.top(searchPanel);
        layout.center(scroll);
        return layout;
    }

    @RequiredUIAccess
    private void filter(ListBox<AnAction> listBox, MutableFlatDataModel<AnAction> model, List<AnAction> allActions, String text) {
        String query = text.trim();

        listBox.setPlaceholder(query.isEmpty()
            ? ProjectLocalize.recentProjectsNoProjectOpenYet()
            : ProjectLocalize.recentProjectsNothingFound());

        model.removeAll();
        for (AnAction action : allActions) {
            if (query.isEmpty() || StringUtil.containsIgnoreCase(searchTextOf(action), query)) {
                model.add(action);
            }
        }

        if (!query.isEmpty() && model.getSize() > 0) {
            listBox.setValueByIndex(0);
        }
    }

    private static String searchTextOf(AnAction action) {
        if (action instanceof ReopenProjectAction item) {
            String home = Platform.current().user().homePath().toString();
            String path = item.getProjectPath();
            if (FileUtil.startsWith(path, home)) {
                path = path.substring(home.length());
            }
            return item.getProjectName() + " " + path;
        }

        if (action instanceof PopupProjectGroupActionGroup actionGroup) {
            return actionGroup.getGroup().getName();
        }

        return action.toString();
    }

    @RequiredUIAccess
    private void onSearchKeyPressed(TextBox search, ListBox<AnAction> listBox, KeyPressedEvent event) {
        KeyboardInputDetails details = event.getInputDetails();
        KeyCode keyCode = details.getKeyCode();
        if (keyCode == null || details.withCtrl() || details.withAlt() || details.withMeta() || details.withShift()) {
            return;
        }

        int key = keyCode.key();
        if (key == KeyCode.UP.key()) {
            listBox.moveSelection(-1);
        }
        else if (key == KeyCode.DOWN.key()) {
            listBox.moveSelection(1);
        }
        else if (key == KeyCode.ENTER.key()) {
            AnAction action = listBox.getValue();
            if (action != null) {
                performAction(listBox, action, event);
            }
        }
        else if (key == KeyCode.ESCAPE.key() && !StringUtil.isEmpty(search.getValue())) {
            search.setValue("");
        }
    }

    @RequiredUIAccess
    private void performAction(ListBox<AnAction> listBox, AnAction action, ComponentEvent<Component> event) {
        AnActionEvent e = AnActionEvent.createFromAnAction(
            action,
            null,
            ActionPlaces.WELCOME_SCREEN,
            myDataManager.getDataContext(listBox),
            event
        );

        action.actionPerformed(e);
    }

    @RequiredUIAccess
    private void showRecentProjectMenu(ListBox<AnAction> listBox, AnAction action, ComponentEvent<Component> event) {
        if (!(myActionManager.getAction(WELCOME_SCREEN_RECENT_PROJECT_ACTION_GROUP) instanceof ActionGroup group)) {
            return;
        }

        listBox.setValue(action, false);

        // asking for the menu again is asking for one menu, wherever it was asked from - the one standing open is
        // not closed by the gesture which opens the next, since that gesture never reaches it
        closeRecentProjectMenu();

        ActionPopupMenu menu = myActionManager.createActionPopupMenu(ActionPlaces.WELCOME_SCREEN, group);
        menu.setTargetComponent(listBox);

        myRecentProjectMenu = menu;
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @RequiredUIAccess
    private void closeRecentProjectMenu() {
        ActionPopupMenu menu = myRecentProjectMenu;
        myRecentProjectMenu = null;

        if (menu != null) {
            menu.hide();
        }
    }
}
