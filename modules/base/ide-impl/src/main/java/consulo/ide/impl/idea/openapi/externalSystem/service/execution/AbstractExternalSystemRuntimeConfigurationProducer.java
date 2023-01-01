/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.service.execution;

import consulo.execution.action.Location;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.action.RuntimeConfigurationProducer;
import consulo.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import consulo.externalSystem.service.execution.ExternalSystemRunConfiguration;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.language.psi.PsiElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 6/5/13 8:14 PM
 */
public abstract class AbstractExternalSystemRuntimeConfigurationProducer extends RuntimeConfigurationProducer {

  private PsiElement mySourceElement;
  
  public AbstractExternalSystemRuntimeConfigurationProducer(@Nonnull AbstractExternalSystemTaskConfigurationType type) {
    super(type);
  }

  @Override
  public PsiElement getSourceElement() {
    return mySourceElement;
  }

  @Nullable
  @Override
  protected RunnerAndConfigurationSettings createConfigurationByElement(Location location, ConfigurationContext context) {
    if (!(location instanceof ExternalSystemTaskLocation)) {
      return null;
    }
    
    ExternalSystemTaskLocation taskLocation = (ExternalSystemTaskLocation)location;
    mySourceElement = taskLocation.getPsiElement();

    RunManagerEx runManager = RunManagerEx.getInstanceEx(taskLocation.getProject());
    RunnerAndConfigurationSettings settings = runManager.createConfiguration("", getConfigurationFactory());
    ExternalSystemRunConfiguration configuration = (ExternalSystemRunConfiguration)settings.getConfiguration();
    ExternalSystemTaskExecutionSettings taskExecutionSettings = configuration.getSettings();
    ExternalTaskExecutionInfo task = taskLocation.getTaskInfo();
    taskExecutionSettings.setExternalProjectPath(task.getSettings().getExternalProjectPath());
    taskExecutionSettings.setTaskNames(task.getSettings().getTaskNames());
    configuration.setName(AbstractExternalSystemTaskConfigurationType.generateName(location.getProject(), taskExecutionSettings));
    return settings;
  }

  @Nullable
  @Override
  protected RunnerAndConfigurationSettings findExistingByElement(Location location,
                                                                 @Nonnull List<RunnerAndConfigurationSettings> existingConfigurationsSettings,
                                                                 ConfigurationContext context) {
    if (!(location instanceof ExternalSystemTaskLocation)) {
      return null;
    }
    ExternalTaskExecutionInfo taskInfo = ((ExternalSystemTaskLocation)location).getTaskInfo();

    for (RunnerAndConfigurationSettings settings : existingConfigurationsSettings) {
      RunConfiguration runConfiguration = settings.getConfiguration();
      if (!(runConfiguration instanceof ExternalSystemRunConfiguration)) {
        continue;
      }
      if (match(taskInfo, ((ExternalSystemRunConfiguration)runConfiguration).getSettings())) {
        return settings;
      }
    }
    return null;
  }

  private static boolean match(@Nonnull ExternalTaskExecutionInfo task, @Nonnull ExternalSystemTaskExecutionSettings settings) {
    if (!task.getSettings().getExternalProjectPath().equals(settings.getExternalProjectPath())) {
      return false;
    }
    List<String> taskNames = settings.getTaskNames();
    return task.getSettings().getTaskNames().equals(taskNames);
  }

  @Override
  public int compareTo(@Nonnull Object o) {
    return PREFERED;
  }
}
