// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class ExecutionTargetManager {
  @Nonnull
  public static ExecutionTargetManager getInstance(@Nonnull Project project) {
    return project.getInstance(ExecutionTargetManager.class);
  }

  @Nonnull
  public static ExecutionTarget getActiveTarget(@Nonnull Project project) {
    return getInstance(project).getActiveTarget();
  }

  public static void setActiveTarget(@Nonnull Project project, @Nonnull ExecutionTarget target) {
    getInstance(project).setActiveTarget(target);
  }

  @Nonnull
  public static List<ExecutionTarget> getTargetsToChooseFor(@Nonnull Project project, @Nullable RunConfiguration configuration) {
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

  public static boolean canRun(@Nonnull ExecutionEnvironment environment) {
    RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
    return settings != null && canRun(settings.getConfiguration(), environment.getExecutionTarget());
  }

  public abstract boolean doCanRun(@Nullable RunConfiguration configuration, @Nonnull ExecutionTarget target);

  public static void update(@Nonnull Project project) {
    getInstance(project).update();
  }

  @Nonnull
  public abstract ExecutionTarget getActiveTarget();

  public abstract void setActiveTarget(@Nonnull ExecutionTarget target);

  @Nonnull
  public abstract List<ExecutionTarget> getTargetsFor(@Nullable RunConfiguration configuration);

  public abstract void update();

  public ExecutionTarget findTarget(RunConfiguration configuration) {
    ExecutionTarget target = getActiveTarget();
    if (canRun(configuration, target)) return target;

    List<ExecutionTarget> targets = getTargetsFor(configuration);
    return ContainerUtil.getFirstItem(targets);
  }
}
