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
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.dumb.IndexNotReadyException;
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
import consulo.sandboxPlugin.lang.psi.stub.SandClassSearch;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Condition-annotated indexing, usage-context dimension: an included file's conditional
 * declarations depend on flags defined by its includer. Both variants are always in the
 * index with their guards; the entry-state environment recorded by the pre-indexing pass
 * selects the active one at query time. Editing the includer only shifts the environment —
 * the included file is never reindexed, which is mandatory because contexts arrive at
 * resolve time when reindexing is impossible.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SandIncludeEntryStateTest {
    private static final long TIMEOUT_SECONDS = 60;

    /**
     * See {@code SandStubIndexTest} — headless UI-thread VFS listeners and sand's action
     * registrations produce known unrelated errors; anything else still fails the test.
     */
    @AllowLogError({
        "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
        "consulo.application.impl.internal.BaseApplication",
        "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
    })
    @Test
    public void includedFileFilteredByIncluderFlags(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-include");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("main.sand"), """
            #flag A
            #include "some.sand"
            class Main {}
            """);
        Files.writeString(src.resolve("some.sand"), """
            #if A
            class WithA {}
            #else
            class NoA {}
            #end
            """);

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

        // some.sand has two contexts: standalone (no flags, NoA active) and the inclusion
        // from main.sand (#flag A, WithA active) - both variants are visible
        waitFor(() -> check(project, true, true));

        // an external tool drops the #flag from the includer - only the environment shifts,
        // no context activates WithA anymore
        Files.writeString(src.resolve("main.sand"), """
            #include "some.sand"
            class Main {}
            """);

        VirtualFile srcFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(src);
        assertThat(srcFile).isNotNull();
        srcFile.refresh(false, true);

        waitFor(() -> check(project, false, true));

        // a requester supplying its own environment still sees the guarded variant -
        // the index carries all possibilities regardless of current contexts
        assertThat(ReadAction.compute(() -> SandClassSearch.matching(project, "WithA", Set.of("A"))))
            .as("requester environment with the flag must select the guarded variant")
            .isNotEmpty();
    }

    /**
     * One read action asserts the context-filtered activity of both variants plus their
     * unconditional presence in the index.
     */
    private static boolean check(Project project, boolean withAActive, boolean noAActive) {
        return ReadAction.compute(() -> {
            try {
                return SandClassSearch.active(project, "WithA").isEmpty() != withAActive
                    && SandClassSearch.active(project, "NoA").isEmpty() != noAActive
                    && !SandClassSearch.allVariants(project, "WithA").isEmpty()
                    && !SandClassSearch.allVariants(project, "NoA").isEmpty();
            }
            catch (IndexNotReadyException e) {
                return false;
            }
        });
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
