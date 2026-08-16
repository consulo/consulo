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
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.application.FrameTitleUtil;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ide.impl.welcomeScreen.BaseUnifiedWelcomeScreenPanel;
import consulo.ide.impl.wm.impl.UnifiedWelcomeIdeFrame;
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
import consulo.ui.Size2D;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.layout.Layout;
import consulo.ui.model.FlatDataModel;
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

        TitlelessDecorator titlelessDecorator = TitlelessDecorator.NOTHING;

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
        FlatDataModel<AnAction> model = FlatDataModel.of(Arrays.asList(recentProjectsActions));

        ListBox<AnAction> listSelect = ListBox.create(model);
        listSelect.setRender((renderer, renderItem) -> {
            ReopenProjectAction action = (ReopenProjectAction) renderItem.getValue();
            if (action.isOpened()) {
                renderer.append(ProjectLocalize.recentProject0OpenedActionText(LocalizeValue.of(action.getProjectName())));
                return;
            }

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
        listSelect.setSize(new Size2D(300, -1));
        return listSelect;
    }
}
