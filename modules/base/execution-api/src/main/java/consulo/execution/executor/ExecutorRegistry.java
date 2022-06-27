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

package consulo.execution.executor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;

import javax.annotation.Nonnull;

/**
 * @author spleaner
 */
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
public abstract class ExecutorRegistry {
  public static ExecutorRegistry getInstance() {
    return ApplicationManager.getApplication().getInstance(ExecutorRegistry.class);
  }

  @Nonnull
  public abstract Executor[] getRegisteredExecutors();

  public abstract Executor getExecutorById(final String executorId);

  /**
   * Consider to use {@link #isStarting(ExecutionEnvironment)}
   */
  public abstract boolean isStarting(Project project, String executorId, String runnerId);

  public abstract boolean isStarting(@Nonnull ExecutionEnvironment environment);
}
