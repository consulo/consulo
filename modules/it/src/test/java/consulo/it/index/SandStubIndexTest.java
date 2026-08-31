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
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.index.impl.internal.UnindexedFilesScanner;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.StubIndex;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.stub.SandIndexKeys;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that stub indexes stay correct through the whole pipeline: initial scan, an external mass change with
 * VFS refresh and changed-files reindexing, and a subsequent full rescan (which resets and repopulates the
 * per-project indexable files filter gating stub index queries).
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SandStubIndexTest {
    private static final long TIMEOUT_SECONDS = 60;
    private static final int FILES = 25;

    /**
     * See {@code ProjectStateReloadTest} - the refresh makes the platform fire VFS events on the UI thread,
     * where the pointer manager and the indexing listeners break their own threading assertions in a headless
     * application. Unrelated to the behavior under test; any other logged error still fails it.
     */
    @AllowLogError({
        "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
        "consulo.application.impl.internal.BaseApplication",
        // the sand plugin registers actions into UI groups (MainMenu etc.) which do not exist in the headless application
        "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
    })
    @Test
    public void stubIndexSurvivesExternalChangesAndRescan(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-stub-index");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        for (int i = 0; i < FILES; i++) {
            Files.writeString(src.resolve("file" + i + ".sand"), "class Foo" + i + " {}");
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

        assertThat(findClasses(project, "Foo5"))
            .as("class from the initial scan must be found through the stub index")
            .isNotEmpty();

        // an external tool renames every class
        for (int i = 0; i < FILES; i++) {
            Files.writeString(src.resolve("file" + i + ".sand"), "class Bar" + i + " {}");
        }

        VirtualFile srcFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(src);
        assertThat(srcFile).isNotNull();
        srcFile.refresh(false, true);

        waitFor(() -> !findClasses(project, "Bar5").isEmpty());
        awaitSmart(dumbService);

        assertThat(findClasses(project, "Bar5"))
            .as("stub index must serve the new class after the external change was reindexed")
            .isNotEmpty();
        assertThat(findClasses(project, "Foo5"))
            .as("stub index must forget the old class after the external change was reindexed")
            .isEmpty();

        // a full rescan resets and repopulates the per-project filter which gates stub index queries;
        // up-to-date files must stay visible afterwards
        dumbService.queueTask(new UnindexedFilesScanner(project));
        awaitSmart(dumbService);

        assertThat(findClasses(project, "Bar5"))
            .as("stub index must keep serving up-to-date files after a full rescan")
            .isNotEmpty();
    }

    private static Collection<SandClass> findClasses(Project project, String name) {
        return ReadAction.compute(
            () -> StubIndex.getElements(SandIndexKeys.SAND_CLASSES, name, project, GlobalSearchScope.allScope(project), SandClass.class)
        );
    }

    private static void awaitSmart(DumbService dumbService) throws InterruptedException {
        CountDownLatch smart = new CountDownLatch(1);
        dumbService.runWhenSmart(smart::countDown);
        assertThat(smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("project must reach smart mode").isTrue();
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) throws Exception {
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

