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
import consulo.it.internal.HeadlessFileWatcher;
import consulo.ui.ModalityState;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.impl.internal.refresh.RefreshWorker;
import consulo.virtualFileSystem.internal.PersistentFS;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * External changes reported by a running file watcher must all reach the VFS: a refresh session only descends into what
 * the watcher marked dirty, so the dirty marks have to survive a session which is cancelled or which runs while the
 * files keep changing.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class WatcherDrivenRefreshTest {
    private static final int DIRS = 20;
    private static final int FILES = 50;
    private static final long TIMEOUT_SECONDS = 30;

    private final AtomicLong myEditCounter = new AtomicLong();
    private LocalFileSystem.@Nullable WatchRequest myWatchRequest;

    @BeforeEach
    public void setUp() {
        HeadlessFileWatcher.enable();
    }

    @AfterEach
    public void tearDown() {
        RefreshWorker.setTestListener(null);
        LocalFileSystem.WatchRequest request = myWatchRequest;
        if (request != null) {
            LocalFileSystem.getInstance().removeWatchedRoot(request);
        }
        HeadlessFileWatcher.disable();
    }

    @Test
    public void refreshFollowsTheWatcher(@TempDir Path tempDir) throws Exception {
        Path directory = createTree(tempDir, 1, 2);
        VirtualFile root = watchAndLoad(directory);

        Path path = directory.resolve("dir0").resolve("file0.txt");
        edit(path, false);
        refresh(root);
        assertThat(isUpToDate(root, path))
            .as("with a running watcher a refresh only visits what the watcher reported")
            .isFalse();

        HeadlessFileWatcher.notifyDirtyPath(path.toString());
        refresh(root);
        assertThat(isUpToDate(root, path)).as("the change the watcher reported must be refreshed").isTrue();
    }

    @Test
    public void editsDuringARefreshAreFoundByTheNextOne(@TempDir Path tempDir) throws Exception {
        Path directory = createTree(tempDir, DIRS, FILES);
        VirtualFile root = watchAndLoad(directory);

        for (Path path : allFiles(directory)) {
            edit(path, true);
        }

        // while the refresh is half way through, the external tool keeps writing: into a directory the refresh already
        // went through and into one it has not reached yet
        VirtualFile middle = root.findChild("dir" + DIRS / 2);
        assertThat(middle).isNotNull();
        AtomicBoolean editedDuringRefresh = new AtomicBoolean();
        RefreshWorker.setTestListener(stopAt -> {
            if (stopAt.equals(middle) && editedDuringRefresh.compareAndSet(false, true)) {
                for (int i = 0; i < FILES; i++) {
                    edit(directory.resolve("dir0").resolve("file" + i + ".txt"), true);
                    edit(directory.resolve("dir" + (DIRS - 1)).resolve("file" + i + ".txt"), true);
                }
            }
        });
        refresh(root);
        RefreshWorker.setTestListener(null);
        assertThat(editedDuringRefresh).as("the refresh never reached the middle directory").isTrue();

        refresh(root);
        assertAllUpToDate(root, directory);
    }

    @Test
    public void refreshAfterACancelledOneFindsEverything(@TempDir Path tempDir) throws Exception {
        // the scan stops at its cancellation checks; count them in a full scan of the same change, then cancel early, in
        // the middle and densely towards the end - the last directories are where the scan has already cleaned the root
        int checks = cancelAndRefresh(Files.createDirectories(tempDir.resolve("probe")), Integer.MAX_VALUE);
        assertThat(checks).isGreaterThan(DIRS);

        Set<Integer> cancelPoints = new TreeSet<>(List.of(1, checks / 4, checks / 2, checks * 3 / 4));
        for (int i = 1; i <= Math.min(60, checks - 1); i += 3) {
            cancelPoints.add(checks - i);
        }
        for (int cancelAtCheck : cancelPoints) {
            cancelAndRefresh(Files.createDirectories(tempDir.resolve("cancel" + cancelAtCheck)), cancelAtCheck);
        }
    }

    /**
     * @return the count of cancellation checks the cancelled scan went through
     */
    private int cancelAndRefresh(Path tempDir, int cancelAtCheck) throws Exception {
        Path directory = createTree(tempDir, DIRS, FILES);
        VirtualFile root = watchAndLoad(directory);

        for (Path path : allFiles(directory)) {
            edit(path, true);
        }

        // the user comes back to the frame while the sync of the big change is running: the new activation refresh
        // cancels the running one and starts over
        AtomicInteger checks = new AtomicInteger();
        AtomicReference<@Nullable RefreshSession> running = new AtomicReference<>();
        RefreshWorker.setTestListener(stopAt -> {
            RefreshSession session = running.get();
            if (session != null && checks.incrementAndGet() == cancelAtCheck) {
                RefreshQueue.getInstance().cancelSession(session.getId());
            }
        });
        RefreshSession cancelled = RefreshQueue.getInstance().createSession(false, true, null, ModalityState.nonModal());
        cancelled.addFile(root);
        running.set(cancelled);
        cancelled.launch();
        RefreshWorker.setTestListener(null);
        if (cancelAtCheck != Integer.MAX_VALUE) {
            assertThat(checks.get()).as("the scan ended before check " + cancelAtCheck).isGreaterThanOrEqualTo(cancelAtCheck);
        }

        refresh(root);
        assertAllUpToDate(root, directory);

        LocalFileSystem.WatchRequest request = myWatchRequest;
        if (request != null) {
            LocalFileSystem.getInstance().removeWatchedRoot(request);
            myWatchRequest = null;
        }
        return checks.get();
    }

    private VirtualFile watchAndLoad(Path directory) throws Exception {
        String rootPath = directory.toString();
        myWatchRequest = LocalFileSystem.getInstance().addRootToWatch(rootPath, true);
        assertThat(myWatchRequest).isNotNull();
        waitFor(() -> HeadlessFileWatcher.getRecursiveRoots().contains(rootPath));

        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(root).isNotNull();
        root.refresh(false, true);
        for (VirtualFile dir : root.getChildren()) {
            dir.getChildren();
        }
        // the children loaded above come in dirty, the watcher would never report them
        refresh(root);
        for (Path path : allFiles(directory)) {
            assertThat(isUpToDate(root, path)).isTrue();
        }
        return root;
    }

    private static void refresh(VirtualFile root) {
        RefreshSession session = RefreshQueue.getInstance().createSession(false, true, null, ModalityState.nonModal());
        session.addFile(root);
        session.launch();
    }

    private void edit(Path path, boolean report) {
        try {
            // each edit is longer than every earlier one, so it never looks unchanged to a length and timestamp check
            Files.writeString(path, "x".repeat((int) myEditCounter.incrementAndGet() + 100));
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (report) {
            HeadlessFileWatcher.notifyDirtyPath(path.toString());
        }
    }

    private static void assertAllUpToDate(VirtualFile root, Path directory) throws IOException {
        List<Path> stale = new ArrayList<>();
        for (Path path : allFiles(directory)) {
            if (!isUpToDate(root, path)) {
                stale.add(path);
            }
        }
        assertThat(stale).as("changes the refresh never picked up").isEmpty();
    }

    private static boolean isUpToDate(VirtualFile root, Path path) throws IOException {
        VirtualFile file = root.findFileByRelativePath(root.toNioPath().relativize(path).toString().replace('\\', '/'));
        assertThat(file).as(path.toString()).isNotNull();
        PersistentFS persistentFS = PersistentFS.getInstance();
        return persistentFS.getLastRecordedLength(file) == Files.size(path)
            && persistentFS.getTimeStamp(file) == Files.getLastModifiedTime(path).toMillis();
    }

    private static Path createTree(Path tempDir, int dirs, int files) throws IOException {
        Path directory = tempDir.toRealPath();
        for (int d = 0; d < dirs; d++) {
            Path dir = Files.createDirectories(directory.resolve("dir" + d));
            for (int f = 0; f < files; f++) {
                Files.writeString(dir.resolve("file" + f + ".txt"), "initial " + d + "/" + f);
            }
        }
        return directory;
    }

    private static List<Path> allFiles(Path directory) throws IOException {
        List<Path> result = new ArrayList<>();
        try (var dirs = Files.list(directory)) {
            for (Path dir : dirs.sorted().toList()) {
                try (var files = Files.list(dir)) {
                    result.addAll(files.sorted().toList());
                }
            }
        }
        return result;
    }

    private static void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not met within " + TIMEOUT_SECONDS + "s");
            }
            Thread.sleep(20);
        }
    }
}
