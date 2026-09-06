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
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.index.ScanningTestSupport.BlockingIterator;
import consulo.it.index.ScanningTestSupport.RecordingIterator;
import consulo.it.index.ScanningTestSupport.TestScans;
import consulo.language.index.impl.internal.PerProjectIndexingQueue;
import consulo.language.index.impl.internal.UnindexedFilesScannerExecutorImpl;
import consulo.language.index.impl.internal.roots.ProjectIndexableFilesIteratorImpl;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static consulo.it.index.ScanningTestSupport.TIMEOUT_SECONDS;
import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.allowOnlyTestScans;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.fullScan;
import static consulo.it.index.ScanningTestSupport.heldFullScan;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.partialScan;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives {@code UnindexedFilesScannerExecutorImpl} with scanning tasks whose timing the test controls: a full scan
 * held on its start condition, or a partial scan whose provider blocks until released.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class UnindexedFilesScannerExecutorTest {
    private static final int FILES = 25;

    @Test
    public void partialScanDoesNotCancelRunningFullScan(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager, Files.createTempDirectory("consulo-it-scanner-executor"));
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        UnindexedFilesScannerExecutorImpl executor = UnindexedFilesScannerExecutorImpl.getInstance(project);

        CompletableFuture<Void> gate = new CompletableFuture<>();
        Future<?> full = heldFullScan(project, "held full", gate).queue();
        waitFor("the full scan must be picked up by the executor", () -> executor.isRunning().get() && !executor.hasQueuedTasks());

        AtomicBoolean fullDoneWhenPartialRan = new AtomicBoolean();
        RecordingIterator recorder = new RecordingIterator(() -> fullDoneWhenPartialRan.set(full.isDone()));
        Future<?> partial = partialScan(project, "partial", recorder).queue();

        assertThatThrownBy(() -> full.get(1, TimeUnit.SECONDS))
            .as("a partial scan must not cancel the running full scan")
            .isInstanceOf(TimeoutException.class);
        assertThat(recorder.getIterations()).as("the partial scan must wait for the running one").isZero();
        assertThat(executor.hasQueuedTasks()).isTrue();

        gate.complete(null);
        full.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        partial.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(recorder.getIterations()).isEqualTo(1);
        assertThat(fullDoneWhenPartialRan).as("the partial scan must run after the full scan completed").isTrue();
    }

    @Test
    public void fullScanCancelsRunningScanAndCompletesExactlyOnce(Application application, ProjectManager projectManager)
        throws Exception {
        Project project = openProject(application, projectManager, Files.createTempDirectory("consulo-it-scanner-executor"));
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        UnindexedFilesScannerExecutorImpl executor = UnindexedFilesScannerExecutorImpl.getInstance(project);

        AtomicInteger startedOrStopped = new AtomicInteger();
        Runnable unsubscribe = executor.startedOrStoppedEvent().addListener(value -> startedOrStopped.incrementAndGet());
        try {
            BlockingIterator blocker = new BlockingIterator();
            Future<?> running = partialScan(project, "running", blocker).queue();
            blocker.awaitStarted();

            Future<?> full = fullScan(project, "full").queue();

            assertThatThrownBy(() -> running.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("a full scan must cancel the running scan")
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ProcessCanceledException.class);
            full.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            awaitIdle(project);

            waitFor(
                "exactly two task executions: the cancelled scan and the full scan, each reporting start and stop",
                () -> startedOrStopped.get() == 4
            );
        }
        finally {
            unsubscribe.run();
        }
    }

    @Test
    public void queuedPartialScansAreMergedIntoOne(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager, Files.createTempDirectory("consulo-it-scanner-executor"));
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        UnindexedFilesScannerExecutorImpl executor = UnindexedFilesScannerExecutorImpl.getInstance(project);

        AtomicInteger startedOrStopped = new AtomicInteger();
        Runnable unsubscribe = executor.startedOrStoppedEvent().addListener(value -> startedOrStopped.incrementAndGet());
        try {
            BlockingIterator blocker = new BlockingIterator();
            Future<?> running = partialScan(project, "running", blocker).queue();
            blocker.awaitStarted();

            RecordingIterator first = new RecordingIterator();
            RecordingIterator second = new RecordingIterator();
            Future<?> firstFuture = partialScan(project, "first", first).queue();
            Future<?> secondFuture = partialScan(project, "second", second).queue();

            assertThat(secondFuture).as("a partial scan queued behind another one must merge into it").isSameAs(firstFuture);
            assertThat(executor.hasQueuedTasks()).isTrue();

            blocker.release();
            running.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            firstFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            awaitIdle(project);

            assertThat(first.getIterations()).isEqualTo(1);
            assertThat(second.getIterations()).isEqualTo(1);
            waitFor(
                "exactly two task executions: the blocked scan and the merged scan, each reporting start and stop",
                () -> startedOrStopped.get() == 4
            );
        }
        finally {
            unsubscribe.run();
        }
    }

    /**
     * See {@code ProjectStateReloadTest} for the VFS categories; the sand plugin registers actions into UI groups which
     * do not exist in the headless application.
     */
    @AllowLogError({
        "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
        "consulo.application.impl.internal.BaseApplication",
        "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
    })
    @Test
    public void cancelledScanStillFlushesCollectedFilesToIndexer(Application application, ProjectManager projectManager)
        throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-scanner-executor-flush");
        Path src = createSandFiles(directory, FILES, "Foo");
        Project project = openProject(application, projectManager, directory);
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);
        UnindexedFilesScannerExecutorImpl executor = UnindexedFilesScannerExecutorImpl.getInstance(project);
        PerProjectIndexingQueue indexingQueue = PerProjectIndexingQueue.getInstance(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            TestScans scans = allowOnlyTestScans(project, disposable);
            addContentRoot(project, directory);
            awaitIdle(project);
            VirtualFile srcFile = findFile(src);
            assertThat(findClasses(project, "Foo5")).as("nothing may be indexed before the test scan").isEmpty();

            BlockingIterator blocker = new BlockingIterator();
            Future<?> scan = scans.queue(partialScan(project, "cancelled", new ProjectIndexableFilesIteratorImpl(srcFile), blocker));
            waitFor("the scan must collect the files before it is cancelled", () -> indexingQueue.getQueuedFiles().getSize() >= FILES);
            blocker.awaitStarted();

            executor.cancelAllTasksAndWait();

            assertThatThrownBy(() -> scan.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("the scan must report its cancellation")
                .isInstanceOf(ExecutionException.class);

            awaitSmart(dumbService);
            waitFor("the collected files must be handed to an indexer", () -> indexingQueue.getQueuedFiles().isEmpty());
            awaitIdle(project);
            for (int i = 0; i < FILES; i++) {
                assertThat(findClasses(project, "Foo" + i)).as("Foo%d must be indexed after the cancelled scan", i).hasSize(1);
            }
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void isRunningTransitionsAndQueueIsEmptyWhenIdle(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager, Files.createTempDirectory("consulo-it-scanner-executor"));
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        UnindexedFilesScannerExecutorImpl executor = UnindexedFilesScannerExecutorImpl.getInstance(project);
        assertThat(executor.isRunning().get()).isFalse();
        assertThat(executor.hasQueuedTasks()).isFalse();

        List<Boolean> transitions = Collections.synchronizedList(new ArrayList<>());
        Runnable unsubscribe = executor.isRunning().addListener(transitions::add);
        try {
            CompletableFuture<Void> gate = new CompletableFuture<>();
            Future<?> held = heldFullScan(project, "held", gate).queue();
            waitFor("the executor must report the held scan as running", () -> executor.isRunning().get());

            gate.complete(null);
            held.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            waitFor("the executor must stop after the scan", () -> !executor.isRunning().get());

            assertThat(executor.hasQueuedTasks()).isFalse();
            assertThat(transitions).containsExactly(true, false);
        }
        finally {
            unsubscribe.run();
        }
    }
}
