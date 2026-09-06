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
package consulo.it.project;

import consulo.application.Application;
import consulo.component.messagebus.MessageBusConnection;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.index.impl.internal.UnindexedFilesScannerStartup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.event.ProjectManagerListener;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.ui.UIAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A project is dumb from the moment it is opened until the initial dumb task and the first scan it queues are done;
 * {@code runWhenSmart} callbacks registered at that point run afterwards.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ProjectStartsDumbTest {
    private static final long TIMEOUT_SECONDS = 60;

    @Test
    public void projectIsDumbWhenOpenedUntilFirstScanCompletes(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-project-starts-dumb");

        AtomicReference<Boolean> dumbAtOpen = new AtomicReference<>();
        AtomicReference<Boolean> canRunSmartAtOpen = new AtomicReference<>();
        CountDownLatch smart = new CountDownLatch(1);
        AtomicBoolean dumbWhenSmart = new AtomicBoolean(true);
        AtomicBoolean scanningWhenSmart = new AtomicBoolean(true);
        AtomicBoolean firstScanPerformedWhenSmart = new AtomicBoolean();

        MessageBusConnection connection = application.getMessageBus().connect();
        connection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectOpened(Project project, UIAccess uiAccess) {
                String basePath = project.getBasePath();
                if (basePath == null || !Path.of(basePath).equals(directory)) {
                    return;
                }
                DumbService dumbService = DumbService.getInstance(project);
                dumbAtOpen.set(dumbService.isDumb());
                canRunSmartAtOpen.set(dumbService.canRunSmart());
                dumbService.runWhenSmart(() -> {
                    dumbWhenSmart.set(dumbService.isDumb());
                    scanningWhenSmart.set(UnindexedFilesScannerExecutor.getInstance(project).isRunning().get());
                    firstScanPerformedWhenSmart.set(UnindexedFilesScannerStartup.isFirstProjectScanningPerformed(project));
                    smart.countDown();
                });
            }
        });

        try {
            Project project = projectManager
                .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(project).isNotNull();

            assertThat(dumbAtOpen.get()).as("projectOpened must be published for the test project").isNotNull();
            assertThat(dumbAtOpen.get()).as("a freshly opened project must be dumb").isTrue();
            assertThat(canRunSmartAtOpen.get()).as("smart mode must not be reachable before the initial dumb task ran").isFalse();

            assertThat(smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("runWhenSmart registered on project open must run once the project is smart")
                .isTrue();
            assertThat(dumbWhenSmart).isFalse();
            assertThat(scanningWhenSmart).as("the initial scan must be over before smart mode").isFalse();
            assertThat(firstScanPerformedWhenSmart).as("the initial scan must have been performed before smart mode").isTrue();
            assertThat(UnindexedFilesScannerStartup.isFirstProjectScanningPerformed(project)).isTrue();
        }
        finally {
            connection.disconnect();
        }
    }
}
