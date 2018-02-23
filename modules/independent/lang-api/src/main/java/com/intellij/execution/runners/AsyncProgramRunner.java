/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.execution.runners;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunProfileStarter;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.ThrowableComputable;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05-May-17
 * <p>
 * from kotlin platform\lang-api\src\com\intellij\execution\runners\GenericProgramRunner.kt
 */
public abstract class AsyncProgramRunner<Settings extends RunnerSettings> extends BaseProgramRunner<Settings> {
  @Override
  protected void execute(@Nonnull ExecutionEnvironment environment, @javax.annotation.Nullable Callback callback, @Nonnull RunProfileState state) throws ExecutionException {
    startRunProfile(environment, state, callback, runProfileStarter(() -> execute(environment, state)));
  }

  @Nonnull
  protected abstract AsyncResult<RunContentDescriptor> execute(@Nonnull ExecutionEnvironment environment, @Nonnull RunProfileState state) throws ExecutionException;


  protected static void startRunProfile(ExecutionEnvironment environment,
                                        RunProfileState state,
                                        ProgramRunner.Callback callback,
                                        @javax.annotation.Nullable RunProfileStarter starter) {

    ThrowableComputable<AsyncResult<RunContentDescriptor>, ExecutionException> func = () -> {
      AsyncResult<RunContentDescriptor> promise = starter == null ? AsyncResult.done(null) : starter.executeAsync(state, environment);
      return promise.doWhenDone(it -> BaseProgramRunner.postProcess(environment, it, callback));
    };

    ExecutionManager.getInstance(environment.getProject()).startRunProfile(runProfileStarter(func), state, environment);
  }

  private static RunProfileStarter runProfileStarter(ThrowableComputable<AsyncResult<RunContentDescriptor>, ExecutionException> starter) {
    return new RunProfileStarterImpl(starter);
  }

  private static class RunProfileStarterImpl extends RunProfileStarter {
    private final ThrowableComputable<AsyncResult<RunContentDescriptor>, ExecutionException> starter;

    private RunProfileStarterImpl(ThrowableComputable<AsyncResult<RunContentDescriptor>, ExecutionException> starter) {
      this.starter = starter;
    }

    @Override
    public AsyncResult<RunContentDescriptor> executeAsync(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
      return starter.compute();
    }
  }
}

