/*
 * Copyright 2013-2025 consulo.io
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
package consulo.execution.impl.internal.action.runPopup;

import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.localize.ExecutionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-10-25
 */
public class SaveConfigurationAction extends DumbAwareAction {
    private final Project myProject;
    private final RunnerAndConfigurationSettings myRunConfigurationsSettings;

    public SaveConfigurationAction(Project project, RunnerAndConfigurationSettings runConfigurationsSettings) {
        super(
            ExecutionLocalize.actionSaveConfigurationFromPopup(),
            ExecutionLocalize.actionSaveConfigurationFromPopup(),
            null
        );
        myProject = project;
        myRunConfigurationsSettings = runConfigurationsSettings;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        RunManager runManager = RunManager.getInstance(myProject);
        if (myRunConfigurationsSettings.isTemporary()) {
            runManager.makeStable(myRunConfigurationsSettings);
        }
    }
}
