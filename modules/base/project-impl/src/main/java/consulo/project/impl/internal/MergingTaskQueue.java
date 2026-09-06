// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.internal.AbstractProgressIndicatorExBase;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.MergeableQueueTask;
import consulo.util.lang.ControlFlowException;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MergingTaskQueue<T extends MergeableQueueTask<T>> {
    public static final class SubmissionReceipt {
        private final long mySubmittedTaskCount;

        private SubmissionReceipt(long submittedTaskCount) {
            mySubmittedTaskCount = submittedTaskCount;
        }

        public boolean isAfter(SubmissionReceipt other) {
            return mySubmittedTaskCount > other.mySubmittedTaskCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return mySubmittedTaskCount == ((SubmissionReceipt) o).mySubmittedTaskCount;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(mySubmittedTaskCount);
        }

        @Override
        public String toString() {
            return "SubmissionReceipt{" + mySubmittedTaskCount + '}';
        }
    }

    private static final Logger LOG = Logger.getInstance(MergingTaskQueue.class);

    private final Object myLock = new Object();
    //does not include a running task
    private final List<T> myTasksQueue = new ArrayList<>();

    //includes running tasks too
    private final Map<T, ProgressIndicatorBase> myProgresses = new HashMap<>();
    private final AtomicLong mySubmittedTasksCount = new AtomicLong();

    /**
     * Disposes tasks, cancel underlying progress indicators, clears tasks queue
     */
    public void disposePendingTasks() {
        List<T> disposeQueue;
        List<ProgressIndicatorEx> indicatorsQueue;
        synchronized (myLock) {
            //running task is not included, we must not dispose it if it is (probably) running
            disposeQueue = new ArrayList<>(myTasksQueue);
            // the keys also include a running task(s)
            indicatorsQueue = new ArrayList<>(myProgresses.values());

            myTasksQueue.clear();
            myProgresses.clear();
        }

        cancelIndicatorSafe(indicatorsQueue);
        disposeSafe(disposeQueue);
    }

    // This method is not public because it cannot cancel tasks paused by ProgressSuspender.
    // Use methods from appropriate executor instead (e.g. MergingQueueGuiExecutor#cancelAllTasks)
    public void cancelAllTasks() {
        List<ProgressIndicatorEx> tasks;
        synchronized (myLock) {
            tasks = new ArrayList<>(myProgresses.values());
        }

        for (ProgressIndicatorEx indicator : tasks) {
            indicator.cancel();
        }
    }

    public void cancelTask(T task) {
        ProgressIndicatorEx indicator;
        synchronized (myLock) {
            indicator = myProgresses.get(task);
        }

        if (indicator != null) {
            indicator.cancel();
        }
    }

    /**
     * Adds a task to the queue. Added task can be merged with one of the existing tasks.
     *
     * @param task to add
     * @return receipt that later can be used to handle concurrent operations. Note that addTask may produce duplicate receipt if new task
     * does not modify queue state (e.g. when new task is merged into one of the existing tasks)
     */
    public SubmissionReceipt addTask(T task) {
        List<T> disposeQueue = new ArrayList<>(1);
        T newTask = task;
        SubmissionReceipt receipt;

        synchronized (myLock) {
            for (int i = myTasksQueue.size() - 1; i >= 0; i--) {
                T oldTask = myTasksQueue.get(i);
                ProgressIndicatorBase indicator = myProgresses.get(oldTask);
                //dispose cancelled tasks
                if (indicator == null || indicator.isCanceled()) {
                    myTasksQueue.remove(i);
                    disposeQueue.add(oldTask);
                    continue;
                }

                // note that parent may know nothing about children, so the following may happen
                // (just like in case with `equals` with inheritance):
                //     class Parent; class Child extends Parent
                //     parent.tryMergeWith(child) != child.tryMergeWith(parent)
                // At the moment we prevent accidental errors by forcing tasks' class equality.
                // More permissive strategy (that we don't apply) would be to check "isAssignableFrom" and always use `child.tryMergeWith`
                if (task.getClass() != oldTask.getClass()) {
                    continue;
                }

                T mergedTask = task.tryMergeWith(oldTask);
                if (mergedTask == oldTask) {
                    // new task completely absorbed by the old task which means that we don't need to modify the queue
                    newTask = null;
                    disposeQueue.add(task);
                    break;
                }

                if (mergedTask != null) {
                    LOG.debug("Merged " + task + " with " + oldTask);
                    newTask = mergedTask;
                    myTasksQueue.remove(i);
                    disposeQueue.add(oldTask);
                    if (mergedTask != task) {
                        disposeQueue.add(task);
                    }
                    break;
                }
            }

            //register the new task last, preserving FIFO order
            T taskToAdd = newTask;
            if (taskToAdd != null) {
                myTasksQueue.add(taskToAdd);
                mySubmittedTasksCount.incrementAndGet();
                ProgressIndicatorBase progress = new ProgressIndicatorBase();
                myProgresses.put(taskToAdd, progress);
                Disposer.register(taskToAdd, () -> {
                    //a removed progress means the task would be ignored on queue processing
                    synchronized (myLock) {
                        myProgresses.remove(taskToAdd);
                    }
                    progress.cancel();
                });
            }

            receipt = new SubmissionReceipt(mySubmittedTasksCount.get());
        }

        disposeSafe(disposeQueue);
        return receipt;
    }

    /**
     * @return receipt that equals to the latest receipt returned by {@linkplain #addTask(MergeableQueueTask)}
     */
    public SubmissionReceipt getLatestSubmissionReceipt() {
        synchronized (myLock) {
            // don't be fooled by AtomicLong. We need "synchronized" to make sure that mySubmittedTaskCount and myTasksQueue
            // are changing together
            return new SubmissionReceipt(mySubmittedTasksCount.get());
        }
    }

    public @Nullable QueuedTask<T> extractNextTask() {
        List<Disposable> disposeQueue = new ArrayList<>(1);

        try {
            synchronized (myLock) {
                while (true) {
                    if (myTasksQueue.isEmpty()) {
                        return null;
                    }

                    T task = myTasksQueue.remove(0);

                    ProgressIndicatorBase indicator = myProgresses.get(task);
                    //a disposed task is just ignored here
                    if (indicator == null || indicator.isCanceled()) {
                        disposeQueue.add(task);
                        continue;
                    }

                    return wrapTask(task, indicator);
                }
            }
        }
        finally {
            disposeSafe(disposeQueue);
        }
    }

    protected MergingTaskQueue.QueuedTask<T> wrapTask(T task, ProgressIndicatorBase indicator) {
        return new QueuedTask<>(task, indicator);
    }

    public boolean isEmpty() {
        synchronized (myLock) {
            return myTasksQueue.isEmpty();
        }
    }

    private static void disposeSafe(Collection<? extends Disposable> tasks) {
        for (Disposable task : tasks) {
            disposeSafe(task);
        }
    }

    private static void disposeSafe(Disposable task) {
        try {
            if (Disposer.isDisposed(task)) {
                return;
            }

            Disposer.dispose(task);
        }
        catch (Throwable t) {
            if (!(t instanceof ControlFlowException)) {
                LOG.warn("Failed to dispose task: " + t.getMessage(), t);
            }
        }
    }

    private static void cancelIndicatorSafe(List<? extends ProgressIndicatorEx> indicators) {
        for (ProgressIndicatorEx indicator : indicators) {
            cancelIndicatorSafe(indicator);
        }
    }

    private static void cancelIndicatorSafe(ProgressIndicatorEx indicator) {
        try {
            indicator.cancel();
        }
        catch (Throwable t) {
            if (!(t instanceof ControlFlowException)) {
                LOG.warn("Failed to cancel task indicator: " + t.getMessage(), t);
            }
        }
    }

    public static class QueuedTask<T extends MergeableQueueTask<T>> implements AutoCloseable {
        private final T myTask;
        private final ProgressIndicatorEx myIndicator;

        QueuedTask(T task, ProgressIndicatorEx progress) {
            myTask = task;
            myIndicator = progress;
        }

        @Override
        public void close() {
            Disposer.dispose(myTask);
        }

        public ProgressIndicatorEx getIndicator() {
            return myIndicator;
        }

        public void executeTask(Exception trace) {
            executeTask(null, trace);
        }

        public void executeTask(@Nullable ProgressIndicatorEx visibleIndicator, Exception trace) {
            // this is the cancellation check
            myIndicator.checkCanceled();

            beforeTask();
            if (visibleIndicator != null) {
                myIndicator.addStateDelegate(new AbstractProgressIndicatorExBase() {
                    @Override
                    protected void delegateProgressChange(IndicatorAction action) {
                        super.delegateProgressChange(action);
                        action.execute(visibleIndicator);
                    }
                });
            }
            myIndicator.setIndeterminate(true);

            try {
                ProgressManager.getInstance().runProcess(() -> myTask.perform(myIndicator, trace), myIndicator);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                if (myIndicator.isCanceled()) {
                    LOG.warn("Exception during cancellation of Dumb Task: " + e.getMessage(), e);
                    myIndicator.checkCanceled();
                }
                throw e;
            }
        }

        String getInfoString() {
            return String.valueOf(myTask);
        }

        protected T getTask() {
            return myTask;
        }

        public void beforeTask() {
        }
    }
}
