/*
 * Copyright 2013-2025 consulo.io
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
package consulo.welcomeScreen.impl.internal.editor;

import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.internal.VetoActionCloseHandler;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.project.Project;
import consulo.project.internal.ProjectOpenSetting;
import consulo.project.ProjectType;
import consulo.project.RecentProjectsManager;
import consulo.project.internal.RecentProjectsChecker;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.virtualFileSystem.VirtualFile;
import consulo.welcomeScreen.impl.internal.WelcomeScreenPanel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-17
 */
public class WelcomeConfigurationFileEditor extends ConfigurationFileEditor implements VetoActionCloseHandler {
    private final RecentProjectsManager myRecentProjectsManager;
    private final DataManager myDataManager;
    private final ActionManager myActionManager;
    private final ProjectOpenSetting myProjectOpenSetting;

    private final Disposable myUIDisposable = Disposable.newDisposable("welcome tab");
    private final Runnable myCheckerCallback = () -> {
    };

    private @Nullable Component myComponent;

    public WelcomeConfigurationFileEditor(
        Project project,
        VirtualFile virtualFile,
        RecentProjectsManager recentProjectsManager,
        DataManager dataManager,
        ActionManager actionManager,
        ProjectOpenSetting projectOpenSetting
    ) {
        super(project, virtualFile);
        virtualFile.putUserData(FileEditorManager.FORBID_TAB_SPLIT, Boolean.TRUE);
        myRecentProjectsManager = recentProjectsManager;
        myDataManager = dataManager;
        myActionManager = actionManager;
        myProjectOpenSetting = projectOpenSetting;
    }

    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        AnAction[] recentProjectsActions = myRecentProjectsManager.getRecentProjectsActions(false);

        List<String> pathsToCheck = new ArrayList<>();
        for (AnAction action : recentProjectsActions) {
            if (action instanceof ReopenProjectAction reopenProjectAction) {
                pathsToCheck.add(reopenProjectAction.getProjectPath());
            }
        }
        RecentProjectsChecker.getInstance().addCallback(myCheckerCallback, pathsToCheck);

        WelcomeScreenPanel panel = new WelcomeScreenPanel(
            myUIDisposable,
            myDataManager,
            myActionManager,
            myRecentProjectsManager,
            recentProjectsActions,
            myProjectOpenSetting
        );

        myComponent = component = panel.getComponent();
        return component;
    }

    @Override
    public boolean isClosable(Project project) {
        return project.getType() != ProjectType.WELCOME;
    }

    @Override
    public void dispose() {
        RecentProjectsChecker.getInstance().removeCallback(myCheckerCallback);
        Disposer.dispose(myUIDisposable);
    }
}
