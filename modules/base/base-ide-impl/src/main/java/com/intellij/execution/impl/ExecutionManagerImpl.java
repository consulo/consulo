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
package com.intellij.execution.impl;

import com.intellij.CommonBundle;
import com.intellij.execution.*;
import com.intellij.execution.configuration.CompatibilityAwareRunProfile;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.execution.ui.RunContentManagerImpl;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.docking.DockManager;
import com.intellij.util.Alarm;
import com.intellij.util.ObjectUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Singleton
public class ExecutionManagerImpl extends ExecutionManager implements Disposable {
  public static final Key<Object> EXECUTION_SESSION_ID_KEY = Key.create("EXECUTION_SESSION_ID_KEY");

  private static final Logger LOG = Logger.getInstance(ExecutionManagerImpl.class);
  private static final ProcessHandler[] EMPTY_PROCESS_HANDLERS = new ProcessHandler[0];

  private final Application myApplication;
  private final Project myProject;
  @Nonnull
  private final Provider<ToolWindowManager> myToolWindowManager;

  private RunContentManagerImpl myContentManager;
  private final Alarm awaitingTerminationAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private final List<Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor>> myRunningConfigurations = ContainerUtil.createLockFreeCopyOnWriteList();

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Nonnull
  public static ExecutionManagerImpl getInstance(@Nonnull Project project) {
    return (ExecutionManagerImpl)ServiceManager.getService(project, ExecutionManager.class);
  }

  @Inject
  ExecutionManagerImpl(@Nonnull Application application, @Nonnull Project project, @Nonnull Provider<ToolWindowManager> toolWindowManager) {
    myApplication = application;
    myProject = project;
    myToolWindowManager = toolWindowManager;
  }

  public static void stopProcess(@Nullable RunContentDescriptor descriptor) {
    stopProcess(descriptor != null ? descriptor.getProcessHandler() : null);
  }

  public static void stopProcess(@Nullable ProcessHandler processHandler) {
    if (processHandler == null) {
      return;
    }

    processHandler.putUserData(ProcessHandler.TERMINATION_REQUESTED, Boolean.TRUE);

    if (processHandler instanceof KillableProcess && processHandler.isProcessTerminating()) {
      // process termination was requested, but it's still alive
      // in this case 'force quit' will be performed
      ((KillableProcess)processHandler).killProcess();
      return;
    }

    if (!processHandler.isProcessTerminated()) {
      if (processHandler.detachIsDefault()) {
        processHandler.detachProcess();
      }
      else {
        processHandler.destroyProcess();
      }
    }
  }

  @Override
  public void dispose() {
    for (Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity : myRunningConfigurations) {
      Disposer.dispose(trinity.first);
    }
    myRunningConfigurations.clear();
  }

  @Nonnull
  @Override
  public RunContentManager getContentManager() {
    if (myContentManager == null) {
      myContentManager = new RunContentManagerImpl(myProject, myToolWindowManager, DockManager.getInstance(myProject));
      Disposer.register(myProject, myContentManager);
    }
    return myContentManager;
  }

  @Nonnull
  @Override
  public ProcessHandler[] getRunningProcesses() {
    if (myContentManager == null) return EMPTY_PROCESS_HANDLERS;
    List<ProcessHandler> handlers = null;
    for (RunContentDescriptor descriptor : getContentManager().getAllDescriptors()) {
      ProcessHandler processHandler = descriptor.getProcessHandler();
      if (processHandler != null) {
        if (handlers == null) {
          handlers = new SmartList<>();
        }
        handlers.add(processHandler);
      }
    }
    return handlers == null ? EMPTY_PROCESS_HANDLERS : handlers.toArray(new ProcessHandler[handlers.size()]);
  }

  private void compileAndRun(@Nonnull UIAccess uiAccess, @Nonnull Runnable startRunnable, @Nonnull ExecutionEnvironment environment, @Nullable Runnable onCancelRunnable) {
    long id = environment.getExecutionId();
    if (id == 0) {
      id = environment.assignNewExecutionId();
    }

    RunProfile profile = environment.getRunProfile();
    if (!(profile instanceof RunConfiguration)) {
      startRunnable.run();
      return;
    }

    final RunConfiguration runConfiguration = (RunConfiguration)profile;
    final List<BeforeRunTask> beforeRunTasks = RunManagerEx.getInstanceEx(myProject).getBeforeRunTasks(runConfiguration);
    if (beforeRunTasks.isEmpty()) {
      startRunnable.run();
    }
    else {
      DataContext context = environment.getDataContext();
      final DataContext projectContext = context != null ? context : SimpleDataContext.getProjectContext(myProject);

      AsyncResult<Void> result = AsyncResult.undefined();

      runBeforeTask(beforeRunTasks, 0, id, environment, uiAccess, projectContext, runConfiguration, result);

      if (onCancelRunnable != null) {
        result.doWhenRejected(() -> uiAccess.give(onCancelRunnable));
      }

      result.doWhenDone(() -> {
        // important! Do not use DumbService.smartInvokeLater here because it depends on modality state
        // and execution of startRunnable could be skipped if modality state check fails
        uiAccess.give(() -> {
          if (!myProject.isDisposed()) {
            DumbService.getInstance(myProject).runWhenSmart(startRunnable);
          }
        });
      });
    }
  }

  @SuppressWarnings("unchecked")
  private void runBeforeTask(@Nonnull List<BeforeRunTask> beforeRunTasks,
                             int index,
                             long executionSessionId,
                             @Nonnull ExecutionEnvironment environment,
                             @Nonnull UIAccess uiAccess,
                             @Nonnull DataContext dataContext,
                             @Nonnull RunConfiguration runConfiguration,
                             @Nonnull AsyncResult<Void> finishResult) {
    if (beforeRunTasks.size() == index) {
      finishResult.setDone();
      return;
    }

    if (myProject.isDisposed()) {
      return;
    }

    BeforeRunTask task = beforeRunTasks.get(index);

    BeforeRunTaskProvider<BeforeRunTask> provider = BeforeRunTaskProvider.getProvider(myProject, task.getProviderId());
    if (provider == null) {
      LOG.warn("Cannot find BeforeRunTaskProvider for id='" + task.getProviderId() + "'");
      runBeforeTask(beforeRunTasks, index + 1, executionSessionId, environment, uiAccess, dataContext, runConfiguration, finishResult);
      return;
    }

    myApplication.executeOnPooledThread(() -> {
      ExecutionEnvironment taskEnvironment = new ExecutionEnvironmentBuilder(environment).contentToReuse(null).build();
      taskEnvironment.setExecutionId(executionSessionId);
      taskEnvironment.putUserData(EXECUTION_SESSION_ID_KEY, executionSessionId);

      AsyncResult<Void> result = provider.executeTaskAsync(uiAccess, dataContext, runConfiguration, taskEnvironment, task);
      result.doWhenDone(() -> runBeforeTask(beforeRunTasks, index + 1, executionSessionId, environment, uiAccess, dataContext, runConfiguration, finishResult));
      result.doWhenRejected((Runnable)finishResult::setRejected);
    });
  }

  @RequiredUIAccess
  @Override
  public void startRunProfile(@Nonnull final RunProfileStarter starter, @Nonnull final RunProfileState state, @Nonnull final ExecutionEnvironment environment) {
    UIAccess.assertIsUIThread();

    final Project project = environment.getProject();
    RunContentDescriptor reuseContent = getContentManager().getReuseContent(environment);
    if (reuseContent != null) {
      reuseContent.setExecutionId(environment.getExecutionId());
      environment.setContentToReuse(reuseContent);
    }

    final Executor executor = environment.getExecutor();
    project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processStartScheduled(executor.getId(), environment);

    Runnable startRunnable;
    startRunnable = () -> {
      if (project.isDisposed()) {
        return;
      }

      RunProfile profile = environment.getRunProfile();
      project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processStarting(executor.getId(), environment);

      Consumer<Throwable> errorHandler = e -> {
        if (!(e instanceof ProcessCanceledException)) {
          ExecutionException error = e instanceof ExecutionException ? (ExecutionException)e : new ExecutionException(e);
          ExecutionUtil.handleExecutionError(project, ExecutionManager.getInstance(project).getContentManager().getToolWindowIdByEnvironment(environment), profile, error);
        }
        LOG.info(e);
        project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processNotStarted(executor.getId(), environment);
      };

      try {

        starter.executeAsync(state, environment).doWhenDone(descriptor -> AppUIUtil.invokeOnEdt(() -> {
          if (descriptor != null) {
            final Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity = Trinity.create(descriptor, environment.getRunnerAndConfigurationSettings(), executor);
            myRunningConfigurations.add(trinity);
            Disposer.register(descriptor, () -> myRunningConfigurations.remove(trinity));
            getContentManager().showRunContent(executor, descriptor, environment.getContentToReuse());
            final ProcessHandler processHandler = descriptor.getProcessHandler();
            if (processHandler != null) {
              if (!processHandler.isStartNotified()) {
                processHandler.startNotify();
              }
              project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processStarted(executor.getId(), environment, processHandler);

              ProcessExecutionListener listener = new ProcessExecutionListener(project, executor.getId(), environment, processHandler, descriptor);
              processHandler.addProcessListener(listener);

              // Since we cannot guarantee that the listener is added before process handled is start notified,
              // we have to make sure the process termination events are delivered to the clients.
              // Here we check the current process state and manually deliver events, while
              // the ProcessExecutionListener guarantees each such event is only delivered once
              // either by this code, or by the ProcessHandler.

              boolean terminating = processHandler.isProcessTerminating();
              boolean terminated = processHandler.isProcessTerminated();
              if (terminating || terminated) {
                listener.processWillTerminate(new ProcessEvent(processHandler), false /*doesn't matter*/);

                if (terminated) {
                  //noinspection ConstantConditions
                  Integer exitCode = processHandler.getExitCode();
                  listener.processTerminated(new ProcessEvent(processHandler, processHandler.isStartNotified() ? ObjectUtil.notNull(processHandler.getExitCode(), -1) : -1));
                }
              }
            }
            environment.setContentToReuse(descriptor);
          }
          else {
            project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processNotStarted(executor.getId(), environment);
          }
        }, o -> project.isDisposed())).doWhenRejectedWithThrowable(errorHandler);
      }
      catch (ExecutionException e) {
        errorHandler.accept(e);
      }
    };

    if (myApplication.isUnitTestMode()) {
      startRunnable.run();
    }
    else {
      compileAndRun(UIAccess.current(), () -> TransactionGuard.submitTransaction(project, startRunnable), environment, () -> {
        if (!project.isDisposed()) {
          project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processNotStarted(executor.getId(), environment);
        }
      });
    }
  }

  @Override
  public void restartRunProfile(@Nonnull Project project,
                                @Nonnull Executor executor,
                                @Nonnull ExecutionTarget target,
                                @Nullable RunnerAndConfigurationSettings configuration,
                                @Nullable ProcessHandler processHandler) {
    ExecutionEnvironmentBuilder builder = createEnvironmentBuilder(project, executor, configuration);
    if (processHandler != null) {
      for (RunContentDescriptor descriptor : getContentManager().getAllDescriptors()) {
        if (descriptor.getProcessHandler() == processHandler) {
          builder.contentToReuse(descriptor);
          break;
        }
      }
    }
    restartRunProfile(builder.target(target).build());
  }

  @Nonnull
  private static ExecutionEnvironmentBuilder createEnvironmentBuilder(@Nonnull Project project, @Nonnull Executor executor, @Nullable RunnerAndConfigurationSettings configuration) {
    ExecutionEnvironmentBuilder builder = new ExecutionEnvironmentBuilder(project, executor);

    ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), configuration != null ? configuration.getConfiguration() : null);
    if (runner == null && configuration != null) {
      LOG.error("Cannot find runner for " + configuration.getName());
    }
    else if (runner != null) {
      assert configuration != null;
      builder.runnerAndSettings(runner, configuration);
    }
    return builder;
  }

  @Override
  public void restartRunProfile(@Nonnull Project project,
                                @Nonnull Executor executor,
                                @Nonnull ExecutionTarget target,
                                @Nullable RunnerAndConfigurationSettings configuration,
                                @Nullable RunContentDescriptor currentDescriptor) {
    ExecutionEnvironmentBuilder builder = createEnvironmentBuilder(project, executor, configuration);
    restartRunProfile(builder.target(target).contentToReuse(currentDescriptor).build());
  }

  @Override
  public void restartRunProfile(@Nullable ProgramRunner runner, @Nonnull ExecutionEnvironment environment, @Nullable RunContentDescriptor currentDescriptor) {
    ExecutionEnvironmentBuilder builder = new ExecutionEnvironmentBuilder(environment).contentToReuse(currentDescriptor);
    if (runner != null) {
      builder.runner(runner);
    }
    restartRunProfile(builder.build());
  }

  public static boolean isProcessRunning(@Nullable RunContentDescriptor descriptor) {
    ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
    return processHandler != null && !processHandler.isProcessTerminated();
  }

  @Override
  public void restartRunProfile(@Nonnull final ExecutionEnvironment environment) {
    RunnerAndConfigurationSettings configuration = environment.getRunnerAndConfigurationSettings();

    List<RunContentDescriptor> runningIncompatible;
    if (configuration == null) {
      runningIncompatible = Collections.emptyList();
    }
    else {
      runningIncompatible = getIncompatibleRunningDescriptors(configuration);
    }

    RunContentDescriptor contentToReuse = environment.getContentToReuse();
    final List<RunContentDescriptor> runningOfTheSameType = new SmartList<>();
    if (configuration != null && configuration.isSingleton()) {
      runningOfTheSameType.addAll(getRunningDescriptorsOfTheSameConfigType(configuration));
    }
    else if (isProcessRunning(contentToReuse)) {
      runningOfTheSameType.add(contentToReuse);
    }

    List<RunContentDescriptor> runningToStop = ContainerUtil.concat(runningOfTheSameType, runningIncompatible);
    if (!runningToStop.isEmpty()) {
      if (configuration != null) {
        if (!runningOfTheSameType.isEmpty() &&
            (runningOfTheSameType.size() > 1 || contentToReuse == null || runningOfTheSameType.get(0) != contentToReuse) &&
            !userApprovesStopForSameTypeConfigurations(environment.getProject(), configuration.getName(), runningOfTheSameType.size())) {
          return;
        }
        if (!runningIncompatible.isEmpty() && !userApprovesStopForIncompatibleConfigurations(myProject, configuration.getName(), runningIncompatible)) {
          return;
        }
      }

      for (RunContentDescriptor descriptor : runningToStop) {
        stopProcess(descriptor);
      }
    }

    awaitingTerminationAlarm.addRequest(new Runnable() {
      @Override
      public void run() {
        if (ExecutorRegistry.getInstance().isStarting(environment)) {
          awaitingTerminationAlarm.addRequest(this, 100);
          return;
        }

        for (RunContentDescriptor descriptor : runningOfTheSameType) {
          ProcessHandler processHandler = descriptor.getProcessHandler();
          if (processHandler != null && !processHandler.isProcessTerminated()) {
            awaitingTerminationAlarm.addRequest(this, 100);
            return;
          }
        }
        start(environment);
      }
    }, 50);
  }

  private static void start(@Nonnull ExecutionEnvironment environment) {
    RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
    ProgramRunnerUtil.executeConfiguration(environment, settings != null && settings.isEditBeforeRun(), true);
  }

  private static boolean userApprovesStopForSameTypeConfigurations(Project project, String configName, int instancesCount) {
    RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
    final RunManagerConfig config = runManager.getConfig();
    if (!config.isRestartRequiresConfirmation()) return true;

    DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
      @Override
      public boolean isToBeShown() {
        return config.isRestartRequiresConfirmation();
      }

      @Override
      public void setToBeShown(boolean value, int exitCode) {
        config.setRestartRequiresConfirmation(value);
      }

      @Override
      public boolean canBeHidden() {
        return true;
      }

      @Override
      public boolean shouldSaveOptionsOnCancel() {
        return false;
      }

      @Nonnull
      @Override
      public String getDoNotShowMessage() {
        return CommonBundle.message("dialog.options.do.not.show");
      }
    };
    return Messages.showOkCancelDialog(project, ExecutionBundle.message("rerun.singleton.confirmation.message", configName, instancesCount),
                                       ExecutionBundle.message("process.is.running.dialog.title", configName), ExecutionBundle.message("rerun.confirmation.button.text"),
                                       CommonBundle.message("button.cancel"), Messages.getQuestionIcon(), option) == Messages.OK;
  }

  private static boolean userApprovesStopForIncompatibleConfigurations(Project project, String configName, List<RunContentDescriptor> runningIncompatibleDescriptors) {
    RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
    final RunManagerConfig config = runManager.getConfig();
    if (!config.isRestartRequiresConfirmation()) return true;

    DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
      @Override
      public boolean isToBeShown() {
        return config.isRestartRequiresConfirmation();
      }

      @Override
      public void setToBeShown(boolean value, int exitCode) {
        config.setRestartRequiresConfirmation(value);
      }

      @Override
      public boolean canBeHidden() {
        return true;
      }

      @Override
      public boolean shouldSaveOptionsOnCancel() {
        return false;
      }

      @Nonnull
      @Override
      public String getDoNotShowMessage() {
        return CommonBundle.message("dialog.options.do.not.show");
      }
    };

    final StringBuilder names = new StringBuilder();
    for (final RunContentDescriptor descriptor : runningIncompatibleDescriptors) {
      String name = descriptor.getDisplayName();
      if (names.length() > 0) {
        names.append(", ");
      }
      names.append(StringUtil.isEmpty(name) ? ExecutionBundle.message("run.configuration.no.name") : String.format("'%s'", name));
    }

    //noinspection DialogTitleCapitalization
    return Messages.showOkCancelDialog(project, ExecutionBundle.message("stop.incompatible.confirmation.message", configName, names.toString(), runningIncompatibleDescriptors.size()),
                                       ExecutionBundle.message("incompatible.configuration.is.running.dialog.title", runningIncompatibleDescriptors.size()),
                                       ExecutionBundle.message("stop.incompatible.confirmation.button.text"), CommonBundle.message("button.cancel"), Messages.getQuestionIcon(), option) == Messages.OK;
  }

  @Nonnull
  private List<RunContentDescriptor> getRunningDescriptorsOfTheSameConfigType(@Nonnull final RunnerAndConfigurationSettings configurationAndSettings) {
    return getRunningDescriptors(runningConfigurationAndSettings -> configurationAndSettings == runningConfigurationAndSettings);
  }

  @Nonnull
  private List<RunContentDescriptor> getIncompatibleRunningDescriptors(@Nonnull RunnerAndConfigurationSettings configurationAndSettings) {
    final RunConfiguration configurationToCheckCompatibility = configurationAndSettings.getConfiguration();
    return getRunningDescriptors(runningConfigurationAndSettings -> {
      RunConfiguration runningConfiguration = runningConfigurationAndSettings == null ? null : runningConfigurationAndSettings.getConfiguration();
      if (runningConfiguration == null || !(runningConfiguration instanceof CompatibilityAwareRunProfile)) {
        return false;
      }
      return ((CompatibilityAwareRunProfile)runningConfiguration).mustBeStoppedToRun(configurationToCheckCompatibility);
    });
  }

  @Nonnull
  public List<RunContentDescriptor> getRunningDescriptors(@Nonnull Condition<RunnerAndConfigurationSettings> condition) {
    List<RunContentDescriptor> result = new SmartList<>();
    for (Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity : myRunningConfigurations) {
      if (condition.value(trinity.getSecond())) {
        ProcessHandler processHandler = trinity.getFirst().getProcessHandler();
        if (processHandler != null /*&& !processHandler.isProcessTerminating()*/ && !processHandler.isProcessTerminated()) {
          result.add(trinity.getFirst());
        }
      }
    }
    return result;
  }

  @Nonnull
  public List<RunContentDescriptor> getDescriptors(@Nonnull Condition<RunnerAndConfigurationSettings> condition) {
    List<RunContentDescriptor> result = new SmartList<>();
    for (Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity : myRunningConfigurations) {
      if (trinity.getSecond() != null && condition.value(trinity.getSecond())) {
        result.add(trinity.getFirst());
      }
    }
    return result;
  }

  @Nonnull
  public Set<Executor> getExecutors(RunContentDescriptor descriptor) {
    Set<Executor> result = new HashSet<>();
    for (Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity : myRunningConfigurations) {
      if (descriptor == trinity.first) result.add(trinity.third);
    }
    return result;
  }

  @Nonnull
  public Set<RunnerAndConfigurationSettings> getConfigurations(RunContentDescriptor descriptor) {
    Set<RunnerAndConfigurationSettings> result = new HashSet<>();
    for (Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity : myRunningConfigurations) {
      if (descriptor == trinity.first) result.add(trinity.second);
    }
    return result;
  }

  private static class ProcessExecutionListener extends ProcessAdapter {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final String myExecutorId;
    @Nonnull
    private final ExecutionEnvironment myEnvironment;
    @Nonnull
    private final ProcessHandler myProcessHandler;
    @Nonnull
    private final RunContentDescriptor myDescriptor;
    @Nonnull
    private final AtomicBoolean myWillTerminateNotified = new AtomicBoolean();
    @Nonnull
    private final AtomicBoolean myTerminateNotified = new AtomicBoolean();

    public ProcessExecutionListener(@Nonnull Project project,
                                    @Nonnull String executorId,
                                    @Nonnull ExecutionEnvironment environment,
                                    @Nonnull ProcessHandler processHandler,
                                    @Nonnull RunContentDescriptor descriptor) {
      myProject = project;
      myExecutorId = executorId;
      myEnvironment = environment;
      myProcessHandler = processHandler;
      myDescriptor = descriptor;
    }

    @Override
    public void processTerminated(ProcessEvent event) {
      if (myProject.isDisposed()) return;
      if (!myTerminateNotified.compareAndSet(false, true)) return;

      ApplicationManager.getApplication().invokeLater(() -> {
        RunnerLayoutUi ui = myDescriptor.getRunnerLayoutUi();
        if (ui != null && !ui.isDisposed()) {
          ui.updateActionsNow();
        }
      }, ModalityState.any());

      myProject.getMessageBus().syncPublisher(EXECUTION_TOPIC).processTerminated(myExecutorId, myEnvironment, myProcessHandler, event.getExitCode());

      SaveAndSyncHandler saveAndSyncHandler = SaveAndSyncHandler.getInstance();
      if (saveAndSyncHandler != null) {
        saveAndSyncHandler.scheduleRefresh();
      }
    }

    @Override
    public void processWillTerminate(ProcessEvent event, boolean shouldNotBeUsed) {
      if (myProject.isDisposed()) return;
      if (!myWillTerminateNotified.compareAndSet(false, true)) return;

      myProject.getMessageBus().syncPublisher(EXECUTION_TOPIC).processTerminating(myExecutorId, myEnvironment, myProcessHandler);
    }
  }
}
