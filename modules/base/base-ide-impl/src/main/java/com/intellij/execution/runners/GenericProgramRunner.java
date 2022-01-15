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

package com.intellij.execution.runners;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunProfileStarter;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.ui.RunContentDescriptor;
import javax.annotation.Nonnull;

public abstract class GenericProgramRunner<Settings extends RunnerSettings> extends BaseProgramRunner<Settings> {
  @Override
  protected void execute(@Nonnull ExecutionEnvironment environment, @javax.annotation.Nullable final Callback callback, @Nonnull RunProfileState state) throws ExecutionException {
    ExecutionManager.getInstance(environment.getProject()).startRunProfile(new RunProfileStarter() {
      @Override
      public RunContentDescriptor execute(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
        return postProcess(environment, doExecute(state, environment), callback);
      }
    }, state, environment);
  }

  @javax.annotation.Nullable
  protected RunContentDescriptor doExecute(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
    return null;
  }
}
