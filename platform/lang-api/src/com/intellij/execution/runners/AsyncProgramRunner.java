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
import com.intellij.openapi.util.ThrowableComputable;
import consulo.concurrency.Promises;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

/**
 * @author VISTALL
 * @since 05-May-17
 * <p>
 * from kotlin platform\lang-api\src\com\intellij\execution\runners\GenericProgramRunner.kt
 */
public abstract class AsyncProgramRunner<Settings extends RunnerSettings> extends BaseProgramRunner<Settings> {
  @Override
  protected void execute(@NotNull ExecutionEnvironment environment, @Nullable Callback callback, @NotNull RunProfileState state) throws ExecutionException {
    startRunProfile(environment, state, callback, runProfileStarter(() -> execute(environment, state)));
  }

  @NotNull
  protected abstract Promise<RunContentDescriptor> execute(@NotNull ExecutionEnvironment environment, @NotNull RunProfileState state) throws ExecutionException;


  protected static void startRunProfile(ExecutionEnvironment environment,
                                        RunProfileState state,
                                        ProgramRunner.Callback callback,
                                        @Nullable RunProfileStarter starter) {

    ThrowableComputable<Promise<RunContentDescriptor>, ExecutionException> func = () -> {
      Promise<RunContentDescriptor> promise = starter == null ? Promises.<RunContentDescriptor>resolvedPromise() : starter.executeAsync(state, environment);
      return promise.then(it -> BaseProgramRunner.postProcess(environment, it, callback));
    };

    ExecutionManager.getInstance(environment.getProject()).startRunProfile(runProfileStarter(func), state, environment);
  }

  private static RunProfileStarter runProfileStarter(ThrowableComputable<Promise<RunContentDescriptor>, ExecutionException> starter) {
    return new RunProfileStarterImpl(starter);
  }

  private static class RunProfileStarterImpl extends RunProfileStarter {
    private final ThrowableComputable<Promise<RunContentDescriptor>, ExecutionException> starter;

    private RunProfileStarterImpl(ThrowableComputable<Promise<RunContentDescriptor>, ExecutionException> starter) {
      this.starter = starter;
    }

    @Override
    public Promise<RunContentDescriptor> executeAsync(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
      return starter.compute();
    }
  }
}

