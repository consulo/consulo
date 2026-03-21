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
package consulo.externalSystem.view;

import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.task.TaskData;
import consulo.externalSystem.util.Order;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;

import java.util.*;

/**
 * Container node for external system tasks. Supports flat and grouped display.
 *
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemNode.BUILTIN_TASKS_DATA_NODE_ORDER)
public class TasksNode extends ExternalSystemNode<Object> {
    private final Collection<DataNode<?>> myTaskDataNodes;

    public TasksNode(ExternalProjectsView externalProjectsView,
                     Collection<DataNode<?>> taskDataNodes) {
        super(externalProjectsView, null, null);
        myTaskDataNodes = taskDataNodes;
    }

    @Override
    public String getName() {
        return "Tasks";
    }

    @Override
    protected void update(PresentationData presentation) {
        super.update(presentation);
        presentation.setIcon(PlatformIconGroup.nodesConfigfolder());
    }

    @Override
   
    protected List<ExternalSystemNode<?>> doBuildChildren() {
        List<TaskNode> taskNodes = new ArrayList<>();
        for (DataNode<?> dataNode : myTaskDataNodes) {
            if (dataNode.getData() instanceof TaskData) {
                //noinspection unchecked
                taskNodes.add(new TaskNode(getExternalProjectsView(), (DataNode<TaskData>) dataNode));
            }
        }

        if (!getExternalProjectsView().getGroupTasks()) {
            return new ArrayList<>(taskNodes);
        }

        // Group tasks by their group name (lowercased)
        MultiMap<String, TaskNode> groups = new MultiMap<>();
        List<TaskNode> ungrouped = new ArrayList<>();
        for (TaskNode taskNode : taskNodes) {
            TaskData data = taskNode.getData();
            if (data != null && !StringUtil.isEmptyOrSpaces(data.getGroup())) {
                groups.putValue(data.getGroup().toLowerCase(Locale.ROOT), taskNode);
            }
            else {
                ungrouped.add(taskNode);
            }
        }

        if (groups.isEmpty()) {
            return new ArrayList<>(taskNodes);
        }

        List<ExternalSystemNode<?>> result = new ArrayList<>();
        for (Map.Entry<String, Collection<TaskNode>> entry : groups.entrySet()) {
            if (entry.getValue().size() == 1) {
                result.addAll(entry.getValue());
            }
            else {
                GroupNode groupNode = new GroupNode(getExternalProjectsView(), entry.getKey());
                groupNode.addAll(entry.getValue());
                result.add(groupNode);
            }
        }
        result.addAll(ungrouped);
        return result;
    }

    /**
     * Inner node grouping tasks of the same group.
     */
    private static class GroupNode extends ExternalSystemNode<Object> {
        private final String myGroupName;

        GroupNode(ExternalProjectsView view, String groupName) {
            super(view, null, null);
            myGroupName = groupName;
        }

        @Override
        public String getName() {
            return myGroupName;
        }

        @Override
        protected void update(PresentationData presentation) {
            super.update(presentation);
            presentation.setIcon(PlatformIconGroup.nodesConfigfolder());
        }

        @Override
        protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
            // Children are pre-populated via addAll() in TasksNode.doBuildChildren().
            // Calling getChildren() here would recurse into buildChildren() → doBuildChildren()
            // when myChildren==null, causing a StackOverflowError.
            // The base class already returns myChildrenList when myDataNode==null, which is correct.
            return super.doBuildChildren();
        }
    }
}
