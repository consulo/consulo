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
package consulo.ide.impl.idea.openapi.externalSystem.service.task;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.model.task.TaskData;
import consulo.externalSystem.service.project.ExternalConfigPathAware;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.externalSystem.ui.awt.ExternalSystemTasksTreeModel;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.externalSystem.util.Order;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.ide.impl.idea.util.containers.ContainerUtilRt;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Denis Zhdanov
 * @since 5/15/13 7:21 PM
 */
@Order(ExternalSystemConstants.BUILTIN_TOOL_WINDOW_SERVICE_ORDER)
@ExtensionImpl
public class ToolWindowTaskService extends AbstractToolWindowService<TaskData> {

  @Nonnull
  public static final Function<DataNode<TaskData>, ExternalTaskPojo> MAPPER = node -> ExternalTaskPojo.from(node.getData());

  public static final NullableFunction<DataNode<TaskData>, ExternalConfigPathAware> TASK_HOLDER_RETRIEVAL_STRATEGY = new NullableFunction<>() {
    @Nullable
    @Override
    public ExternalConfigPathAware apply(DataNode<TaskData> node) {
      ModuleData moduleData = node.getData(ProjectKeys.MODULE);
      return moduleData == null ? node.getData(ProjectKeys.PROJECT) : moduleData;
    }
  };

  @Nonnull
  @Override
  public Key<TaskData> getTargetDataKey() {
    return ProjectKeys.TASK;
  }

  @Override
  protected void processData(@Nonnull Collection<DataNode<TaskData>> nodes, @Nonnull Project project, @Nullable final ExternalSystemTasksTreeModel model) {
    if (nodes.isEmpty()) {
      return;
    }
    ProjectSystemId externalSystemId = nodes.iterator().next().getData().getOwner();
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;

    Map<ExternalConfigPathAware, List<DataNode<TaskData>>> grouped = ExternalSystemApiUtil.groupBy(nodes, TASK_HOLDER_RETRIEVAL_STRATEGY);
    Map<String, Collection<ExternalTaskPojo>> data = new HashMap<>();
    for (Map.Entry<ExternalConfigPathAware, List<DataNode<TaskData>>> entry : grouped.entrySet()) {
      data.put(entry.getKey().getLinkedExternalProjectPath(), ContainerUtilRt.map2List(entry.getValue(), MAPPER));
    }

    AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(project);
    Map<String, Collection<ExternalTaskPojo>> availableTasks = ContainerUtilRt.newHashMap(settings.getAvailableTasks());
    availableTasks.putAll(data);
    settings.setAvailableTasks(availableTasks);

    if (model != null) {
      ExternalSystemUiUtil.apply(settings, model);
    }
  }
}
