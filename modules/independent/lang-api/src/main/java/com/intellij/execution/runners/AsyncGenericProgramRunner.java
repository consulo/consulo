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
package com.intellij.execution.runners;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunProfileStarter;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;

/**
 * @deprecated Use AsyncProgramRunner
 */
@Deprecated
public abstract class AsyncGenericProgramRunner<Settings extends RunnerSettings> extends BaseProgramRunner<Settings> {
  @Override
  protected final void execute(@Nonnull ExecutionEnvironment environment, @javax.annotation.Nullable Callback callback, @Nonnull RunProfileState state)
          throws ExecutionException {
    prepare(environment, state).doWhenDone(result -> UIUtil.invokeLaterIfNeeded(() -> {
      if (!environment.getProject().isDisposed()) {
        AsyncProgramRunner.startRunProfile(environment, state, callback, result);
      }
    }));
  }

  /**
   * Makes all the needed preparations for the further execution. Although this method is called in EDT,
   * these preparations can be performed in a background thread.
   * Please note that {@link RunProfileState#execute(Executor, ProgramRunner)} should not be called during the preparations
   * to not execute the run profile before "Before launch" tasks.
   * <p>
   * You must call {@link ExecutionUtil#handleExecutionError} in case of error
   *
   * @param environment ExecutionEnvironment instance
   * @param state       RunProfileState instance
   * @return RunProfileStarter async result
   */
  @Nonnull
  protected abstract AsyncResult<RunProfileStarter> prepare(@Nonnull ExecutionEnvironment environment, @Nonnull RunProfileState state) throws ExecutionException;
}
