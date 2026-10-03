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
package consulo.execution.profiler.impl.internal.toolwindow;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposer;
import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.ProfilerToolWindowManager;
import consulo.execution.profiler.configuration.ProfilerRunConfigurationManager;
import consulo.execution.profiler.impl.internal.session.ProfilerSession;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionLayout;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionListener;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionManager;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionPanel;
import consulo.execution.profiler.impl.internal.setting.ProfilerSessionHost;
import consulo.execution.profiler.impl.internal.setting.ProfilerUISettings;
import consulo.execution.profiler.impl.internal.snapshot.ProfilerSnapshotService;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@Singleton
@ServiceImpl
public class ProfilerToolWindowManagerImpl implements ProfilerToolWindowManager {
    private static final Logger LOG = Logger.getInstance(ProfilerToolWindowManagerImpl.class);

    private final Project myProject;
    private final Application myApplication;
    private final Provider<ToolWindowManager> myToolWindowManager;
    private final Provider<ProfilerSnapshotService> mySnapshotService;
    private final ProfilerRunConfigurationManager myConfigurationsManager;
    private final ContentFactory myContentFactory;
    private final ProfilerSessionManager mySessionManager;
    private final ProfilerUISettings myUISettings;
    private final Map<ProfilerSession, Content> mySessionContents = new IdentityHashMap<>();

    @Inject
    public ProfilerToolWindowManagerImpl(
        Project project,
        Application application,
        Provider<ToolWindowManager> toolWindowManager,
        Provider<ProfilerSnapshotService> snapshotService,
        ProfilerRunConfigurationManager configurationsManager,
        ContentFactory contentFactory,
        ProfilerSessionManager sessionManager,
        ProfilerUISettings uiSettings
    ) {
        myProject = project;
        myApplication = application;
        myToolWindowManager = toolWindowManager;
        mySnapshotService = snapshotService;
        myConfigurationsManager = configurationsManager;
        myContentFactory = contentFactory;
        mySessionManager = sessionManager;
        myUISettings = uiSettings;
    }

    @RequiredUIAccess
    public void initToolWindow(ToolWindow toolWindow) {
        ProfilerHomePanel homePanel = new ProfilerHomePanel(
            myProject,
            myApplication,
            myConfigurationsManager,
            mySnapshotService.get(),
            mySessionManager,
            this
        );

        Content content = myContentFactory.createUIContent(homePanel.getComponent(), LocalizeValue.localizeTODO("Home").get(), false);
        content.setCloseable(false);
        content.setDisposer(homePanel);

        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content, 0);

        Disposer.register(homePanel, mySessionManager.addListener(new ProfilerSessionListener() {
            @Override
            @RequiredUIAccess
            public void sessionRemoved(ProfilerSession session) {
                Content sessionContent = mySessionContents.get(session);
                if (sessionContent != null) {
                    contentManager.removeContent(sessionContent, true);
                }
            }
        }));
    }

    @Override
    public void addProfilerProcessTab(ProfilerProcess<?> process, boolean activate) {
        myProject.getUIAccess().giveIfNeed(() -> {
            if (myProject.isDisposed()) {
                return;
            }

            ProfilerSession session = mySessionManager.getOrCreateSession(process);
            if (session != null) {
                showSession(session, activate);
            }
        });
    }

    @RequiredUIAccess
    public void showSession(ProfilerSession session, boolean activate) {
        if (myProject.isDisposed() || session.isDisposed()) {
            return;
        }

        if (mySessionManager.isSessionEditorOpen(session)) {
            if (activate) {
                mySessionManager.openSessionEditor(session, true);
            }
            return;
        }

        Content content = mySessionContents.get(session);
        if (content != null && content.isValid()) {
            showInToolWindow(session, activate);
            return;
        }

        if (myUISettings.getSessionHost() == ProfilerSessionHost.TOOL_WINDOW) {
            showInToolWindow(session, activate);
        }
        else if (activate) {
            mySessionManager.openSessionEditor(session, true);
        }
    }

    @RequiredUIAccess
    public void bringSessionToFront(ProfilerSession session) {
        if (mySessionManager.isSessionEditorOpen(session)) {
            mySessionManager.openSessionEditor(session, false);
        }
    }

    @RequiredUIAccess
    private void showInToolWindow(ProfilerSession session, boolean activate) {
        ToolWindow toolWindow = myToolWindowManager.get().getToolWindow(TOOLWINDOW_ID);
        if (toolWindow == null) {
            LOG.warn("The " + TOOLWINDOW_ID + " tool window is not registered; opening " + session + " in an editor tab");
            mySessionManager.openSessionEditor(session, activate);
            return;
        }

        ContentManager contentManager = toolWindow.getContentManager();

        Content content = mySessionContents.get(session);
        if (content == null || !content.isValid()) {
            content = createSessionContent(session);
            contentManager.addContent(content);
        }

        if (activate) {
            contentManager.setSelectedContent(content);
            toolWindow.activate(null);
        }
    }

    @RequiredUIAccess
    private Content createSessionContent(ProfilerSession session) {
        ProfilerSessionPanel panel = mySessionManager.createPanel(session, ProfilerSessionLayout.COMPACT);

        String name = session.getTitle();
        Content content = myContentFactory.createUIContent(panel.getComponent(), name, false);
        content.setCloseable(true);
        content.setDescription(name);
        content.setIcon(session.getIcon());

        mySessionContents.put(session, content);
        Disposer.register(panel, () -> mySessionContents.remove(session, content));
        content.setDisposer(panel);
        return content;
    }
}
