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
package consulo.execution;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunnerRegistry;
import consulo.externalService.statistic.ConvertUsagesUtil;
import consulo.externalService.statistic.UsageTrigger;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ExecutionException;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProgramRunnerUtil {
  private static final Logger LOG = Logger.getInstance(ProgramRunnerUtil.class);

  private ProgramRunnerUtil() {
  }

  @Nullable
  public static ProgramRunner getRunner(@Nonnull String executorId, RunnerAndConfigurationSettings configuration) {
    return configuration == null ? null : RunnerRegistry.getInstance().getRunner(executorId, configuration.getConfiguration());
  }

  public static void executeConfiguration(@Nonnull ExecutionEnvironment environment, boolean showSettings, boolean assignNewId) {
    if (ExecutorRegistry.getInstance().isStarting(environment)) {
      return;
    }

    RunnerAndConfigurationSettings runnerAndConfigurationSettings = environment.getRunnerAndConfigurationSettings();
    if (runnerAndConfigurationSettings != null) {
      if (!ExecutionTargetManager.canRun(environment)) {
        ExecutionUtil.handleExecutionError(environment, new ExecutionException(
                StringUtil.escapeXml("Cannot run '" + environment.getRunProfile().getName() + "' on '" + environment.getExecutionTarget().getDisplayName() + "'")));
        return;
      }

      RunManager runManager = RunManager.getInstance(environment.getProject());
      RunConfigurationEditor runConfigurationEditor = RunConfigurationEditor.getInstance(environment.getProject());

      if (!runManager.canRunConfiguration(environment) || (showSettings && runnerAndConfigurationSettings.isEditBeforeRun())) {
        if (!runConfigurationEditor.editConfiguration(environment, "Edit configuration")) {
          return;
        }

        while (!runManager.canRunConfiguration(environment)) {
          if (Messages.YES ==
              Messages.showYesNoDialog(environment.getProject(), "Configuration is still incorrect. Do you want to edit it again?", "Change Configuration Settings", "Edit", "Continue Anyway",
                                       Messages.getErrorIcon())) {
            if (!runConfigurationEditor.editConfiguration(environment, "Edit configuration")) {
              return;
            }
          }
          else {
            break;
          }
        }
      }

      ConfigurationType configurationType = runnerAndConfigurationSettings.getType();
      if (configurationType != null) {
        UsageTrigger.trigger("execute." + ConvertUsagesUtil.ensureProperKey(configurationType.getId()) + "." + environment.getExecutor().getId());
      }
    }

    try {
      if (assignNewId) {
        environment.assignNewExecutionId();
      }
      environment.getRunner().execute(environment);
    }
    catch (ExecutionException e) {
      String name = runnerAndConfigurationSettings != null ? runnerAndConfigurationSettings.getName() : null;
      if (name == null) {
        name = environment.getRunProfile().getName();
      }
      if (name == null && environment.getContentToReuse() != null) {
        name = environment.getContentToReuse().getDisplayName();
      }
      if (name == null) {
        name = "<Unknown>";
      }
      ExecutionUtil.handleExecutionError(environment.getProject(), environment.getExecutor().getToolWindowId(), name, e);
    }
  }

  public static void executeConfiguration(@Nonnull RunnerAndConfigurationSettings configuration,
                                          @Nonnull Executor executor) {
    ExecutionEnvironmentBuilder builder;
    try {
      builder = ExecutionEnvironmentBuilder.create(executor, configuration);
    }
    catch (ExecutionException e) {
      LOG.error(e);
      return;
    }

    executeConfiguration(builder.contentToReuse(null).dataContext(null).activeTarget().build(), true, true);
  }

  @Nonnull
  public static Image getConfigurationIcon(RunnerAndConfigurationSettings settings, boolean invalid) {
    Image icon = getPrimaryIcon(settings);

    if (invalid) {
      return ImageEffects.layered(icon, PlatformIconGroup.runconfigurationsInvalidconfigurationlayer());
    }

    return icon;
  }

  @Nonnull
  public static Image getPrimaryIcon(RunnerAndConfigurationSettings settings) {
    RunConfiguration configuration = settings.getConfiguration();
    ConfigurationFactory factory = settings.getFactory();
    Image icon = factory != null ? factory.getIcon(configuration) : null;
    if (icon == null) {
      icon = PlatformIconGroup.actionsHelp();
    }

    if (settings.isTemporary()) {
      icon = ImageEffects.transparent(icon, 0.3f);
    }

    return icon;
  }

  public static String shortenName(String name, int toBeAdded) {
    if (name == null) return "";
    int symbols = Math.max(10, 20 - toBeAdded);
    if (name.length() < symbols) {
      return name;
    }
    else {
      return name.substring(0, symbols) + "...";
    }
  }
}
