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
package consulo.execution.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.*;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.*;
import consulo.execution.configuration.CompatibilityAwareRunProfile;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.event.ExecutionListener;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.internal.RunManagerConfig;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.RunContentManager;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerStopper;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public class ExecutionManagerImpl implements ExecutionManager, Disposable {
  private record RunInfo(RunContentDescriptor descriptor, RunnerAndConfigurationSettings settings, ProgramRunner<?> runner, Executor executor) {
  }

  private record InProgressEntry(String executorId, String runnerId) {
  }

  public static final Key<Object> EXECUTION_SESSION_ID_KEY = Key.create("EXECUTION_SESSION_ID_KEY");

  private static final Logger LOG = Logger.getInstance(ExecutionManagerImpl.class);
  private static final ProcessHandler[] EMPTY_PROCESS_HANDLERS = new ProcessHandler[0];

  private final Application myApplication;
  private final Project myProject;

  private final Provider<RunContentManager> myContentManager;
  private final List<RunInfo> myRunningConfigurations =  Lists.newLockFreeCopyOnWriteList();
  private final Set<InProgressEntry> myInProgress = ConcurrentHashMap.newKeySet();

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Nonnull
  public static ExecutionManagerImpl getInstance(@Nonnull Project project) {
    return (ExecutionManagerImpl)project.getInstance(ExecutionManager.class);
  }

  @Inject
  ExecutionManagerImpl(@Nonnull Application application,
                       @Nonnull Project project,
                       @Nonnull Provider<RunContentManager> contentManager) {
    myApplication = application;
    myProject = project;
    myContentManager = contentManager;
  }

  public static void stopProcess(@Nullable RunContentDescriptor descriptor) {
    stopProcess(descriptor != null ? descriptor.getProcessHandler() : null);
  }

  public static void stopProcess(@Nullable ProcessHandler processHandler) {
    if (processHandler == null) {
      return;
    }

    ProcessHandlerStopper.stop(processHandler);
  }

  @Override
  public void dispose() {
    for (RunInfo trinity : myRunningConfigurations) {
      Disposer.dispose(trinity.descriptor());
    }
    myRunningConfigurations.clear();
  }

  @Nonnull
  @Override
  public RunContentManager getContentManager() {
    return myContentManager.get();
  }

  @Nonnull
  @Override
  public ProcessHandler[] getRunningProcesses() {
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

  private void compileAndRun(@Nonnull UIAccess uiAccess,
                             @Nonnull Runnable startRunnable,
                             @Nonnull ExecutionEnvironment environment,
                             @Nullable Runnable onCancelRunnable) {
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
      final DataContext projectContext = context != null ? context : DataContext.builder().add(Project.KEY, myProject).build();

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
      result.doWhenDone(() -> runBeforeTask(beforeRunTasks,
                                            index + 1,
                                            executionSessionId,
                                            environment,
                                            uiAccess,
                                            dataContext,
                                            runConfiguration,
                                            finishResult));
      result.doWhenRejected((Runnable)finishResult::setRejected);
    });
  }

  @RequiredUIAccess
  @Override
  public void startRunProfile(@Nonnull final RunProfileStarter starter,
                              @Nonnull final RunProfileState state,
                              @Nonnull final ExecutionEnvironment environment) {
    UIAccess.assertIsUIThread();

    final Project project = environment.getProject();
    RunContentDescriptor reuseContent = getContentManager().getReuseContent(environment);
    if (reuseContent != null) {
      reuseContent.setExecutionId(environment.getExecutionId());
      environment.setContentToReuse(reuseContent);
    }

    final Executor executor = environment.getExecutor();
    project.getMessageBus().syncPublisher(ExecutionListener.class).processStartScheduled(executor.getId(), environment);

    InProgressEntry entry = new InProgressEntry(executor.getId(), environment.getRunner().getRunnerId());

    myInProgress.add(entry);

    Runnable startRunnable;
    startRunnable = () -> {
      if (project.isDisposed()) {
        return;
      }

      RunProfile profile = environment.getRunProfile();
      project.getMessageBus().syncPublisher(ExecutionListener.class).processStarting(executor.getId(), environment);

      Consumer<Throwable> errorHandler = e -> {
        if (!(e instanceof ProcessCanceledException)) {
          ExecutionException error = e instanceof ExecutionException ? (ExecutionException)e : new ExecutionException(e);
          ExecutionUtil.handleExecutionError(project,
                                             ExecutionManager.getInstance(project)
                                                             .getContentManager()
                                                             .getToolWindowIdByEnvironment(environment),
                                             profile,
                                             error);
        }
        LOG.info(e);
        project.getMessageBus().syncPublisher(ExecutionListener.class).processNotStarted(executor.getId(), environment);
      };

      try {

        starter.executeAsync(state, environment).doWhenDone(descriptor -> AppUIUtil.invokeOnEdt(() -> {
          if (descriptor != null) {
            RunInfo info = new RunInfo(descriptor, environment.getRunnerAndConfigurationSettings(), environment.getRunner(), executor);
            myRunningConfigurations.add(info);
            Disposer.register(descriptor, () -> myRunningConfigurations.remove(info));

            RunContentManager contentManager = getContentManager();

            String toolWindowId = contentManager.getContentDescriptorToolWindowId(environment);
            if (toolWindowId != null) {
              descriptor.setContentToolWindowId(toolWindowId);
            }

            contentManager.showRunContent(executor, descriptor, environment.getContentToReuse());
            final ProcessHandler processHandler = descriptor.getProcessHandler();
            if (processHandler != null) {
              if (!processHandler.isStartNotified()) {
                processHandler.startNotify();
              }

              myInProgress.remove(entry);

              project.getMessageBus().syncPublisher(ExecutionListener.class).processStarted(executor.getId(), environment, processHandler);

              ProcessExecutionListener listener =
                new ProcessExecutionListener(project, executor.getId(), environment, processHandler, descriptor);
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
                  listener.processTerminated(new ProcessEvent(processHandler,
                                                              processHandler.isStartNotified() ? ObjectUtil.notNull(processHandler.getExitCode(),
                                                                                                                    -1) : -1));
                }
              }
            }
            environment.setContentToReuse(descriptor);
          }
          else {
            project.getMessageBus().syncPublisher(ExecutionListener.class).processNotStarted(executor.getId(), environment);
          }
        }, project::isDisposed)).doWhenRejectedWithThrowable(errorHandler);
      }
      catch (ExecutionException e) {
        errorHandler.accept(e);
      }
    };

    compileAndRun(UIAccess.current(), () -> TransactionGuard.submitTransaction(project, startRunnable), environment, () -> {
      if (!project.isDisposed()) {
        myInProgress.remove(entry);
        project.getMessageBus().syncPublisher(ExecutionListener.class).processNotStarted(executor.getId(), environment);
      }
    });
  }

  @Override
  public void restartRunProfile(@Nonnull Project project,
                                @Nonnull Executor executor,
                                @Nonnull ExecutionTarget target,
                                @Nullable RunnerAndConfigurationSettings configuration,
                                @Nullable ProcessHandler processHandler,
                                @Nonnull Consumer<ExecutionEnvironment> envCustomizer) {
    ExecutionEnvironmentBuilder builder = createEnvironmentBuilder(project, executor, configuration);
    if (processHandler != null) {
      for (RunContentDescriptor descriptor : getContentManager().getAllDescriptors()) {
        if (descriptor.getProcessHandler() == processHandler) {
          builder.contentToReuse(descriptor);
          break;
        }
      }
    }
    
    ExecutionEnvironment executionEnvironment = builder.target(target).build();
    envCustomizer.accept(executionEnvironment);
    restartRunProfile(executionEnvironment);
  }

  @Nonnull
  private static ExecutionEnvironmentBuilder createEnvironmentBuilder(@Nonnull Project project,
                                                                      @Nonnull Executor executor,
                                                                      @Nullable RunnerAndConfigurationSettings configuration) {
    ExecutionEnvironmentBuilder builder = new ExecutionEnvironmentBuilder(project, executor);

    ProgramRunner runner =
      RunnerRegistry.getInstance().getRunner(executor.getId(), configuration != null ? configuration.getConfiguration() : null);
    if (runner == null && configuration != null) {
      LOG.error("Cannot find runner for " + configuration.getName());
    }
    else if (runner != null) {
      assert configuration != null;
      builder.runnerAndSettings(runner, configuration);
    }
    return builder;
  }

  public static boolean isProcessRunning(@Nullable RunContentDescriptor descriptor) {
    ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
    return processHandler != null && !processHandler.isProcessTerminated();
  }

  @Override
  public boolean isStarting(String exectutorId, String runnerId) {
    return myInProgress.contains(new InProgressEntry(exectutorId, runnerId));
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
        if (!runningIncompatible.isEmpty() && !userApprovesStopForIncompatibleConfigurations(myProject,
                                                                                             configuration.getName(),
                                                                                             runningIncompatible)) {
          return;
        }
      }

      for (RunContentDescriptor descriptor : runningToStop) {
        stopProcess(descriptor);
      }
    }

    myProject.getUIAccess().getScheduler().schedule(new Runnable() {
      @Override
      public void run() {
        if (ExecutorRegistry.getInstance().isStarting(environment)) {
          myProject.getUIAccess().getScheduler().schedule(this, 100, TimeUnit.MILLISECONDS);
          return;
        }

        for (RunContentDescriptor descriptor : runningOfTheSameType) {
          ProcessHandler processHandler = descriptor.getProcessHandler();
          if (processHandler != null && !processHandler.isProcessTerminated()) {
            myProject.getUIAccess().getScheduler().schedule(this, 100, TimeUnit.MILLISECONDS);
            return;
          }
        }

        start(environment);
      }
    }, 50, TimeUnit.MILLISECONDS);
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
    return Messages.showOkCancelDialog(project,
                                       ExecutionBundle.message("rerun.singleton.confirmation.message", configName, instancesCount),
                                       ExecutionBundle.message("process.is.running.dialog.title", configName),
                                       ExecutionBundle.message("rerun.confirmation.button.text"),
                                       CommonBundle.message("button.cancel"),
                                       Messages.getQuestionIcon(),
                                       option) == Messages.OK;
  }

  private static boolean userApprovesStopForIncompatibleConfigurations(Project project,
                                                                       String configName,
                                                                       List<RunContentDescriptor> runningIncompatibleDescriptors) {
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
    return Messages.showOkCancelDialog(project,
                                       ExecutionBundle.message("stop.incompatible.confirmation.message",
                                                               configName,
                                                               names.toString(),
                                                               runningIncompatibleDescriptors.size()),
                                       ExecutionBundle.message("incompatible.configuration.is.running.dialog.title",
                                                               runningIncompatibleDescriptors.size()),
                                       ExecutionBundle.message("stop.incompatible.confirmation.button.text"),
                                       CommonBundle.message("button.cancel"),
                                       Messages.getQuestionIcon(),
                                       option) == Messages.OK;
  }

  @Nonnull
  private List<RunContentDescriptor> getRunningDescriptorsOfTheSameConfigType(@Nonnull final RunnerAndConfigurationSettings configurationAndSettings) {
    return getRunningDescriptors(runningConfigurationAndSettings -> configurationAndSettings == runningConfigurationAndSettings);
  }

  @Nonnull
  private List<RunContentDescriptor> getIncompatibleRunningDescriptors(@Nonnull RunnerAndConfigurationSettings configurationAndSettings) {
    final RunConfiguration configurationToCheckCompatibility = configurationAndSettings.getConfiguration();
    return getRunningDescriptors(runningConfigurationAndSettings -> {
      RunConfiguration runningConfiguration =
        runningConfigurationAndSettings == null ? null : runningConfigurationAndSettings.getConfiguration();
      if (runningConfiguration == null || !(runningConfiguration instanceof CompatibilityAwareRunProfile)) {
        return false;
      }
      return ((CompatibilityAwareRunProfile)runningConfiguration).mustBeStoppedToRun(configurationToCheckCompatibility);
    });
  }

  @Override
  @Nonnull
  public List<RunContentDescriptor> getRunningDescriptors(@Nonnull Predicate<? super RunnerAndConfigurationSettings> condition) {
    List<RunContentDescriptor> result = new SmartList<>();
    for (RunInfo runInfo : myRunningConfigurations) {
      if (condition.test(runInfo.settings())) {
        ProcessHandler processHandler = runInfo.descriptor().getProcessHandler();
        if (processHandler != null /*&& !processHandler.isProcessTerminating()*/ && !processHandler.isProcessTerminated()) {
          result.add(runInfo.descriptor());
        }
      }
    }
    return result;
  }

  @Override
  @Nonnull
  public List<RunContentDescriptor> getDescriptors(@Nonnull Predicate<? super RunnerAndConfigurationSettings> condition) {
    List<RunContentDescriptor> result = new SmartList<>();
    for (RunInfo runInfo : myRunningConfigurations) {
      if (runInfo.settings() != null && condition.test(runInfo.settings())) {
        result.add(runInfo.descriptor());
      }
    }
    return result;
  }

  @Nonnull
  public Set<Executor> getExecutors(RunContentDescriptor descriptor) {
    Set<Executor> result = new HashSet<>();
    for (RunInfo runInfo : myRunningConfigurations) {
      if (descriptor == runInfo.descriptor()) result.add(runInfo.executor());
    }
    return result;
  }

  @Nonnull
  public Set<RunnerAndConfigurationSettings> getConfigurations(RunContentDescriptor descriptor) {
    Set<RunnerAndConfigurationSettings> result = new HashSet<>();
    for (RunInfo trinity : myRunningConfigurations) {
      if (descriptor == trinity.descriptor()) result.add(trinity.settings());
    }
    return result;
  }

  public static RunProfile getDelegatedRunProfile(RunConfiguration runConfiguration) {
    // TODO this is not implemented - return as is
    return runConfiguration;
  }

  private static class ProcessExecutionListener implements ProcessListener {
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

      myProject.getMessageBus()
               .syncPublisher(ExecutionListener.class)
               .processTerminated(myExecutorId, myEnvironment, myProcessHandler, event.getExitCode());

      SaveAndSyncHandler saveAndSyncHandler = SaveAndSyncHandler.getInstance();
      if (saveAndSyncHandler != null) {
        saveAndSyncHandler.scheduleRefresh();
      }
    }

    @Override
    public void processWillTerminate(ProcessEvent event, boolean shouldNotBeUsed) {
      if (myProject.isDisposed()) return;
      if (!myWillTerminateNotified.compareAndSet(false, true)) return;

      myProject.getMessageBus().syncPublisher(ExecutionListener.class).processTerminating(myExecutorId, myEnvironment, myProcessHandler);
    }
  }
}
