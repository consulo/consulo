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
package consulo.externalSystem.execution;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.execution.BeforeRunTaskProvider;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.event.ExecutionListener;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.service.module.extension.ExternalSystemModuleExtension;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.nodep.collection.ContainerUtilRt;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Vladislav.Soroka
 * @since 2014-05-30
 */
public abstract class ExternalSystemBeforeRunTaskProvider extends BeforeRunTaskProvider<ExternalSystemBeforeRunTask> {

    private static final Logger LOG = Logger.getInstance(ExternalSystemBeforeRunTaskProvider.class);

    @Nonnull
    private final ProjectSystemId mySystemId;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final Key<ExternalSystemBeforeRunTask> myId;

    public ExternalSystemBeforeRunTaskProvider(@Nonnull ProjectSystemId systemId,
                                               @Nonnull Project project,
                                               @Nonnull Key<ExternalSystemBeforeRunTask> id) {
        mySystemId = systemId;
        myProject = project;
        myId = id;
    }

    @Override
    @Nonnull
    public Key<ExternalSystemBeforeRunTask> getId() {
        return myId;
    }

    @Nonnull
    @Override
    public String getName() {
        return ExternalSystemLocalize.tasksBeforeRunEmpty(mySystemId.getDisplayName()).get();
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public AsyncResult<Void> configureTask(RunConfiguration runConfiguration, ExternalSystemBeforeRunTask task) {
        ExternalSystemEditTaskDialog dialog = new ExternalSystemEditTaskDialog(myProject, task.getTaskExecutionSettings(), mySystemId);
        dialog.setTitle(ExternalSystemLocalize.tasksSelectTaskTitle(mySystemId.getDisplayName()));
        return dialog.showAsync();
    }

    @Override
    public boolean canExecuteTask(RunConfiguration configuration, ExternalSystemBeforeRunTask beforeRunTask) {
        final ExternalSystemTaskExecutionSettings executionSettings = beforeRunTask.getTaskExecutionSettings();

        final List<ExternalTaskPojo> tasks = ContainerUtilRt.newArrayList();
        for (String taskName : executionSettings.getTaskNames()) {
            tasks.add(new ExternalTaskPojo(taskName, executionSettings.getExternalProjectPath(), null));
        }
        if (tasks.isEmpty()) {
            return true;
        }

        final Pair<ProgramRunner, ExecutionEnvironment> pair =
            ExternalSystemApiUtil.createRunner(executionSettings, DefaultRunExecutor.EXECUTOR_ID, myProject, mySystemId);

        if (pair == null) {
            return false;
        }

        final ProgramRunner runner = pair.first;
        final ExecutionEnvironment environment = pair.second;

        return runner.canRun(DefaultRunExecutor.EXECUTOR_ID, environment.getRunProfile());
    }

    @Nonnull
    @Override
    public AsyncResult<Void> executeTaskAsync(UIAccess uiAccess,
                                              DataContext context,
                                              RunConfiguration configuration,
                                              ExecutionEnvironment env,
                                              ExternalSystemBeforeRunTask beforeRunTask) {
        final ExternalSystemTaskExecutionSettings executionSettings = beforeRunTask.getTaskExecutionSettings();

        final List<ExternalTaskPojo> tasks = ContainerUtilRt.newArrayList();
        for (String taskName : executionSettings.getTaskNames()) {
            tasks.add(new ExternalTaskPojo(taskName, executionSettings.getExternalProjectPath(), null));
        }
        if (tasks.isEmpty()) {
            return AsyncResult.resolved();
        }

        final Pair<ProgramRunner, ExecutionEnvironment> pair =
            ExternalSystemApiUtil.createRunner(executionSettings, DefaultRunExecutor.EXECUTOR_ID, myProject, mySystemId);

        if (pair == null) {
            return AsyncResult.rejected();
        }

        final ProgramRunner runner = pair.first;
        final ExecutionEnvironment environment = pair.second;
        environment.setExecutionId(env.getExecutionId());

        final Disposable disposable = Disposable.newDisposable();

        final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        final String executorId = executor.getId();

        AsyncResult<Void> result = AsyncResult.undefined();
        result.doWhenProcessed(disposable::disposeWithTree);

        myProject.getMessageBus().connect(disposable).subscribe(ExecutionListener.class, new ExecutionListener() {
            @Override
            public void processStartScheduled(@Nonnull final String executorIdLocal, @Nonnull final ExecutionEnvironment environmentLocal) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                }
            }

            @Override
            public void processNotStarted(@Nonnull final String executorIdLocal, @Nonnull final ExecutionEnvironment environmentLocal) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                    result.setRejected();
                }
            }

            @Override
            public void processStarted(@Nonnull final String executorIdLocal,
                                       @Nonnull final ExecutionEnvironment environmentLocal,
                                       @Nonnull final ProcessHandler handler) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                    handler.addProcessListener(new ProcessListener() {
                        @Override
                        public void processTerminated(ProcessEvent event) {
                            if (event.getExitCode() == 0) {
                                result.setDone();
                            }
                            else {
                                result.setRejected();
                            }

                            environmentLocal.getContentToReuse();
                        }
                    });
                }
            }
        });

        uiAccess.give(() -> {
            try {
                runner.execute(environment);
            }
            catch (ExecutionException e) {
                result.rejectWithThrowable(e);
                LOG.error(e);
            }
        });

        return result;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public String getDescription(ExternalSystemBeforeRunTask task) {
        final String externalProjectPath = task.getTaskExecutionSettings().getExternalProjectPath();

        if (task.getTaskExecutionSettings().getTaskNames().isEmpty()) {
            return ExternalSystemLocalize.tasksBeforeRunEmpty(mySystemId.getDisplayName()).get();
        }

        String desc = StringUtil.join(task.getTaskExecutionSettings().getTaskNames(), " ");
        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            ExternalSystemModuleExtension extension = module.getExtension(ExternalSystemModuleExtension.class);
            if (extension == null) {
                continue;
            }

            if (!mySystemId.toString().equals(extension.getOption(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY))) {
                continue;
            }

            if (StringUtil.equals(externalProjectPath, extension.getOption(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY))) {
                desc = module.getName() + ": " + desc;
                break;
            }
        }

        return ExternalSystemLocalize.tasksBeforeRun(mySystemId.getDisplayName(), desc).get();
    }
}