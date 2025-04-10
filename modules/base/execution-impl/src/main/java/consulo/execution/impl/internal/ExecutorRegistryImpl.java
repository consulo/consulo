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
import consulo.application.Application;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.execution.event.ExecutionListener;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.action.ExecutorAction;
import consulo.execution.impl.internal.action.RunContextAction;
import consulo.execution.impl.internal.action.RunCurrentFileService;
import consulo.execution.internal.ExecutorRegistryEx;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.logging.Logger;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.*;
import consulo.util.lang.Trinity;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@ServiceImpl
public class ExecutorRegistryImpl extends ExecutorRegistryEx implements Disposable {
    private static final Logger LOG = Logger.getInstance(ExecutorRegistryImpl.class);

    public static final String RUNNERS_GROUP = IdeActions.GROUP_RUNNER_ACTIONS;
    public static final String RUN_CONTEXT_GROUP = "RunContextGroupInner";

    private List<Executor> myExecutors = new ArrayList<>();
    private ActionManager myActionManager;
    private final RunCurrentFileService myRunCurrentFileService;
    private final Map<String, Executor> myId2Executor = new HashMap<>();
    private final Set<String> myContextActionIdSet = new HashSet<>();
    private final Map<String, AnAction> myId2Action = new HashMap<>();
    private final Map<String, AnAction> myContextActionId2Action = new HashMap<>();

    // [Project, ExecutorId, RunnerId]
    private final Set<Trinity<Project, String, String>> myInProgress = Collections.synchronizedSet(new HashSet<Trinity<Project, String, String>>());

    @Inject
    public ExecutorRegistryImpl(Application application, ActionManager actionManager, RunCurrentFileService runCurrentFileService) {
        myActionManager = actionManager;
        myRunCurrentFileService = runCurrentFileService;

        MessageBusConnection connection = application.getMessageBus().connect(this);
        connection.subscribe(ExecutionListener.class, new ExecutionListener() {
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

        connection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
                // perform cleanup
                synchronized (myInProgress) {
                    for (Iterator<Trinity<Project, String, String>> it = myInProgress.iterator(); it.hasNext(); ) {
                        Trinity<Project, String, String> trinity = it.next();
                        if (project.equals(trinity.first)) {
                            it.remove();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void initExecuteActions() {
        Executor.EP_NAME.forEachExtensionSafe(this::initExecutor);
    }

    synchronized void initExecutor(@Nonnull Executor executor) {
        if (myId2Executor.get(executor.getId()) != null) {
            LOG.error("Executor with id: \"" + executor.getId() + "\" was already registered!");
        }

        if (myContextActionIdSet.contains(executor.getContextActionId())) {
            LOG.error("Executor with context action id: \"" + executor.getContextActionId() + "\" was already registered!");
        }

        myExecutors.add(executor);
        myId2Executor.put(executor.getId(), executor);
        myContextActionIdSet.add(executor.getContextActionId());

        registerAction(executor.getId(), new ExecutorAction(this, executor, myRunCurrentFileService), RUNNERS_GROUP, myId2Action);
        registerAction(executor.getContextActionId(), new RunContextAction(executor), RUN_CONTEXT_GROUP, myContextActionId2Action);
    }

    private void registerAction(@Nonnull String actionId, @Nonnull AnAction anAction, @Nonnull String groupId, @Nonnull Map<String, AnAction> map) {
        AnAction action = myActionManager.getAction(actionId);
        if (action == null) {
            myActionManager.registerAction(actionId, anAction);
            map.put(actionId, anAction);
            action = anAction;
        }

        ((DefaultActionGroup) myActionManager.getAction(groupId)).add(action, Constraints.LAST, myActionManager);
    }

    synchronized void deinitExecutor(@Nonnull Executor executor) {
        myExecutors.remove(executor);
        myId2Executor.remove(executor.getId());
        myContextActionIdSet.remove(executor.getContextActionId());

        unregisterAction(executor.getId(), RUNNERS_GROUP, myId2Action);
        unregisterAction(executor.getContextActionId(), RUN_CONTEXT_GROUP, myContextActionId2Action);
    }

    private void unregisterAction(@Nonnull String actionId, @Nonnull String groupId, @Nonnull Map<String, AnAction> map) {
        DefaultActionGroup group = (DefaultActionGroup) myActionManager.getAction(groupId);
        if (group != null) {
            group.remove(myActionManager.getAction(actionId));
            AnAction action = map.get(actionId);
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
    public Executor getExecutorById(String executorId) {
        return myId2Executor.get(executorId);
    }

    @Nonnull
    private static Trinity<Project, String, String> createExecutionId(String executorId, @Nonnull ExecutionEnvironment environment) {
        return Trinity.create(environment.getProject(), executorId, environment.getRunner().getRunnerId());
    }

    @Override
    public boolean isStarting(Project project, String executorId, String runnerId) {
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

}
