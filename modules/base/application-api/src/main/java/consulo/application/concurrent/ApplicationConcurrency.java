/*
 * Copyright 2013-2023 consulo.io
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
package consulo.application.concurrent;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author VISTALL
 * @since 13/09/2023
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ApplicationConcurrency {
  @Nonnull
  ExecutorService getExecutorService();

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
  ScheduledExecutorService getScheduledExecutorService();

  /**
   * @return the bounded executor (executor which runs no more than {@code maxThreads} tasks simultaneously) backed by the application pool
   * (i.e. all tasks are run in the {@link #getExecutorService()} global thread pool).
   * @see #getExecutorService()
   */
  @Nonnull
  ScheduledExecutorService createBoundedScheduledExecutorService(@Nonnull String name, int maxThreads);


  /**
   * @return the bounded executor (executor which runs no more than {@code maxThreads} tasks simultaneously) backed by the {@code backendExecutor}
   */
  @Nonnull
  ExecutorService createBoundedApplicationPoolExecutor(@Nonnull String name,
                                                       @Nonnull Executor backendExecutor,
                                                       int maxThreads);

  /**
   * @param name is used to generate thread name which will be shown in thread dumps, so it should be human readable and use title capitalization
   * @return the bounded executor (executor which runs no more than {@code maxThreads} tasks simultaneously) backed by the {@code backendExecutor}
   * which will shutdown itself when {@code parentDisposable} gets disposed.
   */
  @Nonnull
  ExecutorService createBoundedApplicationPoolExecutor(@Nonnull String name,
                                                       @Nonnull Executor backendExecutor,
                                                       int maxThreads,
                                                       @Nonnull Disposable parentDisposable);

  /**
   * @param name is used to generate thread name which will be shown in thread dumps, so it should be human readable and use title capitalization
   * @return the bounded executor (executor which runs no more than {@code maxThreads} tasks simultaneously) backed by the {@code backendExecutor}
   * which will shutdown itself when {@code parentDisposable} gets disposed.
   */
  @Nonnull
  ExecutorService createBoundedApplicationPoolExecutor(@Nonnull String name,
                                                       int maxThreads,
                                                       @Nonnull Disposable parentDisposable);

  @Nonnull
  default ExecutorService createSequentialApplicationPoolExecutor(@Nonnull String name) {
    return createBoundedApplicationPoolExecutor(name, getExecutorService(), 1);
  }

  @Nonnull
  default ExecutorService createSequentialApplicationPoolExecutor(@Nonnull String name, @Nonnull Executor executor) {
    return createBoundedApplicationPoolExecutor(name, executor, 1);
  }
}
