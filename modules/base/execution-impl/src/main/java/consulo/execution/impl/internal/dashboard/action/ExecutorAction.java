// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.dashboard.action;

import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressManager;
import consulo.dataContext.DataContext;
import consulo.execution.*;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RuntimeConfigurationError;
import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.compound.CompoundRunConfiguration;
import consulo.execution.impl.internal.ui.RunContentManagerImpl;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ProcessHandler;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.content.Content;
import consulo.ui.image.Image;
import consulo.util.collection.JBIterable;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static consulo.execution.impl.internal.dashboard.action.RunDashboardActionUtils.getLeafTargets;

/**
 * @author konstantin.aleev
 */
public abstract class ExecutorAction extends DumbAwareAction {
  private static final Key<List<RunDashboardRunConfigurationNode>> RUNNABLE_LEAVES_KEY =
    Key.create("RUNNABLE_LEAVES_KEY");

  protected ExecutorAction() {
  }

  protected ExecutorAction(String text, String description, Image icon) {
    super(text, description, icon);
  }

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      update(e, false);
      return;
    }
    JBIterable<RunDashboardRunConfigurationNode> targetNodes = getLeafTargets(e);
    boolean running = targetNodes.filter(node -> {
      Content content = node.getContent();
      return content != null && !RunContentManagerImpl.isTerminated(content);
    }).isNotEmpty();
    update(e, running);
    List<RunDashboardRunConfigurationNode> runnableLeaves = targetNodes.filter(this::canRun).toList();
    Presentation presentation = e.getPresentation();
    if (!runnableLeaves.isEmpty()) {
      presentation.putClientProperty(RUNNABLE_LEAVES_KEY, runnableLeaves);
    }
    presentation.setEnabled(!runnableLeaves.isEmpty());
    presentation.setVisible(targetNodes.isNotEmpty());
  }

  private boolean canRun(@Nonnull RunDashboardRunConfigurationNode node) {
    ProgressManager.checkCanceled();

    Project project = node.getProject();
    return canRun(node.getConfigurationSettings(),
                  null,
                  DumbService.isDumb(project));
  }

  private boolean canRun(RunnerAndConfigurationSettings settings, ExecutionTarget target, boolean isDumb) {
    if (isDumb && !settings.getType().isDumbAware()) return false;

    String executorId = getExecutor().getId();
    RunConfiguration configuration = settings.getConfiguration();
    Project project = configuration.getProject();
    if (configuration instanceof CompoundRunConfiguration) {
      if (ExecutionTargetManager.getInstance(project).getTargetsFor(configuration).isEmpty()) return false;
      RunManager runManager = RunManager.getInstance(project);

      Map<RunConfiguration, ExecutionTarget> subConfigurations =
        ((CompoundRunConfiguration)configuration).getConfigurationsWithTargets(runManager);

      if (subConfigurations.isEmpty()) return false;

      for (Map.Entry<RunConfiguration, ExecutionTarget> subConfiguration : subConfigurations.entrySet()) {
        RunnerAndConfigurationSettings subSettings = runManager.findSettings(subConfiguration.getKey());
        if (subSettings == null || !canRun(subSettings, subConfiguration.getValue(), isDumb)) {
          return false;
        }
      }
      return true;
    }

    if (!isValid(settings)) return false;

    ProgramRunner<?> runner = ProgramRunner.getRunner(executorId, configuration);
    if (runner == null) return false;

    if (target == null) {
      target = ExecutionTargetManager.getInstance(project).findTarget(configuration);
      if (target == null) return false;
    }
    else if (!ExecutionTargetManager.canRun(configuration, target)) {
      return false;
    }
    return !ExecutionManager.getInstance(project).isStarting(executorId, runner.getRunnerId());
  }

  private static boolean isValid(RunnerAndConfigurationSettings settings) {
    try {
      settings.checkSettings(null);
      return true;
    }
    catch (RuntimeConfigurationError ex) {
      return false;
    }
    catch (IndexNotReadyException | RuntimeConfigurationException ex) {
      return true;
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) return;

    List<RunDashboardRunConfigurationNode> runnableLeaves = e.getPresentation().getClientProperty(RUNNABLE_LEAVES_KEY);
    if (runnableLeaves == null) return;

    for (RunDashboardRunConfigurationNode node : runnableLeaves) {
      run(node.getConfigurationSettings(), node.getDescriptor(), e.getDataContext());
    }
  }

  private void run(RunnerAndConfigurationSettings settings, RunContentDescriptor descriptor, @Nonnull DataContext context) {
    runSubProcess(settings, null, descriptor, environment -> {
      environment.setDataContext(context);
    });
  }

  private void runSubProcess(RunnerAndConfigurationSettings settings,
                             ExecutionTarget target,
                             RunContentDescriptor descriptor,
                             @Nonnull Consumer<ExecutionEnvironment> envCustomization) {
    RunConfiguration configuration = settings.getConfiguration();
    Project project = configuration.getProject();
    RunManager runManager = RunManager.getInstance(project);
    if (configuration instanceof CompoundRunConfiguration) {
      Map<RunConfiguration, ExecutionTarget> subConfigurations =
        ((CompoundRunConfiguration)configuration).getConfigurationsWithTargets(runManager);
      for (Map.Entry<RunConfiguration, ExecutionTarget> subConfiguration : subConfigurations.entrySet()) {
        RunnerAndConfigurationSettings subSettings = runManager.findSettings(subConfiguration.getKey());
        if (subSettings != null) {
          runSubProcess(subSettings, subConfiguration.getValue(), null, envCustomization);
        }
      }
    }
    else {
      if (target == null) {
        target = ExecutionTargetManager.getInstance(project).findTarget(configuration);
        assert target != null : "No target for configuration of type " + configuration.getType().getDisplayName();
      }
      ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();

      ExecutionManager.getInstance(project)
                      .restartRunProfile(project,
                                         getExecutor(),
                                         target,
                                         settings,
                                         processHandler,
                                         envCustomization);
    }
  }

  protected abstract Executor getExecutor();

  protected abstract void update(@Nonnull AnActionEvent e, boolean running);
}
