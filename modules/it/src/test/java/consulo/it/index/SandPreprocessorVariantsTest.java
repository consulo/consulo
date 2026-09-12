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
 * Broader C-family directive semantics under the preprocessor model: the include-guard
 * idiom ({@code #ifndef} + self {@code #flag}), an {@code #elif} chain, {@code #undef}, and
 * self-defined flags enabling the file's own guards. Only the enabled chain segment is
 * parsed and indexed; a seed change (module flags) re-parses and re-indexes the file.
 * <p>
 * Enabled-region garbage degrades with run granularity: an unparsable token run becomes one
 * opaque raw block and declarations around it survive ({@code BrokenOk} keeps its
 * trailing-garbage declaration). Disabled-region garbage does not exist at all.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SandPreprocessorVariantsTest {
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
    public void directiveVariantsFollowEnvironment(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-preproc");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("lib.sand"), """
            #ifndef GUARD
            #flag GUARD
            class Lib {}
            #end
            #flag SELF
            #if SELF
            class SelfVisible {}
            #end
            #if MODE_A
            class ModeA {}
            #elif MODE_B
            class ModeB {}
            #else
            class ModeDefault {}
            #end
            #flag TEMP
            #undef TEMP
            #if TEMP
            class TempVisible {}
            #end
            """);
        Files.writeString(src.resolve("broken.sand"), """
            #if MODE_A
            %%% class Ghost {}
            #elif MODE_B
            class BrokenB {}
            #else
            class BrokenOk {} 1
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

        // guard idiom enabled standalone, self-defined flag enables its own guard, the #else
        // chain segment wins without mode flags, #undef keeps TEMP dead - and every disabled
        // branch is simply absent: no PSI, no stubs, no index entries
        waitFor(() -> ReadAction.compute(() -> {
            try {
                return indexed(project, "Lib")
                    && indexed(project, "SelfVisible")
                    && indexed(project, "ModeDefault")
                    && !indexed(project, "ModeA")
                    && !indexed(project, "ModeB")
                    && !indexed(project, "TempVisible")
                    && indexed(project, "BrokenOk") && !indexed(project, "BrokenB")
                    && !indexed(project, "Ghost");
            }
            catch (IndexNotReadyException e) {
                return false;
            }
        }));

        Module module = moduleManager.findModuleByName("main");
        assertThat(module).isNotNull();
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            SandMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
            assertThat(extension).isNotNull();
            extension.setEnabled(true);
            extension.setFlags(Set.of("MODE_B"));
            rootModel.commit();
        });

        // seed change reparses and reindexes: the #elif segment takes over, the #else dies,
        // and the broken #if MODE_A branch stays disabled (its garbage still invisible)
        waitFor(() -> ReadAction.compute(() -> {
            try {
                return indexed(project, "ModeB")
                    && !indexed(project, "ModeDefault")
                    && !indexed(project, "ModeA")
                    && indexed(project, "Lib")
                    && indexed(project, "SelfVisible")
                    && !indexed(project, "TempVisible")
                    && indexed(project, "BrokenB") && !indexed(project, "BrokenOk")
                    && !indexed(project, "Ghost");
            }
            catch (IndexNotReadyException e) {
                return false;
            }
        }));
    }

    private static boolean indexed(Project project, String name) {
        return !SandClassSearch.allVariants(project, name).isEmpty();
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
