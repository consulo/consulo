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

import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.Executor;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jspecify.annotations.Nullable;

class ExecuteExternalSystemRunConfigurationAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private final Executor myExecutor;
    private final boolean myEnabled;
    private final Project myProject;
    private final RunnerAndConfigurationSettings mySettings;
    private final @Nullable ProjectSystemId mySystemId;

    ExecuteExternalSystemRunConfigurationAction(Executor executor,
                                                boolean enabled,
                                                Project project,
                                                @Nullable ProjectSystemId projectSystemId,
                                                RunnerAndConfigurationSettings settings) {
        super(executor.getActionName(), LocalizeValue.empty(), executor.getIcon());
        myExecutor = executor;
        myEnabled = enabled;
        myProject = project;
        mySettings = settings;
        mySystemId = projectSystemId;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent event) {
        if (myEnabled) {
            ProgramRunnerUtil.executeConfiguration(mySettings, myExecutor);
            RunManager.getInstance(myProject).setSelectedConfiguration(mySettings);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(myEnabled);
    }
}
