// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.logging.Logger;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An executor that can run exactly one task. The task can be re-started many times, but there is at most one running task at any moment.
 * {@link #tryStartProcess} should be invoked to start execution.
 * <p>
 * It is safe to invoke {@link #tryStartProcess} many times, and it is guaranteed that no more than one task is executing at any moment.
 * <p>
 * It is guaranteed that after {@link #tryStartProcess} the task will be executed at least one more time. Task can reset scheduled
 * executions by invoking {@link #clearScheduledFlag}
 */
final class SingleTaskExecutor {
    interface Reportable {
        void run(@Nullable ProgressIndicator visibleIndicator);
    }

    interface AutoclosableProgressive extends AutoCloseable, Reportable {
        @Override
        void close();
    }

    private final class StateAwareTask implements AutoclosableProgressive {
        private final Reportable myTask;
        private final AtomicBoolean myUsed = new AtomicBoolean(false);

        private StateAwareTask(Reportable task) {
            myTask = task;
        }

        @Override
        public void close() {
            if (myUsed.compareAndSet(false, true)) {
                runWithStateHandling(() -> {
                });
            }
        }

        @Override
        public void run(@Nullable ProgressIndicator visibleIndicator) {
            if (myUsed.compareAndSet(false, true)) {
                runWithStateHandling(() -> myTask.run(visibleIndicator));
            }
            else {
                LOG.error("StateAwareTask cannot be reused");
            }
        }
    }

    private enum RunState {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING
    }

    private static final Logger LOG = Logger.getInstance(SingleTaskExecutor.class);

    private final Reportable myTask;
    private final ObservableValue<RunState> myRunState = ObservableValue.of(RunState.STOPPED);
    private final AtomicBoolean myShouldContinueBackgroundProcessing = new AtomicBoolean(false);
    private final ObservableValue<Long> myModificationCount = ObservableValue.of(0L);
    private final ObservableValue<Boolean> myIsRunning = myRunState.map(state -> state != RunState.STOPPED);

    SingleTaskExecutor(Reportable task) {
        myTask = task;
    }

    private void debugLog(Supplier<String> getMessage) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("[" + Thread.currentThread().threadId() + "] " + getMessage.get());
        }
    }

    private void runWithStateHandling(Runnable runnable) {
        try {
            do {
                try {
                    debugLog(() -> "STARTING");
                    RunState currentStateForAssert = myRunState.get();
                    LOG.assertTrue(
                        currentStateForAssert == RunState.STARTING,
                        "Old state should be STARTING, but was " + currentStateForAssert
                    );
                    myRunState.set(RunState.RUNNING);

                    // shouldContinueBackgroundProcessing is normally cleared before reading next item from the queue.
                    // Here we clear the flag just in case, if runnable fail to clear the flag (e.g. during cancellation)
                    myShouldContinueBackgroundProcessing.set(false);
                    debugLog(() -> "RUNNING");
                    runnable.run();
                }
                finally {
                    RunState currentStateForAssert = myRunState.get();
                    LOG.assertTrue(
                        currentStateForAssert == RunState.RUNNING,
                        "Old state should be RUNNING, but was " + currentStateForAssert
                    );
                    myRunState.set(RunState.STOPPING);
                    debugLog(() -> "STOPPING");
                }
            }
            while (myShouldContinueBackgroundProcessing.get() && myRunState.compareAndSet(RunState.STOPPING, RunState.STARTING));
        }
        finally {
            if (myRunState.compareAndSet(RunState.STOPPING, RunState.STOPPED)) {
                debugLog(() -> "STOPPED");
                myModificationCount.update(it -> it + 1);
            }
        }
    }

    /**
     * Generates a task that will be fed into {@code #processRunner}. Consumer must invoke {@code run} or {@code close} on the task.
     * New invocations of this method will have no effect until previous task completes either of its {@code run} or {@code closed}
     * methods
     *
     * @param processRunner receiver for the task that must be executed by consumer (in any thread).
     * @return true if current thread won the competition and started processing
     */
    boolean tryStartProcess(Consumer<AutoclosableProgressive> processRunner) {
        debugLog(() -> "tryStartProcess");
        if (!myShouldContinueBackgroundProcessing.compareAndSet(false, true)) {
            debugLog(() -> "another thread will continue");
            // the thread that set shouldContinueBackgroundProcessing (not this thread) should compete with the background thread
            return false;
        }
        if (myRunState.get() == RunState.RUNNING) {
            debugLog(() -> "already running in another thread");
            return false; // there will be at least one more check of shouldContinueBackgroundProcessing in the background thread
        }
        else {
            // The order is important. The other thread will set states in this order: STOPPING>STOPPED.
            // We should try changing states in the same order, therwise we may endup in STOPPED state:
            // {T1: RUNNING => STOPPING},{T2: STOPPED ~> STARTING},{T1: STOPPING => STOPPED},{T2: STOPPING ~> STARTING}
            boolean stoppingToStarting = myRunState.compareAndSet(RunState.STOPPING, RunState.STARTING);
            boolean stoppedToStarting = myRunState.compareAndSet(RunState.STOPPED, RunState.STARTING);
            boolean thisThreadShouldProcessQueue = stoppingToStarting || stoppedToStarting;
            // whatever thread (this or background) wins the competition and sets STARTING - that thread should process the queue
            if (stoppedToStarting) {
                debugLog(() -> "this thread switched STOPPED > STARTING");
                myModificationCount.update(it -> it + 1);
            }
            else if (!thisThreadShouldProcessQueue) {
                debugLog(() -> "this thread could not acquire STARTING state");
                return false;
            }
        }
        debugLog(() -> "this thread will run the task " + myTask);
        processRunner.accept(new StateAwareTask(myTask));
        return true;
    }

    void clearScheduledFlag() {
        myShouldContinueBackgroundProcessing.set(false);
    }

    ObservableValue<Boolean> isRunning() {
        return myIsRunning;
    }
}
