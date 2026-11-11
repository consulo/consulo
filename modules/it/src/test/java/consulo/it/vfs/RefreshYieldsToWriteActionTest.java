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
package consulo.it.vfs;

import consulo.application.WriteAction;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.HeadlessApplicationExtension;
import consulo.ui.ModalityState;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.internal.PersistentFS;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The async listeners of a large sync can run for a long time; a write action requested meanwhile - typing in the editor,
 * the indexer's own updates - must not wait for them. The listeners give way and run again after the write action, and
 * the refresh still applies its events.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class RefreshYieldsToWriteActionTest {
    private static final long LISTENER_HOLD_SECONDS = 10;
    private static final long TIMEOUT_SECONDS = 30;

    @Test
    public void writeActionDoesNotWaitForAsyncListeners(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.toRealPath().resolve("file.txt");
        Files.writeString(path, "before");
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertThat(file).isNotNull();

        CountDownLatch listenerStarted = new CountDownLatch(1);
        AtomicBoolean release = new AtomicBoolean();
        AtomicInteger listenerRuns = new AtomicInteger();
        Disposable disposable = Disposable.newDisposable();
        try {
            VirtualFileManager.getInstance().addAsyncFileListener(new AsyncFileListener() {
                @Override
                public @Nullable ChangeApplier prepareChange(List<? extends VFileEvent> events) {
                    if (events.stream().noneMatch(e -> file.equals(e.getFile()))) {
                        return null;
                    }
                    listenerRuns.incrementAndGet();
                    listenerStarted.countDown();
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LISTENER_HOLD_SECONDS);
                    while (!release.get() && System.nanoTime() < deadline) {
                        ProgressManager.checkCanceled();
                        sleep();
                    }
                    return null;
                }
            }, disposable);

            Files.writeString(path, "after, with a different length");
            CountDownLatch finished = new CountDownLatch(1);
            RefreshSession session = RefreshQueue.getInstance().createSession(true, false, finished::countDown, ModalityState.nonModal());
            session.addFile(file);
            session.launch();

            assertThat(listenerStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("the async listener never ran").isTrue();

            long start = System.nanoTime();
            WriteAction.run(() -> {
            });
            long writeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            release.set(true);

            assertThat(writeMillis)
                .as("the write action waited for the async listeners of the refresh")
                .isLessThan(TimeUnit.SECONDS.toMillis(LISTENER_HOLD_SECONDS) / 2);

            assertThat(finished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("the refresh never applied its events").isTrue();
            assertThat(PersistentFS.getInstance().getLastRecordedLength(file)).isEqualTo(Files.size(path));
            assertThat(listenerRuns.get()).as("the listeners must run again after the write action").isGreaterThanOrEqualTo(2);
        }
        finally {
            release.set(true);
            Disposer.dispose(disposable);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(10);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
