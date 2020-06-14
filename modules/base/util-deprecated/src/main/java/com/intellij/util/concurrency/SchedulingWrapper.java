// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.concurrency;

import consulo.logging.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Makes a {@link ScheduledExecutorService} from the supplied plain, non-scheduling {@link ExecutorService} by awaiting scheduled tasks in a separate thread
 * and then passing them for execution to the {@code backendExecutorService}.
 * Unlike the existing {@link ScheduledThreadPoolExecutor}, this pool can be unbounded if the {@code backendExecutorService} is.
 */
class SchedulingWrapper implements ScheduledExecutorService {
  private static final Logger LOG = Logger.getInstance(SchedulingWrapper.class);
  private final AtomicBoolean shutdown = new AtomicBoolean();
  @Nonnull
  final ExecutorService backendExecutorService;
  final AppDelayQueue delayQueue;

  SchedulingWrapper(@Nonnull final ExecutorService backendExecutorService, @Nonnull AppDelayQueue delayQueue) {
    this.delayQueue = delayQueue;
    if (backendExecutorService instanceof ScheduledExecutorService) {
      throw new IllegalArgumentException("backendExecutorService: " + backendExecutorService + " is already ScheduledExecutorService");
    }
    this.backendExecutorService = backendExecutorService;
  }

  @Nonnull
  @Override
  public List<Runnable> shutdownNow() {
    return doShutdownNow();
  }

  @Override
  public void shutdown() {
    doShutdown();
  }

  void doShutdown() {
    if (!shutdown.compareAndSet(false, true)) {
      throw new IllegalStateException("Already shutdown");
    }
  }

  @Nonnull
  List<Runnable> doShutdownNow() {
    doShutdown(); // shutdown me first to avoid further delayQueue offers
    return cancelAndRemoveTasksFromQueue();
  }

  @Nonnull
  List<Runnable> cancelAndRemoveTasksFromQueue() {
    List<MyScheduledFutureTask> result = ContainerUtil.filter(delayQueue, new Condition<MyScheduledFutureTask>() {
      @Override
      public boolean value(MyScheduledFutureTask task) {
        if (task.getBackendExecutorService() == backendExecutorService) {
          task.cancel(false);
          return true;
        }
        return false;
      }
    });
    delayQueue.removeAll(new HashSet<MyScheduledFutureTask>(result));
    if (LOG.isTraceEnabled()) {
      LOG.trace("Shutdown. Drained tasks: " + result);
    }
    //noinspection unchecked
    return (List)result;
  }

  @Override
  public boolean isShutdown() {
    return shutdown.get();
  }

  @Override
  public boolean isTerminated() {
    return isShutdown();
  }

  @Override
  public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
    if (!isShutdown()) throw new IllegalStateException("must await termination after shutdown() or shutdownNow() only");
    List<MyScheduledFutureTask> tasks = new ArrayList<MyScheduledFutureTask>(delayQueue);
    for (MyScheduledFutureTask task : tasks) {
      if (task.getBackendExecutorService() != backendExecutorService) {
        continue;
      }
      try {
        task.get(timeout, unit);
      }
      catch (ExecutionException ignored) {

      }
      catch (TimeoutException e) {
        return false;
      }
    }
    return backendExecutorService.awaitTermination(timeout, unit);
  }

  class MyScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {
    /**
     * Sequence number to break ties FIFO
     */
    private final long sequenceNumber;

    /**
     * The time the task is enabled to execute in nanoTime units
     */
    private long time;

    /**
     * Period in nanoseconds for repeating tasks.  A positive
     * value indicates fixed-rate execution.  A negative value
     * indicates fixed-delay execution.  A value of 0 indicates a
     * non-repeating task.
     */
    private final long period;

    /**
     * Creates a one-shot action with given nanoTime-based trigger time.
     */
    MyScheduledFutureTask(@Nonnull Runnable r, V result, long ns) {
      super(r, result);
      time = ns;
      period = 0;
      sequenceNumber = sequencer.getAndIncrement();
    }

    /**
     * Creates a periodic action with given nano time and period.
     */
    private MyScheduledFutureTask(@Nonnull Runnable r, V result, long ns, long period) {
      super(r, result);
      time = ns;
      this.period = period;
      sequenceNumber = sequencer.getAndIncrement();
    }

    /**
     * Creates a one-shot action with given nanoTime-based trigger time.
     */
    private MyScheduledFutureTask(@Nonnull Callable<V> callable, long ns) {
      super(callable);
      time = ns;
      period = 0;
      sequenceNumber = sequencer.getAndIncrement();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      boolean canceled = super.cancel(mayInterruptIfRunning);
      delayQueue.remove(this);
      return canceled;
    }

    @Override
    public long getDelay(@Nonnull TimeUnit unit) {
      return unit.convert(time - now(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(@Nonnull Delayed other) {
      if (other == this) {
        return 0;
      }
      if (other instanceof MyScheduledFutureTask) {
        MyScheduledFutureTask<?> x = (MyScheduledFutureTask<?>)other;
        long diff = time - x.time;
        if (diff < 0) {
          return -1;
        }
        if (diff > 0) {
          return 1;
        }
        if (sequenceNumber < x.sequenceNumber) {
          return -1;
        }
        return 1;
      }
      long diff = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
      return diff < 0 ? -1 : diff > 0 ? 1 : 0;
    }

    /**
     * Returns {@code true} if this is a periodic (not a one-shot) action.
     *
     * @return {@code true} if periodic
     */
    @Override
    public boolean isPeriodic() {
      return period != 0;
    }

    /**
     * Sets the next time to run for a periodic task.
     */
    private void setNextRunTime() {
      long p = period;
      if (p > 0) {
        time += p;
      }
      else {
        time = triggerTime(delayQueue, -p);
      }
    }

    /**
     * Overrides FutureTask version so as to reset/requeue if periodic.
     */
    @Override
    public void run() {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Executing " + BoundedTaskExecutor.info(this));
      }
      boolean periodic = isPeriodic();
      if (!periodic) {
        super.run();
      }
      else if (runAndReset()) {
        setNextRunTime();
        delayQueue.offer(this);
      }
    }

    @Override
    public String toString() {
      Object info = BoundedTaskExecutor.info(this);
      return "Delay: " + getDelay(TimeUnit.MILLISECONDS) + "ms; " + (info == this ? super.toString() : info) + " backendExecutorService: " + backendExecutorService;
    }

    @Nonnull
    private ExecutorService getBackendExecutorService() {
      return backendExecutorService;
    }

    void executeMeInBackendExecutor() {
      backendExecutorService.execute(this);
    }
  }

  /**
   * Sequence number to break scheduling ties, and in turn to
   * guarantee FIFO order among tied entries.
   */
  private static final AtomicLong sequencer = new AtomicLong();

  /**
   * Returns the trigger time of a delayed action.
   */
  static long triggerTime(@Nonnull AppDelayQueue queue, long delay, TimeUnit unit) {
    return triggerTime(queue, unit.toNanos(delay < 0 ? 0 : delay));
  }

  private static long now() {
    return System.nanoTime();
  }

  /**
   * Returns the trigger time of a delayed action.
   */
  private static long triggerTime(@Nonnull AppDelayQueue queue, long delay) {
    return now() + (delay < Long.MAX_VALUE >> 1 ? delay : overflowFree(queue, delay));
  }

  /**
   * Constrains the values of all delays in the queue to be within
   * Long.MAX_VALUE of each other, to avoid overflow in compareTo.
   * This may occur if a task is eligible to be dequeued, but has
   * not yet been, while some other task is added with a delay of
   * Long.MAX_VALUE.
   */
  private static long overflowFree(@Nonnull AppDelayQueue queue, long delay) {
    Delayed head = queue.peek();
    if (head != null) {
      long headDelay = head.getDelay(TimeUnit.NANOSECONDS);
      if (headDelay < 0 && delay - headDelay < 0) {
        delay = Long.MAX_VALUE + headDelay;
      }
    }
    return delay;
  }

  @Nonnull
  @Override
  public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
    MyScheduledFutureTask<?> t = new MyScheduledFutureTask<Void>(command, null, triggerTime(delayQueue, delay, unit));
    return delayedExecute(t);
  }

  @Nonnull
  <T> MyScheduledFutureTask<T> delayedExecute(@Nonnull MyScheduledFutureTask<T> t) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Submit at delay " + t.getDelay(TimeUnit.MILLISECONDS) + "ms " + BoundedTaskExecutor.info(t));
    }
    if (isShutdown()) {
      throw new RejectedExecutionException("Already shutdown");
    }
    delayQueue.add(t);
    if (t.getDelay(TimeUnit.DAYS) > 31 && !t.isPeriodic()) {
      // guard against inadvertent queue overflow
      throw new IllegalArgumentException("Unsupported crazy delay " + t.getDelay(TimeUnit.DAYS) + " days: " + BoundedTaskExecutor.info(t));
    }
    return t;
  }

  @Nonnull
  @Override
  public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
    MyScheduledFutureTask<V> t = new MyScheduledFutureTask<V>(callable, triggerTime(delayQueue, delay, unit));
    return delayedExecute(t);
  }

  @Nonnull
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
    throw new IncorrectOperationException("Not supported because it's bad for hibernation; use scheduleWithFixedDelay() with the same parameters instead.");
  }

  @Nonnull
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
    if (delay <= 0) {
      throw new IllegalArgumentException("delay must be positive but got: " + delay);
    }
    MyScheduledFutureTask<Void> sft = new MyScheduledFutureTask<Void>(command, null, triggerTime(delayQueue, initialDelay, unit), unit.toNanos(-delay));
    return delayedExecute(sft);
  }

  /////////////////////// delegates for ExecutorService ///////////////////////////

  @Nonnull
  @Override
  public <T> Future<T> submit(@Nonnull Callable<T> task) {
    return backendExecutorService.submit(task);
  }

  @Nonnull
  @Override
  public <T> Future<T> submit(@Nonnull Runnable task, T result) {
    return backendExecutorService.submit(task, result);
  }

  @Nonnull
  @Override
  public Future<?> submit(@Nonnull Runnable task) {
    return backendExecutorService.submit(task);
  }

  @Nonnull
  @Override
  public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return backendExecutorService.invokeAll(tasks);
  }

  @Nonnull
  @Override
  public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
    return backendExecutorService.invokeAll(tasks, timeout, unit);
  }

  @Nonnull
  @Override
  public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return backendExecutorService.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return backendExecutorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(@Nonnull Runnable command) {
    backendExecutorService.execute(command);
  }
}
