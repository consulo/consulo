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
package com.intellij.execution;

import com.intellij.execution.actions.RunContextAction;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.Trinity;
import com.intellij.util.containers.ContainerUtil;
import java.util.HashMap;

import com.intellij.util.messages.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Singleton
public class ExecutorRegistryImpl extends ExecutorRegistry implements Disposable {
  private static final Logger LOG = Logger.getInstance(ExecutorRegistryImpl.class);

  public static final String RUNNERS_GROUP = "RunnerActions";
  public static final String RUN_CONTEXT_GROUP = "RunContextGroupInner";

  private List<Executor> myExecutors = new ArrayList<>();
  private ActionManager myActionManager;
  private final Map<String, Executor> myId2Executor = new HashMap<>();
  private final Set<String> myContextActionIdSet = new HashSet<>();
  private final Map<String, AnAction> myId2Action = new HashMap<>();
  private final Map<String, AnAction> myContextActionId2Action = new HashMap<>();

  // [Project, ExecutorId, RunnerId]
  private final Set<Trinity<Project, String, String>> myInProgress = Collections.synchronizedSet(new HashSet<Trinity<Project, String, String>>());

  @Inject
  public ExecutorRegistryImpl(Application application, ActionManager actionManager) {
    myActionManager = actionManager;

    MessageBusConnection connection = application.getMessageBus().connect(this);
    connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
      @Override
      public void processStartScheduled(@Nonnull String executorId, @Nonnull ExecutionEnvironment environment) {
        myInProgress.add(createExecutionId(executorId, environment));
      }

      @Override
      public void processNotStarted(@Nonnull String executorId, @Nonnull ExecutionEnvironment environment) {
        myInProgress.remove(createExecutionId(executorId, environment));
      }

      @Override
      public void processStarted(@Nonnull String executorId, @Nonnull ExecutionEnvironment environment, @Nonnull ProcessHandler handler) {
        myInProgress.remove(createExecutionId(executorId, environment));
      }
    });

    connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosed(@Nonnull final Project project, @Nonnull UIAccess uiAccess) {
        // perform cleanup
        synchronized (myInProgress) {
          for (Iterator<Trinity<Project, String, String>> it = myInProgress.iterator(); it.hasNext(); ) {
            final Trinity<Project, String, String> trinity = it.next();
            if (project.equals(trinity.first)) {
              it.remove();
            }
          }
        }
      }
    });

    for (Executor executor : Executor.EP_NAME.getExtensionList()) {
      initExecutor(executor);
    }
  }

  synchronized void initExecutor(@Nonnull final Executor executor) {
    if (myId2Executor.get(executor.getId()) != null) {
      LOG.error("Executor with id: \"" + executor.getId() + "\" was already registered!");
    }

    if (myContextActionIdSet.contains(executor.getContextActionId())) {
      LOG.error("Executor with context action id: \"" + executor.getContextActionId() + "\" was already registered!");
    }

    myExecutors.add(executor);
    myId2Executor.put(executor.getId(), executor);
    myContextActionIdSet.add(executor.getContextActionId());

    registerAction(executor.getId(), new ExecutorAction(executor), RUNNERS_GROUP, myId2Action);
    registerAction(executor.getContextActionId(), new RunContextAction(executor), RUN_CONTEXT_GROUP, myContextActionId2Action);
  }

  private void registerAction(@Nonnull final String actionId, @Nonnull final AnAction anAction, @Nonnull final String groupId, @Nonnull final Map<String, AnAction> map) {
    AnAction action = myActionManager.getAction(actionId);
    if (action == null) {
      myActionManager.registerAction(actionId, anAction);
      map.put(actionId, anAction);
      action = anAction;
    }

    ((DefaultActionGroup)myActionManager.getAction(groupId)).add(action, Constraints.LAST, myActionManager);
  }

  synchronized void deinitExecutor(@Nonnull final Executor executor) {
    myExecutors.remove(executor);
    myId2Executor.remove(executor.getId());
    myContextActionIdSet.remove(executor.getContextActionId());

    unregisterAction(executor.getId(), RUNNERS_GROUP, myId2Action);
    unregisterAction(executor.getContextActionId(), RUN_CONTEXT_GROUP, myContextActionId2Action);
  }

  private void unregisterAction(@Nonnull final String actionId, @Nonnull final String groupId, @Nonnull final Map<String, AnAction> map) {
    final DefaultActionGroup group = (DefaultActionGroup)myActionManager.getAction(groupId);
    if (group != null) {
      group.remove(myActionManager.getAction(actionId));
      final AnAction action = map.get(actionId);
      if (action != null) {
        myActionManager.unregisterAction(actionId);
        map.remove(actionId);
      }
    }
  }

  @Override
  @Nonnull
  public synchronized Executor[] getRegisteredExecutors() {
    return myExecutors.toArray(new Executor[myExecutors.size()]);
  }

  @Override
  public Executor getExecutorById(final String executorId) {
    return myId2Executor.get(executorId);
  }

  @Nonnull
  private static Trinity<Project, String, String> createExecutionId(String executorId, @Nonnull ExecutionEnvironment environment) {
    return Trinity.create(environment.getProject(), executorId, environment.getRunner().getRunnerId());
  }

  @Override
  public boolean isStarting(Project project, final String executorId, final String runnerId) {
    return myInProgress.contains(Trinity.create(project, executorId, runnerId));
  }

  @Override
  public boolean isStarting(@Nonnull ExecutionEnvironment environment) {
    return isStarting(environment.getProject(), environment.getExecutor().getId(), environment.getRunner().getRunnerId());
  }

  @Override
  public synchronized void dispose() {
    if (!myExecutors.isEmpty()) {
      for (Executor executor : new ArrayList<>(myExecutors)) {
        deinitExecutor(executor);
      }
    }
    myExecutors = null;
    myActionManager = null;
  }

  private class ExecutorAction extends AnAction implements DumbAware, UpdateInBackground {
    private final Executor myExecutor;

    private ExecutorAction(@Nonnull final Executor executor) {
      super(executor.getStartActionText(), executor.getDescription(), executor.getIcon());
      getTemplatePresentation().setVisible(false);
      myExecutor = executor;
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull final AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      final Project project = e.getProject();

      if (project == null || project.isDisposed()) {
        presentation.setEnabledAndVisible(false);
        return;
      }

      presentation.setVisible(myExecutor.isApplicable(project));
      if(!presentation.isVisible()) {
        return;
      }

      if(DumbService.getInstance(project).isDumb() || !project.isInitialized()) {
        presentation.setEnabled(false);
        return;
      }

      final RunnerAndConfigurationSettings selectedConfiguration = getConfiguration(project);
      boolean enabled = false;
      String text;
      final String textWithMnemonic = getTemplatePresentation().getTextWithMnemonic();
      if (selectedConfiguration != null) {
        presentation.setIcon(getInformativeIcon(project, selectedConfiguration));

        final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(myExecutor.getId(), selectedConfiguration.getConfiguration());

        ExecutionTarget target = ExecutionTargetManager.getActiveTarget(project);
        enabled = ExecutionTargetManager.canRun(selectedConfiguration, target)
                  && runner != null && !isStarting(project, myExecutor.getId(), runner.getRunnerId());

        if (enabled) {
          presentation.setDescription(myExecutor.getDescription());
        }
        text = myExecutor.getActionText(selectedConfiguration.getName());
      }
      else {
        text = textWithMnemonic;
      }

      presentation.setEnabled(enabled);
      presentation.setText(text);
    }

    @Nonnull
    private Image getInformativeIcon(Project project, final RunnerAndConfigurationSettings selectedConfiguration) {
      final ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);

      List<RunContentDescriptor> runningDescriptors = executionManager.getRunningDescriptors(s -> s == selectedConfiguration);
      runningDescriptors = ContainerUtil.filter(runningDescriptors, descriptor -> {
        RunContentDescriptor contentDescriptor = executionManager.getContentManager().findContentDescriptor(myExecutor, descriptor.getProcessHandler());
        return contentDescriptor != null && executionManager.getExecutors(contentDescriptor).contains(myExecutor);
      });

      if (!runningDescriptors.isEmpty() && DefaultRunExecutor.EXECUTOR_ID.equals(myExecutor.getId()) && selectedConfiguration.isSingleton()) {
        return AllIcons.Actions.Restart;
      }
      if (runningDescriptors.isEmpty()) {
        return myExecutor.getIcon();
      }

      if (runningDescriptors.size() == 1) {
        return ExecutionUtil.getIconWithLiveIndicator(myExecutor.getIcon());
      }
      else {
        return ImageEffects.withText(myExecutor.getIcon(), String.valueOf(runningDescriptors.size()));
      }
    }

    @Nullable
    private RunnerAndConfigurationSettings getConfiguration(@Nonnull final Project project) {
      return RunManagerEx.getInstanceEx(project).getSelectedConfiguration();
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull final AnActionEvent e) {
      final Project project = e.getProject();
      if (project == null || project.isDisposed()) {
        return;
      }

      RunnerAndConfigurationSettings configuration = getConfiguration(project);
      ExecutionEnvironmentBuilder builder = configuration == null ? null : ExecutionEnvironmentBuilder.createOrNull(myExecutor, configuration);
      if (builder == null) {
        return;
      }
      ExecutionManager.getInstance(project).restartRunProfile(builder.activeTarget().dataContext(e.getDataContext()).build());
    }
  }
}
