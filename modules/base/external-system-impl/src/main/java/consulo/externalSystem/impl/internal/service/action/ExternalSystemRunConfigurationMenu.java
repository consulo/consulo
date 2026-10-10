// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
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
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorGroup;
import consulo.execution.runner.ProgramRunner;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ActionImpl(
    id = "ExternalSystemView.RunConfigurationMenu",
    children = {
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = EditExternalSystemRunConfigurationAction.class),
        @ActionRef(type = RemoveExternalSystemRunConfigurationAction.class)
    }
)
public class ExternalSystemRunConfigurationMenu extends DefaultActionGroup implements DumbAware {
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        Project project = e == null ? null : e.getData(Project.KEY);
        RunnerAndConfigurationSettings settings = e == null ? null : EditExternalSystemRunConfigurationAction.findSettings(e);
        if (project == null || settings == null) {
            return super.getChildren(e);
        }

        ProjectSystemId projectSystemId = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
        List<Executor> executors = new ArrayList<>();
        project.getApplication().getExtensionPoint(Executor.class).forEach(executor -> {
            if (executor instanceof ExecutorGroup<?> executorGroup) {
                executors.addAll(executorGroup.childExecutors());
            }
            else {
                executors.add(executor);
            }
        });

        List<AnAction> actions = new ArrayList<>();
        for (Executor executor : executors) {
            if (!executor.isApplicable(project)) {
                continue;
            }
            ProgramRunner<?> runner = ProgramRunnerUtil.getRunner(executor.getId(), settings);
            actions.add(new ExecuteExternalSystemRunConfigurationAction(executor, runner != null, project, projectSystemId, settings));
        }
        Collections.addAll(actions, super.getChildren(e));
        return actions.toArray(AnAction.EMPTY_ARRAY);
    }
}
