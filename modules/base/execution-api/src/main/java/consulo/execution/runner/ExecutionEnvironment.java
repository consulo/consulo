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
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.concurrent.atomic.AtomicLong;

public class ExecutionEnvironment extends UserDataHolderBase implements Disposable {
    public static final Key<ExecutionEnvironment> KEY = Key.create("executionEnvironment");

    private static final AtomicLong myIdHolder = new AtomicLong(1L);

   
    private final Project myProject;

   
    private RunProfile myRunProfile;
   
    private final Executor myExecutor;
   
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
    private  ProgramRunner.@Nullable Callback myCallback;


    public ExecutionEnvironment(Executor executor, ProgramRunner runner, RunnerAndConfigurationSettings configuration, Project project) {
        this(configuration.getConfiguration(), executor, DefaultExecutionTarget.INSTANCE, project, configuration.getRunnerSettings(runner), configuration.getConfigurationSettings(runner), null, null,
            runner);
    }

    /**
     * @deprecated, use {@link consulo.ide.impl.idea.execution.runners.ExecutionEnvironmentBuilder} instead
     * to remove in IDEA 14
     */
    @TestOnly
    public ExecutionEnvironment(Executor executor,
                                ProgramRunner runner,
                                ExecutionTarget target,
                                RunnerAndConfigurationSettings configuration,
                                Project project) {
        this(configuration.getConfiguration(), executor, target, project, configuration.getRunnerSettings(runner), configuration.getConfigurationSettings(runner), null, configuration, runner);
    }

    /**
     * @deprecated, use {@link consulo.ide.impl.idea.execution.runners.ExecutionEnvironmentBuilder} instead
     * to remove in IDEA 15
     */
    public ExecutionEnvironment(RunProfile runProfile, Executor executor, Project project, @Nullable RunnerSettings runnerSettings) {
        //noinspection ConstantConditions
        this(runProfile, executor, DefaultExecutionTarget.INSTANCE, project, runnerSettings, null, null, null, RunnerRegistry.getInstance().getRunner(executor.getId(), runProfile));
    }

    ExecutionEnvironment(RunProfile runProfile,
                         Executor executor,
                         ExecutionTarget target,
                         Project project,
                         @Nullable RunnerSettings runnerSettings,
                         @Nullable ConfigurationPerRunnerSettings configurationSettings,
                         @Nullable RunContentDescriptor contentToReuse,
                         @Nullable RunnerAndConfigurationSettings settings,
                         ProgramRunner<?> runner) {
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

   
    public Project getProject() {
        return myProject;
    }

   
    public ExecutionTarget getExecutionTarget() {
        return myTarget;
    }

   
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

   
    public Executor getExecutor() {
        return myExecutor;
    }

    public void setCallback(ProgramRunner.@Nullable Callback callback) {
        myCallback = callback;
    }

    public ProgramRunner.@Nullable Callback getCallback() {
        return myCallback;
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

    public void setDataContext(DataContext dataContext) {
        myDataContext = ExecutionDataContextCacher.getInstance().getCachedContext(dataContext);
    }

    @Nullable
    public DataContext getDataContext() {
        return myDataContext;
    }
}
