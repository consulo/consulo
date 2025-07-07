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

import consulo.execution.BeforeRunTask;
import consulo.execution.RunManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Factory for run configuration instances.
 *
 * @author dyoma
 * @see ConfigurationType#getConfigurationFactories()
 */
public abstract class ConfigurationFactory {
  private final ConfigurationType myType;

  /**
   * Used only buy SimpleConfigurationType
   */
  protected ConfigurationFactory() {
    myType = (ConfigurationType)this;
  }

  protected ConfigurationFactory(@Nonnull final ConfigurationType type) {
    myType = type;
  }

  /**
   * Creates a new run configuration with the specified name by cloning the specified template.
   *
   * @param name     the name for the new run configuration.
   * @param template the template from which the run configuration is copied
   * @return the new run configuration.
   */
  public RunConfiguration createConfiguration(String name, RunConfiguration template) {
    RunConfiguration newConfiguration = template.clone();
    newConfiguration.setName(name);
    return newConfiguration;
  }

  /**
   * Override this method and return {@code false} to hide the configuration from 'New' popup in 'Edit Configurations' dialog
   *
   * @return {@code true} if it makes sense to create configurations of this type in {@code project}
   */
  public boolean isApplicable(@Nonnull Project project) {
    return true;
  }

  /**
   * Creates a new template run configuration within the context of the specified project.
   *
   * @param project the project in which the run configuration will be used
   * @return the run configuration instance.
   */
  public abstract RunConfiguration createTemplateConfiguration(Project project);

  public RunConfiguration createTemplateConfiguration(Project project, RunManager runManager) {
    return createTemplateConfiguration(project);
  }

  /**
   * @return id for factory
   */
  @Nonnull
  public String getId() {
    return myType.getId();
  }

  /**
   * Returns the name of the run configuration variant created by this factory.
   *
   * @return the name of the run configuration variant created by this factory
   */
  @Nonnull
  public LocalizeValue getDisplayName() {
    return myType.getDisplayName();
  }

  @Nullable
  public Image getIcon(@Nonnull final RunConfiguration configuration) {
    return getIcon();
  }

  @Nullable
  public Image getIcon() {
    return myType.getIcon();
  }

  @Nonnull
  public ConfigurationType getType() {
    return myType;
  }

  /**
   * In this method you can configure defaults for the task, which are preferable to be used for your particular configuration type
   *
   * @param providerID
   * @param task
   */
  public void configureBeforeRunTaskDefaults(Key<? extends BeforeRunTask> providerID, BeforeRunTask task) {
  }

  public boolean isConfigurationSingletonByDefault() {
    return true;
  }

  public boolean canConfigurationBeSingleton() {
    return true;
  }

  public void onNewConfigurationCreated(@Nonnull RunConfiguration configuration) {
      if (configuration instanceof ConfigurationCreationListener listener) {
          listener.onNewConfigurationCreated();
      }
  }

  public void onConfigurationCopied(@Nonnull RunConfiguration configuration) {
      if (configuration instanceof ConfigurationCreationListener listener) {
          listener.onConfigurationCopied();
      }
  }
}
