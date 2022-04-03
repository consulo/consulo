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
package consulo.execution.runner;

import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.DefaultExecutionTarget;
import consulo.execution.ExecutionTarget;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.RunnerRegistry;
import consulo.execution.configuration.ConfigurationPerRunnerSettings;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.executor.Executor;
import consulo.execution.internal.ExecutionDataContextCacher;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

public class ExecutionEnvironment extends UserDataHolderBase implements Disposable {
  public static final Key<ExecutionEnvironment> KEY = Key.create("executionEnvironment");

  private static final AtomicLong myIdHolder = new AtomicLong(1L);

  @Nonnull
  private final Project myProject;

  @Nonnull
  private RunProfile myRunProfile;
  @Nonnull
  private final Executor myExecutor;
  @Nonnull
  private ExecutionTarget myTarget;

  @Nullable
  private RunnerSettings myRunnerSettings;
  @Nullable
  private ConfigurationPerRunnerSettings myConfigurationSettings;
  @Nullable
  private final RunnerAndConfigurationSettings myRunnerAndConfigurationSettings;
  @Nullable
  private RunContentDescriptor myContentToReuse;
  private final ProgramRunner<?> myRunner;
  private long myExecutionId = 0;
  @Nullable
  private DataContext myDataContext;

  @TestOnly
  public ExecutionEnvironment() {
    myProject = null;
    myContentToReuse = null;
    myRunnerAndConfigurationSettings = null;
    myExecutor = null;
    myRunner = null;
  }

  public ExecutionEnvironment(@Nonnull Executor executor, @Nonnull ProgramRunner runner, @Nonnull RunnerAndConfigurationSettings configuration, @Nonnull Project project) {
    this(configuration.getConfiguration(), executor, DefaultExecutionTarget.INSTANCE, project, configuration.getRunnerSettings(runner), configuration.getConfigurationSettings(runner), null, null,
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
    this(configuration.getConfiguration(), executor, target, project, configuration.getRunnerSettings(runner), configuration.getConfigurationSettings(runner), null, configuration, runner);
  }

  /**
   * @deprecated, use {@link com.intellij.execution.runners.ExecutionEnvironmentBuilder} instead
   * to remove in IDEA 15
   */
  public ExecutionEnvironment(@Nonnull RunProfile runProfile, @Nonnull Executor executor, @Nonnull Project project, @Nullable RunnerSettings runnerSettings) {
    //noinspection ConstantConditions
    this(runProfile, executor, DefaultExecutionTarget.INSTANCE, project, runnerSettings, null, null, null, RunnerRegistry.getInstance().getRunner(executor.getId(), runProfile));
  }

  ExecutionEnvironment(@Nonnull RunProfile runProfile,
                       @Nonnull Executor executor,
                       @Nonnull ExecutionTarget target,
                       @Nonnull Project project,
                       @Nullable RunnerSettings runnerSettings,
                       @Nullable ConfigurationPerRunnerSettings configurationSettings,
                       @Nullable RunContentDescriptor contentToReuse,
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

  @Nullable
  public RunnerAndConfigurationSettings getRunnerAndConfigurationSettings() {
    return myRunnerAndConfigurationSettings;
  }

  @Nullable
  public RunContentDescriptor getContentToReuse() {
    return myContentToReuse;
  }

  public void setContentToReuse(@Nullable RunContentDescriptor contentToReuse) {
    myContentToReuse = contentToReuse;

    if (contentToReuse != null) {
      Disposer.register(contentToReuse, this);
    }
  }

  @Nullable
  @Deprecated
  /**
   * Use {@link #getRunner()} instead
   * to remove in IDEA 15
   */ public String getRunnerId() {
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

  @Nullable
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

  public void setDataContext(@Nonnull DataContext dataContext) {
    myDataContext = ExecutionDataContextCacher.getInstance().getCachedContext(dataContext);
  }

  @Nullable
  public DataContext getDataContext() {
    return myDataContext;
  }
}
