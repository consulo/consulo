// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.concurrency;

import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.ex.util.Invoker;
import consulo.util.collection.Sets;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.concurrent.Obsolescent;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static consulo.disposer.Disposer.register;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class InvokerImpl implements Disposable, Invoker {
    private static final int THRESHOLD = Integer.MAX_VALUE;
    private static final Logger LOG = Logger.getInstance(InvokerImpl.class);
    private static final AtomicInteger UID = new AtomicInteger();
    private final Map<AsyncPromise<?>, ProgressIndicatorBase> indicators = new ConcurrentHashMap<>();
    private final AtomicInteger count = new AtomicInteger();
    private final String description;
    private volatile boolean disposed;

    private InvokerImpl(@Nonnull String prefix, @Nonnull Disposable parent) {
        description = "Invoker." + UID.getAndIncrement() + "." + prefix + ": " + parent;
        register(parent, this);
    }

    @Override
    public String toString() {
        return description;
    }

    @Override
    public void dispose() {
        disposed = true;
        while (!indicators.isEmpty()) {
            indicators.keySet().forEach(AsyncPromise::cancel);
        }
    }

    /**
     * Computes the specified task immediately if the current thread is valid,
     * or asynchronously after all pending tasks have been processed.
     *
     * @param task a task to execute on the valid thread
     * @return an object to control task processing
     */
    @Nonnull
    public final <T> CancellablePromise<T> compute(@Nonnull Supplier<? extends T> task) {
        return promise(new Task<>(task));
    }

    /**
     * Computes the specified task asynchronously on the valid thread.
     * Even if this method is called from the valid thread
     * the specified task will still be deferred
     * until all pending events have been processed.
     *
     * @param task a task to execute asynchronously on the valid thread
     * @return an object to control task processing
     */
    @Nonnull
    public final <T> CancellablePromise<T> computeLater(@Nonnull Supplier<? extends T> task) {
        return computeLater(task, 0);
    }

    /**
     * Computes the specified task on the valid thread after the specified delay.
     *
     * @param task  a task to execute asynchronously on the valid thread
     * @param delay milliseconds for the initial delay
     * @return an object to control task processing
     */
    @Nonnull
    public final <T> CancellablePromise<T> computeLater(@Nonnull Supplier<? extends T> task, int delay) {
        return promise(new Task<>(task), delay);
    }

    /**
     * Invokes the specified task immediately if the current thread is valid,
     * or asynchronously after all pending tasks have been processed.
     *
     * @param task a task to execute on the valid thread
     * @return an object to control task processing
     */
    @Nonnull
    @Override
    public final CancellablePromise<?> invoke(@Nonnull Runnable task) {
        return compute(new Wrapper(task));
    }

    /**
     * Invokes the specified task asynchronously on the valid thread.
     * Even if this method is called from the valid thread
     * the specified task will still be deferred
     * until all pending events have been processed.
     *
     * @param task a task to execute asynchronously on the valid thread
     * @return an object to control task processing
     */
    @Nonnull
    @Override
    public final CancellablePromise<?> invokeLater(@Nonnull Runnable task) {
        return invokeLater(task, 0);
    }

    /**
     * Invokes the specified task on the valid thread after the specified delay.
     *
     * @param task  a task to execute asynchronously on the valid thread
     * @param delay milliseconds for the initial delay
     * @return an object to control task processing
     */
    @Nonnull
    @Override
    public final CancellablePromise<?> invokeLater(@Nonnull Runnable task, int delay) {
        return computeLater(new Wrapper(task), delay);
    }

    /**
     * Invokes the specified task immediately if the current thread is valid,
     * or asynchronously after all pending tasks have been processed.
     *
     * @param task a task to execute on the valid thread
     * @return an object to control task processing
     * @deprecated use {@link #invoke(Runnable)} or {@link #compute(Supplier)} instead
     */
    @Deprecated
    @Nonnull
    @Override
    public final CancellablePromise<?> runOrInvokeLater(@Nonnull Runnable task) {
        return invoke(task);
    }

    /**
     * Returns a workload of the task queue.
     *
     * @return amount of tasks, which are executing or waiting for execution
     */
    @Override
    public final int getTaskCount() {
        return disposed ? 0 : count.get();
    }

    abstract void offer(@Nonnull Runnable runnable, int delay);

    /**
     * @param task    a task to execute on the valid thread
     * @param attempt an attempt to run the specified task
     * @param delay   milliseconds for the initial delay
     */
    private void offerSafely(@Nonnull Task<?> task, int attempt, int delay) {
        try {
            count.incrementAndGet();
            offer(() -> invokeSafely(task, attempt), delay);
        }
        catch (RejectedExecutionException exception) {
            count.decrementAndGet();
            if (LOG.isTraceEnabled()) {
                LOG.debug("Executor is shutdown");
            }
            task.promise.setError("shutdown");
        }
    }

    /**
     * @param task    a task to execute on the valid thread
     * @param attempt an attempt to run the specified task
     */
    private void invokeSafely(@Nonnull Task<?> task, int attempt) {
        try {
            if (task.canInvoke(disposed)) {
                ProgressManager.getInstance().runProcess(task, indicator(task.promise));

                task.setResult();
            }
        }
        catch (ProcessCanceledException | IndexNotReadyException exception) {
            offerRestart(task, attempt);
        }
        catch (Throwable throwable) {
            try {
                LOG.error(throwable);
            }
            finally {
                task.promise.setError(throwable);
            }
        }
        finally {
            count.decrementAndGet();
        }
    }

    /**
     * @param task    a task to execute on the valid thread
     * @param attempt an attempt to run the specified task
     */
    private void offerRestart(@Nonnull Task<?> task, int attempt) {
        if (task.canRestart(disposed, attempt)) {
            offerSafely(task, attempt + 1, 10);
            if (LOG.isTraceEnabled()) {
                LOG.debug("Task is restarted");
            }
        }
    }

    /**
     * Promises to invoke the specified task immediately if the current thread is valid,
     * or asynchronously after all pending tasks have been processed.
     *
     * @param task a task to execute on the valid thread
     * @return an object to control task processing
     */
    @Nonnull
    private <T> CancellablePromise<T> promise(@Nonnull Task<T> task) {
        if (!isValidThread()) {
            return promise(task, 0);
        }
        count.incrementAndGet();
        invokeSafely(task, 0);
        return task.promise;
    }

    /**
     * Promises to invoke the specified task on the valid thread after the specified delay.
     *
     * @param task  a task to execute on the valid thread
     * @param delay milliseconds for the initial delay
     * @return an object to control task processing
     */
    @Nonnull
    private <T> CancellablePromise<T> promise(@Nonnull Task<T> task, int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("delay must be non-negative: " + delay);
        }
        if (task.canInvoke(disposed)) {
            offerSafely(task, 0, delay);
        }
        return task.promise;
    }

    /**
     * This data class is intended to combine a developer's task
     * with the corresponding object used to control its processing.
     */
    static final class Task<T> implements Runnable {
        final AsyncPromise<T> promise = new AsyncPromise<>();
        private final Supplier<? extends T> supplier;
        private volatile T result;

        Task(@Nonnull Supplier<? extends T> supplier) {
            this.supplier = supplier;
        }

        boolean canRestart(boolean disposed, int attempt) {
            if (LOG.isTraceEnabled()) {
                LOG.debug("Task is canceled");
            }
            if (attempt < THRESHOLD) {
                return canInvoke(disposed);
            }
            LOG.warn("Task is always canceled: " + supplier);
            promise.setError("timeout");
            return false; // too many attempts to run the task
        }

        boolean canInvoke(boolean disposed) {
            if (promise.isDone()) {
                if (LOG.isTraceEnabled()) {
                    LOG.debug("Promise is cancelled: ", promise.isCancelled());
                }
                return false; // the given promise is already done or cancelled
            }
            if (disposed) {
                if (LOG.isTraceEnabled()) {
                    LOG.debug("Invoker is disposed");
                }
                promise.setError("disposed");
                return false; // the current invoker is disposed
            }
            if (supplier instanceof Obsolescent obsolescent) {
                if (obsolescent.isObsolete()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.debug("Task is obsolete");
                    }
                    promise.setError("obsolete");
                    return false; // the specified task is obsolete
                }
            }
            return true;
        }

        void setResult() {
            promise.setResult(result);
        }

        @Override
        public void run() {
            result = supplier.get();
        }

        @Override
        public String toString() {
            return "Invoker.Task: " + supplier;
        }
    }


    /**
     * This wrapping class is intended to convert a developer's runnable to the obsolescent supplier.
     */
    private static final class Wrapper implements Obsolescent, Supplier<Void> {
        private final Runnable task;

        Wrapper(@Nonnull Runnable task) {
            this.task = task;
        }

        @Override
        public Void get() {
            task.run();
            return null;
        }

        @Override
        public boolean isObsolete() {
            return task instanceof Obsolescent obsolescent && obsolescent.isObsolete();
        }

        @Override
        public String toString() {
            return task.toString();
        }
    }


    @Nonnull
    private ProgressIndicatorBase indicator(@Nonnull AsyncPromise<?> promise) {
        ProgressIndicatorBase indicator = indicators.get(promise);
        if (indicator == null) {
            indicator = new ProgressIndicatorBase(true, false);
            ProgressIndicatorBase old = indicators.put(promise, indicator);
            if (old != null) {
                LOG.error("the same task is running in parallel");
            }
            promise.onProcessed(done -> indicators.remove(promise).cancel());
        }
        return indicator;
    }

    /**
     * This class is the {@code Invoker} in the Event Dispatch Thread,
     * which is the only one valid thread for this invoker.
     */
    public static final class UIAccessInvoker extends InvokerImpl {
        @Nonnull
        private final UIAccess myUiAccess;

        /**
         * Creates the invoker of user tasks on the event dispatch thread.
         *
         * @param parent a disposable parent object
         */
        public UIAccessInvoker(@Nonnull UIAccess uiAccess, @Nonnull Disposable parent) {
            super("UI", parent);
            myUiAccess = uiAccess;
        }

        @Override
        public boolean isValidThread() {
            return UIAccess.isUIThread();
        }

        @Override
        void offer(@Nonnull Runnable runnable, int delay) {
            if (delay > 0) {
                myUiAccess.getScheduler().schedule(runnable, delay, MILLISECONDS);
            }
            else {
                myUiAccess.execute(runnable);
            }
        }

        @Override
        public String toString() {
            return "UIAccessInvoker";
        }
    }

    public static final class Background extends InvokerImpl {
        private final Set<Thread> threads = Sets.newConcurrentHashSet();
        private final ScheduledExecutorService myExecutor;

        public Background(@Nonnull Disposable parent) {
            super("Background", parent);
            myExecutor = AppExecutorUtil.createBoundedScheduledExecutorService(toString(), 1);
        }

        @Override
        public void dispose() {
            super.dispose();
            myExecutor.shutdown();
        }

        @Override
        public boolean isValidThread() {
            return threads.contains(Thread.currentThread());
        }

        @Override
        void offer(@Nonnull Runnable runnable, int delay) {
            schedule(myExecutor, () -> {
                Thread thread = Thread.currentThread();
                if (!threads.add(thread)) {
                    LOG.error("current thread is already used");
                }
                else {
                    try {
                        runnable.run(); // may throw an assertion error
                    }
                    finally {
                        if (!threads.remove(thread)) {
                            LOG.error("current thread is already removed");
                        }
                    }
                }
            }, delay);
        }
    }

    public static final class BackgroundRead extends InvokerImpl {
        private final Set<Thread> threads = Sets.newConcurrentHashSet();
        private final ScheduledExecutorService myExecutor;
        @Nonnull
        private final Application myApplication;

        public BackgroundRead(@Nonnull Application application, @Nonnull Disposable parent) {
            super("Read", parent);
            myApplication = application;
            myExecutor = AppExecutorUtil.createBoundedScheduledExecutorService(toString(), 1);
        }

        @Override
        public void dispose() {
            super.dispose();
            myExecutor.shutdown();
        }

        @Override
        public boolean isValidThread() {
            return threads.contains(Thread.currentThread()) && myApplication.isReadAccessAllowed();
        }

        @Override
        void offer(@Nonnull Runnable runnable, int delay) {
            schedule(myExecutor, () -> {
                Thread thread = Thread.currentThread();
                if (!threads.add(thread)) {
                    LOG.error("current thread is already used");
                }
                else {
                    try {
                        myApplication.runReadAction(runnable);
                    }
                    finally {
                        if (!threads.remove(thread)) {
                            LOG.error("current thread is already removed");
                        }
                    }
                }
            }, delay);
        }
    }

    private static void schedule(ScheduledExecutorService executor, Runnable runnable, int delay) {
        if (delay > 0) {
            executor.schedule(runnable, delay, MILLISECONDS);
        }
        else {
            executor.execute(runnable);
        }
    }

    @Nonnull
    public static InvokerImpl forEventDispatchThread(@Nonnull UIAccess uiAccess, @Nonnull Disposable parent) {
        return new UIAccessInvoker(uiAccess, parent);
    }

    @Nonnull
    public static InvokerImpl forBackgroundThreadWithReadAction(@Nonnull Disposable parent) {
        return new BackgroundRead(Application.get(), parent);
    }

    @Nonnull
    public static InvokerImpl forBackgroundThreadWithoutReadAction(@Nonnull Disposable parent) {
        return new Background(parent);
    }
}
