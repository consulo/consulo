/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal;

import consulo.ui.Component;
import consulo.component.messagebus.MessageBusConnection;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentUtilEx;
import consulo.ui.ex.content.ContentsUtil;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.versionControlSystem.change.ChangesViewContentProvider;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.ui.layout.DockLayout;
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import org.jspecify.annotations.Nullable;


/**
 * Provides the Content tab to the ChangesView log toolwindow.
 * <p/>
 * Delegates to the VcsLogManager.
 */
public class VcsLogContentProvider implements ChangesViewContentProvider {
    public static final String TAB_NAME = "Log";

    
    private final Project myProject;
    
    private final VcsProjectLogImpl myProjectLog;
    
    private final DockLayout myContainer;

    @RequiredUIAccess
    public VcsLogContentProvider(Project project, VcsProjectLogImpl projectLog) {
        myProject = project;
        myProjectLog = projectLog;
        myContainer = DockLayout.create();

        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(ProjectLogListener.class, new ProjectLogListener() {
            @Override
            @RequiredUIAccess
            public void logCreated() {
                addLogUi();
            }

            @Override
            @RequiredUIAccess
            public void logDisposed() {
                myContainer.removeAll();
                closeLogTabs();
            }
        });

        if (myProjectLog.getLogManager() != null) {
            addLogUi();
        }
    }

    @RequiredUIAccess
    private void addLogUi() {
        myContainer.center(myProjectLog.initMainLog(TAB_NAME));
    }

    @Override
    @RequiredUIAccess
    public Component initContent() {
        myProjectLog.createLog();
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.VCS);
        if (toolWindow != null) {
            myProjectLog.toolWindowShown(toolWindow);
        }
        return myContainer;
    }

    @Override
    public void disposeContent() {
        myContainer.removeAll();
        closeLogTabs();
    }

    @RequiredUIAccess
    public static AbstractVcsLogUi openLogTab(
        VcsLogManager logManager,
        Project project,
        String shortName,
        @Nullable VcsLogFilter filter
    ) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS);

        String name = ContentUtilEx.getFullName(TAB_NAME, shortName);

        AbstractVcsLogUi logUi = logManager.createLogUi(name, name, filter);

        Content content = ContentFactory.getInstance().createUIContent(logUi.createPanel(logManager), name, false);
        content.putUserData(Content.TAB_GROUP_NAME_KEY, TAB_NAME);
        content.setDisposer(logUi);
        toolWindow.getContentManager().addContent(content);
        toolWindow.getContentManager().setSelectedContent(content);
        toolWindow.activate(null);

        logManager.scheduleInitialization();
        return logUi;
    }

    private void closeLogTabs() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.VCS);

        if (toolWindow != null) {
            for (Content content : toolWindow.getContentManager().getContents()) {
                if (ContentUtilEx.isContentTab(content, TAB_NAME)) {
                    ContentsUtil.closeContentTab(toolWindow.getContentManager(), content);
                }
            }
        }
    }
}
