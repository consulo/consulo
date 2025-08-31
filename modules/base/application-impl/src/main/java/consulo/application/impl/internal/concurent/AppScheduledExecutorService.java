// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.concurent;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.util.LowMemoryWatcherManager;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.CountingThreadFactory;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A ThreadPoolExecutor which also implements {@link ScheduledExecutorService} by awaiting scheduled tasks in a separate thread
 * and then executing them in the owned ThreadPoolExecutor.
 * Unlike the existing {@link ScheduledThreadPoolExecutor}, this pool is unbounded.
 */
public final class AppScheduledExecutorService extends SchedulingWrapper {
  static final String POOLED_THREAD_PREFIX = "ApplicationImpl pooled thread ";
  @Nonnull
  private final String myName;
  private final LowMemoryWatcherManager myLowMemoryWatcherManager;
  private final MyThreadFactory myCountingThreadFactory;

  private static class MyThreadFactory extends CountingThreadFactory {
    private Consumer<? super Thread> newThreadListener;

    @Nonnull
    @Override
    public Thread newThread(@Nonnull Runnable r) {
      Thread thread = new Thread(r, POOLED_THREAD_PREFIX + counter.incrementAndGet());

      thread.setPriority(Thread.NORM_PRIORITY - 1);

      Consumer<? super Thread> listener = newThreadListener;
      if (listener != null) {
        listener.accept(thread);
      }
      return thread;
    }

    void setNewThreadListener(@Nonnull Consumer<? super Thread> threadListener) {
      if (newThreadListener != null) throw new IllegalStateException("Listener was already set: " + newThreadListener);
      newThreadListener = threadListener;
    }
  }

  AppScheduledExecutorService(@Nonnull String name, ApplicationConcurrency applicationConcurrency) {
    super(new BackendThreadPoolExecutor(new MyThreadFactory()), new AppDelayQueue());
    myName = name;
    myCountingThreadFactory = (MyThreadFactory)((BackendThreadPoolExecutor)backendExecutorService).getThreadFactory();
    myLowMemoryWatcherManager = new LowMemoryWatcherManager(this, applicationConcurrency);
  }

  public void setNewThreadListener(@Nonnull Consumer<? super Thread> threadListener) {
    myCountingThreadFactory.setNewThreadListener(threadListener);
  }

  @Nonnull
  @Override
  public List<Runnable> shutdownNow() {
    return error();
  }

  @Override
  public void shutdown() {
    error();
  }

  public static List<Runnable> error() {
    throw new UnsupportedOperationException("You must not call this method on the global app pool");
  }

  @Override
  protected void doShutdown() {
    super.doShutdown();
    ((BackendThreadPoolExecutor)backendExecutorService).superShutdown();
  }

  @Nonnull
  @Override
  protected List<Runnable> doShutdownNow() {
    return ContainerUtil.concat(super.doShutdownNow(), ((BackendThreadPoolExecutor)backendExecutorService).superShutdownNow());
  }

  public void shutdownAppScheduledExecutorService() {
    // LowMemoryWatcher starts background threads so stop it now to avoid RejectedExecutionException
    Disposer.dispose(myLowMemoryWatcherManager);
    delayQueue.shutdown(); // shutdown delay queue first to avoid rejected execution exceptions in Alarm
    doShutdown();
  }

  @Nonnull
  @TestOnly
  public String statistics() {
    return myName + " threads created counter = " + myCountingThreadFactory.getCount();
  }

  @TestOnly
  public String dumpQueue() {
    return delayQueue.toString();
  }

  public int getBackendPoolExecutorSize() {
    return ((BackendThreadPoolExecutor)backendExecutorService).getPoolSize();
  }

  @TestOnly
  void setBackendPoolCorePoolSize(int size) {
    ((BackendThreadPoolExecutor)backendExecutorService).superSetCorePoolSize(size);
  }

  static class BackendThreadPoolExecutor extends ThreadPoolExecutor {
    BackendThreadPoolExecutor(@Nonnull ThreadFactory factory) {
      super(1, Integer.MAX_VALUE, 1, TimeUnit.MINUTES, new SynchronousQueue<>(), factory);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      if (t != null) {
        Logger.getInstance(AppScheduledExecutorService.class).error("Worker exited due to exception", t);
      }
    }

    private void superShutdown() {
      super.shutdown();
    }

    @Nonnull
    private List<Runnable> superShutdownNow() {
      return super.shutdownNow();
    }

    // stub out sensitive methods
    @Override
    public void shutdown() {
      error();
    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
      return error();
    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
      error();
    }

    private void superSetCorePoolSize(int corePoolSize) {
      super.setCorePoolSize(corePoolSize);
    }

    @Override
    public void allowCoreThreadTimeOut(boolean value) {
      error();
    }

    @Override
    public void setMaximumPoolSize(int maximumPoolSize) {
      error();
    }

    @Override
    public void setKeepAliveTime(long time, TimeUnit unit) {
      error();
    }

    void superSetKeepAliveTime(long time, TimeUnit unit) {
      super.setKeepAliveTime(time, unit);
    }

    @Override
    public void setThreadFactory(ThreadFactory threadFactory) {
      error();
    }
  }

  @Nonnull
  public Thread getPeriodicTasksThread() {
    return delayQueue.getThread();
  }

  @TestOnly
  void awaitQuiescence(long timeout, @Nonnull TimeUnit unit) {
    BackendThreadPoolExecutor executor = (BackendThreadPoolExecutor)backendExecutorService;
    executor.getKeepAliveTime(TimeUnit.NANOSECONDS);
    executor.superSetKeepAliveTime(1, TimeUnit.NANOSECONDS); // no need for zombies in tests
    executor.superSetCorePoolSize(0); // interrupt idle workers
    ReentrantLock mainLock = ReflectionUtil.getField(executor.getClass(), executor, ReentrantLock.class, "mainLock");
    mainLock.lock();
    Set workers;
    try {
      HashSet workersField = ReflectionUtil.getField(executor.getClass(), executor, HashSet.class, "workers");
      workers = new HashSet(workersField); // to be able to iterate thread-safely outside the lock
    }
    finally {
      mainLock.unlock();
    }
    executor.superSetKeepAliveTime(1, TimeUnit.SECONDS);

    for (Object worker : workers) {
      Thread thread = ReflectionUtil.getField(worker.getClass(), worker, Thread.class, "thread");
      try {
        thread.join(unit.toMillis(timeout));
      }
      catch (InterruptedException e) {
        String trace = "Thread leaked: " + thread + "; " + thread.getState() + " (" + thread.isAlive() + ")\n--- its stacktrace:\n";
        for (StackTraceElement stackTraceElement : thread.getStackTrace()) {
          trace += " at " + stackTraceElement + "\n";
        }
        trace += "---\n";
        System.err.println("Executor " + executor + " is still active after " + unit.toSeconds(timeout) + " seconds://///\n" +
                           "Thread " + thread + " dump:\n" + trace +
                           "all thread dump:\n" + ThreadDumper.dumpThreadsToString() + "\n/////");
        break;
      }
    }
  }
}
