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
package com.intellij.execution.runners;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * User: Vassiliy.Kudryashov
 */
public final class ExecutionEnvironmentBuilder {
  @Nonnull
  private RunProfile myRunProfile;
  @Nonnull
  private ExecutionTarget myTarget = DefaultExecutionTarget.INSTANCE;

  @Nonnull
  private final Project myProject;

  @javax.annotation.Nullable
  private RunnerSettings myRunnerSettings;
  @javax.annotation.Nullable
  private ConfigurationPerRunnerSettings myConfigurationSettings;
  @Nullable private RunContentDescriptor myContentToReuse;
  @Nullable private RunnerAndConfigurationSettings myRunnerAndConfigurationSettings;
  @javax.annotation.Nullable
  private String myRunnerId;
  private ProgramRunner<?> myRunner;
  private boolean myAssignNewId;
  @Nonnull
  private Executor myExecutor;
  @javax.annotation.Nullable
  private DataContext myDataContext;

  public ExecutionEnvironmentBuilder(@Nonnull Project project, @Nonnull Executor executor) {
    myProject = project;
    myExecutor = executor;
  }

  @Nonnull
  public static ExecutionEnvironmentBuilder create(@Nonnull Project project, @Nonnull Executor executor, @Nonnull RunProfile runProfile) throws ExecutionException {
    ExecutionEnvironmentBuilder builder = createOrNull(project, executor, runProfile);
    if (builder == null) {
      throw new ExecutionException("Cannot find runner for " + runProfile.getName());
    }
    return builder;
  }

  @javax.annotation.Nullable
  public static ExecutionEnvironmentBuilder createOrNull(@Nonnull Project project, @Nonnull Executor executor, @Nonnull RunProfile runProfile) {
    ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), runProfile);
    if (runner == null) {
      return null;
    }
    return new ExecutionEnvironmentBuilder(project, executor).runner(runner).runProfile(runProfile);
  }

  @Nullable
  public static ExecutionEnvironmentBuilder createOrNull(@Nonnull Executor executor, @Nonnull RunnerAndConfigurationSettings settings) {
    ExecutionEnvironmentBuilder builder = createOrNull(settings.getConfiguration().getProject(), executor, settings.getConfiguration());
    return builder == null ? null : builder.runnerAndSettings(builder.myRunner, settings);
  }

  @Nonnull
  public static ExecutionEnvironmentBuilder create(@Nonnull Executor executor, @Nonnull RunnerAndConfigurationSettings settings) throws ExecutionException {
    ExecutionEnvironmentBuilder builder = create(settings.getConfiguration().getProject(), executor, settings.getConfiguration());
    return builder.runnerAndSettings(builder.myRunner, settings);
  }

  @Nonnull
  public static ExecutionEnvironmentBuilder create(@Nonnull Executor executor, @Nonnull RunConfiguration configuration) {
    return new ExecutionEnvironmentBuilder(configuration.getProject(), executor).runProfile(configuration);
  }

  @Nonnull
  Executor getExecutor() {
    return myExecutor;
  }

  /**
   * Creates an execution environment builder initialized with a copy of the specified environment.
   *
   * @param copySource the environment to copy from.
   */
  public ExecutionEnvironmentBuilder(@Nonnull ExecutionEnvironment copySource) {
    myTarget = copySource.getExecutionTarget();
    myProject = copySource.getProject();
    myRunnerAndConfigurationSettings = copySource.getRunnerAndConfigurationSettings();
    myRunProfile = copySource.getRunProfile();
    myRunnerSettings = copySource.getRunnerSettings();
    myConfigurationSettings = copySource.getConfigurationSettings();
    //noinspection deprecation
    myRunner = copySource.getRunner();
    myContentToReuse = copySource.getContentToReuse();
    myExecutor = copySource.getExecutor();
  }

  @SuppressWarnings("UnusedDeclaration")
  @Deprecated
  /**
   * to remove in IDEA 15
   */
  public ExecutionEnvironmentBuilder setTarget(@Nonnull ExecutionTarget target) {
    return target(target);
  }

  public ExecutionEnvironmentBuilder target(@Nullable ExecutionTarget target) {
    if (target != null) {
      myTarget = target;
    }
    return this;
  }

  public ExecutionEnvironmentBuilder activeTarget() {
    myTarget = ExecutionTargetManager.getActiveTarget(myProject);
    return this;
  }

  @SuppressWarnings("UnusedDeclaration")
  @Deprecated
  /**
   * to remove in IDEA 15
   */
  public ExecutionEnvironmentBuilder setRunnerAndSettings(@Nonnull ProgramRunner programRunner,
                                                          @Nonnull RunnerAndConfigurationSettings settings) {
    return runnerAndSettings(programRunner, settings);
  }

  public ExecutionEnvironmentBuilder runnerAndSettings(@Nonnull ProgramRunner runner,
                                                       @Nonnull RunnerAndConfigurationSettings settings) {
    myRunnerAndConfigurationSettings = settings;
    myRunProfile = settings.getConfiguration();
    myRunnerSettings = settings.getRunnerSettings(runner);
    myConfigurationSettings = settings.getConfigurationSettings(runner);
    myRunner = runner;
    return this;
  }

  @Nonnull
  public ExecutionEnvironmentBuilder runnerSettings(@javax.annotation.Nullable RunnerSettings runnerSettings) {
    myRunnerSettings = runnerSettings;
    return this;
  }

  @Nonnull
  public ExecutionEnvironmentBuilder contentToReuse(@Nullable RunContentDescriptor contentToReuse) {
    myContentToReuse = contentToReuse;
    return this;
  }

  @Nonnull
  public ExecutionEnvironmentBuilder runProfile(@Nonnull RunProfile runProfile) {
    myRunProfile = runProfile;
    return this;
  }

  @Nonnull
  public ExecutionEnvironmentBuilder runner(@Nonnull ProgramRunner<?> runner) {
    myRunner = runner;
    return this;
  }

  public ExecutionEnvironmentBuilder assignNewId() {
    myAssignNewId = true;
    return this;
  }

  @Nonnull
  public ExecutionEnvironmentBuilder dataContext(@Nullable DataContext dataContext) {
    myDataContext = dataContext;
    return this;
  }

  @Nonnull
  public ExecutionEnvironmentBuilder executor(@Nonnull Executor executor) {
    myExecutor = executor;
    return this;
  }

  @Nonnull
  public ExecutionEnvironment build() {
    if (myRunner == null) {
      if (myRunnerId == null) {
        myRunner = RunnerRegistry.getInstance().getRunner(myExecutor.getId(), myRunProfile);
      }
      else {
        myRunner = RunnerRegistry.getInstance().findRunnerById(myRunnerId);
      }
    }

    if (myRunner == null) {
      throw new IllegalStateException("Runner must be specified");
    }

    ExecutionEnvironment environment = new ExecutionEnvironment(myRunProfile, myExecutor, myTarget, myProject, myRunnerSettings, myConfigurationSettings, myContentToReuse,
                                                                myRunnerAndConfigurationSettings, myRunner);
    if (myAssignNewId) {
      environment.assignNewExecutionId();
    }
    if (myDataContext != null) {
      environment.setDataContext(myDataContext);
    }
    return environment;
  }

  public void buildAndExecute() throws ExecutionException {
    ExecutionEnvironment environment = build();
    myRunner.execute(environment);
  }
}
