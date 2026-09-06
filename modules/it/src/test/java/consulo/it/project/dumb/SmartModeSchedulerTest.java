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
package consulo.it.project.dumb;

import consulo.application.Application;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.index.ScanningTestSupport.BlockingIterator;
import consulo.it.index.ScanningTestSupport.RecordingIterator;
import consulo.it.index.ScanningTestSupport.TestScans;
import consulo.language.index.impl.internal.PerProjectIndexingQueue;
import consulo.language.index.impl.internal.roots.ProjectIndexableFilesIteratorImpl;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.DumbModeListenerBackgroundable;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static consulo.it.index.ScanningTestSupport.TIMEOUT_SECONDS;
import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.allowOnlyTestScans;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.heldFullScan;
import static consulo.it.index.ScanningTestSupport.isDumb;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.partialScan;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smart mode is the conjunction of "not dumb" and "no scanning in progress": {@code SmartModeScheduler} holds
 * {@code runWhenSmart} callbacks and {@code waitForSmartMode} while a scan runs even though {@code isDumb()} is false.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class SmartModeSchedulerTest {
    private static final int FILES = 5;

    @Test
    public void runWhenSmartRegisteredDuringScanFiresAfterScanAndIndexing(Application application, ProjectManager projectManager)
        throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-smart-mode-scheduler");
        Path src = createSandFiles(directory, FILES, "Smart");
        Project project = openProject(application, projectManager, directory);
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);

        UnindexedFilesScannerExecutor executor = UnindexedFilesScannerExecutor.getInstance(project);
        PerProjectIndexingQueue indexingQueue = PerProjectIndexingQueue.getInstance(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            TestScans scans = allowOnlyTestScans(project, disposable);
            addContentRoot(project, directory);
            awaitIdle(project);
            VirtualFile srcFile = findFile(src);

            CountDownLatch afterIndexing = new CountDownLatch(1);
            AtomicBoolean queueDrainedAfterIndexing = new AtomicBoolean();
            AtomicBoolean dumbAfterIndexing = new AtomicBoolean(true);
            MessageBusConnection connection = project.getMessageBus().connect(disposable);
            connection.subscribe(DumbModeListenerBackgroundable.class, new DumbModeListenerBackgroundable() {
                @Override
                public void enteredDumbMode() {
                    dumbService.runWhenSmart(() -> {
                        queueDrainedAfterIndexing.set(indexingQueue.getQueuedFiles().isEmpty());
                        dumbAfterIndexing.set(dumbService.isDumb());
                        afterIndexing.countDown();
                    });
                }
            });

            BlockingIterator blocker = new BlockingIterator();
            Future<?> scan = scans.queue(partialScan(project, "held scan", new ProjectIndexableFilesIteratorImpl(srcFile), blocker));
            waitFor("the scan must collect the files", () -> indexingQueue.getQueuedFiles().getSize() >= FILES);
            blocker.awaitStarted();

            assertThat(isDumb(dumbService)).as("few files keep the project out of dumb mode while scanning").isFalse();
            assertThat(executor.isRunning().get()).isTrue();
            assertThat(dumbService.canRunSmart()).isFalse();

            CountDownLatch fired = new CountDownLatch(1);
            AtomicBoolean scanDoneWhenFired = new AtomicBoolean();
            AtomicBoolean scanningWhenFired = new AtomicBoolean(true);
            dumbService.runWhenSmart(() -> {
                scanDoneWhenFired.set(scan.isDone());
                scanningWhenFired.set(executor.isRunning().get());
                fired.countDown();
            });

            assertThat(fired.await(1, TimeUnit.SECONDS)).as("runWhenSmart must not fire while a scan is running").isFalse();

            blocker.release();
            scan.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(fired.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("runWhenSmart must fire once the scan is over").isTrue();
            assertThat(scanDoneWhenFired).as("the callback must run after the scan completed").isTrue();
            assertThat(scanningWhenFired).as("the callback must not run while scanning").isFalse();

            assertThat(afterIndexing.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("the indexer scheduled by the scan must run in dumb mode and let the project become smart again")
                .isTrue();
            assertThat(queueDrainedAfterIndexing).as("a callback registered while indexing runs must wait for the indexer").isTrue();
            assertThat(dumbAfterIndexing).isFalse();
            awaitIdle(project);
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void waitForSmartModeTimesOutWhileScanIsHeld(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager, Files.createTempDirectory("consulo-it-smart-mode-scheduler"));
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);
        UnindexedFilesScannerExecutor executor = UnindexedFilesScannerExecutor.getInstance(project);

        CompletableFuture<Void> gate = new CompletableFuture<>();
        Future<?> held = heldFullScan(project, "held", gate).queue();
        waitFor("the executor must report the held scan as running", () -> executor.isRunning().get());

        long started = System.nanoTime();
        assertThat(dumbService.waitForSmartMode(500)).as("waitForSmartMode must give up while a scan is running").isFalse();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isGreaterThanOrEqualTo(500);
        assertThat(isDumb(dumbService)).isFalse();

        gate.complete(null);
        held.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(dumbService.waitForSmartMode(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))).isTrue();
        assertThat(executor.isRunning().get()).isFalse();
    }

    @Test
    public void modificationTrackerBumpsAfterScanWithoutIndexing(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager, Files.createTempDirectory("consulo-it-smart-mode-scheduler"));
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);

        long before = dumbService.getModificationTracker().getModificationCount();

        RecordingIterator nothing = new RecordingIterator();
        partialScan(project, "nothing to index", nothing).queue().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        awaitIdle(project);

        assertThat(nothing.getIterations()).isEqualTo(1);
        assertThat(dumbService.getModificationTracker().getModificationCount())
            .as("a scan which scheduled no indexing must still advance the dumb service modification tracker")
            .isGreaterThan(before);
    }
}
