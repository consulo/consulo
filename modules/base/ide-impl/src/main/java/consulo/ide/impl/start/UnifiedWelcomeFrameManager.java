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
import consulo.ide.impl.application.FrameTitleUtil;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ide.impl.wm.impl.UnifiedWelcomeIdeFrame;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.project.ProjectManager;
import consulo.project.internal.RecentProjectsChecker;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionRunnerAsync;
import consulo.ui.ex.action.*;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.ListModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedWelcomeFrameManager extends WelcomeFrameManager {
    private final ProjectManager myProjectManager;
    private final RecentProjectsManager myRecentProjectsManager;
    private final DataManager myDataManager;

    @Inject
    public UnifiedWelcomeFrameManager(
        Application application,
        ProjectManager projectManager,
        RecentProjectsManager recentProjectsManager,
        DataManager dataManager
    ) {
        super(application);
        myProjectManager = projectManager;
        myRecentProjectsManager = recentProjectsManager;
        myDataManager = dataManager;
    }

    @RequiredUIAccess
    @Override
    public void closeFrame() {
        super.closeFrame();
        frameClosed();
    }

    @RequiredUIAccess
    
    @Override
    public IdeFrame createFrame() {
        Window welcomeFrame = Window.create(FrameTitleUtil.buildTitle(), WindowOptions.builder().disableResize().build());
        welcomeFrame.setSize(WelcomeFrameManager.getDefaultWindowSize());
        welcomeFrame.setContent(Label.create("Loading..."));

        AnAction[] recentProjectsActions = myRecentProjectsManager.getRecentProjectsActions(false);

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
        welcomeFrame.addCloseListener(event -> {
            checker.removeCallback(checkerCallback);
            frameClosed();
        });

        ListModel<AnAction> model = ListModel.of(Arrays.asList(recentProjectsActions));

        ListBox<AnAction> listSelect = ListBox.create(model);
        listSelect.setRenderer((renderer, index, item) -> {
            ReopenProjectAction action = (ReopenProjectAction) item;
            renderer.append(action.getProjectName());
            String branch = RecentProjectsChecker.getInstance().getBranch(action.getProjectPath());
            if (branch != null && !branch.isEmpty()) {
                renderer.append(" [" + branch + "]");
            }
        });
        listSelect.addValueListener(event -> {
            ReopenProjectAction value = (ReopenProjectAction) event.getValue();

            AnActionEvent e =
                AnActionEvent.createFromAnAction(value, null, ActionPlaces.WELCOME_SCREEN, myDataManager.getDataContext(welcomeFrame));

            value.actionPerformed(e);
        });
        listSelect.addBorder(BorderPosition.RIGHT);
        listSelect.setSize(new Size2D(300, -1));

        DockLayout layout = DockLayout.create();
        layout.left(listSelect);

        VerticalLayout projectActionLayout = VerticalLayout.create();

        VerticalLayout quickStartLayout = VerticalLayout.create();
        projectActionLayout.add(quickStartLayout);

        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup quickStart = (ActionGroup) actionManager.getAction(IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART);
        List<AnAction> group = new ArrayList<>();
        collectAllActions(group, quickStart);

        List<AnActionEvent> events = new ArrayList<>(group.size());
        List<CompletableFuture<?>> updates = new ArrayList<>(group.size());
        for (AnAction action : group) {
            AnActionEvent e =
                AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, myDataManager.getDataContext(welcomeFrame));
            events.add(e);
            updates.add(ActionRunnerAsync.performDumbAwareUpdateAsync(action, e));
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.allOf(updates.toArray(new CompletableFuture[0])).whenCompleteAsync((r, throwable) -> {
            for (int i = 0; i < group.size(); i++) {
                AnAction action = group.get(i);
                AnActionEvent e = events.get(i);

                Presentation presentation = e.getPresentation();
                if (presentation.isVisible()) {
                    LocalizeValue text = presentation.getTextValue();

                    Hyperlink component = Hyperlink.create(text, (event) -> action.actionPerformed(e));

                    component.setIcon(presentation.getIcon());

                    quickStartLayout.add(component);
                }
            }
        }, uiAccess);

        projectActionLayout.add(Button.create(
            "Settings",
            (e) -> ShowSettingsUtil.getInstance().showSettingsDialog(null)
        ));

        layout.center(projectActionLayout);

        welcomeFrame.setContent(layout);

        return new UnifiedWelcomeIdeFrame(welcomeFrame, myProjectManager.getDefaultProject());
    }

    public static void collectAllActions(List<AnAction> group, ActionGroup actionGroup) {
        for (AnAction action : actionGroup.getChildren(null)) {
            if (action instanceof ActionGroup && !((ActionGroup) action).isPopup()) {
                collectAllActions(group, (ActionGroup) action);
            }
            else {
                group.add(action);
            }
        }
    }
}
