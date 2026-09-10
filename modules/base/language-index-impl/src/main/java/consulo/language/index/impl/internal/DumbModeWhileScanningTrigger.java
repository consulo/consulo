// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.index.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.impl.internal.DumbServiceImpl;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.AwaitValue;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.Loop;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tracks {@link PerProjectIndexingQueue} state and starts dumb mode if too many dirty files in the queue.
 * You don't need this service except for statistics, because dumb mode can start for many different reasons, not only because of scanning.
 * Use {@link consulo.project.DumbService} to schedule tasks in smart mode.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class DumbModeWhileScanningTrigger implements Disposable {
    private static final int DUMB_MODE_THRESHOLD = Registry.intValue("scanning.dumb.mode.threshold", 20);

    public static DumbModeWhileScanningTrigger getInstance(Project project) {
        return project.getInstance(DumbModeWhileScanningTrigger.class);
    }

    private final Project myProject;
    private final CoroutineScope myCoroutineScope;
    private final ObservableValue<Boolean> myDumbModeForScanningIsActive = ObservableValue.of(false);

    @Inject
    public DumbModeWhileScanningTrigger(Project project) {
        myProject = project;
        myCoroutineScope = CoroutineScope.of(project.coroutineContext());
    }

    public ObservableValue<Boolean> isDumbModeForScanningActive() {
        return myDumbModeForScanningIsActive;
    }

    public void subscribe() {
        if (DumbServiceImpl.isSynchronousTaskExecution()) {
            // in synchronous mode it will be a deadlock
            return;
        }

        ObservableValue<Boolean> manyFilesChanged = PerProjectIndexingQueue.getInstance(myProject)
            .estimatedFilesCount()
            .map(count -> count >= DUMB_MODE_THRESHOLD);

        ObservableValue<Boolean> scanningInProgress = UnindexedFilesScannerExecutor.getInstance(myProject).isRunning();

        Coroutine<Boolean, Boolean> iteration = Coroutine.<Boolean, Boolean>first(AwaitValue.until(manyFilesChanged, Boolean::booleanValue))
            .then(CodeExecution.apply(manyFiles -> {
                runDumbModeWhileScanning(manyFilesChanged, scanningInProgress);
                return Boolean.FALSE;
            }));

        Coroutine.first(Loop.loopWhile(value -> !myProject.isDisposed(), CallSubroutine.call(iteration)))
            .runAsync(myCoroutineScope, Boolean.FALSE);
    }

    private void runDumbModeWhileScanning(ObservableValue<Boolean> manyFilesChanged, ObservableValue<Boolean> scanningInProgress) {
        myDumbModeForScanningIsActive.set(true);
        try {
            DumbService.getInstance(myProject).runInDumbMode("Waiting for scanning to complete", () -> {
                // this is kind of trigger with memory: to start dumb mode, it's enough to have many changed files, but to end dumb mode
                // we also should wait for all the scanning tasks to finish.
                // also wait for all the other scanning tasks to complete before starting indexing tasks
                awaitScanningFinishedAndQueueDrained(manyFilesChanged, scanningInProgress);
            });
        }
        finally {
            myDumbModeForScanningIsActive.set(false);
        }
    }

    private void awaitScanningFinishedAndQueueDrained(
        ObservableValue<Boolean> manyFilesChanged,
        ObservableValue<Boolean> scanningInProgress
    ) {
        CountDownLatch finished = new CountDownLatch(1);
        Runnable check = () -> {
            if (!manyFilesChanged.get() && !scanningInProgress.get()) {
                finished.countDown();
            }
        };
        Runnable removeManyFilesListener = manyFilesChanged.addListener(value -> check.run());
        Runnable removeScanningListener = scanningInProgress.addListener(value -> check.run());
        try {
            check.run();
            while (!myProject.isDisposed()) {
                ProgressManager.checkCanceled();
                try {
                    if (finished.await(50, TimeUnit.MILLISECONDS)) {
                        return;
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        finally {
            removeManyFilesListener.run();
            removeScanningListener.run();
        }
    }

    @Override
    public void dispose() {
        myCoroutineScope.cancel();
    }
}
