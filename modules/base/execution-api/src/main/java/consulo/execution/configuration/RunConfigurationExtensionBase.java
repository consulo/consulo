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
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows a plugin to extend a run configuration created by another plugin.
 *
 * @author traff
 */
public abstract class RunConfigurationExtensionBase<T extends RunConfigurationBase> {
  /**
   * Returns the ID used to serialize the settings.
   *
   * @return the serialization ID (must be unique across all run configuration extensions).
   */
  @Nonnull
  protected String getSerializationId() {
    return getClass().getCanonicalName();
  }

  /**
   * Loads the settings of this extension from the run configuration XML element. In memory, the settings can be placed into the
   * userdata of the run configuration.
   *
   * @param runConfiguration the run configuration being deserialized.
   * @param element          the element with persisted settings.
   */
  protected void readExternal(@Nonnull final T runConfiguration, @Nonnull final Element element) {
  }

  /**
   * Saves the settings of this extension to the run configuration XML element.
   *
   * @param runConfiguration the run configuration being serialized.
   * @param element          the element into which the settings should be persisted,
   */
  protected void writeExternal(@Nonnull T runConfiguration, @Nonnull Element element) {
    throw new WriteExternalException();
  }

  /**
   * Creates an editor for the settings of this extension. The editor is displayed as an additional tab of the run configuration options
   * in the Run/Debug Configurations dialog.
   *
   * @param configuration the configuration being edited.
   * @return the editor component, or null if this extension doesn't provide any UI for editing the settings.
   */
  @Nullable
  protected <P extends T> SettingsEditor<P> createEditor(@Nonnull final P configuration) {
    return null;
  }

  /**
   * Returns the title of the tab in which the settings editor is displayed.
   *
   * @return the editor tab title, or null if this extension doesn't provide any UI for editing the settings.
   */
  @Nullable
  protected String getEditorTitle() {
    return null;
  }

  /**
   * @param configuration Run configuration
   * @return True if extension in general applicable to given run configuration - just to attach settings tab, etc. But extension may be
   *         turned off in its settings. E.g. RCov in general available for given run configuration, but may be turned off.
   */
  protected abstract boolean isApplicableFor(@Nonnull final T configuration);

  /**
   *
   * @param applicableConfiguration Applicable run configuration
   * @return True if extension is turned on in configuration extension settings.
   *         E.g. RCov is turned on for given run configuration.
   */
  protected abstract boolean isEnabledFor(@Nonnull final T applicableConfiguration, @Nullable RunnerSettings runnerSettings);

  /**
   * Patches the command line of the process about to be started by the underlying run configuration.
   *
   * @param configuration  the underlying run configuration.
   * @param runnerSettings the runner-specific settings.
   * @param cmdLine        the command line of the process about to be started.
   * @param runnerId       the ID of the {@link ProgramRunner} used to start the process.
   * @throws ExecutionException if there was an error configuring the command line and the execution should be canceled.
   */
  protected abstract void patchCommandLine(@Nonnull final T configuration,
                                           @Nullable RunnerSettings runnerSettings,
                                           @Nonnull final GeneralCommandLine cmdLine,
                                           @Nonnull final String runnerId) throws ExecutionException;

  /**
   * Attaches the extension to a process that has been started.
   *
   * @param configuration  the underlying run configuration.
   * @param handler        the ProcessHandler for the running process.
   * @param runnerSettings the runner-specific settings.
   */
  protected void attachToProcess(@Nonnull final T configuration,
                                 @Nonnull final ProcessHandler handler,
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
  protected void validateConfiguration(@Nonnull final T configuration, final boolean isExecution) throws Exception {
  }

  /**
   * Setup extension settings for a run configuration that has been created from context.
   *
   * @param configuration Configuration created from context.
   * @param location      the location from which the configuration was created.
   */
  protected void extendCreatedConfiguration(@Nonnull final T configuration,
                                            @Nonnull final Location location) {

  }

  protected void extendTemplateConfiguration(@Nonnull final T configuration) {
  }
}
