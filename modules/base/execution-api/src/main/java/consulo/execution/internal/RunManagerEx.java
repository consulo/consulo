/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.internal;

import consulo.execution.BeforeRunTask;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class RunManagerEx extends RunManager {
  public static RunManagerEx getInstanceEx(Project project) {
    return (RunManagerEx)project.getInstance(RunManager.class);
  }

  /**
   * @deprecated use {@link #setSelectedConfiguration(RunnerAndConfigurationSettings)} instead
   */
  @Deprecated
  public void setActiveConfiguration(@Nullable RunnerAndConfigurationSettings configuration) {
    setSelectedConfiguration(configuration);
  }

  public abstract void setTemporaryConfiguration(@Nullable RunnerAndConfigurationSettings tempConfiguration);

  public abstract RunManagerConfig getConfig();

  /**
   * @param name
   * @param type
   * @return
   * @deprecated use {@link RunManager#createRunConfiguration(String, ConfigurationFactory)} instead
   */
  @Nonnull
  public abstract RunnerAndConfigurationSettings createConfiguration(String name, ConfigurationFactory type);

  public abstract RunnerAndConfigurationSettings createConfiguration(@Nonnull RunConfiguration configuration, boolean isTemplate);

  public abstract void addConfiguration(RunnerAndConfigurationSettings settings, boolean isShared, List<BeforeRunTask> tasks, boolean addTemplateTasksIfAbsent);

  public abstract boolean isConfigurationShared(RunnerAndConfigurationSettings settings);

  @Override
  @Nonnull
  public abstract List<BeforeRunTask> getBeforeRunTasks(RunConfiguration settings);

  @Override
  public abstract void setBeforeRunTasks(RunConfiguration runConfiguration, List<BeforeRunTask> tasks, boolean addEnabledTemplateTasksIfAbsent);

  @Nonnull
  public abstract <T extends BeforeRunTask> List<T> getBeforeRunTasks(RunConfiguration settings, Key<T> taskProviderID);

  @Nonnull
  public abstract <T extends BeforeRunTask> List<T> getBeforeRunTasks(Key<T> taskProviderID);

  public abstract RunnerAndConfigurationSettings findConfigurationByName(@Nullable String name);

  @Nonnull
  public abstract Image getConfigurationIcon(@Nonnull RunnerAndConfigurationSettings settings);

  @Nonnull
  public abstract Collection<RunnerAndConfigurationSettings> getSortedConfigurations();

  public abstract void removeConfiguration(@Nullable RunnerAndConfigurationSettings settings);

  @Nonnull
  public abstract Map<String, List<RunnerAndConfigurationSettings>> getStructure(@Nonnull ConfigurationType type);

  public abstract void fireBeforeRunTasksUpdated();
}