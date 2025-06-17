/*
 * Copyright 2013-2022 consulo.io
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
package consulo.execution.impl.internal.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.Executor;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2022-04-05
 */
@Singleton
@ServiceImpl
public class RunConfigurationEditorImpl implements RunConfigurationEditor {
    private final Project myProject;

    @Inject
    public RunConfigurationEditorImpl(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public void editAll() {
        if (EarlyAccessProgramManager.is(RunConfigurationFileEditorEarlyAccessDescriptor.class)) {
            UIAccess.current().give(
                () -> myProject.getApplication().getInstance(ConfigurationFileEditorManager.class)
                    .open(myProject, RunConfigurationEditorProvider.class, Map.of())
            );
        }
        else {
            new EditConfigurationsDialog(myProject).showAsync();
        }
    }

    @Override
    public boolean editConfiguration(
        Project project,
        RunnerAndConfigurationSettings configuration,
        String title,
        @Nullable Executor executor
    ) {
        return RunDialog.editConfiguration(project, configuration, title, executor);
    }
}
