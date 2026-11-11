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

import consulo.it.HeadlessApplicationExtension;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.impl.internal.refresh.RefreshWorker;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A large sync cancelled half way - the user refocuses the frame and the new activation refresh cancels the running one -
 * must leave every not yet processed change reachable for the next refresh.
 * <p>
 * With a running native watcher a refresh session only descends into dirty files, so a dirty file whose ancestors were
 * already marked clean is never visited again, and later watcher marks on it are no-ops because it is dirty already.
 * The scan cleans each directory right after listing it, so the root is clean by the time the worker reaches the files
 * of the last directory; a cancel there must re-mark the chain up to the root.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class CancelledRefreshKeepsDirtyChainTest {
    private static final int FILES = 10;
    private static final int CANCEL_AT_CHECK = 2;

    @AfterEach
    public void tearDown() {
        RefreshWorker.setTestListener(null);
    }

    @Test
    public void cancelInLastDirectoryKeepsChangesReachable(@TempDir Path directory) throws Exception {
        Path changed = directory.resolve("changed");
        Files.createDirectories(directory.resolve("untouched"));
        Files.createDirectories(changed);
        for (int i = 0; i < FILES; i++) {
            Files.writeString(directory.resolve("untouched").resolve("file" + i + ".txt"), "same " + i);
            Files.writeString(changed.resolve("file" + i + ".txt"), "before " + i);
        }

        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(root).isNotNull();
        root.refresh(false, true);
        for (VirtualFile child : root.getChildren()) {
            assertThat(child.getChildren()).hasSize(FILES);
        }
        root.refresh(false, true);
        assertThat(((NewVirtualFile) root).isDirty()).isFalse();

        VirtualFile changedDir = root.findChild("changed");
        assertThat(changedDir).isNotNull();

        // an external tool rewrites the files, the watcher marks each of them dirty
        for (int i = 0; i < FILES; i++) {
            Files.writeString(changed.resolve("file" + i + ".txt"), "after, with a different length " + i);
            VirtualFile file = changedDir.findChild("file" + i + ".txt");
            assertThat(file).isNotNull();
            ((NewVirtualFile) file).markDirty();
        }
        assertThat(((NewVirtualFile) root).isDirty()).isTrue();

        // the activation refresh cancels the running scan when it starts reporting the changes of the directory: past the
        // listing and the attribute reads, at the consistency check right before the events are generated
        AtomicReference<@Nullable RefreshWorker> running = new AtomicReference<>();
        AtomicInteger checksInChangedDir = new AtomicInteger();
        RefreshWorker.setTestListener(stopAt -> {
            RefreshWorker worker = running.get();
            if (worker != null && stopAt.equals(changedDir) && checksInChangedDir.incrementAndGet() == CANCEL_AT_CHECK) {
                worker.cancel();
            }
        });
        RefreshWorker cancelled = new RefreshWorker(List.of((NewVirtualFile) root), true);
        running.set(cancelled);
        cancelled.scan();
        RefreshWorker.setTestListener(null);
        assertThat(checksInChangedDir.get()).as("the scan was not cancelled at the consistency check").isEqualTo(CANCEL_AT_CHECK);

        VirtualFile firstChanged = changedDir.findChild("file0.txt");
        assertThat(firstChanged).isNotNull();
        assertThat(((NewVirtualFile) firstChanged).isDirty())
            .as("the cancelled scan never got to the change, it must still be pending")
            .isTrue();
        assertThat(((NewVirtualFile) changedDir).isDirty()).isTrue();
        assertThat(((NewVirtualFile) root).isDirty())
            .as("a pending change below a clean root is never visited by the next refresh")
            .isTrue();

        // the next refresh must find every change
        RefreshWorker next = new RefreshWorker(List.of((NewVirtualFile) root), true);
        List<VFileEvent> events = next.scan();
        assertThat(events.stream().filter(e -> e instanceof VFileContentChangeEvent).count())
            .as("content changes found by the refresh after the cancelled one: " + events)
            .isEqualTo(FILES);
    }
}
