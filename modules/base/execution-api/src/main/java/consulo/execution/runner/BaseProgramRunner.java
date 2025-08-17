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

package consulo.execution.runner;

import consulo.execution.RunManager;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.*;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

abstract class BaseProgramRunner<Settings extends RunnerSettings> implements ProgramRunner<Settings> {
  @Override
  @Nullable
  public Settings createConfigurationData(ConfigurationInfoProvider settingsProvider) {
    return null;
  }

  @Override
  public void checkConfiguration(RunnerSettings settings, ConfigurationPerRunnerSettings configurationPerRunnerSettings) throws RuntimeConfigurationException {
  }

  @Override
  @Nullable
  public SettingsEditor<Settings> getSettingsEditor(Executor executor, RunConfiguration configuration) {
    return null;
  }

  @Override
  public void execute(@Nonnull ExecutionEnvironment environment) throws ExecutionException {
    execute(environment, null);
  }

  @Override
  public void execute(@Nonnull ExecutionEnvironment environment, @Nullable Callback callback) throws ExecutionException {
    RunProfileState state = environment.getState();
    if (state == null) {
      return;
    }

    RunManager.getInstance(environment.getProject()).refreshUsagesList(environment.getRunProfile());
    execute(environment, callback, state);
  }

  protected abstract void execute(@Nonnull ExecutionEnvironment environment, @Nullable Callback callback, @Nonnull RunProfileState state)
          throws ExecutionException;

  @Nullable
  static RunContentDescriptor postProcess(@Nonnull ExecutionEnvironment environment, @Nullable RunContentDescriptor descriptor, @Nullable Callback callback) {
    if (descriptor != null) {
      descriptor.setExecutionId(environment.getExecutionId());
    }
    if (callback != null) {
      callback.processStarted(descriptor);
    }
    return descriptor;
  }
}