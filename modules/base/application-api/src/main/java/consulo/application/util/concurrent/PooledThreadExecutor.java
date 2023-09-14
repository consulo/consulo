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
package consulo.application.util.concurrent;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;

import java.util.concurrent.ExecutorService;

/**
 * Application tread pool.
 * This pool is<ul>
 * <li>Unbounded.</li>
 * <li>Application-wide, always active, non-shutdownable singleton.</li>
 * </ul>
 * You can use this pool for long-running and/or IO-bound tasks.
 *
 * @see Application#executeOnPooledThread(Runnable)
 */
@Deprecated
public final class PooledThreadExecutor {
  public static ExecutorService getInstance() {
    ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
    return concurrency.getExecutorService();
  }
}