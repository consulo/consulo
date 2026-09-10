// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.internal.ProgressSuspender;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.MergeableQueueTask;
import consulo.project.Project;
import consulo.project.impl.internal.MergingTaskQueue.QueuedTask;
import consulo.project.impl.internal.MergingTaskQueue.SubmissionReceipt;
import consulo.project.impl.internal.SingleTaskExecutor.AutoclosableProgressive;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Single-threaded executor for {@link MergingTaskQueue}.
 */
public class MergingQueueGuiExecutor<T extends MergeableQueueTask<T>> {
    public interface ExecutorStateListener {
        /**
         * @return {@code false} if queue processing should be terminated ({@link #afterLastTask} will not be invoked in this case).
         * <p>
         * {@code true} to start queue processing.
         */
        boolean beforeFirstTask();

        /**
         * {@link #beforeFirstTask} and {@link #afterLastTask} always follow one after another. Receiving several
         * {@link #beforeFirstTask} or {@link #afterLastTask} in row is always a failure of {@link MergingQueueGuiExecutor}
         * (except the situation when
         * {@link #beforeFirstTask} returns {@code false} - in this case {@link #afterLastTask} will NOT be invoked)
         *
         * @param latestReceipt latest submission receipt as returned by {@link MergingTaskQueue#getLatestSubmissionReceipt} before
         *                      the queue reported that it is empty.
         *                      <p>
         *                      {@code null} when executor has terminated before the queue is empty (e.g. because the
         *                      executor was paused, or unexpected internal error has happened in the executor preventing it from
         *                      further queue processing)
         */
        void afterLastTask(@Nullable SubmissionReceipt latestReceipt);
    }

    private static final class SafeExecutorStateListenerWrapper implements ExecutorStateListener {
        private final ExecutorStateListener myDelegate;

        private SafeExecutorStateListenerWrapper(ExecutorStateListener delegate) {
            myDelegate = delegate;
        }

        @Override
        public boolean beforeFirstTask() {
            try {
                return myDelegate.beforeFirstTask();
            }
            catch (ProcessCanceledException pce) {
                throw pce;
            }
            catch (Exception e) {
                LOG.error(e);
                return false;
            }
        }

        @Override
        public void afterLastTask(@Nullable SubmissionReceipt latestReceipt) {
            try {
                myDelegate.afterLastTask(latestReceipt);
            }
            catch (ProcessCanceledException pce) {
                throw pce;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    private static final Logger LOG = Logger.getInstance(MergingQueueGuiExecutor.class);

    private final Project myProject;
    private final MergingTaskQueue<T> myTaskQueue;
    private final SingleTaskExecutor mySingleTaskExecutor;
    private final ExecutorStateListener myListener;
    private final MergingQueueGuiSuspender myGuiSuspender = new MergingQueueGuiSuspender();
    private final LocalizeValue myProgressTitle;
    private final LocalizeValue mySuspendedText;
    private final AtomicInteger myBackgroundTasksSubmitted = new AtomicInteger(0);
    private final AtomicInteger myScheduledTasks = new AtomicInteger(0);
    private final ExecutorService mySchedulingExecutor;

    protected MergingQueueGuiExecutor(
        Project project,
        MergingTaskQueue<T> taskQueue,
        ExecutorStateListener listener,
        LocalizeValue progressTitle,
        LocalizeValue suspendedText,
        ApplicationConcurrency concurrency
    ) {
        myProject = project;
        myTaskQueue = taskQueue;
        myListener = new SafeExecutorStateListenerWrapper(listener);
        myProgressTitle = progressTitle;
        mySuspendedText = suspendedText;
        mySchedulingExecutor = concurrency.createSequentialApplicationPoolExecutor("MergingQueueGuiExecutor scheduling");
        mySingleTaskExecutor = new SingleTaskExecutor(
            visibleIndicator -> runWithCallbacks(() -> processTasksWithProgress(visibleIndicator))
        );
    }

    public Project getProject() {
        return myProject;
    }

    public MergingTaskQueue<T> getTaskQueue() {
        return myTaskQueue;
    }

    protected MergingQueueGuiSuspender getGuiSuspender() {
        return myGuiSuspender;
    }

    public @Nullable SubmissionReceipt processTasksWithProgress(@Nullable ProgressIndicator visibleIndicator) {
        ProgressSuspender suspender = visibleIndicator == null ? null : ProgressSuspender.getSuspender(visibleIndicator);
        return myGuiSuspender.setCurrentSuspenderAndSuspendIfRequested(suspender, () -> {
            while (true) {
                if (myProject.isDisposed()) {
                    return null;
                }

                // There is no race: we either observe correct latestSubmittedReceipt and no next task, either non-null next task
                // (latestSubmittedReceipt might be stale then, but it is not used in this case anyway)
                SubmissionReceipt submittedTaskCount = myTaskQueue.getLatestSubmissionReceipt();
                mySingleTaskExecutor.clearScheduledFlag(); // reset the flag before peeking the following task
                try (QueuedTask<T> task = myTaskQueue.extractNextTask()) {
                    if (task == null) {
                        return submittedTaskCount;
                    }
                    if (suspender != null) {
                        suspender.attachToProgress(task.getIndicator());
                    }
                    runSingleTask(visibleIndicator, task);
                }
            }
        });
    }

    /**
     * Start task queue processing in background in SINGLE thread. If background process is already running, this method does nothing.
     * <p>
     * It is guaranteed that this method invokes onFinish, even if the method itself threw an exception
     */
    public void startBackgroundProcess(Runnable onFinish) {
        boolean startedInBackground = false;
        try {
            if (myTaskQueue.isEmpty()) {
                return; // there is no race: client first adds a task to myTaskQueue, then invokes startBackgroundProcess
            }
            // this means that if myTaskQueue empty, then recently added task is already handled
            startedInBackground = mySingleTaskExecutor.tryStartProcess(task -> {
                try {
                    // TODO: there seems to be a race between mySingleTaskExecutor.tryStartProcess and FileBasedIndexTumbler. Return now
                    myBackgroundTasksSubmitted.incrementAndGet();
                    AtomicBoolean actionStarted = new AtomicBoolean(false);
                    AtomicBoolean finished = new AtomicBoolean(false);
                    myScheduledTasks.incrementAndGet();
                    Runnable finishOnce = () -> {
                        if (finished.compareAndSet(false, true)) {
                            myScheduledTasks.decrementAndGet();
                            onFinish.run();
                        }
                    };
                    Runnable closeIfNotStarted = () -> {
                        if (!actionStarted.get()) {
                            task.close();
                            finishOnce.run();
                        }
                    };
                    mySchedulingExecutor.execute(() -> {
                        try {
                            ProgressManager.getInstance().run(new Task.Backgroundable(myProject, myProgressTitle, false) {
                                @Override
                                public void run(ProgressIndicator indicator) {
                                    actionStarted.set(true);
                                    try (ProgressSuspender ignored = ProgressSuspender.markSuspendable(indicator, mySuspendedText)) {
                                        try (task) {
                                            task.run(indicator);
                                        }
                                    }
                                    catch (ProcessCanceledException pce) {
                                        throw pce;
                                    }
                                    catch (Throwable t) {
                                        LOG.error("Failed to execute background index update task", t);
                                    }
                                    finally {
                                        // it is important to run onFinish after the task execution and not as a callback
                                        // to Task.Backgroundable because these callbacks are executed on EDT in NON_MODAL,
                                        // while this task can run on background regardless of modality
                                        finishOnce.run();
                                    }
                                }

                                @Override
                                public void onFinished() {
                                    closeIfNotStarted.run();
                                }
                            });
                        }
                        catch (Throwable t) {
                            closeIfNotStarted.run();
                            LOG.error("Failed to start background index update task", t);
                        }
                    });
                }
                catch (ProcessCanceledException pce) {
                    task.close();
                    onFinish.run();
                    throw pce;
                }
                catch (Throwable t) {
                    task.close();
                    mySingleTaskExecutor.clearScheduledFlag();
                    onFinish.run();
                    LOG.error("Failed to start background index update task", t);
                    throw t;
                }
            });
        }
        finally {
            if (!startedInBackground) {
                onFinish.run();
            } // else - will be invoked from a background process
        }
    }

    /**
     * Start task queue processing in this thread under progress indicator. If background thread is already running, this method
     * does nothing and returns immediately.
     */
    boolean tryStartProcessInThisThread(Consumer<AutoclosableProgressive> processRunner) {
        return mySingleTaskExecutor.tryStartProcess(processRunner);
    }

    private void runWithCallbacks(Supplier<@Nullable SubmissionReceipt> runnable) {
        boolean shouldProcessQueue = myListener.beforeFirstTask();
        if (shouldProcessQueue) {
            SubmissionReceipt receipt = null;
            try {
                receipt = runnable.get();
            }
            finally {
                myListener.afterLastTask(receipt);
            }
        }
    }

    public void runSingleTask(@Nullable ProgressIndicator visibleIndicator, QueuedTask<T> task) {
        LOG.info("Running task: " + task.getInfoString());
        try {
            task.executeTask(visibleIndicator instanceof ProgressIndicatorEx indicatorEx ? indicatorEx : null, new Exception());
        }
        catch (ProcessCanceledException e) {
            LOG.info("Task canceled (PCE): " + task.getInfoString());
        }
        catch (CancellationException e) {
            LOG.info("Task canceled (CancellationException): " + task.getInfoString());
        }
        catch (Throwable unexpected) {
            LOG.error("Failed to execute task " + task.getInfoString() + ". " + unexpected.getMessage(), unexpected);
        }
        LOG.info("Task finished: " + task.getInfoString());
    }

    /**
     * @return state containing {@code true} if some task is currently executed in background thread.
     */
    public ObservableValue<Boolean> isRunning() {
        return mySingleTaskExecutor.isRunning();
    }

    public void suspendAndRun(LocalizeValue activityName, Runnable activity) {
        myGuiSuspender.suspendAndRun(activityName, activity);
    }

    public void cancelAllTasks() {
        myTaskQueue.cancelAllTasks();
        myGuiSuspender.resumeProgressIfPossible();
    }

    public int getBackgroundTasksSubmittedCount() {
        return myBackgroundTasksSubmitted.get();
    }

    public boolean hasScheduledTasks() {
        return myScheduledTasks.get() > 0;
    }
}
