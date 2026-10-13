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
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.Notifications;
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
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ActionImpl(id = "SandCheckPlatformFiles", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class CheckPlatformFilesAction extends DumbAwareAction {
    private final ServerConnectionManager myServerConnectionManager;
    private final NotificationService myNotificationService;

    @Inject
    public CheckPlatformFilesAction(ServerConnectionManager serverConnectionManager, NotificationService notificationService) {
        super(LocalizeValue.localizeTODO("Check Files API On Platforms"));
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

        new Task.Backgroundable(project, LocalizeValue.localizeTODO("Checking Files API"), true) {
            private final List<String> myResults = new ArrayList<>();

            @Override
            public void run(ProgressIndicator indicator) {
                for (Platform platform : platforms) {
                    myResults.add(platform.getName() + ": " + check(platform));
                }
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                myNotificationService.newInfo(Notifications.SYSTEM_MESSAGES_GROUP)
                    .title(LocalizeValue.localizeTODO("Files API on platforms"))
                    .content(LocalizeValue.localizeTODO(String.join("\n", myResults)))
                    .notify(project);
            }
        }.queue();
    }

    private static String check(Platform platform) {
        Path directory = platform.user().homePath().resolve("consulo-files-check");
        List<String> steps = new ArrayList<>();
        try {
            Files.createDirectories(directory);
            Files.createDirectories(directory);
            steps.add("createDirectories");

            try {
                Files.createDirectory(directory);
                steps.add("createDirectory: no FileAlreadyExistsException");
            }
            catch (FileAlreadyExistsException ignored) {
                steps.add("createDirectory exists");
            }

            Path file = directory.resolve("hello.txt");
            Files.writeString(file, "hello\n");
            Files.writeString(file, "world\n", StandardOpenOption.APPEND);
            String text = Files.readString(file);
            steps.add("writeString+append+readString=" + text.replace("\n", "|"));

            try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
                steps.add("newByteChannel size=" + channel.size());
            }

            steps.add("size=" + Files.size(file) + " regular=" + Files.isRegularFile(file) + " dir=" + Files.isDirectory(directory));
            try (Stream<Path> children = Files.list(directory)) {
                steps.add("list=" + children.map(p -> p.getFileName().toString()).toList());
            }

            Path shell = platform.findExecutableInPath("sh");
            steps.add("sh=" + shell + " executable=" + (shell != null && Files.isExecutable(shell))
                + " writableFile=" + Files.isWritable(file));

            Files.delete(file);
            steps.add("delete");
            steps.add("deleteIfExists(missing)=" + Files.deleteIfExists(file));
            try {
                Files.delete(file);
                steps.add("delete missing: no NoSuchFileException");
            }
            catch (NoSuchFileException ignored) {
                steps.add("delete missing NoSuchFileException");
            }
            Files.delete(directory);
            steps.add("exists after delete=" + Files.exists(directory));
            return String.join(", ", steps);
        }
        catch (IOException | RuntimeException ex) {
            return String.join(", ", steps) + " FAILED: " + ex;
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
