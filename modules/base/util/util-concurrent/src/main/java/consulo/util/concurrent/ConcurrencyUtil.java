/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.util.concurrent;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author cdr
 */
public class ConcurrencyUtil {
  /**
   * Invokes and waits all tasks using threadPool, avoiding thread starvation on the way
   * (see <a href="http://gafter.blogspot.com/2006/11/thread-pool-puzzler.html">"A Thread Pool Puzzler"</a>).
   */
  public static <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, ExecutorService executorService) throws Throwable {
    if (executorService == null) {
      for (Callable<T> task : tasks) {
        task.call();
      }
      return null;
    }

    List<Future<T>> futures = new ArrayList<>(tasks.size());
    boolean done = false;
    try {
      for (Callable<T> t : tasks) {
        Future<T> future = executorService.submit(t);
        futures.add(future);
      }
      // force not started futures to execute using the current thread
      for (Future f : futures) {
        ((Runnable)f).run();
      }
      for (Future f : futures) {
        try {
          f.get();
        }
        catch (CancellationException ignore) {
        }
        catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause != null) {
            throw cause;
          }
        }
      }
      done = true;
    }
    finally {
      if (!done) {
        for (Future f : futures) {
          f.cancel(false);
        }
      }
    }
    return futures;
  }

  /**
   * @return defaultValue if the reference contains null (in that case defaultValue is placed there), or reference value otherwise.
   */
  @Nonnull
  public static <T> T cacheOrGet(@Nonnull AtomicReference<T> ref, @Nonnull T defaultValue) {
    return ref.updateAndGet(prev -> prev == null ? defaultValue : prev);
  }

  @Nonnull
  public static ThreadPoolExecutor newSingleThreadExecutor(@Nonnull String name) {
    return newSingleThreadExecutor(name, Thread.NORM_PRIORITY);
  }

  @Nonnull
  public static ThreadPoolExecutor newSingleThreadExecutor(@Nonnull String name, int priority) {
    return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), newNamedThreadFactory(name, true, priority));
  }

  @Nonnull
  public static ScheduledThreadPoolExecutor newSingleScheduledThreadExecutor(@Nonnull String name) {
    return newSingleScheduledThreadExecutor(name, Thread.NORM_PRIORITY);
  }

  @Nonnull
  public static ScheduledThreadPoolExecutor newSingleScheduledThreadExecutor(@Nonnull String name, int priority) {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, newNamedThreadFactory(name, true, priority));
    executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    return executor;
  }

  /**
   * Service which executes tasks synchronously immediately after they submitted
   */
  @Nonnull
  public static ExecutorService newSameThreadExecutorService() {
    return new SameThreadExecutorService();
  }

  @Nonnull
  public static ThreadFactory newNamedThreadFactory(@Nonnull final String name, final boolean isDaemon, final int priority) {
    return r -> {
      Thread thread = new Thread(r, name);
      thread.setDaemon(isDaemon);
      thread.setPriority(priority);
      return thread;
    };
  }

  @Nonnull
  public static ThreadFactory newNamedThreadFactory(@Nonnull final String name) {
    return r -> new Thread(r, name);
  }

  ///**
  // * Awaits for all tasks in the {@code executor} to finish for the specified {@code timeout}
  // */
  //@TestOnly
  //public static void awaitQuiescence(@Nonnull ThreadPoolExecutor executor, long timeout, @Nonnull TimeUnit unit) {
  //  executor.setKeepAliveTime(1, TimeUnit.NANOSECONDS); // no need for zombies in tests
  //  executor.setCorePoolSize(0); // interrupt idle workers
  //  ReentrantLock mainLock = ReflectionUtil.getField(executor.getClass(), executor, ReentrantLock.class, "mainLock");
  //  Set workers;
  //  mainLock.lock();
  //  try {
  //    HashSet workersField = ReflectionUtil.getField(executor.getClass(), executor, HashSet.class, "workers");
  //    workers = new HashSet(workersField); // to be able to iterate thread-safely outside the lock
  //  }
  //  finally {
  //    mainLock.unlock();
  //  }
  //  for (Object worker : workers) {
  //    Thread thread = ReflectionUtil.getField(worker.getClass(), worker, Thread.class, "thread");
  //    try {
  //      thread.join(unit.toMillis(timeout));
  //    }
  //    catch (InterruptedException e) {
  //      String trace = "Thread leaked: " + thread + "; " + thread.getState() + " (" + thread.isAlive() + ")\n--- its stacktrace:\n";
  //      for (final StackTraceElement stackTraceElement : thread.getStackTrace()) {
  //        trace += " at " + stackTraceElement + "\n";
  //      }
  //      trace += "---\n";
  //      System.err.println("Executor " + executor + " is still active after " + unit.toSeconds(timeout) + " seconds://///\n" +
  //                         "Thread " + thread + " dump:\n" + trace +
  //                         "all thread dump:\n" + ThreadDumper.dumpThreadsToString() + "\n/////");
  //      break;
  //    }
  //  }
  //}

  public static void joinAll(@Nonnull Collection<? extends Thread> threads) throws RuntimeException {
    for (Thread thread : threads) {
      try {
        thread.join();
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static void joinAll(@Nonnull Thread... threads) throws RuntimeException {
    joinAll(Arrays.asList(threads));
  }

  public static void getAll(@Nonnull Collection<? extends Future<?>> futures) throws ExecutionException, InterruptedException {
    for (Future<?> future : futures) {
      future.get();
    }
  }

  @Nonnull
  @Contract(pure = true)
  public static Runnable underThreadNameRunnable(@Nonnull final String name, @Nonnull final Runnable runnable) {
    return () -> runUnderThreadName(name, runnable);
  }

  public static void runUnderThreadName(@Nonnull final String name, @Nonnull final Runnable runnable) {
    Thread currentThread = Thread.currentThread();
    String oldThreadName = currentThread.getName();
    if (name.equals(oldThreadName)) {
      runnable.run();
    }
    else {
      currentThread.setName(name);
      try {
        runnable.run();
      }
      finally {
        currentThread.setName(oldThreadName);
      }
    }
  }

  @Nonnull
  public static Runnable once(@Nonnull final Runnable delegate) {
    final AtomicBoolean done = new AtomicBoolean(false);
    return () -> {
      if (done.compareAndSet(false, true)) {
        delegate.run();
      }
    };
  }

  //public static <T, E extends Throwable> T withLock(@Nonnull Lock lock, @Nonnull ThrowableComputable<T, E> runnable) throws E {
  //  lock.lock();
  //  try {
  //    return runnable.compute();
  //  }
  //  finally {
  //    lock.unlock();
  //  }
  //}
  //
  //public static <E extends Throwable> void withLock(@Nonnull Lock lock, @Nonnull ThrowableRunnable<E> runnable) throws E {
  //  lock.lock();
  //  try {
  //    runnable.run();
  //  }
  //  finally {
  //    lock.unlock();
  //  }
  //}
}
