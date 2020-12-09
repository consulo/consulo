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
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ExecutionEnvironment extends UserDataHolderBase implements Disposable {
  private static final AtomicLong myIdHolder = new AtomicLong(1L);

  @Nonnull
  private final Project myProject;

  @Nonnull
  private RunProfile myRunProfile;
  @Nonnull
  private final Executor myExecutor;
  @Nonnull
  private ExecutionTarget myTarget;

  @Nullable private RunnerSettings myRunnerSettings;
  @Nullable private ConfigurationPerRunnerSettings myConfigurationSettings;
  @Nullable private final RunnerAndConfigurationSettings myRunnerAndConfigurationSettings;
  @javax.annotation.Nullable
  private RunContentDescriptor myContentToReuse;
  private final ProgramRunner<?> myRunner;
  private long myExecutionId = 0;
  @Nullable private DataContext myDataContext;

  @TestOnly
  public ExecutionEnvironment() {
    myProject = null;
    myContentToReuse = null;
    myRunnerAndConfigurationSettings = null;
    myExecutor = null;
    myRunner = null;
  }

  public ExecutionEnvironment(@Nonnull Executor executor,
                              @Nonnull ProgramRunner runner,
                              @Nonnull RunnerAndConfigurationSettings configuration,
                              @Nonnull Project project) {
    this(configuration.getConfiguration(),
         executor,
         DefaultExecutionTarget.INSTANCE,
         project,
         configuration.getRunnerSettings(runner),
         configuration.getConfigurationSettings(runner),
         null,
         null,
         runner);
  }

  /**
   * @deprecated, use {@link com.intellij.execution.runners.ExecutionEnvironmentBuilder} instead
   * to remove in IDEA 14
   */
  @TestOnly
  public ExecutionEnvironment(@Nonnull Executor executor,
                              @Nonnull final ProgramRunner runner,
                              @Nonnull final ExecutionTarget target,
                              @Nonnull final RunnerAndConfigurationSettings configuration,
                              @Nonnull Project project) {
    this(configuration.getConfiguration(),
         executor,
         target,
         project,
         configuration.getRunnerSettings(runner),
         configuration.getConfigurationSettings(runner),
         null,
         configuration,
         runner);
  }

  /**
   * @deprecated, use {@link com.intellij.execution.runners.ExecutionEnvironmentBuilder} instead
   * to remove in IDEA 15
   */
  public ExecutionEnvironment(@Nonnull RunProfile runProfile,
                              @Nonnull Executor executor,
                              @Nonnull Project project,
                              @Nullable RunnerSettings runnerSettings) {
    //noinspection ConstantConditions
    this(runProfile, executor, DefaultExecutionTarget.INSTANCE, project, runnerSettings, null, null, null, RunnerRegistry.getInstance().getRunner(executor.getId(), runProfile));
  }

  ExecutionEnvironment(@Nonnull RunProfile runProfile,
                       @Nonnull Executor executor,
                       @Nonnull ExecutionTarget target,
                       @Nonnull Project project,
                       @Nullable RunnerSettings runnerSettings,
                       @Nullable ConfigurationPerRunnerSettings configurationSettings,
                       @javax.annotation.Nullable RunContentDescriptor contentToReuse,
                       @Nullable RunnerAndConfigurationSettings settings,
                       @Nonnull ProgramRunner<?> runner) {
    myExecutor = executor;
    myTarget = target;
    myRunProfile = runProfile;
    myRunnerSettings = runnerSettings;
    myConfigurationSettings = configurationSettings;
    myProject = project;
    setContentToReuse(contentToReuse);
    myRunnerAndConfigurationSettings = settings;

    myRunner = runner;
  }

  @Override
  public void dispose() {
    myContentToReuse = null;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public ExecutionTarget getExecutionTarget() {
    return myTarget;
  }

  @Nonnull
  public RunProfile getRunProfile() {
    return myRunProfile;
  }

  @javax.annotation.Nullable
  public RunnerAndConfigurationSettings getRunnerAndConfigurationSettings() {
    return myRunnerAndConfigurationSettings;
  }

  @javax.annotation.Nullable
  public RunContentDescriptor getContentToReuse() {
    return myContentToReuse;
  }

  public void setContentToReuse(@Nullable RunContentDescriptor contentToReuse) {
    myContentToReuse = contentToReuse;

    if (contentToReuse != null) {
      Disposer.register(contentToReuse, this);
    }
  }

  @javax.annotation.Nullable
  @Deprecated
  /**
   * Use {@link #getRunner()} instead
   * to remove in IDEA 15
   */
  public String getRunnerId() {
    return myRunner.getRunnerId();
  }

  @Nonnull
  public ProgramRunner<?> getRunner() {
    return myRunner;
  }

  @Nullable
  public RunnerSettings getRunnerSettings() {
    return myRunnerSettings;
  }

  @javax.annotation.Nullable
  public ConfigurationPerRunnerSettings getConfigurationSettings() {
    return myConfigurationSettings;
  }

  @Nullable
  public RunProfileState getState() throws ExecutionException {
    return myRunProfile.getState(myExecutor, this);
  }

  public long assignNewExecutionId() {
    myExecutionId = myIdHolder.incrementAndGet();
    return myExecutionId;
  }

  public void setExecutionId(long executionId) {
    myExecutionId = executionId;
  }

  public long getExecutionId() {
    return myExecutionId;
  }

  @Nonnull
  public Executor getExecutor() {
    return myExecutor;
  }

  @Override
  public String toString() {
    if (myRunnerAndConfigurationSettings != null) {
      return myRunnerAndConfigurationSettings.getName();
    }
    else if (myRunProfile != null) {
      return myRunProfile.getName();
    }
    else if (myContentToReuse != null) {
      return myContentToReuse.getDisplayName();
    }
    return super.toString();
  }

  void setDataContext(@Nonnull DataContext dataContext) {
    myDataContext = CachingDataContext.cacheIfNeed(dataContext);
  }

  @Nullable
  public DataContext getDataContext() {
    return myDataContext;
  }

  private static class CachingDataContext implements DataContext {
    private static final Key[] keys = {CommonDataKeys.PROJECT, PlatformDataKeys.PROJECT_FILE_DIRECTORY, CommonDataKeys.EDITOR, CommonDataKeys.VIRTUAL_FILE, CommonDataKeys.MODULE, CommonDataKeys.PSI_FILE};
    private final Map<Key, Object> values = new HashMap<>();

    @Nonnull
    static CachingDataContext cacheIfNeed(@Nonnull DataContext context) {
      if (context instanceof CachingDataContext)
        return (CachingDataContext)context;
      return new CachingDataContext(context);
    }

    private CachingDataContext(DataContext context) {
      for (Key key : keys) {
        values.put(key, context.getData(key));
      }
    }

    @Override
    @SuppressWarnings("unchekced")
    public <T> T getData(@NonNls Key<T> dataId) {
      return (T)values.get(dataId);
    }
  }
}
