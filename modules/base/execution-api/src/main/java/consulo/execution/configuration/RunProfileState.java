/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.configuration;

import consulo.execution.ExecutionResult;
import consulo.execution.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Describes a process which is ready to be started. Normally, a RunProfileState contains an initialized command line, set of environment
 * variables, working directory etc.
 *
 * @see CommandLineState
 * @see RunConfiguration#getState(Executor, ExecutionEnvironment)
 */
public interface RunProfileState {
  /**
   * Starts the process.
   *
   * @param executor the executor used to start up the process.
   * @param runner   the program runner used to start up the process.
   * @return the result (normally an instance of {@link com.intellij.execution.DefaultExecutionResult}), containing a process handler
   * and a console attached to it.
   * @throws ExecutionException if the execution has failed.
   */
  @Nullable
  ExecutionResult execute(final Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException;
}
