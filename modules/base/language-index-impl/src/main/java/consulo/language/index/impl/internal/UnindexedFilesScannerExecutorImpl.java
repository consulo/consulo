// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessToken;
import consulo.application.ReadAction;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.PingProgress;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.component.util.ModificationTracker;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.index.impl.internal.dependencies.ProjectIndexingDependenciesService;
import consulo.language.index.impl.internal.gist.GistManagerImpl;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.psi.stub.gist.GistManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.internal.FilesScanningTask;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.Mutex;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.AwaitValue;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.Loop;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.ObjectUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public final class UnindexedFilesScannerExecutorImpl implements Disposable, UnindexedFilesScannerExecutor {
    private static final Logger LOG = Logger.getInstance(UnindexedFilesScannerExecutor.class);

    private static final String MUTEX_OWNER = "scanning";

    public static UnindexedFilesScannerExecutorImpl getInstance(Project project) {
        return (UnindexedFilesScannerExecutorImpl) UnindexedFilesScannerExecutor.getInstance(project);
    }

    private final Project myProject;

    // helpers for tests
    private final ObservableValue<@Nullable Boolean> myScanningWaitsForNonDumbModeOverride = ObservableValue.of(null);
    private volatile @Nullable Predicate<UnindexedFilesScanner> myTaskFilter;

    // note that shouldShowProgressIndicator = false in UnindexedFilesScannerExecutor, so there is no suspender for the progress indicator
    private final ObservableValue<List<LocalizeValue>> myPauseReason = ObservableValue.of(List.of());

    // 1. Should only be SET inside WA to prevent mode change during RA.
    // 2. Will be cleared without WA to avoid deadlocks when some code waits for smart mode under modal progress
    //    (at the moment there are no real arguments for WA to clear the flag)
    // 3. You may set isRunning = true anywhere in the code (given, that it is set under WA), but never set to false.
    //    Only executor coroutine may set it to false, otherwise isRunning will be cleared in the middle of scanning task execution.
    private final ObservableValue<Boolean> myIsRunning = ObservableValue.of(false);

    /**
     * Modification counter that increases each time the executor starts or stops
     * <p>
     * This is not the same as {@link #myIsRunning}, because {@link #myIsRunning} is a state flow, meaning that it is conflated and
     * deduplicated, i.e. short transitions true-false-true can be missed in {@link #myIsRunning}. {@link #myStartedOrStoppedEvent} is still
     * conflated, but never miss the latest event.
     * <p>
     * TODO: {@link #myIsRunning} should be a shared flow without deduplication, then we wont need {@link #myStartedOrStoppedEvent}
     */
    private final ObservableValue<Integer> myStartedOrStoppedEvent = ObservableValue.of(0);

    private final AtomicLong myModCount = new AtomicLong();
    private final ModificationTracker myModificationTracker = new ModificationTracker() {
        @Override
        public long getModificationCount() {
            return myModCount.get();
        }
    };

    private static final class ScheduledScanningTask {
        private final UnindexedFilesScanner myTask;
        private final CompletableFuture<Void> myFutureHistory;

        private ScheduledScanningTask(UnindexedFilesScanner task, CompletableFuture<Void> futureHistory) {
            myTask = task;
            myFutureHistory = futureHistory;
        }

        void close() {
            myTask.close();
        }
    }

    private final ObservableValue<@Nullable ScheduledScanningTask> myScanningTask = ObservableValue.of(null);
    private final ObservableValue<Boolean> myScanningEnabled = ObservableValue.of(true);

    private final CoroutineScope myCoroutineScope;

    private volatile @Nullable ProgressIndicator myRunningTask;

    @Inject
    public UnindexedFilesScannerExecutorImpl(Project project) {
        myProject = project;
        myCoroutineScope = CoroutineScope.of(project.coroutineContext());

        Coroutine.first(Loop.loopWhile(value -> !myProject.isDisposed(), CallSubroutine.call(this::scanningTaskExecutionTrigger)))
            .withName("scanning task execution trigger")
            .runAsync(myCoroutineScope, Boolean.FALSE);

        Coroutine.first(Loop.loopWhile(value -> !myProject.isDisposed(), CallSubroutine.call(this::scanningTaskExecution)))
            .withName("Scanning (root)")
            .runAsync(myCoroutineScope, Boolean.FALSE);
    }

    private Coroutine<Boolean, Boolean> scanningTaskExecutionTrigger() {
        ObservableValue<DumbService.DumbState> dumbState = DumbService.getInstance(myProject).getState();
        return Coroutine.<Boolean, Boolean>first(AwaitValue.until(
                () -> !myIsRunning.get() && isNextTaskExecutionAllowed(),
                myIsRunning, myScanningEnabled, myScanningTask, dumbState, myScanningWaitsForNonDumbModeOverride
            ))
            // write action is needed, because otherwise we may get "Constraint inSmartMode cannot be satisfied" in NBRA
            .then(WriteLock.apply(value -> {
                // we should only set the flag here (if needed), not clear it,
                // otherwise, isRunning may become false in the middle of scanning task execution
                myIsRunning.set(myIsRunning.get() || myScanningTask.get() != null);
                return Boolean.TRUE;
            }));
    }

    private Coroutine<Boolean, Boolean> scanningTaskExecution() {
        Mutex scanningIndexingMutex = PerProjectIndexingQueue.getInstance(myProject).getScanningIndexingMutex();
        // first wait for isRunning, otherwise we can find ourselves in a situation
        // isRunning=false, hasScheduledTask=false, but in fact we do have a scheduled task
        // which is about to be running.
        return Coroutine.<Boolean, Boolean>first(AwaitValue.until(myIsRunning, Boolean::booleanValue))
            .then(scanningIndexingMutex.lock(MUTEX_OWNER))
            .then(CodeExecution.apply(value -> {
                runNextScanningTask(scanningIndexingMutex);
                return Boolean.TRUE;
            }));
    }

    private void runNextScanningTask(Mutex scanningIndexingMutex) {
        try {
            if (!isNextTaskExecutionAllowed()) {
                return; // to finally block which will clear isRunning flag and release scanningIndexingMutex
                // There are no situations where we need isRunning to be cleared, neither we have situations where we need isRunning stay
                // intact.
                // Feel free to adjust this logic as needed. Clearing the flag looks like the "least surprising" behavior to me.
            }

            myStartedOrStoppedEvent.getAndUpdate(value -> value + 1);

            ScheduledScanningTask task = myScanningTask.getAndUpdate(value -> null);
            if (task == null) {
                return;
            }
            try {
                logInfo("Running task: " + task.myTask);
                LOG.assertTrue(myRunningTask == null, "Task is already running (will be cancelled)");
                ProgressIndicator previousRunningTask = myRunningTask;
                if (previousRunningTask != null) {
                    previousRunningTask.cancel(); // We expect that running task is null. But it's better to be on the safe side
                }
                ScanningParameters scanningParameters = task.myTask.getScanningParameters();
                if (scanningParameters instanceof ScanningIterators scanningIterators) {
                    try {
                        runScanningTask(task.myTask, scanningIterators);
                    }
                    finally {
                        // Scanning may throw exception (or error).
                        // In this case, we should either clear or flush the indexing queue; otherwise, dumb mode will not end in the
                        // project.
                        // TODO: we should flush the queue before setting the future, otherwise we have a race in UnindexedFilesScannerTest:
                        //  it clears "allowFlushing" after future is set, expecting that if flush might be called, it had already been
                        //  called
                        boolean indexingScheduled = PerProjectIndexingQueue.getInstance(myProject)
                            .flushNow(scanningIterators.getIndexingReason());
                        if (!indexingScheduled) {
                            myModCount.incrementAndGet();
                        }
                    }
                    task.myFutureHistory.complete(null);
                    logInfo("Task finished (scanning id=" + task.myTask.getScanningSessionId() + "): " + task.myTask);
                }
                else {
                    logInfo("Skipping task: " + task.myTask);
                }
            }
            catch (Throwable t) {
                task.myFutureHistory.completeExceptionally(t);
                logInfo("Task interrupted: " + task.myTask + ". " + t.getMessage());
                ProjectIndexingDependenciesService.getInstance(myProject)
                    .requestHeavyScanningOnProjectOpen("Task interrupted: " + task.myTask);

                // other exceptions: log and forget
                Throwable cause = t instanceof CompletionException || t instanceof ExecutionException
                    ? ObjectUtil.notNull(t.getCause(), t)
                    : t;
                if (cause instanceof ControlFlowException || cause instanceof CancellationException) {
                    String message = prepareLogMessage("Task was cancelled: " + task.myTask
                        + ". (enable debug log to see cancellation trace)");
                    LOG.info(message, LOG.isDebugEnabled() ? new RuntimeException(t) : null);
                }
                else {
                    logError("Failed to execute task " + task.myTask, t);
                }
            }
            finally {
                task.close();
                myRunningTask = null;
            }
        }
        catch (Throwable t) {
            // other exceptions: log and forget
            try {
                logError("Unexpected exception during scanning (ignored)", t);
            }
            catch (Throwable ignored) {
                // If logError throws, we ignore this exception, because this will stop scanning service for the project.
                // NOTE: logError throws AE in tests.
            }
        }
        finally {
            // There is no race. When a task is submitted, the reference to scanningTask is updated first (hasQueuedTasks == true), then
            // optionally, isRunning set to true. There is no chance clear isRunning flag by accident.
            //
            // We don't use WA. This allows scanning finishing during RA or while modal dialog is shown
            // (feel free to add WA if you know why finishing is not desired)
            myIsRunning.set(hasQueuedTasks());
            myStartedOrStoppedEvent.getAndUpdate(value -> value + 1);
            scanningIndexingMutex.unlock(MUTEX_OWNER);
        }
    }

    private String prepareLogMessage(String message) {
        return "[" + myProject.getLocationHash() + "] " + message;
    }

    private void logInfo(String message) {
        LOG.info(prepareLogMessage(message));
    }

    private void logError(String message, Throwable t) {
        LOG.error(prepareLogMessage(message), t);
    }

    private static boolean scanningWaitsForNonDumbMode(@Nullable Boolean override) {
        return override != null ? override : Registry.is("scanning.waits.for.non.dumb.mode", true);
    }


    public boolean scanningWaitsForNonDumbMode() {
        return scanningWaitsForNonDumbMode(myScanningWaitsForNonDumbModeOverride.get());
    }

    private boolean isNextTaskExecutionAllowed() {
        boolean enabled = myScanningEnabled.get();
        boolean hasTask = myScanningTask.get() != null;
        boolean isRunning = myIsRunning.get();
        boolean isDumb = false;
        boolean shouldWaitForNonDumb = false;

        // Delay scanning tasks until all the scheduled dumb tasks are finished.
        // For example, PythonLanguageLevelPusher.initExtra is invoked from RequiredForSmartModeActivity and may submit additional dumb
        // tasks.
        // We want scanning to start after all these "extra" dumb tasks are finished.
        // Note that a project may become dumb immediately after the check. This is not a problem - we schedule scanning anyway.
        if (scanningWaitsForNonDumbMode()) {
            isDumb = DumbService.getInstance(myProject).getState().get().isDumb();
            shouldWaitForNonDumb = scanningWaitsForNonDumbMode(myScanningWaitsForNonDumbModeOverride.get());
        }

        return enabled && hasTask &&
            // Warning: don't wait for smart mode if scanning is already running
            (isRunning || !(isDumb && shouldWaitForNonDumb));
    }

    @TestOnly
    public void overrideScanningWaitsForNonDumbMode(@Nullable Boolean newValue) {
        myScanningWaitsForNonDumbModeOverride.set(newValue);
    }

    private void runScanningTask(UnindexedFilesScanner task, ScanningIterators scanningParameters) {
        ObservableValue<Boolean> shouldShowProgress = task.shouldHideProgressInSmartMode()
            ? DumbModeWhileScanningTrigger.getInstance(myProject).isDumbModeForScanningActive()
            : ObservableValue.of(true);

        IndexingProgressReporter progressReporter = new IndexingProgressReporter();
        IndexingProgressReporter.CheckPauseOnlyProgressIndicatorImpl taskIndicator =
            new IndexingProgressReporter.CheckPauseOnlyProgressIndicatorImpl(getPauseReason());
        Runnable stopProgressReporting = IndexingProgressReporter.launchIndexingProgressUIReporter(
            myProject,
            shouldShowProgress,
            progressReporter,
            IndexingLocalize.progressIndexingScanning(),
            taskIndicator.getPauseReason()
        );

        try {
            ProgressIndicator indicator = taskIndicator.getProgressIndicator();
            myRunningTask = indicator;
            ProgressManager.getInstance().runProcess(() -> {
                try (AccessToken ignored = ((GistManagerImpl) GistManager.getInstance()).mergeDependentCacheInvalidations()) {
                    task.applyDelayedPushOperations();
                }
                task.perform(taskIndicator, progressReporter, scanningParameters);
            }, indicator);
        }
        finally {
            stopProgressReporting.run();
            taskIndicator.dispose();
        }
    }

    private void cancelAllTasks(String debugReason) {
        boolean wasEnabled = myScanningEnabled.get();
        if (wasEnabled) {
            myScanningEnabled.set(false);
        }
        try {
            ScheduledScanningTask task = myScanningTask.getAndUpdate(value -> null);
            if (task != null) {
                task.close();
            }
            ProgressIndicator runningTask = myRunningTask;
            if (runningTask != null) {
                logInfo("Cancelling running scanning task: " + debugReason);
                runningTask.cancel();
            }
        }
        finally {
            if (wasEnabled) {
                myScanningEnabled.set(true);
            }
        }
    }

    @Override
    public Future<?> submitTask(FilesScanningTask filesScanningTask) {
        UnindexedFilesScanner task = (UnindexedFilesScanner) filesScanningTask;

        Predicate<UnindexedFilesScanner> taskFilter = myTaskFilter;
        if (taskFilter != null && !taskFilter.test(task)) {
            logInfo("Skipping task (rejected by filter): " + task);
            task.close();
            return CompletableFuture.failedFuture(new RejectedExecutionException("(rejected by filter)"));
        }

        // Two tasks with limited checks should be just run one after another.
        // A case of a full check followed by a limited change cancelling the first one and making a full check anew results
        // in endless restart of full checks on Windows with empty Maven cache.
        // So only in case the second one is a full check should the first one be cancelled.
        Boolean isFullIndexUpdate = task.isFullIndexUpdate();
        if (isFullIndexUpdate != null && isFullIndexUpdate) {
            // we don't want to execute any of the existing tasks - the only task we want to execute will be submitted the few lines below
            cancelAllTasks("Full scanning is queued");
        }

        Future<?> res = startTaskInSmartMode(task);

        ApplicationEx application = (ApplicationEx) myProject.getApplication();
        if (application.isWriteThread()) {
            // make this executor "running" immediately: clients immediately invoking "runWhenSmart" expect that this scanning is processed
            // first.
            application.runWriteAction(this::markAsRunning);
        }
        else if (DumbService.isDumb(myProject)) {
            // here we want to immediately "start" executor without EDT in the case when scanning is started under a dumb task.
            // Acquire RA to make sure that dumb mode won't change to smart, otherwise we risk changing smart mode to not-smart outside WA.
            ReadAction.run(() -> {
                if (DumbService.isDumb(myProject)) {
                    markAsRunning();
                }
            });
        }

        return res;
    }

    // should be invoked under RA + dumb mode or WA
    private void markAsRunning() {
        if (hasQueuedTasks()) {
            myIsRunning.set(true);
        } // else: the task is already picked by the executor. Don't touch isRunning in this case.
        // There is no problem if the task is not only picked by the executor, but also completed, and isRunning is
        // already set to false - there will be one "empty" cycle performed by the executor, and nothing bad.
    }

    private Future<?> startTaskInSmartMode(UnindexedFilesScanner task) {
        ScheduledScanningTask newTask;
        boolean updated;
        do {
            ScheduledScanningTask old = myScanningTask.get();
            if (old != null) {
                newTask = new ScheduledScanningTask(old.myTask.tryMergeWith(task), old.myFutureHistory);
            }
            else {
                newTask = new ScheduledScanningTask(task, new CompletableFuture<>());
            }

            updated = myScanningTask.compareAndSet(old, newTask);
            if (updated) {
                if (old != null) {
                    old.close();
                }
                if (newTask.myTask != task) {
                    task.close();
                }
            }
            else {
                newTask.close();
            }
        }
        while (!updated);
        return newTask.myFutureHistory;
    }

    @Override
    public void cancelAllTasksAndWait() {
        cancelAllTasks("cancelAllTasksAndWait"); // this also cancels a running task even if it is paused by ProgressSuspender
        // we don't check isRunning here, because this method is usually invoked on EDT. There is no chance for a bgt thread to clear
        // isRunning flag.
        while (myRunningTask != null && !myProject.isDisposed()) {
            PingProgress.interactWithEdtProgress();
            LockSupport.parkNanos(50_000_000);
        }
    }

    @Override
    public void dispose() {
        myCoroutineScope.cancel();
        ScheduledScanningTask task = myScanningTask.getAndUpdate(value -> null);
        if (task != null) {
            task.close();
        }
        ProgressIndicator runningTask = myRunningTask;
        if (runningTask != null) {
            runningTask.cancel();
        }
    }

    /**
     * This method does not have "happens before" semantics. It requests GUI suspender to suspend and executes runnable without waiting for
     * all the running tasks to pause.
     */
    @Override
    public void suspendScanningAndIndexingThenRun(LocalizeValue activityName, Runnable runnable) {
        myPauseReason.update(reasons -> adding(reasons, activityName));
        try {
            DumbService.getInstance(myProject).suspendIndexingAndRun(activityName, runnable);
        }
        finally {
            myPauseReason.update(reasons -> removing(reasons, activityName));
        }
    }

    private static List<LocalizeValue> adding(List<LocalizeValue> reasons, LocalizeValue reason) {
        List<LocalizeValue> copy = new ArrayList<>(reasons);
        copy.add(reason);
        return List.copyOf(copy);
    }

    private static List<LocalizeValue> removing(List<LocalizeValue> reasons, LocalizeValue reason) {
        List<LocalizeValue> copy = new ArrayList<>(reasons);
        copy.remove(reason);
        return List.copyOf(copy);
    }

    @Override
    public void suspendQueue() {
        myScanningEnabled.set(false);
    }

    @Override
    public void resumeQueue() {
        myScanningEnabled.set(true);
    }

    @Override
    public boolean hasQueuedTasks() {
        return myScanningTask.get() != null;
    }

    @Override
    public ObservableValue<Boolean> isRunning() {
        return myIsRunning;
    }

    @Override
    public ObservableValue<Integer> startedOrStoppedEvent() {
        return myStartedOrStoppedEvent;
    }

    @Override
    public ModificationTracker getModificationTracker() {
        return myModificationTracker;
    }

    @Override
    public ObservableValue<List<LocalizeValue>> getPauseReason() {
        return myPauseReason;
    }

    @TestOnly
    public void setTaskFilterInTest(Disposable disposable, Predicate<UnindexedFilesScanner> filter) {
        Disposer.register(disposable, () -> myTaskFilter = null);
        myTaskFilter = filter;
    }
}
