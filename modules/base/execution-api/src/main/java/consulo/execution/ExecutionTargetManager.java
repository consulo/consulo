// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;

import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.List;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class ExecutionTargetManager {
  
  public static ExecutionTargetManager getInstance(Project project) {
    return project.getInstance(ExecutionTargetManager.class);
  }

  
  public static ExecutionTarget getActiveTarget(Project project) {
    return getInstance(project).getActiveTarget();
  }

  public static void setActiveTarget(Project project, ExecutionTarget target) {
    getInstance(project).setActiveTarget(target);
  }

  
  public static List<ExecutionTarget> getTargetsToChooseFor(Project project, @Nullable RunConfiguration configuration) {
    List<ExecutionTarget> result = getInstance(project).getTargetsFor(configuration);
    if (result.size() == 1 && DefaultExecutionTarget.INSTANCE.equals(result.get(0))) return Collections.emptyList();
    result = Collections.unmodifiableList(ContainerUtil.filter(result, target -> !target.isExternallyManaged()));
    if (result.size() == 1 && DefaultExecutionTarget.INSTANCE.equals(result.get(0))) {
      return Collections.emptyList();
    }
    return result;
  }

  /**
   * @deprecated use {@link #canRun(RunConfiguration, ExecutionTarget)} instead
   */
  @Deprecated(forRemoval = true)
  public static boolean canRun(@Nullable RunnerAndConfigurationSettings settings, @Nullable ExecutionTarget target) {
    return canRun(settings != null ? settings.getConfiguration() : null, target);
  }

  public static boolean canRun(@Nullable RunConfiguration configuration, @Nullable ExecutionTarget target) {
    if (configuration == null || target == null) {
      return false;
    }
    else {
      return getInstance(configuration.getProject()).doCanRun(configuration, target);
    }
  }

  public static boolean canRun(ExecutionEnvironment environment) {
    RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
    return settings != null && canRun(settings.getConfiguration(), environment.getExecutionTarget());
  }

  public abstract boolean doCanRun(@Nullable RunConfiguration configuration, ExecutionTarget target);

  public static void update(Project project) {
    getInstance(project).update();
  }

  
  public abstract ExecutionTarget getActiveTarget();

  public abstract void setActiveTarget(ExecutionTarget target);

  
  public abstract List<ExecutionTarget> getTargetsFor(@Nullable RunConfiguration configuration);

  public abstract void update();

  public ExecutionTarget findTarget(RunConfiguration configuration) {
    ExecutionTarget target = getActiveTarget();
    if (canRun(configuration, target)) return target;

    List<ExecutionTarget> targets = getTargetsFor(configuration);
    return ContainerUtil.getFirstItem(targets);
  }
}
