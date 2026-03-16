// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.concurent;

import consulo.util.collection.ContainerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SchedulingWrapper implements ScheduledExecutorService {
  private static final Logger LOG = LoggerFactory.getLogger(SchedulingWrapper.class);
  private final AtomicBoolean shutdown = new AtomicBoolean();
  
  protected final ExecutorService backendExecutorService;
  protected final AppDelayQueue delayQueue;

  public SchedulingWrapper(ExecutorService backendExecutorService, AppDelayQueue delayQueue) {
    this.delayQueue = delayQueue;
    if (backendExecutorService instanceof ScheduledExecutorService) {
      throw new IllegalArgumentException("backendExecutorService: " + backendExecutorService + " is already ScheduledExecutorService");
    }
    this.backendExecutorService = backendExecutorService;
  }

  
  @Override
  public List<Runnable> shutdownNow() {
    return doShutdownNow();
  }

  @Override
  public void shutdown() {
    doShutdown();
  }

  protected void doShutdown() {
    if (!shutdown.compareAndSet(false, true)) {
      throw new IllegalStateException("Already shutdown");
    }
  }

  
  protected List<Runnable> doShutdownNow() {
    doShutdown(); // shutdown me first to avoid further delayQueue offers
    return cancelAndRemoveTasksFromQueue();
  }

  
  protected List<Runnable> cancelAndRemoveTasksFromQueue() {
    List<MyScheduledFutureTask> result = ContainerUtil.filter(delayQueue, task -> {
      if (task.getBackendExecutorService() == backendExecutorService) {
        task.cancel(false);
        return true;
      }
      return false;
    });
    delayQueue.removeAll(new HashSet<>(result));
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
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
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

  public class MyScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {
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
    public MyScheduledFutureTask(Runnable r, V result, long ns) {
      super(r, result);
      time = ns;
      period = 0;
      sequenceNumber = sequencer.getAndIncrement();
    }

    /**
     * Creates a periodic action with given nano time and period.
     */
    private MyScheduledFutureTask(Runnable r, V result, long ns, long period) {
      super(r, result);
      time = ns;
      this.period = period;
      sequenceNumber = sequencer.getAndIncrement();
    }

    /**
     * Creates a one-shot action with given nanoTime-based trigger time.
     */
    private MyScheduledFutureTask(Callable<V> callable, long ns) {
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
    public long getDelay(TimeUnit unit) {
      return unit.convert(time - now(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
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
        LOG.trace("Executing " + AppDelayQueue.info(this));
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
      Object info = AppDelayQueue.info(this);
      return "Delay: " + getDelay(TimeUnit.MILLISECONDS) + "ms; " + (info == this ? super.toString() : info) + " backendExecutorService: " + backendExecutorService;
    }

    
    private ExecutorService getBackendExecutorService() {
      return backendExecutorService;
    }

    public void executeMeInBackendExecutor() {
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
  public static long triggerTime(AppDelayQueue queue, long delay, TimeUnit unit) {
    return triggerTime(queue, unit.toNanos(delay < 0 ? 0 : delay));
  }

  private static long now() {
    return System.nanoTime();
  }

  /**
   * Returns the trigger time of a delayed action.
   */
  private static long triggerTime(AppDelayQueue queue, long delay) {
    return now() + (delay < Long.MAX_VALUE >> 1 ? delay : overflowFree(queue, delay));
  }

  /**
   * Constrains the values of all delays in the queue to be within
   * Long.MAX_VALUE of each other, to avoid overflow in compareTo.
   * This may occur if a task is eligible to be dequeued, but has
   * not yet been, while some other task is added with a delay of
   * Long.MAX_VALUE.
   */
  private static long overflowFree(AppDelayQueue queue, long delay) {
    Delayed head = queue.peek();
    if (head != null) {
      long headDelay = head.getDelay(TimeUnit.NANOSECONDS);
      if (headDelay < 0 && delay - headDelay < 0) {
        delay = Long.MAX_VALUE + headDelay;
      }
    }
    return delay;
  }

  
  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    MyScheduledFutureTask<?> t = new MyScheduledFutureTask<Void>(command, null, triggerTime(delayQueue, delay, unit));
    return delayedExecute(t);
  }

  
  public <T> MyScheduledFutureTask<T> delayedExecute(MyScheduledFutureTask<T> t) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Submit at delay " + t.getDelay(TimeUnit.MILLISECONDS) + "ms " + AppDelayQueue.info(t));
    }
    if (isShutdown()) {
      throw new RejectedExecutionException("Already shutdown");
    }
    delayQueue.add(t);
    if (t.getDelay(TimeUnit.DAYS) > 31 && !t.isPeriodic()) {
      // guard against inadvertent queue overflow
      throw new IllegalArgumentException("Unsupported crazy delay " + t.getDelay(TimeUnit.DAYS) + " days: " + AppDelayQueue.info(t));
    }
    return t;
  }

  public AppDelayQueue getDelayQueue() {
    return delayQueue;
  }

  
  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    MyScheduledFutureTask<V> t = new MyScheduledFutureTask<V>(callable, triggerTime(delayQueue, delay, unit));
    return delayedExecute(t);
  }

  
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    throw new UnsupportedOperationException("Not supported because it's bad for hibernation; use scheduleWithFixedDelay() with the same parameters instead.");
  }

  
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    if (delay <= 0) {
      throw new IllegalArgumentException("delay must be positive but got: " + delay);
    }
    MyScheduledFutureTask<Void> sft = new MyScheduledFutureTask<Void>(command, null, triggerTime(delayQueue, initialDelay, unit), unit.toNanos(-delay));
    return delayedExecute(sft);
  }

  /////////////////////// delegates for ExecutorService ///////////////////////////

  
  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return backendExecutorService.submit(task);
  }

  
  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return backendExecutorService.submit(task, result);
  }

  
  @Override
  public Future<?> submit(Runnable task) {
    return backendExecutorService.submit(task);
  }

  
  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return backendExecutorService.invokeAll(tasks);
  }

  
  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
    return backendExecutorService.invokeAll(tasks, timeout, unit);
  }

  
  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return backendExecutorService.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return backendExecutorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    backendExecutorService.execute(command);
  }

  
  public ExecutorService getBackendExecutorService() {
    return backendExecutorService;
  }
}
