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
package consulo.externalSystem.model;

import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.externalSystem.internal.ui.ExternalSystemRecentTasksList;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.ui.awt.ExternalSystemTasksTreeModel;
import consulo.externalSystem.view.ExternalProjectsView;
import consulo.externalSystem.view.ExternalSystemNode;
import consulo.externalSystem.view.ProjectNode;
import consulo.project.ui.notification.NotificationGroup;
import consulo.ui.ex.awt.tree.SimpleTree;
import consulo.util.dataholder.Key;

import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 2/7/12 11:19 AM
 */
public interface ExternalSystemDataKeys {
  Key<ProjectSystemId> EXTERNAL_SYSTEM_ID = Key.create("external.system.id");
  Key<NotificationGroup> NOTIFICATION_GROUP = Key.create("external.system.notification");
  Key<ExternalSystemTasksTreeModel> ALL_TASKS_MODEL = Key.create("external.system.all.tasks.model");
  Key<ExternalTaskExecutionInfo> SELECTED_TASK = Key.create("external.system.selected.task");
  Key<ExternalProjectPojo> SELECTED_PROJECT = Key.create("external.system.selected.project");

  Key<ExternalSystemRecentTasksList> RECENT_TASKS_LIST = Key.create("external.system.recent.tasks.list");

  Key<Boolean> NEWLY_IMPORTED_PROJECT = Key.create("external.system.newly.imported");

  // Keys for ExternalProjectsView-based tool window
  Key<ExternalProjectsView> VIEW = Key.create("external.system.view");
  Key<SimpleTree> PROJECTS_TREE = Key.create("external.system.projects.tree");
  @SuppressWarnings("rawtypes")
  Key<List<ExternalSystemNode>> SELECTED_NODES = Key.create("external.system.selected.nodes");
  Key<ProjectNode> SELECTED_PROJECT_NODE = Key.create("external.system.selected.project.node");
  Key<ExternalSystemUiAware> UI_AWARE = Key.create("external.system.ui.aware");
}
