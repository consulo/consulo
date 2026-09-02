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
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexMetaStorage;
import consulo.language.index.impl.internal.stub.StubUpdatingIndex;
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
import consulo.virtualFileSystem.VirtualFileWithId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the "missing meta on an indexed file means stale" rule: a file indexed under an
 * options-sensitive index whose meta record disappears (analog: the file was indexed
 * before the provider's plugin was installed) must be reindexed by the next
 * {@code rootsChanged} revalidation, restoring the meta.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SandMetaHoleGuardTest {
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
    public void missingMetaOnIndexedFileTriggersReindex(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-meta-hole");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("a.sand"), "class Foo {}");

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
        waitFor(() -> hasClass(project, "Foo"));

        VirtualFile file = directoryFile.findFileByRelativePath("src/a.sand");
        assertThat(file).isNotNull();
        int fileId = ((VirtualFileWithId) file).getId();

        ModuleAwareIndexMetaStorage storage = ModuleAwareIndexMetaStorage.getInstance();
        waitFor(() -> storage.get(StubUpdatingIndex.INDEX_ID, fileId) != null);

        // the analog of "indexed before the provider existed": stored data, no meta
        storage.delete(StubUpdatingIndex.INDEX_ID, fileId);
        assertThat(storage.get(StubUpdatingIndex.INDEX_ID, fileId)).isNull();

        // any rootsChanged triggers revalidation; missing meta on an indexed file must reindex it
        WriteAction.run(() -> {
            ModifiableModuleModel moduleModel = moduleManager.getModifiableModel();
            moduleModel.newModule("other", directory.resolve("other").toString());
            moduleModel.commit();
        });

        // a single dirty file is below the dumb-mode threshold and reindexes lazily on the
        // next index access - so the poll must query the index to drive the update
        waitFor(() -> hasClass(project, "Foo") && storage.get(StubUpdatingIndex.INDEX_ID, fileId) != null);
        awaitSmart(dumbService);
    }

    private static boolean hasClass(Project project, String name) {
        return ReadAction.compute(() -> {
            try {
                return !StubIndex.getElements(SandIndexKeys.SAND_CLASSES, name, project, GlobalSearchScope.allScope(project), SandClass.class).isEmpty();
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
