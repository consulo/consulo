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
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.MessageBoxes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

@ActionImpl(id = "ExternalSystem.RemoveRunConfiguration")
public class RemoveExternalSystemRunConfigurationAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public RemoveExternalSystemRunConfigurationAction() {
        super(
            ExternalSystemLocalize.actionRemoveRunConfigurationText(),
            ExternalSystemLocalize.actionRemoveRunConfigurationDescription(),
            PlatformIconGroup.generalRemove()
        );
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(
            e.getData(Project.KEY) != null && EditExternalSystemRunConfigurationAction.findSettings(e) != null
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        RunnerAndConfigurationSettings settings = EditExternalSystemRunConfigurationAction.findSettings(e);
        if (project == null || settings == null) {
            return;
        }

        MessageBoxes.yesNo()
            .asQuestion()
            .title(ExternalSystemLocalize.confirmation())
            .text(ExternalSystemLocalize.delete0(settings.getName()))
            .showAsync()
            .whenComplete((confirmed, error) -> {
                if (Boolean.TRUE.equals(confirmed)) {
                    RunManager.getInstance(project).removeConfiguration(settings);
                }
            });
    }
}
