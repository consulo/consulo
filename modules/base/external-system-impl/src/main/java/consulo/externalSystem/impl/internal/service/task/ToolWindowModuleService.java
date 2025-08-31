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
package consulo.externalSystem.impl.internal.service.task;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.externalSystem.ui.awt.ExternalSystemTasksTreeModel;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.externalSystem.util.Order;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Ensures that all external system sub-projects are correctly represented at the external system tool window.
 *
 * @author Denis Zhdanov
 * @since 5/15/13 1:02 PM
 */
@Order(ExternalSystemConstants.BUILTIN_TOOL_WINDOW_SERVICE_ORDER)
@ExtensionImpl
public class ToolWindowModuleService extends AbstractToolWindowService<ModuleData> {

  @Nonnull
  public static final Function<DataNode<ModuleData>, ExternalProjectPojo> MAPPER = node -> ExternalProjectPojo.from(node.getData());

  @Nonnull
  @Override
  public Key<ModuleData> getTargetDataKey() {
    return ProjectKeys.MODULE;
  }

  @Override
  protected void processData(
    @Nonnull Collection<DataNode<ModuleData>> nodes,
    @Nonnull Project project,
    @Nullable ExternalSystemTasksTreeModel model
  ) {
    if (nodes.isEmpty()) {
      return;
    }
    ProjectSystemId externalSystemId = nodes.iterator().next().getData().getOwner();
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;

    Map<DataNode<ProjectData>, List<DataNode<ModuleData>>> grouped = ExternalSystemApiUtil.groupBy(nodes, ProjectKeys.PROJECT);
    Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> data = new HashMap<>();
    for (Map.Entry<DataNode<ProjectData>, List<DataNode<ModuleData>>> entry : grouped.entrySet()) {
      data.put(ExternalProjectPojo.from(entry.getKey().getData()), ContainerUtil.map2List(entry.getValue(), MAPPER));
    }

    AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(project);
    Set<String> pathsToForget = detectRenamedProjects(data, settings.getAvailableProjects());
    if (!pathsToForget.isEmpty()) {
      settings.forgetExternalProjects(pathsToForget);
    }
    Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects = new HashMap<>(settings.getAvailableProjects());
    projects.putAll(data);
    settings.setAvailableProjects(projects);
  }

  @Nonnull
  private static Set<String> detectRenamedProjects(
    @Nonnull Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> currentInfo,
    @Nonnull Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> oldInfo
  ) {
    Map<String/* external config path */, String/* project name */> map = new HashMap<>();
    for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : currentInfo.entrySet()) {
      map.put(entry.getKey().getPath(), entry.getKey().getName());
      for (ExternalProjectPojo pojo : entry.getValue()) {
        map.put(pojo.getPath(), pojo.getName());
      }
    }

    Set<String> result = new HashSet<>();
    for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : oldInfo.entrySet()) {
      String newName = map.get(entry.getKey().getPath());
      if (newName != null && !newName.equals(entry.getKey().getName())) {
        result.add(entry.getKey().getPath());
      }
      for (ExternalProjectPojo pojo : entry.getValue()) {
        newName = map.get(pojo.getPath());
        if (newName != null && !newName.equals(pojo.getName())) {
          result.add(pojo.getPath());
        }
      }
    }
    return result;
  }
}
