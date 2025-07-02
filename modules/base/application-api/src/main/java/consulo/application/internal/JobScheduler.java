/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.application.internal;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.util.concurrent.AppExecutorUtil;
import jakarta.annotation.Nonnull;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author max
 */
@Deprecated
public abstract class JobScheduler {
  /**
   * Returns application-wide instance of {@link ScheduledExecutorService} which is:
   * <ul>
   * <li>Unbounded. I.e. multiple {@link ScheduledExecutorService#schedule}(command, 0, TimeUnit.SECONDS) will lead to multiple executions of the {@code command} in parallel.</li>
   * <li>Backed by the application thread pool. I.e. every scheduled task will be executed in IDEA own thread pool. See {@link Application#executeOnPooledThread(Runnable)}</li>
   * <li>Non-shutdownable singleton. Any attempts to call {@link ExecutorService#shutdown()}, {@link ExecutorService#shutdownNow()} will be severely punished.</li>
   * <li>{@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} is disallowed because it's bad for hibernation.
   * Use {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)} instead.</li>
   * </ul>
   * If you need to execute only one task (when it's ready) at a time, you can use {@link AppExecutorUtil#createBoundedScheduledExecutorService(int)}.
   */
  @Nonnull
  public static ScheduledExecutorService getScheduler() {
    ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
    return concurrency.getScheduledExecutorService();
  }
}