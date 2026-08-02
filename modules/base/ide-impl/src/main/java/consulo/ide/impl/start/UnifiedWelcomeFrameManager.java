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
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ProjectManager;
import consulo.project.internal.RecentProjectsChecker;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.*;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.ex.internal.LogoImage;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.ListModel;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedWelcomeFrameManager extends WelcomeFrameManager {
    private final Provider<ProjectManager> myProjectManager;
    // asking for the instance here rather than at the frame would have the platform build the service - and
    // read its stored state - while the application is still coming up, and it answers no recent project at all
    private final Provider<RecentProjectsManager> myRecentProjectsManager;
    private final DataManager myDataManager;

    @Inject
    public UnifiedWelcomeFrameManager(
        Application application,
        Provider<ProjectManager> projectManager,
        Provider<RecentProjectsManager> recentProjectsManager,
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
        WindowOptions.Builder builder = WindowOptions.builder();
        String welcomeTitle = FrameTitleUtil.buildTitle();

        // disable close and resize, and remove title, we not allow it in web mode
        if (Platform.current().isInBrowser()) {
            builder.disableClose().disableResize();
            welcomeTitle = "";
        }

        Window welcomeFrame = Window.create(
            welcomeTitle,
            builder.build()
        );
        welcomeFrame.setSize(WelcomeFrameManager.getDefaultWindowSize());
        welcomeFrame.setContent(Label.create("Loading..."));

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
        listSelect.addBorder(BorderPosition.RIGHT, BorderStyle.LINE, ComponentColors.BORDER, 1);
        listSelect.setSize(new Size2D(300, -1));

        DockLayout layout = DockLayout.create();
        layout.left(listSelect);

        // the awt screen stacks the logo and every entry in a single column and centres each of them on the
        // width of the widest, rather than stretching them
        VerticalLayout rightLayout = VerticalLayout.create(0, HorizontalAlignment.CENTER);

        ImageBox logo = ImageBox.create(LogoImage.create(8, StandardColors.GRAY));
        // the insets the awt screen keeps around its logo
        logo.addBorder(BorderPosition.TOP, BorderStyle.EMPTY, null, 53);
        logo.addBorder(BorderPosition.BOTTOM, BorderStyle.EMPTY, null, 45);
        rightLayout.add(logo);

        VerticalLayout actionLayout = VerticalLayout.create(0, HorizontalAlignment.CENTER);
        rightLayout.add(actionLayout);

        // quick start is filled once the platform has answered which of its actions are visible, so it is a
        // layout of its own - otherwise it would land after the two group entries
        VerticalLayout quickStartLayout = VerticalLayout.create(0, HorizontalAlignment.CENTER);
        actionLayout.add(quickStartLayout);

        UIAccess uiAccess = UIAccess.current();

        addActionGroup(quickStartLayout, IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART, welcomeFrame, uiAccess);

        // the awt screen keeps these two as groups behind a single entry rather than listing what is in them
        actionLayout.add(createGroupButton(
            LocalizeValue.localizeTODO("Configure"),
            IdeActions.GROUP_WELCOME_SCREEN_CONFIGURE,
            PlatformIconGroup.welcomePreferences()
        ));

        actionLayout.add(createGroupButton(
            LocalizeValue.localizeTODO("Get Help"),
            IdeActions.GROUP_WELCOME_SCREEN_DOC,
            PlatformIconGroup.welcomeHelp()
        ));

        DockLayout rightDock = DockLayout.create();
        rightDock.top(rightLayout);

        layout.center(rightDock);

        welcomeFrame.setContent(layout);

        return new UnifiedWelcomeIdeFrame(welcomeFrame, myProjectManager.get().getDefaultProject());
    }

    /**
     * A group stays a group - the entry opens it as a popup, the way the awt welcome screen does.
     */
    @RequiredUIAccess
    private Button createGroupButton(LocalizeValue text, String groupId, Image icon) {
        Button button = Button.create(text, event -> {
            ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction(groupId);
            if (group == null) {
                return;
            }

            InputDetails inputDetails = Objects.requireNonNull(event.getInputDetails());

            ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.WELCOME_SCREEN, group);
            menu.show(event.getComponent(), inputDetails.getX(), inputDetails.getY());
        });
        button.addStyle(ButtonStyle.BORDERLESS);
        button.setIcon(icon);
        return button;
    }

    /**
     * Every action of the group as a link, once the platform has told which of them are visible.
     */
    @RequiredUIAccess
    private void addActionGroup(VerticalLayout target, String groupId, Window welcomeFrame, UIAccess uiAccess) {
        ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction(groupId);
        if (actionGroup == null) {
            return;
        }

        List<AnAction> group = new ArrayList<>();
        collectAllActions(group, actionGroup);

        List<AnActionEvent> events = new ArrayList<>(group.size());
        List<CompletableFuture<?>> updates = new ArrayList<>(group.size());
        for (AnAction action : group) {
            AnActionEvent e =
                AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, myDataManager.getDataContext(welcomeFrame));
            events.add(e);
            updates.add(ActionRunnerAsync.performDumbAwareUpdateAsync(action, e));
        }

        CompletableFuture.allOf(updates.toArray(new CompletableFuture[0])).whenCompleteAsync((r, throwable) -> {
            for (int i = 0; i < group.size(); i++) {
                AnAction action = group.get(i);
                AnActionEvent e = events.get(i);

                Presentation presentation = e.getPresentation();
                if (presentation.isVisible()) {
                    Button component = Button.create(presentation.getTextValue(), (event) -> action.actionPerformed(e));
                    component.addStyle(ButtonStyle.BORDERLESS);
                    component.setIcon(presentation.getIcon());

                    target.add(component);
                }
            }
        }, uiAccess);
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
