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
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.index.ScanningTestSupport.BlockingIterator;
import consulo.it.index.ScanningTestSupport.TestScans;
import consulo.language.index.impl.internal.DumbModeWhileScanningTrigger;
import consulo.language.index.impl.internal.PerProjectIndexingQueue;
import consulo.language.index.impl.internal.roots.ProjectIndexableFilesIteratorImpl;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.DumbModeListenerBackgroundable;
import consulo.project.impl.internal.DumbServiceImpl;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import static consulo.it.index.ScanningTestSupport.isDumb;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.partialScan;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code DumbModeWhileScanningTrigger} enters dumb mode while a scan is still running once the indexing queue holds
 * {@code scanning.dumb.mode.threshold} files, and keeps it until the scan finished and the queue was drained. The
 * headless application is neither a unit test nor a headless environment, so {@code DumbServiceImpl.isSynchronousTaskExecution()}
 * is false and the trigger is subscribed for every opened project without {@code idea.force.dumb.queue.tasks}.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class DumbModeWhileScanningTriggerTest {
    private static final int MANY_FILES = 25;
    private static final int FEW_FILES = 5;

    @Test
    public void manyQueuedFilesEnterDumbModeOnceWhileScanningAndExitAfterIndexing(Application application, ProjectManager projectManager)
        throws Exception {
        assertThat(DumbServiceImpl.isSynchronousTaskExecution()).as("the trigger must be subscribed in the headless application").isFalse();

        Path directory = Files.createTempDirectory("consulo-it-dumb-while-scanning-many");
        Path src = createSandFiles(directory, MANY_FILES, "Many");
        Project project = openProject(application, projectManager, directory);
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);

        UnindexedFilesScannerExecutor executor = UnindexedFilesScannerExecutor.getInstance(project);
        DumbModeWhileScanningTrigger trigger = DumbModeWhileScanningTrigger.getInstance(project);
        PerProjectIndexingQueue indexingQueue = PerProjectIndexingQueue.getInstance(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            TestScans scans = allowOnlyTestScans(project, disposable);
            addContentRoot(project, directory);
            awaitIdle(project);
            VirtualFile srcFile = findFile(src);

            AtomicInteger entered = new AtomicInteger();
            AtomicInteger exited = new AtomicInteger();
            AtomicBoolean queueDrainedAtExit = new AtomicBoolean();
            MessageBusConnection connection = project.getMessageBus().connect(disposable);
            connection.subscribe(DumbModeListenerBackgroundable.class, new DumbModeListenerBackgroundable() {
                @Override
                public void enteredDumbMode() {
                    entered.incrementAndGet();
                }

                @Override
                public void exitDumbMode() {
                    queueDrainedAtExit.set(indexingQueue.getQueuedFiles().isEmpty());
                    exited.incrementAndGet();
                }
            });

            BlockingIterator blocker = new BlockingIterator();
            Future<?> scan = scans.queue(partialScan(project, "many files", new ProjectIndexableFilesIteratorImpl(srcFile), blocker));

            waitFor("dumb mode must be entered while the scan is still running", () -> entered.get() == 1);
            waitFor("the trigger must report its dumb mode", () -> trigger.isDumbModeForScanningActive().get());
            assertThat(isDumb(dumbService)).isTrue();
            assertThat(executor.isRunning().get()).isTrue();
            assertThat(scan.isDone()).isFalse();
            assertThat(indexingQueue.getQueuedFiles().getSize()).isGreaterThanOrEqualTo(20);
            assertThat(exited.get()).isZero();

            blocker.release();
            scan.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            awaitSmart(dumbService);
            awaitIdle(project);
            waitFor("the trigger must release its dumb mode", () -> !trigger.isDumbModeForScanningActive().get());

            assertThat(entered.get()).as("scanning and the indexing it scheduled must share a single dumb mode").isEqualTo(1);
            assertThat(exited.get()).isEqualTo(1);
            assertThat(queueDrainedAtExit).as("dumb mode must end only after the indexer took the queued files").isTrue();
            assertThat(isDumb(dumbService)).isFalse();
            for (int i = 0; i < MANY_FILES; i++) {
                assertThat(findClasses(project, "Many" + i)).as("Many%d must be indexed", i).hasSize(1);
            }
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void fewQueuedFilesDoNotEnterDumbModeWhileScanning(Application application, ProjectManager projectManager) throws Exception {
        assertThat(DumbServiceImpl.isSynchronousTaskExecution()).as("the trigger must be subscribed in the headless application").isFalse();

        Path directory = Files.createTempDirectory("consulo-it-dumb-while-scanning-few");
        Path src = createSandFiles(directory, FEW_FILES, "Few");
        Project project = openProject(application, projectManager, directory);
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);

        DumbModeWhileScanningTrigger trigger = DumbModeWhileScanningTrigger.getInstance(project);
        PerProjectIndexingQueue indexingQueue = PerProjectIndexingQueue.getInstance(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            TestScans scans = allowOnlyTestScans(project, disposable);
            addContentRoot(project, directory);
            awaitIdle(project);
            VirtualFile srcFile = findFile(src);

            AtomicInteger entered = new AtomicInteger();
            AtomicBoolean triggerActivated = new AtomicBoolean();
            MessageBusConnection connection = project.getMessageBus().connect(disposable);
            connection.subscribe(DumbModeListenerBackgroundable.class, new DumbModeListenerBackgroundable() {
                @Override
                public void enteredDumbMode() {
                    entered.incrementAndGet();
                }
            });
            Runnable unsubscribe = trigger.isDumbModeForScanningActive().addListener(active -> {
                if (active) {
                    triggerActivated.set(true);
                }
            });

            try {
                BlockingIterator blocker = new BlockingIterator();
                Future<?> scan = scans.queue(partialScan(project, "few files", new ProjectIndexableFilesIteratorImpl(srcFile), blocker));
                waitFor("the scan must collect the files", () -> indexingQueue.getQueuedFiles().getSize() >= FEW_FILES);
                blocker.awaitStarted();

                assertThat(trigger.isDumbModeForScanningActive().get()).isFalse();
                assertThat(isDumb(dumbService)).as("few files must not start dumb mode during scanning").isFalse();
                assertThat(entered.get()).isZero();

                blocker.release();
                scan.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                awaitSmart(dumbService);
                awaitIdle(project);

                assertThat(triggerActivated).as("the trigger must stay inactive below the threshold").isFalse();
                assertThat(entered.get()).as("only the indexer itself runs in dumb mode, after the scan").isEqualTo(1);
                for (int i = 0; i < FEW_FILES; i++) {
                    assertThat(findClasses(project, "Few" + i)).as("Few%d must be indexed", i).hasSize(1);
                }
            }
            finally {
                unsubscribe.run();
            }
        }
        finally {
            Disposer.dispose(disposable);
        }
    }
}
