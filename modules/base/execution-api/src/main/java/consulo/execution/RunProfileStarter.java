/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution;

import consulo.execution.configuration.RunProfileState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Internal use only. Please use {@link consulo.ide.impl.idea.execution.runners.GenericProgramRunner} or {@link consulo.ide.impl.idea.execution.runners.AsyncProgramRunner}.
 *
 * The callback used to execute a process from the {@link ExecutionManager#startRunProfile(RunProfileStarter, RunProfileState, ExecutionEnvironment)}*
 * @author nik
 */
public abstract class RunProfileStarter {
  @Nullable
  @Deprecated
  public RunContentDescriptor execute(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
    throw new AbstractMethodError();
  }

  /**
   * You should NOT throw exceptions in this method.
   * Instead return {@link AsyncResult#done(Object)} (Throwable)} or call {@link AsyncResult#rejectWithThrowable(Throwable)} 
   */
  public AsyncResult<RunContentDescriptor> executeAsync(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
    return AsyncResult.done(execute(state, environment));
  }
}