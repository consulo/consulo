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
package consulo.externalSystem.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.task.TaskData;
import consulo.externalSystem.view.ExternalProjectsView;
import consulo.externalSystem.view.ExternalSystemNode;
import consulo.externalSystem.view.TaskNode;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the selected external system task(s).
 * Triggered by double-click or Enter on a TaskNode in the external projects view.
 */
@ActionImpl(id = "ExternalSystem.RunTask")
public class RunExternalSystemTaskAction extends AnAction implements DumbAware {
    public RunExternalSystemTaskAction() {
        super(
            LocalizeValue.localizeTODO("Run Task"),
            LocalizeValue.localizeTODO("Run the selected external system task"),
            PlatformIconGroup.actionsExecute()
        );
    }

    @Override
    public void update(AnActionEvent e) {
        List<TaskNode> taskNodes = getSelectedTaskNodes(e);
        ExternalProjectsView view = e.getData(ExternalSystemDataKeys.VIEW);
        e.getPresentation().setEnabled(!taskNodes.isEmpty() && view != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        ExternalProjectsView view = e.getData(ExternalSystemDataKeys.VIEW);
        if (view == null) return;
        Project project = view.getProject();
        ProjectSystemId systemId = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
        if (systemId == null) systemId = view.getSystemId();
        if (project == null || systemId == null) return;

        List<TaskNode> taskNodes = getSelectedTaskNodes(e);
        if (taskNodes.isEmpty()) return;

        for (TaskNode taskNode : taskNodes) {
            TaskData taskData = taskNode.getData();
            if (taskData == null) continue;

            ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
            settings.setExternalSystemIdString(systemId.getId());
            settings.setExternalProjectPath(taskData.getLinkedExternalProjectPath());
            settings.setTaskNames(java.util.Collections.singletonList(taskData.getName()));
            settings.setExecutionName(taskData.getName());

            ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, project, systemId);
        }
    }

    private static List<TaskNode> getSelectedTaskNodes(AnActionEvent e) {
        List<ExternalSystemNode> nodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
        List<TaskNode> result = new ArrayList<>();
        if (nodes != null) {
            for (ExternalSystemNode<?> node : nodes) {
                if (node instanceof TaskNode taskNode) {
                    result.add(taskNode);
                }
            }
        }
        return result;
    }
}
