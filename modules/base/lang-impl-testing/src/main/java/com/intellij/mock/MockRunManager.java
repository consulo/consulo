package com.intellij.mock;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import consulo.util.dataholder.Key;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author gregsh
 */
public class MockRunManager extends RunManagerEx {
  @Nonnull
  @Override
  public List<ConfigurationType> getConfigurationFactories(boolean includeUnknown) {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public RunConfiguration[] getConfigurations(@Nonnull ConfigurationType type) {
    return new RunConfiguration[0];
  }

  @Nonnull
  @Override
  public List<RunConfiguration> getConfigurationsList(@Nonnull ConfigurationType type) {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public RunConfiguration[] getAllConfigurations() {
    return new RunConfiguration[0];
  }

  @Nonnull
  @Override
  public List<RunConfiguration> getAllConfigurationsList() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public RunConfiguration[] getTempConfigurations() {
    return new RunConfiguration[0];
  }

  @Nonnull
  @Override
  public List<RunnerAndConfigurationSettings> getTempConfigurationsList() {
    return Collections.emptyList();
  }

  @Override
  public boolean isTemporary(@Nonnull RunConfiguration configuration) {
    return false;
  }

  @Override
  public void makeStable(@Nonnull RunConfiguration configuration) {
  }

  @Override
  public void makeStable(@Nonnull RunnerAndConfigurationSettings settings) {
  }

  @Override
  public RunnerAndConfigurationSettings getSelectedConfiguration() {
    return null;
  }

  @Nonnull
  @Override
  public RunnerAndConfigurationSettings createRunConfiguration(@Nonnull String name, @Nonnull ConfigurationFactory type) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RunnerAndConfigurationSettings createConfiguration(@Nonnull RunConfiguration runConfiguration, @Nonnull ConfigurationFactory factory) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RunnerAndConfigurationSettings getConfigurationTemplate(ConfigurationFactory factory) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public RunnerAndConfigurationSettings[] getConfigurationSettings(@Nonnull ConfigurationType type) {
    return new RunnerAndConfigurationSettings[0];
  }

  @Override
  @Nonnull
  public List<RunnerAndConfigurationSettings> getConfigurationSettingsList(@Nonnull ConfigurationType type) {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Map<String, List<RunnerAndConfigurationSettings>> getStructure(@Nonnull ConfigurationType type) {
    return Collections.emptyMap();
  }

  @Nonnull
  @Override
  public List<RunnerAndConfigurationSettings> getAllSettings() {
    return Collections.emptyList();
  }

  @Override
  public void setSelectedConfiguration(RunnerAndConfigurationSettings configuration) {
  }

  @Override
  public void setTemporaryConfiguration(RunnerAndConfigurationSettings tempConfiguration) {
  }

  @Override
  public RunManagerConfig getConfig() {
    return null;
  }

  @Nonnull
  @Override
  public RunnerAndConfigurationSettings createConfiguration(String name, ConfigurationFactory type) {
    return null;
  }

  @Override
  public void addConfiguration(RunnerAndConfigurationSettings settings,
                               boolean isShared,
                               List<BeforeRunTask> tasks,
                               boolean addTemplateTasksIfAbsent) {
  }

  @Override
  public void addConfiguration(RunnerAndConfigurationSettings settings, boolean isShared) {
  }

  @Override
  public boolean isConfigurationShared(RunnerAndConfigurationSettings settings) {
    return false;
  }

  @Nonnull
  @Override
  public List<BeforeRunTask> getBeforeRunTasks(RunConfiguration settings) {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public <T extends BeforeRunTask> List<T> getBeforeRunTasks(Key<T> taskProviderID) {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public <T extends BeforeRunTask> List<T> getBeforeRunTasks(RunConfiguration settings, Key<T> taskProviderID) {
    return Collections.emptyList();
  }

  @Override
  public void setBeforeRunTasks(RunConfiguration runConfiguration, List<BeforeRunTask> tasks, boolean addEnabledTemplateTasksIfAbsent) {
  }

  @Override
  public RunnerAndConfigurationSettings findConfigurationByName(@Nonnull String name) {
    return null;
  }

  @Override
  public Image getConfigurationIcon(@Nonnull RunnerAndConfigurationSettings settings) {
    return null;
  }

  @Override
  @Nonnull
  public Collection<RunnerAndConfigurationSettings> getSortedConfigurations() {
    return Collections.emptyList();
  }

  @Override
  public void removeConfiguration(RunnerAndConfigurationSettings settings) {
  }

  @Override
  public void refreshUsagesList(RunProfile profile) {
  }

  @Override
  public boolean hasSettings(RunnerAndConfigurationSettings settings) {
    return false;
  }
}
