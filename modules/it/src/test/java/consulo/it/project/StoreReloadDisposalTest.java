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
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.component.persist.PersistentStateComponentAsync;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.project.Project;
import consulo.project.StoreReloadManager;
import consulo.project.impl.internal.store.IProjectStore;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Disposes a project while a store reload is parked inside {@code loadState}, the window a reload triggered
 * shortly before a close runs into. The reload's remaining steps then run against a project whose message bus
 * is already gone, which must not publish anything and must not log.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class StoreReloadDisposalTest {
    private static final long TIMEOUT_SECONDS = 30;
    private static final String STORAGE_FILE = "it-reload-disposal-test.xml";

    @Test
    public void reloadParkedInLoadStateSurvivesProjectDisposal(Application application, HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-reload-disposal");

        Project project = projects.open(directory);
        StoreReloadManager.getInstance(project);

        IProjectStore store = project.getInstance(IProjectStore.class);

        GatedComponent component = new GatedComponent();
        run(project.coroutineContext(), store.loadStateIfStorableAsync(component));

        component.myValue = "written-by-ide";
        run(project.coroutineContext(), project.saveAsync(project.getUIAccess()));

        Path storageFile = directory.resolve(Project.DIRECTORY_STORE_FOLDER).resolve(STORAGE_FILE);
        assertThat(storageFile).exists();

        VirtualFile storageVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(storageFile);
        assertThat(storageVirtualFile).isNotNull();

        try {
            component.myGated = true;

            Files.writeString(storageFile, Files.readString(storageFile).replace("written-by-ide", "changed-on-disk"));
            storageVirtualFile.refresh(false, false);

            assertThat(component.myEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("the reload must reach loadState, otherwise nothing is parked when the project is disposed")
                .isTrue();

            projects.close(project);
            assertThat(project.isDisposed()).isTrue();
        }
        finally {
            component.myGate.countDown();
        }

        waitFor(() -> component.myAfterLoadCalled);

        run(application.coroutineContext(), Coroutine.first(WriteLock.apply((input, continuation) -> null)));
    }

    private static void run(CoroutineContext context, Coroutine<?, ?> coroutine) throws Exception {
        coroutine.runAsync(CoroutineScope.of(context), null)
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
        throw new AssertionError("The parked reload did not continue within " + TIMEOUT_SECONDS + "s");
    }

    public static class Bean {
        public String value = "default";
    }

    @State(name = "ItReloadDisposalTest", storages = @Storage(STORAGE_FILE))
    public static class GatedComponent implements PersistentStateComponentAsync<Bean> {
        public volatile String myValue = "default";
        public volatile boolean myGated;
        public volatile boolean myAfterLoadCalled;

        public final CountDownLatch myEntered = new CountDownLatch(1);
        public final CountDownLatch myGate = new CountDownLatch(1);

        @Override
        public Coroutine<?, @Nullable Bean> getState() {
            return Coroutine.first(CodeExecution.apply(input -> {
                Bean bean = new Bean();
                bean.value = myValue;
                return bean;
            }));
        }

        @Override
        public Coroutine<?, ?> loadState(Bean state) {
            return Coroutine.first(CodeExecution.apply(input -> {
                myValue = state.value;

                if (myGated) {
                    myEntered.countDown();
                    try {
                        myGate.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return null;
            }));
        }

        @Override
        public Coroutine<?, ?> afterLoad(boolean first) {
            return Coroutine.first(CodeExecution.apply(input -> {
                if (myGated) {
                    myAfterLoadCalled = true;
                }
                return null;
            }));
        }
    }
}
