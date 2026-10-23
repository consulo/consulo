package consulo.execution.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.execution.BeforeRunFragment;
import consulo.execution.ExecutionTargetManager;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.ListBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author Vassiliy Kudryashov
 */
@ExtensionImpl
public class RunConfigurationBeforeRunFragment implements BeforeRunFragment<RunConfigurableBeforeRunTask> {
    private static final Logger LOG = Logger.getInstance(RunConfigurationBeforeRunFragment.class);

    private final Project myProject;

    @Inject
    public RunConfigurationBeforeRunFragment(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return "RunConfigurationTask";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.beforeLaunchRunAnotherConfiguration();
    }

    @Override
    public Image getIcon(RunConfiguration configuration) {
        return PlatformIconGroup.actionsExecute();
    }

    @Override
    public @Nullable Image getTaskIcon(RunConfiguration configuration, RunConfigurableBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = findSettings(myProject, task);
        return settings == null ? null : ProgramRunnerUtil.getConfigurationIcon(settings, false);
    }

    @Override
    public LocalizeValue getDescription(RunConfigurableBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = findSettings(myProject, task);
        return settings == null
            ? ExecutionLocalize.beforeLaunchRunAnotherConfiguration()
            : ExecutionLocalize.beforeLaunchRunCertainConfiguration(settings.getName());
    }

    @Override
    public Class<RunConfigurableBeforeRunTask> getStateClass() {
        return RunConfigurableBeforeRunTask.class;
    }

    @Override
    public @Nullable RunConfigurableBeforeRunTask createTask(RunConfiguration configuration) {
        if (configuration.getProject().isInitialized() && getAvailableConfigurations(configuration).isEmpty()) {
            return null;
        }
        return new RunConfigurableBeforeRunTask();
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void readLegacy(Element element, RunConfigurableBeforeRunTask task) {
        BeforeRunFragment.super.readLegacy(element, task);
        task.setConfigurationName(element.getAttributeValue("run_configuration_name"));
        task.setConfigurationType(element.getAttributeValue("run_configuration_type"));
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfigurableBeforeRunTask> createEditor(RunConfiguration configuration, Disposable parent) {
        List<RunnerAndConfigurationSettings> available = getAvailableConfigurations(configuration);
        ListBox<RunnerAndConfigurationSettings> list = ListBox.create(available);
        list.setRenderer((presentation, item) -> {
            RunnerAndConfigurationSettings settings = item.getValue();
            if (settings != null) {
                presentation.withIcon(RunManagerImpl.getInstanceImpl(myProject).getConfigurationIcon(settings));
                presentation.append(settings.getName());
            }
        });

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addBottom(list);
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfigurableBeforeRunTask task) {
                for (int i = 0; i < available.size(); i++) {
                    if (task.isFor(available.get(i))) {
                        list.setValueByIndex(i);
                        return;
                    }
                }
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfigurableBeforeRunTask task) {
                task.setSettings(list.getValue());
            }
        };
    }

    private List<RunnerAndConfigurationSettings> getAvailableConfigurations(RunConfiguration configuration) {
        Project project = configuration.getProject();
        if (!project.isInitialized()) {
            return Collections.emptyList();
        }

        String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
        List<RunnerAndConfigurationSettings> result = new ArrayList<>();
        for (RunnerAndConfigurationSettings settings : RunManagerImpl.getInstanceImpl(project).getSortedConfigurations()) {
            if (settings.getConfiguration() != configuration && ProgramRunnerUtil.getRunner(executorId, settings) != null) {
                result.add(settings);
            }
        }
        return result;
    }

    public static @Nullable RunnerAndConfigurationSettings findSettings(Project project, RunConfigurableBeforeRunTask task) {
        if (StringUtil.isEmpty(task.getConfigurationName()) || StringUtil.isEmpty(task.getConfigurationType())) {
            return null;
        }

        for (RunnerAndConfigurationSettings settings : RunManagerImpl.getInstanceImpl(project).getSortedConfigurations()) {
            if (task.isFor(settings)) {
                return settings;
            }
        }
        return null;
    }

    @Override
    public boolean canExecuteTask(RunConfiguration configuration, RunConfigurableBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = findSettings(myProject, task);
        if (settings == null) {
            return false;
        }
        String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
        ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
        return runner != null && runner.canRun(executorId, settings.getConfiguration());
    }

    @Override
    public CompletableFuture<?> executeAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment env,
        RunConfigurableBeforeRunTask task
    ) {
        RunnerAndConfigurationSettings settings = findSettings(myProject, task);
        if (settings == null) {
            return CompletableFuture.failedFuture(new CancellationException());
        }
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        String executorId = executor.getId();
        ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
        if (runner == null) {
            return CompletableFuture.failedFuture(new CancellationException());
        }
        ExecutionEnvironment environment = new ExecutionEnvironment(executor, runner, settings, myProject);
        environment.setExecutionId(env.getExecutionId());
        if (!ExecutionTargetManager.canRun(settings, env.getExecutionTarget()) || !runner.canRun(executorId, environment.getRunProfile())) {
            return CompletableFuture.failedFuture(new CancellationException());
        }

        CompletableFuture<Void> result = new CompletableFuture<>();
        uiAccess.giveAsync(() -> {
            try {
                environment.setCallback(descriptor -> {
                    ProcessHandler processHandler = descriptor != null ? descriptor.getProcessHandler() : null;
                    if (processHandler != null) {
                        processHandler.addProcessListener(new ProcessListener() {
                            @Override
                            public void processTerminated(ProcessEvent event) {
                                if (event.getExitCode() == 0) {
                                    result.complete(null);
                                }
                                else {
                                    result.completeExceptionally(new CancellationException());
                                }
                            }
                        });
                    }
                });
                runner.execute(environment);
            }
            catch (ExecutionException e) {
                result.completeExceptionally(e);
                LOG.error(e);
            }
        }).exceptionally(error -> {
            result.completeExceptionally(error);
            return null;
        });
        return result;
    }
}
