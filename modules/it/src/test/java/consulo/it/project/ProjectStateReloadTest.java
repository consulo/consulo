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
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.impl.internal.store.IProjectStore;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.StoreReloadManager;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives an external change of a project config file against a real opened project and asserts the owning
 * component is reloaded in place, without the project being reloaded as a whole.
 * <p>
 * Covers the whole chain: the storage's VFS listener publishes {@code StateStorageListener}, which reaches
 * {@code StoreReloadManagerImpl}, which diffs the storage and calls {@code reinitComponents}, which runs
 * {@code loadState} followed by {@code afterLoad(false)}.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ProjectStateReloadTest {
    private static final long TIMEOUT_SECONDS = 30;
    private static final String STORAGE_FILE = "it-reload-test.xml";

    /**
     * The refresh below makes the platform fire VFS events on the UI thread, where
     * {@code VirtualFilePointerManagerImpl} and the indexing listeners break their own threading assertions in a
     * headless application. Both are unrelated to the state reload this test is about, so they are tolerated -
     * any other logged error still fails the test.
     */
    @AllowLogError({"consulo.virtualFileSystem.internal.BaseVirtualFileManager", "consulo.application.impl.internal.BaseApplication"})
    @Test
    public void externalChangeReloadsComponentInPlace(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-state-reload");

        Project project = projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // makes the manager register its VFS listener before anything is written
        StoreReloadManager.getInstance(project);

        IProjectStore store = project.getInstance(IProjectStore.class);

        ReloadableComponent component = new ReloadableComponent();
        assertThat(store.loadStateIfStorable(component)).isNotNull();

        component.myValue = "written-by-ide";
        saveProject(project, application);

        Path storageFile = directory.resolve(Project.DIRECTORY_STORE_FOLDER).resolve(STORAGE_FILE);
        assertThat(storageFile).exists();
        assertThat(Files.readString(storageFile)).contains("written-by-ide");

        component.myAfterLoadFlags.clear();

        // let the VFS cache the current content first, otherwise the refresh below has nothing to diff against
        VirtualFile storageVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(storageFile);
        assertThat(storageVirtualFile).isNotNull();
        assertThat(storageVirtualFile.getPath()).isNotNull();

        // rewrite the file the way an external tool or a VCS update would
        Files.writeString(storageFile, Files.readString(storageFile).replace("written-by-ide", "changed-on-disk"));
        storageVirtualFile.refresh(false, false);

        waitFor(() -> "changed-on-disk".equals(component.myValue));

        assertThat(component.myValue).isEqualTo("changed-on-disk");
        assertThat(component.myAfterLoadFlags).containsExactly(false);
        assertThat(project.isDisposed()).isFalse();
    }

    private static void saveProject(Project project, Application application) throws Exception {
        project.saveAsync(application.getLastUIAccess())
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture()
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Component was not reloaded within " + TIMEOUT_SECONDS + "s");
    }

    public static class Bean {
        public String value = "default";
    }

    @State(name = "ItReloadTest", storages = @Storage(STORAGE_FILE))
    public static class ReloadableComponent implements PersistentStateComponent<Bean> {
        public volatile String myValue = "default";
        public final List<Boolean> myAfterLoadFlags = new ArrayList<>();

        @Override
        public @Nullable Bean getState() {
            Bean bean = new Bean();
            bean.value = myValue;
            return bean;
        }

        @Override
        public void loadState(Bean state) {
            myValue = state.value;
        }

        @Override
        public void afterLoad(boolean first) {
            myAfterLoadFlags.add(first);
        }
    }
}
