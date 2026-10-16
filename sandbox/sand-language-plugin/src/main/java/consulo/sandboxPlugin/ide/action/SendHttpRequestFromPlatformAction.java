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
package consulo.sandboxPlugin.ide.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.http.HttpMethod;
import consulo.http.HttpRequestBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.NotificationGroup;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.platformAware.PlatformAwareServerRuntimeInstance;
import consulo.remoteServer.runtime.ConnectionStatus;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.deployment.ServerRuntimeInstance;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ActionImpl(id = "SandSendHttpRequestFromPlatform", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class SendHttpRequestFromPlatformAction extends DumbAwareAction {
    private static final List<String> URLS = List.of("https://postman-echo.com/get?from=consulo", "http://localhost:63342/");

    private final HttpRequestBuilderFactory myHttpRequestBuilderFactory;
    private final ServerConnectionManager myServerConnectionManager;
    private final NotificationService myNotificationService;

    @Inject
    public SendHttpRequestFromPlatformAction(HttpRequestBuilderFactory httpRequestBuilderFactory,
                                             ServerConnectionManager serverConnectionManager,
                                             NotificationService notificationService) {
        super(LocalizeValue.localizeTODO("Send HTTP Request From Platform"));
        myHttpRequestBuilderFactory = httpRequestBuilderFactory;
        myServerConnectionManager = serverConnectionManager;
        myNotificationService = notificationService;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        List<Platform> platforms = new ArrayList<>();
        platforms.add(Platform.current());
        for (ServerConnection<?> connection : myServerConnectionManager.getConnections()) {
            if (connection.getStatus() == ConnectionStatus.CONNECTED) {
                addPlatform(connection, platforms);
            }
        }

        new Task.Backgroundable(project, LocalizeValue.localizeTODO("Sending HTTP requests"), true) {
            private final List<String> myResults = new ArrayList<>();

            @Override
            public void run(ProgressIndicator indicator) {
                for (Platform platform : platforms) {
                    for (String url : URLS) {
                        myResults.add(platform.getName() + " -> " + url + ": " + send(platform, url));
                    }
                }
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                myNotificationService.newInfo(NotificationGroup.SYSTEM_MESSAGES)
                    .title(LocalizeValue.localizeTODO("HTTP requests from platforms"))
                    .content(LocalizeValue.localizeTODO(String.join("\n", myResults)))
                    .notify(project);
            }
        }.queue();
    }

    private String send(Platform platform, String url) {
        try {
            return myHttpRequestBuilderFactory.newBuilder(platform, url, HttpMethod.GET)
                .allowErrorCodes(true)
                .connect(request -> request.statusCode() + " " + StringUtil.first(request.readString(null), 120, true));
        }
        catch (IOException | UnsupportedOperationException e) {
            return "failed: " + e.getMessage();
        }
    }

    private static <D extends DeploymentConfiguration> void addPlatform(ServerConnection<D> connection, List<Platform> platforms) {
        connection.connectIfNeeded(new ServerConnector.ConnectionCallback<>() {
            @Override
            public void connected(ServerRuntimeInstance<D> instance) {
                if (instance instanceof PlatformAwareServerRuntimeInstance<D> platformAwareInstance) {
                    platforms.add(platformAwareInstance.getPlatform());
                }
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
            }
        });
    }
}
