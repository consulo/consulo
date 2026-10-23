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

import consulo.annotation.DeprecationInfo;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.runner.ProgramRunner;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.xml.serializer.JDOMExternalizable;

import org.jspecify.annotations.Nullable;

import java.util.List;

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
   * The fragments of this configuration type shown in the run configuration editor, in page order. Shared fragments
   * (program arguments, working directory, environment, ...) are added by the platform.
   */
  default List<RunConfigurationFragment<?>> createFragments() {
    return List.of();
  }

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
  @Nullable ConfigurationPerRunnerSettings createRunnerSettings(ConfigurationInfoProvider provider);

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
