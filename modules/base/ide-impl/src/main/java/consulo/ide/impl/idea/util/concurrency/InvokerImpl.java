// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.concurrency;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.application.Application;
import consulo.logging.Logger;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressManager;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.dumb.IndexNotReadyException;
import consulo.ui.UIAccess;
import consulo.util.lang.ThreeState;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import consulo.ui.ex.util.Invoker;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.concurrent.Obsolescent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static consulo.application.ApplicationManager.getApplication;
import static consulo.application.internal.ProgressIndicatorUtils.runInReadActionWithWriteActionPriority;
import static consulo.disposer.Disposer.register;
import static java.awt.EventQueue.isDispatchThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class InvokerImpl implements Disposable, Invoker {
  private static final int THRESHOLD = Integer.MAX_VALUE;
  private static final Logger LOG = Logger.getInstance(InvokerImpl.class);
  private static final AtomicInteger UID = new AtomicInteger();
  private final Map<AsyncPromise<?>, ProgressIndicatorBase> indicators = new ConcurrentHashMap<>();
  private final AtomicInteger count = new AtomicInteger();
  private final ThreeState useReadAction;
  private final String description;
  private volatile boolean disposed;

  private InvokerImpl(@Nonnull String prefix, @Nonnull Disposable parent, @Nonnull ThreeState useReadAction) {
    description = "Invoker." + UID.getAndIncrement() + "." + prefix + (useReadAction != ThreeState.UNSURE ? ".ReadAction=" + useReadAction : "") + ": " + parent;
    this.useReadAction = useReadAction;
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
   * Returns {@code true} if the current thread allows to process a task.
   *
   * @return {@code true} if the current thread is valid, or {@code false} otherwise
   */
  public boolean isValidThread() {
    if (useReadAction != ThreeState.NO) return true;
    Application application = getApplication();
    return application == null || !application.isReadAccessAllowed();
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
  @Nonnull
  @Deprecated
  public final CancellablePromise<?> runOrInvokeLater(@Nonnull Runnable task) {
    return invoke(task);
  }

  /**
   * Returns a workload of the task queue.
   *
   * @return amount of tasks, which are executing or waiting for execution
   */
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
      if (LOG.isTraceEnabled()) LOG.debug("Executor is shutdown");
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
        if (getApplication() == null) {
          task.run(); // is not interruptible in tests without application
        }
        else if (useReadAction != ThreeState.YES || isDispatchThread()) {
          ProgressManager.getInstance().runProcess(task, indicator(task.promise));
        }
        else if (!runInReadActionWithWriteActionPriority(task, indicator(task.promise))) {
          offerRestart(task, attempt);
          return;
        }
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
      if (LOG.isTraceEnabled()) LOG.debug("Task is restarted");
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
    if (!isValidThread()) return promise(task, 0);
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
    if (delay < 0) throw new IllegalArgumentException("delay must be non-negative: " + delay);
    if (task.canInvoke(disposed)) offerSafely(task, 0, delay);
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
      if (LOG.isTraceEnabled()) LOG.debug("Task is canceled");
      if (attempt < THRESHOLD) return canInvoke(disposed);
      LOG.warn("Task is always canceled: " + supplier);
      promise.setError("timeout");
      return false; // too many attempts to run the task
    }

    boolean canInvoke(boolean disposed) {
      if (promise.isDone()) {
        if (LOG.isTraceEnabled()) LOG.debug("Promise is cancelled: ", promise.isCancelled());
        return false; // the given promise is already done or cancelled
      }
      if (disposed) {
        if (LOG.isTraceEnabled()) LOG.debug("Invoker is disposed");
        promise.setError("disposed");
        return false; // the current invoker is disposed
      }
      if (supplier instanceof Obsolescent) {
        Obsolescent obsolescent = (Obsolescent)supplier;
        if (obsolescent.isObsolete()) {
          if (LOG.isTraceEnabled()) LOG.debug("Task is obsolete");
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
      return task instanceof Obsolescent && ((Obsolescent)task).isObsolete();
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
      if (old != null) LOG.error("the same task is running in parallel");
      promise.onProcessed(done -> indicators.remove(promise).cancel());
    }
    return indicator;
  }

  /**
   * This class is the {@code Invoker} in the Event Dispatch Thread,
   * which is the only one valid thread for this invoker.
   */
  public static final class EDT extends InvokerImpl {
    @Nonnull
    private final UIAccess myUiAccess;

    /**
     * Creates the invoker of user tasks on the event dispatch thread.
     *
     * @param parent a disposable parent object
     */
    public EDT(@Nonnull UIAccess uiAccess, @Nonnull Disposable parent) {
      super("UI", parent, ThreeState.UNSURE);
      myUiAccess = uiAccess;
    }

    @Override
    public boolean isValidThread() {
      return isDispatchThread();
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
  }

  /**
   * This class is the {@code Invoker} in a single background thread.
   * This invoker does not need additional synchronization.
   *
   * @deprecated use {@link Background#Background(Disposable)} instead
   */
  @Deprecated
  public static final class BackgroundThread extends InvokerImpl {
    private final ScheduledExecutorService executor;
    private volatile Thread thread;

    public BackgroundThread(@Nonnull Disposable parent) {
      super("Background.Thread", parent, ThreeState.YES);
      executor = AppExecutorUtil.createBoundedScheduledExecutorService(toString(), 1);
    }

    @Override
    public void dispose() {
      super.dispose();
      executor.shutdown();
    }

    @Override
    public boolean isValidThread() {
      return thread == Thread.currentThread();
    }

    @Override
    void offer(@Nonnull Runnable runnable, int delay) {
      schedule(executor, () -> {
        if (thread != null) LOG.error("unexpected thread: " + thread);
        try {
          thread = Thread.currentThread();
          runnable.run(); // may throw an assertion error
        }
        finally {
          thread = null;
        }
      }, delay);
    }
  }

  public static final class Background extends InvokerImpl {
    private final Set<Thread> threads = ContainerUtil.newConcurrentSet();
    private final ScheduledExecutorService executor;

    /**
     * Creates the invoker of user read actions on a background thread.
     *
     * @param parent a disposable parent object
     * @deprecated use {@link #forBackgroundThreadWithReadAction} instead
     */
    @Deprecated
    public Background(@Nonnull Disposable parent) {
      this(parent, true);
    }

    /**
     * Creates the invoker of user read actions on background threads.
     *
     * @param parent     a disposable parent object
     * @param maxThreads the number of threads used for parallel calculation,
     *                   where 1 guarantees sequential calculation,
     *                   which allows not to use additional synchronization
     * @deprecated use {@link #forBackgroundPoolWithReadAction} instead
     */
    @Deprecated
    public Background(@Nonnull Disposable parent, int maxThreads) {
      this(parent, ThreeState.YES, maxThreads);
    }

    /**
     * Creates the invoker of user tasks on a background thread.
     *
     * @param parent        a disposable parent object
     * @param useReadAction {@code true} to run user tasks as read actions with write action priority,
     *                      {@code false} to run user tasks without read locks
     * @deprecated use {@link #forBackgroundThreadWithReadAction} or {@link #forBackgroundThreadWithoutReadAction} instead
     */
    @Deprecated
    public Background(@Nonnull Disposable parent, boolean useReadAction) {
      this(parent, ThreeState.fromBoolean(useReadAction));
    }

    /**
     * Creates the invoker of user tasks on a background thread.
     *
     * @param parent        a disposable parent object
     * @param useReadAction {@code YES} to run user tasks as read actions with write action priority,
     *                      {@code NO} to run user tasks without read locks,
     *                      {@code UNSURE} does not guarantee that read action is allowed
     * @deprecated use {@link #forBackgroundThreadWithReadAction} or {@link #forBackgroundThreadWithoutReadAction} instead
     */
    @Deprecated
    public Background(@Nonnull Disposable parent, @Nonnull ThreeState useReadAction) {
      this(parent, useReadAction, 1);
    }

    /**
     * Creates the invoker of user tasks on background threads.
     *
     * @param parent        a disposable parent object
     * @param useReadAction {@code YES} to run user tasks as read actions with write action priority,
     *                      {@code NO} to run user tasks without read locks,
     *                      {@code UNSURE} does not guarantee that read action is allowed
     * @param maxThreads    the number of threads used for parallel calculation,
     *                      where 1 guarantees sequential calculation,
     *                      which allows not to use additional synchronization
     * @deprecated use {@link #forBackgroundThreadWithReadAction} or {@link #forBackgroundThreadWithoutReadAction} instead
     */
    @Deprecated
    public Background(@Nonnull Disposable parent, @Nonnull ThreeState useReadAction, int maxThreads) {
      super(maxThreads != 1 ? "Pool(" + maxThreads + ")" : "Thread", parent, useReadAction);
      executor = AppExecutorUtil.createBoundedScheduledExecutorService(toString(), maxThreads);
    }

    @Override
    public void dispose() {
      super.dispose();
      executor.shutdown();
    }

    @Override
    public boolean isValidThread() {
      return threads.contains(Thread.currentThread()) && super.isValidThread();
    }

    @Override
    void offer(@Nonnull Runnable runnable, int delay) {
      schedule(executor, () -> {
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
    return new EDT(uiAccess, parent);
  }

  @Nonnull
  public static InvokerImpl forBackgroundPoolWithReadAction(@Nonnull Disposable parent) {
    return new Background(parent, ThreeState.YES, 8);
  }

  @Nonnull
  public static InvokerImpl forBackgroundThreadWithReadAction(@Nonnull Disposable parent) {
    return new Background(parent, ThreeState.YES, 1);
  }

  @Nonnull
  public static InvokerImpl forBackgroundThreadWithoutReadAction(@Nonnull Disposable parent) {
    return new Background(parent, ThreeState.NO, 1);
  }
}
