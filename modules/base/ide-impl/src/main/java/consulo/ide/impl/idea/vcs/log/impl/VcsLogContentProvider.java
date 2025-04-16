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
package consulo.ide.impl.idea.vcs.log.impl;

import consulo.component.messagebus.MessageBusConnection;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentProvider;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogPanel;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBPanel;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentUtilEx;
import consulo.ui.ex.content.ContentsUtil;
import consulo.ui.ex.content.TabbedContent;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.versionControlSystem.log.VcsLogFilter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Provides the Content tab to the ChangesView log toolwindow.
 * <p/>
 * Delegates to the VcsLogManager.
 */
public class VcsLogContentProvider implements ChangesViewContentProvider {
    public static final String TAB_NAME = "Log";

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final VcsProjectLog myProjectLog;
    @Nonnull
    private final JPanel myContainer = new JBPanel(new BorderLayout());

    public VcsLogContentProvider(@Nonnull Project project, @Nonnull VcsProjectLog projectLog) {
        myProject = project;
        myProjectLog = projectLog;

        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(ProjectLogListener.class, new ProjectLogListener() {
            @Override
            public void logCreated() {
                addLogUi();
            }

            @Override
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
        myContainer.add(myProjectLog.initMainLog(TAB_NAME), BorderLayout.CENTER);
    }

    @Override
    public JComponent initContent() {
        myProjectLog.createLog();
        return myContainer;
    }

    @Override
    public void disposeContent() {
        myContainer.removeAll();
        closeLogTabs();
    }

    public static void openAnotherLogTab(@Nonnull VcsLogManager logManager, @Nonnull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS);
        openLogTab(logManager, project, generateShortName(toolWindow), null);
    }

    public static VcsLogUiImpl openLogTab(
        @Nonnull VcsLogManager logManager,
        @Nonnull Project project,
        @Nonnull String shortName,
        @Nullable VcsLogFilter filter
    ) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS);

        String name = ContentUtilEx.getFullName(TAB_NAME, shortName);

        VcsLogUiImpl logUi = logManager.createLogUi(name, name, filter);

        ContentUtilEx
            .addTabbedContent(toolWindow.getContentManager(), new VcsLogPanel(logManager, logUi), TAB_NAME, shortName, true, logUi);
        toolWindow.activate(null);

        logManager.scheduleInitialization();
        return logUi;
    }

    @Nonnull
    private static String generateShortName(@Nonnull ToolWindow toolWindow) {
        TabbedContent tabbedContent = ContentUtilEx.findTabbedContent(toolWindow.getContentManager(), TAB_NAME);
        if (tabbedContent != null) {
            return String.valueOf(tabbedContent.getTabs().size() + 1);
        }
        else {
            List<Content> contents = ContainerUtil.filter(
                toolWindow.getContentManager().getContents(),
                content -> TAB_NAME.equals(content.getUserData(Content.TAB_GROUP_NAME_KEY))
            );
            return String.valueOf(contents.size() + 1);
        }
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
