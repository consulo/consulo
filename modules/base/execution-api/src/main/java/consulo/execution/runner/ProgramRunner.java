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
package consulo.execution.runner;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.execution.ExecutionResult;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.*;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A ProgramRunner is responsible for the execution workflow of certain types of run configurations with a certain executor. For example,
 * one ProgramRunner can be responsible for debugging all Java-based run configurations (applications, JUnit tests, etc.); the run
 * configuration takes care of building a command line and the program runner takes care of how exactly it needs to be executed.
 *
 * @param <Settings>
 * @see GenericProgramRunner
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProgramRunner<Settings extends RunnerSettings> {
  ExtensionPointName<ProgramRunner> PROGRAM_RUNNER_EP = ExtensionPointName.create(ProgramRunner.class);

  interface Callback {
    void processStarted(RunContentDescriptor descriptor);
  }

  @Nullable
  static ProgramRunner findRunnerById(@Nonnull String id) {
    for (ProgramRunner registeredRunner : PROGRAM_RUNNER_EP.getExtensionList()) {
      if (id.equals(registeredRunner.getRunnerId())) {
        return registeredRunner;
      }
    }
    return null;
  }

  @Nullable
  static ProgramRunner<RunnerSettings> getRunner(@Nonnull String executorId, @Nonnull RunProfile settings) {
    for (ProgramRunner<RunnerSettings> runner : PROGRAM_RUNNER_EP.getExtensionList()) {
      if (runner.canRun(executorId, settings)) {
        return runner;
      }
    }
    return null;
  }

  /**
   * Returns the unique ID of this runner. This ID is used to store settings and must not change between plugin versions.
   *
   * @return the program runner ID.
   */
  @Nonnull
  String getRunnerId();

  /**
   * Checks if the program runner is capable of running the specified configuration with the specified executor.
   *
   * @param executorId ID of the {@link Executor} with which the user is trying to run the configuration.
   * @param profile    the configuration being run.
   * @return true if the runner can handle it, false otherwise.
   */
  boolean canRun(@Nonnull final String executorId, @Nonnull final RunProfile profile);

  /**
   * Creates a block of per-configuration settings used by this program runner.
   *
   * @param settingsProvider source of assorted information about the configuration being edited.
   * @return the per-runner settings, or null if this runner doesn't use any per-runner settings.
   */
  @Nullable
  Settings createConfigurationData(ConfigurationInfoProvider settingsProvider);

  void checkConfiguration(RunnerSettings settings, @Nullable ConfigurationPerRunnerSettings configurationPerRunnerSettings) throws RuntimeConfigurationException;

  void onProcessStarted(RunnerSettings settings, ExecutionResult executionResult);

  @Nullable
  SettingsEditor<Settings> getSettingsEditor(Executor executor, RunConfiguration configuration);

  void execute(@Nonnull ExecutionEnvironment environment) throws ExecutionException;

  void execute(@Nonnull ExecutionEnvironment environment, @Nullable Callback callback) throws ExecutionException;
}