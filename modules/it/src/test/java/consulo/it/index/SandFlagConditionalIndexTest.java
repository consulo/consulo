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
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
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
 * Condition-annotated indexing, standalone-file dimension: both branches of a
 * {@code #if} construct are always in the stub index with their guards; the module flag
 * environment filters at query time, so flipping the flag flips the answer without any
 * dependence on reindexing.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SandFlagConditionalIndexTest {
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
    public void moduleFlagFlipsFilteredQuery(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-flag");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("a.sand"), """
            #if A
            class Foo {}
            #else
            class Bar {}
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

        // without the flag the #else branch is active; both variants are in the index
        waitFor(() -> activeIsAndBothIndexed(project, "Bar", "Foo"));

        Module module = moduleManager.findModuleByName("main");
        assertThat(module).isNotNull();
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            SandMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
            assertThat(extension).isNotNull();
            extension.setEnabled(true);
            extension.setFlags(Set.of("A"));
            rootModel.commit();
        });

        // the environment is read at query time - the filtered answer flips without reindexing
        waitFor(() -> activeIsAndBothIndexed(project, "Foo", "Bar"));
    }

    /**
     * One read action asserts three facts: the expected variant is active, the other is
     * filtered out, and both variants are physically present in the index.
     */
    private static boolean activeIsAndBothIndexed(Project project, String active, String inactive) {
        return ReadAction.compute(() -> {
            try {
                return !SandClassSearch.active(project, active).isEmpty()
                    && SandClassSearch.active(project, inactive).isEmpty()
                    && !SandClassSearch.allVariants(project, active).isEmpty()
                    && !SandClassSearch.allVariants(project, inactive).isEmpty();
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
