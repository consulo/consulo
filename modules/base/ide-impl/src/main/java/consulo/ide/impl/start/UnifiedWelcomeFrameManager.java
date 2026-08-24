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
package consulo.ide.impl.start;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.util.lang.ref.SimpleReference;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.event.ComponentEvent;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.RecentProjectsWelcomeScreenActionBase;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.application.FrameTitleUtil;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ide.impl.welcomeScreen.BaseUnifiedWelcomeScreenPanel;
import consulo.ide.impl.welcomeScreen.RecentProjectItemRender;
import consulo.project.ui.impl.internal.wm.UnifiedWelcomeIdeFrame;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.ProjectManager;
import consulo.project.internal.RecentProjectsChecker;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.TitlelessDecoratorService;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.layout.Layout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedWelcomeFrameManager extends WelcomeFrameManager {
    private static final String WELCOME_SCREEN_RECENT_PROJECT_ACTION_GROUP = "WelcomeScreenRecentProjectActionGroup";

    private @Nullable ActionPopupMenu myRecentProjectMenu;

    private final Provider<ProjectManager> myProjectManager;
    // asking for the instance here rather than at the frame would have the platform build the service - and
    // read its stored state - while the application is still coming up, and it answers no recent project at all
    private final Provider<RecentProjectsManager> myRecentProjectsManager;
    private final DataManager myDataManager;
    private final ActionManager myActionManager;

    @Inject
    public UnifiedWelcomeFrameManager(
        Application application,
        Provider<ProjectManager> projectManager,
        Provider<RecentProjectsManager> recentProjectsManager,
        DataManager dataManager,
        ActionManager actionManager
    ) {
        super(application);
        myProjectManager = projectManager;
        myRecentProjectsManager = recentProjectsManager;
        myDataManager = dataManager;
        myActionManager = actionManager;
    }

    @Override
    public void closeFrame(@Nullable UIAccess uiAccess) {
        super.closeFrame(uiAccess);

        if (uiAccess != null) {
            uiAccess.giveIfNeed(this::frameClosed);
        }
    }

    @RequiredUIAccess
    @Override
    public IdeFrame createFrame() {
        WindowOptions.Builder builder = WindowOptions.builder();

        String welcomeTitle;
        // disable close and resize, and remove title, we not allow it in web mode
        if (Platform.current().isInBrowser()) {
            builder.disableClose().disableResize();
            welcomeTitle = "";
        }
        else {
            welcomeTitle = FrameTitleUtil.buildTitle();
        }

        Window welcomeFrame = Window.create(
            welcomeTitle,
            builder.build()
        );
        welcomeFrame.setSize(WelcomeFrameManager.getDefaultWindowSize());

        AnAction[] recentProjectsActions = myRecentProjectsManager.get().getRecentProjectsActions(false);

        List<String> pathsToCheck = new ArrayList<>();
        for (AnAction action : recentProjectsActions) {
            if (action instanceof ReopenProjectAction reopenProjectAction) {
                pathsToCheck.add(reopenProjectAction.getProjectPath());
            }
        }

        RecentProjectsChecker checker = RecentProjectsChecker.getInstance();
        Runnable checkerCallback = () -> {
        };
        checker.addCallback(checkerCallback, pathsToCheck);

        Disposable uiDisposable = Disposable.newDisposable("welcome screen panel");

        welcomeFrame.addCloseListener(event -> {
            checker.removeCallback(checkerCallback);
            Disposer.dispose(uiDisposable);
            frameClosed();
        });

        TitlelessDecorator titlelessDecorator =
            TitlelessDecoratorService.getInstance().of(welcomeFrame, TitlelessDecorator.WELCOME_WINDOW);

        BaseUnifiedWelcomeScreenPanel panel =
            new BaseUnifiedWelcomeScreenPanel(uiDisposable, myDataManager, myActionManager, titlelessDecorator) {
            @Override
            public void setTitle(LocalizeValue title) {
                welcomeFrame.setTitle(title.get());
            }

            @Override
            @RequiredUIAccess
            public void removeSlide(Layout target) {
                super.removeSlide(target);

                welcomeFrame.setTitle(welcomeTitle);
            }

            @Override
            @RequiredUIAccess
            protected Component createLeftComponent(Disposable parentDisposable) {
                return createRecentProjectsList(recentProjectsActions, welcomeFrame);
            }
        };

        welcomeFrame.setContent(panel.getComponent());

        return new UnifiedWelcomeIdeFrame(welcomeFrame, myProjectManager.get().getDefaultProject());
    }

    @RequiredUIAccess
    private ListBox<AnAction> createRecentProjectsList(AnAction[] recentProjectsActions, Window welcomeFrame) {
        MutableFlatDataModel<AnAction> model = FlatDataModel.of(Arrays.asList(recentProjectsActions));

        SimpleReference<ListBox<AnAction>> listBoxRef = SimpleReference.create();

        ListBox<AnAction> listSelect = ListBox.create(model);
        listBoxRef.set(listSelect);
        listSelect.setRender(new RecentProjectItemRender(
            myRecentProjectsManager.get(),
            RecentProjectsChecker.getInstance(),
            (action, event) -> {
                AnActionEvent e = AnActionEvent.createFromAnAction(
                    action,
                    null,
                    ActionPlaces.WELCOME_SCREEN,
                    myDataManager.getDataContext(welcomeFrame),
                    event.getInputDetails()
                );

                action.actionPerformed(e);
            },
            (action, event) -> showRecentProjectMenu(listBoxRef.get(), action, event)
        ));
        // the menu is built against the list, so what the actions of the menu work on is published by it
        listSelect.putUserData(
            UiDataProvider.KEY,
            sink -> sink.set(RecentProjectsWelcomeScreenActionBase.RECENT_PROJECTS_LIST, listSelect)
        );

        listSelect.setSelectOnHover(true);
        listSelect.setPlaceholder(ProjectLocalize.recentProjectsNoProjectOpenYet());
        listSelect.setItemHeightGetter(action -> RecentProjectItemRender.ROW_HEIGHT);
        listSelect.setSize(RecentProjectItemRender.LIST_WIDTH, null);
        return listSelect;
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
