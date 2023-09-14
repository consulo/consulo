// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.concurrent;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;

import java.util.concurrent.*;

@Deprecated
@DeprecationInfo("Use ApplicationConcurrency")
public final class AppExecutorUtil {
  /**
   * Returns application-wide instance of {@link ScheduledExecutorService} which is:
   * <ul>
   * <li>Unbounded. I.e. multiple {@link ScheduledExecutorService#schedule}(command, 0, TimeUnit.SECONDS) will lead to multiple executions of the {@code command} in parallel.</li>
   * <li>Backed by the application thread pool. I.e. every scheduled task will be executed in the IDE's own thread pool. See {@link Application#executeOnPooledThread(Runnable)}</li>
   * <li>Non-shutdownable singleton. Any attempts to call {@link ExecutorService#shutdown()}, {@link ExecutorService#shutdownNow()} will be severely punished.</li>
   * <li>{@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} is disallowed because it's bad for hibernation.
   * Use {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)} instead.</li>
   * </ul>
   * </ul>
   */
  @Nonnull
  public static ScheduledExecutorService getAppScheduledExecutorService() {
    ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
    return concurrency.getScheduledExecutorService();
  }

  /**
   * Application thread pool.
   * This pool is<ul>
   * <li>Unbounded.</li>
   * <li>Application-wide, always active, non-shutdownable singleton.</li>
   * </ul>
   * You can use this pool for long-running and/or IO-bound tasks.
   *
   * @see Application#executeOnPooledThread(Runnable)
   */
  @Nonnull
  public static ExecutorService getAppExecutorService() {
    ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
    return concurrency.getExecutorService();
  }

  /**
   * Returns {@link ScheduledExecutorService} which allows to {@link ScheduledExecutorService#schedule(Callable, long, TimeUnit)} tasks later
   * and execute them in parallel in the application pool (see {@link #getAppExecutorService()} not more than at {@code maxThreads} at a time.
   */
  @Nonnull
  public static ScheduledExecutorService createBoundedScheduledExecutorService(@Nonnull String name, int maxThreads) {
    ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
    return concurrency.createBoundedScheduledExecutorService(name, maxThreads);
  }

  /**
   * @return the bounded executor (executor which runs no more than {@code maxThreads} tasks simultaneously) backed by the application pool
   * (i.e. all tasks are run in the {@link #getAppExecutorService()} global thread pool).
   * @see #getAppExecutorService()
   */
  @Nonnull
  public static ExecutorService createBoundedApplicationPoolExecutor(@Nonnull String name, int maxThreads) {
    return createBoundedApplicationPoolExecutor(name, getAppExecutorService(), maxThreads);
  }
  /**
   * @return the bounded executor (executor which runs no more than {@code maxThreads} tasks simultaneously) backed by the {@code backendExecutor}
   */
  @Nonnull
  public static ExecutorService createBoundedApplicationPoolExecutor(@Nonnull String name,
                                                                     @Nonnull Executor backendExecutor,
                                                                     int maxThreads) {
    ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
    return concurrency.createBoundedApplicationPoolExecutor(name, backendExecutor, maxThreads);
  }

  /**
   * @param name is used to generate thread name which will be shown in thread dumps, so it should be human readable and use title capitalization
   * @return the bounded executor (executor which runs no more than {@code maxThreads} tasks simultaneously) backed by the {@code backendExecutor}
   * which will shutdown itself when {@code parentDisposable} gets disposed.
   */
  @Nonnull
  public static ExecutorService createBoundedApplicationPoolExecutor(@Nonnull String name,
                                                                     @Nonnull Executor backendExecutor,
                                                                     int maxThreads,
                                                                     @Nonnull Disposable parentDisposable) {
    ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
    return concurrency.createBoundedApplicationPoolExecutor(name, backendExecutor, maxThreads, parentDisposable);
  }
}
