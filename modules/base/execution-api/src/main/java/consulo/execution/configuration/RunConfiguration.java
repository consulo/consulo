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

import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.runner.ProgramRunner;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.xml.serializer.JDOMExternalizable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Interface for run configurations which can be managed by a user and displayed in the UI.
 *
 * @see consulo.ide.impl.idea.execution.RunManager
 * @see RunConfigurationBase
 * @see RefactoringListenerProvider
 */
public interface RunConfiguration extends RunProfile, JDOMExternalizable, Cloneable {
  Key<RunConfiguration> KEY = Key.of(RunConfiguration.class);
  @Deprecated
  Key<RunConfiguration> DATA_KEY = KEY;

  /**
   * Returns the type of the run configuration.
   *
   * @return the configuration type.
   */
  @Nonnull
  ConfigurationType getType();

  /**
   * Returns the factory that has created the run configuration.
   *
   * @return the factory instance.
   */
  ConfigurationFactory getFactory();

  /**
   * Sets the name of the configuration.
   *
   * @param name the new name of the configuration.
   */
  void setName(String name);

  /**
   * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
   * returned from this method may also implement {@link consulo.ide.impl.idea.execution.impl.CheckableRunConfigurationEditor}. The returned object
   * can also implement {@link consulo.ide.impl.idea.openapi.options.SettingsEditorGroup} if the settings it provides need to be displayed in
   * multiple tabs.
   *
   * @return the settings editor component.
   */
  @Nonnull
  SettingsEditor<? extends RunConfiguration> getConfigurationEditor();

  /**
   * Returns the project in which the run configuration exists.
   *
   * @return the project instance.
   */
  Project getProject();

  /**
   * Creates a block of settings for a specific {@link ProgramRunner}. Can return null if the configuration has no settings specific
   * to a program runner.
   *
   * @param provider source of assorted information about the configuration being edited.
   * @return the per-runner settings.
   */
  @Nullable
  ConfigurationPerRunnerSettings createRunnerSettings(ConfigurationInfoProvider provider);

  /**
   * Creates a UI control for editing the settings for a specific {@link ProgramRunner}. Can return null if the configuration has no
   * settings specific to a program runner.
   *
   * @param runner the runner the settings for which need to be edited.
   * @return the editor for the per-runner settings.
   */
  @Nullable
  SettingsEditor<ConfigurationPerRunnerSettings> getRunnerSettingsEditor(ProgramRunner runner);

  /**
   * Clones the run configuration.
   *
   * @return a clone of this run configuration.
   */
  RunConfiguration clone();

  /**
   * Returns the unique identifier of the run configuration. The identifier does not need to be persisted between the sessions.
   *
   * @return the unique ID of the configuration.
   */
  @Deprecated
  int getUniqueID();

  /**
   * Checks whether the run configuration settings are valid.
   *
   * @throws RuntimeConfigurationException if the configuration settings contain a non-fatal problem which the user should be warned about
   * but the execution should still be allowed
   * @throws RuntimeConfigurationError if the configuration settings contain a fatal problem which makes it impossible to execute the run
   * configuration.
   */
  void checkConfiguration() throws RuntimeConfigurationException;
}
