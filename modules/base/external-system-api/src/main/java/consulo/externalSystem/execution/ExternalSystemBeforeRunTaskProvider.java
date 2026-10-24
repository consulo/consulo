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
import consulo.configurable.ConfigurationException;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.execution.BeforeRunFragment;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.event.ExecutionListener;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.service.execution.ExternalSystemTaskSettingsControl;
import consulo.externalSystem.service.module.extension.ExternalSystemModuleExtension;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author Vladislav.Soroka
 * @since 2014-05-30
 */
public abstract class ExternalSystemBeforeRunTaskProvider<T extends ExternalSystemBeforeRunTask> implements BeforeRunFragment<T> {

    private static final Logger LOG = Logger.getInstance(ExternalSystemBeforeRunTaskProvider.class);

    
    private final ProjectSystemId mySystemId;
    
    private final Project myProject;
    
    private final String myId;

    public ExternalSystemBeforeRunTaskProvider(ProjectSystemId systemId, Project project, String id) {
        mySystemId = systemId;
        myProject = project;
        myId = id;
    }

    @Override
    public String getId() {
        return myId;
    }

    protected ProjectSystemId getSystemId() {
        return mySystemId;
    }

    
    @Override
    public LocalizeValue getName() {
        return ExternalSystemLocalize.tasksBeforeRunEmpty(mySystemId.getDisplayName());
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public RunConfigurationFragmentEditor<T> createEditor(RunConfiguration configuration, Disposable parent) {
        ExternalSystemTaskSettingsControl control = new ExternalSystemTaskSettingsControl(myProject, mySystemId);
        Component component = control.createUIComponent();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addBottom(component);
            }

            @RequiredUIAccess
            @Override
            public void reset(T task) {
                control.setOriginalSettings(task.createTaskExecutionSettings());
                control.reset();
            }

            @RequiredUIAccess
            @Override
            public void apply(T task) throws ConfigurationException {
                ExternalSystemTaskExecutionSettings settings = task.createTaskExecutionSettings();
                control.apply(settings);
                task.applyTaskExecutionSettings(settings);
            }
        };
    }

    @Override
    public boolean canExecuteTask(RunConfiguration configuration, T beforeRunTask) {
        ExternalSystemTaskExecutionSettings executionSettings = beforeRunTask.createTaskExecutionSettings();

        List<ExternalTaskPojo> tasks = new ArrayList<>();
        for (String taskName : executionSettings.getTaskNames()) {
            tasks.add(new ExternalTaskPojo(taskName, executionSettings.getExternalProjectPath(), null));
        }
        if (tasks.isEmpty()) {
            return true;
        }

        Pair<ProgramRunner, ExecutionEnvironment> pair =
            ExternalSystemApiUtil.createRunner(executionSettings, DefaultRunExecutor.EXECUTOR_ID, myProject, mySystemId);

        if (pair == null) {
            return false;
        }

        ProgramRunner runner = pair.first;
        ExecutionEnvironment environment = pair.second;

        return runner.canRun(DefaultRunExecutor.EXECUTOR_ID, environment.getRunProfile());
    }

    
    @Override
    public CompletableFuture<Void> executeAsync(UIAccess uiAccess,
                                                    DataContext context,
                                                    RunConfiguration configuration,
                                                    ExecutionEnvironment env,
                                                    T beforeRunTask) {
        ExternalSystemTaskExecutionSettings executionSettings = beforeRunTask.createTaskExecutionSettings();

        List<ExternalTaskPojo> tasks = new ArrayList<>();
        for (String taskName : executionSettings.getTaskNames()) {
            tasks.add(new ExternalTaskPojo(taskName, executionSettings.getExternalProjectPath(), null));
        }
        if (tasks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Pair<ProgramRunner, ExecutionEnvironment> pair =
            ExternalSystemApiUtil.createRunner(executionSettings, DefaultRunExecutor.EXECUTOR_ID, myProject, mySystemId);

        if (pair == null) {
            return CompletableFuture.failedFuture(new CancellationException());
        }

        ProgramRunner runner = pair.first;
        final ExecutionEnvironment environment = pair.second;
        environment.setExecutionId(env.getExecutionId());

        Disposable disposable = Disposable.newDisposable();

        Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        final String executorId = executor.getId();

        CompletableFuture<Void> result = new CompletableFuture<>();
        result.whenComplete((value, error) -> disposable.disposeWithTree());

        myProject.getMessageBus().connect(disposable).subscribe(ExecutionListener.class, new ExecutionListener() {
            @Override
            public void processStartScheduled(String executorIdLocal, ExecutionEnvironment environmentLocal) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                }
            }

            @Override
            public void processNotStarted(String executorIdLocal, ExecutionEnvironment environmentLocal) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                    result.completeExceptionally(new CancellationException());
                }
            }

            @Override
            public void processStarted(String executorIdLocal,
                                       final ExecutionEnvironment environmentLocal,
                                       ProcessHandler handler) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                    handler.addProcessListener(new ProcessListener() {
                        @Override
                        public void processTerminated(ProcessEvent event) {
                            if (event.getExitCode() == 0) {
                                result.complete(null);
                            }
                            else {
                                result.completeExceptionally(new CancellationException());
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
                result.completeExceptionally(e);
                LOG.error(e);
            }
        });

        return result;
    }

    
    @Override
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public LocalizeValue getDescription(T task) {
        String externalProjectPath = task.createTaskExecutionSettings().getExternalProjectPath();

        if (task.createTaskExecutionSettings().getTaskNames().isEmpty()) {
            return ExternalSystemLocalize.tasksBeforeRunEmpty(mySystemId.getDisplayName());
        }

        String desc = StringUtil.join(task.createTaskExecutionSettings().getTaskNames(), " ");
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

        return ExternalSystemLocalize.tasksBeforeRun(mySystemId.getDisplayName(), desc);
    }
}