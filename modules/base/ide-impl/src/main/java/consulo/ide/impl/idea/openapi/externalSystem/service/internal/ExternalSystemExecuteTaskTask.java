/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.service.internal;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.externalSystem.service.ExternalSystemFacadeManager;
import consulo.ide.impl.idea.openapi.externalSystem.service.RemoteExternalSystemFacade;
import consulo.ide.impl.idea.openapi.externalSystem.service.remote.RemoteExternalSystemTaskManager;
import consulo.ide.impl.idea.util.containers.ContainerUtilRt;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author Denis Zhdanov
 * @since 3/15/13 10:02 PM
 */
public class ExternalSystemExecuteTaskTask extends AbstractExternalSystemTask {

  @Nonnull
  private static final Function<ExternalTaskPojo, String> MAPPER = ExternalTaskPojo::getName;

  @Nonnull
  private final List<ExternalTaskPojo> myTasksToExecute;
  @Nullable
  private final String myVmOptions;
  @Nullable
  private String myScriptParameters;
  @Nullable private final String myDebuggerSetup;

  public ExternalSystemExecuteTaskTask(
    @Nonnull ProjectSystemId externalSystemId,
    @Nonnull Project project,
    @Nonnull List<ExternalTaskPojo> tasksToExecute,
    @Nullable String vmOptions,
    @Nullable String scriptParameters,
    @Nullable String debuggerSetup
  ) throws IllegalArgumentException {
    super(externalSystemId, ExternalSystemTaskType.EXECUTE_TASK, project, getLinkedExternalProjectPath(tasksToExecute));
    myTasksToExecute = tasksToExecute;
    myVmOptions = vmOptions;
    myScriptParameters = scriptParameters;
    myDebuggerSetup = debuggerSetup;
  }

  @Nonnull
  private static String getLinkedExternalProjectPath(@Nonnull Collection<ExternalTaskPojo> tasks) throws IllegalArgumentException {
    if (tasks.isEmpty()) {
      throw new IllegalArgumentException("Can't execute external tasks. Reason: given tasks list is empty");
    }
    String result = null;
    for (ExternalTaskPojo task : tasks) {
      String path = task.getLinkedExternalProjectPath();
      if (result == null) {
        result = path;
      }
      else if (!result.equals(path)) {
        throw new IllegalArgumentException(String.format(
          "Can't execute given external system tasks. Reason: expected that all of them belong to the same external project " +
          "but they are not (at least two different projects detected - '%s' and '%s'). Tasks: %s",
          result,
          task.getLinkedExternalProjectPath(),
          tasks
        ));
      }
    }
    assert result != null;
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doExecute() throws Exception {
    final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
    ExternalSystemExecutionSettings settings =
      ExternalSystemApiUtil.getExecutionSettings(getIdeProject(), getExternalProjectPath(), getExternalSystemId());
    RemoteExternalSystemFacade facade = manager.getFacade(getIdeProject(), getExternalProjectPath(), getExternalSystemId());
    RemoteExternalSystemTaskManager taskManager = facade.getTaskManager();
    List<String> taskNames = ContainerUtilRt.map2List(myTasksToExecute, MAPPER);

    final List<String> vmOptions = parseCmdParameters(myVmOptions);
    final List<String> scriptParametersList = parseCmdParameters(myScriptParameters);

    taskManager.executeTasks(getId(), taskNames, getExternalProjectPath(), settings, vmOptions, scriptParametersList, myDebuggerSetup);
  }

  @Override
  protected boolean doCancel() throws Exception {
    final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
    RemoteExternalSystemFacade facade = manager.getFacade(getIdeProject(), getExternalProjectPath(), getExternalSystemId());
    RemoteExternalSystemTaskManager taskManager = facade.getTaskManager();

    return taskManager.cancelTask(getId());
  }

  private static List<String> parseCmdParameters(@Nullable String cmdArgsLine) {
    return cmdArgsLine != null ? ParametersListUtil.parse(cmdArgsLine) : new ArrayList<>();
  }
}
