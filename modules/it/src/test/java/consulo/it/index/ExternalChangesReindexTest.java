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
package consulo.it.index;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.event.DumbModeListenerBackgroundable;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the external-change scenario which used to cycle refresh and indexing: an external tool rewrites many
 * files at once, the VFS refresh publishes the changes and the index must catch up in a small, bounded number of dumb
 * mode passes instead of feeding itself new work forever.
 * <p>
 * The count of changed files is deliberately above {@code ide.dumb.mode.minFilesToStart} (20), so the changed-files
 * path has to go through a dumb mode reindex ({@code FileBasedIndexProjectHandler.scheduleReindexingInDumbMode})
 * rather than a lazy update.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ExternalChangesReindexTest {
    private static final long TIMEOUT_SECONDS = 60;
    private static final int FILES = 30;

    /**
     * See {@code ProjectStateReloadTest} - the refresh makes the platform fire VFS events on the UI thread,
     * where the pointer manager and the indexing listeners break their own threading assertions in a headless
     * application. Unrelated to the behavior under test; any other logged error still fails it.
     */
    @AllowLogError({"consulo.virtualFileSystem.internal.BaseVirtualFileManager", "consulo.application.impl.internal.BaseApplication"})
    @Test
    public void externalMassChangeCausesBoundedReindex(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-external-reindex");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        for (int i = 0; i < FILES; i++) {
            Files.writeString(src.resolve("file" + i + ".txt"), "hello " + i);
        }

        Project project = projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(project).isNotNull();

        VirtualFile directoryFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(directoryFile).isNotNull();

        ModuleManager moduleManager = ModuleManager.getInstance(project);
        WriteAction.run(() -> {
            ModifiableModuleModel moduleModel = moduleManager.getModifiableModel();
            Module module = moduleModel.newModule("main", directory.toString());
            moduleModel.commit();

            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            rootModel.addContentEntry(directoryFile);
            rootModel.commit();
        });

        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);

        AtomicInteger dumbModeEntries = new AtomicInteger();
        project.getMessageBus().connect().subscribe(
            DumbModeListenerBackgroundable.class,
            new DumbModeListenerBackgroundable() {
                @Override
                public void enteredDumbMode() {
                    dumbModeEntries.incrementAndGet();
                }
            }
        );

        // an external tool rewrites every file at once
        for (int i = 0; i < FILES; i++) {
            Files.writeString(src.resolve("file" + i + ".txt"), "changed content of file " + i);
        }

        VirtualFile srcFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(src);
        assertThat(srcFile).isNotNull();
        srcFile.refresh(false, true);

        // the changed-files reindex is scheduled from a background worker after the events settle
        waitFor(() -> dumbModeEntries.get() >= 1);
        awaitSmart(dumbService);

        // give a refresh/index feedback loop a chance to expose itself, then require the count to be small and stable
        int cyclesAfterReindex = dumbModeEntries.get();
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        awaitSmart(dumbService);

        assertThat(dumbModeEntries.get())
            .as("dumb mode kept cycling after the reindex - refresh and indexing feed each other")
            .isEqualTo(cyclesAfterReindex);
        assertThat(cyclesAfterReindex)
            .as("a single external change wave must be absorbed by a couple of dumb mode passes")
            .isLessThanOrEqualTo(3);
        assertThat(dumbService.isDumb()).isFalse();
    }

    private static void awaitSmart(DumbService dumbService) throws InterruptedException {
        CountDownLatch smart = new CountDownLatch(1);
        dumbService.runWhenSmart(smart::countDown);
        assertThat(smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("project must reach smart mode").isTrue();
    }

    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        assertThat(condition.getAsBoolean()).as("timed out waiting for condition").isTrue();
    }
}

