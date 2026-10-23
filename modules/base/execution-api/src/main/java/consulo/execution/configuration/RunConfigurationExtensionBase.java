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
package consulo.execution.configuration;

import consulo.execution.action.Location;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;

import org.jspecify.annotations.Nullable;


/**
 * Run-time behaviour a plugin adds to run configurations of another plugin. Its settings and their editor are a contributed
 * {@link consulo.execution.configuration.fragment.RunConfigurationFragment} with its own state, read here through
 * {@link RunConfigurationBase#getFragmentState(Class)}; old {@code <extension>} elements are converted by that fragment's
 * {@link consulo.execution.configuration.fragment.RunConfigurationFragment#convertLegacyState}.
 *
 * @author traff
 */
public abstract class RunConfigurationExtensionBase<T extends RunConfigurationBase> {
  /**
   * @param configuration Run configuration
   * @return True if extension in general applicable to given run configuration. But extension may be turned off in its settings.
   *         E.g. RCov in general available for given run configuration, but may be turned off.
   */
  protected abstract boolean isApplicableFor(T configuration);

  /**
   *
   * @param applicableConfiguration Applicable run configuration
   * @return True if extension is turned on in configuration extension settings.
   *         E.g. RCov is turned on for given run configuration.
   */
  protected abstract boolean isEnabledFor(T applicableConfiguration, @Nullable RunnerSettings runnerSettings);

  /**
   * Patches the command line of the process about to be started by the underlying run configuration.
   *
   * @param configuration  the underlying run configuration.
   * @param runnerSettings the runner-specific settings.
   * @param cmdLine        the command line of the process about to be started.
   * @param runnerId       the ID of the {@link ProgramRunner} used to start the process.
   * @throws ExecutionException if there was an error configuring the command line and the execution should be canceled.
   */
  protected abstract void patchCommandLine(T configuration,
                                           @Nullable RunnerSettings runnerSettings,
                                           GeneralCommandLine cmdLine,
                                           String runnerId) throws ExecutionException;

  /**
   * Attaches the extension to a process that has been started.
   *
   * @param configuration  the underlying run configuration.
   * @param handler        the ProcessHandler for the running process.
   * @param runnerSettings the runner-specific settings.
   */
  protected void attachToProcess(T configuration,
                                 ProcessHandler handler,
                                 @Nullable RunnerSettings runnerSettings) {
  }

  /**
   * Validate extensions after general configuration validation passed
   *
   * @param configuration
   * @param isExecution   true if the configuration is about to be executed, false if the configuration settings are being edited.
   * @throws ExecutionException
   *
   */
  protected void validateConfiguration(T configuration, boolean isExecution) throws Exception {
  }

  /**
   * Setup extension settings for a run configuration that has been created from context.
   *
   * @param configuration Configuration created from context.
   * @param location      the location from which the configuration was created.
   */
  protected void extendCreatedConfiguration(T configuration,
                                            Location location) {
  }

  protected void extendTemplateConfiguration(T configuration) {
  }
}
