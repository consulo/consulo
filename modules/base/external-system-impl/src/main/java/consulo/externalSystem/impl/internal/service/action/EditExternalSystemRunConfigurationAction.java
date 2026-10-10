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
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.view.ExternalSystemNode;
import consulo.externalSystem.view.RunConfigurationNode;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ActionImpl(id = "ExternalSystem.EditRunConfiguration")
public class EditExternalSystemRunConfigurationAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public EditExternalSystemRunConfigurationAction() {
        super(
            ExternalSystemLocalize.actionEditRunConfigurationText(),
            ExternalSystemLocalize.actionEditRunConfigurationDescription(),
            PlatformIconGroup.actionsEdit()
        );
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null && findSettings(e) != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        RunnerAndConfigurationSettings settings = findSettings(e);
        if (project == null || settings == null) {
            return;
        }

        RunManager.getInstance(project).setSelectedConfiguration(settings);
        RunConfigurationEditor.getInstance(project).editAll();
    }

    static @Nullable RunnerAndConfigurationSettings findSettings(AnActionEvent e) {
        List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
        if (selectedNodes == null || selectedNodes.size() != 1 || !(selectedNodes.get(0) instanceof RunConfigurationNode runConfigurationNode)) {
            return null;
        }
        return runConfigurationNode.getSettings();
    }
}
