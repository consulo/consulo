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
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * Leaf node representing a single external system task.
 * Shows shortcut and task-activation hints in grey text.
 *
 * @author Vladislav.Soroka
 */
public class TaskNode extends ExternalSystemNode<TaskData> {
    private final TaskData myTaskData;

    public TaskNode(ExternalProjectsView externalProjectsView,
                    DataNode<TaskData> dataNode) {
        super(externalProjectsView, null, dataNode);
        myTaskData = dataNode.getData();
    }

    @Override
    protected void update(PresentationData presentation) {
        super.update(presentation);
        presentation.setIcon(getUiAware().getTaskIcon());

        String shortcutHint = StringUtil.nullize(
            getShortcutsManager().getDescription(myTaskData.getLinkedExternalProjectPath(), myTaskData.getName()));
        String activatorHint = StringUtil.nullize(
            getTaskActivator().getDescription(myTaskData.getOwner(), myTaskData.getLinkedExternalProjectPath(), myTaskData.getName()));

        String hint;
        if (shortcutHint == null) {
            hint = activatorHint;
        }
        else if (activatorHint == null) {
            hint = shortcutHint;
        }
        else {
            hint = shortcutHint + ", " + activatorHint;
        }

        setNameAndTooltip(presentation, getName(), myTaskData.getDescription(), hint);
    }

    @Override
    public boolean isVisible() {
        if (!super.isVisible()) return false;
        return !myTaskData.isInherited() || getExternalProjectsView().showInheritedTasks();
    }

    @Override
    public boolean isAlwaysLeaf() {
        return true;
    }

    @Override
    @Nullable
    protected String getMenuId() {
        return "ExternalSystemView.TaskMenu";
    }

    @Override
    @Nullable
    protected String getActionId() {
        return "ExternalSystem.RunTask";
    }
}
