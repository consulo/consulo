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
package consulo.continuousIntegration.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.continuousIntegration.impl.internal.setting.CIServersConfigurable;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

/**
 * @since 2026-11-09
 */
@ActionImpl(id = "ContinuousIntegration.Settings")
public class CISettingsAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public CISettingsAction() {
        super(
            ContinuousIntegrationImplLocalize.actionSettingsText(),
            ContinuousIntegrationImplLocalize.actionSettingsDescription(),
            PlatformIconGroup.generalSettings()
        );
    }

    @RequiredUIAccess
    public static void showSettings(Project project) {
        project.getApplication().getInstance(ShowConfigurableService.class).showAndSelect(project, CIServersConfigurable.class);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project != null) {
            showSettings(project);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabled(project != null && !project.isDefault());
    }
}
