/*
 * Copyright 2013-2024 consulo.io
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.util.concurrent.AppExecutorUtil;
import jakarta.annotation.Nonnull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 27-Jun-24
 */
public class ChangeListScheduler {
  private final ScheduledExecutorService myExecutor =
    AppExecutorUtil.createBoundedScheduledExecutorService("ChangeListManagerImpl pool", 1);

  public void schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
    myExecutor.schedule(command, delay, unit);
  }

  public void submit(@Nonnull Runnable command) {
    myExecutor.submit(command);
  }
}
