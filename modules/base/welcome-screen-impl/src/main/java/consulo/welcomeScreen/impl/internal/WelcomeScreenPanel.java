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

import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.welcomeScreen.WelcomeScreenSlider;
import consulo.localize.LocalizeValue;
import consulo.project.RecentProjectsManager;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Space;
import consulo.ui.StaticPosition;
import consulo.ui.UIAccess;
import consulo.ui.VerticalAlignment;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.welcomeScreen.impl.internal.icon.WelcomeScreenIconGroup;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The welcome screen as an editor tab: the recent projects on the left, and what to start with as large buttons of
 * one width, centred on a soft backdrop. New project and checkout slide in over it.
 *
 * @author VISTALL
 * @since 2026-11-07
 */
public class WelcomeScreenPanel implements WelcomeScreenSlider {
    private static final String MAIN = "main";

    private final Disposable myParentDisposable;
    private final DataManager myDataManager;
    private final ActionManager myActionManager;
    private final RecentProjectsManager myRecentProjectsManager;
    private final AnAction[] myRecentProjectsActions;

    private @Nullable SwipeLayout myRoot;

    public WelcomeScreenPanel(
        Disposable parentDisposable,
        DataManager dataManager,
        ActionManager actionManager,
        RecentProjectsManager recentProjectsManager,
        AnAction[] recentProjectsActions
    ) {
        myParentDisposable = parentDisposable;
        myDataManager = dataManager;
        myActionManager = actionManager;
        myRecentProjectsManager = recentProjectsManager;
        myRecentProjectsActions = recentProjectsActions;
    }

    @RequiredUIAccess
    public Component getComponent() {
        SwipeLayout root = myRoot;
        if (root == null) {
            myRoot = root = SwipeLayout.create();
            root.putUserData(UiDataProvider.KEY, sink -> sink.set(WelcomeScreenSlider.KEY, this));
            root.register(MAIN, this::buildMainLayout);
        }
        return root;
    }

    @RequiredUIAccess
    private Layout buildMainLayout() {
        DockLayout layout = DockLayout.create(Space.NONE);
        layout.setBackgroundColor(WelcomeBackdrop.baseColor());

        if (myRecentProjectsActions.length > 0) {
            Component recentProjects = new RecentProjectsList(myRecentProjectsManager, myDataManager, myActionManager)
                .create(myRecentProjectsActions);
            recentProjects.borderBuilder().rightSet().apply();
            layout.left(recentProjects);
        }

        layout.center(createActions());
        return layout;
    }

    @RequiredUIAccess
    private Component createActions() {
        TableLayout buttons = TableLayout.create(StaticPosition.CENTER);
        buttons.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);

        VerticalLayout layout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        layout.setBackgroundColor(WelcomeBackdrop.create());
        layout.add(buttons);

        ActionGroup actionGroup = (ActionGroup) myActionManager.getAction(IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART);
        if (actionGroup == null) {
            return layout;
        }

        List<AnAction> actions = new ArrayList<>();
        collectAllActions(actions, actionGroup);

        Component rootComponent = getComponent();

        List<AnActionEvent> events = new ArrayList<>(actions.size());
        List<CompletableFuture<?>> updates = new ArrayList<>(actions.size());
        for (AnAction action : actions) {
            AnActionEvent e =
                AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, myDataManager.getDataContext(rootComponent));
            events.add(e);
            updates.add(ActionRunnerAsync.performDumbAwareUpdateAsync(action, e));
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.allOf(updates.toArray(new CompletableFuture[0])).whenCompleteAsync((r, throwable) -> {
            int row = 0;
            for (int i = 0; i < actions.size(); i++) {
                Presentation presentation = events.get(i).getPresentation();
                if (presentation.isVisible()) {
                    buttons.add(createButton(actions.get(i), presentation), TableLayout.cell(row++, 0).fill());
                }
            }
        }, uiAccess);
        return layout;
    }

    @RequiredUIAccess
    private Button createButton(AnAction action, Presentation presentation) {
        Button button = Button.create(presentation.getTextValue(), event -> performAction(action, presentation, event));
        button.addStyle(ButtonStyle.LARGE);
        button.setHorizontalAlignment(HorizontalAlignment.LEFT);

        Image icon = welcomeIcon(myActionManager.getId(action));
        if (icon == null) {
            icon = presentation.getIcon();
        }
        if (icon != null) {
            button.setIcon(icon);
        }
        return button;
    }

    private static @Nullable Image welcomeIcon(@Nullable String actionId) {
        if (actionId == null) {
            return null;
        }

        return switch (actionId) {
            case "WelcomeScreen.CreateNewProject" -> WelcomeScreenIconGroup.newproject();
            case "WelcomeScreen.OpenProject" -> WelcomeScreenIconGroup.open();
            case "WelcomeScreen.GetFromVcs" -> WelcomeScreenIconGroup.fromvcs();
            default -> null;
        };
    }

    /**
     * The event of the update answered whether the action is shown at all, it says nothing about the click which
     * follows - an action opening a popup reads the position out of the event it is given, and it is only the click
     * that has one.
     */
    @RequiredUIAccess
    private void performAction(AnAction action, Presentation presentation, ComponentEvent<Component> event) {
        AnActionEvent clickEvent = new AnActionEvent(
            null,
            myDataManager.getDataContext(event.getComponent()),
            ActionPlaces.WELCOME_SCREEN,
            presentation,
            myActionManager,
            0,
            false,
            false,
            event
        );
        clickEvent.setInjectedContext(action.isInInjectedContext());

        action.actionPerformed(clickEvent);
    }

    private static void collectAllActions(List<AnAction> target, ActionGroup actionGroup) {
        for (AnAction action : actionGroup.getChildren(null)) {
            if (action instanceof ActionGroup childGroup && !childGroup.isPopup()) {
                collectAllActions(target, childGroup);
            }
            else {
                target.add(action);
            }
        }
    }

    @Override
    public void setTitle(LocalizeValue title) {
    }

    @Override
    public TitlelessDecorator getTitlelessDecorator() {
        return TitlelessDecorator.NOTHING;
    }

    @Override
    @RequiredUIAccess
    public Layout showSlide(String id, @RequiredUIAccess Supplier<Layout> layoutSupplier) {
        SwipeLayout root = (SwipeLayout) getComponent();
        root.register(id, layoutSupplier);
        return root.swipeLeftTo(id);
    }

    @Override
    @RequiredUIAccess
    public void removeSlide(Layout target) {
        SwipeLayout root = (SwipeLayout) getComponent();
        root.swipeRightTo(MAIN);
        root.remove(target);

        if (target instanceof Disposable disposable) {
            disposable.disposeWithTree();
        }
    }

    @Override
    public Disposable getDisposable() {
        return myParentDisposable;
    }
}
